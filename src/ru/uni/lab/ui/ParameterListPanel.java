package ru.uni.lab.ui;

import ru.uni.lab.model.ParameterInfo;
import ru.uni.lab.model.TmRecord;
import ru.uni.lab.service.DataService;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Map;
import java.util.Vector;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

public class ParameterListPanel extends JPanel {

    private JList<ListItem> list;
    private DefaultListModel<ListItem> listModel;

    public ParameterListPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Параметры"));

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom renderer for tooltips
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ListItem) {
                    ListItem item = (ListItem) value;
                    setText(item.toString());
                    
                    StringBuilder tooltip = new StringBuilder();
                    tooltip.append("<html><b>").append(item.fullName).append("</b>");
                    if (item.description != null && !item.description.isEmpty()) {
                        tooltip.append("<br>").append(item.description);
                    }
                    tooltip.append("</html>");
                    setToolTipText(tooltip.toString());
                }
                return c;
            }
        });

        add(new JScrollPane(list), BorderLayout.CENTER);
    }

//    public void updateParameters() {
//        listModel.clear();
//        Map<Integer, String> params = DataService.getInstance().getUniqueParameters();
//
//        // Сортируем по имени параметра для сортировки по алфавиту
//        List<Map.Entry<Integer, String>> sortedEntries = new ArrayList<>(params.entrySet());
//        sortedEntries.sort(Map.Entry.comparingByValue());
//
//        for (Map.Entry<Integer, String> entry : sortedEntries) {
//            ParameterInfo info = DataService.getInstance().getParameterInfo(entry.getKey());
//            String desc = (info != null) ? info.getDescription() : null;
//            listModel.addElement(new ListItem(entry.getKey(), entry.getValue(), desc));
//        }
//    }
    public void updateParameters() {
        listModel.clear();
        Map<Integer, String> params = DataService.getInstance().getUniqueParameters();
//        Сортируем по имени параметра для сортировки по алфавиту
        List<Map.Entry<Integer, String>> sortedEntries = new ArrayList<>(params.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue());
        for (Map.Entry<Integer, String> entry : sortedEntries) {
            ParameterInfo info = DataService.getInstance().getParameterInfo(entry.getKey());
            String desc = (info != null) ? info.getDescription() : null;
            listModel.addElement(new ListItem(entry.getKey(), entry.getValue(), desc));
        }
    }

    public void addSelectionListener(ListSelectionListener listener) {
        list.addListSelectionListener(listener);
    }

    public Integer getSelectedParameterId() {
        ListItem selected = list.getSelectedValue();
        return selected != null ? selected.id : null;
    }

    private static class ListItem {
        int id;
        String fullName;
        String description;

        ListItem(int id, String fullName, String description) {
            this.id = id;
            this.fullName = fullName;
            this.description = description;
        }

        @Override
        public String toString() {
            return String.format("%s: %d", fullName, id);
        }
    }
}
