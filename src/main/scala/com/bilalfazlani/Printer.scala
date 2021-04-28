package com.bilalfazlani

trait Printer {
  def println(str: String): Unit
  def print(str: String): Unit
}
object Printer {
  val console = new Printer {
    def println(str: String): Unit = System.out.println(str)
    def print(str: String): Unit = System.out.print(str)
  }
}
