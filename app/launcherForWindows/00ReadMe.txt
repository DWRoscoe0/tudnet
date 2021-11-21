The directory that contains this file also contains 
files used to build TUDNet.exe, which is 
the TUDNet app in the form of a Windows executable file.
The files here are based on the files at: 
  https://github.com/markkolich/7zip-sfx-java

TUDNet.exe is a self-extracting .7z archive file.  When run:
* It extracts to a temporary directory:
  * the files and folders of a Java Runtime Environment (JRE) and  
  * TUDNet.jar which is the app as a Java program stored in 
    an executable .jar file .
* Next it runs the app by using the JRE to run TUDNet.jar .
* After TUDNet.jar and the JRE terminate, it deletes the temporary directory.

TUDNet.exe is the concatenation of the following 3 files:
* sfx\7zSD.sfx : The native executable part that does extracting and launching.
* config.txt   : The file that contains parameters for sfx\7zSD.sfx.
* TUDNet.7z    : A temporary .7z archive file containing files to be extracted.

For more information see build.bat which is the script that builds TUDNet.exe .

