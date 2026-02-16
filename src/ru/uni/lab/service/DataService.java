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

        long dataCount = getRecordCount();
        long svcCount = getServiceMessageCount();
        long totalCount = dataCount + svcCount;
        int uniqueInFile = getUniqueParameters().size();
        int knownInXml = getAllKnownParameters().size();

        // Time range (data records only)
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for (TmRecord r : allRecords) {
            long t = r.getTime();
            if (t < minTime) minTime = t;
            if (t > maxTime) maxTime = t;
        }

        insights.add("Сводка загрузки");
        insights.add(String.format("• Записей: %d (полезных %d, служебных %d)", totalCount, dataCount, svcCount));
        insights.add(String.format("• Уникальных параметров в файле: %d", uniqueInFile));
        insights.add(String.format("• Параметров в XML: %d", knownInXml));
        if (dataCount > 0) {
            insights.add(String.format("• Время (по полезным): %s … %s (%.1f мин)",
                    formatTime(minTime), formatTime(maxTime), (maxTime - minTime) / 60000.0));
        }

        // Mode overview (based on parsed mode change messages applied to data records)
        Map<Integer, Long> modeCounts = new HashMap<>();
        int modeSegments = 0;
        Integer prevMode = null;
        for (TmRecord r : allRecords) {
            int m = r.getSessionModeNumber();
            modeCounts.merge(m, 1L, Long::sum);
            if (prevMode == null || m != prevMode) {
                modeSegments++;
                prevMode = m;
            }
        }
        long nonNp = allRecords.stream().filter(r -> r.getSessionModeNumber() != 1).count();
        insights.add("");
        insights.add("Режимы (по «смена режима»)");
        insights.add(String.format("• Сегментов: %d", modeSegments));
        insights.add(String.format("• Записей вне НП: %d (%.2f%%)", nonNp, dataCount == 0 ? 0.0 : (100.0 * nonNp / dataCount)));
        // Show top modes by count
        modeCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> insights.add(String.format("  - %s: %d", modeName(e.getKey()), e.getValue())));

        // Deviations overview
        Map<Integer, Integer> deviationByParam = new HashMap<>();
        Map<Integer, Integer> deviationByAttr = new HashMap<>();
        long deviationsTotal = 0;
        for (TmRecord rec : allRecords) {
            int attr = rec.getAttribute();
            if (attr != 0) {
                deviationsTotal++;
                deviationByParam.merge(rec.getParameterNumber(), 1, Integer::sum);
                deviationByAttr.merge(attr, 1, Integer::sum);
            }
        }

        insights.add("");
        insights.add("Отклонения (attribute != 0)");
        if (deviationsTotal == 0) {
            insights.add("• Отклонений не найдено");
        } else {
            insights.add(String.format("• Всего событий: %d (%.2f%% от полезных)",
                    deviationsTotal, dataCount == 0 ? 0.0 : (100.0 * deviationsTotal / dataCount)));

            // Attribute breakdown
            deviationByAttr.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(5)
                    .forEach(e -> insights.add(String.format("  - %s: %d", getAttributeText(e.getKey()), e.getValue())));

            // Top affected params
            insights.add("• Топ параметров по числу отклонений:");
            deviationByParam.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(8)
                    .forEach(e -> {
                        String name = getParameterName(e.getKey());
                        insights.add(String.format("  - %s (ID: %d): %d", name, e.getKey(), e.getValue()));
                    });
        }

        // Time gaps (data only)
        insights.add("");
        insights.add("Временная шкала");
        List<TmRecord> sorted = new ArrayList<>(allRecords);
        sorted.sort(Comparator.comparingLong(TmRecord::getTime));

        long prevTime = -1;
        int gapCount = 0;
        long maxGap = 0;
        long maxGapStart = -1;
        long maxGapEnd = -1;

        for (TmRecord rec : sorted) {
            if (prevTime != -1) {
                long diff = rec.getTime() - prevTime;
                // Gap > 1 minute (60000 ms) and not a day wraparound
                if (diff > 60000 && diff < 80000000) {
                    gapCount++;
                    if (diff > maxGap) {
                        maxGap = diff;
                        maxGapStart = prevTime;
                        maxGapEnd = rec.getTime();
                    }
                }
            }
            prevTime = rec.getTime();
        }
        if (gapCount == 0) {
            insights.add("• Разрывов > 1 мин не найдено");
        } else {
            insights.add(String.format("• Разрывы > 1 мин: %d", gapCount));
            insights.add(String.format("• Макс. разрыв: %.1f мин (%s → %s)",
                    maxGap / 60000.0, formatTime(maxGapStart), formatTime(maxGapEnd)));
        }

        // Helpful “where to look” sections
        insights.add("");
        insights.add("Куда смотреть (быстрые подсказки)");

        // Top parameters by record count
        Map<Integer, Long> byCount = allRecords.stream()
                .collect(Collectors.groupingBy(TmRecord::getParameterNumber, Collectors.counting()));
        insights.add("• Топ параметров по числу измерений:");
        byCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .forEach(e -> insights.add(String.format("  - %s (ID: %d): %d",
                        getParameterName(e.getKey()), e.getKey(), e.getValue())));

        // Numeric stats for top dense numeric params
        class NumAcc {
            long count = 0;
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            String name = "";
            String dim = "";
            void add(double v, String n, String d) {
                count++;
                sum += v;
                if (v < min) min = v;
                if (v > max) max = v;
                if (name.isEmpty() && n != null) name = n;
                if (dim.isEmpty() && d != null) dim = d;
            }
        }

        Map<Integer, NumAcc> numeric = new HashMap<>();
        for (TmRecord r : allRecords) {
            double v = Double.NaN;
            if (r instanceof TmLong) v = ((TmLong) r).getValue();
            else if (r instanceof TmDouble) v = ((TmDouble) r).getValue();
            if (Double.isNaN(v)) continue;

            NumAcc acc = numeric.computeIfAbsent(r.getParameterNumber(), k -> new NumAcc());
            acc.add(v, r.getParameterName(), r.getDimension());
        }

        List<Map.Entry<Integer, NumAcc>> topNumeric = numeric.entrySet().stream()
                .filter(e -> e.getValue().count >= 20)
                .filter(e -> Math.abs(e.getValue().max - e.getValue().min) > 1e-9)
                .sorted((a, b) -> Long.compare(b.getValue().count, a.getValue().count))
                .limit(5)
                .collect(Collectors.toList());

        if (!topNumeric.isEmpty()) {
            insights.add("• Числовые параметры (min/max/avg) по самым плотным:");
            for (Map.Entry<Integer, NumAcc> e : topNumeric) {
                NumAcc a = e.getValue();
                String dim = (a.dim == null || a.dim.isBlank()) ? "" : (" " + a.dim);
                insights.add(String.format("  - %s (ID: %d): min=%.3f, max=%.3f, avg=%.3f%s (n=%d)",
                        a.name.isEmpty() ? getParameterName(e.getKey()) : a.name,
                        e.getKey(),
                        a.min, a.max, (a.sum / a.count), dim, a.count));
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

    private String formatTime(long msFromStartOfDay) {
        long hours = msFromStartOfDay / 3600000;
        long minutes = (msFromStartOfDay % 3600000) / 60000;
        long seconds = (msFromStartOfDay % 60000) / 1000;
        long ms = msFromStartOfDay % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
    }

    private String modeName(int mode) {
        if (mode == 1) return "НП";
        if (mode == 0) return "недостоверный режим";
        if (mode >= 2 && mode <= 8) return "ВП(" + mode + ")";
        return "режим(" + mode + ")";
    }
}
