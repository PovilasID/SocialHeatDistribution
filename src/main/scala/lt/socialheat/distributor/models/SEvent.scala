package lt.socialheat.distributor.models

import sprest.models.Model
import sprest.models.ModelCompanion
import spray.httpx.unmarshalling.{Unmarshaller}
import spray.util._
import spray.http._
import org.joda.time.DateTime
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSONReader
import reactivemongo.bson.BSONWriter
import spray.json.JsonFormat
import spray.json.JsNumber
import spray.json.JsValue
import sprest.reactivemongo.typemappers.SprayJsonTypeMapper
import spray.json.JsString
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONBoolean
import spray.json.JsFalse
import spray.json.JsTrue
import spray.json.JsNull
import reactivemongo.bson.BSONNull
import spray.json.JsArray
import spray.json.JsObject
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONValue
import reactivemongo.bson.BSONObjectID
import spray.json.JsBoolean
import reactivemongo.bson.BSONDateTime

case class GeoJson(
    `type`: 		Option[String], //Prepear 
    coordinates:	Option[/*Either[*/List[Option[Double]]/*,List[List[Double]]]*/],
    distance: Option[Double] = None)
case class SVHours(
    days:		Option[List[Int]],
    hours:		Option[List[String]])
case class SVenue(
    title:			Option[String] = None,
    phones:			Option[List[String]] = None,
    webites:		Option[List[String]] = None,
    country:		Option[String] = None,
    city:			Option[String] = None,
    street:			Option[String] = None,
    zip:			Option[String] = None,
    working_hours:	Option[SVHours] = None,
    explicit:		Option[Boolean] = Some(false))
case class SEvent(
    var id: 	Option[String] = None,
    nid: 		Option[Int] = None,
    title: 		Option[String] = None,
    desc: 		Option[String] = None,
    cover:		Option[String] = None,
    start_time:	Option[Long] = None, //@ TODO Change to date if increase performance
    end_time:	Option[Long] = None,
    categories:	Option[List[String]] = None,
    tags:		Option[List[String]] = None,
    heat:		Option[Int] = None,
    facebook:	Option[String] = None,
    lastfm:		Option[String] = None,
    eventbrite:	Option[String] = None,
    foursquare:	Option[String] = None,
    venues:		Option[List[SVenue]] = None,
    location:	Option[GeoJson] = None,
    tickets:	Option[List[String]] = None,
    related:	Option[List[List[String]]] = None,
    explicit:	Option[Boolean] = Some(false),
    version:	Option[Long] = None) extends Model[String]

//@ TODO Add media: images, videos, links etc...

/*object fbUnmarshaling{
  val `application/vnd.acme.person` =
  MediaTypes.register(MediaType.custom("application/vnd.acme.person"))

  implicit val SimplerPersonUnmarshaller =
  	Unmarshaller.delegate[String, Person](`application/json`) { string =>
  		val Array(_, name, first, age) = string.split(":,".toCharArray).map(_.trim)
  	SEvent(name, first, age.toInt)
  }
}*/
object SEvent extends ModelCompanion[SEvent, String] with SprayJsonTypeMapper {
  import sprest.Formats._
  implicit val geoJsonJsonFormat = jsonFormat3(GeoJson.apply _)
  implicit val sVHoursJsonFormat = jsonFormat2(SVHours.apply _)
  implicit val sVenueJsonFormat = jsonFormat9(SVenue.apply _) 
  implicit val sEventJsonFormat = jsonFormat20(SEvent.apply _)
  /*implicit val geoJsonHandler = Macros.handler[GeoJson]
  implicit val sVHoursHandler = Macros.handler[SVHours]
  implicit val sVenueHandler = Macros.handler[SVenue]
  implicit val containerHandler = Macros.handler[SEvent]*/
}


