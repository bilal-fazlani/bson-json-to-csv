package com.bilalfazlani

// NotUsed not used
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.{Flow, Source}
import org.apache.pekko.util.ByteString
import scala.concurrent.Future
import scala.annotation.nowarn

extension [Mat](flow: Flow[ByteString, ByteString, Mat])
  def dropUntil(
      separator: ByteString,
      inclusive: Boolean
  ): Flow[ByteString, ByteString, Mat] =
    separate(separator, inclusive).drop(1)

  @nowarn("cat=deprecation")
  private def separate(separator: ByteString, inclusive: Boolean) =
    flow
      .statefulMapConcat(() => {
        var total: ByteString = ByteString.empty
        var last: ByteString = ByteString.empty
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
      .via(
        org.apache.pekko.stream.scaladsl.JsonFraming.objectScanner(Int.MaxValue)
      )
      .map(_.utf8String)
  }
}
