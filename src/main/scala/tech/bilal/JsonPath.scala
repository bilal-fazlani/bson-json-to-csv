package tech.bilal

import tech.bilal.Node._

case class JsonPath(seq: Seq[Node]) {
  def -->(node: Node): JsonPath = JsonPath(seq.appended(node))

  override def toString: String =
    seq.foldLeft[String]("") {
      case (acc, Name(n))  => acc + "." + n
      case (acc, Index(i)) => acc + "[" + i + "]"
    }
}
object JsonPath {
  def apply(node: Node, nodes: Node*): JsonPath =
    new JsonPath(node +: nodes)
}
