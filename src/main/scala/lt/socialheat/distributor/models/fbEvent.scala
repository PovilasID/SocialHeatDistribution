package lt.socialheat.distributor.models

import spray.json.{JsonFormat, DefaultJsonProtocol}


case class fbECover(
    cover_id:	Option[Either[String,Int]],
    source:		Option[String]){
}

case class fbEVenue(
    name:			Option[String],
    latitude:		Option[Double],
    longitude:		Option[Double],
    city:			Option[String],
    state:			Option[String],
    country:		Option[String],
    id:				Option[Int],
    street:			Option[String],
    zip:			Option[String])

case class fbSummary(
    noreply_count:	Option[Int],
    maybe_count:	Option[Int],
    declined_count:	Option[Int],
    attending_count:Option[Int],
    count:			Option[Int])

case class fbEven (
    eid:				Option[Long],
    name:				Option[String],
    host:				Option[String],
    creator:			Option[Int],
    parent_group_id:	Option[Int],
    description:		Option[String],
    pic:				Option[String],
    pic_square:			Option[String],
    pic_cover:			Option[fbECover],
    start_time:			Option[String],
    end_time:			Option[String],
    timezone:			Option[String],
    location:			Option[String],
    venue:				Option[fbEVenue],
    all_members_count:	Option[Int],
    attending_count:	Option[Int],
    unsure_count:		Option[Int],
    declined_count:		Option[Int],
    ticket_uri:			Option[String],
    update_time:		Option[Long],
    summary:			Option[fbSummary])

case class fbApiData[T](data: List[T])

object fbEventJsonProtocol extends DefaultJsonProtocol {
  implicit val coverFormat = jsonFormat2(fbECover)
  implicit val venueFormat = jsonFormat9(fbEVenue)
  implicit val summaryFormat = jsonFormat5(fbSummary)
  implicit val eventFormat = jsonFormat21(fbEven)
  implicit def fbApiDataFormat[T :JsonFormat] = jsonFormat1(fbApiData.apply[T])
}