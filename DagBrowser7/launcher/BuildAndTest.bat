rem  Delete the old executable jar file Infogora.jar .
del Infogora.jar

rem  Create the new jar file.
jar cvfm Infogora.jar Manifest.txt -C ../bin allClasses

rem  Run the jar file.
Infogora.jar
