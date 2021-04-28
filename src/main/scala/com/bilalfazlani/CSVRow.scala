package com.bilalfazlani

case class CSVRow(items: Seq[String]) {
  override def toString: String = items.mkString(",") + "\n"
}
object CSVRow {
  def apply(item: String, items: String*): CSVRow =
    new CSVRow(items.prepended(item))
}
