package tech.bilal

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source}
import org.mongodb.scala.bson.*
import scopt.OParser
import tech.bilal.Extensions.*
import tech.bilal.StringEncoder.*

import java.io.File
import java.nio.file.{Path, StandardOpenOption}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

case class CLIOptions(
    inputFile: File = new File("."),
    outputFile: Option[File] = None
)

object MongoMain extends StreamFlows {

  def main(args:Array[String]):Unit = {
    val builder = OParser.builder[CLIOptions]
    val parser = {
      import builder.*
      OParser.sequence(
        programName("bson-json-to-csv"),
        head("convert nested bson/json files to flat csv files"),
        arg[File]("input-file")
          .valueName("<file>")
          .required()
          .text("[REQUIRED] input bson file")
          .validate(f =>
            if (f.exists()) success
            else failure(s"file ${f.getPath} does not exist")
          )
          .action((f, c) => c.copy(inputFile = f)),
        arg[Option[File]]("output-file")
          .valueName("<file>")
          .text(
            "Path for output csv file. If a file exits, it will be overridden. " +
              "If no value is provided, a new file with same path + name is " +
              "created with '.csv' extension"
          )
          .optional()
          .action((o, c) => c.copy(outputFile = o)),
        help("help").text("prints this usage text")
      )
    }

    OParser.parse(parser, args, CLIOptions()) match {
      case Some(options) =>
        run(options)
      case None =>
    }
  }

  def run(options: CLIOptions): Unit = {
    implicit val system: ActorSystem = ActorSystem("main")

    val schema = new SchemaGen

    println("-" * 60)
    println("Generating schema...")

    val params = schema
      .generate(file(options.inputFile.getPath))
      .via(viaIndex(x => print(s"\rfound ${x._2 + 1} columns...")))
      .runWith(Sink.collection)
      .recover {
        case NonFatal(_: FramingException) =>
          println("Invalid JSON encountered")
          system.terminate().block()
          sys.exit(1)
        case NonFatal(err) =>
          err.printStackTrace()
          system.terminate().block()
          sys.exit(1)
      }
      .block()
      .toList

    println("DONE")
    println("-" * 60)
    println("Generating csv...")

    val contents: Source[CSVRow, Future[IOResult]] =
      file(options.inputFile.getPath)
        .via(lineMaker)
        .dropWhile(!_.utf8String.startsWith("{"))
        .via(jsonFrame)
        .via(bsonConvert)
        .map(x => getCsvRow(x, params))
        .via(viaIndex(x => print(s"\rprocessed ${x._2 + 1} rows...")))

    val header: Source[CSVRow, NotUsed] =
      Source.single(CSVRow(params.map(_.toString)))

    header
      .concatMat(contents)(Keep.right)
      .via(byteString)
      .recover {
        case NonFatal(err) =>
          err.printStackTrace()
          system.terminate().block()
          sys.exit(1)
      }
      .toMat(
        FileIO.toPath(
          Path.of(
            options.outputFile
              .map(_.getPath)
              .getOrElse(options.inputFile.getPath + ".csv")
          ),
          Set(
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
          ),
          0
        )
      )(Keep.both)
      .mapMaterializedValue(a => a._1.flatMap(_ => a._2))
      .run()
      .block()

    system.terminate().block()

    println("DONE")
    println("-" * 60)
  }

  def getCsvRow(bsonDocument: BsonDocument, params: List[JsonPath]): CSVRow =
    CSVRow(
      params
        .map(bsonDocument.getLeafValue)
        .map(getStringRepr)
    )

  def getStringRepr(bsonValueMaybe: Option[BsonValue]): String =
    (bsonValueMaybe match {
      case Some(null) | None => ""
      case Some(bsonValue) =>
        bsonValue match {
          case x: BsonString     => x.encodeToString
          case x: BsonBoolean    => x.encodeToString
          case x: BsonDateTime   => x.encodeToString
          case x: BsonObjectId   => x.encodeToString
          case x: BsonInt32      => x.encodeToString
          case x: BsonInt64      => x.encodeToString
          case x: BsonDouble     => x.encodeToString
          case x: BsonDecimal128 => x.encodeToString
          case x: BsonTimestamp  => x.encodeToString
          case _: BsonNull       => ""
        }
    })
      .replaceAll(",", "")
      .replaceAll("\n", "")
}
