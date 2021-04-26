package tech.bilal

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{Flow, Framing, JsonFraming, Source}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

class JsonFraming(fileTypeFinder: FileTypeFinder)(using ExecutionContext) {
  def flow(source: => Source[ByteString, Future[IOResult]]): Source[String, Future[IOResult]] = {
    Source.futureSource(
      fileTypeFinder.find(source).map(fileType => framed(source, fileType))
    ).mapMaterializedValue(_.flatten)
  }

  private val lineMaker: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(
      ByteString("\n"),
      Int.MaxValue,
      allowTruncation = true
    )

  private def framed(source: => Source[ByteString, Future[IOResult]], ft: FileType): Source[String, Future[IOResult]] = {
    ft match {
      case FileType.Array =>
        source
          .via(lineMaker)
          .dropWhile(!_.utf8String.startsWith("["))
          .via(JsonReader.select("$[*]"))
          .map(_.utf8String)
      case FileType.ObjectStream =>
        source
          .via(lineMaker)
          .dropWhile(!_.utf8String.startsWith("{"))
          .via(akka.stream.scaladsl.JsonFraming.objectScanner(Int.MaxValue))
          .map(_.utf8String)
    }
  }
}
