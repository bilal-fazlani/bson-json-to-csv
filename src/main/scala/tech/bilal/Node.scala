package tech.bilal

import tech.bilal.Node.{Index, Name}

import scala.language.implicitConversions

sealed trait Node {
  def /(node: Node): JsonPath = JsonPath(this, node)

  def /(name: String): JsonPath = JsonPath(this, Name(name))
  def /(index: Int): JsonPath = JsonPath(this, Index(index))
}
object Node {

  implicit def nodeToPath(node: Node): JsonPath = JsonPath(node)

  case class Name(name: String) extends Node
  case class Index(index: Int) extends Node

  implicit class StringExtensions(nodeName: String) {
    def n: Name = Name(nodeName)
    def /(name: String): JsonPath = JsonPath(Name(nodeName), Name(name))
    def /(index: Int): JsonPath = JsonPath(Name(nodeName), Index(index))
  }
}
