package com.bilalfazlani

import munit.FunSuite
import com.bilalfazlani.Node.{Index, Name}

class FieldSelectorTest extends FunSuite {

  test("parse simple field selector") {
    val result = FieldSelector.parseSelector(".name")
    assert(result.isRight)

    val pattern = result.toOption.get
    assertEquals(pattern.nodes.length, 1)
    assertEquals(pattern.nodes.head, FieldNode("name"))
  }

  test("parse nested field selector") {
    val result = FieldSelector.parseSelector(".location.city")
    assert(result.isRight)

    val pattern = result.toOption.get
    assertEquals(pattern.nodes.length, 2)
    assertEquals(pattern.nodes(0), FieldNode("location"))
    assertEquals(pattern.nodes(1), FieldNode("city"))
  }

  test("parse array index selector") {
    val result = FieldSelector.parseSelector(".tags[0]")
    assert(result.isRight)

    val pattern = result.toOption.get
    assertEquals(pattern.nodes.length, 1)
    assertEquals(pattern.nodes.head, ArrayIndexNode("tags", 0))
  }

  test("parse array wildcard selector") {
    val result = FieldSelector.parseSelector(".tags[]")
    assert(result.isRight)

    val pattern = result.toOption.get
    assertEquals(pattern.nodes.length, 1)
    assertEquals(pattern.nodes.head, ArrayWildcardNode("tags"))
  }

  test("parse complex nested selector with arrays") {
    val result = FieldSelector.parseSelector(".users[0].address.city")
    assert(result.isRight)

    val pattern = result.toOption.get
    assertEquals(pattern.nodes.length, 3)
    assertEquals(pattern.nodes(0), ArrayIndexNode("users", 0))
    assertEquals(pattern.nodes(1), FieldNode("address"))
    assertEquals(pattern.nodes(2), FieldNode("city"))
  }

  test("reject selector without leading dot") {
    val result = FieldSelector.parseSelector("name")
    assert(result.isLeft)
    assert(result.swap.toOption.get.contains("must start with '.'"))
  }

  test("reject empty selector") {
    val result = FieldSelector.parseSelector(".")
    assert(result.isLeft)
    assert(result.swap.toOption.get.contains("cannot be just '.'"))
  }

  test("reject negative array index") {
    val result = FieldSelector.parseSelector(".tags[-1]")
    assert(result.isLeft)
    assert(result.swap.toOption.get.contains("must be non-negative"))
  }

  test("reject invalid array syntax") {
    val result = FieldSelector.parseSelector(".tags[abc]")
    assert(result.isLeft)
    assert(result.swap.toOption.get.contains("Invalid array index"))
  }

  test("hasArrayWildcard detection") {
    val simplePattern = FieldSelector.parseSelector(".name").toOption.get
    assertEquals(simplePattern.hasArrayWildcard, false)

    val indexPattern = FieldSelector.parseSelector(".tags[0]").toOption.get
    assertEquals(indexPattern.hasArrayWildcard, false)

    val wildcardPattern = FieldSelector.parseSelector(".tags[]").toOption.get
    assertEquals(wildcardPattern.hasArrayWildcard, true)
  }

  test("toJsonPath conversion for simple fields") {
    val pattern = FieldSelector.parseSelector(".name").toOption.get
    val jsonPath = pattern.toJsonPath

    assert(jsonPath.isDefined)
    assertEquals(jsonPath.get.seq.length, 1)
    assertEquals(jsonPath.get.seq.head, Name("name"))
  }

  test("toJsonPath conversion for nested fields") {
    val pattern = FieldSelector.parseSelector(".location.city").toOption.get
    val jsonPath = pattern.toJsonPath

    assert(jsonPath.isDefined)
    assertEquals(jsonPath.get.seq.length, 2)
    assertEquals(jsonPath.get.seq(0), Name("location"))
    assertEquals(jsonPath.get.seq(1), Name("city"))
  }

  test("toJsonPath conversion for array index") {
    val pattern = FieldSelector.parseSelector(".tags[0]").toOption.get
    val jsonPath = pattern.toJsonPath

    assert(jsonPath.isDefined)
    assertEquals(jsonPath.get.seq.length, 2)
    assertEquals(jsonPath.get.seq(0), Name("tags"))
    assertEquals(jsonPath.get.seq(1), Index(0))
  }

  test("toJsonPath returns None for wildcards") {
    val pattern = FieldSelector.parseSelector(".tags[]").toOption.get
    val jsonPath = pattern.toJsonPath

    assertEquals(jsonPath, None)
  }

  test("expandArrayWildcards with simple schema") {
    val schema = Schema(
      Set(
        JsonPath(Name("name")),
        JsonPath(Name("tags"), Index(0)),
        JsonPath(Name("tags"), Index(1)),
        JsonPath(Name("tags"), Index(2))
      ),
      10L
    )

    val patterns = List(
      FieldSelector.parseSelector(".tags[]").toOption.get
    )

    val expanded = FieldSelector.expandArrayWildcards(patterns, schema)
    assertEquals(expanded.length, 3)
    assert(expanded.contains(JsonPath(Name("tags"), Index(0))))
    assert(expanded.contains(JsonPath(Name("tags"), Index(1))))
    assert(expanded.contains(JsonPath(Name("tags"), Index(2))))
  }

  test("expandArrayWildcards with no wildcards") {
    val schema = Schema(
      Set(
        JsonPath(Name("name")),
        JsonPath(Name("age"))
      ),
      5L
    )

    val patterns = List(
      FieldSelector.parseSelector(".name").toOption.get
    )

    val expanded = FieldSelector.expandArrayWildcards(patterns, schema)
    assertEquals(expanded.length, 1)
    assertEquals(expanded.head, JsonPath(Name("name")))
  }

  test("isPathSelected with exact match") {
    val discoveredPath = JsonPath(Name("name"))
    val patterns = List(
      FieldSelector.parseSelector(".name").toOption.get
    )

    val result = FieldSelector.isPathSelected(discoveredPath, patterns)
    assertEquals(result, true)
  }

  test("isPathSelected with no match") {
    val discoveredPath = JsonPath(Name("age"))
    val patterns = List(
      FieldSelector.parseSelector(".name").toOption.get
    )

    val result = FieldSelector.isPathSelected(discoveredPath, patterns)
    assertEquals(result, false)
  }

  test("isPathSelected with array index match") {
    val discoveredPath = JsonPath(Name("tags"), Index(0))
    val patterns = List(
      FieldSelector.parseSelector(".tags[0]").toOption.get
    )

    val result = FieldSelector.isPathSelected(discoveredPath, patterns)
    assertEquals(result, true)
  }

  test("isPathSelected with nested field match") {
    val discoveredPath = JsonPath(Name("location"), Name("city"))
    val patterns = List(
      FieldSelector.parseSelector(".location.city").toOption.get
    )

    val result = FieldSelector.isPathSelected(discoveredPath, patterns)
    assertEquals(result, true)
  }
}
