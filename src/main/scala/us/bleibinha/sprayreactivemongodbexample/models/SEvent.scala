package us.bleibinha.sprayreactivemongodbexample.models

import spray.json.DefaultJsonProtocol.jsonFormat3
import sprest.models.Model
import sprest.models.ModelCompanion
import sprest.reactivemongo.typemappers.jsObjectBSONDocumentWriter

case class GeoJson(
    `type`: 		String, //Prepear 
    coordinates:	Either[List[Double],List[List[Double]]])
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
    var id: Option[String] = None,
    nid: 		Option[Int],
    title: 		Option[String],
    desc: 		Option[String],
    cover:		Option[String],
    start_time:	Option[String], //@ TODO Change to date if increase performance
    end_time:	Option[String],
    categories:	Option[List[String]],
    tags:		Option[List[String]],
    heat:		Option[Int],
    facebook:	Option[String],
    lastfm:		Option[String],
    eventbrite:	Option[String],
    foursquare:	Option[String],
    venues:		Option[List[SVenue]],
    location:	Option[GeoJson],
    related:	Option[List[Option[List[String]]]]) extends Model[String]

//@ TODO Add media: images, videos, links etc...

object SEvent extends ModelCompanion[SEvent, String] {
  import sprest.Formats._
  implicit val geoJsonJsonFormat = jsonFormat2(GeoJson.apply _)
  implicit val sVHoursJsonFormat = jsonFormat2(SVHours.apply _)
  implicit val sVenueJsonFormat = jsonFormat8(SVenue.apply _) 
  implicit val sEventJsonFormat = jsonFormat17(SEvent.apply _)
}
