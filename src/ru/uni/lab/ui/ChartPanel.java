package ru.uni.lab.ui;

import ru.uni.lab.model.TmDouble;
import ru.uni.lab.model.TmLong;
import ru.uni.lab.model.TmRecord;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class ChartPanel extends JPanel {

    private List<TmRecord> records;
    private static final int PADDING = 50;
    private static final int LABEL_PADDING = 25;
    private String chartTitle = "График изменения значений";
    private String yAxisLabel = "Значение";
    private String xAxisLabel = "Время (ЧЧ:ММ:СС)";

    public ChartPanel() {
        this.records = new ArrayList<>();
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder(chartTitle));
        setToolTipText("<html>" +
                "График показывает изменение значения <b>выбранного параметра</b> во времени.<br>" +
                "Ось X — время (ЧЧ:ММ:СС), ось Y — числовое значение параметра.<br>" +
                "Используется только для параметров с типами Long/Double." +
                "</html>");
    }

    public void setContext(String title, String yAxisLabel) {
        if (title != null && !title.isBlank()) {
            this.chartTitle = title;
        } else {
            this.chartTitle = "График изменения значений";
        }
        if (yAxisLabel != null && !yAxisLabel.isBlank()) {
            this.yAxisLabel = yAxisLabel;
        } else {
            this.yAxisLabel = "Значение";
        }
        setBorder(BorderFactory.createTitledBorder(this.chartTitle));
        repaint();
    }

    public void setRecords(List<TmRecord> records) {
        // Filter only numeric records
        this.records = new ArrayList<>();
        if (records != null) {
            for (TmRecord r : records) {
                if (r instanceof TmLong || r instanceof TmDouble) {
                    this.records.add(r);
                }
            }
        }
        repaint();
    }
    
    public void clear() {
        this.records.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (records == null || records.isEmpty()) {
            g2.setColor(Color.GRAY);
            String msg = "Нет числовых данных для отображения графика";
            FontMetrics fm = g2.getFontMetrics();
            int msgWidth = fm.stringWidth(msg);
            g2.drawString(msg, (width - msgWidth) / 2, height / 2);
            return;
        }

        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        // Calculate Min/Max
        for (TmRecord r : records) {
            double val = extractValue(r);
            long t = r.getTime();
            if (val < minVal) minVal = val;
            if (val > maxVal) maxVal = val;
            if (t < minTime) minTime = t;
            if (t > maxTime) maxTime = t;
        }
        
        // Add minimal padding to Y range to avoid flat lines on border
        if (maxVal == minVal) {
            maxVal += 1.0;
            minVal -= 1.0;
        }
        
        // Coordinates for drawing area
        int x0 = PADDING + LABEL_PADDING; // Left margin
        int y0 = height - PADDING - LABEL_PADDING; // Bottom margin (Y=0 visually)
        int xMax = width - PADDING; // Right margin
        int yMax = PADDING; // Top margin

        // Draw Axes
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x0, y0, xMax, y0); // X Axis
        g2.drawLine(x0, y0, x0, yMax); // Y Axis

        // Grid and Labels
        drawGrid(g2, x0, y0, xMax, yMax, minVal, maxVal, minTime, maxTime);

        // Axis captions
        drawAxisCaptions(g2, x0, y0, xMax, yMax);

        // Draw Data Line
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(2f));

        Path2D.Double path = new Path2D.Double();
        boolean first = true;

        for (TmRecord r : records) {
            double val = extractValue(r);
            long t = r.getTime();

            double x = x0 + (double)(t - minTime) / (maxTime - minTime) * (xMax - x0);
            double y = y0 - (val - minVal) / (maxVal - minVal) * (y0 - yMax);

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
            
            // Draw point
            g2.fillOval((int)x - 2, (int)y - 2, 4, 4);
        }
        g2.draw(path);
    }

    private void drawAxisCaptions(Graphics2D g2, int x0, int y0, int xMax, int yMax) {
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(Color.DARK_GRAY);

        // X axis caption (centered under axis)
        FontMetrics fm = g2.getFontMetrics();
        int xLabelWidth = fm.stringWidth(xAxisLabel);
        int xCenter = x0 + (xMax - x0) / 2;
        int xLabelX = xCenter - xLabelWidth / 2;
        int xLabelY = Math.min(getHeight() - 8, y0 + 35);
        g2.drawString(xAxisLabel, xLabelX, xLabelY);

        // Y axis caption (left side, top)
        g2.drawString(yAxisLabel, Math.max(8, x0 - PADDING - LABEL_PADDING + 5), Math.max(20, yMax - 10));
    }
    
    private void drawGrid(Graphics2D g2, int x0, int y0, int xMax, int yMax, 
                          double minVal, double maxVal, long minTime, long maxTime) {
        
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        Stroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
        Stroke solid = new BasicStroke(1f);

        // Y Axis Grid (5 steps)
        int stepsY = 5;
        for (int i = 0; i <= stepsY; i++) {
            double fraction = (double)i / stepsY;
            int y = y0 - (int)(fraction * (y0 - yMax));
            
            // Grid line
            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(dashed);
            g2.drawLine(x0, y, xMax, y);
            
            // Label
            double val = minVal + fraction * (maxVal - minVal);
            String label = String.format("%.2f", val);
            g2.setColor(Color.BLACK);
            // Right align label
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2.drawString(label, x0 - labelWidth - 5, y + 5);
        }
        
        // X Axis Grid (5 steps)
        int stepsX = 5;
        for (int i = 0; i <= stepsX; i++) {
            double fraction = (double)i / stepsX;
            int x = x0 + (int)(fraction * (xMax - x0));
            
            // Grid line
            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(dashed);
            g2.drawLine(x, y0, x, yMax);
            
            // Label
            long t = minTime + (long)(fraction * (maxTime - minTime));
            String label = formatTime(t);
            g2.setColor(Color.BLACK);
            g2.drawString(label, x - 15, y0 + 15);
        }
        
        // Restore stroke
        g2.setStroke(solid);
    }

    private double extractValue(TmRecord r) {
        if (r instanceof TmLong) return ((TmLong) r).getValue();
        if (r instanceof TmDouble) return ((TmDouble) r).getValue();
        return 0.0;
    }
    
    private String formatTime(long msFromStartOfDay) {
        long hours = msFromStartOfDay / 3600000;
        long minutes = (msFromStartOfDay % 3600000) / 60000;
        long seconds = (msFromStartOfDay % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
