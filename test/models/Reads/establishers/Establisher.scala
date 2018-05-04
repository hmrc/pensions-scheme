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

package models.Reads.establishers

import models.EstablisherDetails
import models.Reads.establishers.EstablishersTestJson._
import play.api.libs.json.JsObject


sealed trait EstablisherType {
  def apiType: String
}

sealed trait CompanyType extends EstablisherType
sealed trait IndividualType extends EstablisherType

// API Types
//    Individual
//    Company/Org
//    Director
//    Partnership
//    Partner
//    Individual Trustee
//    Company/Org Trustee
//    Partnership Trustee

case object EstablisherIndividualType extends IndividualType {
  override val apiType: String = "Individual"
}

case object EstablisherCompanyType extends CompanyType {
  override val apiType: String = "Company/Org"
}

case object CompanyDirectorType extends IndividualType {
  override val apiType: String = "Director"
}

case object TrusteeIndividualType extends IndividualType {
  override val apiType: String = "Individual Trustee"
}

case object TrusteeCompanyType extends CompanyType {
  override val apiType: String = "Company/Org Trustee"
}

sealed trait Establisher {
  def json: JsObject
  def establishers: Seq[EstablisherDetails]
}

sealed trait SchemeEstablisher extends Establisher

case class EstablisherCompany(company: EstablisherDetails, directors: Seq[EstablisherDetails]) extends SchemeEstablisher {
  override def json: JsObject = establisherCompanyJson(company, directors)
  override def establishers: Seq[EstablisherDetails] = company +: directors
}

case class EstablisherIndividual(individual: EstablisherDetails) extends SchemeEstablisher {
  override def json: JsObject = establisherIndividualJson(individual)
  override def establishers: Seq[EstablisherDetails] = Seq(individual)
}

sealed trait Trustee extends Establisher

case class TrusteeCompany(company: EstablisherDetails) extends Trustee {
  override def json: JsObject = trusteeCompanyJson(company)
  override def establishers: Seq[EstablisherDetails] = Seq(company)
}

case class TrusteeIndividual(individual: EstablisherDetails) extends Trustee {
  override def json: JsObject = trusteeIndividualJson(individual)
  override def establishers: Seq[EstablisherDetails] = Seq(individual)
}

case class CompanyDirector(director: EstablisherDetails) extends Establisher {
  override def json: JsObject = companyDirectorJson(director)
  override def establishers: Seq[EstablisherDetails] = Seq(director)
}
