package com.github.kgrech.djss.service.processing

import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.jooq.TransferStatus
import com.github.kgrech.djss.service.TransferProcessingScheduler
import com.github.kgrech.djss.service.proccessing.TransferDispensingRunnable

import java.sql.Timestamp
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER

class TransferDispensingRunnableTest extends DBTest   {

    private final int MAX_QUEUE_SIZE = 10
    private final int MAX_BATCH_SIZE = 10

    def setup() {
        insertAccount(1, "Account 1", 1000)
        insertAccount(2, "Account 2", 1000)
    }

    def "Runnable generates the run"() {
        setup:
        for(i in 1..20) {
            insertTransfer(10, 1, 2)
        }

        def runnable = new TransferDispensingRunnable(getCtx(),
                MAX_BATCH_SIZE, MAX_QUEUE_SIZE,
                Mock(TransferProcessingScheduler))

        when:
        runnable.run()

        def count = getCtx().selectCount()
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .fetchOne(0, int.class)

        def ids = getCtx()
                .select(TRANSFER.PROCESSING_ID)
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .fetch()
                .into(String.class)
                .unique(false)

        def times = getCtx()
                .select(TRANSFER.PROCESSING_START)
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .fetch()
                .into(Timestamp.class)

        then:
            count == MAX_QUEUE_SIZE
            !ids.isEmpty()
            ids[0] != null
            !ids[0].isEmpty()
            times.size() == MAX_QUEUE_SIZE
            times.every { it != null }
    }

    def "Runnable schedules runs with no overlap"() {
        setup:
        def numElements = 1000
        def executorService = Executors.newFixedThreadPool(10)
        for(i in 1..numElements) {
            insertTransfer(10, 1, 2)
        }

        when:
        def itemsToProcess = numElements.intdiv(2)
        def numThreads = itemsToProcess.intdiv(MAX_QUEUE_SIZE)
        for (i in 1..numThreads) {
            def runnable = new TransferDispensingRunnable(getCtx(),
                    MAX_BATCH_SIZE, MAX_QUEUE_SIZE,
                    Mock(TransferProcessingScheduler))
            executorService.submit(runnable)
        }
        executorService.shutdown()
        while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) { }

        List<String> ids = getCtx()
                .select(TRANSFER.PROCESSING_ID)
                .from(TRANSFER)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .fetch()
                .into(String.class)

        def unique = ids.unique(false)

        then:
            ids.size() == itemsToProcess
            unique.size() == numThreads
    }

    def "Runnable schedules the run"() {
        setup:
        for(i in 1..5) {
            insertTransfer(10, 1, 2)
        }

        def scheduler = Mock(TransferProcessingScheduler)
        def runnable = new TransferDispensingRunnable(getCtx(),
                MAX_BATCH_SIZE, MAX_QUEUE_SIZE, scheduler)

        when:
        runnable.run()

        then:
        1 * scheduler.scheduleProcessing(_)

    }

    def "Runnable does not schedule the run with a full queue"() {
        setup:
        for(i in 1..5) {
            insertTransfer(10, 1, 2)
        }

        def scheduler = Mock(TransferProcessingScheduler)
        scheduler.queueSize() >> MAX_QUEUE_SIZE + 1
        def runnable = new TransferDispensingRunnable(getCtx(),
                MAX_BATCH_SIZE, MAX_QUEUE_SIZE, scheduler)

        when:
        runnable.run()

        then:
        0 * scheduler.scheduleProcessing(_)

    }
}
