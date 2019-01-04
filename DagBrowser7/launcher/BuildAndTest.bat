rem  Create new 7zip archive after deleting old one.
del test.7z
"C:\Program Files (x86)\7-Zip\7z" a -t7z -r test.7z setup.exe

copy /b 7zSD.sfx+config.txt+test.7z test.exe

