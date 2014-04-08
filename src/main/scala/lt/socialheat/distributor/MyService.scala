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
import models.SEvent
import Mongo.SUsers
import spray.routing.authentication.BasicAuth
import spray.routing.authentication.UserPass
import reactivemongo.api.MongoDriver
import akka.actor.ActorSystem
import reactivemongo.core.nodeset.Authenticate
import shapeless.isDefined
import lt.socialheat.distributor.models.SUser
import scala.util.Success
import scala.util.Failure


class MyServiceActor extends Actor with MyService {
  def actorRefFactory = context
  def receive = runRoute(myRoute)
}

trait MyService extends HttpService {

  var user:Option[String] = None
  
  def myUserPassAuthenticator(userPass: Option[UserPass]):Future[Option[String]] = {
	    userPass match {
	      case Some(up) => 
	        SUsers.checkUser(up.user, up.pass).map {
	          users => users(0).id
	        }
	    }
  }
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
    pathPrefix("user"){
      path("events"){
        authenticate(BasicAuth(myUserPassAuthenticator _, realm = "Personalized evensts")) { userId =>
          user = Some(userId)
          getSEventRoute
        }
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


 protected lazy val getSEventRoute =
    parameter(
        'q ?,
        'categories ?,			//Filtering
        'explicit ? false, //skip that shit
        'explicitVenues.as[Boolean] ?,
        'tags ?,
        'skip ?, //@ TODO Check
        'start_time.as[Int] ?,
        'end_time.as[Int] ?,
        'location ?, //lat:long:proximity
        'sort ?,			//soon
        'locale ?, 
        'limit ? 10,
        'offset ? 0
        ) { 
    (q, categories, explicit, explicitVenues, tags, skip, start_time, end_time, location, sort, locale, limit, offset) =>
      detach() {
        complete {          
          var events = SEvents.findLimitedEvent(user, q, categories, Some(explicit), explicitVenues, tags, skip, start_time, end_time, location, sort, limit, offset)
          events
        }
      }
  }
  //val simpleCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("30 min"))  

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
