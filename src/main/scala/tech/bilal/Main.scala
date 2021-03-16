package tech.bilal

import com.bilalfazlani.scala.rainbow.*
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
    outputFile: Option[File] = None,
    noColor: Boolean = false
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
        opt[Unit]("no-color")
          .text("does not use colors for output text")
          .optional()
          .action((o, c) => c.copy(noColor = true)),
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
    given ColorContext = ColorContext(enable = !options.noColor)
    given system: ActorSystem = ActorSystem("main")
    val schemaGen = new SchemaGen

    val schema = schemaGen.generate(file(options.inputFile.getPath))
      .alsoTo(Sink.foreach{ (x:Schema) => 
        val columns = (s"${(x.paths.size)} unique fields").yellow
        val rows = (s"${x.rows} records").yellow
        val title = "Generating schema".bold
        print(s"\r$title: found $columns in $rows ")
      })
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
      .flatMap { schema => 
        println("DONE".green.bold)
        val csvGen = new CsvGen(schema, Printer.console)
        val stream = csvGen.generateCsv(file(options.inputFile.getPath))
        val fileName = options.outputFile
              .map(_.getPath)
              .getOrElse(options.inputFile.getPath + ".csv")
        stream
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
      }.onComplete {
        case Success(_) => 
          println("DONE".green.bold)
          system.terminate().block()
        case Failure(err) => 
          println("FAILED".red.bold)
          err.printStackTrace
          system.terminate().block()
      }
  }
}
