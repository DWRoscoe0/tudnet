package allClasses;

import static allClasses.Globals.appLogger;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

public class SystemState

  /* This class is for reading, calculating,
     and logging various system state variables.
    
    ///enh Determine what type of file was the initiator of this app:
      ? class file in Eclipse (main app or starter).
      ? jar file with manifest entry point.
      ? java.exe (class or jar).
      ? 7ZipSFXModule-based launcher.
    
    ///enh Eventually these parameters could be displayed as part of tree.
      For now they are only logged.
   */
  {

    /* The initiator file is the first file which is both

      * executed, and
      * is considered to be part of the application.

      It can be either

      * a .exe file, a self-extracting file, containing the app's 
        7ZipSFXModule-based launcher code, the app's java code, plus 
        its JRE (Java Runtie Environent), or
      * a .jar file, containing the app's java code, 
        but needing a separate JRE to run, or
      * a .class file, the file containing the app's entry point and
        one file in a collection of class files making up the app,
        and needing a JRE to run.

     */
    public static String initiatorExtensionString= null;
      // Either ".exe", ".jar", or ".class".
    public static File initiatorFile= null;  //// new File("UNDEFINED");
      /* This contains the full path to
        * the app's .exe initiator file,
        * the app's .jar initiator file,
        * the app's .class initiator file containing the 
          app's .class file containing the main(..) method entry point.
          This can happen during development and when running from Eclipse.
        * ERROR.ERR, indicating an error during calculations.
        */

    public static void initializeV(
        Class<?> entryPointClass, CommandArgs theCommandArgs)
      /* This method initializes this class.
        It does some value calculations, and it does a lot of value logging.
        */
      {
        appLogger.info("SystemState.initializeV(..) argStrings=\n  "
            +Arrays.toString(theCommandArgs.args()));
        appLogger.info("SystemState.initializeV(..) entryPointClass="+
            entryPointClass.getCanonicalName());
        setInitiatorV(entryPointClass, theCommandArgs);
        
        logSystemPropertyV("java.class.path"); // class directories and JAR archives.
        logSystemPropertyV("java.home"); // Installation directory for JRE.
        logSystemPropertyV("java.version"); // JRE version number.
        logSystemPropertyV("os.arch"); // Operating system architecture
        logSystemPropertyV("os.name"); // Operating system name
        logSystemPropertyV("os.version"); // Operating system version
        logSystemPropertyV("user.dir"); // User working directory
        logSystemPropertyV("user.home"); // User home directory
        logSystemPropertyV("user.name"); // User account name
        }

    private static void setInitiatorV(
        Class<?> entryPointClass, CommandArgs theCommandArgs) 
      /* This messy method calculates the value of the app initiator variables,
        initiatorFile and initiatorExtensionString.  */
      {
          String pathString;
        toReport: { 
          pathString= theCommandArgs.switchValue("-userDir");
          if (pathString != null) { // -userDir switch means exe launcher used.
            initiatorFile= 
              new File(pathString + File.separator + Config.appString + ".exe");
            break toReport;
            }
          try { // Try converting entry point to URI and File.
              initiatorFile= new File( 
                  entryPointClass.getProtectionDomain().
                  getCodeSource().getLocation().toURI());
            } catch (URISyntaxException e1) {
              appLogger.exception("SystemState.setInitiatorV()", e1);
              initiatorFile= new File("ERROR.ERR"); 
              break toReport;
            }
          if (initiatorFile.getName().endsWith(".jar")) break toReport; 
          initiatorFile= new File( // Append entry-point class file name.
              initiatorFile, entryPointClass.getCanonicalName() + ".class");
        } // toReport:
          appLogger.info("SystemState.setInitiatorV(), "
              + "initiatorFile=\n  " + initiatorFile);
        }

    private static void logSystemPropertyV(String nameString) 
      {
        appLogger.info(
            "logSystemPropertyV(..) "+nameString+
            "="+System.getProperty(nameString)
            );
        }

    }