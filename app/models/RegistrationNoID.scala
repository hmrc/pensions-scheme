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

sealed trait Address

sealed trait Registrant

object Address {

 implicit val reads: Reads[Address] = {
   import play.api.libs.json._
   (__ \ "countryCode").read[String].flatMap {
     case "GB" => {
       UkAddress.format.map[Address](identity)
     }
     case _ =>
       ForeignAddress.format.map[Address](identity)
   }
  }

  implicit val writes: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.writes.writes(address)
    case address: ForeignAddress =>
      ForeignAddress.format.writes(address)
  }
}

case class ForeignAddress(addressLine1:String,addressLine2:String,
                     addressLine3:Option[String],addressLine4:Option[String],
                     postalCode:Option[String],countryCode:String) extends Address {

 // override val postCode: Option[String] = postalCode
}

object ForeignAddress {

  implicit val format: Format[ForeignAddress] = Json.format[ForeignAddress]
}


case class UkAddress(addressLine1:String,addressLine2:String,
                     addressLine3:Option[String],addressLine4:Option[String],
                     postalCode:String
                    ) extends Address {

 // override val postCode: Option[String] = Some(postalCode)
}

object UkAddress {

  implicit val format: Reads[UkAddress] =
    Json.reads[UkAddress]

  implicit val writes: Writes[UkAddress] = Writes {
    address =>
      Json.writes[UkAddress].writes(address) ++ Json.obj("countryCode" -> "GB")
  }
}


case class ContactDetailsType(phoneNumber:Option[String],
                              mobileNumber:Option[String],
                              faxNumber:Option[String],
                              emailAddress:Option[String])

object ContactDetailsType {

  implicit val format: Format[ContactDetailsType] = Json.format[ContactDetailsType]
}

case class IdentificationType(idNumber:String,issuingInstitution:String,issuingCountryCode:String)

object IdentificationType {

  implicit val format: Format[IdentificationType] = Json.format[IdentificationType]
}

case class Organisation(organisationName:String)

object Organisation {

  implicit val format: Format[Organisation] = Json.format[Organisation]
}


case class Individual(firstName:String,middleName:Option[String],lastName:String,dateOfBirth:String)


object Individual {

  implicit val format: Format[Individual] = Json.format[Individual]
}


case class IndividualRegistrant(regime:String,
                                acknowledgementReference:String,
                                isAnAgent:Boolean,
                                isAGroup:Boolean,
                                identification:Option[IdentificationType],
                                individual:Individual,
                                address:Address,
                                contactDetails:ContactDetailsType
                               ) extends Registrant

object IndividualRegistrant {

  implicit val format: Format[IndividualRegistrant] = Json.format[IndividualRegistrant]
}


case class OrganisationRegistrant(regime:String,
                                  acknowledgementReference:String,
                                  isAnAgent:Boolean,
                                  isAGroup:Boolean,
                                  identification:Option[IdentificationType],
                                  organisation:Organisation,
                                  address:Address,
                                  contactDetails:ContactDetailsType
                                 ) extends Registrant

object OrganisationRegistrant {

  implicit val format: Format[OrganisationRegistrant] = Json.format[OrganisationRegistrant]
}

object Registrant {

  implicit val reads: Reads[Registrant] = {
    IndividualRegistrant.format.map[Registrant](identity) orElse
      OrganisationRegistrant.format.map[Registrant](identity)
  }
  implicit val writes: Writes[Registrant] = Writes {
    case address: IndividualRegistrant =>
      IndividualRegistrant.format.writes(address)
    case address: OrganisationRegistrant =>
      OrganisationRegistrant.format.writes(address)
  }
}





