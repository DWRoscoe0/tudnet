echo This is subTest.bat outputting this: %1, %2, %3
echo About to run java app.
pause Before java.
rem jre1.8.0_191\bin\java.exe -jar Infogora.jar allClasses.Infogora
rem jre1.8.0_191\bin\java.exe -cp Infogora.jar allClasses.InfogoraStarter
rem jre1.8.0_191\bin\java.exe -cp Infogora.jar allClasses.Infogora -userDir \"%1\" -tempDir \"%2\"
jre1.8.0_191\bin\java.exe -cp Infogora.jar allClasses.Infogora -userDir "%1" -tempDir "%2"
pause After java.
