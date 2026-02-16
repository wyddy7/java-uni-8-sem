package ru.uni.lab.model;

/**
 * Telemetry Record for Long (Type 0) values.
 */
public class TmLong extends TmRecord {
    private long value;

    public TmLong(int parameterNumber, long time, int attribute, int valueType, long value) {
        super(parameterNumber, time, attribute, valueType);
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(value);
    }
}
