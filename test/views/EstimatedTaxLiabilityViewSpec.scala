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

package views

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import assets.EstimatesTestConstants._
import assets.FinancialTransactionsTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import models.calculation.CalculationDataModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import implicits.ImplicitCurrencyFormatter._
import implicits.ImplicitDateFormatter
import play.api.mvc.Request
import testUtils.TestSupport


class EstimatedTaxLiabilityViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), singleBusinessIncome)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), propertyIncomeOnly)(FakeRequest())

  override def beforeEach(): Unit = {
    mockAppConfig.features.calcBreakdownEnabled(true)
  }

  private def pageSetup(calcDataModel: CalculationDataModel, user: MtdItUser[_])(implicit request: Request[_] = FakeRequest()) = new {
    lazy val page: HtmlFormat.Appendable = views.html.estimatedTaxLiability(
      calculationDisplaySuccessModel(calcDataModel),
      testYear)(request, applicationMessages, mockAppConfig, user)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    lazy val cPage: HtmlFormat.Appendable = views.html.crystallised(
      calculationDisplaySuccessCrystalisationModel(calcDataModel), transactionModel(),
      testYear)(FakeRequest(), applicationMessages, mockAppConfig, user)
    lazy val cDocument: Document = Jsoup.parse(contentAsString(cPage))

    lazy val noBreakdownPage: HtmlFormat.Appendable = views.html.estimatedTaxLiability(
      calculationDisplayNoBreakdownModel, testYear
    )(request, applicationMessages, mockAppConfig, user)

    lazy val noBreakdownDocument: Document = Jsoup.parse(contentAsString(noBreakdownPage))

    implicit val model: CalculationDataModel = calcDataModel


    def personalAllowance: String = "-" + (
      model.annualAllowances.personalAllowance
      ).toCurrencyString
  }

  "breadcrumb trail" should {

    "have a breadcrumb trail with estimate when there is more than one estimate tax year" in {
      implicit val request : Request[_] = FakeRequest().withSession("singleEstimate"->"false")
      val setup = pageSetup(busPropBRTCalcDataModel, bizAndPropertyUser)(request)
      import setup._
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-estimates").text shouldBe breadcrumbMessages.estimates
      document.getElementById("breadcrumb-it-estimate").text shouldBe breadcrumbMessages.itEstimate(testYear)
    }

    "have a breadcrumb trail without estimate when there is only one estimate tax year" in {
      implicit val request : Request[_] = FakeRequest().withSession("singleEstimate"->"true")
      val setup = pageSetup(busPropBRTCalcDataModel, bizAndPropertyUser)(request)
      import setup._
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      Option(document.getElementById("breadcrumb-estimates")) shouldBe None
      document.getElementById("breadcrumb-it-estimate").text shouldBe breadcrumbMessages.itEstimate(testYear)
    }
  }

  "The EstimatedTaxLiability view" should {

    val setup = pageSetup(busPropBRTCalcDataModel, bizAndPropertyUser)(FakeRequest())
    import setup._
    val messages = new Messages.Calculation(testYear)

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }



    s"have the tax year '${messages.subheading}'" in {
      document.getElementById("sub-heading").text() shouldBe messages.subheading
    }

    s"have the page heading '${messages.heading}'" in {
      document.getElementById("heading").text() shouldBe messages.heading
    }

    s"has a paragraph to explain the reported figures '${messages.reportedFigures}'" in {
      document.getElementById("reported-figures").text() shouldBe messages.reportedFigures
    }

    s"have an Estimated Tax Liability section" which {

      lazy val estimateSection = document.getElementById("estimated-tax")

      "has a section for EoY Estimate" which {

        lazy val eoySection = estimateSection.getElementById("eoyEstimate")

        s"has the correct Annual Tax Amount Estimate Heading of '${
          messages.EoyEstimate.heading(busPropBRTCalcDataModel.eoyEstimate.get.totalNicAmount.toCurrencyString)
        }" in {
          eoySection.getElementById("eoyEstimateHeading").text shouldBe
            messages.EoyEstimate.heading(busPropBRTCalcDataModel.eoyEstimate.get.totalNicAmount.toCurrencyString)
        }

        s"has the correct estimate p1 paragraph '${messages.EoyEstimate.p1}'" in {
          eoySection.getElementById("eoyP1").text shouldBe messages.EoyEstimate.p1
        }
      }

      "has a section for In Year (Current) Estimate" which {

        lazy val inYearSection = estimateSection.getElementById("inYearEstimate")

        s"has the correct Annual Tax Amount Estimate Heading of" +
          s" '${messages.InYearEstimate.heading(busPropBRTCalcDataModel.totalIncomeTaxNicYtd.toCurrencyString)}" in {
          inYearSection.getElementById("inYearEstimateHeading").text shouldBe
            messages.InYearEstimate.heading(busPropBRTCalcDataModel.totalIncomeTaxNicYtd.toCurrencyString)
        }

        s"has the correct estimate p1 paragraph '${messages.InYearEstimate.p1(lastTaxCalcSuccess.calcTimestamp.get.toLocalDateTime.toLongDateTime)}'" in {
          inYearSection.getElementById("inYearP1").text shouldBe
            messages.InYearEstimate.p1(lastTaxCalcSuccess.calcTimestamp.get.toLocalDateTime.toLongDateTime)
        }

        "has progressive disclosure for why there estimate might change" which {

          s"has the heading '${messages.InYearEstimate.WhyThisMayChange.heading}'" in {
            inYearSection.getElementById("whyEstimateMayChange").text shouldBe messages.InYearEstimate.WhyThisMayChange.heading
          }

          s"has the p1 heading '${messages.InYearEstimate.WhyThisMayChange.p1}'" in {
            inYearSection.getElementById("whyMayChangeP1").text shouldBe messages.InYearEstimate.WhyThisMayChange.p1
          }

          s"has the 1st bullet '${messages.InYearEstimate.WhyThisMayChange.bullet1}'" in {
            inYearSection.select("#whyMayChange ul li:nth-child(1)").text shouldBe messages.InYearEstimate.WhyThisMayChange.bullet1
          }

          s"has the 2nd bullet '${messages.InYearEstimate.WhyThisMayChange.bullet2}'" in {
            inYearSection.select("#whyMayChange ul li:nth-child(2)").text shouldBe messages.InYearEstimate.WhyThisMayChange.bullet2
          }

          s"has the 2nd bullet '${messages.InYearEstimate.WhyThisMayChange.bullet3}'" in {
            inYearSection.select("#whyMayChange ul li:nth-child(3)").text shouldBe messages.InYearEstimate.WhyThisMayChange.bullet3
          }
        }
      }
    }

    "have the calculation breakdown section visible" when {
      "the feature switch is enabled" in {
        mockAppConfig.features.calcBreakdownEnabled(true)
        val setup = pageSetup(justBusinessCalcDataModel, bizUser)(FakeRequest())
        import setup._
        Option(document.getElementById("inYearCalcBreakdown")).isDefined shouldBe true
      }
    }

    "have the calculation breakdown section hidden" when {
      "the feature switch is disabled" in {
        mockAppConfig.features.calcBreakdownEnabled(false)
        val setup = pageSetup(justBusinessCalcDataModel, bizUser)(FakeRequest())
        import setup._
        Option(document.getElementById("inYearCalcBreakdown")).isDefined shouldBe false
      }
      "the breakdown is empty" in {
        mockAppConfig.features.calcBreakdownEnabled(false)
        val setup = pageSetup(justBusinessCalcDataModel, bizUser)(FakeRequest())
        import setup._
        Option(noBreakdownDocument.getElementById("inYearCalcBreakdown")).isDefined shouldBe false
      }
    }
  }
}
