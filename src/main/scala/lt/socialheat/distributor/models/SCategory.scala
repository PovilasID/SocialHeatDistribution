package lt.socialheat.distributor.models

import spray.json.{JsonFormat, DefaultJsonProtocol}
import sprest.models.Model
import sprest.models.ModelCompanion
import sprest.reactivemongo.typemappers.SprayJsonTypeMapper

case class SCategory(var id:Option[String] = None, name:Option[String]) extends Model[String]


object SCategory extends ModelCompanion[SCategory, String] with SprayJsonTypeMapper {
  import sprest.Formats._
  implicit val sCategoryFormat = jsonFormat2(SCategory.apply _)
}