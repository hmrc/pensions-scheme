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

package models

import play.api.libs.json._

case class ForeignAddress(addressLine1:String,addressLine2:String,
                     addressLine3:Option[String],addressLine4:Option[String],
                     postalCode:Option[String],countryCode:String)

object ForeignAddress {

  implicit val format: Format[ForeignAddress] = Json.format[ForeignAddress]
}


case class ContactDetailsType(phoneNumber:Option[String],
                              emailAddress:Option[String])

object ContactDetailsType {

  implicit val format: Format[ContactDetailsType] = Json.format[ContactDetailsType]
}

case class Organisation(organisationName:String)

object Organisation {

  implicit val format: Format[Organisation] = Json.format[Organisation]
}

case class OrganisationRegistrant(regime:String,
                                  acknowledgementReference:String,
                                  isAnAgent:Boolean = false,
                                  isAGroup:Boolean = false,
                                  organisation:Organisation,
                                  address:ForeignAddress,
                                  contactDetails:ContactDetailsType
                                 )

object OrganisationRegistrant {

  implicit val format: Format[OrganisationRegistrant] = Json.format[OrganisationRegistrant]
}






