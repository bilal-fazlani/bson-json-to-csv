package com.bilalfazlani

case class SchemaWithFilename(
    schema: Schema,
    includeFilename: Boolean,
    filenameColumnName: String = "_filename"
) {
  def allColumns: List[String] = {
    val jsonColumns = schema.paths.toList.map(_.toString)
    if (includeFilename) {
      filenameColumnName :: jsonColumns
    } else {
      jsonColumns
    }
  }

  def totalRows: Long = schema.rows
  def totalPaths: Int = schema.paths.size
}
