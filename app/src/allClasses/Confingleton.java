package allClasses;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static allClasses.AppLog.theAppLog;


public class Confingleton

{
    /* This class manages a text file containing 
      a single configuration value as a string.
      The name is the name of the file minus the ".txt" extension.
      It is assumed to be in the app's standard directory
      which is known to the Config class.
      */

    private static File makeFile( String keyString )
      {
        return FileOps.makeRelativeToAppFolderFile( keyString + ".txt" );
        }


    public static String getValueString( String keyString )
      /* This method returns the string stored in the file.
        If the file doesn't exist, or there are any read errors,
        then null is returned.
       */
      {
        String valueString= null;
        File theFile= makeFile(keyString);
        try {
          FileReader theFileReader= new FileReader(theFile);
          char[] chars= new char[(int) theFile.length()];
          theFileReader.read(chars);
          Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theFileReader);
          valueString= new String(chars);
        } catch (Exception theException) {
            // Ignore exception caused by no file or bad data.
            // In this cases, null will be returned.
            }
        theAppLog.info("Confingleton.getValueString(..) keyString="
            +keyString+" valueString="+valueString);
        return valueString;
        }
    
    public static void putValueV( String keyString, String valueString )
      /* This method stores valueString in the file.

        ///enh Make this an atomic operation. 
        */
      {
        theAppLog.info("Confingleton.putValueV(..) keyString="
            +keyString+" valueString="+valueString);

        File theFile= makeFile(keyString);
        
        try ( FileWriter theFileWriter = new FileWriter(theFile ) ) 
          { // Write [new] value string to file.
              theFileWriter.write(valueString);
          } catch (IOException e) {
              System.err.println(e);
          }
        }

    public static int getValueI( String keyString )
      /* This method returns the integer stored as a string in the file.
        If the file doesn't exist, or there are any read errors,
        or the value is not a valid number, then-1 is returned.
        Otherwise the numerical value is returned.
       */
      {
        int valueI= -1;  // Default value indicating error.
        String valueString= getValueString( keyString );
        if ( valueString != null )
          try { 
              valueI= Integer.parseInt(valueString); 
            } catch ( NumberFormatException e ) { 
              // Ignore, default value will be returned. 
            }
        return valueI;
        }
    
    public static void putValueV( String keyString, int valueI )
      /* This method stores the integer valueI in the file.
        */
      {
        putValueV(keyString, ""+valueI); // Convert and use string method.
        }
  
    }
