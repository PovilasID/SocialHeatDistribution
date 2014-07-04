package lt.socialheat.distributor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.Actor
import models.Person
import models.Person.personJsonFormat
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.LastError
import spray.http.ContentTypes
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.json.pimpAny
import spray.routing.HttpService
import spray.routing.Directive.pimpApply
import spray.routing.directives.DetachMagnet.fromUnit
import spray.routing.directives.ParamDefMagnet.apply
import Mongo.SEvents
import Mongo.SCategories
import models.SEvent
import lt.socialheat.distributor.models.SCategory


class MyServiceActor extends Actor with MyService {
  def actorRefFactory = context
  def receive = runRoute(myRoute)
}

trait MyService extends HttpService {

lazy val myRoute =
    path("person") {
      put {
        putRoute
      } ~
        get {
          getRoute
        } ~
        delete {
          deleteRoute
        }
    } ~
    path("events") {
      get {
        getSEventRoute
      } ~
      put {
        putSEventRoute
      }
    } ~
    path("categories"){
      get{
        getSCategoryRoute
      }
    }~
    path("trigger"){
      get{
        getTriggerFB
      }
    }

  protected lazy val putRoute =
    entity(as[Person]) { person ⇒
      detach() {
        complete {
          //Persons.add(person)
          "Success"
        }
      }
    }
 protected lazy val putSEventRoute =
    entity(as[SEvent]) { sEvent ⇒
      detach() {
        complete {
          SEvents.add(sEvent)
        }
      }
    }
 import Akka.actorSystem
  protected lazy val getTriggerFB =
      detach() {
        complete {
          //val fbData = FbGet
          "Succes"
        }
      }
 
  protected lazy val getSCategoryRoute = 
      parameter('offset ? 0, 'limit ? 0){
        (offset, limit) =>
          validate(offset >= 0, "query parameter 'offset' must be >= 0")
          validate(limit >= 0, "query parameter 'limit' must be >= 0") {
            complete{
              //val sCategoryDal = new sCategoryDal
              //val sCategories = sCategoryDal.findSCategories(offset, limit)
              //sCategories
              SCategories.findAllCategories()
              // @ TODO add categories handler
            }
          }
  		}
  protected lazy val getSEventRoute =
    parameter(
        'q ?,
        'categories ?,			//Filtering
        'explicit ? false, //skip that shit
        'explicitVenues.as[Boolean] ?,
        'tags ?,
        'skip ?, //@ TODO Check
        'start_time.as[Long] ?,
        'end_time.as[Long] ?,
        'location ?, //lat:long:proximity
        'sort ?,			//soon
        'locale ?, 
        'limit ? 10,
        'offset ? 0
        ) { 
    (q, categories, explicit, explicitVenues, tags, skip, start_time, end_time, location, sort, locale, limit, offset) =>
      detach() {
        complete {
          /*SEvents.findEvents(categories,
              explicit,
              tags,
              skip,
              start_date,
              end_date,
              location,
              sort,
              limit,
              offset)*/
          
          var events = SEvents.findLimitedEvent(q, categories, Some(explicit), explicitVenues, tags, skip, start_time, end_time, location, sort, limit, offset)
          events
        }
      }
  }
  
  
  protected lazy val getRoute =
    parameters('id.as[String]) { id ⇒
      detach() {
        complete {
          val person = Some() //Persons.findById(id)
          person map { person ⇒
            person match {
              /*case Some(person) ⇒
                HttpResponse(
                  StatusCodes.OK,
                  HttpEntity(ContentTypes.`application/json`, person.toJson.prettyPrint)
                )*/
              case _ ⇒
                HttpResponse(StatusCodes.BadRequest)
            }
          }
        }
      }
    } ~
      parameters('name.as[String]) { name ⇒
        detach() {
          complete {
            //Persons.findByName(name)
            "Success"
          }
        }
      } ~
      parameters('age.as[Int]) { age ⇒
        detach() {
          complete {
            //Persons.findByAge(age)
            "Success"
          }
        }
      } ~
      detach() {
        complete {
          //Persons.findAll()
          "Success"
        }
      }

  protected lazy val deleteRoute =
    detach() {
      dynamic {
        //Persons.removeAll()
        complete(StatusCodes.OK)
      }
    }
}
