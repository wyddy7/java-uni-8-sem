package ru.uni.lab.service;

public class StatQuestion {
    private final String id;
    private final String title;
    private final String sourceRef;

    public StatQuestion(String id, String title, String sourceRef) {
        this.id = id;
        this.title = title;
        this.sourceRef = sourceRef;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceRef() {
        return sourceRef;
    }
}

