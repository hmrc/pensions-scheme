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

import models._
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

       "It is false then boxes 1,2,6 7,8 and 9 are false" in {
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
       "dormant field is true then box 4 is true and box5 is None" in {
         val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box4 mustBe Some(true)
         result.box5 mustBe None
       }
       "dormant field is false then box 5 is true and box4 is None" in {
         val result = (declaration + ("declarationDormant" -> JsBoolean(false))).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box4 mustBe None
         result.box5 mustBe Some(true)
       }
     }
     "if we do not have a dormant field" when {
       "box 4 and box 5 should be None" in {
         val result = Json.obj("declaration" -> JsBoolean(true), "declarationDuties" -> JsBoolean(true)).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box4 mustBe None
         result.box5 mustBe None
       }
     }
     "if we have a duties field" when {
       "It is true, then box10 will be true and box11 is None, addressAndContactDetails is None" in {
         val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box10 mustBe Some(true)
         result.box11 mustBe None
         result.addressAndContactDetails mustBe None
       }

       "It is false and containing 'adviser' details box11 is true, box10 is None and details populated" in {

         val advisorDetails = "adviserDetails" -> Json.obj("adviserName" -> JsString("John"),"phoneNumber" -> "07592113", "emailAddress" -> "test@test.com")
         val advisorAddress = "adviserAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
           "postcode" -> JsString("NE1"), "country" -> JsString("GB"))

         val name="John"
         val address=UkAddress("line1",Some("line2"),Some("line3"),Some("line4"),"GB","NE1")
         val contact=ContactDetails("07592113",None,None,"test@test.com")
         val declaration = Json.obj("declaration" -> JsBoolean(true), "declarationDormant" -> JsBoolean(true), "declarationDuties" -> JsBoolean(false))
         val result = (declaration + advisorDetails + advisorAddress).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

         result.box10 mustBe None
         result.box11 mustBe Some(true)
         result.pensionAdviserName mustBe Some(name)
         result.addressAndContactDetails mustBe Some(AddressAndContactDetails(address,contact))
       }

       "if we have a duties field and is false but no contact or name details then addressAndContactDetails be None" in {
         val result = (declaration + ("declarationDuties" -> JsBoolean(false))).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
         result.box11 mustBe Some(true)
         result.box10 mustBe None
         result.addressAndContactDetails mustBe None
         result.pensionAdviserName mustBe None

       }

     }

   }
 }
}
