package lt.socialheat.distributor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.Actor
import models.Person
import models.Person.personJsonFormat
//import Mongo.Persons
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
import lt.socialheat.distributor.Mongo._
import lt.socialheat.distributor.models.SEvent
import spray.routing.Directive.pimpApply
import spray.routing.directives.DetachMagnet.fromUnit
import spray.routing.directives.ParamDefMagnet.apply


class MyServiceActor extends Actor with MyService {
  def actorRefFactory = context
  def receive = runRoute(myRoute)
}

trait MyService extends HttpService {

  import lt.socialheat.distributor.Mongo
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
  protected lazy val getSEventRoute =
    parameter('categories ?,			//Filtering 
        'explicit ? false, //skip that shit
        'tags ?,
        'skip ?,
        'start_time ?,
        'end_time ?,
        'location ?, //lat:long:proximity
        'sort ?,			//soon
        'limit ? 10,
        'offset ? 0
        ) { 
    (categories, explicit, tags, skip, start_time, end_time, location, sort, limit, offset) =>
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
          var events = SEvents.findLimitedEvent(categories, tags, start_time, end_time, location, sort, limit, offset)
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
