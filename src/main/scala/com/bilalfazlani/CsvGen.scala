package com.bilalfazlani

import com.bilalfazlani.rainbowcli.*
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.{Keep, Source}
import org.apache.pekko.util.ByteString
import org.mongodb.scala.bson.*
import StringEncoder.*
import scala.concurrent.Future

class CsvGen(schema: Schema, printer: Printer, jsonFraming: JsonFraming)(using
    ColorContext
) extends StreamFlows {

  import printer.*

  def generateCsv(
      source: => Source[ByteString, Future[IOResult]]
  ): Source[CSVRow, Future[IOResult]] = {
    val params = schema.paths.toList
    val contents: Source[CSVRow, Future[IOResult]] =
      jsonFraming
        .frame(source)
        .via(bsonConvert)
        .map(x => getCsvRow(x, params))
        .via(viaIndex { x =>
          val pValue = (((x._2 + 1d) / schema.rows.toDouble) * 100).toInt
          val percentage = s"$pValue%".yellow
          val title = "Generating csv".bold
          print(s"\r$title: $percentage ")
        })

    val header: Source[CSVRow, NotUsed] =
      Source.single(CSVRow(params.map(_.toString)))

    header
      .concatMat(contents)(Keep.right)
  }

  private def getCsvRow(
      bsonDocument: BsonDocument,
      params: List[JsonPath]
  ): CSVRow =
    CSVRow(
      params
        .map(bsonDocument.getLeafValue)
        .map(getStringRepr)
    )

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
