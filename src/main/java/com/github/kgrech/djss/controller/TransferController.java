package com.github.kgrech.djss.controller;

import static com.github.kgrech.djss.jooq.tables.Transfer.TRANSFER;
import static org.jooq.impl.DSL.defaultValue;

import com.github.kgrech.djss.exception.NotFoundException;
import com.github.kgrech.djss.jooq.TransferStatus;
import com.github.kgrech.djss.jooq.tables.pojos.Transfer;
import com.github.kgrech.djss.jooq.tables.records.TransferRecord;
import com.github.kgrech.djss.view.Page;
import java.util.List;
import org.jooq.DSLContext;

public class TransferController extends CRUDController<Transfer> {

    public static final String BASE_URL = "/transfers";
    private final DSLContext ctx;

    public TransferController(DSLContext ctx) {
        super(Transfer.class);
        this.ctx = ctx;
    }

    @Override
    protected String getPath() {
        return BASE_URL;
    }

    @Override
    protected Transfer get(int id) {
        TransferRecord value = ctx.selectFrom(TRANSFER)
                .where(TRANSFER.ID.eq(id))
                .fetchOne();
        if (value != null) {
            return value.into(Transfer.class);
        } else {
            throw new NotFoundException("Account with " + id + " is not found");
        }
    }

    @Override
    protected Page<Transfer> getPage(int page, int pageSize) {
        return ctx.transactionResult(() -> {
            List<Transfer> content = ctx
                    .selectFrom(TRANSFER)
                    .offset(pageSize * page)
                    .limit(pageSize)
                    .fetch()
                    .into(Transfer.class);
            long total = ctx.selectCount()
                    .from(TRANSFER)
                    .fetchOne().value1();
            return new Page<>(content, total);
        });
    }

    @Override
    protected Transfer create(Transfer newInstance) {
        return ctx.insertInto(TRANSFER)
                .values(
                        defaultValue(TRANSFER.ID),
                        newInstance.getSenderAccountId(),
                        newInstance.getReceiverAccountId(),
                        TransferStatus.PENDING,
                        newInstance.getAmount(),
                        null,
                        null,
                        null
                )
                .returning()
                .fetchOne()
                .into(Transfer.class);
    }

    @Override
    protected Transfer update(int id, Transfer updateInstance) {
        try {
            return ctx.transactionResult(() -> {
                TransferRecord value = ctx.selectFrom(TRANSFER)
                        .where(TRANSFER.ID.eq(id))
                        .forUpdate()
                        .fetchOne();
                if (value == null) {
                    throw new NotFoundException("Account with " + id + " is not found");
                }
                Transfer transfer = value.into(Transfer.class);
                if (transfer.getStatus() != TransferStatus.PENDING) {
                    throw new UnsupportedOperationException(
                            "Can't update transfer in " + transfer.getStatus() +
                                    " status");
                }

                //We do not allow to update status
                ctx.update(TRANSFER)
                        .set(TRANSFER.AMOUNT, updateInstance.getAmount())
                        .set(TRANSFER.SENDER_ACCOUNT_ID, updateInstance.getSenderAccountId())
                        .set(TRANSFER.RECEIVER_ACCOUNT_ID, updateInstance.getReceiverAccountId())
                        .execute();
                return ctx
                        .selectFrom(TRANSFER)
                        .where(TRANSFER.ID.eq(id))
                        .fetchOne()
                        .into(Transfer.class);
            });
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    @Override
    protected void delete(int id) {
        ctx.transaction(() -> {
            TransferRecord value = ctx.selectFrom(TRANSFER)
                    .where(TRANSFER.ID.eq(id))
                    .forUpdate()
                    .fetchOne();
            if (value == null) {
                throw new NotFoundException("Account with " + id + " is not found");
            }
            Transfer transfer = value.into(Transfer.class);
            if (transfer.getStatus() != TransferStatus.PENDING) {
                throw new UnsupportedOperationException(
                        "Can't delete transfer in " + transfer.getStatus() +
                                " status");
            }
            ctx.deleteFrom(TRANSFER)
                    .where(TRANSFER.ID.eq(id))
                    .execute();
        });
    }
}
