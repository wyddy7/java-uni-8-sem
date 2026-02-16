# TODO: Королькова — Java проект (телеметрия)

> Статус: ✅ ПРАКТИЧЕСКИ ЗАКРЫТ (за 1 неделю)

## Что сделано
- ✅ Написан проект на Java (навайбкожен с моделями)
- ✅ Проект сдан Корольковой
- ✅ Доказал авторство (показал структуру, сервисы, разбивку)

## Что осталось
- [ ] Отходить ~2 недели, помогая другим студентам объяснять код
- [ ] Уметь отвечать на вопросы по коду (шпаргалка ниже)

## Шпаргалка по проекту (что знать)

### Архитектура
- Пакеты: `model/`, `service/`, `ui/` (MVC-паттерн)
- Главный файл: `src/ru/uni/lab/ui/MainFrame.java`
- Точка входа: `src/ru/uni/lab/app/Main.java`

### Ключевые классы
| Класс | Что делает |
|-------|-----------|
| `DataService` | Синглтон, читает все 3 файла ОДИН раз, хранит данные в памяти |
| `KnpParser` | Парсит бинарный .knp (big-endian, TM-записи) |
| `XmlParser` | Рекурсивный обход XML для извлечения параметров |
| `DimensionParser` | Читает dimens.ion (Windows-1251 кодировка) |
| `TmRecord` | Базовый класс записи (TmLong, TmDouble, TmCode, TmPoint) |
| `MainFrame` | Главное окно (BorderLayout, сплиттеры) |
| `ChartPanel` | Графики через Java 2D API (paintComponent) |
| `ValueTablePanel` | Таблица значений (AbstractTableModel, виртуализация) |
| `ParameterListPanel` | Список параметров слева (JList + кастомный рендерер) |

### Часто задаваемые вопросы
1. **"Где чтение файлов?"** → `DataService.loadData()` — XML, dimens.ion, KNP
2. **"Как парсится XML?"** → Рекурсивный обход DOM-дерева (`parseXmlRecursive`)
3. **"Как строятся графики?"** → `ChartPanel.paintComponent()`, Java 2D, `g2.drawLine`
4. **"Как работает список параметров?"** → `JList` + `ListCellRenderer`, показывает "ID: Имя"
5. **"Как сделана таблица?"** → `AbstractTableModel`, не хранит копии — берёт из `TmRecord`
6. **"Зачем SwingWorker?"** → Фоновая загрузка, чтобы UI не зависал

### Как запустить
```bat
javac -encoding UTF-8 -d out src\ru\uni\lab\app\Main.java src\ru\uni\lab\model\*.java src\ru\uni\lab\service\*.java src\ru\uni\lab\ui\*.java
java -cp out ru.uni.lab.app.Main
```

### Реализованный функционал (20 пунктов)
Полный список: `implemented_features.txt`

## Файлы проекта
- `TZ_FINAL.md` — полное ТЗ со всеми требованиями
- `about.md` — шпаргалка "где что лежит"
- `implemented_features.txt` — список реализованного функционала
- `src/` — исходный код
- `input/` — входные данные для тестирования
