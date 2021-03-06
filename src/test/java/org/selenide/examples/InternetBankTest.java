package org.selenide.examples;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.codeborne.xlstest.XLS;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.FileDownloadMode.PROXY;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selenide.*;
import static org.junit.Assert.assertThat;

@Owner("donald.duck")
public class InternetBankTest {

  @BeforeClass
  public static void setUp() {
    closeWebDriver();
    Configuration.baseUrl = "https://idemo.bspb.ru";
    Configuration.proxyEnabled = true;
    Configuration.proxyHost = "0.0.0.0";
    Configuration.fileDownload = PROXY;
    SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(true).savePageSource(true));
  }

  @Before
  public void login() {
    open("/");

    $(By.name("username")).val("demo");
    $(By.name("password")).val("demo").pressEnter();

    enter2ndFactor();
  }

  private void enter2ndFactor() {
    $("#otp-code-text").shouldBe(visible);

    if ($(byId("login-crypto-button")).isDisplayed()) {
      $(byId("login-crypto-button")).click();
    } else {
      $(byName("otpCode")).val("0000").pressEnter();
    }
  }

  @Test
  public void userCanLoginToBspbDemo() {
    $("#user-greeting").shouldHave(text("Hello World!"));
  }

  @Test
  @Feature("search")
  public void userCanLoadStatementForLastMonth() {
    open("/bank/overview");

    $("#accounts .account a").click();
    $(byClassName("page-header")).shouldHave(text("Statement"));
    $("#defined-periods").find(byText("Beginning of last month until today")).click();
  }

  @Test
  public void userCanViewTransactionDetails() {
    openStatement();
    $(".statement-container .transaction-row.tx-debit .counterparty-name").scrollTo().click();
    $("#transaction-dialog #transaction-header").shouldHave(text("Transaction details"));

    $("#transaction-dialog #payment-beneficiary")
            .shouldHave(text("Beneficiary"))
            .shouldHave(cssValue("border-bottom-color", "red"))
            .shouldHave(attribute("title", "Payment"))
            .shouldHave(cssClass("payment"));

    $("#transaction-dialog #beneficiary-account").shouldHave(
            text("Beneficiary account"),
            text("***** *** * **** ***0000")
    );
  }

  @Test
  @Ignore
  public void userCanDownloadStatementAsPdf() throws IOException {
    openStatement();

    $(byId("statement-export")).click();

    File statementPdf = $(byId("btn-export-pdf")).download();

    PDF pdf = new PDF(statementPdf);
    assertThat(pdf, PDF.containsText("ПАО \"БАНК \"САНКТ-ПЕТЕРБУРГ\""));
    assertThat(pdf, PDF.containsText("BIC 044030790"));
    assertThat(pdf, PDF.containsText("Королёва Ольга"));
    assertThat(pdf, PDF.containsText("Statement Period:"));
  }

  @Feature("bank")
  @Test
  @Ignore
  public void userCanDownloadStatementAsExcel() throws IOException {
    openStatement();

    $(byId("statement-export")).click();

    File statementExcel = $("#btn-export-xls").download();

    XLS xls = new XLS(statementExcel);
    assertThat(xls, XLS.containsText("Королёва Ольга"));
    assertThat(xls, XLS.containsText("40817810048000102279"));
    assertThat(xls, XLS.containsText("Closing balance"));
    assertThat(xls, XLS.containsText("Credit turnover"));
    assertThat(xls, XLS.containsText("Debit turnover"));
    assertThat(xls, XLS.containsText("Reserved"));
  }

  @Test
  @Feature("bank")
  public void userCanAssignAliasForAccount() {
    open("/bank/overview");
    SelenideElement account = $("#accounts .account", 2).scrollTo();

    account.find("a.alias")
            .shouldHave(or("account or alias", text("50817 810 0 4800 0104467"), text("Это типа счёт"))
                    .because("the alias could already be changed by previous test run"))
            .hover();

    account.find(byClassName("icon-edit")).should(appear).click();

    account.find(By.name("alias"))
            .shouldHave(cssValue("border-bottom-color", "red"))
            .val("Это типа счёт")
            .pressEnter();

    account.shouldHave(text("Это типа счёт"));

    account.shouldNotHave(text("50817 810 0 4800 0104467"));
  }

  private void openStatement() {
    open("/statement");

    $(byId("defined-periods")).find(byText("Beginning of last month until today")).click();
  }
}
