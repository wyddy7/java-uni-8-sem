package ru.uni.lab.ui;

import ru.uni.lab.service.DataService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LogPanel extends JPanel {

    private JTextArea logArea;

    public LogPanel() {
        setLayout(new BorderLayout());
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setForeground(new Color(50, 50, 50));
        logArea.setBackground(new Color(240, 240, 240));
        
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Обновить");
        refreshBtn.addActionListener(e -> updateContent());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public void updateContent() {
        StringBuilder sbLog = new StringBuilder();
        List<String> log = DataService.getInstance().getSystemLog();
        for (String line : log) {
            sbLog.append(line).append("\n");
        }
        
        // Removed manual append of "FILE CLOSED" message to avoid confusion.
        // The real close event is already logged by DataService at the correct timestamp.
        
        logArea.setText(sbLog.toString());
        try {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } catch (Exception ignored) {}
    }
}
