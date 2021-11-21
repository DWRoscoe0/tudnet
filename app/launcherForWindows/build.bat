rem This script builds TUDNet.exe .

rem Delete old temporary .7z file and final .exe file.
del TUDNet.7z
del TUDNet.exe

rem Create temporary .7z archive from the JRE and app .jar file.
rem 7zip option -mx9 is for slow maximum-compression archiving.
rem CODE 7zip\7z a -t7z -r -mx9 TUDNet.7z jre1.8.0_191 TUDNet.jar
rem 7zip option -mx0 is for fast no-compression archiving.
7zip\7z a -t7z -r -mx0 TUDNet.7z jre1.8.0_191 TUDNet.jar

rem Create final self-extracting file TUDNet.exe by concatenating 3 files.
copy /b sfx\7zSD.sfx + config.txt + TUDNet.7z TUDNet.exe

rem End of script.
