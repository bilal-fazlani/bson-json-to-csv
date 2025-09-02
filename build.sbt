name := "bson-json-to-csv"

version := sys.env.get("GIT_TAG_NAME").getOrElse("0.1.0-SNAPSHOT")

organization := "com.bilal-fazlani"
Compile / mainClass := Some("com.bilalfazlani.Main")

scalaVersion := "3.7.2"

// Enable strict warnings and fail build on warnings
scalacOptions ++= Seq(
  "-Wunused:all",
  "-deprecation",
  "-feature",
  "-Werror"
)

// Format sources on compile
ThisBuild / scalafmtOnCompile := true

enablePlugins(BuildInfoPlugin)
enablePlugins(NativeImagePlugin)

buildInfoKeys := Seq[BuildInfoKey](name, version)
buildInfoPackage := "com.bilalfazlani"

//GRAAL NATIVE IMAGE
nativeImageGraalHome := (sbt.io.Path.userHome / ".sdkman/candidates/java/current/").toPath
nativeImageOptions := Seq(
  "--no-fallback",
  "-march=native",
  "-Ob"
)
nativeImageAgentOutputDir := baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image"
nativeImageAgentMerge := false
nativeImageInstalled := true
//GRAAL NATIVE IMAGE

libraryDependencies ++= Seq(
  Pekko.pekkoStreams,
  Mongo.`mongo-scala-driver`,
  Libs.scopt,
  Libs.rainbowcli,
  Libs.semver4j,
  Libs.sttpCore,
  Libs.sttpCirce,
  Libs.`scala-csv` % Test,
  Libs.munit % Test
)
