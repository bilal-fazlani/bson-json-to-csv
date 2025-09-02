package com.bilalfazlani

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString
import com.github.tototoshi.csv.{CSVFormat, CSVReader, DefaultCSVFormat}
import munit.FunSuite
import org.junit.Assert
import org.mongodb.scala.bson.{BsonDocument, BsonInt32, BsonString, BsonValue}
import com.bilalfazlani.rainbowcli.ColorContext

import java.io.{Reader, StringReader}
import scala.concurrent.{ExecutionContext, Future}

class CsvGenTest extends CustomFixtures {
  private val streamingJson: String = """
{
    "name": "john",
    "location": {
        "city": "mumbai",
        "country": "india"
    }
}
{
    "name": "jane",
    "location": "delhi",
    "tags": ["scala", "java", "big data"]
}""".stripMargin

  private val jsonArray =
    """
      |[{
      |    "name": "john",
      |    "location": {
      |        "city": "mumbai",
      |        "country": "india"
      |    }
      |},
      |{
      |    "name": "jane",
      |    "location": "delhi",
      |    "tags": ["scala", "java", "big data"]
      |}]""".stripMargin

  val fakePrinter = new Printer {
    def println(str: String): Unit = ()
    def print(str: String): Unit = ()
  }

  def test(data: String, name: String) =
    actorSystemFixture.test(s"can generate CSV: $name") { system =>
      given ActorSystem = system
      given ExecutionContext = system.dispatcher
      given ColorContext = ColorContext(false)
      val jsonFraming = new JsonFraming
      val schemaGen = new SchemaGen(jsonFraming)
      val source = Source
        .single(ByteString(data))
        .mapMaterializedValue(_ => Future.successful(IOResult(0)))
      schemaGen
        .generate(source)
        .runWith(Sink.last)
        .map { schema =>
          val csvGen = new CsvGen(schema, fakePrinter, jsonFraming)
          csvGen
            .generateCsv(source)
            .runWith(Sink.seq)
            .map(_.map(_.toString))
            .map(_.mkString)
            .map { obtained =>
              given CSVFormat = new DefaultCSVFormat {}
              val reader: Reader = new StringReader(obtained)
              val csv: List[Map[String, String]] =
                CSVReader.open(reader).allWithHeaders()

              assertEquals(
                csv(0),
                Map(
                  ".name" -> "john",
                  ".location.city" -> "mumbai",
                  ".location.country" -> "india",
                  ".location" -> "",
                  ".tags[0]" -> "",
                  ".tags[1]" -> "",
                  ".tags[2]" -> ""
                )
              )

              assertEquals(
                csv(1),
                Map(
                  ".name" -> "jane",
                  ".location.city" -> "",
                  ".location.country" -> "",
                  ".location" -> "delhi",
                  ".tags[0]" -> "scala",
                  ".tags[1]" -> "java",
                  ".tags[2]" -> "big data"
                )
              )
            }
        }
    }

  test(streamingJson, "stream")
  test(jsonArray, "array")
}
