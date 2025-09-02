package com.bilalfazlani

import org.apache.pekko.actor.ActorSystem

trait CustomFixtures extends munit.FunSuite {
  val actorSystemFixture: FunFixture[ActorSystem] = FunFixture[ActorSystem](
    setup = { _ => ActorSystem(s"testActorSystem") },
    teardown = { actorSystem => actorSystem.terminate() }
  )
}
