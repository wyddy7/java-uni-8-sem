package ru.uni.lab.model;

/**
 * Telemetry Record for Code (Type 2) values.
 */
public class TmCode extends TmRecord {
    private int codeLength;
    private long value;

    public TmCode(int parameterNumber, long time, int attribute, int valueType, int codeLength, long value) {
        super(parameterNumber, time, attribute, valueType);
        this.codeLength = codeLength;
        this.value = value;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String getValueAsString() {
        // Display as binary string with leading zeros if necessary, or just hex/dec
        // Requirement says: "binary representation (e.g., '0101') and code length"
        String binaryString = Long.toBinaryString(value);
        // Pad with zeros to match codeLength if needed (though codeLength might be in bytes or bits? 
        // TZ says "codeLength (2 bytes)" in file structure, but usually it refers to bits in display.
        // Assuming simple display for now.
        return String.format("Bin: %s (Len: %d)", binaryString, codeLength);
    }
}
