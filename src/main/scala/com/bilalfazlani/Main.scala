package com.bilalfazlani

import com.bilalfazlani.rainbowcli.*
import scala.util.{Success, Failure}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Framing.FramingException
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import scopt.OParser
import java.io.File
import org.bson.json.JsonParseException
import FileTypeFinder.UnknownFileTypeException
import java.nio.file.{Path, Paths}
import scala.concurrent.Await
import scala.concurrent.duration.*

case class CLIOptions(
    inputPath: File = new File("."),
    outputFile: Option[File] = None,
    filePattern: String = "*.{json,bson}",
    recursive: Boolean = false,
    addFilenameColumn: Option[Boolean] = None,
    filenameColumnName: String = "_filename",
    noColor: Boolean = false,
    overrideFile: Boolean = false
)

object CLIOptions {
  def getOutputFile(config: CLIOptions): File =
    config.outputFile.getOrElse {
      if (config.inputPath.isDirectory) {
        // Directory processing: default to directoryname_combined.csv
        val directoryName = config.inputPath.getName
        new File(s"${directoryName}_combined.csv")
      } else {
        // File processing: existing logic
        val inputFile = config.inputPath
        if inputFile.toPath.toString != "." && inputFile.exists then
          val fileName = inputFile.getName
          val reg = "(.*)\\.(.*)".r
          val withoutExtension = fileName match {
            case reg(n, _) => n
            case x         => x
          }
          val parent = Path.of(inputFile.getCanonicalPath).getParent.toString
          Paths.get(parent, withoutExtension + ".csv").toFile
        else File(".")
      }
    }
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
      case VersionCheck.UpdateAvailable(update, _) =>
        println(
          "[info] ".yellow.bold + "update ".yellow + s"v$update".green.bold + " available".yellow
        )
        println()
      case _ =>
    }

  private def terminate(using system: ActorSystem): Unit =
    Await.result(system.terminate(), 10.seconds)

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
        arg[File]("input-path")
          .valueName("<path>")
          .required()
          .text("[REQUIRED] input file or directory path")
          .validate(f =>
            if (f.exists()) success
            else failure(s"path ${f.getPath} does not exist")
          )
          .action((f, c) => c.copy(inputPath = f)),
        arg[Option[File]]("output-file")
          .valueName("<file>")
          .text(
            "output CSV file. Default: <input>.csv or <dirname>_combined.csv"
          )
          .optional()
          .action((o, c) => c.copy(outputFile = o)),
        opt[String]("pattern")
          .valueName("<pattern>")
          .text("file pattern for directories. Default: *.{json,bson}")
          .action((pattern, c) => c.copy(filePattern = pattern)),
        opt[Unit]('r', "recursive")
          .text("process directories recursively")
          .action((_, c) => c.copy(recursive = true)),
        opt[Unit]("add-filename")
          .text("add filename column (auto-enabled for directories)")
          .action((_, c) => c.copy(addFilenameColumn = Some(true))),
        opt[Unit]("no-filename")
          .text("disable filename column for directories")
          .action((_, c) => c.copy(addFilenameColumn = Some(false))),
        opt[String]("filename-column")
          .valueName("<name>")
          .text("custom filename column name. Default: _filename")
          .action((name, c) => c.copy(filenameColumnName = name)),
        opt[Unit]('f', "overwrite")
          .text("delete and create new output file if exists")
          .action((_, c) => c.copy(overrideFile = true)),
        opt[Unit]("no-color")
          .text("disable colored output")
          .action((_, c) => c.copy(noColor = true)),
        help("help").text("print help text"),
        checkConfig { config =>
          if (!config.overrideFile) {
            val outputFile = CLIOptions.getOutputFile(config)
            if (outputFile.toPath.toString != "." && outputFile.exists) {
              failure(
                s"file ${outputFile.toPath} already exists." +
                  s"\nUse 'overwrite' option to ignore this error"
              )
            } else success
          } else success
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
    println("Input: ".bold + options.inputPath.toPath)

    given system: ActorSystem = ActorSystem("main")
    import system.dispatcher

    val processingFuture = if (options.inputPath.isDirectory) {
      // Directory processing
      val directoryProcessor = new DirectoryProcessor()
      directoryProcessor.processDirectory(options)
    } else {
      // Single file processing (existing logic)
      processSingleFile(options)
    }

    processingFuture
      .onComplete {
        case Success(_) =>
          if (!options.inputPath.isDirectory) {
            println("DONE".green.bold)
          }
          terminate
        case Failure(Error.NoRows) =>
          println("FAILED".red.bold)
          println("No json records found in file".red)
          terminate
          sys.exit(1)
        case Failure(Error.NoFields) =>
          println("FAILED".red.bold)
          println("No fields found in any records".red)
          terminate
          sys.exit(1)
        case Failure(err) if err.isInstanceOf[UnknownFileTypeException] =>
          println("FAILED".red.bold)
          println(err.getMessage.red)
          terminate
          sys.exit(1)
        case Failure(err) if err.getCause.isInstanceOf[FramingException] =>
          println("FAILED".red.bold)
          println("Invalid JSON encountered")
          terminate
          sys.exit(1)
        case Failure(err) if err.getCause.isInstanceOf[JsonParseException] =>
          println("FAILED".red.bold)
          println("Invalid JSON encountered")
          terminate
          sys.exit(1)
        case Failure(err) =>
          println("FAILED".red.bold)
          terminate
          err.printStackTrace
          sys.exit(1)
      }
  }

  private def processSingleFile(
      options: CLIOptions
  )(using ActorSystem, ColorContext): scala.concurrent.Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val outputFile = CLIOptions.getOutputFile(options)
    println("Output: ".bold + outputFile.toPath.toString)

    val fileTypeFinder = new FileTypeFinder
    print("File type: ".bold)

    fileTypeFinder
      .find(file(options.inputPath.toPath))
      .flatMap { fileType =>
        println(fileType.toString.yellow)
        val jsonFraming = new JsonFraming
        val schemaGen = new SchemaGen(jsonFraming)
        schemaGen
          .generate(file(options.inputPath.toPath))
          .alsoTo(Sink.foreach { (x: Schema) =>
            val columns = s"${x.paths.size} unique fields".yellow
            val rows = s"${x.rows} records".yellow
            val title = "Generating schema".bold
            print(s"\r$title: found $columns in $rows ")
          })
          .toMat(Sink.last)(Keep.both)
          .mapMaterializedValue(x => x._1.flatMap(_ => x._2))
          .run()
          .flatMap { schema =>
            if (schema.rows == 0) throw Error.NoRows
            else if (schema.paths.size == 0) throw Error.NoFields
            else println("DONE".green.bold)

            // Check if filename column should be added for single file
            val includeFilename = options.addFilenameColumn.getOrElse(false)

            if (includeFilename) {
              // Use DirectoryCsvGen for single file with filename column
              val schemaWithFilename =
                SchemaWithFilename(schema, true, options.filenameColumnName)
              val filename = options.inputPath.getName
              val filesWithSources =
                List((filename, file(options.inputPath.toPath)))
              val csvGen = new DirectoryCsvGen(
                schemaWithFilename,
                Printer.console,
                jsonFraming
              )
              csvGen
                .generateCsv(filesWithSources)
                .via(byteString)
                .runWith(fileSink(outputFile.toPath, options.overrideFile))
                .map(_ => ())
            } else {
              // Use original CsvGen for single file without filename column
              val csvGen = new CsvGen(schema, Printer.console, jsonFraming)
              val stream = csvGen.generateCsv(file(options.inputPath.toPath))
              stream
                .via(byteString)
                .runWith(fileSink(outputFile.toPath, options.overrideFile))
                .map(_ => ())
            }
          }
      }
  }
}

enum Error extends RuntimeException {
  case NoRows, NoFields
}
