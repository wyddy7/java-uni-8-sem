package ru.uni.lab.model;

/**
 * Telemetry Record for Double (Type 1) values.
 */
public class TmDouble extends TmRecord {
    private double value;

    public TmDouble(int parameterNumber, long time, int attribute, int valueType, double value) {
        super(parameterNumber, time, attribute, valueType);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(value);
    }
}
