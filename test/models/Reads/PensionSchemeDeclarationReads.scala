/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package models.Reads

import models.{PensionSchemeDeclaration, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsBoolean, JsString, Json}

class PensionSchemeDeclarationReads extends WordSpec with MustMatchers with OptionValues with Samples {
 "A json payload containing declarations" should {
   "Map correctly to a Declaration model" when {
     val declaration = Json.obj("declaration" -> JsBoolean(true), "declarationDormant" -> JsBoolean(true), "declarationDuties" -> JsBoolean(true))
     "We have a declaration field" when {
       "It is true then boxes 1,2,6 7,8 and 9 are true" in {
         val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box1 mustBe true
         result.box2 mustBe true
         result.box6 mustBe true
         result.box7 mustBe true
         result.box8 mustBe true
         result.box9 mustBe true
       }

       "It is true then boxes 1,2,6 7,8 and 9 are false" in {
         val result = (declaration + ("declaration" -> JsBoolean(false))).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box1 mustBe false
         result.box2 mustBe false
         result.box6 mustBe false
         result.box7 mustBe false
         result.box8 mustBe false
         result.box9 mustBe false
       }
     }
     "if we have a dormant field" when {
       "if we have a company and its dormant field is true" in {
         val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box4 mustBe Some(true)
       }
       "if we have a company and its dormant field is false" in {
         val result = (declaration + ("declarationDormant" -> JsBoolean(false))).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box5 mustBe Some(true)
       }
     }
     "if we do not have a dormant field" when {
       "box 4 and box 5 should be none" in {
         val result = Json.obj("declaration" -> JsBoolean(true), "declarationDuties" -> JsBoolean(true)).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box4 mustBe None
         result.box5 mustBe None
       }
     }
     "if we have a duties field" when {
       "if we have a duties field and is true" in {
         val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box10 mustBe Some(true)
       }
       "if we have a duties field and is false" in {
         val result = (declaration + ("declarationDuties" -> JsBoolean(false))).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box11 mustBe Some(true)
       }

       "set as true and containing 'adviser' details" in {
         val advisorDetails = "advisorDetails" -> Json.obj("name" -> JsString("John"),"phone" -> "07592113", "email" -> "test@test.com")

         val advisorAddress = "advisorAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
           "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

         val result = (declaration + advisorDetails + advisorAddress).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box11 mustBe Some(true)
       //  result.pensionAdvisorDetail.value mustBe pensionAdvisorSample

       }


     }
     "if we do not have a duties field" when {
       "box 10 and box 11 should be none" in {
         val result = Json.obj("declaration" -> JsBoolean(true), "declarationDormant" -> JsBoolean(true)).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box10 mustBe None
         result.box11 mustBe None
       }
     }

   }
 }
}
