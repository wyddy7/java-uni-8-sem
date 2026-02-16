package ru.uni.lab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class DimensionParser {

    public Map<Integer, String> parse(File file) throws Exception {
        Map<Integer, String> dimensionMap = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(file), 
                    "Windows-1251" // Critical requirement
                )
        )) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // "Empty lines should be ignored" - but does the line number increment?
                // TZ Example:
                // (empty line) -> line 1?
                // 8 -> line 2?
                // "Line number (starting from 1) = dimension number"
                // "Empty lines should be ignored" usually means we don't put them in the map, 
                // BUT do they consume a line number?
                // The example:
                // 150: (empty string)
                // 151: (empty string)
                // 152: 8
                // If 8 is at 152, then empty lines DO count for line numbering.
                // Wait, looking at the snippet in TZ:
                // 151: (empty)
                // 152: 8
                // 153: 16
                // ...
                // 163: %
                // And the code snippet:
                // if (line.trim().length() > 0) { dimensionMap.put(lineNumber, line.trim()); }
                // lineNumber++;
                // The lineNumber++ is OUTSIDE the if. So yes, empty lines consume a number.
                
                if (!trimmed.isEmpty()) {
                    dimensionMap.put(lineNumber, trimmed);
                }
                lineNumber++;
            }
        }
        
        return dimensionMap;
    }
}
