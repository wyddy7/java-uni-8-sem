package ru.uni.lab.model;

/**
 * Service message record from KNP (paramNumber == 0xFFFF).
 *
 * Message types (byte 6), per docs:
 * 0 - empty packet
 * 1 - start session (32 bytes total)
 * 2 - current time
 * 3 - end session
 * 4 - mode change
 * 5 - date change
 * 6 - error
 * 7 - (unused/other)
 */
public class ServiceMessage {
    private final long time; // milliseconds from start of day (same field location as in data records)
    private final int messageType; // byte 6
    private final int valueType;   // byte 7 (as in docs)
    private final byte[] payload;  // bytes 8..end of message (8 or 24 bytes typically)

    // Optional decoded fields (when applicable)
    private final Integer modeNumber;   // for type 4
    private final Integer errorNumber;  // for type 6
    private final Integer daysFrom1980; // for type 5 or 2 (when encoded)

    public ServiceMessage(long time,
                          int messageType,
                          int valueType,
                          byte[] payload,
                          Integer modeNumber,
                          Integer errorNumber,
                          Integer daysFrom1980) {
        this.time = time;
        this.messageType = messageType;
        this.valueType = valueType;
        this.payload = payload;
        this.modeNumber = modeNumber;
        this.errorNumber = errorNumber;
        this.daysFrom1980 = daysFrom1980;
    }

    public long getTime() {
        return time;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getValueType() {
        return valueType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public Integer getModeNumber() {
        return modeNumber;
    }

    public Integer getErrorNumber() {
        return errorNumber;
    }

    public Integer getDaysFrom1980() {
        return daysFrom1980;
    }
}

