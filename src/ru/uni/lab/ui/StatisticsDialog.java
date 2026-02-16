package ru.uni.lab.ui;

import ru.uni.lab.service.DataService;

import javax.swing.*;
import java.awt.*;

public class StatisticsDialog extends JDialog {

    public StatisticsDialog(Frame owner) {
        super(owner, "Статистика", true);
        setLayout(new BorderLayout());
        setSize(400, 300);
        setLocationRelativeTo(owner);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        DataService service = DataService.getInstance();
        
        addLabel(content, "Всего записей: " + service.getRecordCount());
        addLabel(content, "Уникальных параметров: " + service.getUniqueParameters().size());
        
        // Calculate min/max time
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        
        if (service.getRecordCount() > 0) {
            for (var rec : service.getAllRecords()) {
                if (rec.getTime() < minTime) minTime = rec.getTime();
                if (rec.getTime() > maxTime) maxTime = rec.getTime();
            }
            addLabel(content, "Начало измерений: " + formatTime(minTime));
            addLabel(content, "Конец измерений: " + formatTime(maxTime));
        } else {
            addLabel(content, "Нет данных");
        }

        add(content, BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Закрыть");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
    
    private void addLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(label);
    }
    
    private String formatTime(long msFromStartOfDay) {
        long hours = msFromStartOfDay / 3600000;
        long minutes = (msFromStartOfDay % 3600000) / 60000;
        long seconds = (msFromStartOfDay % 60000) / 1000;
        long ms = msFromStartOfDay % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
    }
}
