package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.ConfigurationReader;
import com.library.utility.DB_Util;
import com.library.utility.LibraryAPI_Util;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


public class ApiStepDef  {


    String token;

    String acceptHeader;
    String pathParam;

    Response response;
    RequestSpecification reqSpec;
    ValidatableResponse thenPart;


    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {
        token = LibraryAPI_Util.getToken(userType);
        System.out.println("token = " + token);
        reqSpec=given().log().uri()
                .header("x-library-token",token);


    }

    @Given("Accept header is {string}")
    public void accept_header_is(String acceptHeader) {

       this.acceptHeader = acceptHeader;

        reqSpec = reqSpec.accept(acceptHeader);



    }

    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {

        response = reqSpec.when().get(ConfigurationReader.getProperty("library.baseUri") + endpoint).prettyPeek();

        thenPart = response.then();
    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expectedStatusCode) {
        thenPart.statusCode(expectedStatusCode);
    }

    @Then("Response Content type is {string}")
    public void response_content_type_is(String expectedContentType) {

        thenPart.contentType(expectedContentType);
    }

    @Then("{string} field should not be null")
    public void field_should_not_be_null(String field) {
        thenPart.body(field, notNullValue());
    }

    @And("Path param is {string}")
    public void pathParamIs(String pathParam) {
        this.pathParam = pathParam;
        reqSpec.pathParam("id", pathParam);
    }


    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String id) {
        thenPart.body(id, is(pathParam));
    }

    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> list) {
        for (String s : list) {
            thenPart.body(s, notNullValue());

        }

    }
    //us03

    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String contentType) {
        reqSpec.contentType(contentType);
    }

    Map<String, Object> randomDataMap;

    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String randomData) {
        Map<String, Object> requestBody = new LinkedHashMap<>();

        switch (randomData) {
            case "user":
                   requestBody=LibraryAPI_Util.getRandomUserMap();
                break;
            case "book":
                requestBody = LibraryAPI_Util.getRandomBookMap();
                break;
            default:
                throw new RuntimeException("Unexpected value: " + randomData);
        }

        System.out.println("requestBody = " + requestBody);
        randomDataMap = requestBody;
        reqSpec.formParams(requestBody);


    }

    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {
        response = reqSpec.when().post(ConfigurationReader.getProperty("library.baseUri") + endpoint).prettyPeek();
        thenPart = response.then();

    }

    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String value, String message) {


        thenPart.body(value, is(message));
    }
    LoginPage loginPage=new LoginPage();
    @Given("I logged in Library UI as {string}")
    public void i_logged_in_library_ui_as(String uiUser) {
        BrowserUtil.waitFor(3);
        loginPage.login(uiUser);


    }

BookPage bookPage=new BookPage();
    @Given("I navigate to {string} page")
    public void i_navigate_to_page(String page) {
        bookPage.navigateModule(page);

    }
    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() {
        // UI Part

        BrowserUtil.waitFor(3);

        String bookName= (String) randomDataMap.get("name");
        System.out.println("bookName = " + bookName);

        bookPage.search.sendKeys(bookName);
        BrowserUtil.waitFor(3);

        bookPage.editBook(bookName).click();
        BrowserUtil.waitFor(3);

        // Get book info
        String UIBookName = bookPage.bookName.getAttribute("value");
        String UIAuthorName = bookPage.author.getAttribute("value");
        String UIYear = bookPage.year.getAttribute("value");
        String UIIsbn = bookPage.isbn.getAttribute("value");
        String UIDesc = bookPage.description.getAttribute("value");

        // We don't have category name information in book page.
        // We only have id of category
        // with the help of category id we will find category name by running query
        // Find category as category_id
        String UIBookCategory = BrowserUtil.getSelectedOption(bookPage.categoryDropdown);
        DB_Util.runQuery("select id from book_categories where name='"+UIBookCategory+"'");
        String UICategoryId= DB_Util.getFirstRowFirstColumn();

        System.out.println("--------- UI DATA -------------");
        Map<String,Object> UIBook=new LinkedHashMap<>();
        UIBook.put("name",UIBookName);
        UIBook.put("isbn",UIIsbn);
        UIBook.put("year",UIYear);
        UIBook.put("author",UIAuthorName);
        UIBook.put("book_category_id",UICategoryId);
        UIBook.put("description",UIDesc);

        System.out.println("UIBook = " + UIBook);

        //API Part

        Map<String, Object> APIBook=randomDataMap;
        System.out.println("APIBook = " + APIBook);

        //DATA Part
        String bookID=response.path("book_id");

        DB_Util.runQuery("select * from books where id='"+bookID+"'");
        Map<String, Object>DataBook=DB_Util.getRowMap(1);
        DataBook.remove("id");
        DataBook.remove("added_date");

        System.out.println("DataBook = " + DataBook);
        Assert.assertEquals(APIBook,UIBook);
        Assert.assertEquals(APIBook,DataBook);


    }


    //us04

    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {
        //DB part

        Map<String, Object>APIUser=new LinkedHashMap<>();

        APIUser=randomDataMap;

        String userId=response.path("user_id");
        System.out.println("userId = " + userId);

        System.out.println("APIUser = " + APIUser);

        //DB Part

        DB_Util.runQuery("Select * from users where id="+"'"+userId+"'");
        Map<String, Object>DBUser=DB_Util.getRowMap(1);
        DBUser.remove("id");
        DBUser.remove("image");
        DBUser.remove("extra_data");
        DBUser.remove("is_admin");
        System.out.println("DBUser = " + DBUser);

       // Assert.assertEquals(APIUser,DBUser);


    }

    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {

        String apiUserEmail= (String) randomDataMap.get("email");
        String apiUserPassword= (String) randomDataMap.get("password");


        loginPage.login(apiUserEmail,apiUserPassword );
        BrowserUtil.waitFor(3);



    }

    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {
        BrowserUtil.waitFor(2);
        String UIFullName = bookPage.accountHolderName.getText();
        String APIFullName = (String) randomDataMap.get("full_name");

        Assert.assertEquals(APIFullName,UIFullName);

    }
//us05

    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {
      loginPage.login(email,password);
      token=LibraryAPI_Util.getToken(email,password);
        reqSpec=given().log().uri()
                .header("x-library-token",token);



    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {
       reqSpec.formParams("token",token);
    }

}


