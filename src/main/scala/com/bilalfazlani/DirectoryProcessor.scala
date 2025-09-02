package com.bilalfazlani

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.concurrent.{ExecutionContext, Future}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import com.bilalfazlani.rainbowcli.*

class DirectoryProcessor(using ActorSystem, ExecutionContext, ColorContext)
    extends StreamFlows {

  def processDirectory(options: CLIOptions): Future[Unit] = {
    val inputPath = options.inputPath.toPath

    // Discover files
    val files = DirectoryProcessor.discoverFiles(
      inputPath,
      options.filePattern,
      options.recursive
    )

    if (files.isEmpty) {
      println(
        s"No files found matching pattern '${options.filePattern}' in $inputPath".yellow
      )
      return Future.successful(())
    }

    println(
      s"Discovering files: found ${files.size} files matching ${options.filePattern}".bold
    )

    // Determine if filename column should be included
    val includeFilename = options.addFilenameColumn.getOrElse {
      files.size > 1 || options.inputPath.isDirectory
    }

    // Build unified schema
    buildUnifiedSchema(files).flatMap { baseSchema =>
      val schemaWithFilename = SchemaWithFilename(
        baseSchema,
        includeFilename,
        options.filenameColumnName
      )

      val outputFile = CLIOptions.getOutputFile(options)
      println(s"Output: ".bold + outputFile.toPath.toString)
      println()

      // Create file sources with filename context
      val filesWithSources = files.map { path =>
        (path.getFileName.toString, file(path))
      }

      val csvGen = new DirectoryCsvGen(
        schemaWithFilename,
        Printer.console,
        new JsonFraming()
      )
      csvGen
        .generateCsv(filesWithSources)
        .via(byteString)
        .runWith(fileSink(outputFile.toPath, options.overrideFile))
        .map { _ =>
          println("DONE".green.bold)
          ()
        }
    }
  }

  private def buildUnifiedSchema(filePaths: List[Path]): Future[Schema] = {
    filePaths.zipWithIndex
      .foldLeft(Future.successful(Schema())) {
        case (schemaFuture, (filePath, index)) =>
          schemaFuture.flatMap { currentSchema =>
            val jsonFraming = new JsonFraming
            val schemaGen = new SchemaGen(jsonFraming)

            schemaGen
              .generate(file(filePath))
              .alsoTo(Sink.foreach { schema =>
                val columns = s"${schema.paths.size} unique fields".yellow
                val rows = s"${schema.rows} records".yellow
                val fileProgress = s"(file ${index + 1}/${filePaths.size})".cyan
                val title = "Generating schema".bold
                print(s"\r$title: found $columns in $rows $fileProgress ")
              })
              .runWith(Sink.last)
              .map { fileSchema =>
                Schema(
                  paths = currentSchema.paths ++ fileSchema.paths,
                  rows = currentSchema.rows + fileSchema.rows
                )
              }
              .recover { case ex =>
                println(
                  s"\nWarning: Skipping ${filePath.getFileName} - ${ex.getMessage}".yellow
                )
                currentSchema
              }
          }
      }
      .map { finalSchema =>
        println("DONE".green.bold)
        finalSchema
      }
  }
}

object DirectoryProcessor {

  /** Discover files in a directory matching the given pattern
    */
  def discoverFiles(
      directory: Path,
      pattern: String,
      recursive: Boolean
  ): List[Path] = {
    if (!Files.isDirectory(directory)) {
      throw new IllegalArgumentException(s"$directory is not a directory")
    }

    val globPattern = convertToGlobPattern(pattern)
    val pathMatcher =
      directory.getFileSystem.getPathMatcher(s"glob:$globPattern")

    val stream = if (recursive) {
      Files.walk(directory)
    } else {
      Files.list(directory)
    }

    try {
      stream
        .filter(Files.isRegularFile(_))
        .filter(path => pathMatcher.matches(path.getFileName))
        .collect(java.util.stream.Collectors.toList[Path])
        .asScala
        .toList
        .sorted
    } finally {
      stream.close()
    }
  }

  /** Convert user-friendly patterns to glob patterns Examples:
    *   - "*.json" -> "*.json"
    *   - "*.{json,bson}" -> "*.{json,bson}"
    *   - "data_*.json" -> "data_*.json"
    */
  private def convertToGlobPattern(pattern: String): String = {
    // Pattern is already in glob format, just return it
    pattern
  }

  /** Check if a file matches common JSON/BSON extensions
    */
  def isJsonOrBsonFile(path: Path): Boolean = {
    val fileName = path.getFileName.toString.toLowerCase
    fileName.endsWith(".json") ||
    fileName.endsWith(".bson") ||
    fileName.endsWith(".jsonl") ||
    fileName.endsWith(".ndjson")
  }
}
