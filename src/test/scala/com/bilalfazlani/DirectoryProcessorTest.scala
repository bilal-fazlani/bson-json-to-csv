package com.bilalfazlani

import munit.FunSuite
import java.nio.file.{Files, Paths}

class DirectoryProcessorTest extends FunSuite {

  // Test file discovery functionality
  test("discoverFiles should find JSON files with default pattern") {
    val tempDir = Files.createTempDirectory("json-test")
    try {
      // Create test files
      Files.write(tempDir.resolve("file1.json"), """{"test": 1}""".getBytes)
      Files.write(tempDir.resolve("file2.bson"), """{"test": 2}""".getBytes)
      Files.write(tempDir.resolve("file3.txt"), """not json""".getBytes)
      Files.write(tempDir.resolve("file4.jsonl"), """{"test": 4}""".getBytes)

      val files = DirectoryProcessor.discoverFiles(
        tempDir,
        "*.{json,bson}",
        recursive = false
      )

      assertEquals(files.size, 2)
      assert(files.exists(_.getFileName.toString == "file1.json"))
      assert(files.exists(_.getFileName.toString == "file2.bson"))
    } finally {
      // Cleanup
      Files
        .walk(tempDir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(Files.delete)
    }
  }

  test("discoverFiles should work recursively") {
    val tempDir = Files.createTempDirectory("json-test-recursive")
    try {
      // Create nested structure
      val subDir = Files.createDirectory(tempDir.resolve("subdir"))
      Files.write(
        tempDir.resolve("root.json"),
        """{"level": "root"}""".getBytes
      )
      Files.write(
        subDir.resolve("nested.json"),
        """{"level": "nested"}""".getBytes
      )

      val files = DirectoryProcessor.discoverFiles(
        tempDir,
        "*.json",
        recursive = true
      )

      assertEquals(files.size, 2)
      assert(files.exists(_.getFileName.toString == "root.json"))
      assert(files.exists(_.getFileName.toString == "nested.json"))
    } finally {
      Files
        .walk(tempDir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(Files.delete)
    }
  }

  test("discoverFiles should handle custom patterns") {
    val tempDir = Files.createTempDirectory("json-test-pattern")
    try {
      Files.write(tempDir.resolve("data_001.json"), """{"id": 1}""".getBytes)
      Files.write(tempDir.resolve("data_002.json"), """{"id": 2}""".getBytes)
      Files.write(tempDir.resolve("other.json"), """{"id": 3}""".getBytes)

      val files = DirectoryProcessor.discoverFiles(
        tempDir,
        "data_*.json",
        recursive = false
      )

      assertEquals(files.size, 2)
      assert(files.forall(_.getFileName.toString.startsWith("data_")))
    } finally {
      Files
        .walk(tempDir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(Files.delete)
    }
  }

  test("discoverFiles should return empty list for non-existent directory") {
    val nonExistentDir = Paths.get("/non/existent/directory")

    intercept[IllegalArgumentException] {
      DirectoryProcessor.discoverFiles(nonExistentDir, "*.json", false)
    }
  }

  test("isJsonOrBsonFile should identify correct file types") {
    assert(DirectoryProcessor.isJsonOrBsonFile(Paths.get("test.json")))
    assert(DirectoryProcessor.isJsonOrBsonFile(Paths.get("test.bson")))
    assert(DirectoryProcessor.isJsonOrBsonFile(Paths.get("test.jsonl")))
    assert(DirectoryProcessor.isJsonOrBsonFile(Paths.get("test.ndjson")))
    assert(!DirectoryProcessor.isJsonOrBsonFile(Paths.get("test.txt")))
    assert(!DirectoryProcessor.isJsonOrBsonFile(Paths.get("test.csv")))
  }
}
