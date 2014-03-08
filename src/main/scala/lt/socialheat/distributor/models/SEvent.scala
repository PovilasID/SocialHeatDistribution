package lt.socialheat.distributor.models

import sprest.models.Model
import sprest.models.ModelCompanion
import spray.httpx.unmarshalling.{Unmarshaller}
import spray.util._
import spray.http._
import org.joda.time.DateTime

case class GeoJson(
    `type`: 		Option[String], //Prepear 
    coordinates:	Option[/*Either[*/List[Double]/*,List[List[Double]]]*/],
    distance: Option[Double])
case class SVHours(
    days:		Option[List[Int]],
    hours:		Option[List[String]])
case class SVenue(
    title:			Option[String],
    phones:			Option[List[String]],
    webites:		Option[List[String]],
    country:		Option[String],
    city:			Option[String],
    street:			Option[String],
    zip:			Option[String],
    working_hours:	Option[SVHours])
case class SEvent(
    var id: 	Option[String] = None,
    nid: 		Option[Int],
    title: 		Option[String],
    desc: 		Option[String],
    cover:		Option[String],
    start_time:	Option[DateTime], //@ TODO Change to date if increase performance
    end_time:	Option[DateTime],
    categories:	Option[List[String]],
    tags:		Option[List[String]],
    heat:		Option[Int],
    facebook:	Option[String],
    lastfm:		Option[String],
    eventbrite:	Option[String],
    foursquare:	Option[String],
    venues:		Option[List[SVenue]],
    location:	Option[GeoJson],
    related:	Option[List[List[String]]],
    version:	Option[Long]) extends Model[String]

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
object SEvent extends ModelCompanion[SEvent, String] {
  import sprest.Formats._
  implicit val geoJsonJsonFormat = jsonFormat3(GeoJson.apply _)
  implicit val sVHoursJsonFormat = jsonFormat2(SVHours.apply _)
  implicit val sVenueJsonFormat = jsonFormat8(SVenue.apply _) 
  implicit val sEventJsonFormat = jsonFormat18(SEvent.apply _)
  
  
  /*implicit val geoJsonHandler = Macros.handler[GeoJson]
  implicit val sVHoursHandler = Macros.handler[SVHours]
  implicit val sVenueHandler = Macros.handler[SVenue]
  implicit val containerHandler = Macros.handler[SEvent]*/
}
