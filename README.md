cd "D:\D\Papka\uni\info.indeed\8 semestr\korolkova"

rem Компиляция всех исходников в папку out
javac -encoding UTF-8 -d out ^
  src\ru\uni\lab\app\Main.java ^
  src\ru\uni\lab\model\*.java ^
  src\ru\uni\lab\service\*.java ^
  src\ru\uni\lab\ui\*.java

javac -encoding UTF-8 -d out src\ru\uni\lab\app\Main.java src\ru\uni\lab\model\*.java src\ru\uni\lab\service\*.java src\ru\uni\lab\ui\*.java                                                                                 

rem Запуск приложения
java -cp out ru.uni.lab.app.Main