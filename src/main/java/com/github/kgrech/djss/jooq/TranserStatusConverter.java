package com.github.kgrech.djss.jooq;

import org.jooq.impl.EnumConverter;

public class TranserStatusConverter extends EnumConverter<Integer, TransferStatus> {

    public TranserStatusConverter() {
        super(Integer.class, TransferStatus.class);
    }
}