package com.bilalfazlani

import org.bson.BsonInvalidOperationException
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue}
import Node.{Index, Name}

import scala.annotation.tailrec
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

extension [T](f: Future[T]) {
  def block(duration: FiniteDuration): T =
    Await.result(f, duration)

  def block: T =
    Await.result(f, 5.seconds)
}

extension (doc: BsonValue) {

  def getLeafValue(path: JsonPath): Option[BsonValue] =
    getValue(path, doc, 0) match {
      case Some(_: BsonDocument) => None
      case Some(_: BsonArray)    => None
      case None                  => None
      case s @ Some(_)           => s
    }

  private def toOptionSafe[T](v: => T): Option[T] =
    try {
      Option(v)
    } catch {
      case NonFatal(_: NullPointerException) => None
      case NonFatal(x: BsonInvalidOperationException) => None
    }

  @tailrec
  private def getValue(
      path: JsonPath,
      currentValue: BsonValue,
      currentIndex: Int
  ): Option[BsonValue] =
    path.seq(currentIndex) match {
      case Name(name) =>
        if (currentValue.isNull) None
        else
          toOptionSafe(currentValue.asDocument()).flatMap(x =>
            toOptionSafe(x.get(name))
          ) match {
            case s @ Some(value) =>
              if (currentIndex == path.seq.length - 1) s
              else getValue(path, value, currentIndex + 1)
            case None => None
          }
      case Index(index) =>
        toOptionSafe(currentValue.asArray()).flatMap(x =>
          toOptionSafe(x.get(index))
        ) match {
          case s @ Some(bsonValue) =>
            if (currentIndex == path.seq.length - 1) s
            else getValue(path, bsonValue, currentIndex + 1)
          case None => None
        }
    }
}
