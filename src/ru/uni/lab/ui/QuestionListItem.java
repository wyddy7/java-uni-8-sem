package ru.uni.lab.ui;

import ru.uni.lab.service.StatQuestion;

public class QuestionListItem {
    private final StatQuestion question;
    private boolean selected;

    public QuestionListItem(StatQuestion question, boolean selected) {
        this.question = question;
        this.selected = selected;
    }

    public StatQuestion getQuestion() {
        return question;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return question.getTitle();
    }
}

