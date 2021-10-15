package com.bilalfazlani

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{Flow, Framing, JsonFraming, Keep, Sink, Source}
import akka.util.ByteString
import scala.concurrent.Future
import scala.util.control.NonFatal

extension [Mat](flow: Flow[ByteString, ByteString, Mat])
  def dropUntil(
      separator: ByteString,
      inclusive: Boolean
  ): Flow[ByteString, ByteString, Mat] =
    separate(separator, inclusive).drop(1)

  private def separate(separator: ByteString, inclusive: Boolean) =
    flow
      .statefulMapConcat(() => {
        var total: ByteString = ByteString.emptyByteString
        var last: ByteString = ByteString.emptyByteString
        var inHeader = true
        current => {
          if (inHeader) {
            val recent = last ++ current
            val batchHasSeparator =
              recent.utf8String.contains(separator.utf8String)
            total = total ++ current
            if (!batchHasSeparator) {
              last = current
              List.empty[ByteString]
            } else {
              last = current
              inHeader = false
              val totalUtf = total.utf8String
              val sepIndex = totalUtf.indexOf(separator.utf8String)
              val bodyBeginning = total.utf8String.substring(
                sepIndex + (if inclusive then 0
                            else separator.utf8String.length),
                totalUtf.length
              )
              List[ByteString](
                ByteString.fromString(total.utf8String.substring(0, sepIndex)),
                ByteString.fromString(bodyBeginning)
              )
            }
          } else List(current)
        }
      })

class JsonFraming {
  def frame(
      source: => Source[ByteString, Future[IOResult]]
  ): Source[String, Future[IOResult]] = {
    source
      .via(Flow[ByteString].dropUntil(ByteString.fromString("{"), true))
      .via(akka.stream.scaladsl.JsonFraming.objectScanner(Int.MaxValue))
      .map(_.utf8String)
  }
}
