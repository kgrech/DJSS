package com.github.kgrech.djss.controller;

import static com.github.kgrech.djss.jooq.tables.Account.ACCOUNT;
import static org.jooq.impl.DSL.defaultValue;

import com.github.kgrech.djss.view.Page;
import com.github.kgrech.djss.exception.NotFoundException;
import com.github.kgrech.djss.jooq.tables.pojos.Account;
import com.github.kgrech.djss.jooq.tables.records.AccountRecord;
import java.util.List;
import org.jooq.DSLContext;

public class AccountController extends CRUDController<Account> {

    public static final String BASE_URL = "/accounts";
    private final DSLContext ctx;

    public AccountController(DSLContext ctx) {
        super(Account.class);
        this.ctx = ctx;
    }

    @Override
    protected String getPath() {
        return BASE_URL;
    }

    @Override
    protected Account get(int id) {
        AccountRecord value = ctx.selectFrom(ACCOUNT)
                .where(ACCOUNT.ID.eq(id))
                .fetchOne();
        if (value != null) {
            return value.into(Account.class);
        } else {
            throw new NotFoundException("Account with " + id + " is not found");
        }
    }

    @Override
    protected Page<Account> getPage(int page, int pageSize) {
        return ctx.transactionResult(() -> {
            List<Account> content = ctx
                    .selectFrom(ACCOUNT)
                    .offset(pageSize * page)
                    .limit(pageSize)
                    .fetch()
                    .into(Account.class);
            long total = ctx.selectCount()
                    .from(ACCOUNT)
                    .fetchOne().value1();
            return new Page<>(content, total);
        });
    }

    @Override
    protected Account create(Account newInstance) {
        return ctx.insertInto(ACCOUNT)
                .values(
                        defaultValue(ACCOUNT.ID),
                        newInstance.getName(),
                        newInstance.getAmount()
                )
                .returning()
                .fetchOne()
                .into(Account.class);
    }

    @Override
    protected Account update(int id, Account updateInstance) {
        try {
            return ctx.transactionResult(() -> {
                AccountRecord value = ctx.selectFrom(ACCOUNT)
                        .where(ACCOUNT.ID.eq(id))
                        .forUpdate()
                        .fetchOne();
                if (value == null) {
                    throw new NotFoundException("Account with " + id + " is not found");
                }

                ctx.update(ACCOUNT)
                        .set(ACCOUNT.NAME, updateInstance.getName())
                        .set(ACCOUNT.AMOUNT, updateInstance.getAmount())
                        .where(ACCOUNT.ID.eq(id))
                        .execute();
                return ctx
                        .selectFrom(ACCOUNT)
                        .where(ACCOUNT.ID.eq(id))
                        .fetchOne()
                        .into(Account.class);
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
        ctx.deleteFrom(ACCOUNT)
                .where(ACCOUNT.ID.eq(id))
                .execute();
    }
}
