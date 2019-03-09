package allClasses;

import static allClasses.Globals.appLogger;

import java.util.Arrays;

public class SystemState
  /* This class is for reading and logging various system state variables.
    
    ///enh Eventually these could be displayed as part of tree.
      For now they are only logged.
   */
  {

    public static void logSystemStateV(String[] argStrings)
      {
      appLogger.debug(
          "logSystemStateV(..) argStrings="+Arrays.toString(argStrings));
      
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
    
    public static void logSystemPropertyV(String nameString) 
      {
        appLogger.debug(
            "logSystemPropertyV(..) "+nameString+
            "="+System.getProperty(nameString)
            );
        }

    }