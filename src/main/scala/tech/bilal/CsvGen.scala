package tech.bilal

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.mongodb.scala.bson.*
import tech.bilal.Extensions.*
import tech.bilal.StringEncoder.*
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CsvGen(schema: SchemaGen, printer: Printer)(using system: ActorSystem) extends StreamFlows {

  import printer.*
  
  def generateCsv(source: => Source[ByteString, Future[IOResult]]): Source[CSVRow, Future[IOResult]] = {
    println("-" * 60)
    println("Generating schema...")

    val Schema(paths, totalRows) = schema.generate(source)
      .alsoTo(Sink.foreach(x => print(s"\rfound ${x.paths.size + 1} unique fields in ${x.rows} records... ")))
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
      .toMat(Sink.last)(Keep.both)
      .mapMaterializedValue(x => x._1.flatMap(_ => x._2))
      .run
      .block()

    val params = paths.toList 
    
    println("DONE")
    println("-" * 60)
    println("Generating csv...")

    val contents: Source[CSVRow, Future[IOResult]] =
      source
        .via(lineMaker)
        .dropWhile(!_.utf8String.startsWith("{"))
        .via(jsonFrame)
        .via(bsonConvert)
        .map(x => getCsvRow(x, params))
        .via(viaIndex(x => print(s"\rprocessed ${(((x._2 + 1D) / totalRows.toDouble) * 100).toInt}%... ")))

    val header: Source[CSVRow, NotUsed] =
      Source.single(CSVRow(params.map(_.toString)))

    header
      .concatMat(contents)(Keep.right)
  }

  private def getCsvRow(bsonDocument: BsonDocument, params: List[JsonPath]): CSVRow =
    CSVRow(
      params
        .map(bsonDocument.getLeafValue)
        .map(getStringRepr)
    )
    
  private def getStringRepr(bsonValueMaybe: Option[BsonValue]): String =
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