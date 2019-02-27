package com.github.kgrech.djss.service;

public interface TransferProcessingScheduler {

    void scheduleProcessing(String processingId);

    int queueSize();
}
