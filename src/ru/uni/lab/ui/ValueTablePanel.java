package ru.uni.lab.ui;

import ru.uni.lab.model.ParameterInfo;
import ru.uni.lab.model.TmRecord;
import ru.uni.lab.model.TmLong;
import ru.uni.lab.model.TmCode;
import ru.uni.lab.service.DataService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ValueTablePanel extends JPanel {

    private JTable table;
    private TmTableModel tableModel;

    public ValueTablePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Значения"));

        tableModel = new TmTableModel();
        table = new JTable(tableModel);
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(120); // Time
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // Value
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // Dimension
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Attribute

        // Center align Time
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        
        // Status renderer with colors
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void updateData(int parameterId) {
        List<TmRecord> records = DataService.getInstance().getRecordsForParameter(parameterId);
        ParameterInfo info = DataService.getInstance().getParameterInfo(parameterId);
        tableModel.setRecords(records, info);
    }
    
    public void clear() {
        tableModel.setRecords(Collections.emptyList(), null);
    }
    
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = String.valueOf(value);
            
            if (!isSelected) {
                if (status.contains("Не норма") || status.contains("Вне диапазона") || status.contains("Зашкаливание")) {
                    c.setForeground(Color.RED);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if (status.contains("Внимание")) {
                    c.setForeground(new Color(200, 100, 0)); // Orange-ish
                } else if ("Норма".equals(status)) {
                    c.setForeground(new Color(0, 128, 0)); // Green
                } else {
                    c.setForeground(Color.BLACK);
                }
            } else {
                c.setForeground(table.getSelectionForeground());
            }
            return c;
        }
    }

    private static class TmTableModel extends AbstractTableModel {
        private final String[] columns = {"Время", "Значение", "Размерность", "Статус"};
        private List<TmRecord> records = Collections.emptyList();
        private ParameterInfo paramInfo;
        
        public void setRecords(List<TmRecord> records, ParameterInfo info) {
            this.records = records;
            this.paramInfo = info;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return records.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TmRecord record = records.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return formatTime(record.getTime());
                case 1:
                    return formatValue(record);
                case 2:
                    return record.getDimension();
                case 3:
                    return DataService.getInstance().getAttributeText(record.getAttribute());
                default:
                    return null;
            }
        }
        
        private String formatValue(TmRecord record) {
            // Check for text mapping if available
            if (paramInfo != null && !paramInfo.getValues().isEmpty()) {
                long val = 0;
                boolean isMappable = false;
                
                if (record instanceof TmLong) {
                    val = ((TmLong) record).getValue();
                    isMappable = true;
                } else if (record instanceof TmCode) {
                    val = ((TmCode) record).getValue();
                    isMappable = true;
                }
                
                if (isMappable) {
                    String text = paramInfo.getValueText(val);
                    if (text != null) {
                        return text + " (" + record.getValueAsString() + ")";
                    }
                }
            }
            return record.getValueAsString();
        }
        
        private String formatTime(long msFromStartOfDay) {
            // Normalize to 24 hours
            long msPerDay = 24 * 3600 * 1000;
            long normalized = msFromStartOfDay % msPerDay;
            
            long hours = normalized / 3600000;
            long minutes = (normalized % 3600000) / 60000;
            long seconds = (normalized % 60000) / 1000;
            long ms = normalized % 1000;
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
        }
    }
}
