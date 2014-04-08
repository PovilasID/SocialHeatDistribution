package lt.socialheat.distributor.models

import spray.json.DefaultJsonProtocol.jsonFormat3
import sprest.models.Model
import sprest.models.ModelCompanion
import sprest.reactivemongo.typemappers.jsObjectBSONDocumentWriter

case class SUser(
  var id: Option[String] = None,
  name: String,
  pass: String) extends Model[String]

object SUser extends ModelCompanion[SUser, String] {
  import sprest.Formats._
  implicit val sUserJsonFormat = jsonFormat3(SUser.apply _)
}
