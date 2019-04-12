package allClasses;

//// import java.io.File;

public class SystemSettings

  /* This class is for reading and logging various system settings.
     This includes OS, User, and Java properties.
     */

  {
  
    public static String homeFolderPathString=
        System.getProperty("user.home");

    public static void logSystemPropertiesV(AppLog theAppLog)
      /* This method logs to theAppLog some of the System properties
        that might be useful to developers.
    
        ///enh Eventually these parameters could be displayed as part the tree.
          For now they are only logged.
        */
      {
        logSystemPropertyV(theAppLog, "java.class.path"); // class directories and JAR archives.
        logSystemPropertyV(theAppLog, "java.home"); // Installation directory for JRE.
        logSystemPropertyV(theAppLog, "java.version"); // JRE version number.
        logSystemPropertyV(theAppLog, "os.arch"); // Operating system architecture
        logSystemPropertyV(theAppLog, "os.name"); // Operating system name
        logSystemPropertyV(theAppLog, "os.version"); // Operating system version
        logSystemPropertyV(theAppLog, "user.dir"); // User working directory
        logSystemPropertyV(theAppLog, "user.home"); // User home directory
        logSystemPropertyV(theAppLog, "user.name"); // User account name
        }
    
    private static void logSystemPropertyV(AppLog theAppLog, String nameString) 
      { 
        String valueString= System.getProperty(nameString);
        theAppLog.info( "SystemSettings. "+nameString + "= "+valueString );
        }

    }