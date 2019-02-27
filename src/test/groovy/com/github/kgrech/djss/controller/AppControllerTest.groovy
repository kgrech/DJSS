package com.github.kgrech.djss.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.github.kgrech.djss.view.Page

import static com.github.kgrech.djss.controller.CRUDController.PAGE
import static com.github.kgrech.djss.controller.CRUDController.PAGE_SZE

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kgrech.djss.RestAPITest
import com.github.kgrech.djss.jooq.tables.pojos.Account
import io.restassured.RestAssured

class AppControllerTest extends RestAPITest   {

    final ObjectMapper mapper = new ObjectMapper()

    def setupSpec() {
        RestAssured.baseURI = "http://localhost:8080$AccountController.BASE_URL"
    }

    def 'Account api can create a new account'() {
        setup:
        String name = 'Test account'
        double amount = 1000

        Account account = new Account()
        account.setName(name)
        account.setAmount(amount)

        when:
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .post("/")
        then:
        response.getStatusCode() == 201
        def body = mapper.readValue(response.body.asString(), Account.class)
        body.getAmount() == amount
        body.getName() == name
    }

    def 'Get account API should work'() {
        String name = 'Test account'
        double amount = 1000

        Account account = new Account()
        account.setName(name)
        account.setAmount(amount)
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .post('/')
        def body = mapper.readValue(response.body.asString(), Account.class)
        int id = body.getId()

        when:
        response = RestAssured.given().get("/$id")

        then:
        response.getStatusCode() == 200
        def respBody = mapper.readValue(response.body.asString(), Account.class)
        respBody.getId() == id
        respBody.getAmount() == amount
        respBody.getName() == name
    }

    def "Account pagination should work"() {
        setup:
        int numPages = 10
        int pageSize = 20
        int lastPage = pageSize.intdiv(2)

        def total = (numPages-1) * pageSize  + lastPage
        for (i in 1..total) {
            Account account = new Account()
            account.setName("Test account $i")
            account.setAmount(10000 + i)
            RestAssured.given()
                    .body(mapper.writeValueAsString(account))
                    .post("/")
        }

        when:
        List<Page<Account>> pages = []
        for (i in 0..<numPages) {
            def response = RestAssured.given()
                    .param(PAGE, i)
                    .param(PAGE_SZE, pageSize)
                    .get("/")
            Page<Account> page = mapper.readValue(
                    response.body.asString(),
                    new TypeReference<Page<Account>>() {}
            )
            pages << page
        }

        then:
        for (i in 0..<numPages-1) {
            pages[i].getContent().size() == pageSize
            pages[i].getTotal() == total
        }
        pages[-1].getContent().size() == lastPage
        pages[-1].getTotal() == total

        for (int i in 1..total) {
            int pageId = (i-1).intdiv(pageSize)
            int seqId = (i-1) % pageSize
            def page = pages[pageId]
            Account account = page.getContent()[seqId]
            account.getName() == "Test account $i"
            account.getAmount() == 10000 + i
        }
    }

    def 'Update account API should work'() {
        String name = 'Test account'
        double amount = 1000

        Account account = new Account()
        account.setName(name)
        account.setAmount(amount)
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .post('/')
        def body = mapper.readValue(response.body.asString(), Account.class)
        int id = body.getId()

        int newId = id + 5
        String newName = 'Test account 2'
        double newAmount = 2000

        when:
        account.setId(newId)
        account.setName(newName)
        account.setAmount(newAmount)
        response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .put("/$id")

        then:
        response.getStatusCode() == 200
        def respBody = mapper.readValue(response.body.asString(), Account.class)
        respBody.getId() == id
        respBody.getAmount() == newAmount
        respBody.getName() == newName
    }

    def 'Update the not existing account should return 404'() {
        String name = 'Test account'
        double amount = 1000

        int id = 10
        Account account = new Account()
        account.setId(10)
        account.setName(name)
        account.setAmount(amount)

        when:
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .put("/$id")
        def getResponse = RestAssured.given().get("/$id")

        then:
        response.getStatusCode() == 404
        getResponse.getStatusCode() == 404
    }


    def 'Delete account API should work'() {
        String name = 'Test account'
        double amount = 1000

        Account account = new Account()
        account.setName(name)
        account.setAmount(amount)
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .post('/')
        def body = mapper.readValue(response.body.asString(), Account.class)
        int id = body.getId()

        def deleteResponse = RestAssured.given().delete("/$id")

        when:
        def getResponse = RestAssured.given().get("/$id")

        then:
        deleteResponse.getStatusCode() == 200
        getResponse.getStatusCode() == 404
    }



}
