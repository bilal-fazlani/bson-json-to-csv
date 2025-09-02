package com.bilalfazlani

import org.apache.pekko.stream.{IOResult, Materializer}
import org.apache.pekko.stream.scaladsl.{FileIO, Keep, Sink, Source}
import org.apache.pekko.util.ByteString
import FileTypeFinder.UnknownFileTypeException
import scala.concurrent.{ExecutionContext, Future}

class FileTypeFinder(using Materializer, ExecutionContext) {
  def find(
      source: => Source[ByteString, Future[IOResult]]
  ): Future[FileType] = {
    enum FileState(val str: String):
      case Identified(override val str: String) extends FileState(str)
      case Detecting(override val str: String) extends FileState(str)
    end FileState

    val stream: Source[FileType, Future[IOResult]] = source
      .map { b =>
        val str = b.utf8String
        if (str.contains("{") || str.contains("[")) then
          FileState.Identified(str)
        else FileState.Detecting(str)
      }
      .takeWhile(
        x =>
          x match {
            case FileState.Detecting(_)  => true
            case FileState.Identified(_) => false
          },
        true
      )
      .filter {
        case FileState.Detecting(_)  => false
        case FileState.Identified(_) => true
      }
      .map { (x: FileState) =>
        val objectStreamIdentifierIndex = x.str.indexOf('{')
        val arrayIdentifierIndex = x.str.indexOf('[')

        if (objectStreamIdentifierIndex != -1 && arrayIdentifierIndex != -1) {
          if (
              objectStreamIdentifierIndex.min(
                arrayIdentifierIndex
              ) == objectStreamIdentifierIndex
            )
          then FileType.JsonStream
          else FileType.Array
        } else if (objectStreamIdentifierIndex != -1) FileType.JsonStream
        else if (arrayIdentifierIndex != -1) FileType.Array
        else throw UnknownFileTypeException()
      }

    stream
      .toMat(Sink.last)(Keep.right)
      .run()
      .recover { case x: NoSuchElementException =>
        throw UnknownFileTypeException()
      }
  }
}
object FileTypeFinder {
  class UnknownFileTypeException
      extends RuntimeException("Could not identify file type")
}
