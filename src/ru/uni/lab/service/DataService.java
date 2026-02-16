package ru.uni.lab.service;

import ru.uni.lab.model.ParameterInfo;
import ru.uni.lab.model.ServiceMessage;
import ru.uni.lab.model.TmDouble;
import ru.uni.lab.model.TmLong;
import ru.uni.lab.model.TmRecord;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DataService {
    private static DataService instance;
    
    private List<TmRecord> allRecords;
    private List<ServiceMessage> serviceMessages;
    private Map<Integer, ParameterInfo> paramInfoMap;
    private Map<Integer, String> dimensions;
    private List<String> systemLog;
    private List<String> insights;
    
    private DataService() {
        allRecords = Collections.emptyList();
        serviceMessages = Collections.emptyList();
        paramInfoMap = Collections.emptyMap();
        dimensions = Collections.emptyMap();
        systemLog = new ArrayList<>();
        insights = new ArrayList<>();
        logEvent("Application started. DataService initialized.");
    }
    
    public static synchronized DataService getInstance() {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }
    
    public void logEvent(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        systemLog.add("[" + timestamp + "] " + message);
    }
    
    public List<String> getSystemLog() {
        return new ArrayList<>(systemLog);
    }
    
    public List<String> getInsights() {
        if (insights.isEmpty() && !allRecords.isEmpty()) {
            generateInsights();
        }
        return new ArrayList<>(insights);
    }
    
    public void loadData(File knpFile, File xmlFile, File dimFile) throws Exception {
        logEvent("Starting file loading sequence...");
        logEvent("Reading XML: " + xmlFile.getName());
        
        // Parse XML
        XmlParser xmlParser = new XmlParser();
        this.paramInfoMap = xmlParser.parse(xmlFile);
        logEvent("XML parsed. Found " + paramInfoMap.size() + " parameter definitions.");
        
        // Parse Dimensions
        logEvent("Reading Dimensions: " + dimFile.getName());
        DimensionParser dimParser = new DimensionParser();
        this.dimensions = dimParser.parse(dimFile);
        logEvent("Dimensions loaded.");
        
        // Parse KNP
        logEvent("Reading Binary KNP: " + knpFile.getName());
        KnpParser knpParser = new KnpParser();
        KnpParseResult parseResult = knpParser.parseWithServiceMessages(knpFile);
        List<TmRecord> records = parseResult.getDataRecords();
        this.serviceMessages = parseResult.getServiceMessages();
        logEvent("Binary parsing complete. Data records: " + records.size() + ", service messages: " + serviceMessages.size());
        
        // Post-process records to enrich with names and dimension strings
        for (TmRecord record : records) {
            // Set Name
            ParameterInfo info = paramInfoMap.get(record.getParameterNumber());
            String name = (info != null) ? info.getName() : "Параметр " + record.getParameterNumber();
            record.setParameterName(name);
            
            // Set Dimension
            try {
                String dimStrRaw = record.getDimension();
                if (dimStrRaw != null && !dimStrRaw.isEmpty()) {
                    int dimId = Integer.parseInt(dimStrRaw);
                    if (dimId >= 32) {
                        String dimStr = dimensions.get(dimId);
                        record.setDimension(dimStr != null ? dimStr : "");
                    } else {
                        record.setDimension(""); 
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        this.allRecords = records;
        
        long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        logEvent("FILE CLOSED. All data residing in RAM. Memory used: " + (memory / 1024 / 1024) + " MB");
        
        generateInsights();
    }

    public List<ServiceMessage> getServiceMessages() {
        return serviceMessages;
    }

    public int getServiceMessageCount() {
        return serviceMessages != null ? serviceMessages.size() : 0;
    }
    
    private void generateInsights() {
        insights.clear();
        if (allRecords.isEmpty()) return;
        
        insights.add("=== АВТОМАТИЧЕСКИЙ АНАЛИЗ ТЕЛЕМЕТРИИ ===");
        
        // 1. Top non-normal parameters
        Map<Integer, Integer> errorCounts = new HashMap<>();
        for (TmRecord rec : allRecords) {
            if (rec.getAttribute() != 0) { // 0 is Normal
                errorCounts.merge(rec.getParameterNumber(), 1, Integer::sum);
            }
        }
        
        if (!errorCounts.isEmpty()) {
            insights.add("\n[!] ТОП СИСТЕМ С ОТКЛОНЕНИЯМИ:");
            errorCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(e -> {
                    String name = getParameterName(e.getKey());
                    insights.add(String.format("   • %s (ID: %d): %d событий 'Не норма'", name, e.getKey(), e.getValue()));
                });
        } else {
            insights.add("\n[OK] Все системы работают в штатном режиме. Отклонений не найдено.");
        }
        
        // 2. Time gaps
        insights.add("\n[i] АНАЛИЗ ВРЕМЕННОЙ ШКАЛЫ:");
        long prevTime = -1;
        int gapCount = 0;
        long maxGap = 0;
        
        // Sort by time to be sure
        List<TmRecord> sorted = new ArrayList<>(allRecords);
        sorted.sort(Comparator.comparingLong(TmRecord::getTime));
        
        for (TmRecord rec : sorted) {
            if (prevTime != -1) {
                long diff = rec.getTime() - prevTime;
                // Gap > 1 minute (60000 ms) and not a day wraparound
                if (diff > 60000 && diff < 80000000) { 
                    gapCount++;
                    if (diff > maxGap) maxGap = diff;
                }
            }
            prevTime = rec.getTime();
        }
        
        if (gapCount > 0) {
            insights.add(String.format("   • Обнаружено разрывов связи (>1 мин): %d", gapCount));
            insights.add(String.format("   • Максимальный перерыв: %.1f мин", maxGap / 60000.0));
        } else {
            insights.add("   • Поток данных непрерывен.");
        }
        
        // 3. Stats for numeric params
        insights.add("\n[i] СТАТИСТИКА ПО КЛЮЧЕВЫМ ПАРАМЕТРАМ:");
        int numericParams = 0;
        for (Integer paramId : getUniqueParameters().keySet()) {
            List<TmRecord> recs = getRecordsForParameter(paramId);
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            int count = 0;
            
            for (TmRecord r : recs) {
                double val = Double.NaN;
                if (r instanceof TmLong) val = ((TmLong) r).getValue();
                else if (r instanceof TmDouble) val = ((TmDouble) r).getValue();
                
                if (!Double.isNaN(val)) {
                    sum += val;
                    if (val < min) min = val;
                    if (val > max) max = val;
                    count++;
                }
            }
            
            if (count > 10 && numericParams < 3) { // Show first 3 numeric params as sample
                String name = getParameterName(paramId);
                // Simple heuristic to avoid showing IDs or codes as stats
                if (Math.abs(max - min) > 0.0001) {
                    insights.add(String.format("   • %s: Min=%.2f, Max=%.2f, Avg=%.2f", name, min, max, sum/count));
                    numericParams++;
                }
            }
        }
        
        logEvent("Analysis complete. Generated " + insights.size() + " insight lines.");
    }
    
    private String getParameterName(int id) {
        ParameterInfo info = paramInfoMap.get(id);
        return (info != null) ? info.getName() : "Параметр " + id;
    }
    
    public List<TmRecord> getAllRecords() {
        return allRecords;
    }
    
    public List<TmRecord> getRecordsForParameter(int paramNumber) {
        return allRecords.stream()
                .filter(r -> r.getParameterNumber() == paramNumber)
                .collect(Collectors.toList());
    }
    
    public Map<Integer, String> getUniqueParameters() {
        // Map of Param Number -> Param Name
        // Only for parameters that exist in the KNP file
        return allRecords.stream()
                .collect(Collectors.toMap(
                    TmRecord::getParameterNumber,
                    TmRecord::getParameterName,
                    (v1, v2) -> v1, // In case of duplicate keys
                    java.util.TreeMap::new // Sort by number
                ));
    }
    
    public ParameterInfo getParameterInfo(int paramId) {
        return paramInfoMap.get(paramId);
    }
    
    public Map<Integer, ParameterInfo> getAllKnownParameters() {
        return paramInfoMap;
    }

    public long getRecordCount() {
        return allRecords.size();
    }
    
    public String getAttributeText(int attribute) {
        switch (attribute) {
            case 0: return "Норма";
            case 1: return "Не норма (нижн)";
            case 2: return "Внимание (нижн)";
            case 3: return "Внимание (верхн)";
            case 4: return "Не норма (верхн)";
            case 8: return "Недостоверно";
            case 9: return "Вне диапазона";
            case 10: return "Зашкаливание";
            default: return String.valueOf(attribute);
        }
    }
}
