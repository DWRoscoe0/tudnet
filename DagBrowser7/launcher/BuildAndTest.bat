rem  Create the new executable jar file Infogora.jar after deleting old one.
del Infogora.jar
jar cvfm Infogora.jar Manifest.txt -C ../bin allClasses

rem  Run the new jar file using local copy of JRE.
rem  jre1.8.0_191\bin\java.exe -jar Infogora.jar

rem  Create new 7zip archive after deleting old one.
del app.7z
"C:\Program Files (x86)\7-Zip\7z" a -t7z -r app.7z jre1.8.0_191 Infogora.jar
