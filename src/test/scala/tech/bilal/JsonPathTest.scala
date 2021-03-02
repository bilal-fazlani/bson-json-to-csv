package tech.bilal

import munit.FunSuite
import org.mongodb.scala.bson.{BsonDocument, BsonInt32, BsonString, BsonValue}
import tech.bilal.Extensions.BsonValueExt
import tech.bilal.Node._

class JsonPathTest extends FunSuite {
  private val json: String =
    """{
      |   "id": "1233",
      |   "work": {
      |       "type":"salaried",
      |       "company": "google"
      |   },
      |   "simple-matrix": [[1,2], [3,4]],
      |   "complex-matrix": [[{"id":1},{"id":2}], [{"id":3},{"id":4}]],
      |   "person": {
      |       "name": "bilal",
      |       "age": 100,
      |       "department": {
      |          "id": 3
      |       },
      |       "alias": ["abd", "pqr"],
      |       "addresses": [
      |       {
      |          "city": "mumbai",
      |          "country": "India"
      |       },
      |       {
      |          "city": "NYC",
      |          "country": "US"
      |       }]
      |   }  
      |}""".stripMargin
  private val document = BsonDocument.apply(json)

  implicit class TestDsl(path: JsonPath) {
    def returns(bsonValue: BsonValue): Unit = {
      val value = document.getLeafValue(path)
      assertEquals(value, Some(bsonValue))
    }

    def returnsNone(): Unit =
      assertEquals(document.getLeafValue(path), None)
  }

  implicit class TestDsl2(path: Node) {
    def returns(bsonValue: BsonValue): Unit = {
      val value = document.getLeafValue(path)
      assertEquals(value, Some(bsonValue))
    }

    def returnsNone(): Unit =
      assertEquals(document.getLeafValue(path), None)
  }

  test("can get top level string") {
    "id".n returns BsonString("1233")
  }

  test("can get int from 2nd level nested object") {
    "person" / "age" returns BsonInt32(100)
  }

  test("can get int from 3rd level nested object") {
    "person" / "department" / "id" returns BsonInt32(3)
  }

  test("complex object from document returns None") {
    "work".n.returnsNone()
  }

  test("can get simple value from an array") {
    "person" / "alias" / 1 returns BsonString("pqr")
  }

  test("can get simple value from an object in an array") {
    "person" / "addresses" / 0 / "city" returns BsonString(
      "mumbai"
    )
  }

  test("complex object from array returns None") {
    ("person" / "addresses" / 1).returnsNone()
  }

  test("simple matrix") {
    "simple-matrix" / 1 / 0 returns BsonInt32(3)
  }

  test("complex matrix") {
    "complex-matrix" / 1 / 0 / "id" returns BsonInt32(3)
  }
}
