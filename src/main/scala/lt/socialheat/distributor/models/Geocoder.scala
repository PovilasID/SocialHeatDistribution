package lt.socialheat.distributor.models

import spray.json.{JsonFormat, DefaultJsonProtocol}

case class GCLocation(lat:Option[Double], lng:Option[Double])
case class GCGeometry(location: Option[GCLocation])
case class geocoderApiResult[T](status: String, results: List[T])

object geocoderJsonProtocol extends DefaultJsonProtocol {
  implicit val gCLocationFormat = jsonFormat2(GCLocation)
  implicit val gCGeometryFormat = jsonFormat1(GCGeometry)
  implicit def geocoderApiDataFormat[T :JsonFormat] = jsonFormat2(geocoderApiResult.apply[T])
}