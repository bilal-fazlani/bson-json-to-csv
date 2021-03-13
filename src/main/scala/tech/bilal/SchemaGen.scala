package tech.bilal

import akka.stream.Materializer
import akka.stream.scaladsl.*
import akka.util.ByteString
import scala.concurrent.Future
import akka.stream.IOResult
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue}
import Node.*
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

opaque type TotalRows = Int

object TotalRows{
  def apply(v:Int):TotalRows = v
}
extension (v: TotalRows) {
  def toInt:Int = v
}

class SchemaGen(implicit mat: Materializer, ec:ExecutionContext) extends StreamFlows {

  private def countSink[T] = Sink.fold[Int, T](0)((i, _) => i + 1)

  def generate(source: Source[ByteString, Future[IOResult]]) : Source[JsonPath, Future[TotalRows]] =
    source
      .via(lineMaker)
      .dropWhile(!_.utf8String.startsWith("{"))
      .via(jsonFrame)
      .alsoToMat(countSink)(Keep.both)
      .via(bsonConvert)
      .map(docToPaths(_, None))
      .mapConcat(identity)
      .via(unique)
      .mapMaterializedValue(x => x match {
        case (a,b) => a.flatMap(_ => b.map(TotalRows.apply))
      })

  private def docToPaths(
      doc: BsonDocument,
      prev: Option[JsonPath]
  ): Set[JsonPath] =
    getDocKeys(doc).flatMap { key =>
      doc.get(key) match {
        case newDoc: BsonDocument =>
          docToPaths(newDoc, Some(getCurrentPath(prev, Name(key))))
        case newArr: BsonArray =>
          arrayToPaths(newArr, Some(getCurrentPath(prev, Name(key))))
        case _ =>
          Set(getCurrentPath(prev, Name(key)))
      }
    }

  private def getCurrentPath(
      prev: Option[JsonPath],
      currentNode: Node
  ): JsonPath =
    prev match {
      case Some(path) => path / currentNode
      case None       => currentNode
    }

  private def arrayToPaths(
      arr: BsonArray,
      prev: Option[JsonPath]
  ): Set[JsonPath] =
    arr.getValues.asScala.toSet.zipWithIndex.flatMap[JsonPath] { x =>
      x match {
        case (node: BsonDocument, index: Int) =>
          docToPaths(node, Some(getCurrentPath(prev, Index(index))))
        case (node: BsonArray, index: Int) =>
          arrayToPaths(node, Some(getCurrentPath(prev, Index(index))))
        case (_, index: Int) =>
          Set(getCurrentPath(prev, Index(index)))
      }
    }

  private def getDocKeys(doc: BsonDocument): Set[String] =
    doc
      .keySet()
      .asScala
      .toSet
}
