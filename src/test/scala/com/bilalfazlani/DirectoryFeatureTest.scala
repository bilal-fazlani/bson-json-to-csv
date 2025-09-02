package com.bilalfazlani

import munit.FunSuite
import java.nio.file.Files
import java.io.File

class DirectoryFeatureTest extends FunSuite {

  test("DirectoryProcessor.discoverFiles should find JSON files") {
    val tempDir = Files.createTempDirectory("test")
    try {
      // Create test files
      Files.write(tempDir.resolve("file1.json"), """{"test": 1}""".getBytes)
      Files.write(tempDir.resolve("file2.bson"), """{"test": 2}""".getBytes)
      Files.write(tempDir.resolve("file3.txt"), """not json""".getBytes)

      val files = DirectoryProcessor.discoverFiles(
        tempDir,
        "*.{json,bson}",
        recursive = false
      )

      assertEquals(files.size, 2)
      assert(files.exists(_.getFileName.toString == "file1.json"))
      assert(files.exists(_.getFileName.toString == "file2.bson"))
      assert(!files.exists(_.getFileName.toString == "file3.txt"))
    } finally {
      Files
        .walk(tempDir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(Files.delete)
    }
  }

  test("CLIOptions.getOutputFile should use directory name for directories") {
    val tempDir = Files.createTempDirectory("test-dir")
    try {
      val config = CLIOptions(
        inputPath = tempDir.toFile,
        outputFile = None
      )

      val outputFile = CLIOptions.getOutputFile(config)
      val expectedName = s"${tempDir.getFileName}_combined.csv"

      assertEquals(outputFile.getName, expectedName)
    } finally {
      Files.deleteIfExists(tempDir)
    }
  }

  test("CLIOptions.getOutputFile should respect custom output file") {
    val tempDir = Files.createTempDirectory("test-dir")
    val customOutput = new File("custom-output.csv")
    try {
      val config = CLIOptions(
        inputPath = tempDir.toFile,
        outputFile = Some(customOutput)
      )

      val outputFile = CLIOptions.getOutputFile(config)
      assertEquals(outputFile.getName, "custom-output.csv")
    } finally {
      Files.deleteIfExists(tempDir)
    }
  }

  test("SchemaWithFilename should include filename column when enabled") {
    import com.bilalfazlani.Node.Name

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

  test("SchemaWithFilename should exclude filename column when disabled") {
    import com.bilalfazlani.Node.Name

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

  test(
    "DirectoryProcessor.isJsonOrBsonFile should identify correct extensions"
  ) {
    assert(
      DirectoryProcessor.isJsonOrBsonFile(java.nio.file.Paths.get("test.json"))
    )
    assert(
      DirectoryProcessor.isJsonOrBsonFile(java.nio.file.Paths.get("test.bson"))
    )
    assert(
      DirectoryProcessor.isJsonOrBsonFile(java.nio.file.Paths.get("test.jsonl"))
    )
    assert(
      DirectoryProcessor.isJsonOrBsonFile(
        java.nio.file.Paths.get("test.ndjson")
      )
    )
    assert(
      !DirectoryProcessor.isJsonOrBsonFile(java.nio.file.Paths.get("test.txt"))
    )
    assert(
      !DirectoryProcessor.isJsonOrBsonFile(java.nio.file.Paths.get("test.csv"))
    )
  }

  test("CLI options should have correct default values") {
    val defaultOptions = CLIOptions()

    assertEquals(defaultOptions.filePattern, "*.{json,bson}")
    assertEquals(defaultOptions.recursive, false)
    assertEquals(defaultOptions.addFilenameColumn, None)
    assertEquals(defaultOptions.filenameColumnName, "_filename")
    assertEquals(defaultOptions.noColor, false)
    assertEquals(defaultOptions.overrideFile, false)
  }
}
