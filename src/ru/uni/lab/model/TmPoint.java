package ru.uni.lab.model;

/**
 * Telemetry Record for Point/Array (Type 3) values.
 */
public class TmPoint extends TmRecord {
    private int elementSize;
    private int arrayLength; // in bytes
    private byte[] data;

    public TmPoint(int parameterNumber, long time, int attribute, int valueType, int elementSize, int arrayLength, byte[] data) {
        super(parameterNumber, time, attribute, valueType);
        this.elementSize = elementSize;
        this.arrayLength = arrayLength;
        this.data = data;
    }

    public int getElementSize() {
        return elementSize;
    }

    public void setElementSize(int elementSize) {
        this.elementSize = elementSize;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    public void setArrayLength(int arrayLength) {
        this.arrayLength = arrayLength;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String getValueAsString() {
        if (data == null) return "null";
        if (arrayLength <= 16) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < data.length; i++) {
                sb.append(String.format("%02X", data[i]));
                if (i < data.length - 1) sb.append(" ");
            }
            sb.append("]");
            return sb.toString();
        }
        return String.format("Array[%d bytes]", arrayLength);
    }
}
