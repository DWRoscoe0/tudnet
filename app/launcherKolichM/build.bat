del test.7z
del test.exe
rem  -mx0 for quick no compression archiving (24s), -mx9 for max compression (~2m).
rem 7zip\7z a -t7z -r -mx9 test.7z subTest.bat jre1.8.0_191 TUDNet.jar
7zip\7z a -t7z -r -mx0 test.7z subTest.bat jre1.8.0_191 TUDNet.jar
copy /b sfx\7zSD.sfx + config.txt + test.7z TUDNet.exe
