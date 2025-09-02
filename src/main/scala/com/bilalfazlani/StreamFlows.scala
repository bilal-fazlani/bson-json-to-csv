package com.bilalfazlani

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.{
  FileIO,
  Flow,
  Framing,
  JsonFraming,
  Sink,
  Source
}
import org.apache.pekko.util.ByteString
import org.mongodb.scala.bson.BsonDocument
import com.bilalfazlani.CSVRow
import java.nio.file.{Path, StandardOpenOption}
import scala.concurrent.Future

trait StreamFlows {
  def file(path: Path): Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(path)

  val bsonConvert: Flow[String, BsonDocument, NotUsed] =
    Flow.fromFunction[String, BsonDocument](BsonDocument.apply)

  val byteString: Flow[CSVRow, ByteString, NotUsed] =
    Flow.fromFunction[CSVRow, ByteString](x => ByteString(x.toString))

  def fileSink(path: Path, overwrite: Boolean) = FileIO.toPath(
    path,
    Set(
      if overwrite then StandardOpenOption.CREATE
      else StandardOpenOption.CREATE_NEW,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING
    ),
    0
  )

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
