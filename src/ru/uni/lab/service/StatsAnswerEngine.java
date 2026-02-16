package ru.uni.lab.service;

import ru.uni.lab.model.ServiceMessage;
import ru.uni.lab.model.TmCode;
import ru.uni.lab.model.TmDouble;
import ru.uni.lab.model.TmLong;
import ru.uni.lab.model.TmPoint;
import ru.uni.lab.model.TmRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StatsAnswerEngine {

    public String answerMany(List<StatQuestion> questions, Set<String> selectedIds, DataService ds) {
        StringBuilder sb = new StringBuilder();
        for (StatQuestion q : questions) {
            if (!selectedIds.contains(q.getId())) continue;
            sb.append("## ").append(q.getTitle()).append("\n");
            sb.append(answerOne(q.getId(), ds)).append("\n");
            sb.append("\n---\n\n");
        }
        return sb.toString().trim();
    }

    public String answerOne(String id, DataService ds) {
        switch (id) {
            case "records_count_total":
            case "s_total":
                return String.valueOf(ds.getRecordCount() + ds.getServiceMessageCount());
            case "data_records_count":
            case "s_tm":
                return String.valueOf(ds.getRecordCount());
            case "svc_records_count_total":
            case "svc_records_count_total_alt":
            case "s_sluz":
                return String.valueOf(ds.getServiceMessageCount());
            case "s_xml":
                return String.valueOf(ds.getAllKnownParameters().size());

            case "long_records_count":
                return String.valueOf(countRecordsByValueType(ds.getAllRecords(), 0));
            case "double_records_count":
                return String.valueOf(countRecordsByValueType(ds.getAllRecords(), 1));
            case "code_records_count":
                return String.valueOf(countRecordsByValueType(ds.getAllRecords(), 2));
            case "point_records_count":
                return String.valueOf(countRecordsByValueType(ds.getAllRecords(), 3));

            case "distinct_params_count":
                return String.valueOf(ds.getUniqueParameters().size());
            case "distinct_long_params_count":
            case "q0_long_params_distinct":
                return String.valueOf(distinctParamsByValueType(ds.getAllRecords(), 0));
            case "distinct_double_params_count":
            case "q1_double_params_distinct":
                return String.valueOf(distinctParamsByValueType(ds.getAllRecords(), 1));
            case "distinct_code_params_count":
            case "q2_code_params_distinct":
                return String.valueOf(distinctParamsByValueType(ds.getAllRecords(), 2));
            case "distinct_point_params_count":
            case "q3_point_params_distinct":
                return String.valueOf(distinctParamsByValueType(ds.getAllRecords(), 3));

            case "svc_records_count_mode_change":
                return String.valueOf(countServiceByType(ds.getServiceMessages(), 4));
            case "svc_records_count_error":
                return String.valueOf(countServiceByType(ds.getServiceMessages(), 6));
            case "svc_records_count_empty":
                return String.valueOf(countServiceByType(ds.getServiceMessages(), 0));
            case "svc_records_count_current_time":
                return String.valueOf(countServiceByType(ds.getServiceMessages(), 2));
            case "svc_records_count_start_or_end":
                return String.valueOf(countServiceByType(ds.getServiceMessages(), 1) + countServiceByType(ds.getServiceMessages(), 3));

            case "svc_mode_change_segments_np":
                return String.valueOf(countModeSegments(ds.getAllRecords(), 1));
            case "svc_mode_change_segments_invalid":
                return String.valueOf(countModeSegments(ds.getAllRecords(), 0));
            case "svc_mode_change_segments_vp_2_8":
                return String.valueOf(countModeSegmentsInRange(ds.getAllRecords(), 2, 8));
            case "svc_mode_change_segments_not_np":
                return String.valueOf(countModeSegmentsNot(ds.getAllRecords(), 1));
            case "records_count_non_np_mode":
                return String.valueOf(ds.getAllRecords().stream().filter(r -> r.getSessionModeNumber() != 1).count());

            case "point_records_len_lt_4":
                return String.valueOf(ds.getAllRecords().stream()
                        .filter(r -> r instanceof TmPoint)
                        .map(r -> (TmPoint) r)
                        .filter(p -> p.getArrayLength() < 4)
                        .count());
            case "point_params_len_gt_4":
                return String.valueOf(ds.getAllRecords().stream()
                        .filter(r -> r instanceof TmPoint)
                        .map(r -> (TmPoint) r)
                        .filter(p -> p.getArrayLength() > 4)
                        .map(TmRecord::getParameterNumber)
                        .distinct()
                        .count());

            case "code_params_len_lt_8":
                return String.valueOf(ds.getAllRecords().stream()
                        .filter(r -> r instanceof TmCode)
                        .map(r -> (TmCode) r)
                        .filter(c -> c.getCodeLength() < 8)
                        .map(TmRecord::getParameterNumber)
                        .distinct()
                        .count());
            case "code_params_len_gt_8":
                return String.valueOf(ds.getAllRecords().stream()
                        .filter(r -> r instanceof TmCode)
                        .map(r -> (TmCode) r)
                        .filter(c -> c.getCodeLength() > 8)
                        .map(TmRecord::getParameterNumber)
                        .distinct()
                        .count());

            case "param_min_records":
                return answerParamWithMinRecords(ds);
            case "params_with_two_records":
                return answerParamsWithExactRecordCount(ds, 2);

            case "dimensions_most_frequent":
                return answerTopDimensions(ds, true);
            case "dimensions_most_rare":
                return answerTopDimensions(ds, false);

            case "temp_params_count":
                return String.valueOf(countTemperatureParams(ds));
            case "temp_params_max_records":
                return answerTemperatureParamsMax(ds);

            case "text_params_count_in_xml":
                return String.valueOf(ds.getAllKnownParameters().values().stream().filter(p -> p.getValues() != null && !p.getValues().isEmpty()).count());

            case "present_params_sorted_by_name":
                return answerParamsSortedByNamePresentInFile(ds);
            case "all_params_sorted_by_name":
                return answerAllParamsSortedByName(ds);

            case "check_total_params_vs_sum_types":
                return answerCheckDistinctParamsVsSumDistinctByType(ds);

            case "mode_segments_chronological_counts":
                return answerModeSegmentsChronological(ds.getAllRecords());
            case "mode_segments_counts_np_vp33":
                return answerModeSegmentsCountsForNpAndVp33(ds.getAllRecords());

            default:
                return "Не реализовано (id=" + id + ")";
        }
    }

    private long countRecordsByValueType(List<TmRecord> records, int type) {
        return records.stream().filter(r -> r.getValueType() == type).count();
    }

    private long distinctParamsByValueType(List<TmRecord> records, int type) {
        return records.stream().filter(r -> r.getValueType() == type).map(TmRecord::getParameterNumber).distinct().count();
    }

    private long countServiceByType(List<ServiceMessage> msgs, int msgType) {
        if (msgs == null) return 0;
        return msgs.stream().filter(m -> m.getMessageType() == msgType).count();
    }

    private int countModeSegments(List<TmRecord> records, int modeNumber) {
        int segments = 0;
        Integer prev = null;
        for (TmRecord r : records) {
            int m = r.getSessionModeNumber();
            if (prev == null) {
                prev = m;
                if (m == modeNumber) segments++;
                continue;
            }
            if (m != prev) {
                if (m == modeNumber) segments++;
                prev = m;
            }
        }
        return segments;
    }

    private int countModeSegmentsInRange(List<TmRecord> records, int min, int max) {
        int segments = 0;
        Integer prev = null;
        for (TmRecord r : records) {
            int m = r.getSessionModeNumber();
            boolean inRange = (m >= min && m <= max);
            if (prev == null) {
                prev = m;
                if (inRange) segments++;
                continue;
            }
            if (m != prev) {
                if (inRange) segments++;
                prev = m;
            }
        }
        return segments;
    }

    private int countModeSegmentsNot(List<TmRecord> records, int excludedMode) {
        int segments = 0;
        Integer prev = null;
        for (TmRecord r : records) {
            int m = r.getSessionModeNumber();
            boolean ok = (m != excludedMode);
            if (prev == null) {
                prev = m;
                if (ok) segments++;
                continue;
            }
            if (m != prev) {
                if (ok) segments++;
                prev = m;
            }
        }
        return segments;
    }

    private String answerParamWithMinRecords(DataService ds) {
        Map<Integer, Long> counts = ds.getAllRecords().stream()
                .collect(Collectors.groupingBy(TmRecord::getParameterNumber, Collectors.counting()));
        if (counts.isEmpty()) return "Нет данных";
        Map.Entry<Integer, Long> min = counts.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getValue))
                .orElse(null);
        if (min == null) return "Нет данных";
        String name = ds.getUniqueParameters().getOrDefault(min.getKey(), "Параметр " + min.getKey());
        return String.format(Locale.ROOT, "%s (ID: %d) — %d записей", name, min.getKey(), min.getValue());
    }

    private String answerParamsWithExactRecordCount(DataService ds, long exact) {
        Map<Integer, Long> counts = ds.getAllRecords().stream()
                .collect(Collectors.groupingBy(TmRecord::getParameterNumber, Collectors.counting()));
        List<Integer> ids = counts.entrySet().stream()
                .filter(e -> e.getValue() == exact)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return "Не найдено";
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids) {
            sb.append(String.format("%d — %s\n", id, ds.getUniqueParameters().getOrDefault(id, "Параметр " + id)));
        }
        return sb.toString().trim();
    }

    private String answerTopDimensions(DataService ds, boolean mostFrequent) {
        Map<String, Long> counts = ds.getAllRecords().stream()
                .map(TmRecord::getDimension)
                .filter(d -> d != null && !d.isBlank())
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        if (counts.isEmpty()) return "Нет данных";

        Comparator<Map.Entry<String, Long>> cmp = Comparator.comparingLong(Map.Entry::getValue);
        if (mostFrequent) cmp = cmp.reversed();

        return counts.entrySet().stream()
                .sorted(cmp.thenComparing(Map.Entry::getKey))
                .limit(10)
                .map(e -> e.getKey() + " — " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    private long countTemperatureParams(DataService ds) {
        // Heuristic: name starts with 'Т' or 'T' (trimmed)
        Set<Integer> ids = new HashSet<>();
        for (TmRecord r : ds.getAllRecords()) {
            String name = r.getParameterName();
            if (name == null) continue;
            String t = name.trim();
            if (t.startsWith("Т") || t.startsWith("T")) {
                ids.add(r.getParameterNumber());
            }
        }
        return ids.size();
    }

    private String answerTemperatureParamsMax(DataService ds) {
        Map<Integer, Long> counts = ds.getAllRecords().stream()
                .filter(r -> {
                    String name = r.getParameterName();
                    if (name == null) return false;
                    String t = name.trim();
                    return t.startsWith("Т") || t.startsWith("T");
                })
                .collect(Collectors.groupingBy(TmRecord::getParameterNumber, Collectors.counting()));

        if (counts.isEmpty()) return "Не найдено";
        long max = counts.values().stream().mapToLong(v -> v).max().orElse(0);
        List<Integer> ids = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        sb.append("Максимум записей: ").append(max).append("\n");
        for (Integer id : ids) {
            sb.append(String.format("%d — %s\n", id, ds.getUniqueParameters().getOrDefault(id, "Параметр " + id)));
        }
        return sb.toString().trim();
    }

    private String answerParamsSortedByNamePresentInFile(DataService ds) {
        // Present in file, sorted by name (case-insensitive)
        TreeMap<String, List<Integer>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<Integer, String> e : ds.getUniqueParameters().entrySet()) {
            map.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Integer>> e : map.entrySet()) {
            List<Integer> ids = e.getValue();
            ids.sort(Integer::compareTo);
            for (Integer id : ids) {
                sb.append(e.getKey()).append(" — ").append(id).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String answerAllParamsSortedByName(DataService ds) {
        TreeMap<String, List<Integer>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ds.getAllKnownParameters().forEach((id, info) -> map.computeIfAbsent(info.getName(), k -> new ArrayList<>()).add(id));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Integer>> e : map.entrySet()) {
            List<Integer> ids = e.getValue();
            ids.sort(Integer::compareTo);
            for (Integer id : ids) {
                sb.append(e.getKey()).append(" — ").append(id).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String answerCheckDistinctParamsVsSumDistinctByType(DataService ds) {
        long distinctAll = ds.getUniqueParameters().size();
        long q0 = distinctParamsByValueType(ds.getAllRecords(), 0);
        long q1 = distinctParamsByValueType(ds.getAllRecords(), 1);
        long q2 = distinctParamsByValueType(ds.getAllRecords(), 2);
        long q3 = distinctParamsByValueType(ds.getAllRecords(), 3);
        long sum = q0 + q1 + q2 + q3;
        boolean eq = (distinctAll == sum);
        return String.format(Locale.ROOT,
                "distinct_all=%d\nq0=%d, q1=%d, q2=%d, q3=%d\nsum=%d\nСовпадает: %s",
                distinctAll, q0, q1, q2, q3, sum, eq ? "ДА" : "НЕТ");
    }

    private String answerModeSegmentsChronological(List<TmRecord> records) {
        if (records.isEmpty()) return "Нет данных";
        StringBuilder sb = new StringBuilder();
        int currentMode = records.get(0).getSessionModeNumber();
        long count = 0;
        for (TmRecord r : records) {
            int m = r.getSessionModeNumber();
            if (m != currentMode) {
                sb.append(modeName(currentMode)).append(" — ").append(count).append("\n");
                currentMode = m;
                count = 0;
            }
            count++;
        }
        sb.append(modeName(currentMode)).append(" — ").append(count);
        return sb.toString();
    }

    private String answerModeSegmentsCountsForNpAndVp33(List<TmRecord> records) {
        if (records.isEmpty()) return "Нет данных";
        // В исходнике упоминается "ВПЗЗ" (скорее всего ВП-33). В документации: 2/3 связаны с ВП-33.
        // Будем считать VP-33 как режимы 2 и 3 (оба).
        StringBuilder sb = new StringBuilder();
        int currentMode = records.get(0).getSessionModeNumber();
        long count = 0;
        for (TmRecord r : records) {
            int m = r.getSessionModeNumber();
            if (m != currentMode) {
                appendIfNpOrVp33(sb, currentMode, count);
                currentMode = m;
                count = 0;
            }
            count++;
        }
        appendIfNpOrVp33(sb, currentMode, count);
        return sb.length() == 0 ? "Не найдено сегментов НП/ВП-33" : sb.toString().trim();
    }

    private void appendIfNpOrVp33(StringBuilder sb, int mode, long count) {
        if (mode == 1) {
            sb.append("НП — ").append(count).append("\n");
        } else if (mode == 2 || mode == 3) {
            sb.append("ВП-33 — ").append(count).append("\n");
        }
    }

    private String modeName(int mode) {
        if (mode == 1) return "НП";
        if (mode == 0) return "недостоверный режим";
        if (mode >= 2 && mode <= 8) return "ВП(" + mode + ")";
        return "режим(" + mode + ")";
    }
}

