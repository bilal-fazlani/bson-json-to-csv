package tech.bilal

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue}
import Node.*
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

class SchemaGen(implicit mat: Materializer) extends StreamFlows {

  def generate[T](source: Source[ByteString, T]): Source[JsonPath, T] =
    source
      .via(lineMaker)
      .dropWhile(!_.utf8String.startsWith("{"))
      .via(jsonFrame)
      .via(bsonConvert)
      .map(docToPaths(_, None))
      .mapConcat(identity)
      .via(unique)

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
