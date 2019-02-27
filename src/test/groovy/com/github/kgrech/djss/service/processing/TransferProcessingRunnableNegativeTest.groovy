package com.github.kgrech.djss.service.processing

import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.jooq.TransferStatus
import com.github.kgrech.djss.service.proccessing.TransferProcessingRunnable

import static com.github.kgrech.djss.jooq.tables.Account.ACCOUNT
import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER

class TransferProcessingRunnableNegativeTest extends DBTest {

    private final double BALANCE = 1_000_000

    private TransferProcessingRunnable runnable

    def setup() {
        insertAccount(1, "Account 1", BALANCE)
        insertAccount(2, "Account 2", BALANCE)

        def id = UUID.randomUUID().toString()
        insertTransfer(100, 1, 2, TransferStatus.PROCESSING, id)
        runnable = Spy(new TransferProcessingRunnable(ctx, id))
    }

    def "Amount is not updated if fetchTransfers fails"() {
        setup:
        runnable.fetchTransfers() >> { throw new RuntimeException() }

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .fetchOne().value1() == 1

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == BALANCE

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == BALANCE
    }

    def "Amount is not updated if fetchAmount fails"() {
        setup:
        runnable.fetchAmount(_) >> { throw new RuntimeException() }

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.ERROR))
                .fetchOne().value1() == 1

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == BALANCE

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == BALANCE
    }

    def "Amount is not updated if processTransfer fails"() {
        setup:
        runnable.processTransfer(_) >> { throw new RuntimeException() }

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.ERROR))
                .fetchOne().value1() == 1

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == BALANCE

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == BALANCE
    }

    def "Amount is not updated if updateAmount fails"() {
        setup:
        runnable.updateAmount(_, _, _) >> { throw new RuntimeException() }

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.ERROR))
                .fetchOne().value1() == 1

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == BALANCE

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == BALANCE
    }

    def "Amount is not updated if updateStatus fails"() {
        setup:
        runnable.updateStatus(_, _) >> { throw new RuntimeException() }

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .fetchOne().value1() == 1

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == BALANCE

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == BALANCE
    }


}
