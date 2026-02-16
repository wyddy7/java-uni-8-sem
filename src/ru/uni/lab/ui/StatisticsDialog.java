package ru.uni.lab.ui;

import ru.uni.lab.service.DataService;
import ru.uni.lab.service.StatQuestion;
import ru.uni.lab.service.StatQuestionsLoader;
import ru.uni.lab.service.StatsAnswerEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatisticsDialog extends JDialog {

    public StatisticsDialog(Frame owner) {
        super(owner, "Статистика — вопросы", true);
        setLayout(new BorderLayout(10, 10));
        setSize(900, 650);
        setLocationRelativeTo(owner);

        DataService ds = DataService.getInstance();

        List<StatQuestion> questions = loadQuestionsSafe();
        List<QuestionListItem> items = new ArrayList<>();
        for (StatQuestion q : questions) {
            items.add(new QuestionListItem(q, false));
        }

        // Left: search + checklist
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Поиск по названию вопроса (выборы не сбрасываются)");

        DefaultListModel<QuestionListItem> listModel = new DefaultListModel<>();
        for (QuestionListItem it : items) listModel.addElement(it);

        JList<QuestionListItem> questionList = new JList<>(listModel);
        questionList.setCellRenderer(new QuestionCheckboxRenderer());
        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane listScroll = new JScrollPane(questionList);

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Вопросы"));
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        // Right: answers text
        JTextArea answerArea = new JTextArea();
        answerArea.setEditable(false);
        answerArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        answerArea.setLineWrap(true);
        answerArea.setWrapStyleWord(true);
        JScrollPane answerScroll = new JScrollPane(answerArea);
        answerScroll.setBorder(BorderFactory.createTitledBorder("Ответы (по отмеченным вопросам)"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, answerScroll);
        split.setDividerLocation(350);
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);

        JButton copyBtn = new JButton("Скопировать");
        JButton resetBtn = new JButton("Сбросить");
        JButton closeBtn = new JButton("Закрыть");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(resetBtn);
        btnPanel.add(copyBtn);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        StatsAnswerEngine engine = new StatsAnswerEngine();

        Runnable recompute = () -> {
            Set<String> selectedIds = new HashSet<>();
            for (QuestionListItem it : items) {
                if (it.isSelected()) selectedIds.add(it.getQuestion().getId());
            }
            if (selectedIds.isEmpty()) {
                answerArea.setText("Отметь слева один или несколько вопросов — здесь появятся ответы.");
                return;
            }
            answerArea.setText(engine.answerMany(questions, selectedIds, ds));
            answerArea.setCaretPosition(0);
        };

        // Toggle selection on click
        questionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = questionList.locationToIndex(e.getPoint());
                if (index < 0) return;
                QuestionListItem it = questionList.getModel().getElementAt(index);
                it.setSelected(!it.isSelected());
                questionList.repaint();
                recompute.run();
            }
        });

        // Search: filter view but preserve selection in underlying items
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void refresh() {
                String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
                listModel.clear();
                for (QuestionListItem it : items) {
                    if (q.isEmpty() || it.toString().toLowerCase().contains(q)) {
                        listModel.addElement(it);
                    }
                }
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });

        copyBtn.addActionListener(e -> {
            String text = answerArea.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        });

        resetBtn.addActionListener(e -> {
            for (QuestionListItem it : items) it.setSelected(false);
            questionList.repaint();
            recompute.run();
        });

        closeBtn.addActionListener(e -> dispose());

        recompute.run();
    }
    
    private List<StatQuestion> loadQuestionsSafe() {
        try {
            File file = new File("resources/stat_questions.txt");
            if (!file.exists()) {
                return List.of();
            }
            return new StatQuestionsLoader().loadFromFile(file);
        } catch (Exception ex) {
            return List.of();
        }
    }
    
    private String formatTime(long msFromStartOfDay) {
        long hours = msFromStartOfDay / 3600000;
        long minutes = (msFromStartOfDay % 3600000) / 60000;
        long seconds = (msFromStartOfDay % 60000) / 1000;
        long ms = msFromStartOfDay % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
    }
}
