import sbt._

object Akka {
  val AkkaVersion = "2.6.15"
  val akkaStreams = ("com.typesafe.akka" %% "akka-stream" % AkkaVersion)
    .withCrossVersion(CrossVersion.for3Use2_13)
  val akkaActorTyped = ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion)
    .withCrossVersion(CrossVersion.for3Use2_13)
  val jsonStreaming =
    ("com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "3.0.2")
      .withCrossVersion(CrossVersion.for3Use2_13)
}

object Libs {
  lazy val munit = "org.scalameta" %% "munit" % "0.7.27"
  lazy val scopt = ("com.github.scopt" %% "scopt" % "4.0.1").withCrossVersion(
    CrossVersion.for3Use2_13
  )
  lazy val `scala-csv` = ("com.github.tototoshi" %% "scala-csv" % "1.3.8")
    .withCrossVersion(CrossVersion.for3Use2_13)
  lazy val rainbowcli =
    "com.github.bilal-fazlani" % "rainbowcli" % "2.0.1"
}

object Mongo {
  val `mongo-scala-driver` =
    ("org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3").withCrossVersion(
      CrossVersion.for3Use2_13
    )
}
