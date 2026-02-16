package ru.uni.lab.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class RawXmlPanel extends JPanel {

    private JTextArea textArea;
    private File currentXmlFile;
    private JCheckBox formatCheckBox;
    private String currentRawText = "";

    public RawXmlPanel() {
        setLayout(new BorderLayout());
        
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setText("Выберите параметр для просмотра XML.");

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formatCheckBox = new JCheckBox("Форматировать (Pretty Print)");
        formatCheckBox.addActionListener(e -> updateDisplay());
        toolbar.add(formatCheckBox);
        add(toolbar, BorderLayout.NORTH);
    }

    public void setXmlFile(File file) {
        this.currentXmlFile = file;
        this.currentRawText = "";
        this.textArea.setText("Выберите параметр для просмотра XML.");
    }

    public void showParameterXml(int paramId) {
        if (currentXmlFile == null || !currentXmlFile.exists()) {
            textArea.setText("XML файл не загружен.");
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // Read full content (files are usually small enough, < 100MB)
                // If files are huge, this should be optimized, but for lab it's fine.
                String content = Files.readString(currentXmlFile.toPath(), StandardCharsets.UTF_8);
                
                String target = "number=\"" + paramId + "\"";
                int attrIndex = content.indexOf(target);
                
                if (attrIndex == -1) {
                    return "Параметр с ID " + paramId + " не найден в XML (возможно, он определен неявно или наследуется).";
                }
                
                // Find start of the tag: search backwards for "<Param"
                int startIndex = content.lastIndexOf("<Param", attrIndex);
                if (startIndex == -1) {
                    return "Ошибка структуры XML: атрибут найден, но открывающий тег нет.";
                }
                
                // Find end of the tag
                int endIndex = findClosingTagIndex(content, startIndex);
                if (endIndex == -1) {
                    // Fallback: just show some context
                    int endContext = Math.min(content.length(), attrIndex + 500);
                    return content.substring(startIndex, endContext) + "\n... (конец тега не найден)";
                }
                
                return content.substring(startIndex, endIndex);
            }

            @Override
            protected void done() {
                try {
                    currentRawText = get();
                    updateDisplay();
                } catch (Exception e) {
                    textArea.setText("Ошибка поиска в XML: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private int findClosingTagIndex(String content, int startIndex) {
        // Simple stack parser for nested <Param> tags
        int depth = 0;
        int index = startIndex;
        
        while (index < content.length()) {
            int open = content.indexOf("<Param", index);
            int closeFull = content.indexOf("</Param>", index);
            int closeSelf = content.indexOf("/>", index); // self-closing <Param ... />
            
            // Check for self-closing first if we are at depth 0 (the target tag)
            if (depth == 0) {
                // Find the first ">" or "/>"
                int tagEnd = content.indexOf(">", startIndex);
                if (tagEnd != -1 && content.charAt(tagEnd - 1) == '/') {
                    return tagEnd + 1; // It was self-closing: <Param ... />
                }
                depth = 1;
                index = tagEnd + 1;
                continue;
            }
            
            // Find next interesting token
            int nextOpen = (open == -1) ? Integer.MAX_VALUE : open;
            int nextCloseFull = (closeFull == -1) ? Integer.MAX_VALUE : closeFull;
            
            if (nextOpen == Integer.MAX_VALUE && nextCloseFull == Integer.MAX_VALUE) {
                break;
            }
            
            if (nextOpen < nextCloseFull) {
                // Check if this open tag is self-closing
                int tagEnd = content.indexOf(">", nextOpen);
                if (tagEnd != -1 && content.charAt(tagEnd - 1) == '/') {
                    // It's self closing, depth doesn't increase
                    index = tagEnd + 1;
                } else {
                    depth++;
                    index = nextOpen + 6; // skip "<Param"
                }
            } else {
                // Closing tag </Param>
                depth--;
                index = nextCloseFull + 8; // skip "</Param>"
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }
    
    private void updateDisplay() {
        if (currentRawText == null || currentRawText.isEmpty()) return;
        
        if (formatCheckBox.isSelected()) {
            textArea.setText(formatXml(currentRawText));
        } else {
            textArea.setText(currentRawText);
        }
        textArea.setCaretPosition(0);
    }
    
    private String formatXml(String input) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            // Wrap in root if needed to be valid XML for transformer
            // But Param fragment is valid XML usually
            
            StreamResult result = new StreamResult(new StringWriter());
            StreamSource source = new StreamSource(new StringReader(input));
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (Exception e) {
            return input; // Fallback to raw if formatting fails
        }
    }
}
