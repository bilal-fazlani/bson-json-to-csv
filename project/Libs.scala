import sbt._

object Pekko {
  val PekkoVersion = "1.0.2"
  val pekkoStreams =
    ("org.apache.pekko" %% "pekko-stream" % PekkoVersion)
      .withCrossVersion(CrossVersion.for3Use2_13)
  val pekkoActorTyped =
    ("org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion)
      .withCrossVersion(CrossVersion.for3Use2_13)
}

object Libs {
  lazy val munit = "org.scalameta" %% "munit" % "1.1.1"
  lazy val scopt = "com.github.scopt" %% "scopt" % "4.1.0"
  lazy val `scala-csv` = "com.github.tototoshi" %% "scala-csv" % "2.0.0"
  lazy val rainbowcli = "com.bilal-fazlani" %% "rainbowcli" % "3.0.1"
  // for version compare
  lazy val semver4j = "com.vdurmont" % "semver4j" % "3.1.0"
  lazy val sttpCore = "com.softwaremill.sttp.client3" %% "core" % "3.11.0"
  lazy val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % "3.11.0"
}

object Mongo {
  val `mongo-scala-driver` =
    ("org.mongodb.scala" %% "mongo-scala-driver" % "5.5.1").withCrossVersion(
      CrossVersion.for3Use2_13
    )
}
