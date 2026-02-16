import javax.swing.*;
import java.awt.*;

public class NumberListFrame extends JFrame {
    
    public NumberListFrame() {
        // Настройка окна
        setTitle("Список номеров");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null);
        
        // Создание списка номеров
        String[] numbers = new String[100];
        for (int i = 0; i < 100; i++) {
            numbers[i] = "Номер " + (i + 1);
        }
        
        JList<String> numberList = new JList<>(numbers);
        numberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        numberList.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Создание скроллбара для списка
        JScrollPane scrollPane = new JScrollPane(numberList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Список номеров"));
        
        // Добавление скроллпанели в окно
        add(scrollPane, BorderLayout.CENTER);
        
        // Отображение окна
        setVisible(true);
    }
    
    public static void main(String[] args) {
        // Запуск в потоке событий Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new NumberListFrame();
            }
        });
    }
}
