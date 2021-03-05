package tech.bilal

import java.nio.file.Path

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Framing, JsonFraming, Sink, Source}
import akka.util.ByteString
import org.mongodb.scala.bson.BsonDocument

import scala.concurrent.Future

trait StreamFlows {
  def file(path: String): Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(Path.of(path))

  val lineMaker: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(
      ByteString("\n"),
      Int.MaxValue,
      allowTruncation = true
    )

  val jsonFrame: Flow[ByteString, String, NotUsed] = JsonFraming
    .objectScanner(Int.MaxValue)
    .map(_.utf8String)

  val bsonConvert: Flow[String, BsonDocument, NotUsed] =
    Flow.fromFunction[String, BsonDocument](BsonDocument.apply)

  val byteString: Flow[CSVRow, ByteString, NotUsed] =
    Flow.fromFunction[CSVRow, ByteString](x => ByteString.apply(x.toString))

  def unique[T]: Flow[T, T, NotUsed] =
    Flow[T].statefulMapConcat(() => {
      var set = Set.empty[T]
      (str: T) => {
        val list =
          if (set.contains(str)) List.empty
          else List(str)
        set += str
        list
      }
    })

  def viaIndex[T](f: => ((T, Long)) => Unit): Flow[T, T, NotUsed] =
    Flow.apply.zipWithIndex
      .alsoTo(Sink.foreach[(T, Long)](f))
      .map(_._1)

  def count[T]: Sink[T, Future[Long]] = Sink.fold[Long, T](0L)((a, _) => a + 1)
}
