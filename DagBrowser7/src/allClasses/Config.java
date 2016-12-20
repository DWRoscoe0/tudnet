package allClasses;

public class Config
  {
	  // Delays, for adjustments and slowing the entire app for debugging.
		//// Add additive and multiplicative adjustments for debugging.
	
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

		// Not final to prevent a "Comparing identical" warning.
		public static long packetSendDelayMsL= 0; // 500; // 1000;

		public static final int QUEUE_SIZE= 5;

		public static final char delimiterChar= '!';
  }
