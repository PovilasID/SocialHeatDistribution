package lt.socialheat.distributor

import akka.actor.ActorSystem

object Akka {
  implicit val actorSystem = ActorSystem("actorsystem")
}
