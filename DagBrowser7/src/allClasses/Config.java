package allClasses;

import java.io.File;

//import static allClasses.Globals.*;  // appLogger;

public class Config

  /* This class stores configuration, preferences, and some state state.
    Most of it is fixed, in the form of constant final values.]
    
    Some of it may later be able to be 
    changed and stored in external text files, such as
    the files used by the class Persist. 
   	*/

  {


  /* This section defines the app's name.  */
  
	private static final String AppNameString= "Infogora";

  public static String getAppNameString()
    { return AppNameString; }

	
  /* This section defines all the folders where this app stores its data.  
  It does some run-time initialization with static initializers.  
  */

	private static File homeFolderFile;  // User home directory.

  static
  /* This class static code block initializes some static variables.  */
  { // Initialize MetaFile static fields.
    String homeFolderString= System.getProperty("user.home");
    // System.out.println( HomeFolderString );
    homeFolderFile= new File( 
      new File( homeFolderString ), 
      Config.getAppNameString()
      );
    homeFolderFile.mkdirs();  // Create home folder if it doesn't exist.
    } // Initialize MetaFile static fields.

  static public File resolveFile( String FileNameString )
    /* This method creates a File name object for 
      a file named FileNameString in the app folder.  */
    {
      return new File( homeFolderFile, FileNameString) ;
      }

  
  	// Delays, for adjustments and slowing the entire app for debugging.
		///enh Add additive and multiplicative adjustments for debugging.
	
		public static final long maxTimeOut5000MsL= 5000;
		public static final long handshakePause5000MsL= 5000;
		public static final long fileCopyRetryPause1000MsL= 1000;
		public static final long errorRetryPause1000MsL= 1000;
		public static final long activityBlinkerPeriod1000MsL= 1000;
		public static final long multicastPeriod10000MsL= 10000;
		public static final long pingReplyHandshakePeriod2000MsL= 2000;

		public static final long systemsMonitorPeriod1000MsL= 1000;

		public static final long initialRoundTripTime100MsL= 25;

		public static final long initialRoundTripTime100MsAsNsL= 
				initialRoundTripTime100MsL * 1000000;

		// Not final to prevent a "Comparing identical" warning where used.
		public static long packetSendDelayMsL= 0; // 500; // 1000;

		public static final int QUEUE_SIZE= 5;

		public static final char delimiterChar= '!';
		
		private Config() {} // Private so it can't be constructed.


    }
