package allClasses;

public class Delay
  /* This class defines time delays.
    This class exists for the following reasons:
    * It makes app time delay adjustments easier.
    * It makes it possible to slow down the entire app for debugging.
    
    //// Add additive and multiplicative adjustments for debugging.
    */
  {
		public static final long maxTimeOut5000MsL= 5000;
		public static final long handshakePause5000MsL= 5000;
		public static final long fileCopyRetryPause1000MsL= 1000;
		public static final long errorRetryPause1000MsL= 1000;
		public static final long activityBlinkerPeriod1000MsL= 1000;
		public static final long multicastPeriod10000MsL= 10000;
		public static final long pingReplyHandshakePeriod2000MsL= 2000;

		public static final long systemsMonitorPeriod1000MsL= 1000;

		public static final long initialRoundTripTime100MsL= 5; // 100;

		// Not final to prevent a "Comparing identical" warning.
		public static long packetSendDelayMsL= 0; // 500; // 1000;

  }
