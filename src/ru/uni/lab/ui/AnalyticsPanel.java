package ru.uni.lab.ui;

import ru.uni.lab.service.DataService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AnalyticsPanel extends JPanel {

    private JTextArea insightsArea;

    public AnalyticsPanel() {
        setLayout(new BorderLayout());
        
        insightsArea = new JTextArea();
        insightsArea.setEditable(false);
        insightsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        insightsArea.setBackground(new Color(245, 250, 255)); // Light blue-ish
        
        add(new JScrollPane(insightsArea), BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Обновить анализ");
        refreshBtn.addActionListener(e -> updateContent());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public void updateContent() {
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
    }
}
