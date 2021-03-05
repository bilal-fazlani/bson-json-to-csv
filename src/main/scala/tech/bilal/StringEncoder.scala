package tech.bilal

import org.mongodb.scala.bson.*

trait StringEncoder[A] {
  def encode(input: A): String
}
object StringEncoder {
  extension [T: StringEncoder](t: T) {
    def encodeToString: String = summon[StringEncoder[T]].encode(t)
  }

  given StringEncoder[BsonInt32] = new {
    override def encode(input: BsonInt32): String = input.getValue.toString
  }
  given StringEncoder[BsonString] = new {
    override def encode(input: BsonString): String = input.getValue
  }
  given StringEncoder[BsonBoolean] = new {
    override def encode(input: BsonBoolean): String = input.getValue.toString
  }
  given StringEncoder[BsonDateTime] = new {
    override def encode(input: BsonDateTime): String = input.getValue.toString
  }
  given StringEncoder[BsonObjectId] = new {
    override def encode(input: BsonObjectId): String = input.getValue.toString
  }
  given StringEncoder[BsonInt64] = new {
    override def encode(input: BsonInt64): String = input.getValue.toString
  }
  given StringEncoder[BsonDouble] = new {
    override def encode(input: BsonDouble): String = input.getValue.toString
  }
  given StringEncoder[BsonDecimal128] = new {
    override def encode(input: BsonDecimal128): String = input.getValue.toString
  }
  given StringEncoder[BsonTimestamp] = new {
    override def encode(input: BsonTimestamp): String = input.getValue.toString
  }
}
