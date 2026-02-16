package ru.uni.lab.model;

import java.util.HashMap;
import java.util.Map;

public class ParameterInfo {
    private int id;
    private String name;
    private String description;
    private Map<Integer, String> values;

    public ParameterInfo(int id, String name) {
        this.id = id;
        this.name = name;
        this.values = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<Integer, String> getValues() {
        return values;
    }

    public void addValue(int value, String text) {
        this.values.put(value, text);
    }
    
    public String getValueText(long value) {
        // Values are stored as Integer in map, but TmRecord uses long.
        // Usually codes are small enough for int.
        return values.get((int)value);
    }
}
