package allClasses;

import static allClasses.Globals.appLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Confingleton 
  /* This class manages a text file containing 
    a single configuration value as a string.
    The name is the name of the file minus the ".txt" extension.
    It is assumed to be in the app's standard directory
    which is known to the Config class.
    */
  {

    private static File makeFile( String keyNameString )
      {
        return Config.makeRelativeToAppFolderFile( keyNameString + ".txt" );
        }

    
    public static String getValueString( String keyNameString )
      /* This method returns the string stored in the file.
        If the file doesn't exist, or there are any read errors,
        then null is returned.
       */
      {
        String valueString= null;
        File theFile= makeFile(keyNameString);
        try {
          FileReader sessionFileReader = 
            new FileReader(theFile);
          char[] chars = new char[(int) theFile.length()];
          sessionFileReader.read(chars);
          sessionFileReader.close();
          valueString = new String(chars);
        } catch (IOException e) {
            // Ignore errors.  These are treated as no file or bad data.
            // null will be returned.
            }
        appLogger.debug("Confinglton.getValueString(..) keyNameString="
            +keyNameString+" port="+valueString);
        return valueString;
        }
    
    public static void putValueV( String keyNameString, String valueString )
      /* This method stores valueString stored in the file.

        ///enh Make this an atomic operation. 
        */
      {
        appLogger.debug("Confinglton.putValueString(..) keyNameString="
            +keyNameString+" port="+valueString);

        File theFile= makeFile(keyNameString);
        
        try ( FileWriter theFileWriter = new FileWriter(theFile ) ) 
          { // Write [new] value string to file.
              theFileWriter.write(valueString);
          } catch (IOException e) {
              System.err.println(e);
          }
        }
  
    }
