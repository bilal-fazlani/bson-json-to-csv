package tech.bilal

import scala.language.implicitConversions

sealed trait Node {
  def -->(node: Node): JsonPath = JsonPath(this, node)
}
object Node {

  implicit def nodeToPath(node: Node): JsonPath = JsonPath(node)

  case class Name(name: String) extends Node
  case class Index(index: Int) extends Node

  implicit class StringNodeDsl(nodeName: String) {
    def n: Name = Name(nodeName)
  }

  implicit class IntNodeDsl(index: Int) {
    def n: Index = Index(index)
  }
}
