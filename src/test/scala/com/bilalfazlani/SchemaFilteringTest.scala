package com.bilalfazlani

import munit.FunSuite
import com.bilalfazlani.Node.{Index, Name}

class SchemaFilteringTest extends FunSuite {

  val sampleSchema = Schema(
    Set(
      JsonPath(Name("name")),
      JsonPath(Name("age")),
      JsonPath(Name("location"), Name("city")),
      JsonPath(Name("location"), Name("country")),
      JsonPath(Name("tags"), Index(0)),
      JsonPath(Name("tags"), Index(1)),
      JsonPath(Name("tags"), Index(2)),
      JsonPath(Name("addresses"), Index(0), Name("street")),
      JsonPath(Name("addresses"), Index(1), Name("street"))
    ),
    100L
  )

  test("filterFields with empty list returns original schema") {
    val result = sampleSchema.filterFields(List.empty)
    assert(result.isRight)
    assertEquals(result.toOption.get, sampleSchema)
  }

  test("filterFields with simple field selection") {
    val result = sampleSchema.filterFields(List(".name"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 1)
    assert(filteredSchema.paths.contains(JsonPath(Name("name"))))
    assertEquals(filteredSchema.rows, 100L)
  }

  test("filterFields with multiple field selection") {
    val result = sampleSchema.filterFields(List(".name", ".age"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 2)
    assert(filteredSchema.paths.contains(JsonPath(Name("name"))))
    assert(filteredSchema.paths.contains(JsonPath(Name("age"))))
  }

  test("filterFields with nested field selection") {
    val result = sampleSchema.filterFields(List(".location.city"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 1)
    assert(
      filteredSchema.paths.contains(JsonPath(Name("location"), Name("city")))
    )
  }

  test("filterFields with array index selection") {
    val result = sampleSchema.filterFields(List(".tags[0]"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 1)
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(0))))
  }

  test("filterFields with array wildcard selection") {
    val result = sampleSchema.filterFields(List(".tags[]"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 3)
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(0))))
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(1))))
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(2))))
  }

  test("filterFields with mixed selection") {
    val result =
      sampleSchema.filterFields(List(".name", ".location.city", ".tags[1]"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 3)
    assert(filteredSchema.paths.contains(JsonPath(Name("name"))))
    assert(
      filteredSchema.paths.contains(JsonPath(Name("location"), Name("city")))
    )
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(1))))
  }

  test("filterFields with invalid selector") {
    val result = sampleSchema.filterFields(List("invalid-selector"))
    assert(result.isLeft)
    assert(result.swap.toOption.get.contains("Invalid field selectors"))
  }

  test("filterFields with non-existent field") {
    val result = sampleSchema.filterFields(List(".nonexistent"))
    assert(result.isLeft)
    assert(result.swap.toOption.get.contains("No fields matched"))
  }

  test("filterFields with partial match - some exist, some don't") {
    val result = sampleSchema.filterFields(List(".name", ".nonexistent"))
    assert(result.isRight) // Should succeed because .name exists

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 1) // Only .name should match
    assert(filteredSchema.paths.contains(JsonPath(Name("name"))))
  }

  test("filterFields with complex nested array selection") {
    val result = sampleSchema.filterFields(List(".addresses[].street"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 2)
    assert(
      filteredSchema.paths.contains(
        JsonPath(Name("addresses"), Index(0), Name("street"))
      )
    )
    assert(
      filteredSchema.paths.contains(
        JsonPath(Name("addresses"), Index(1), Name("street"))
      )
    )
  }

  test("filterFields preserves row count") {
    val result = sampleSchema.filterFields(List(".name"))
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.rows, sampleSchema.rows)
  }

  test("filterFields with complex combinations") {
    val result = sampleSchema.filterFields(
      List(
        ".name",
        ".location.city",
        ".tags[]"
      )
    )
    assert(result.isRight)

    val filteredSchema = result.toOption.get
    assertEquals(filteredSchema.paths.size, 5) // name + location.city + 3 tags
    assert(filteredSchema.paths.contains(JsonPath(Name("name"))))
    assert(
      filteredSchema.paths.contains(JsonPath(Name("location"), Name("city")))
    )
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(0))))
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(1))))
    assert(filteredSchema.paths.contains(JsonPath(Name("tags"), Index(2))))
  }
}
