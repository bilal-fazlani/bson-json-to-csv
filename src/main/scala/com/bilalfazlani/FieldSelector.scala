package com.bilalfazlani

import com.bilalfazlani.Node.{Index, Name}
import scala.util.matching.Regex

object FieldSelector {

  /** Parse a jq-style field selector into a JsonPath
    *
    * Examples:
    *   - ".name" -> JsonPath(Name("name"))
    *   - ".location.city" -> JsonPath(Name("location"), Name("city"))
    *   - ".tags[0]" -> JsonPath(Name("tags"), Index(0))
    *   - ".tags[]" -> JsonPath(Name("tags"), ArrayWildcard)
    */
  def parseSelector(selector: String): Either[String, FieldPattern] = {
    if (!selector.startsWith(".")) {
      return Left(s"Field selector must start with '.': $selector")
    }

    val cleanSelector = selector.substring(1) // Remove leading dot
    if (cleanSelector.isEmpty) {
      return Left("Field selector cannot be just '.'")
    }

    parsePathSegments(cleanSelector.split("\\.").toList)
  }

  private def parsePathSegments(
      segments: List[String]
  ): Either[String, FieldPattern] = {
    val nodes = segments.map(parseSegment)

    // Check if any segment failed to parse
    val errors = nodes.collect { case Left(error) => error }
    if (errors.nonEmpty) {
      return Left(errors.head)
    }

    val parsedNodes = nodes.collect { case Right(node) => node }
    Right(FieldPattern(parsedNodes))
  }

  private def parseSegment(segment: String): Either[String, PathNode] = {
    val arrayPattern: Regex = """^([^\[\]]+)\[([^\[\]]*)\]$""".r

    segment match {
      case arrayPattern(fieldName, "") =>
        // Array wildcard like "tags[]"
        Right(ArrayWildcardNode(fieldName))
      case arrayPattern(fieldName, indexStr) =>
        // Array index like "tags[0]" or invalid like "tags[abc]"
        try {
          val index = indexStr.toInt
          if (index < 0) {
            Left(s"Array index must be non-negative: $index")
          } else {
            Right(ArrayIndexNode(fieldName, index))
          }
        } catch {
          case _: NumberFormatException =>
            Left(s"Invalid array index: $indexStr")
        }
      case fieldName
          if fieldName.nonEmpty && !fieldName.contains("[") && !fieldName
            .contains("]") =>
        // Simple field name
        Right(FieldNode(fieldName))
      case _ =>
        Left(s"Invalid field segment: $segment")
    }
  }

  /** Expand array wildcards based on discovered schema paths
    *
    * For example, if schema contains .tags[0], .tags[1], .tags[2] and selector
    * is .tags[], this will return .tags[0], .tags[1], .tags[2] as JsonPath
    * objects
    */
  def expandArrayWildcards(
      patterns: List[FieldPattern],
      schema: Schema
  ): List[JsonPath] = {
    patterns.flatMap { pattern =>
      if (pattern.hasArrayWildcard) {
        expandSinglePattern(pattern, schema)
      } else {
        pattern.toJsonPath.toList
      }
    }
  }

  private def expandSinglePattern(
      pattern: FieldPattern,
      schema: Schema
  ): List[JsonPath] = {
    val matchingPaths = schema.paths.filter { discoveredPath =>
      isPatternMatch(discoveredPath, pattern)
    }
    matchingPaths.toList
  }

  private def isPatternMatch(
      discoveredPath: JsonPath,
      pattern: FieldPattern
  ): Boolean = {
    matchNodes(discoveredPath.seq.toList, pattern.nodes)
  }

  private def matchNodes(
      pathNodes: List[Node],
      patternNodes: List[PathNode]
  ): Boolean = {
    (pathNodes, patternNodes) match {
      case (Nil, Nil) => true
      case (Nil, _)   => false
      case (_, Nil)   => false
      case (
            Name(pathName) :: pathRest,
            FieldNode(patternName) :: patternRest
          ) =>
        pathName == patternName && matchNodes(pathRest, patternRest)
      case (
            Name(pathName) :: Index(index) :: pathRest,
            ArrayIndexNode(patternName, patternIndex) :: patternRest
          ) =>
        pathName == patternName && index == patternIndex && matchNodes(
          pathRest,
          patternRest
        )
      case (
            Name(pathName) :: Index(_) :: pathRest,
            ArrayWildcardNode(patternName) :: patternRest
          ) =>
        pathName == patternName && matchNodes(pathRest, patternRest)
      case _ => false
    }
  }

  /** Check if a discovered JsonPath matches any of the selected patterns */
  def isPathSelected(
      discoveredPath: JsonPath,
      selectedPatterns: List[FieldPattern]
  ): Boolean = {
    selectedPatterns.exists(pattern => isPatternMatch(discoveredPath, pattern))
  }
}

/** Represents a field selection pattern that may include wildcards */
case class FieldPattern(nodes: List[PathNode]) {
  def hasArrayWildcard: Boolean =
    nodes.exists(_.isInstanceOf[ArrayWildcardNode])

  def toJsonPath: Option[JsonPath] = {
    if (hasArrayWildcard) {
      None // Cannot convert wildcards to concrete paths
    } else {
      // This should never contain ArrayWildcardNode due to hasArrayWildcard check
      val jsonNodes = nodes.flatMap {
        case FieldNode(name)             => List(Name(name))
        case ArrayIndexNode(name, index) => List(Name(name), Index(index))
        case _: ArrayWildcardNode        => List.empty // Should not happen
      }

      if (jsonNodes.nonEmpty) {
        Some(JsonPath(jsonNodes))
      } else {
        None
      }
    }
  }
}

/** Represents different types of path nodes in a field pattern */
sealed trait PathNode

case class FieldNode(name: String) extends PathNode
case class ArrayIndexNode(fieldName: String, index: Int) extends PathNode
case class ArrayWildcardNode(fieldName: String) extends PathNode
