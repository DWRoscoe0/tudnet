package allClasses;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class AppSettings {
  
  /* This class calculates and provides values for app settings.
    These settings are everything not provided by SystemSettings.
    
    Initialization is in 2 parts:
    * Static initialization when this class is loaded.
    * Initialization done when the method initializeV(..) is called.
      This method uses setter-injection.
    */

  // Load-time static initialization.
  
  public static File userAppFolderFile= new File( 
      new File( SystemSettings.homeFolderPathString ), Config.appString );
  
  /* Create app folder if it doesn't exist.
    The error handling is unconventional because this must be done
    before theAppLog logging facility is running.
    Many things, such as Logging, fail without this directory.
    */
  static {
    String errorString= // Try creating app folder if it doesn't exist.
        makeDirectoryAndAncestorsString(userAppFolderFile);
    if (errorString != null) { // If there was an error, display it and exit.
      System.out.println(errorString);
      System.exit(1);
      }
    }
  
  public static void makeDirectoryAndAncestorsWithLoggingV(File directoryFile)
    /* This method creates a directory and its ancestors if needed,
      but if any errors happen, it logs them.
      */
    {
      String errorString= // Try creating desired folder if it doesn't exist.
          makeDirectoryAndAncestorsString(directoryFile);
      if (errorString != null) { // If there was an error, log it.
        theAppLog.error(
            "AppSettings.makeDirectoryAndAncestorsWithLoggingV(.) " + errorString);
        }
      }
  
  private static String makeDirectoryAndAncestorsString(File directoryFile)
    /* This method tries to create the directory whose name is directoryFile,
      and any ancestor directories that do not exist.
      It returns null if successful, an error message string if not.
      */
    {
      String errorString= null; // Assume no errors.
      makeDir: { 
        try {
          if (directoryFile.exists())
            break makeDir; // Do nothing if directory already exists.
          if (! directoryFile.mkdirs())
            errorString= 
              "AppSettings loader: "+directoryFile+" mkdirs() failed.";
        } catch (Exception theException){
          errorString= "AppSettings.makeDirectoryAndAncestorsString(.): "
              + directoryFile + theException;
        }
      } // makeDir:
      return errorString;
      }
  
  // Methods that calculate and return values which depend on app settings.

  public static File makeRelativeToAppFolderFile( 
      String fileRelativePathString )
    /* This method creates a File name object by appending fileRelativePathString
      to the path name for the standard app folder,
      This method works by delegating to a method that takes 
      an array containing fileRelativePathString as its only element. 
      See the delegate method for details. 
      */
    {
      return makePathRelativeToAppFolderFile( fileRelativePathString );
      }

  public static File makePathRelativeToAppFolderFile(String... fileRelativePathStrings )
    /* This method creates a File name path object by adding,
      to the path name for the standard app folder,
      the fileRelativePathStrings, which is an array of Strings 
      representing path elements to be appended..  
      All but the last element represents a directory,
      The last element could be either a directory or a file,
      fileRelativePathStrings may contain a single name element, such as 
        {"TCPCopierStaging" + File.separator + "Infogora.exe"} 
      or multiple elements, such as 
        {"TCPCopierStaging", "Infogora.exe"} 
      Note that the above 2 examples represent the same path
      expressed in 2 different ways.
      */
    {
      File resultFile= AppSettings.userAppFolderFile; // Start with bare app folder

      for // For all the elements of the relative path element array 
        (String elementString: fileRelativePathStrings) {
        resultFile= new File(resultFile, elementString); // add next element.
        theAppLog.debug("AppSettings.makePathRelativeToAppFolderFile(.) " 
            + elementString + " " + resultFile);
        }

      return resultFile; // Return the accumulated result.
      }
  
  public static void initializeV(
      Class<?> entryPointClass, CommandArgs theCommandArgs)
    /* This method does post-load initialization of this class.
      It also does some logging of settings.

      This method can not be called from a static block because
      it has a CommandArgs dependency which comes from the main(..) entry point.

      ///enh In theory it could be made not-static with constructor injection. 
      */
    {
      theAppLog.info("AppSettings.initializeV(..) argStrings=" + NL + "  "
          +Arrays.toString(theCommandArgs.args()));
      theAppLog.info("AppSettings.initializeV(..) entryPointClass="+
          entryPointClass.getCanonicalName());
      
      setInitiatorV(entryPointClass, theCommandArgs);
      
      makeDirectoryAndAncestorsWithLoggingV( // Make another directory we'll need.
          makePathRelativeToAppFolderFile( "Peers" ) );
      
      SystemSettings.logSystemPropertiesV(theAppLog);
      }
    
  
  /* The initiator file is the first file which is both
    * executed, and is considered to be part of the application.
  
    The initiator file can be either
    * an .exe file, a self-extracting file, containing the app's 
      7ZipSFXModule-based launcher code, the app's java code, plus 
      its JRE (Java Runtie Environent), or
    * a .jar file, containing the app's java code, 
      but needing a separate JRE to run, or
    * a .class file, the file containing the app's entry point and
      one file in a collection of class files making up the app,
      also needing a JRE to run.
  
    */
  
    public static String initiatorExtensionString= null;
      // Either ".exe", ".jar", or ".class".
    public static String initiatorNameString= null;
    public static File initiatorFile= null;
      /* This contains the full path to one of the following:
        * the app's .exe initiator file,
        * the app's .jar initiator file,
        * the app's .class initiator file containing the 
          app's .class file containing the main(..) method entry point.
          .class initiator happens during development 
          and when running from Eclipse.
        * ERROR.ERR, indicating an error during calculations.
        */

    static void setInitiatorV(
      Class<?> entryPointClass, CommandArgs theCommandArgs) 
      /* This messy method, called during initialization,
        calculates the value of each of the app initiator variables, 
        initiatorFile and initiatorExtensionString.  */
      {
          String pathString; File iFile;
        toStoreAndExit: { 
          pathString= theCommandArgs.switchValue("-userDir");
          if (pathString != null) { // -userDir switch means exe launcher used.
            iFile= 
              new File(pathString + File.separator + Config.appString + ".exe");
            break toStoreAndExit;
            }
          try { // Try converting entry point to URI and File.
            iFile= new File( 
                  entryPointClass.getProtectionDomain().
                  getCodeSource().getLocation().toURI());
            } catch (URISyntaxException e1) {
              theAppLog.exception("AppSettings.setInitiatorV()", e1);
              iFile= new File("ERROR.ERR"); 
              break toStoreAndExit;
            }
          if (iFile.getName().endsWith(".jar")) break toStoreAndExit; 
          iFile= new File( // Append entry-point class file name.
              iFile, entryPointClass.getCanonicalName() + ".class");
        } // toStoreAndExit:
          AppSettings.initiatorFile= iFile; // Save File.
          initiatorNameString= iFile.getName();
          int dotIndexI= initiatorNameString.lastIndexOf('.');
          AppSettings.initiatorExtensionString= // Save file extension 
            (dotIndexI < 0) ? "ERR" : initiatorNameString.substring(dotIndexI); 
          theAppLog.info("AppSettings.setInitiatorV()"
            + ", initiatorExtensionString= " 
              + AppSettings.initiatorExtensionString
            + ", initiatorFile=" + NL + "  " + AppSettings.initiatorFile);
        }

    }
