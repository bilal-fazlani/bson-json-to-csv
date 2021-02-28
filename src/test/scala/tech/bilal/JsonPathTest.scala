package tech.bilal

import munit.FunSuite
import org.mongodb.scala.bson.{BsonDocument, BsonInt32, BsonString, BsonValue}
import tech.bilal.Extensions.BsonValueExt
import tech.bilal.Node.StringNodeDsl
import Node._

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
    "person".n --> "age".n returns BsonInt32(100)
  }

  test("can get int from 3rd level nested object") {
    "person".n --> "department".n --> "id".n returns BsonInt32(3)
  }

  test("complex object from document returns None") {
    "work".n.returnsNone()
  }

  test("can get simple value from an array") {
    "person".n --> "alias".n --> 1.n returns BsonString("pqr")
  }

  test("can get simple value from an object in an array") {
    "person".n --> "addresses".n --> 0.n --> "city".n returns BsonString(
      "mumbai"
    )
  }

  test("complex object from array returns None") {
    ("person".n --> "addresses".n --> 1.n).returnsNone()
  }

  test("simple matrix") {
    "simple-matrix".n --> 1.n --> 0.n returns BsonInt32(3)
  }

  test("complex matrix") {
    "complex-matrix".n --> 1.n --> 0.n --> "id".n returns BsonInt32(3)
  }
}
