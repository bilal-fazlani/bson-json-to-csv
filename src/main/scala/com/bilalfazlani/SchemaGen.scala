package com.bilalfazlani

import akka.stream.Materializer
import akka.stream.scaladsl.*
import akka.util.ByteString
import akka.stream.IOResult
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue}
import scala.concurrent.{Future, ExecutionContext}
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions
import com.bilalfazlani.Node.*

case class Schema(paths: Set[JsonPath] = Set.empty, rows: Long = 0) {
  infix def +(morePaths: Set[JsonPath]): Schema =
    Schema(paths ++ morePaths, rows + 1)
}

class SchemaGen(jsonFraming: JsonFraming) extends StreamFlows {

  def generate(
      source: => Source[ByteString, Future[IOResult]]
  ): Source[Schema, Future[IOResult]] =
    jsonFraming
      .frame(source)
      .via(bsonConvert)
      .map(docToPaths(_, None))
      .scan(Schema())(_ + _)

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
