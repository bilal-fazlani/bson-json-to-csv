package com.bilalfazlani

import akka.actor.ActorSystem

trait CustomFixtures extends munit.FunSuite {
  val actorSystemFixture: FunFixture[ActorSystem] = FunFixture[ActorSystem](
    setup = { _ => ActorSystem(s"testActorSystem") },
    teardown = { actorSystem => actorSystem.terminate() }
  )
}
