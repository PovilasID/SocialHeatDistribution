package us.bleibinha.sprayreactivemongodbexample.models

import spray.json.DefaultJsonProtocol.jsonFormat3
import sprest.models.Model
import sprest.models.ModelCompanion
import sprest.reactivemongo.typemappers.jsObjectBSONDocumentWriter

case class Home(
    name: String,
    address: String)
case class Person(
  var id: Option[String] = None,
  name: String,
  age: Int,
  home: Option[Home]) extends Model[String]

object Person extends ModelCompanion[Person, String] {
  import sprest.Formats._
  implicit val homeJsonFormat = jsonFormat2(Home.apply _)
  implicit val personJsonFormat = jsonFormat4(Person.apply _)
}
