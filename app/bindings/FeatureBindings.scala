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

///*
// * Copyright 2018 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package bindings
//
///*
// * Copyright 2018 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//import play.api.inject.{Binding, Module}
//import play.api.{Configuration, Environment, Logger}
//import service.{SchemeService, SchemeServiceV1, SchemeServiceV2}
//
//class FeatureBindings extends Module {
//
//  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
//
//    registerSchemeJsonVersion(configuration)
//
//  }
//
//  private def registerSchemeJsonVersion(configuration: Configuration): Seq[Binding[_]] = {
//
//    configuration.underlying.getString("feature.registerSchemeJsonVersion") match {
//      case "v1" =>
//        Logger.debug("SchemeService bound to SchemeServiceV1")
//        Seq(bind[SchemeService].to[SchemeServiceV1])
//
//      case "v2" =>
//        Logger.debug("SchemeService bound to SchemeServiceV2")
//        Seq(bind[SchemeService].to[SchemeServiceV2])
//
//      case _ =>
//        Logger.warn("No application configuration for feature.registerSchemeJsonVersion, defaulting to 'v1'")
//        Seq(bind[SchemeService].to[SchemeServiceV1])
//    }
//
//  }
//
//}
