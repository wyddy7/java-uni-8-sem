package ru.uni.lab.ui;

import javax.swing.*;
import java.awt.*;

public class QuestionCheckboxRenderer extends JCheckBox implements ListCellRenderer<QuestionListItem> {

    public QuestionCheckboxRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends QuestionListItem> list,
                                                  QuestionListItem value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        setText(value != null ? value.toString() : "");
        setSelected(value != null && value.isSelected());

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}

