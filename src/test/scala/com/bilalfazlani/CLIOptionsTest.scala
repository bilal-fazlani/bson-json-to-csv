package com.bilalfazlani

import munit.FunSuite
import java.io.File
import java.nio.file.Files

class CLIOptionsTest extends FunSuite {

  test("getOutputFile should use directory name for directory input") {
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

  test("getOutputFile should use provided output file when specified") {
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

  test("getOutputFile should use filename.csv for single file input") {
    val tempFile = Files.createTempFile("test", ".json")
    try {
      val config = CLIOptions(
        inputPath = tempFile.toFile,
        outputFile = None
      )

      val outputFile = CLIOptions.getOutputFile(config)
      assert(outputFile.getName.endsWith(".csv"))
      assert(outputFile.getName.startsWith("test"))
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  test("default CLI options should have expected values") {
    val defaultOptions = CLIOptions()

    assertEquals(defaultOptions.filePattern, "*.{json,bson}")
    assertEquals(defaultOptions.recursive, false)
    assertEquals(defaultOptions.addFilenameColumn, None)
    assertEquals(defaultOptions.filenameColumnName, "_filename")
    assertEquals(defaultOptions.noColor, false)
    assertEquals(defaultOptions.overrideFile, false)
  }

  test("filename column options should work correctly") {
    val options1 = CLIOptions(addFilenameColumn = Some(true))
    assertEquals(options1.addFilenameColumn, Some(true))

    val options2 = CLIOptions(addFilenameColumn = Some(false))
    assertEquals(options2.addFilenameColumn, Some(false))

    val options3 = CLIOptions(filenameColumnName = "source")
    assertEquals(options3.filenameColumnName, "source")
  }

  test("getOutputFile should handle current directory '.' correctly") {
    val currentDir = new File(".")
    val config = CLIOptions(
      inputPath = currentDir,
      outputFile = None
    )

    val outputFile = CLIOptions.getOutputFile(config)
    val actualDirName = currentDir.getCanonicalFile.getName
    val expectedName = s"${actualDirName}_combined.csv"

    assertEquals(outputFile.getName, expectedName)
    // Should not be "._combined.csv"
    assert(!outputFile.getName.startsWith("._"))
  }
}
