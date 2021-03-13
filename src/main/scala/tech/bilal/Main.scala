package tech.bilal

import akka.NotUsed
import scala.util.{Success, Failure}
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

object Main extends StreamFlows {

  def main(args: Array[String]): Unit = {
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
    given system: ActorSystem = ActorSystem("main")

    val csvGen = new CsvGen(new SchemaGen, Printer.console)
    val stream = csvGen.generateCsv(file(options.inputFile.getPath))
    val fileName = options.outputFile
              .map(_.getPath)
              .getOrElse(options.inputFile.getPath + ".csv")

    val f = stream
      .via(byteString)
      .recover {
        case NonFatal(err) =>
          err.printStackTrace()
          system.terminate().block()
          sys.exit(1)
      }
      .toMat(fileSink(fileName))(Keep.both)
      .mapMaterializedValue(a => a._1.flatMap(_ => a._2))
      .run()

    f.onComplete{
      case Success(_) => 
        println("DONE")
        println("-" * 60)
        system.terminate().block()
      case Failure(err) => 
        println("FAILED")
        println("x" * 60)
        err.printStackTrace
        system.terminate().block()
    }
  }
}
