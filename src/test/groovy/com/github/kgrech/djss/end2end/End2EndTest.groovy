package com.github.kgrech.djss.end2end

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.controller.AccountController
import com.github.kgrech.djss.controller.TransferController
import com.github.kgrech.djss.jooq.TransferStatus
import com.github.kgrech.djss.jooq.tables.pojos.Account
import com.github.kgrech.djss.jooq.tables.pojos.Transfer
import io.restassured.RestAssured

class End2EndTest extends DBTest {

    final ObjectMapper mapper = new ObjectMapper()

    def setupSpec() {
        RestAssured.baseURI = "http://localhost:8080"
    }

    def "It should be possible to create accounts and transfer money"() {
        setup:
        def createdAcc1 = createAccount("Account 1", 1000)
        def createdAcc2 = createAccount("Account 2", 1000)

        when:
        Transfer transfer = createTransfer(createdAcc1.getId(), createdAcc2.getId(),
                100)

        while (transfer.getStatus() != TransferStatus.COMPLETED) {
            Thread.sleep(1000)
            transfer = getTransfer(transfer.getId())
        }

        def updateAcc1 = getAccount(createdAcc1.getId())
        def updateAcc2 = getAccount(createdAcc2.getId())

        then:
            updateAcc1.getAmount() == 900
            updateAcc2.getAmount() == 1100

    }

    private Account createAccount(String name, double amount) {
        Account account = new Account()
        account.setName(name)
        account.setAmount(amount)

        def account1Response = RestAssured.given()
                .body(mapper.writeValueAsString(account))
                .post(AccountController.BASE_URL + "/")
        return mapper.readValue(account1Response.body.asString(), Account.class)
    }

    private Transfer createTransfer(int sId, int rId, double amount) {
        def transfer = new Transfer()
        transfer.setSenderAccountId(sId)
        transfer.setReceiverAccountId(rId)
        transfer.setAmount(amount)
        def response = RestAssured.given()
                .body(mapper.writeValueAsString(transfer))
                .post(TransferController.BASE_URL + '/')
        return mapper.readValue(response.body.asString(), Transfer.class)
    }

    private Transfer getTransfer(int id) {
        def response = RestAssured.given().get(TransferController.BASE_URL + "/$id")
        return mapper.readValue(response.body.asString(), Transfer.class)
    }

    private Account getAccount(int id) {
        def response = RestAssured.given().get(AccountController.BASE_URL + "/$id")
        return mapper.readValue(response.body.asString(), Account.class)
    }
}

