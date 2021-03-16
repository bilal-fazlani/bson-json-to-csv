package tech.bilal

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.github.tototoshi.csv.{CSVFormat, CSVReader, DefaultCSVFormat}
import munit.FunSuite
import org.junit.Assert
import org.mongodb.scala.bson.{BsonDocument, BsonInt32, BsonString, BsonValue}
import tech.bilal.Extensions.*
import tech.bilal.Node
import tech.bilal.CustomFixtures
import com.bilalfazlani.scala.rainbow.ColorContext

import java.io.{Reader, StringReader}
import scala.concurrent.{Future, ExecutionContext}

class CsvGenTest extends CustomFixtures {
  private val json: String ="""
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

  val fakePrinter = new Printer {
    def println(str: String): Unit = ()
    def print(str: String): Unit = ()
  }
  
  actorSystemFixture.test("can generate CSV"){ system =>
    given ActorSystem = system
    given ExecutionContext = system.dispatcher
    given ColorContext = ColorContext(false)
    val csvGen = new CsvGen(new SchemaGen(), fakePrinter)
    val source = Source.single(ByteString(json)).mapMaterializedValue(_ => Future.successful(IOResult(0)))
    val obtained: String = csvGen.generateCsv(source)
      .runWith(Sink.seq).block()
      .map(_.toString)
      .mkString("")

    given CSVFormat = new DefaultCSVFormat{}
    val reader:Reader = new StringReader(obtained)
    val csv: List[Map[String, String]] = CSVReader.open(reader).allWithHeaders()
    
    assertEquals(csv(0), Map(
      ".name" -> "john",
      ".location.city" -> "mumbai",
      ".location.country" -> "india",
      ".location" -> "",
      ".tags[0]" -> "",
      ".tags[1]" -> "",
      ".tags[2]" -> ""
    ))

    assertEquals(csv(1), Map(
      ".name" -> "jane",
      ".location.city" -> "",
      ".location.country" -> "",
      ".location" -> "delhi",
      ".tags[0]" -> "scala",
      ".tags[1]" -> "java",
      ".tags[2]" -> "big data"
    ))
  }
}
