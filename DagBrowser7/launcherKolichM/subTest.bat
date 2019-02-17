echo This is subTest.bat outputting this: %1, %2, %3
echo About to run java app.
pause Before java.
rem jre1.8.0_191\bin\java.exe -jar Infogora.jar allClasses.Infogora
jre1.8.0_191\bin\java.exe -cp Infogora.jar allClasses.InfogoraStarter
pause After java.
