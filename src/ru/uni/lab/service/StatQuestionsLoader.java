package ru.uni.lab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StatQuestionsLoader {

    public List<StatQuestion> loadFromFile(File file) throws Exception {
        List<StatQuestion> questions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split("\\|", 3);
                if (parts.length < 2) continue;

                String id = parts[0].trim();
                String title = parts[1].trim();
                String sourceRef = parts.length >= 3 ? parts[2].trim() : "";
                if (id.isEmpty() || title.isEmpty()) continue;

                questions.add(new StatQuestion(id, title, sourceRef));
            }
        }
        return questions;
    }
}

