package com.github.kgrech.djss.service.proccessing;

import static com.github.kgrech.djss.jooq.Tables.ACCOUNT;
import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER;

import com.github.kgrech.djss.jooq.TransferStatus;
import com.github.kgrech.djss.jooq.tables.pojos.Transfer;
import java.sql.Timestamp;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@Slf4j
public class TransferProcessingRunnable implements Runnable {

    private final DSLContext ctx;
    private final String processingId;

    public TransferProcessingRunnable(DSLContext ctx, String processingId) {
        this.ctx = ctx;
        this.processingId = processingId;
    }

    @Override
    public void run() {
        try {
            log.debug("Staring processing {}", processingId);
            List<Transfer> transfers = fetchTransfers();
            log.debug("Processing {} includes {} transfers", processingId, transfers.size());

            for (Transfer transfer : transfers) {
                ctx.transaction(() -> {
                    TransferStatus status;

                    try {
                        status = processTransfer(transfer);
                    } catch (RuntimeException e) {
                        status = TransferStatus.ERROR;
                        log.error("Error appears during processing transfer {}. " +
                                        "Can't transfer {}$ from account {} " +
                                        "to account {}!",
                                transfer.getId(), transfer.getAmount(),
                                transfer.getSenderAccountId(),
                                transfer.getSenderAccountId(), e);
                    }

                    updateStatus(transfer.getId(), status);
                });
            }
        } catch (RuntimeException e) {
            log.error("Error processing transfer {}",
                    processingId, e);
        }
    }

    public List<Transfer> fetchTransfers() {
        return ctx
                .selectFrom(TRANSFER)
                .where(
                        TRANSFER.PROCESSING_ID.eq(processingId)
                                .and(TRANSFER.STATUS.eq(TransferStatus.PROCESSING))
                )
                .fetch()
                .into(Transfer.class);
    }

    public TransferStatus processTransfer(Transfer transfer) {
        return ctx.transactionResult(() -> {
            log.debug("Processing transfer {}", transfer.getId());

            Double senderAmount =
                    fetchAmount(transfer.getSenderAccountId());

            if (senderAmount >= transfer.getAmount()) {
                updateAmount(transfer.getAmount(),
                        transfer.getSenderAccountId(),
                        transfer.getReceiverAccountId());
                log.info("Transfer {} complete!", transfer.getId());
                return TransferStatus.COMPLETED;
            }

            log.info("Can't perform transfer {}. Can't transfer {}$ " +
                            "from account {} to account {}! " +
                            "Reason: account balance is too low: {}$",
                    transfer.getId(), transfer.getAmount(),
                    transfer.getSenderAccountId(),
                    transfer.getSenderAccountId(),
                    senderAmount);
            return TransferStatus.REJECTED;
        });
    }

    public Double fetchAmount(int accountId) {
        return ctx
                .select(ACCOUNT.AMOUNT)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(accountId))
                .fetchOne()
                .value1();
    }

    public void updateAmount(double amount, int senderId, int receiverId) {
        ctx.update(ACCOUNT)
                .set(ACCOUNT.AMOUNT, ACCOUNT.AMOUNT.minus(amount))
                .where(ACCOUNT.ID.eq(senderId))
                .execute();

        ctx.update(ACCOUNT)
                .set(ACCOUNT.AMOUNT, ACCOUNT.AMOUNT.add(amount))
                .where(ACCOUNT.ID.eq(receiverId))
                .execute();
    }

    public void updateStatus(int id, TransferStatus status) {
        long now = new java.util.Date().getTime();

        ctx.update(TRANSFER)
                .set(TRANSFER.STATUS, status)
                .set(TRANSFER.PROCESSING_END, new Timestamp(now))
                .where(TRANSFER.ID.eq(id))
                .execute();
    }
}
