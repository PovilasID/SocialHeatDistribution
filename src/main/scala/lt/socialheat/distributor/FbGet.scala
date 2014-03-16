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
import models.SEvent
import spray.httpx.encoding.Gzip
import models.SVenue
import scala.actors.Future
import spray.http.HttpRequest
import models.fbEven
import models.fbApiData
import scala.util.{Success, Failure}
import akka.actor.{Props, ActorSystem}
import spray.httpx.SprayJsonSupport
import spray.http._
import spray.util._
import spray.http.HttpHeaders.Accept
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.GetLastError
import 	models.fbECover
import reactivemongo.bson.BSONArray
import Mongo.SEvents
import spray.json.JsObject
import spray.json.JsString
import scala.util.Random
import models.GeoJson
import models.SVenue
import java.text.SimpleDateFormat
import java.text.Format
import java.util.Locale
import reactivemongo.core.nodeset.Authenticate


case class FBApiResult[T](data: List[T])

object ElevationJsonProtocol extends DefaultJsonProtocol {
  implicit def fbApiResultFormat[T :JsonFormat] = jsonFormat1(FBApiResult.apply[T])
}

object FbGet extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures below
  val log = Logging(system, getClass)

  log.info("Requesting the events form Kaunas...")

  import models.fbEventJsonProtocol._
  import SprayJsonSupport._
  val pipeline = addHeader(Accept(MediaTypes.`application/json`)) ~> sendReceive ~> unmarshal[fbApiData[fbEven]]

  val responseFuture = pipeline {
    Get("https://graph.facebook.com/fql?q=SELECT+eid,name,+host,creator,parent_group_id,+description,pic,+pic_square,pic_cover,start_time,end_time,timezone,location,venue,+all_members_count,attending_count,unsure_count,+declined_count,ticket_uri,update_time,version+FROM+event+WHERE+eid+IN+(SELECT+eid+FROM+event_member+WHERE+uid+IN+(SELECT+page_id+FROM+place+WHERE+distance(latitude,+longitude,+%2254.8871751%22,%2224.000000%22)+%3C+50000))AND+start_time+%3E+now()&access_token=562249617160396|007918fae6cd11b6fcbbeea123a132ab")
  }
  responseFuture onComplete {
    case Success(fbApiData(fbData)) => {
      import Akka.actorSystem
	  val driver = new MongoDriver(actorSystem)
	  val dbName = "sprayreactivemongodbexample"
	  val userName = "event-user"
	  val password = "socialheat"
	  val credentials = Authenticate(dbName, userName, password)
	  val connection = driver.connection(List("193.219.158.39"), List(credentials))
	  val db = connection("sprayreactivemongodbexample")
      val collection = db("events")
      for(fbEvent <- fbData){
      val fields = JsObject("title" -> JsString(fbEvent.name.get))
      SEvents.bindByFbID(fbEvent.eid.get.toString()) onComplete {
        //@ TODO fix upsert
        case Success(databseOnline)  => {
          val geo = GeoJson(
                Some("Point"), Some(List(
                    fbEvent.venue.get.latitude, fbEvent.venue.get.longitude)
              ))
          val venue = SVenue(
              title = fbEvent.location,
              country = fbEvent.venue.get.country,
              city = fbEvent.venue.get.city,
              street = fbEvent.venue.get.street,
              zip = fbEvent.venue.get.zip)
          val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
          val cover_data = fbEvent.pic_cover
          val currentEvent = SEvent(
              title = fbEvent.name, 
              nid = Some(Random.nextInt(Integer.MAX_VALUE)),
              desc = fbEvent.description,
              cover = cover_data match {
                case Some(pic) => pic.source
                case None => None
              },
              start_time = Some(
                  df.parse(fbEvent.start_time.get).getTime().toLong /1000),	                  
              facebook = fbEvent.eid.map(_.toString),
              location = Some(geo),
              heat = Some((fbEvent.attending_count.get * 5)
                  + ((fbEvent.all_members_count.get - fbEvent.attending_count.get)*1)
                  - (fbEvent.declined_count.get * 3)
                 ),
              venues = Some(List(venue)),
              explicit = Some(fbEvent.description.get + " " + fbEvent.name.get contains Seq("gay", "porn", "orgy")),
              version = Some(System.currentTimeMillis / 1000)
          )
          if (databseOnline == Nil){
            SEvents.add(currentEvent)
          } else {
            currentEvent.id = databseOnline(0).id
            SEvents.update(currentEvent)
          }
        }
        case Failure(fixDatabase) => None
        case _ => None
      }
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
