del test.7z
del test.exe
..\7zr a test.7z ..\7zr.exe -mx -mf=BCJ2
copy /b ..\7zSD.sfx + config.txt + test.7z test.exe
pause
test.exe
