package lt.socialheat.distributor

import scala.concurrent.duration._
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.can.Http
import spray.client.pipelining._
import models.SEvent
import models._
import scala.util.{Success, Failure}
import akka.actor.ActorSystem
import spray.http._
import spray.http.HttpHeaders.Accept
import reactivemongo.api.MongoDriver
import Mongo.SEvents
import models.GeoJson
import models.SVenue
import java.text.SimpleDateFormat
import reactivemongo.core.nodeset.Authenticate
import spray.httpx.SprayJsonSupport
import scala.concurrent.{ExecutionContext, Future}

case class FBApiResult[T](data: List[T])

object fBApiJsonProtocol extends DefaultJsonProtocol {
  implicit def fbApiResultFormat[T :JsonFormat] = jsonFormat1(FBApiResult.apply[T])
}

object FbGet extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")

  import system.dispatcher // execution context for futures below
  val log = Logging(system, getClass)

  val explicitKeywords = Seq("gay", "porn", "orgy", "xxx")
  
  log.info("Requesting the events form Kaunas...")
  
  import models.fbEventJsonProtocol._
  import SprayJsonSupport._
  val pipeline = addHeader(Accept(MediaTypes.`application/json`)) ~> sendReceive ~> unmarshal[fbApiData[fbEven]]
  
  val responseFuture = pipeline {
    Get("https://graph.facebook.com/fql?q="+
        "SELECT+eid,name,+host,creator,parent_group_id,+description,pic,+pic_square,pic_cover,start_time,end_time,timezone,location,venue,+all_members_count,attending_count,unsure_count,+declined_count,ticket_uri,update_time,version+"+
        "FROM+event+"+
        "WHERE+eid+IN+("+
    		"SELECT+eid+"+
    		"FROM+event_member+"+
    		"WHERE+uid+IN+("+
    			"SELECT+page_id+"+
    			"FROM+place+"+
    			"WHERE+distance("+
    				"latitude,+longitude,+%2254.8871751%22,%2224.000000%22"+
    			")+%3C+50000))"+
    			"AND+start_time+%3E+now()&access_token=562249617160396|007918fae6cd11b6fcbbeea123a132ab")
  }

  responseFuture onComplete {
    case Success(fbApiData(fbData)) => {
	  val driver = new MongoDriver(system)
	  val dbName = "sprayreactivemongodbexample"
	  val userName = "event-user"
	  val password = "socialheat"
	  val credentials = Authenticate(dbName, userName, password)
	  val connection = driver.connection(List("193.219.158.39"), List(credentials))
	  val db = connection("sprayreactivemongodbexample")
      val collection = db("events")
      for(fbEvent <- fbData){
      SEvents.bindByFbID(fbEvent.eid.get.toString) onComplete {
        //@ TODO fix upsert
        case Success(databseOnline)  =>
          var longitude = fbEvent.venue.flatMap(_.longitude)
          var latitude = fbEvent.venue.flatMap(_.latitude)
          (longitude, latitude) match {
            case (None, None) => {
              (fbEvent.venue.flatMap(_.name), fbEvent.location) match {
                case (name, location) if !(name.isEmpty && location.isEmpty) => {
                  import models.geocoderJsonProtocol._
                  import SprayJsonSupport._
                  val pipelineGeocoder = addHeader(Accept(MediaTypes.`application/json`)) ~> sendReceive ~> unmarshal[geocoderApiResult[GCGeometry]]
                  val responseGeocoderFuture = pipelineGeocoder {Get("https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&sensor=false&key=AIzaSyDg7RUkKwvMV-ZbhbgJUf0TunFPujn4e3k")}
                  responseGeocoderFuture onComplete {
                    case Success(geocodedCordinates) => {
                      latitude = geocodedCordinates.results(0).location.flatMap(_.lat)
                      longitude = geocodedCordinates.results(0).location.flatMap(_.lng)
                    }
                  } 
                }
              }
            }
            case _ => None
          }
          val geo = GeoJson(
                Some("Point"), 
                Some(List(longitude,latitude))
                )
          val venue = SVenue(
              title = fbEvent.location,
              country = fbEvent.venue.flatMap(_.country),
              city = fbEvent.venue.get.city,
              street = fbEvent.venue.get.street,
              zip = fbEvent.venue.get.zip)
          val sources = fbEvent.summary match {
            case Some(summary) => fbEvent.summary
            case _ => None
          }
          val dfISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
          val cover_data = fbEvent.pic_cover
          val currentEvent = SEvent(
              title = fbEvent.name,
              desc = fbEvent.description,
              cover = cover_data.flatMap(_.source),
              start_time = Some(
                  dfISO8601.parse(fbEvent.start_time.get).getTime().toLong /1000),
                  //@ TODO add 2014-03-36 date format
              facebook = fbEvent.eid.map(_.toString),
              location = Some(geo),
              heat = Some((fbEvent.attending_count.get * 5)
                  + ((fbEvent.all_members_count.get - fbEvent.attending_count.get)*1)
                  - (fbEvent.declined_count.get * 3)
                 ),
              venues = Some(List(venue)),
              explicit = Some(fbEvent.description.get + " " + fbEvent.name.get contains explicitKeywords),
              version = Some(System.currentTimeMillis / 1000)
          )

          val dbOperation =
            if (databseOnline == Nil) SEvents.add(currentEvent)
	          else {
	            currentEvent.id = databseOnline(0).id
	            SEvents.update(currentEvent)
	          }
          dbOperation.onComplete { op =>
            log.info(s"Yay, database op completed with $op")
            shutdown()
          }
        case Failure(fixDatabase) => None
        case _ => None
      }
      log.info("Title of event is: "+ fbEvent.name.get)
      }
      //shutdown()
    }
    case Success(somethingUnexpected) =>
      log.warning("The Facebook API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
      shutdown()

    case Failure(error) =>
      log.error(error, "Couldn't get elevation")
      shutdown()
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).onComplete { _ => 
      system.shutdown()
    }
    //sys.exit()
  }
}
