package com.bilalfazlani

import com.bilalfazlani.rainbowcli.*
import akka.NotUsed
import scala.util.{Success, Failure}
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source}
import org.mongodb.scala.bson.*
import scopt.OParser
import StringEncoder.*
import java.io.File
import org.bson.json.JsonParseException
import java.nio.file.{Path, StandardOpenOption}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import FileTypeFinder.UnknownFileTypeException
import java.nio.file.Paths

case class CLIOptions(
    inputFile: File = new File("."),
    outputFile: Option[File] = None,
    noColor: Boolean = false,
    overrideFile: Boolean = false
)

object CLIOptions {
  def getOutputFile(inputFile: File): File =
    if inputFile.toPath.toString != "." && inputFile.exists then
      val fileName = inputFile.getName
      val reg = "(.*)\\.(.*)".r
      val withoutExtension = fileName match {
        case reg(n, e) => n
        case x         => x
      }
      val parent = Path.of(inputFile.getCanonicalPath).getParent.toString
      Paths.get(parent, withoutExtension + ".csv").toFile
    else File(".")
}

object Main extends StreamFlows {

  private def getVersionString =
    AppVersion.check match {
      case VersionCheck.UpdateAvailable(update, current) =>
        s"version: v$current (update v$update available)"
      case VersionCheck.NoUpdateAvailable =>
        s"version: v${BuildInfo.version}"
      case VersionCheck.CouldNotCheckLatestVersion =>
        s"version: v${BuildInfo.version}"
    }

  private def printVersionUpdate(using ColorContext) =
    AppVersion.check match {
      case VersionCheck.UpdateAvailable(update, current) =>
        println("[info] ".yellow.bold + "update ".yellow + s"v$update".green.bold + " available".yellow)
        println()
      case _ =>
    }

  def main(args: Array[String]): Unit = {
    val builder = OParser.builder[CLIOptions]
    val parser = {
      import builder.*
      OParser.sequence(
        programName("bson-json-to-csv"),
        head(
          "convert nested bson/json files to flat csv files\n" +
            getVersionString
        ),
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
          .text("Path for output csv file. Default: <input-file>.csv")
          .optional()
          .action((o, c) => c.copy(outputFile = o)),
        opt[Unit]('f', "overwrite")
          .text("delete and create a new new outfile if one already exists")
          .optional()
          .action((o, c) => c.copy(overrideFile = true)),
        opt[Unit]("no-color")
          .text("do not use colors for output text")
          .optional()
          .action((o, c) => c.copy(noColor = true)),
        help("help").text("print help text"),
        checkConfig {
          case x @ CLIOptions(_, _, _, false) =>
            val outputFile =
              x.outputFile.getOrElse(CLIOptions.getOutputFile(x.inputFile))
            if outputFile.toPath.toString != "." && outputFile.exists then
              failure(
                s"file ${outputFile.toPath} already exists." +
                  s"\nUse 'overwrite' option to ignore this error"
              )
            else success
          case _ => success
        }
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
    printVersionUpdate
    println("Input: ".bold + options.inputFile.toPath)
    val outputFile =
      options.outputFile.getOrElse(CLIOptions.getOutputFile(options.inputFile))
    println("Output: ".bold + outputFile.toPath.toString)

    given system: ActorSystem = ActorSystem("main")

    import system.dispatcher
    val fileTypeFinder = new FileTypeFinder
    print("File type: ".bold)

    fileTypeFinder
      .find(file(options.inputFile.toPath))
      .flatMap { fileType =>
        println(fileType.toString.yellow)
        val jsonFraming = new JsonFraming
        val schemaGen = new SchemaGen(jsonFraming)
        schemaGen
          .generate(file(options.inputFile.toPath))
          .alsoTo(Sink.foreach { (x: Schema) =>
            val columns = s"${x.paths.size} unique fields".yellow
            val rows = s"${x.rows} records".yellow
            val title = "Generating schema".bold
            print(s"\r$title: found $columns in $rows ")
          })
          .toMat(Sink.last)(Keep.both)
          .mapMaterializedValue(x => x._1.flatMap(_ => x._2))
          .run
          .flatMap { schema =>
            if (schema.rows == 0) throw Error.NoRows
            else if (schema.paths.size == 0) throw Error.NoFields
            else println("DONE".green.bold)
            val csvGen = new CsvGen(schema, Printer.console, jsonFraming)
            val stream = csvGen.generateCsv(file(options.inputFile.toPath))
            stream
              .via(byteString)
              .toMat(
                fileSink(outputFile.toPath, options.overrideFile)
              )(Keep.both)
              .mapMaterializedValue(a => a._1.flatMap(_ => a._2))
              .run()
          }
      }
      .onComplete {
        case Success(_) =>
          println("DONE".green.bold)
          system.terminate().block
        case Failure(Error.NoRows) =>
          println("FAILED".red.bold)
          println("No json records found in file".red)
          system.terminate().block
          sys.exit(1)
        case Failure(Error.NoFields) =>
          println("FAILED".red.bold)
          println("No fields found in any records".red)
          system.terminate().block
          sys.exit(1)
        case Failure(err) if err.isInstanceOf[UnknownFileTypeException] =>
          println("FAILED".red.bold)
          println(err.getMessage.red)
          system.terminate().block
          sys.exit(1)
        case Failure(err) if err.getCause.isInstanceOf[FramingException] =>
          println("FAILED".red.bold)
          println("Invalid JSON encountered")
          system.terminate().block
          sys.exit(1)
        case Failure(err) if err.getCause.isInstanceOf[JsonParseException] =>
          println("FAILED".red.bold)
          println("Invalid JSON encountered")
          system.terminate().block
          sys.exit(1)
        case Failure(err) =>
          println("FAILED".red.bold)
          system.terminate().block
          err.printStackTrace
          sys.exit(1)
      }
  }
}

enum Error extends RuntimeException {
  case NoRows, NoFields
}
