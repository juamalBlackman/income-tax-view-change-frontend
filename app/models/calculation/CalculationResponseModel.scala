/*
 * Copyright 2019 HM Revenue & Customs
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

package models.calculation

import enums.{CalcStatus, Crystallised, Estimate}
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait CalculationResponseModel

case class CalculationErrorModel(code: Int, message: String) extends CalculationResponseModel

object CalculationErrorModel {
  implicit val format: Format[CalculationErrorModel] = Json.format[CalculationErrorModel]
}

case class CalculationModel(calcID: String,
                            calcAmount: Option[BigDecimal],
                            calcTimestamp: Option[String],
                            crystallised: Option[Boolean],
                            incomeTaxNicYtd: Option[BigDecimal],
                            incomeTaxNicAmount: Option[BigDecimal],
                            calculationDataModel: Option[CalculationDataModel] = None
                           ) extends CalculationResponseModel with CrystallisedViewModel {

  val displayAmount: Option[BigDecimal] = (calcAmount, incomeTaxNicYtd) match {
    case (_, Some(result))    => Some(result)
    case (Some(result), None) => Some(result)
    case (None, None)         => None
  }

  val isBill: Boolean = crystallised.getOrElse(false)
  val status: CalcStatus = if(isBill) Crystallised else Estimate
}

case class CalculationResponseModelWithYear(model: CalculationResponseModel, year: Int) {
  val isError: Boolean = model match {
    case model: CalculationErrorModel => true
    case _ => false
  }

  val isCrystalised: Boolean = model match {
    case model: CalculationModel => model.status == Crystallised
    case _ => false
  }
}

object CalculationModel {
  implicit val writes: Writes[CalculationModel] = Json.writes[CalculationModel]

  implicit val reads: Reads[CalculationModel] = (
    (JsPath \\ "calcOutput" \ "calcID").read[String] and
    (JsPath \\ "calcOutput" \ "calcAmount").readNullable[BigDecimal] and
    (JsPath \\ "calcOutput" \ "calcTimestamp").readNullable[String] and
    (JsPath \\ "calcOutput" \ "crystallised").readNullable[Boolean] and
    (JsPath \\ "calcOutput" \ "calcResult" \ "incomeTaxNicYtd").readNullable[BigDecimal] and
    (JsPath \\ "calcOutput" \ "calcResult" \ "eoyEstimate" \ "incomeTaxNicAmount").readNullable[BigDecimal].orElse(Reads.pure(None)) and
      JsPath.read[CalculationDataModel].map(x => Option(x)).orElse(Reads.pure(None))
  ) (CalculationModel.apply _)
}
