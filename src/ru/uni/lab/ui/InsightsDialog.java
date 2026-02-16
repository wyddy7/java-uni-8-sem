package ru.uni.lab.ui;

import javax.swing.*;
import java.awt.*;

public class InsightsDialog extends JDialog {

    private final InsightsPanel insightsPanel;

    public InsightsDialog(Frame owner) {
        super(owner, "Аналитика и лог", false);
        setLayout(new BorderLayout());
        setSize(900, 650);
        setLocationRelativeTo(owner);

        insightsPanel = new InsightsPanel();
        add(insightsPanel, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Закрыть");
        closeBtn.addActionListener(e -> setVisible(false));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public void refresh() {
        insightsPanel.updateContent();
    }
}

