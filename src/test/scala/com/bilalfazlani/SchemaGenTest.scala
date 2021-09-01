package com.bilalfazlani

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.IOResult

class SchemaGenTest extends CustomFixtures {
  `can generate schema`(
    """
                          |{
                          |   "id": "1233",
                          |   "person": {
                          |       "name": "bilal",
                          |       "address": {
                          |          "city": "mumbai",
                          |          "country": "India"
                          |       }
                          |   }
                          |}
                          |{
                          |   "id": "asd12312",
                          |   "company":"google",
                          |   "person": {
                          |       "name": "john"
                          |       },
                          |   "employee": {
                          |       "uniqueId": "adsad12",
                          |       "address": {
                          |          "line1": "mumbai"
                          |       }
                          |   }
                          |}
                          |""".stripMargin,
    "stream"
  )
  `can generate schema`(
    """
                          |[{
                          |   "id": "1233",
                          |   "person": {
                          |       "name": "bilal",
                          |       "address": {
                          |          "city": "mumbai",
                          |          "country": "India"
                          |       }
                          |   }
                          |},
                          |{
                          |   "id": "asd12312",
                          |   "company":"google",
                          |   "person": {
                          |       "name": "john"
                          |       },
                          |   "employee": {
                          |       "uniqueId": "adsad12",
                          |       "address": {
                          |          "line1": "mumbai"
                          |       }
                          |   }
                          |}]
                          |""".stripMargin,
    "array"
  )

  def `can generate schema`(source: String, name: String) =
    actorSystemFixture.test(s"can generate schema: $name") { actorSystem =>
      implicit val mat: Materializer = Materializer(actorSystem)
      given ExecutionContext = actorSystem.dispatcher
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)

