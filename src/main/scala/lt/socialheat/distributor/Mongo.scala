package lt.socialheat.distributor

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import spray.json.RootJsonFormat
import sprest.models.UniqueSelector
import sprest.models.UUIDStringId
import reactivemongo.core.commands.RawCommand
import reactivemongo.api.QueryOpts
import reactivemongo.bson.BSONArray
import akka.event.Logging
import models.SEvent
import spray.json.JsonFormat
import sprest.reactivemongo.typemappers.BSONTypeMapper
import spray.json.JsObject
import spray.json.JsNumber
import spray.json.JsString
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.nodeset.Authenticate
import sprest.reactivemongo.ReactiveMongoPersistence
import sprest.reactivemongo.typemappers.SprayJsonTypeMapper
import sprest.reactivemongo.typemappers.NormalizedIdTransformer
import spray.json.RootJsonFormat
import sprest.Formats._
import scala.actors.Future
import reactivemongo.core.commands.LastError
import reactivemongo.core.commands.GetLastError
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Await
import scala.concurrent.duration._

trait Mongo extends ReactiveMongoPersistence {
  import Akka.actorSystem
  
  private val driver = new MongoDriver(actorSystem)
  private val dbName = "socialheat"
  private val userName = "event-user"
  private val password = "socialheat"
  private val credentials = Authenticate(dbName, userName, password)
  private val connection = driver.connection(List("193.219.158.39"), List(credentials))
  private val db = connection("socialheat")


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
    
    override protected val collection = db(collName)
    
    //def findLimitedEvents()(implicit ec: ExecutionContext) = collection.find(BSONDocument.empty)/*.sort(BSONDocument("heat" -> -1))*/.query(BSONDocument("tags" -> "lazy")).options(QueryOpts().batchSize(10)).cursor.collect[List](10, true)
    
	  implicit val system = actorSystem
	  import system.dispatcher // execution context for futures
	  val log = Logging(system, getClass)
    def pullEventsFB (args: Array[String])(implicit ec: ExecutionContext) = {
    	
    }
    var newParmeter = BSONArray()

    /**
     * Method for matching multiple valued data like tags or categories
     * matchData is split on ,
     */
    def mongoMultiMatcher(matchParmeters:BSONArray, matchData: Option[String], matchName: String) = {
      matchData match {
        case Some(data) => newParmeter = matchParmeters add BSONDocument(matchName -> 
        BSONDocument("$in" -> data.split(",")))
        case None => None
      }
      newParmeter
    }
    
    /**
     * Method for matching false data
     */
    def mongoFalseFilter(matchParmeters:BSONArray, matchData: Option[Boolean], matchName: String) = {
      matchData match {
    	case Some(data) if data == false => 
          newParmeter = matchParmeters add BSONDocument(matchName -> false)
        case Some(data) if data == true => None
        case _ => None
      }
      newParmeter
    }
    
    def findTags(limit: Int, offset: Int)(implicit ec: ExecutionContext) = {
      var parameters = BSONArray()
      
      parameters = parameters add BSONDocument("$unwind" -> "$tags")
      
      parameters = parameters add BSONDocument("$group" -> BSONDocument(
    		  									"_id" -> "$tags",
    		  									"count" -> BSONDocument(
    		  									    "$sum" -> 1)))
      parameters = parameters add BSONDocument("$sort" -> BSONDocument(
    		  									"count" -> -1))
      
      parameters = parameters add BSONDocument("$limit" -> (limit + offset))
      parameters = parameters add BSONDocument("$skip" -> offset)
      log.info(BSONArray.pretty(parameters))
      val data = db.command(RawCommand(
        BSONDocument(
            "aggregate" -> collName,
            "pipeline" -> parameters
        ))
      ).map { doc => doc.getAs[List[M]]("result") }
      
      data
    }
    
