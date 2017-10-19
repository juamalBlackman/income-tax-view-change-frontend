/*
 * Copyright 2017 HM Revenue & Customs
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

package services

import javax.inject.{Inject, Singleton}

import models._
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ImplicitListMethods

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class BTAPartialService @Inject()(val financialDataService: FinancialDataService) extends ImplicitListMethods {

  def getNextObligation(sources: IncomeSourcesModel)(implicit hc: HeaderCarrier): ObligationsResponseModel = {
    if (sources.allObligationsErrored) {
      Logger.warn("[BTAPartialService][getNextObligation] - No Obligations obtained")
      ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Could not retrieve obligations")
    } else {
      getMostRecentDueDate(
        sources.incomeSources.map(_.obligations)
          .collect{case o: ObligationsModel => o.obligations}.flatten
      )
    }
  }

  def getEstimate(nino: String, year: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {
    financialDataService.getLastEstimatedTaxCalculation(nino, year) map {
      case calc: LastTaxCalculation => calc
      case NoLastTaxCalculation => NoLastTaxCalculation
      case error: LastTaxCalculationError =>
        Logger.warn("[BTAPartialService][getNextObligation] - No LastCalc data retrieved")
        error
    }
  }

  private[BTAPartialService] def getMostRecentDueDate(obligationList: List[ObligationModel]): ObligationModel = {
    if(!obligationList.exists(!_.met)){
      obligationList.reduceLeft((x,y) => if(x.due isAfter y.due) x else y)
    } else {
      obligationList.filter(!_.met).reduceLeft((x,y) => if(x.due isBefore y.due) x else y)
    }
  }

}
