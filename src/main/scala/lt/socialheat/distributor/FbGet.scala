package lt.socialheat.distributor

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import lt.socialheat.distributor.models.SEvent
import spray.httpx.encoding.Gzip
import lt.socialheat.distributor.models.SVenue
import scala.actors.Future
import spray.http.HttpRequest
import lt.socialheat.distributor.models.fbEven
import lt.socialheat.distributor.models.fbApiData
import scala.util.{Success, Failure}
import akka.actor.{Props, ActorSystem}
import spray.httpx.SprayJsonSupport
import spray.http._
import spray.util._
import spray.http.HttpHeaders.Accept
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.GetLastError
import lt.socialheat.distributor.models.fbECover
import reactivemongo.bson.BSONArray


case class FBApiResult[T](data: List[T])

object ElevationJsonProtocol extends DefaultJsonProtocol {
  implicit def fbApiResultFormat[T :JsonFormat] = jsonFormat1(FBApiResult.apply[T])
}

object FbGet extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures below
  val log = Logging(system, getClass)

  log.info("Requesting the events form Klaipeda...")

  import lt.socialheat.distributor.models.fbEventJsonProtocol._
  import SprayJsonSupport._
  val pipeline = addHeader(Accept(MediaTypes.`application/json`)) ~> sendReceive ~> unmarshal[fbApiData[fbEven]]

  val responseFuture = pipeline {
    Get("https://graph.facebook.com/fql?q=SELECT+eid,name,+host,creator,parent_group_id,+description,pic,+pic_square,pic_cover,start_time,end_time,timezone,location,venue,+all_members_count,attending_count,unsure_count,+declined_count,ticket_uri,update_time,version+FROM+event+WHERE+eid+IN+(SELECT+eid+FROM+event_member+WHERE+uid+IN+(SELECT+page_id+FROM+place+WHERE+distance(latitude,+longitude,+%2255.7171751%22,%2221.1416276%22)+%3C+50000))AND+start_time+%3E+now()&access_token=562249617160396|007918fae6cd11b6fcbbeea123a132ab")
  }
  responseFuture onComplete {
    case Success(fbApiData(fbData)) => {
      import Akka.actorSystem
      val driver = new MongoDriver(actorSystem)
      val connection = driver.connection(List("localhost"))
      val db = connection("sprayreactivemongodbexample")
      val collection = db("events")
      for(fbEvent <- fbData){
        //val cover:Option[fbECover] = fbEvent.pic_cover
      val update = BSONDocument(
          "$set" -> BSONDocument(
              "title" -> fbEvent.name,
              "desc" -> fbEvent.description,
              "facebook"-> fbEvent.eid.get.toString(),
              /*"cover" -> {
                cover match { 
                  case Some(coverData) => coverData.source
                  case None => None}
                },*/
              "start_time" -> fbEvent.start_time,
              "end_time" -> fbEvent.end_time,
              "venue" -> BSONArray(BSONDocument(
                  "title" -> fbEvent.location,
                  "country" -> fbEvent.venue.get.country, //@ TODO Turn to iso codes
                  "city" -> fbEvent.venue.get.city,
                  "street" -> fbEvent.venue.get.street)),
              "location" ->  BSONDocument(
                  "type"->"Point",
                  "coordinates" -> BSONArray(
                      fbEvent.venue.get.latitude,
                      fbEvent.venue.get.longitude))
               ))
      collection.update(BSONDocument("facebook"-> fbEvent.eid.get.toString()), update, GetLastError(), true)
      log.info("Titile of event is: "+ fbEvent.name.get)
      }
      shutdown()
      }

    case Success(somethingUnexpected) =>
      log.warning("The Google API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
      shutdown()

    case Failure(error) =>
      log.error(error, "Couldn't get elevation")
      shutdown()
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}