      val singleSource: Source[ByteString, Future[IOResult]] = Source
        .single(
          ByteString(source)
        )
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))

      val schema: Seq[JsonPath] =
        schemaGen.generate(singleSource).runWith(Sink.last).block.paths.toList

      val expected: Seq[String] = Seq(
        ".id",
        ".person.name",
        ".person.address.city",
        ".person.address.country",
        ".company",
        ".employee.uniqueId",
        ".employee.address.line1"
      )

      assertEquals(
        schema.map(_.toString).sorted,
        expected.sorted
      )
    }

  `array support`(
    """
                    |{
                    |   "id": "1233",
                    |   "person": {
                    |       "name": "bilal",
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
                    |}
                    |{
                    |   "id": "asd12312",
                    |   "company":"google",
                    |   "person": {
                    |       "name": "john"
                    |       },
                    |   "employee": {
                    |       "uniqueId": "adsad12",
                    |       "address": {
                    |          "line1": "mumbai"
                    |       }
                    |   }
                    |}
                    |""".stripMargin,
    "stream"
  )
  `array support`(
    """
                    |[{
                    |   "id": "1233",
                    |   "person": {
                    |       "name": "bilal",
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
                    |},
                    |{
                    |   "id": "asd12312",
                    |   "company":"google",
                    |   "person": {
                    |       "name": "john"
                    |       },
                    |   "employee": {
                    |       "uniqueId": "adsad12",
                    |       "address": {
                    |          "line1": "mumbai"
                    |       }
                    |   }
                    |}]
                    |""".stripMargin,
    "array"
  )

  def `array support`(source: String, name: String) =
    actorSystemFixture.test(s"array support: $name") { actorSystem =>
      implicit val mat: Materializer = Materializer(actorSystem)
      given ExecutionContext = actorSystem.dispatcher
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)

      val singleSource = Source
        .single(ByteString(source))
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))

      //   "simple-matrix": [[1,2], [3,4]],
      //   "complex-matrix": [[{"id":1},{"id":2}], [{"id":3},{"id":4}]],

      val schema: Seq[JsonPath] =
        schemaGen.generate(singleSource).runWith(Sink.last).block.paths.toList

      val expected: Seq[String] = Seq(
        ".id",
        ".person.name",
        ".person.alias[0]",
        ".person.alias[1]",
        ".person.addresses[0].city",
        ".person.addresses[0].country",
        ".person.addresses[1].city",
        ".person.addresses[1].country",
        ".company",
        ".employee.uniqueId",
        ".employee.address.line1"
      )

      assertEquals(
        schema.map(_.toString).sorted,
        expected.sorted
      )
    }

  `matrix support`(
    """
                     |{
                     |   "simple-matrix": [[1,2], [3,4]],
                     |   "complex-matrix": [[{"id":1},{"id":2}], [{"id":3},{"id":4}]]
                     |}
                     |""".stripMargin,
    "stream"
  )
  `matrix support`(
    """
                     |[{
                     |   "simple-matrix": [[1,2], [3,4]],
                     |   "complex-matrix": [[{"id":1},{"id":2}], [{"id":3},{"id":4}]]
                     |}]
                     |""".stripMargin,
    "array"
  )

  def `matrix support`(source: String, name: String) =
    actorSystemFixture.test(s"matrix support: $name") { actorSystem =>
      implicit val mat: Materializer = Materializer(actorSystem)
      given ExecutionContext = actorSystem.dispatcher
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)

      val singleSource = Source
        .single(ByteString(source))
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))

      val schema: Seq[JsonPath] =
        schemaGen.generate(singleSource).runWith(Sink.last).block.paths.toList

      val expected: Seq[String] = Seq(
        ".simple-matrix[0][0]",
        ".simple-matrix[0][1]",
        ".simple-matrix[1][0]",
        ".simple-matrix[1][1]",
        ".complex-matrix[0][0].id",
        ".complex-matrix[0][1].id",
        ".complex-matrix[1][0].id",
        ".complex-matrix[1][1].id"
      )

      assertEquals(
        schema.map(_.toString).sorted,
        expected.sorted
      )
    }

  `conflicting data types`(
    """
                             |{
                             |   "person": {
                             |     "name": "bilal"
                             |   }
                             |}
                             |{
                             |   "person": {
                             |     "name": {
                             |       "first": "bilal",
                             |       "last": "fazlani"
                             |     }
                             |   }
                             |}
                             |{
                             |   "person": {
                             |     "name": 3
                             |   }
                             |}
                             |""".stripMargin,
    "stream"
  )
  `conflicting data types`(
    """
                             |[{
                             |   "person": {
                             |     "name": "bilal"
                             |   }
                             |},
                             |{
                             |   "person": {
                             |     "name": {
                             |       "first": "bilal",
                             |       "last": "fazlani"
                             |     }
                             |   }
                             |},
                             |{
                             |   "person": {
                             |     "name": 3
                             |   }
                             |}]
                             |""".stripMargin,
    "array"
  )

  def `conflicting data types`(source: String, name: String) =
    actorSystemFixture.test(s"conflicting data types: $name") { actorSystem =>
      implicit val mat: Materializer = Materializer(actorSystem)
      given ExecutionContext = actorSystem.dispatcher
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)

      val singleSource = Source
        .single(ByteString(source))
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))

      val schema: Seq[JsonPath] =
        schemaGen.generate(singleSource).runWith(Sink.last).block.paths.toList

      val expected: Seq[String] = Seq(
        ".person.name",
        ".person.name.first",
        ".person.name.last"
      )

      assertEquals(
        schema.map(_.toString).sorted,
        expected.sorted
      )
    }

  `empty records`(
    """
                    |{
                    |}
                    |{
                    |}
                    |""".stripMargin,
    "stream"
  )
  `empty records`(
    """
                    |[{
                    |},
                    |{
                    |}]
                    |""".stripMargin,
    "array"
  )

  def `empty records`(source: String, name: String) =
    actorSystemFixture.test(s"empty records: $name") { actorSystem =>
      implicit val mat: Materializer = Materializer(actorSystem)
      given ExecutionContext = actorSystem.dispatcher
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)

      val singleSource = Source
        .single(ByteString(source))
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))

      val schema: Schema =
        schemaGen.generate(singleSource).runWith(Sink.last).block

      val expected: Schema = Schema(Set.empty, 2)

      assertEquals(schema, expected)
    }

  `invalid text`(
    """asdasd...dsaasdas
                   |asdasdasdasd a.asd
                   |{
                   |"name":"john"
                   |}""".stripMargin,
    "stream"
  )
  `invalid text`(
    """asdasd...dsaasdas
                   |asdasdasdasd a.asd
                   |[{
                   |"name":"john"
                   |}]""".stripMargin,
    "array"
  )

  def `invalid text`(source: String, name: String) =
    actorSystemFixture.test(s"invalid text before json: $name") { actorSystem =>
      implicit val mat: Materializer = Materializer(actorSystem)
      given ExecutionContext = actorSystem.dispatcher
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)

      val singleSource: Source[ByteString, Future[IOResult]] = Source
        .single(
          ByteString(source)
        )
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))

      val schema: Seq[JsonPath] =
        schemaGen.generate(singleSource).runWith(Sink.last).block.paths.toList

      val expected: Seq[String] = Seq(".name")

      assertEquals(
        schema.map(_.toString).sorted,
        expected.sorted
      )
    }
}
