package com.github.kgrech.djss

import com.github.kgrech.djss.jooq.TransferStatus
import org.jooq.DSLContext
import spock.lang.Specification

import static com.github.kgrech.djss.jooq.tables.Account.ACCOUNT
import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER
import static org.jooq.impl.DSL.defaultValue

abstract class DBTest extends Specification {

    private static App app

    static void setApp(App app) {
        DBTest.@app = app
    }

    static DSLContext getCtx() {
        return app.getCtx()
    }

    def insertAccount(long id, String name, double amount) {
        getCtx().insertInto(ACCOUNT)
            .values(
                id,
                name,
                amount
            )
            .execute()
    }

    def insertTransfer(double amount,
                       long from,
                       double to,
                       TransferStatus status = TransferStatus.PENDING,
                       String runId = null) {
        getCtx().insertInto(TRANSFER)
                .values(
                    defaultValue(TRANSFER.ID),
                    from,
                    to,
                    status,
                    amount,
                    runId,
                    null,
                    null
                )
                .execute()
    }

    def cleanup() {
        getCtx().deleteFrom(ACCOUNT).execute()
        getCtx().deleteFrom(TRANSFER).execute()
    }

}
