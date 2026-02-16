package ru.uni.lab.service;

import ru.uni.lab.model.ServiceMessage;
import ru.uni.lab.model.TmRecord;

import java.util.List;

public class KnpParseResult {
    private final List<TmRecord> dataRecords;
    private final List<ServiceMessage> serviceMessages;

    public KnpParseResult(List<TmRecord> dataRecords, List<ServiceMessage> serviceMessages) {
        this.dataRecords = dataRecords;
        this.serviceMessages = serviceMessages;
    }

    public List<TmRecord> getDataRecords() {
        return dataRecords;
    }

    public List<ServiceMessage> getServiceMessages() {
        return serviceMessages;
    }
}

