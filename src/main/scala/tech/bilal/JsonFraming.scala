package tech.bilal

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{Flow, Framing, JsonFraming, Source}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

class JsonFraming(fileType: FileType)(using ExecutionContext) {
  def frame(source: => Source[ByteString, Future[IOResult]]): Source[String, Future[IOResult]] = {
    fileType match {
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

  private val lineMaker: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(
      ByteString("\n"),
      Int.MaxValue,
      allowTruncation = true
    )
}
