package allClasses;

public class Config

  /* This class stores configuration and preferences.
    Some are for development.  Some are for normal production use.
    Most of the values are constant, in the form of final values.
    
    ///enh Any values that are not final should be set as early as possible.
    
    Some of these values may later be able to be changed and stored in 
    external text files, such as the files used by the class Persist. 
   	*/

  {
    /* Disabling for debugging. */
      public static boolean tcpThreadsDisableB= true;  //// false;
      public static boolean multicasterThreadsDisableB= false;
      public static boolean unicasterThreadsDisableB= true;  //// false;
        
		/* Miscellaneous startup behavior. */
		
	    /* The following are used to delay some activities, most at startup.
	      Their purpose might be for debugging or controlling the organization
	      of the beginning of the app's log file. */
				///enh Add additive and multiplicative adjustments for debugging.
				public static final int localUpdateDelaySI=0;
  
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
		public static final long multicastPeriodMsL=
  		600000; // 10 minutes.
  		// 3600000; // 1 hour for testing to disable multicasting.
      // 2000; //// for debugging.
      // 10000; //// for debugging.
      // 40000; // 40 seconds for normal use.
		public static final long pingReplyHandshakePeriod2000MsL= 2000;
		public static final long reconnectTimeOutMsL= 10000; ///dbg 30000;
		
		public static final long systemsMonitorPeriod1000MsL= 1000;
	
		public static final long initialRoundTripTime100MsL= 1000; ///dbg 100
		  ///dbg High to allow response before time-out for testing.  Was 25;
	
		public static final long initialRoundTripTime100MsAsNsL= 
				initialRoundTripTime100MsL * 1000000;
	
		// AppLog configuration times, in ms.
    public static final long LOG_OPEN_RETRY_TIME= 1;
		public static final long LOG_MIN_CLOSE_TIME= 10; 
    public static final long LOG_PAUSE_TIMEOUT= 200; 
    public static final long LOG_OUTPUT_TIMEOUT= 400; 

		// Following was not final to prevent a "Comparing identical".
		public static final long packetSendDelayMsL= 0; // 500; // 1000;

		public static final int QUEUE_SIZE= 5;

		public static final char delimiterC= '!';
		public static final char pathSeperatorC= '/'; 

		public static final String appString= "Infogora";
	  public static final String appJarString= appString + ".jar";

  	private Config() {} // Constructor is private so it can't be constructed.
	
  	}
