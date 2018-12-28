rem  Delete the old executable jar file Infogora.jar .
del Infogora.jar

rem  Create the new jar file.
jar cvfm Infogora.jar Manifest.txt -C ../bin allClasses

rem  Run the jar file.
"C:\Program Files (x86)\Java\jre1.8.0_191\bin\java.exe" -jar Infogora.jar
