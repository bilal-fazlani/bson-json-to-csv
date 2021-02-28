package tech.bilal

import org.mongodb.scala.bson._

trait StringEncoder[A] {
  def encode(input: A): String
}
object StringEncoder {
  implicit class Extension[T: StringEncoder](t: T) {
    def encodeToString: String = implicitly[StringEncoder[T]].encode(t)
  }

  implicit object Int32Enc extends StringEncoder[BsonInt32] {
    override def encode(input: BsonInt32): String = input.getValue.toString
  }
  implicit object BsonStringEnc extends StringEncoder[BsonString] {
    override def encode(input: BsonString): String = input.getValue
  }
  implicit object BsonBooleanEnc extends StringEncoder[BsonBoolean] {
    override def encode(input: BsonBoolean): String = input.getValue.toString
  }
  implicit object BsonDateTimeEnc extends StringEncoder[BsonDateTime] {
    override def encode(input: BsonDateTime): String = input.getValue.toString
  }
  implicit object BsonObjectIdEnc extends StringEncoder[BsonObjectId] {
    override def encode(input: BsonObjectId): String = input.getValue.toString
  }
  implicit object BsonInt64Enc extends StringEncoder[BsonInt64] {
    override def encode(input: BsonInt64): String = input.getValue.toString
  }
  implicit object BsonDoubleEnc extends StringEncoder[BsonDouble] {
    override def encode(input: BsonDouble): String = input.getValue.toString
  }
  implicit object BsonDecimal128Enc extends StringEncoder[BsonDecimal128] {
    override def encode(input: BsonDecimal128): String = input.getValue.toString
  }
  implicit object BsonTimestampEnc extends StringEncoder[BsonTimestamp] {
    override def encode(input: BsonTimestamp): String = input.getValue.toString
  }
}
