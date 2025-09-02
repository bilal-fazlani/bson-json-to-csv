package com.bilalfazlani

import munit.FunSuite
import com.bilalfazlani.Node.Name

class SchemaWithFilenameTest extends FunSuite {

  test("allColumns should include filename column when enabled") {
    val baseSchema = Schema(
      paths = Set(JsonPath(Name("name")), JsonPath(Name("age"))),
      rows = 10
    )

    val schemaWithFilename = SchemaWithFilename(
      schema = baseSchema,
      includeFilename = true,
      filenameColumnName = "_filename"
    )

    val columns = schemaWithFilename.allColumns
    assertEquals(columns.size, 3)
    assertEquals(columns.head, "_filename")
    assert(columns.contains(".name"))
    assert(columns.contains(".age"))
  }

  test("allColumns should exclude filename column when disabled") {
    val baseSchema = Schema(
      paths = Set(JsonPath(Name("name")), JsonPath(Name("age"))),
      rows = 10
    )

    val schemaWithFilename = SchemaWithFilename(
      schema = baseSchema,
      includeFilename = false
    )

    val columns = schemaWithFilename.allColumns
    assertEquals(columns.size, 2)
    assert(columns.contains(".name"))
    assert(columns.contains(".age"))
    assert(!columns.contains("_filename"))
  }

  test("allColumns should use custom filename column name") {
    val baseSchema = Schema(
      paths = Set(JsonPath(Name("id"))),
      rows = 5
    )

    val schemaWithFilename = SchemaWithFilename(
      schema = baseSchema,
      includeFilename = true,
      filenameColumnName = "source_file"
    )

    val columns = schemaWithFilename.allColumns
    assertEquals(columns.size, 2)
    assertEquals(columns.head, "source_file")
    assert(columns.contains(".id"))
  }

  test("totalRows should return schema rows") {
    val baseSchema = Schema(paths = Set.empty, rows = 42)
    val schemaWithFilename = SchemaWithFilename(baseSchema, true)

    assertEquals(schemaWithFilename.totalRows, 42L)
  }

  test("totalPaths should return schema paths count") {
    val baseSchema = Schema(
      paths =
        Set(JsonPath(Name("a")), JsonPath(Name("b")), JsonPath(Name("c"))),
      rows = 10
    )
    val schemaWithFilename = SchemaWithFilename(baseSchema, false)

    assertEquals(schemaWithFilename.totalPaths, 3)
  }
}
