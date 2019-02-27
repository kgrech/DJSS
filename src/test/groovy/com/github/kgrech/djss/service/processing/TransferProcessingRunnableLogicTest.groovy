package com.github.kgrech.djss.service.processing

import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.jooq.TransferStatus
import com.github.kgrech.djss.service.proccessing.TransferProcessingRunnable

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.github.kgrech.djss.jooq.tables.Account.ACCOUNT
import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER

class TransferProcessingRunnableLogicTest extends DBTest  {

    private final double BALANCE = 1_000_000

    def setup() {
        insertAccount(1, "Account 1", BALANCE)
        insertAccount(2, "Account 2", BALANCE)
    }

    def "Runnable updates status and timestamp"() {
        setup:
            def id = UUID.randomUUID().toString()
            insertTransfer(5, 1, 2, TransferStatus.PROCESSING, id)
            def runnable = new TransferProcessingRunnable(ctx, id)

        when:
        runnable.run()

        then:
        ctx.select(TRANSFER.PROCESSING_END)
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.COMPLETED))
                .fetchOne().value1() != null
    }

    def "Runnable counts balance"() {
        setup:
        def id = UUID.randomUUID().toString()
        insertTransfer(100, 1, 2, TransferStatus.PROCESSING, id)
        def runnable = new TransferProcessingRunnable(ctx, id)

        when:
        runnable.run()

        then:
        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == BALANCE - 100

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == BALANCE + 100
    }

    def "Runnable processes all records"() {
        setup:
        def numRecords = 100
        def id = UUID.randomUUID().toString()
        for(i in 1..numRecords) {
            insertTransfer(5, 1, 2, TransferStatus.PROCESSING, id)
            insertTransfer(5, 2, 1, TransferStatus.PROCESSING, id)
        }
        def runnable = new TransferProcessingRunnable(ctx, id)

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.COMPLETED))
                .fetchOne(0, int.class) == 2 * numRecords
    }

    def "Runnable rejects transfers"() {
        setup:
        def transferSize = 1000
        def numRecords = (int) (BALANCE / transferSize)
        def id = UUID.randomUUID().toString()
        for (i in 1..2*numRecords) {
            insertTransfer(transferSize, 1, 2, TransferStatus.PROCESSING, id)
        }
        def runnable = new TransferProcessingRunnable(ctx, id)

        when:
        runnable.run()

        then:
        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.COMPLETED))
                .fetchOne(0, int.class) == numRecords

        ctx.selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.REJECTED))
                .fetchOne(0, int.class) == numRecords

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1() == 0

        ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1() == 2 * BALANCE
    }

    def "Concurrent processing work well"() {
        setup:
        def maxTransfer = 100
        def transfersPerRun = 10
        def numRuns = (int) (BALANCE / (2 * transfersPerRun * maxTransfer)) - 1
        def numThreads = 10

        List<Runnable> runs = []
        for (i in 1..numRuns) {
            def id = UUID.randomUUID().toString()
            for (j in 1..transfersPerRun) {
                insertTransfer(Math.random() * 10, 1, 2,
                        TransferStatus.PROCESSING, id)
            }
            runs << new TransferProcessingRunnable(ctx, id)
        }

        for (i in 1..numRuns) {
            def id = UUID.randomUUID().toString()
            for (j in 1..transfersPerRun) {
                insertTransfer(Math.random() * 10, 2, 1,
                        TransferStatus.PROCESSING, id)
            }
            runs << new TransferProcessingRunnable(ctx, id)
        }

        when:
        def executorService = Executors.newFixedThreadPool(numThreads)
        runs.each { executorService.submit(it)}

        executorService.shutdown()
        while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) { }

        double amount1 = ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(1))
                .fetchOne().value1()

        double amount2 = ctx.select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(2))
                .fetchOne().value1()

        then:
            amount1 + amount2 == 2 * BALANCE
    }
}
