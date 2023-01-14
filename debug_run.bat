
set JAVA_HOME="D:\JetBrains\IntelliJ IDEA Community Edition 2021.3.3\jbr\"
set PATH=%JAVA_HOME%bin
%JAVA_HOME%bin\javaw  -Dfile.encoding=UTF-8 -classpath "target\classes;libs/*;MIJI/joni/*" com.knziha.plod.PlainDict.PlainDictionaryPcJFX
pause