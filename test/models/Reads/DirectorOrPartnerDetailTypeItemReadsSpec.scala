package models.Reads

import models.DirectorOrPartnerDetailTypeItem
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsString, Json}

class DirectorOrPartnerDetailTypeItemReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "JSON Payload of a Director" should  {
    "Map correctly into a DirectorOrPartnerDetailTypeItem" when {
      "We have a first name" in {
        val directors = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("Test")))

        val result = directors.as[DirectorOrPartnerDetailTypeItem]


      }
    }
  }
}
