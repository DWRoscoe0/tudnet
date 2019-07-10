package allClasses;

import allClasses.AppLog.LogLevel;

public class Config

  /* This class stores configuration and preferences.
    Some are for development.  Some are for normal production use.
    Most of the values are constant, expressed as Java final values.
    
    ///enh Any values that are not final should be set as early as possible.
    
    Some of these values may later be able to be changed and stored in 
    external text files, such as the files used by the class Persist. 
   	*/

  {
    /* Disabling for debugging. */
      public static boolean tcpThreadsDisableB= false;
      public static boolean multicasterThreadsDisableB= false;
      public static boolean unicasterThreadsDisableB= false;
        
		/* Miscellaneous startup behavior. */
		
	    /* The following are used to delay some activities, most at startup.
	      Their purpose might be for debugging or controlling the organization
	      of the beginning of the app's log file. */
				///enh Add additive and multiplicative adjustments for debugging.
				public static final int localUpdateDelaySI=0;
  
    public static final int antiCPUHogLoopDelayMsI=2000;
				
		// Delays, for adjustments and slowing the entire app for debugging.

	  /* TCPCopier parameters.  */
		public static final long tcpCopierRunDelayMsL=4000; //delay logging.
		public static final int tcpFileUpdateDelaySI= 30;
    //// public static final long tcpClientPeriodMsL= 5*60000; // 5 minutes.
    //// public static final long tcpServerCyclePauseMsL= 4000;
    //// public static final long tcpServerMaximumWaitMsL= 30000;
		//// public static final long tcpServerRunDelayMsL=15000;
 		public static final int tcpCopierServerTimeoutMsI= 2*60000;
 		public static final int tcpConnectTimeoutMsI= 5000;
	  ///dbg public static final String tcpCopierInputFolderString= "TCPCopierTmp";
	  public static final String tcpCopierInputFolderString= "TCPCopierStaging";
	  public static final String tcpCopierOutputFolderString= "TCPCopierStaging";

	  
	  public static final long maxTimeOutMsL= 5000;
		public static final long measurementPauseMsL= 60000; 
		public static final long fileCopyRetryPause1000MsL= 1000;
		public static final long errorRetryPause1000MsL= 1000;
		public static final long activityBlinkerPeriod1000MsL= 1000;
    public static final long multicastDelayMsL= 5000;
    public static final long multicastPeriodMsL=
  		600000; // 10 minutes.
  		// 3600000; // 1 hour for testing by effectively disabling multicasting.
		public static final long pingReplyHandshakePeriod2000MsL= 2000;
		public static final long reconnectTimeOutMsL= 120000;
		
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

    public static LogLevel packetLogLevel= AppLog.LogLevel.DEBUG; //TRACE;  // INFO; // DEBUG;
      // By setting this to TRACE, packets sent and received
      // will be logged only when the app is being traced,
      // and maxLogLevel is also TRACE.

		public static final int QUEUE_SIZE= 5;

		public static final char delimiterC= '!';
		public static final char pathSeperatorC= '/'; 

		public static final String appString= "Infogora";
	  public static final String appJarString= appString + ".jar";

  	private Config() {} // Constructor is private so it can't be constructed.
	
  	}
