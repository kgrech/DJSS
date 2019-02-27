package com.github.kgrech.djss.service;

import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER;

import com.github.kgrech.djss.jooq.TransferStatus;
import com.github.kgrech.djss.service.proccessing.TransferDispensingRunnable;
import com.github.kgrech.djss.service.proccessing.TransferProcessingRunnable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jooq.DSLContext;

public class ProcessingService implements AutoCloseable, TransferProcessingScheduler {

    private final DSLContext ctx;
    private final ScheduledExecutorService transferDispensingScheduler;
    private final ThreadPoolExecutor processingExecutor;

    /**
     * Creates new instance
     * @param delay delay between runs in seconds
     * @param maxThreads max number of transaction processing threads
     * @param batchSize the size of one batch of transactions to process
     * @param maxQueueSize the max allowed length of the queue
     * @param ctx jooq context
     */
    public ProcessingService(long delay, int maxThreads,
                             int batchSize, int maxQueueSize,
                             DSLContext ctx) {
        this.ctx = ctx;

        cleanup();

        this.transferDispensingScheduler = Executors.newScheduledThreadPool(1);
        this.processingExecutor = new ThreadPoolExecutor(maxThreads, maxThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        Runnable dispensingRunnable = new TransferDispensingRunnable(ctx, batchSize,
                maxQueueSize,this);
        this.transferDispensingScheduler.scheduleWithFixedDelay(dispensingRunnable,
                0, delay, TimeUnit.SECONDS);
    }

    private void cleanup() {
        //In case of power failure or etc
        //Assuming there is only 1 process of this service
        //Search for all transactions in PROCESSING state and move them to PENDING
        ctx.update(TRANSFER)
                .set(TRANSFER.STATUS, TransferStatus.PENDING)
                .where(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                .execute();
    }

    @Override
    public void scheduleProcessing(String processingId) {
        processingExecutor.submit(new TransferProcessingRunnable(ctx, processingId));
    }

    @Override
    public int queueSize() {
        return processingExecutor.getQueue().size();
    }

    @Override
    public void close() {
        this.transferDispensingScheduler.shutdown();
        this.processingExecutor.shutdown();
    }
}
