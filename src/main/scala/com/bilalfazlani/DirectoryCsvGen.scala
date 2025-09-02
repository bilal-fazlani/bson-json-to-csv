package com.bilalfazlani

import com.bilalfazlani.rainbowcli.*
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mongodb.scala.bson.*
import StringEncoder.*
import scala.concurrent.Future

class DirectoryCsvGen(
    schemaWithFilename: SchemaWithFilename,
    printer: Printer,
    jsonFraming: JsonFraming
)(using ColorContext)
    extends StreamFlows {

  import printer.*

  def generateCsv(
      filesWithSources: List[(String, Source[ByteString, Future[IOResult]])]
  ): Source[CSVRow, NotUsed] = {

    // Header with optional filename column
    val header: Source[CSVRow, NotUsed] =
      Source.single(CSVRow(schemaWithFilename.allColumns))

    // Process each file with filename context
    val contents: Source[CSVRow, NotUsed] =
      Source(filesWithSources).zipWithIndex
        .flatMapConcat { case ((filename, fileSource), fileIndex) =>
          jsonFraming
            .frame(fileSource)
            .via(bsonConvert)
            .zipWithIndex
            .map { case (doc, recordIndex) =>
              val totalRecords = schemaWithFilename.totalRows
              val progress = if (totalRecords > 0) {
                val globalIndex =
                  recordIndex + (fileIndex * 1000) // Approximate
                val pValue =
                  ((globalIndex.toDouble / totalRecords.toDouble) * 100).toInt
                    .min(100)
                s"$pValue%".yellow
              } else "processing".yellow

              val title = "Generating csv".bold
              print(s"\r$title: $progress ")

              getCsvRowWithFilename(doc, filename, schemaWithFilename)
            }
        }

    header.concat(contents)
  }

  private def getCsvRowWithFilename(
      bsonDocument: BsonDocument,
      filename: String,
      schemaWithFilename: SchemaWithFilename
  ): CSVRow = {
    val jsonColumns = schemaWithFilename.schema.paths.toList
      .map(bsonDocument.getLeafValue)
      .map(getStringRepr)

    val allColumns = if (schemaWithFilename.includeFilename) {
      filename :: jsonColumns
    } else {
      jsonColumns
    }

    CSVRow(allColumns)
  }

  private def getStringRepr(bsonValueMaybe: Option[BsonValue]): String =
    (bsonValueMaybe match {
      case Some(null) | None       => ""
      case Some(x: BsonString)     => x.encodeToString
      case Some(x: BsonBoolean)    => x.encodeToString
      case Some(x: BsonDateTime)   => x.encodeToString
      case Some(x: BsonObjectId)   => x.encodeToString
      case Some(x: BsonInt32)      => x.encodeToString
      case Some(x: BsonInt64)      => x.encodeToString
      case Some(x: BsonDouble)     => x.encodeToString
      case Some(x: BsonUndefined)  => x.encodeToString
      case Some(x: BsonDecimal128) => x.encodeToString
      case Some(x: BsonTimestamp)  => x.encodeToString
      case Some(_: BsonNull)       => ""
      case Some(x)                 => x.toString
    })
      .replaceAll(",", "")
      .replaceAll("\n", "")
      .replaceAll("\r", "")
}
