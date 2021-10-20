name := "bson-json-to-csv"

version := sys.env.get("GIT_TAG_NAME").getOrElse("0.1.0-SNAPSHOT")

organization := "com.bilal-fazlani"

scalaVersion := "3.0.2"

// resolvers += Resolver.JCenterRepository
resolvers += "jitpack" at "https://jitpack.io"

enablePlugins(NativeImagePlugin)
enablePlugins(BuildInfoPlugin)

scalacOptions ++= Seq(
  "-rewrite",
  "-source",
  "future-migration",
  "-deprecation"
)

buildInfoKeys := Seq[BuildInfoKey](name, version)
buildInfoPackage := "com.bilalfazlani"

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
  Libs.rainbowcli,
  Libs.mavenArtifact,
  Libs.sttpCore,
  Libs.sttpCirce,
  Libs.`scala-csv` % Test,
  Libs.munit % Test
)
