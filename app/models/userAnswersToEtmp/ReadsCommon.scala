/*
 * Copyright 2020 HM Revenue & Customs
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

package models.userAnswersToEtmp

import models._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import ReadsEstablisherIndividual.readsEstablisherIndividuals

import scala.annotation.tailrec

object ReadsCommon {

  def previousAddressDetails(addressYears: String, previousAddress: Option[Address],
                             tradingTime: Option[Boolean] = None): Option[PreviousAddressDetails] = {

    val tradingTimeAnswer = tradingTime.getOrElse(true)
    if (addressYears == "under_a_year" && tradingTimeAnswer) {
      Some(
        PreviousAddressDetails(isPreviousAddressLast12Month = true, previousAddress)
      )
    }
    else {
      None
    }
  }

  implicit val readsContactDetails: Reads[ContactDetails] = (
    (JsPath \ "emailAddress").read[String] and
      (JsPath \ "phoneNumber").read[String]
    ) ((email, phone) => ContactDetails(telephone = phone, email = email))

  implicit val readsPersonalDetails: Reads[PersonalDetails] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "date").read[String]
    ) ((firstName, middleName, lastName, dateOfBirth) =>
    PersonalDetails(
      None,
      firstName,
      middleName,
      lastName,
      dateOfBirth
    )
  )

  def readsPersonDetails(userAnswersBase: String): Reads[PersonalDetails] =
    (
      (JsPath \ userAnswersBase \ "firstName").read[String] and
        (JsPath \ userAnswersBase \ "lastName").read[String] and
        (JsPath \ "dateOfBirth").read[String]
      ) ((firstName, lastName, date) => PersonalDetails(None, firstName, None, lastName, date))

  case class Company(name: String, vatNumber: Option[String], payeNumber: Option[String], utr: Option[String],
                     noUtrReason: Option[String], crn: Option[String], noCrnReason: Option[String], address: Address,
                     contactDetails: ContactDetails, tradingTime: Option[Boolean], previousAddress: Option[Address], addressYears: String)

  def companyReads: Reads[Company] = (
    (JsPath \ "companyDetails" \ "companyName").read[String] and
      (JsPath \ "companyVat").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "companyPaye").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "companyRegistrationNumber").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noCrnReason").readNullable[String] and
      (JsPath \ "companyAddress").read[Address] and
      (JsPath \ "companyContactDetails").read[ContactDetails](readsContactDetails) and
      (JsPath \ "hasBeenTrading").readNullable[Boolean] and
      (JsPath \ "companyPreviousAddress").readNullable[Address] and
      ((JsPath \ "companyAddressYears").read[String] orElse (JsPath \ "trusteesCompanyAddressYears").read[String])
    ) (Company.apply _)



  case class PartnershipDetail(name: String, vat: Option[String], paye: Option[String], utr: Option[String], utrReason: Option[String],
                               address: Address, contact: ContactDetails, addressYears: String, previousAddress: Option[Address])

  def partnershipReads: Reads[PartnershipDetail] = (
    (JsPath \ "partnershipDetails" \ "name").read[String] and
      (JsPath \ "partnershipVat").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "partnershipPaye").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "partnershipAddress").read[Address] and
      (JsPath \ "partnershipContactDetails").read[ContactDetails] and
      (JsPath \ "partnershipAddressYears").read[String] and
      (JsPath \ "partnershipPreviousAddress").readNullable[Address]
    ) (PartnershipDetail.apply _)

  //noinspection ConvertExpressionToSAM
  def readsFiltered[T](isA: JsValue => JsLookupResult, readsA: Reads[T], detailsType: String): Reads[Seq[T]] = new Reads[Seq[T]] {
    override def reads(json: JsValue): JsResult[Seq[T]] = {
      json match {
        case JsArray(establishers) =>
          readFilteredSeq(JsSuccess(Nil), filterDeleted(establishers, detailsType), isA, readsA)
        case _ => JsSuccess(Nil)
      }
    }
  }

  private def filterDeleted(jsValueSeq: Seq[JsValue], detailsType: String): Seq[JsValue] = {
    jsValueSeq.filterNot { json =>
      (json \ detailsType \ "isDeleted").validate[Boolean] match {
        case JsSuccess(e, _) => e
        case _ => false
      }
    }
  }

  @tailrec
  private def readFilteredSeq[T](result: JsResult[Seq[T]], js: Seq[JsValue], isA: JsValue => JsLookupResult, reads: Reads[T]): JsResult[Seq[T]] = {
    js match {
      case Seq(h, t@_*) =>
        isA(h) match {
          case JsDefined(_) =>
            reads.reads(h) match {
              case JsSuccess(individual, _) => readFilteredSeq(JsSuccess(result.get :+ individual), t, isA, reads)
              case error@JsError(_) => error
            }
          case _ => readFilteredSeq(result, t, isA, reads)
        }
      case Nil => result
    }
  }





}
