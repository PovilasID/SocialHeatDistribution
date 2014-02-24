package lt.socialheat.distributor

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import spray.json.RootJsonFormat
import sprest.models.UniqueSelector
import sprest.models.UUIDStringId
import sprest.reactivemongo.ReactiveMongoPersistence
import sprest.reactivemongo.typemappers.NormalizedIdTransformer
import sprest.reactivemongo.typemappers.SprayJsonTypeMapper
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands._
import reactivemongo.api.QueryOpts
import reactivemongo.bson.BSONArray
import Akka.actorSystem
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.bson.Producer.valueProducer


trait Mongo extends ReactiveMongoPersistence {
  import Akka.actorSystem

  private val driver = new MongoDriver(actorSystem)
  private val connection = driver.connection(List("localhost"))
  private val db = connection("sprayreactivemongodbexample")

  // Json mapping to / from BSON - in this case we want "_id" from BSON to be
  // mapped to "id" in JSON in all cases
  implicit object JsonTypeMapper extends SprayJsonTypeMapper with NormalizedIdTransformer

  abstract class UnsecuredDAO[M <: sprest.models.Model[String]](collName: String)(implicit jsformat: RootJsonFormat[M])
      extends CollectionDAO[M, String](db(collName)) {

    case class Selector(id: String) extends UniqueSelector[M, String]

    override def generateSelector(id: String) = Selector(id)
    override protected def addImpl(m: M)(implicit ec: ExecutionContext) = doAdd(m)
    override protected def updateImpl(m: M)(implicit ec: ExecutionContext) = doUpdate(m)
    override def remove(selector: Selector)(implicit ec: ExecutionContext) = uncheckedRemoveById(selector.id)

    protected val collection = db(collName)
    /*val command = Count(collName, Some(BSONDocument("tag"->"cloed")))
    db.command(command)*/
    def findLimitedEvents()(implicit ec: ExecutionContext) = collection.find(BSONDocument.empty)/*.sort(BSONDocument("heat" -> -1))*/.query(BSONDocument("tags" -> "lazy")).options(QueryOpts().batchSize(10)).cursor.collect[List](10, true)
    
    /*def pullEventsFB ()(implicit ec: ExecutionContext) = {
      implicit val system = ActorSystem("simple-spray-client")
	  import system.dispatcher // execution context for futures below
	  val log = Logging(system, getClass)
	
	  log.info("Requesting the elevation of Mt. Everest from Googles Elevation API...")
	
	  import ElevationJsonProtocol._
	  import SprayJsonSupport._
	  val pipeline = sendReceive ~> unmarshal[GoogleApiResult[Elevation]]
	
	  val responseFuture = pipeline {
	    Get("http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false")
	  }
	  responseFuture onComplete {
	    case Success(GoogleApiResult(_, Elevation(_, elevation) :: _)) =>
	      log.info("The elevation of Mt. Everest is: {} m", elevation)
	      shutdown()
	
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
    }*/
    
    def findLimitedEvent(
        categories: Option[String],
        tags: Option[String],
        start_time: Option[String],
        end_time: Option[String],
        location: Option[String],
        sort: Option[String],
        limit: Int,
        offset: Int)(implicit ec: ExecutionContext) = {
      var parameters = BSONArray()
      var matchPrams = BSONArray()
      var sortPrams = BSONDocument()
      location match {
        case Some(location) => {
          val locationSplit = location.split(":")
          val maxDist = locationSplit(2) match {case dist if !dist.isEmpty() => dist.toDouble case _ => 0}
          parameters = parameters add BSONDocument(
        		"$geoNear" -> BSONDocument(
                    "near" -> BSONArray(locationSplit(0).toDouble,locationSplit(1).toDouble),
                    "distanceField" -> "location.distance",
                    "maxDistance" -> maxDist,
                    "spherical"-> true,
                    "uniqueDocs" -> true))
        }
        case None => None
      }
      val startDateFormated = "ISODate(\""+start_time+"\")"
      start_time match {
      	case Some(start_time) => 
      		  matchPrams = matchPrams add BSONDocument("start" ->
      				  BSONDocument("$gte" -> startDateFormated)) 
      	case None => None
      }
      val endDateFormated = "ISODate(\""+end_time+"\")"
      end_time match {
      	case Some(end_time) => 
      		  matchPrams = matchPrams add BSONDocument("end" ->
      				  BSONDocument("$lt" -> endDateFormated))
      	case None => None
      }
      categories match {
        case Some(categories) => matchPrams = matchPrams add BSONDocument("categories" -> 
        									BSONDocument("$in" -> categories.split(",")))
        case None => None
      }
      tags match {
        case Some(tags) => matchPrams = matchPrams add BSONDocument("tags" -> 
        									BSONDocument("$in" ->tags.split(",")))
        case None => None
      }
      (categories, tags) match {
        case (categories, tags) 
        	if (categories.isDefined || tags.isDefined) => 
        	  parameters = parameters add BSONDocument(
        			"$match" ->  
      					BSONDocument("$and" -> matchPrams))
        case _ => None
      }
      sort match {
        case Some(rawSort) => {
          val sortPramsSplit = rawSort.split(":")
          for(sPram <- sortPramsSplit){
            sPram match {
              case sPram if(sPram.head == '-') => sortPrams = sortPrams add BSONDocument(sPram.substring(1) -> -1)
              case "soon" => sortPrams = sortPrams add BSONDocument(
                  "location.distance" -> 1,
                  "start_time" -> 1)
              case "-soon" => sortPrams = sortPrams add BSONDocument(
                  "location.distance" -> -1,
                  "start_time" -> -1)
              case _ => sortPrams = sortPrams add BSONDocument(sPram -> 1)
            }
          }
          parameters = parameters add BSONDocument("$sort" -> sortPrams)
        }
        case None => None
      }
      parameters = parameters add BSONDocument("$limit" -> limit)
      parameters = parameters add BSONDocument("$skip" -> offset)
      val data = db.command(RawCommand(
        BSONDocument(
            "aggregate" -> collName,
            "pipeline" -> parameters
                ))).map { doc => doc.getAs[List[M]]("result")
            }
      data
    }
    def removeAll()(implicit ec: ExecutionContext) = collection.remove(BSONDocument.empty)
    def findAll()(implicit ec: ExecutionContext) = find(BSONDocument.empty)
  }

  // MongoDB collections:
  import models._
  object Persons extends UnsecuredDAO[Person]("persons") with UUIDStringId {
    def findByName(name: String)(implicit ec: ExecutionContext) = find(BSONDocument("name" → name))
    def findByAge(age: Int)(implicit ec: ExecutionContext) = find(BSONDocument("age" → age))
  }
  object SEvents extends UnsecuredDAO[SEvent]("events") with UUIDStringId {
    def findEvents(categories: Option[String],
        explicit: Boolean,
        tags: Option[String],
        skip: Option[String],
        start_time: Option[String],
        end_time: Option[String],
        location: Option[String],
        sort: Option[String],
        limit: Int,
        offset: Int)(implicit ec: ExecutionContext) = {
      val data = db.command(RawCommand(BSONDocument.empty))
      data
    }
  }
}
object Mongo extends Mongo
