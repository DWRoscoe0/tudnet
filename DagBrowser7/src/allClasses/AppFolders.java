package allClasses;

import java.io.File;

public class AppFolders

  /* This class manages all the folders where this app stores its data.  
    It does some run-time initialization with static initializers.  
    */

	{
    private static File homeFolderFile;  // User home directory.

    static
    /* This class static code block initializes some static variables.  */
    { // Initialize MetaFile static fields.
      String homeFolderString= System.getProperty("user.home");
      // System.out.println( HomeFolderString );
      homeFolderFile= new File( 
        new File( homeFolderString ), 
        AppName.getAppNameString()
        );
      homeFolderFile.mkdirs();  // Create home folder if it doesn't exist.
      } // Initialize MetaFile static fields.

    static public File resolveFile( String FileNameString )
      /* This method creates a File name object for 
        a file named FileNameString in the app folder.  */
      {
        return new File( homeFolderFile, FileNameString) ;
        }
    }
