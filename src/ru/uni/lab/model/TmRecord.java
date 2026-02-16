package ru.uni.lab.model;

/**
 * Base abstract class for Telemetry Record.
 */
public abstract class TmRecord {
    private int parameterNumber;
    private String parameterName;
    private long time; // milliseconds from start of day
    private String dimension;
    private int attribute;
    private int valueType;

    public TmRecord(int parameterNumber, long time, int attribute, int valueType) {
        this.parameterNumber = parameterNumber;
        this.time = time;
        this.attribute = attribute;
        this.valueType = valueType;
    }

    public int getParameterNumber() {
        return parameterNumber;
    }

    public void setParameterNumber(int parameterNumber) {
        this.parameterNumber = parameterNumber;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public int getAttribute() {
        return attribute;
    }

    public void setAttribute(int attribute) {
        this.attribute = attribute;
    }

    public int getValueType() {
        return valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }
    
    public abstract String getValueAsString();
}
