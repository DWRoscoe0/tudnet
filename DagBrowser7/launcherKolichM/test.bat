del test.7z
del test.exe
rem 7zip\7z a test.7z subTest.bat jre1.8.0_191 -mx
rem 7zip\7z a test.7z subTest.bat -mx
rem  -mx0 for quick no compression archiving (24s), -mx9 for max compression (~2m).
rem 7zip\7z a -t7z -r -mx9 test.7z subTest.bat jre1.8.0_191 Infogora.jar
7zip\7z a -t7z -r -mx0 test.7z subTest.bat jre1.8.0_191 Infogora.jar
copy /b sfx\7zSD.sfx + config.txt + test.7z Infogora.exe
rem pause Before execution...
Infogora.exe
rem pause After execution...
