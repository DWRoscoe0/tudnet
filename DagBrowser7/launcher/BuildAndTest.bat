rem  Delete the old jar file.
del Infogora.jar

rem  Create new jar file Infogora.jar
jar cvfm Infogora.jar Manifest.txt -C ../bin allClasses

rem  Run the executabe jar file.
Infogora.jar
