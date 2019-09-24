package allClasses;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

import static allClasses.AppLog.theAppLog;
import static allClasses.Globals.NL;


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
  
  static {
    String errorString= null;

    makeDir: { // Make directory for the user's app, with full error checking.
      try {
        if (AppSettings.userAppFolderFile.exists())
          break makeDir; // Do nothing if directory already exists.
        if (! AppSettings.userAppFolderFile.mkdirs())
          errorString= 
            "AppSettings loader: "+userAppFolderFile+" mkdirs() failed.";
        } catch (Exception e){
          errorString= 
            "AppSettings loader: "+userAppFolderFile+" mkdirs() "+e;
        } // Many things, such as Logging, fail without this directory.
      } // makeDir:
    
      if (errorString != null) { // Handle error by displaying and exiting.
        System.out.println(errorString);
        System.exit(1);
        }
    }

  
  // Methods that calculate and return values which depend on app settings.
  
  public static File makeRelativeToAppFolderFile( 
      String fileRelativePathString )
    /* This method creates a File name object from fileRelativePathString.
      fileRelativePathString is a String representing the relative path
      from the standard app folder to the file of interest.  
      fileRelativePathString may be a single element path name, 
      such as "Infogora.jar", or a multiple element path name, 
      such as "TCPCopierStaging\Infogora.jar". 
      */
    {
      return new File( AppSettings.userAppFolderFile, fileRelativePathString );
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
