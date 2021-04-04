package tech.bilal

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import tech.bilal.Extensions.*
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.IOResult

class SchemaGenTest extends CustomFixtures {
  actorSystemFixture.test("can generate schema") { actorSystem =>
    implicit val mat: Materializer = Materializer(actorSystem)
    given ExecutionContext = actorSystem.dispatcher
    val schemaGen = new SchemaGen

    val singleSource: Source[ByteString, Future[IOResult]] = Source
      .single(
        ByteString(
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
          |""".stripMargin
        )
      )
      .mapMaterializedValue(_ => Future.successful(IOResult(0)))

    val schema: Seq[JsonPath] =
      schemaGen.generate(singleSource).runWith(Sink.last).block().paths.toList

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

  actorSystemFixture.test("array support") { actorSystem =>
    implicit val mat: Materializer = Materializer(actorSystem)
    given ExecutionContext = actorSystem.dispatcher
    val schemaGen = new SchemaGen

    val singleSource = Source
      .single(
        ByteString(
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
          |""".stripMargin
        )
      )
      .mapMaterializedValue(_ => Future.successful(IOResult(0)))

    //   "simple-matrix": [[1,2], [3,4]],
    //   "complex-matrix": [[{"id":1},{"id":2}], [{"id":3},{"id":4}]],

    val schema: Seq[JsonPath] =
      schemaGen.generate(singleSource).runWith(Sink.last).block().paths.toList

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

  actorSystemFixture.test("matrix support") { actorSystem =>
    implicit val mat: Materializer = Materializer(actorSystem)
    given ExecutionContext = actorSystem.dispatcher
    val schemaGen = new SchemaGen

    val singleSource = Source
      .single(
        ByteString(
          """
          |{
          |   "simple-matrix": [[1,2], [3,4]],
          |   "complex-matrix": [[{"id":1},{"id":2}], [{"id":3},{"id":4}]]
          |}
          |""".stripMargin
        )
      )
      .mapMaterializedValue(_ => Future.successful(IOResult(0)))

    val schema: Seq[JsonPath] =
      schemaGen.generate(singleSource).runWith(Sink.last).block().paths.toList

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

  actorSystemFixture.test("conflicting data types") { actorSystem =>
    implicit val mat: Materializer = Materializer(actorSystem)
    given ExecutionContext = actorSystem.dispatcher
    val schemaGen = new SchemaGen

    val singleSource = Source
      .single(
        ByteString(
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
          |""".stripMargin
        )
      )
      .mapMaterializedValue(_ => Future.successful(IOResult(0)))

    val schema: Seq[JsonPath] =
      schemaGen.generate(singleSource).runWith(Sink.last).block().paths.toList

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

  actorSystemFixture.test("empty records") { actorSystem =>
    implicit val mat: Materializer = Materializer(actorSystem)
    given ExecutionContext = actorSystem.dispatcher
    val schemaGen = new SchemaGen

    val singleSource = Source.single(
      ByteString(
        """
          |{
          |}
          |{
          |}
          |""".stripMargin
      )
    ).mapMaterializedValue(_ => Future.successful(IOResult(0)))

    val schema: Schema =
      schemaGen.generate(singleSource).runWith(Sink.last).block()

    val expected: Schema = Schema(Set.empty, 2)

    assertEquals(schema,expected)
  }
}
