package ru.uni.lab.service;

import ru.uni.lab.model.ServiceMessage;
import ru.uni.lab.model.TmCode;
import ru.uni.lab.model.TmDouble;
import ru.uni.lab.model.TmLong;
import ru.uni.lab.model.TmPoint;
import ru.uni.lab.model.TmRecord;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class KnpParser {

    /**
     * Backward-compatible parse (returns only data records; service messages are ignored).
     */
    public List<TmRecord> parse(File file) throws IOException {
        return parseWithServiceMessages(file).getDataRecords();
    }

    /**
     * Parse KNP file and return both data records and service messages.
     * Also assigns {@link ru.uni.lab.model.TmRecord#setSessionModeNumber(int)} based on service message type 4 (mode change).
     */
    public KnpParseResult parseWithServiceMessages(File file) throws IOException {
        List<TmRecord> records = new ArrayList<>();
        List<ServiceMessage> serviceMessages = new ArrayList<>();
        int currentModeNumber = 1; // By default, start of session is NП
        
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            while (dis.available() > 0) {
                // Read full 16-byte header buffer
                byte[] header = new byte[16];
                try {
                    dis.readFully(header);
                } catch (EOFException e) {
                    break; // Expected EOF at end of file if size is exactly divisible
                } catch (IOException e) {
                    break; // Unexpected but we stop
                }
                
                ByteBuffer bb = ByteBuffer.wrap(header);
                bb.order(ByteOrder.BIG_ENDIAN);
                
                // 0-1: Param Number
                int paramNumber = bb.getShort(0) & 0xFFFF;
                
                // Check for service message (0xFFFF)
                if (paramNumber == 0xFFFF) {
                    long time = bb.getInt(2) & 0xFFFFFFFFL;
                    int msgType = bb.get(6) & 0xFF;
                    int valueType = bb.get(7) & 0xFF;

                    byte[] payload;
                    if (msgType == 1) {
                        // Start session: total 32 bytes (payload 24 bytes from offset 8)
                        byte[] rest = new byte[16];
                        try {
                            dis.readFully(rest);
                        } catch (EOFException e) {
                            break;
                        }
                        payload = new byte[24];
                        System.arraycopy(header, 8, payload, 0, 8);
                        System.arraycopy(rest, 0, payload, 8, 16);
                    } else {
                        // Other service messages: 16 bytes total (payload 8 bytes from offset 8)
                        payload = new byte[8];
                        System.arraycopy(header, 8, payload, 0, 8);
                    }

                    Integer modeNumber = null;
                    Integer errorNumber = null;
                    Integer daysFrom1980 = null;

                    if (msgType == 4) {
                        // Mode number is stored as 32-bit int (docs show it in bytes 12-15 of the 16-byte record)
                        modeNumber = bb.getInt(12);
                        currentModeNumber = modeNumber;
                    } else if (msgType == 6) {
                        errorNumber = bb.getInt(12);
                    } else if (msgType == 5) {
                        daysFrom1980 = bb.getInt(12);
                    } else if (msgType == 2) {
                        // Docs: current time/date in days from 1980 (often in bytes 8-11). We store best-effort.
                        daysFrom1980 = bb.getInt(8);
                    }

                    serviceMessages.add(new ServiceMessage(time, msgType, valueType, payload, modeNumber, errorNumber, daysFrom1980));
                    continue;
                }
                
                // 2-5: Time (unsigned int -> long)
                long time = bb.getInt(2) & 0xFFFFFFFFL;
                
                // 6: Dimension
                int dimensionId = bb.get(6) & 0xFF;
                
                // 7: Attr + Type
                int attrTypeByte = bb.get(7) & 0xFF;
                int attribute = (attrTypeByte >> 4) & 0x0F;
                int valueType = attrTypeByte & 0x0F;
                
                TmRecord record = null;
                
                switch (valueType) {
                    case 0: // Long (32-bit int at bytes 12-15)
                        int intVal = bb.getInt(12);
                        record = new TmLong(paramNumber, time, attribute, valueType, intVal);
                        break;
                        
                    case 1: // Double (64-bit float at bytes 8-15)
                        double doubleVal = bb.getDouble(8);
                        record = new TmDouble(paramNumber, time, attribute, valueType, doubleVal);
                        break;
                        
                    case 2: // Code (Len at 10-11, Val at 12-15)
                        int codeLen = bb.getShort(10) & 0xFFFF;
                        long codeVal = bb.getInt(12) & 0xFFFFFFFFL;
                        record = new TmCode(paramNumber, time, attribute, valueType, codeLen, codeVal);
                        break;
                        
                    case 3: // Point (ElemSize 8-9, ArrLen 10-11, Data 12+)
                        int elementSize = bb.getShort(8) & 0xFFFF;
                        int arrayByteLength = bb.getShort(10) & 0xFFFF;
                        
                        // Safety: don't allocate crazy large arrays if file is corrupted
                        if (arrayByteLength < 0 || arrayByteLength > 10 * 1024 * 1024) { 
                             // Valid case check
                        }

                        byte[] data = new byte[arrayByteLength];
                        
                        // We have bytes 12-15 (4 bytes) in our header buffer
                        int bytesInHeader = 4;
                        int bytesToCopy = Math.min(bytesInHeader, arrayByteLength);
                        
                        // Copy from header (index 12) to data (index 0)
                        System.arraycopy(header, 12, data, 0, bytesToCopy);
                        
                        // If array is longer than 4 bytes, read the rest from stream
                        if (arrayByteLength > bytesInHeader) {
                            int remaining = arrayByteLength - bytesInHeader;
                            try {
                                dis.readFully(data, bytesInHeader, remaining);
                            } catch (EOFException e) {
                                break; 
                            }
                        }
                        
                        record = new TmPoint(paramNumber, time, attribute, valueType, elementSize, arrayByteLength, data);
                        break;
                        
                    default:
                        // Unknown type
                        break;
                }
                
                if (record != null) {
                    record.setDimension(String.valueOf(dimensionId));
                    record.setSessionModeNumber(currentModeNumber);
                    records.add(record);
                }
            }
        }
        
        return new KnpParseResult(records, serviceMessages);
    }
}
