del test.7z
del test.exe
rem ..\7zr a test.7z ..\7zr.exe -mx -mf=BCJ2
..\7zr a test.7z ..\7zr.exe
copy /b ..\7zSD.sfx + config.txt + test.7z test.exe

