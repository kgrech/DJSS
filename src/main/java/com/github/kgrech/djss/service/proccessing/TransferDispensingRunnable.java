package com.github.kgrech.djss.service.proccessing;

import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER;
import static org.jooq.impl.DSL.select;

import com.github.kgrech.djss.jooq.TransferStatus;
import com.github.kgrech.djss.service.TransferProcessingScheduler;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@Slf4j
public class TransferDispensingRunnable implements Runnable {

    private final DSLContext ctx;
    private final TransferProcessingScheduler scheduler;
    private final int batchSize;
    private final int maxQueueSize;


    public TransferDispensingRunnable(DSLContext ctx,
                                      int batchSize,
                                      int maxQueueSize,
                                      TransferProcessingScheduler scheduler) {
        this.ctx = ctx;
        this.batchSize = batchSize;
        this.maxQueueSize = maxQueueSize;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        if (scheduler.queueSize() > maxQueueSize) {
            log.debug("Number of pending jobs ({}) exceeds {}.",
                    scheduler.queueSize(), batchSize);
            return;
        }
        final String uuid = UUID.randomUUID().toString();
        final long now = new java.util.Date().getTime();
        try {
            //jooq does tot support update ... limit
            ctx.transaction(() -> {
                ctx.update(TRANSFER)
                        .set(TRANSFER.PROCESSING_ID, uuid)
                        .set(TRANSFER.PROCESSING_START, new Timestamp(now))
                        .set(TRANSFER.STATUS, TransferStatus.PROCESSING)
                        .where(TRANSFER.ID.in(
                                select(TRANSFER.ID)
                                        .from(TRANSFER)
                                        .where(TRANSFER.STATUS.eq(TransferStatus.PENDING))
                                        .orderBy(TRANSFER.ID)
                                        .limit(batchSize)
                                ))
                        .execute();
            });
            scheduler.scheduleProcessing(uuid);
        } catch (RuntimeException e) {
            log.error("Error scheduling new set of processing", e);
        }
    }
}
