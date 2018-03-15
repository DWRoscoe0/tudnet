package allClasses;

import java.io.File;

//import static allClasses.Globals.*;  // appLogger;

public class Config

  /* This class stores configuration, preferences, and some dependent state.
    Most of it is fixed, in the form of constant final values.
    
    Some of it may later be able to be 
    changed and stored in external text files, such as
    the files used by the class Persist. 
   	*/

  {
  
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

		public static final String appString= "Infogora";
	  public static final String appJarString= appString + ".jar";

	  ////public static final String tcpCopierInputFolderString= "TCPCopierTmp";
	  public static final String tcpCopierInputFolderString= "TCPCopierStaging";
	  public static final String tcpCopierOutputFolderString= "TCPCopierStaging";

		private static final File userAppFolderFile;

		public static final File userAppJarFile;


	  static { // static run-time initialization.
	    String homeFolderPathString= System.getProperty("user.home");
	    userAppFolderFile= 
	    		new File( new File( homeFolderPathString ), appString );
	    userAppJarFile= makeRelativeToAppFolderFile( appJarString ); 
	  	initializeV();
	    }

  	private static void initializeV() // Should be called only once.
  	  // Cannot initialize final variables from here.
	  	{ 
		    userAppFolderFile.mkdirs();  // Create home folder if it doesn't exist.
	  		}

  	private Config() {} // Constructor is private so it can't be constructed.

	  static public File makeRelativeToAppFolderFile( 
	  		String fileRelativePathString )
	    /* This method creates a File name object for 
	      a file named by the fileRelativePathString 
	      relative to the app folder.  */
	    {
	      return new File( userAppFolderFile, fileRelativePathString );
	      }
	
	
  	}
