package allClasses;

import java.io.File;

//import static allClasses.Globals.*;  // appLogger;

public class Config

  /* This class stores configuration, preferences, and some dependent state.
    Most of it is fixed, in the form of constant final values.
    But there are some methods that must be called to be used.
    
    Some of it may later be able to be 
    changed and stored in external text files, such as
    the files used by the class Persist. 
   	*/

  {
	
		/* Miscellaneous startup behavior. */
		
	    /* The following are used to delay some activities, most at startup.
	      Their purpose might be for debugging or controlling the organization
	      of the beginning of the app's log file. */
				///enh Add additive and multiplicative adjustments for debugging.
				public static final int localUpdateDelaySI=0;
				
		  /* Log file.  */
				public static final boolean clearLogFileB= true;
  
		// Delays, for adjustments and slowing the entire app for debugging.

	  /* TCPCopier parameters.  */
		public static final int tcpUpdateDelaySI= 30;
		public static final long tcpClientRunDelayMsL=4000; //delay logging.
		public static final long tcpClientPeriodMsL= 8000;
		public static final long tcpServerCyclePauseMsL= 4000;
		public static final long tcpServerRunDelayMsL=15000;
 		public static final int tcpCopierTimeoutMsI= 5000;
 		public static final int tcpConnectTimeoutMsI= 5000;
	  ///dbg public static final String tcpCopierInputFolderString= "TCPCopierTmp";
	  public static final String tcpCopierInputFolderString= "TCPCopierStaging";
	  public static final String tcpCopierOutputFolderString= "TCPCopierStaging";

	  
	  public static final long maxTimeOut5000MsL= 5000;
		public static final long handshakePause5000MsL= 5000;
		public static final long fileCopyRetryPause1000MsL= 1000;
		public static final long errorRetryPause1000MsL= 1000;
		public static final long activityBlinkerPeriod1000MsL= 1000;
		public static final long multicastPeriod10000MsL= 10000;
		public static final long pingReplyHandshakePeriod2000MsL= 2000;
	
		public static final long systemsMonitorPeriod1000MsL= 1000;
	
		public static final long initialRoundTripTime100MsL= 1000;
		  ///dbg High to allow response before time-out for testing.  Was 25;
	
		public static final long initialRoundTripTime100MsAsNsL= 
				initialRoundTripTime100MsL * 1000000;
	

		// Not final to prevent a "Comparing identical" warning where used.
		public static long packetSendDelayMsL= 0; // 500; // 1000;

		public static final int QUEUE_SIZE= 5;

		public static final char delimiterChar= '!';

		public static final String appString= "Infogora";
	  public static final String appJarString= appString + ".jar";

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
