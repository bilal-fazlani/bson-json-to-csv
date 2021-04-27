name := "bson-json-to-csv"

version := "0.1.0-SNAPSHOT"

organization := "tech.bilal"

scalaVersion := "3.0.0-RC3"

resolvers += Resolver.JCenterRepository
resolvers += "jitpack" at "https://jitpack.io"

enablePlugins(NativeImagePlugin)

scalacOptions ++= Seq(
  "-source",
  "future",
  "-deprecation"
)

//GRAAL NATIVE IMAGE
nativeImageOptions ++= Seq("--initialize-at-build-time", "--no-fallback")
nativeImageInstalled := true
nativeImageJvm := "graalvm-java11"
nativeImageVersion := "21.0.0"
nativeImageOutput := file(name.value)
nativeImageAgentMerge := true
nativeImageOptions ++= Seq(
  "--initialize-at-run-time=org.bson.json.DateTimeFormatter$JaxbDateTimeFormatter"
)
nativeImageAgentOutputDir := baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image"
//GRAAL NATIVE IMAGE

libraryDependencies ++= Seq(
  Akka.akkaStreams,
  Akka.jsonStreaming,
  Mongo.`mongo-scala-driver`,
  Libs.scopt,
  Libs.`scala-rainbow`,
  Libs.`scala-csv` % Test,
  Libs.munit % Test
)
