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
import tech.bilal.StringEncoder.*
import java.io.File
import org.bson.json.JsonParseException
import java.nio.file.{Path, StandardOpenOption}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

case class CLIOptions(
    inputFile: File = new File("."),
    outputFile: Option[File] = None,
    noColor: Boolean = false,
    overrideFile: Boolean = false
){
  val outputFilePath:Path = Path.of(outputFile
        .map(_.getPath)
        .getOrElse(inputFile.getPath + ".csv"))
}

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
          .text("Path for output csv file. Default: <input-file>.csv")
          .optional()
          .action((o, c) => c.copy(outputFile = o)),  
        opt[Unit]("overwrite")
          .text("delete and create a new new outfile if one already exists")
          .optional()
          .action((o, c) => c.copy(overrideFile = true)),
        opt[Unit]("no-color")
          .text("do not use colors for output text")
          .optional()
          .action((o, c) => c.copy(noColor = true)),
        help("help").text("print help text"),
        checkConfig{
          case x@CLIOptions(_, _, _, false) => 
            if x.outputFilePath.toFile.exists then failure(s"file ${x.outputFilePath.toString} already exists")
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
    given system: ActorSystem = ActorSystem("main")
    val schemaGen = new SchemaGen

    val schema = schemaGen.generate(file(options.inputFile.getPath))
      .alsoTo(Sink.foreach{ (x:Schema) => 
        val columns = s"${x.paths.size} unique fields".yellow
        val rows = s"${x.rows} records".yellow
        val title = "Generating schema".bold
        print(s"\r$title: found $columns in $rows ")
      })
      .toMat(Sink.last)(Keep.both)
      .mapMaterializedValue(x => x._1.flatMap(_ => x._2))
      .run
      .flatMap { schema => 
        if(schema.rows == 0) throw Error.NoRows
        else if(schema.paths.size == 0) throw Error.NoFields
        else println("DONE".green.bold)
        val csvGen = new CsvGen(schema, Printer.console)
        val stream = csvGen.generateCsv(file(options.inputFile.getPath))
        stream
          .via(byteString)
          .toMat(fileSink(options.outputFilePath.toString, options.overrideFile))(Keep.both)
          .mapMaterializedValue(a => a._1.flatMap(_ => a._2))
          .run()
      }.onComplete {
        case Success(_) => 
          println("DONE".green.bold)
          system.terminate().block
        case Failure(Error.NoRows) =>
          println("FAILED".red.bold)
          println("No json records found in file".red)
          sys.exit(1)
        case Failure(Error.NoFields) => 
          println("FAILED".red.bold)
          println("No fields found in any records".red)
          sys.exit(1)
        case Failure(err) if err.getCause.isInstanceOf[FramingException] =>
          println("FAILED".red.bold)
          println("Invalid JSON encountered")
          sys.exit(1)
        case Failure(err) if err.getCause.isInstanceOf[JsonParseException] =>
          println("FAILED".red.bold)
          println("Invalid JSON encountered")
          sys.exit(1)
        case Failure(err) => 
          println("FAILED".red.bold)
          err.printStackTrace
          sys.exit(1)
      }
  }
}

enum Error extends RuntimeException{
  case NoRows, NoFields
}