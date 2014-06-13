package lt.socialheat.distributor

import reactivemongo.api.MongoDriver
import reactivemongo.core.nodeset.Authenticate
import sprest.reactivemongo.ReactiveMongoPersistence
import akka.actor.ActorSystem

object MongoFactory extends ReactiveMongoPersistence{
    
  implicit val system = ActorSystem("simple-spray-client")
  val driver = new MongoDriver(system)
  val dbName = "sprayreactivemongodbexample"
  val userName = "event-user"
  val password = "socialheat"
  val credentials = Authenticate(dbName, userName, password)
  val connection = driver.connection(List("193.219.158.39"), List(credentials))
  //@ TODO FIX connction factory
  //val db = connection("sprayreactivemongodbexample")
  //val collection = db("events")

}