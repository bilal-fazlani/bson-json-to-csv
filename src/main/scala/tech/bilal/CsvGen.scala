package tech.bilal

import com.bilalfazlani.scala.rainbow.*
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.mongodb.scala.bson.*
import tech.bilal.*
import tech.bilal.StringEncoder.*
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CsvGen(schema: Schema, printer: Printer)(using ColorContext) extends StreamFlows {

  import printer.*
  
  def generateCsv(source: => Source[ByteString, Future[IOResult]]): Source[CSVRow, Future[IOResult]] = {
    val params = schema.paths.toList
    val contents: Source[CSVRow, Future[IOResult]] =
      source
        .via(lineMaker)
        .dropWhile(!_.utf8String.startsWith("{"))
        .via(jsonFrame)
        .via(bsonConvert)
        .map(x => getCsvRow(x, params))
        .via(viaIndex{x => 
          val pValue = (((x._2 + 1D) / schema.rows.toDouble) * 100).toInt
          val percentage = s"$pValue%".yellow
          val title = "Generating csv".bold
          print(s"\r$title: $percentage ")
        })

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
      case Some(null) | None       => ""
      case Some(x: BsonString)     => x.encodeToString
      case Some(x: BsonBoolean)    => x.encodeToString
      case Some(x: BsonDateTime)   => x.encodeToString
      case Some(x: BsonObjectId)   => x.encodeToString
      case Some(x: BsonInt32)      => x.encodeToString
      case Some(x: BsonInt64)      => x.encodeToString
      case Some(x: BsonDouble)     => x.encodeToString
      case Some(x: BsonDecimal128) => x.encodeToString
      case Some(x: BsonTimestamp)  => x.encodeToString
      case Some(_: BsonNull)       => ""
    })
    .replaceAll(",", "")
    .replaceAll("\n", "")
}