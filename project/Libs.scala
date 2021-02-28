import sbt._

object Akka {
  val AkkaVersion = "2.6.13"
  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
  val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
  val jsonStreaming =
    "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "2.0.2"
}

object Libs {
  lazy val munit = "org.scalameta" %% "munit" % "0.7.22"
  lazy val scopt = "com.github.scopt" %% "scopt" % "4.0.0"
}

object Mongo {
  val `mongo-scala-driver` =
    "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"
}