    def findLimitedEvent(
        //userId: Option[String],
        q: Option[String],
        ids: Option[String],
        categories: Option[String],
        explicit: Option[Boolean],
        explicitVenues: Option[Boolean],
        tags: Option[String],
        skip: Option[String],
        start_time: Option[Long],
        end_time: Option[Long],
        location: Option[String],
        sort: Option[String],
        limit: Int,
        offset: Int)(implicit ec: ExecutionContext) = {
      var parameters = BSONArray()
      var matchPrams = BSONArray()
      var sortPrams = BSONDocument()
      //db.events.ensureIndex({"location.coordinates": "2dsphere"})
      location match {
        case Some(location) => {
          val locationSplit = location.split(":")
          val maxDist = locationSplit match {case dist if locationSplit.length > 2 => dist(2).toDouble case _ => 0}
          parameters = parameters add BSONDocument(
        		"$geoNear" -> BSONDocument(
                    "near" -> BSONArray(locationSplit(1).toDouble,locationSplit(0).toDouble),
                    "distanceField" -> "location.distance",
                    "maxDistance" -> maxDist,
                    "spherical"-> true,
                    "distanceMultiplier" -> 6371000,
                    "uniqueDocs" -> true))
        }
        case None => None
      }
     val soonProjet = BSONDocument("$project" ->
    		 BSONDocument("_id" -> 1,
    		     "sources" -> 1,
    		     "title" -> 1,
    		     "desc" -> 1,
    		     "cover" -> 1,
    		     "categories" -> 1,
    		     "tags" -> 1,
    		     "start_time" -> 1,
    		     "end_time" -> 1,
    		     "venues" -> 1,
    		     "location" -> 1,
    		     "tickets" -> 1,
    		     "heat" -> 1,
    		     "related" -> 1,
    		     "travel_time" ->
    		     	BSONDocument("$divide" -> BSONArray("$location.distance", 3)),
    		     "arrival_time" ->
    		     	BSONDocument("$add" -> BSONArray(BSONDocument("$divide" -> 
    		     		BSONArray("$location.distance", 3)), "$start_time")),
    		     "explicit" -> 1,
    		     "version" -> 1))


      /*
       * Full text search index requerd for full text search to work
       * db.emails.ensureIndex({tags: "text", subject: "text", "body": "text"}, {
       *     name: "email_text_index",
       *         weight: {
       *               tags: 5,    // Assume folks are better at tagging than writing
       *               subject: 4, // Assume the subject is better than the body at description
       *               body: 1
       *          }
       *  })
       */
      matchPrams = mongoMultiMatcher(matchPrams, tags, "tags")
      matchPrams = mongoMultiMatcher(matchPrams, ids, "_id")
      matchPrams = mongoMultiMatcher(matchPrams, categories, "categories")
      matchPrams = mongoFalseFilter(matchPrams, explicit, "explicit")
      matchPrams = mongoFalseFilter(matchPrams, explicitVenues, "venues.explicit")
      
      newParmeter = BSONArray()
            
      start_time match {
      	case Some(start_time) => 
      		  matchPrams = matchPrams add BSONDocument("start_time" ->
      				  BSONDocument("$gte" -> start_time)) 
      	case None => None
      }
      end_time match {
      	case Some(end_time) => 
      		  matchPrams = matchPrams add BSONDocument("start_time" ->
      				  BSONDocument("$lt" -> end_time))
      	case None => None
      }
      /*
       * db.events.ensureIndex(
                           {
                             title: "text",
                             desc: "text",
                             categories: "text",
                             tags: "text"
                           }
                         )
       */
      q match {
        case Some(keyword) => {
          matchPrams = matchPrams add BSONDocument("$text" ->
        		BSONDocument("$search" -> keyword))
          sortPrams = sortPrams add BSONDocument("score" -> 
          		BSONDocument("$meta" -> "textScore"))
          }
        case None => None
      }
      var soon = false
      
       sort match {
        case Some(rawSort) => {
          val sortPramsSplit = rawSort.split(":")
          for(sPram <- sortPramsSplit){
            sPram match {
              case sPram if(sPram.head == '-') => sortPrams = sortPrams add BSONDocument(sPram.substring(1) -> -1)
              case "soon" => {
                parameters = parameters add soonProjet
                sortPrams = sortPrams add BSONDocument(
                  "arrival_time" -> 1)
                soon = true
              }
              case "-soon" => {
                parameters = parameters add soonProjet
                sortPrams = sortPrams add BSONDocument(
                  "travel_time" -> -1)
                soon = true
              }
              case _ => sortPrams = sortPrams add BSONDocument(sPram -> 1)
            }
          }
          parameters = parameters add BSONDocument("$sort" -> sortPrams)
        }
        case None => None
      }
      soon match {
        case soon if soon == true => matchPrams = matchPrams add BSONDocument("heat" ->
      				  BSONDocument("$gt" -> 0))
        case false => None
      }
      
      
     (q, categories, tags, start_time, end_time, explicit, explicitVenues) match {
        case (q, categories, tags, start_time, end_time, explicit, explicitVenues) 
        	if !(q.isEmpty &&
        	    categories.isEmpty && 
        	    tags.isEmpty && 
        	    start_time.isEmpty && 
        	    end_time.isEmpty &&
        	    explicit.isEmpty &&
        	    explicitVenues.isEmpty) => 
        	  parameters = parameters add BSONDocument(
        			"$match" ->  
      					BSONDocument("$and" -> matchPrams))
        case _ => None
      }
     

      parameters = parameters add BSONDocument("$limit" -> (limit + offset))
      parameters = parameters add BSONDocument("$skip" -> offset)
      log.info(BSONArray.pretty(parameters))
      val data = db.command(RawCommand(
        BSONDocument(
            "aggregate" -> collName,
            "pipeline" -> parameters
        ))
      ).map { doc => doc.getAs[List[M]]("result") }
      
      data
    }
    def bindByFbID(eid:String)(implicit ec: ExecutionContext)= 
      find(BSONDocument("facebook" -> eid))
      
    //def removeAll()(implicit ec: ExecutionContext) = collection.remove(BSONDocument.empty)
    def findAll()(implicit ec: ExecutionContext) = find(BSONDocument.empty)
  }

  // MongoDB collections:
  import models._
  object Persons extends UnsecuredDAO[Person]("person") with UUIDStringId {
    def findByName(name: String)(implicit ec: ExecutionContext) = find(BSONDocument("name" → name))
    def findByAge(age: Int)(implicit ec: ExecutionContext) = find(BSONDocument("age" → age))
  }
 /* object SUsers extends UnsecuredDAO[SUser]("users") with UUIDStringId {
    def checkUser(name:String, pass:String)(implicit ec: ExecutionContext)= 
      find(BSONDocument("name" -> name, "pass" -> pass))
  }*/
  
  object SCategories extends UnsecuredDAO[SCategory]("categories") with UUIDStringId {
    def findAllCategories()(implicit ec: ExecutionContext)= 
      find(BSONDocument())
  }
  object SEvents extends UnsecuredDAO[SEvent]("distevents") with UUIDStringId {
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
