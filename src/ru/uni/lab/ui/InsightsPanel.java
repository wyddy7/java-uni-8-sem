package ru.uni.lab.ui;

import ru.uni.lab.service.DataService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InsightsPanel extends JPanel {

    private JTextArea insightsArea;
    private JTextArea logArea;

    public InsightsPanel() {
        setLayout(new BorderLayout());
        
        // Insights Section (Top)
        JPanel insightsContainer = new JPanel(new BorderLayout());
        insightsContainer.setBorder(BorderFactory.createTitledBorder("Умная Аналитика (Smart Insights)"));
        
        insightsArea = new JTextArea();
        insightsArea.setEditable(false);
        insightsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        insightsArea.setBackground(new Color(245, 250, 255)); // Light blue-ish
        
        insightsContainer.add(new JScrollPane(insightsArea), BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Обновить анализ");
        refreshBtn.addActionListener(e -> updateContent());
        insightsContainer.add(refreshBtn, BorderLayout.SOUTH);

        // Log Section (Bottom)
        JPanel logContainer = new JPanel(new BorderLayout());
        logContainer.setBorder(BorderFactory.createTitledBorder("Системный журнал (Proof of Read)"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setForeground(new Color(50, 50, 50));
        logArea.setBackground(new Color(240, 240, 240));
        
        logContainer.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, insightsContainer, logContainer);
        splitPane.setResizeWeight(0.6); // 60% for insights
        add(splitPane, BorderLayout.CENTER);
    }

    public void updateContent() {
        // Update Insights
        StringBuilder sbInsights = new StringBuilder();
        List<String> insights = DataService.getInstance().getInsights();
        if (insights.isEmpty()) {
            sbInsights.append("Нет данных для анализа. Загрузите файлы.");
        } else {
            for (String line : insights) {
                sbInsights.append(line).append("\n");
            }
        }
        insightsArea.setText(sbInsights.toString());
        insightsArea.setCaretPosition(0);

        // Update Log
        StringBuilder sbLog = new StringBuilder();
        List<String> log = DataService.getInstance().getSystemLog();
        for (String line : log) {
            sbLog.append(line).append("\n");
        }
        
        // Add live RAM usage to log view (dynamic)
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        sbLog.append("\n--- LIVE MONITOR ---\n");
        sbLog.append("Current RAM Usage: ").append(usedMem).append(" MB\n");
        sbLog.append("Objects in Memory: ").append(DataService.getInstance().getRecordCount()).append(" records");
        
        logArea.setText(sbLog.toString());
        // Scroll log to bottom generally, but maybe better to stay? 
        // Usually logs auto-scroll.
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
