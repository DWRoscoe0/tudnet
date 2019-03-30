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

    public static File initiatorFile= new File("UNDEFINED");
      /* This names the file that was the initiator of this app:
        It can have the following values and meanings:
        * Path ending in .exe : copy-able 7ZipSFXModule-based launcher
          which uncompresses the JRE and the jar file to a temporary directory
          and then runs the JRE java.exe on the jar file app.
        * Path ending in .jar : copy-able executable jar file. 
        * Some other path : probably the directory containing
          the class file containing the main(..) method,
          such as the Eclipse bin directory.
        * "UNDEFINED" : value before this method has executed.
  
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
        calculateInitiatorFileV(entryPointClass, theCommandArgs);
        
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

    private static void calculateInitiatorFileV(
        Class<?> entryPointClass, CommandArgs theCommandArgs) 
      /* This method defines the value of the initiatorFile variable.
        It will be:
        * exe file if a self-extracting exe launcher was the first execution.
        * jar file if jave.exe was the first execution and it ran a jar file.
        * UNDEFINED if jave.exe was the first execution and it ran 
          a class file. This last case happens when running from Eclipse.
          The initiator might be considered to be a class file.
        */
      {
        process: {
          { // Test for exe initiator file.
            String pathString= theCommandArgs.switchValue("-userDir");
            if (pathString != null) {
              initiatorFile= new File(pathString,"Infogora.exe");
              //break process;   //// Disable until exe support is better.
              }
            }
          // Not exe file.
          try { // Test for jar initiator file.
              initiatorFile= new File( 
                  entryPointClass.getProtectionDomain().
                  getCodeSource().getLocation().toURI());
              break process;
            } catch (URISyntaxException e1) {
              appLogger.exception("SystemState.calculateAppFileV()", e1);
            }
          }
        appLogger.info(
            "SystemState.calculateAppFileV(), appFile=\n  "+initiatorFile);
        if (initiatorFile.getName().endsWith(".jar")) appLogger.info(
            "SystemState.calculateAppFileV(), executable jar file found.");
        if (initiatorFile.getName().endsWith(".exe")) appLogger.info(
            "SystemState.calculateAppFileV(), executable exe file found.");
        }

    private static void logSystemPropertyV(String nameString) 
      {
        appLogger.info(
            "logSystemPropertyV(..) "+nameString+
            "="+System.getProperty(nameString)
            );
        }

    }