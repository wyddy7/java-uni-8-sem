package ru.uni.lab.ui;

import ru.uni.lab.model.TmRecord;
import ru.uni.lab.service.DataService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {

    private ParameterListPanel parameterListPanel;
    private ValueTablePanel valueTablePanel;
    private ChartPanel chartPanel;
    private RawXmlPanel rawXmlPanel;
    
    private LogPanel logPanel;
    private AnalyticsPanel analyticsPanel;
    
    private JMenuItem statsItem;
    private JLabel statusLabel;
    private JLabel proofLabel;

    private JSplitPane rootSplitPane;
    private JSplitPane mainSplitPane;
    private JTabbedPane bottomTabbedPane;
    private JTabbedPane centerTabbedPane;
    
    private int lastDividerLocation = -1;

    public MainFrame() {
        setTitle("Анализатор телеметрических данных");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Initialize Components
        parameterListPanel = new ParameterListPanel();
        valueTablePanel = new ValueTablePanel();
        chartPanel = new ChartPanel();
        rawXmlPanel = new RawXmlPanel();
        
        logPanel = new LogPanel();
        analyticsPanel = new AnalyticsPanel();

        // 1. Bottom Panel (Tabs: Log, Analytics)
        bottomTabbedPane = new JTabbedPane();
        bottomTabbedPane.addTab("Логи", logPanel);
        bottomTabbedPane.addTab("Аналитика", analyticsPanel);

        // 2. Center Panel (Tabs: Table, Chart, XML)
        centerTabbedPane = new JTabbedPane();
        centerTabbedPane.addTab("Таблица значений", valueTablePanel);
        centerTabbedPane.addTab("График", chartPanel);
        centerTabbedPane.addTab("Исходный XML", rawXmlPanel);

        // 3. Layout: Main Split (Left: Params, Right: Center)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, parameterListPanel, centerTabbedPane);
        mainSplitPane.setDividerLocation(300);
        mainSplitPane.setResizeWeight(0.2);

        // 4. Root Split (Top: Main, Bottom: Logs)
        // DEFAULT IS HORIZONTAL (3 columns)
        rootSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainSplitPane, bottomTabbedPane);
        rootSplitPane.setResizeWeight(0.7); // 70% width for main content

        add(rootSplitPane, BorderLayout.CENTER);

        // Menu
        createMenuBar();
        
        // Status Bar
        createStatusBar();

        // Listeners
        setupListeners();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem openItem = new JMenuItem("Загрузить данные...");
        openItem.addActionListener(e -> showOpenDialog());
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // View Menu
        JMenu viewMenu = new JMenu("Вид");
        statsItem = new JMenuItem("Статистика (окно)");
        statsItem.setEnabled(false);
        statsItem.addActionListener(e -> showStatistics());
        viewMenu.add(statsItem);
        viewMenu.addSeparator();

        // Toggle Panels
        JCheckBoxMenuItem toggleParamsItem = new JCheckBoxMenuItem("Список параметров", true);
        toggleParamsItem.addActionListener(e -> togglePanel(parameterListPanel, mainSplitPane, toggleParamsItem.isSelected()));
        viewMenu.add(toggleParamsItem);

        JCheckBoxMenuItem toggleLogItem = new JCheckBoxMenuItem("Нижняя панель (Лог/Аналитика)", true);
        toggleLogItem.addActionListener(e -> togglePanel(bottomTabbedPane, rootSplitPane, toggleLogItem.isSelected()));
        viewMenu.add(toggleLogItem);
        
        // Layout Orientation
        JMenu layoutMenu = new JMenu("Расположение");
        ButtonGroup layoutGroup = new ButtonGroup();
        
        JRadioButtonMenuItem horizItem = new JRadioButtonMenuItem("Стандартное (Лог снизу)", false);
        horizItem.addActionListener(e -> setOrientation(JSplitPane.VERTICAL_SPLIT));
        layoutGroup.add(horizItem);
        layoutMenu.add(horizItem);
        
        // Default Vertical
        JRadioButtonMenuItem vertItem = new JRadioButtonMenuItem("Вертикальное (3 колонки)", true);
        vertItem.addActionListener(e -> setOrientation(JSplitPane.HORIZONTAL_SPLIT));
        layoutGroup.add(vertItem);
        layoutMenu.add(vertItem);
        
        viewMenu.addSeparator();
        viewMenu.add(layoutMenu);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }

    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel("Готов к работе");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        // RAM Icon / Text
        proofLabel = new JLabel("RAM: -- | Файлы: --");
        proofLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        proofLabel.setForeground(Color.DARK_GRAY);
        proofLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        statusPanel.add(proofLabel, BorderLayout.EAST);
        
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupListeners() {
        parameterListPanel.addSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Integer paramId = parameterListPanel.getSelectedParameterId();
                if (paramId != null) {
                    long start = System.nanoTime();
                    List<TmRecord> records = DataService.getInstance().getRecordsForParameter(paramId);
                    long duration = (System.nanoTime() - start) / 1000;
                    
                    valueTablePanel.updateData(paramId);
                    
                    // MANAGE CHART TAB VISIBILITY
                    int chartIndex = centerTabbedPane.indexOfComponent(chartPanel);
                    if (records.size() < 2) {
                        // Not enough data for a chart -> hide tab
                        if (chartIndex != -1) {
                            centerTabbedPane.remove(chartIndex);
                        }
                    } else {
                        // Enough data -> show tab
                        if (chartIndex == -1) {
                            // Insert at index 1 (between Table and XML) if possible
                            int count = centerTabbedPane.getTabCount();
                            int index = Math.min(count, 1);
                            centerTabbedPane.insertTab("График", null, chartPanel, null, index);
                        }
                        chartPanel.setRecords(records);
                        
                        // Chart context
                        String paramName = DataService.getInstance().getUniqueParameters().getOrDefault(paramId, "Параметр " + paramId);
                        String dim = "";
                        for (TmRecord r : records) {
                            if (r.getDimension() != null && !r.getDimension().isBlank()) {
                                dim = r.getDimension();
                                break;
                            }
                        }
                        String yLabel = dim.isBlank() ? "Значение" : ("Значение (" + dim + ")");
                        chartPanel.setContext("График: " + paramName + " (ID: " + paramId + ")", yLabel);
                    }
                    
                    rawXmlPanel.showParameterXml(paramId);
                    
                    DataService.getInstance().logEvent(String.format("User selected param %d. Fetched %d records from RAM in %d µs.", paramId, records.size(), duration));
                } else {
                    valueTablePanel.clear();
                    // Clear chart if visible, but usually no selection means no action needed
                    if (centerTabbedPane.indexOfComponent(chartPanel) != -1) {
                        chartPanel.clear();
                        chartPanel.setContext("График изменения значений", "Значение");
                    }
                }
            }
        });
    }

    private void togglePanel(JComponent panel, JSplitPane splitPane, boolean show) {
        panel.setVisible(show);
        splitPane.setDividerSize(show ? 10 : 0);
        
        if (show) {
            splitPane.resetToPreferredSizes();
        }
        
        splitPane.revalidate();
        splitPane.repaint();
    }
    
    private void setOrientation(int orientation) {
        rootSplitPane.setOrientation(orientation);
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            rootSplitPane.setDividerLocation(0.7); // 70% width for main content
        } else {
            rootSplitPane.setDividerLocation(550); // Height
        }
    }

    private void showOpenDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Выберите файлы (.knp, .xml, dimens.ion)");
        
        File inputDir = new File("input");
        if (inputDir.exists()) chooser.setCurrentDirectory(inputDir);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            File knp = null, xml = null, dim = null;
            
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".knp")) knp = f;
                else if (name.endsWith(".xml")) xml = f;
                else if (name.endsWith("dimens.ion")) dim = f;
            }
            
            if (knp == null || xml == null || dim == null) {
                JOptionPane.showMessageDialog(this, 
                    "Необходимо выбрать 3 файла:\n- .knp\n- .xml\n- dimens.ion\n\nВы выбрали: " + files.length, 
                    "Ошибка выбора файлов", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            loadFiles(knp, xml, dim);
        }
    }

    private void loadFiles(File knp, File xml, File dim) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText("Загрузка данных...");
        
        DataService.getInstance().logEvent("Initiating user-requested file load...");
        
        FileLoaderWorker worker = new FileLoaderWorker(knp, xml, dim, 
            () -> {
                // On success
                setCursor(Cursor.getDefaultCursor());
                parameterListPanel.updateParameters();
                statsItem.setEnabled(true);
                statusLabel.setText("Данные загружены.");
                
                // Update panels
                logPanel.updateContent();
                analyticsPanel.updateContent();
                rawXmlPanel.setXmlFile(xml);
                
                long ram = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                proofLabel.setText("RAM: " + ram + "MB | Файлы: ЗАКРЫТЫ");
                proofLabel.setForeground(new Color(0, 100, 0));
                
                JOptionPane.showMessageDialog(this, "Данные успешно загружены!", "Успех", JOptionPane.INFORMATION_MESSAGE);
            },
            (ex) -> {
                // On error
                setCursor(Cursor.getDefaultCursor());
                statusLabel.setText("Ошибка загрузки.");
                DataService.getInstance().logEvent("CRITICAL ERROR during load: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        );
        worker.execute();
    }

    private void showStatistics() {
        new StatisticsDialog(this).setVisible(true);
    }
}
