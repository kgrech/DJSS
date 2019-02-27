package com.github.kgrech.djss.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kgrech.djss.RestAPITest
import com.github.kgrech.djss.jooq.tables.pojos.Transfer
import com.github.kgrech.djss.jooq.tables.pojos.Transfer
import com.github.kgrech.djss.view.Page
import io.restassured.RestAssured

import static com.github.kgrech.djss.controller.CRUDController.PAGE
import static com.github.kgrech.djss.controller.CRUDController.PAGE_SZE

class TransferControllerTest extends RestAPITest   {

    final ObjectMapper mapper = new ObjectMapper()

    final int rId = 1
    final int sId = 2
    final double amount = 10


    def setupSpec() {
        RestAssured.baseURI = "http://localhost:8080$TransferController.BASE_URL"
    }

    def setup() {
        insertAccount(sId, "Account 1", 10000)
        insertAccount(rId, "Account 2", 10000)
    }

    private Transfer newTransfer() {
        newTransfer(sId, rId, amount)
    }

    private Transfer newTransfer(int sId, int rId, double amount) {
        def transfer = new Transfer()
        transfer.setSenderAccountId(sId)
        transfer.setReceiverAccountId(rId)
        transfer.setAmount(amount)
        return transfer
    }

    def 'Transfer api can create a new account'() {
        setup:
        def transfer = newTransfer()

        when:
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .post("/")
        then:
        response.getStatusCode() == 201
        def body = mapper.readValue(response.body.asString(), Transfer.class)
        body.getAmount() == amount
        body.getSenderAccountId() == sId
        body.getReceiverAccountId() == rId
    }

    def 'It should not be possible to create transfer for not existing receiver'() {
        setup:
        def transfer = newTransfer(sId, 10, 100)

        when:
        def response = RestAssured.given()
                .accept("application/json")
                .body(mapper.writeValueAsString(transfer))
                .post("/")
        then:
        response.getStatusCode() != 200
    }

    def 'It should not be possible to create transfer for not existing sender'() {
        setup:
        def transfer = newTransfer(10, sId, 100)

        when:
        def response = RestAssured.given()
                .accept("application/json")
                .body(mapper.writeValueAsString(transfer))
                .post("/")
        then:
        response.getStatusCode() != 200
    }

    def 'Get transfer API should work'() {
        def transfer = newTransfer()

        def response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .post('/')
        def body = mapper.readValue(response.body.asString(), Transfer.class)
        int id = body.getId()

        when:
        response = RestAssured.given().get("/$id")

        then:
        response.getStatusCode() == 200
        def respBody = mapper.readValue(response.body.asString(), Transfer.class)
        body.getAmount() == amount
        body.getSenderAccountId() == sId
        body.getReceiverAccountId() == rId
    }

    def "Transfer pagination should work"() {
        setup:
        int numPages = 10
        int pageSize = 20
        int lastPage = pageSize.intdiv(2)

        def total = (numPages-1) * pageSize  + lastPage
        for (i in 1..total) {
            Transfer transfer = newTransfer(sId, rId, i)
            RestAssured.given()
                    .body(mapper.writeValueAsString(transfer))
                    .post("/")
        }

        when:
        List<Page<Transfer>> pages = []
        for (i in 0..<numPages) {
            def response = RestAssured.given()
                    .param(PAGE, i)
                    .param(PAGE_SZE, pageSize)
                    .get("/")
            Page<Transfer> page = mapper.readValue(
                    response.body.asString(),
                    new TypeReference<Page<Transfer>>() {}
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
            Transfer transfer = page.getContent()[seqId]
            (int) transfer.getAmount() == i
        }
    }

    def 'Update transfer API should work'() {
        def transfer = newTransfer()

        def response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .post('/')
        def body = mapper.readValue(response.body.asString(), Transfer.class)
        int id = body.getId()

        double newAmount = 500

        when:
        transfer.setAmount(newAmount)
        transfer.setReceiverAccountId(sId)
        transfer.setSenderAccountId(rId)
        response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .put("/$id")

        then:
        response.getStatusCode() == 200
        def respBody = mapper.readValue(response.body.asString(), Transfer.class)
        respBody.getAmount() == newAmount
        respBody.getSenderAccountId() == rId
        respBody.getReceiverAccountId() == sId
    }

    def 'Update the not existing transfer should return 404'() {
        def transfer = newTransfer()
        int id = 10

        when:
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .put("/$id")
        def getResponse = RestAssured.given().get("/$id")

        then:
        response.getStatusCode() == 404
        getResponse.getStatusCode() == 404
    }


    def 'Delete transfer API should work'() {
        def transfer = newTransfer()

        def response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .post('/')
        def body = mapper.readValue(response.body.asString(), Transfer.class)
        int id = body.getId()

        def deleteResponse = RestAssured.given().delete("/$id")

        when:
        def getResponse = RestAssured.given().get("/$id")

        then:
        deleteResponse.getStatusCode() == 200
        getResponse.getStatusCode() == 404
    }



}
