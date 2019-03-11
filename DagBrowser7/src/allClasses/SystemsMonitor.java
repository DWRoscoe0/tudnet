package allClasses;

import static allClasses.Globals.appLogger;

import java.util.concurrent.atomic.AtomicLong;

import static allClasses.AppLog.LogLevel.*;

//import static allClasses.Globals.*;  // appLogger;

public class SystemsMonitor 

  extends MutableList

  implements Runnable
  
  /* This class measures and displays various performance parameters.

	  ///enh? Maybe completely rewrite to measure everything in 2 ms periods,
	  without binary search, as follows:
	  * in first ms
	    * Start with a wait for ms boundary.
	    * do some measurements, new and more robust.
	    * do all displays 
	    * busy wait while reading ms time to next ms boundary.
	  * in second ms
	    * do a simple CPU counting loop to measure CPU performance. 
	    * Loop if reading ms time shows next ms boundary not reached yet.
	  
	  ///enh Measure and display CPU and memory usage using 
	    https://github.com/jezhumble/javasysmon/ or something similar.
	    
	  */

  { // class SystemsMonitor
  
    // Injected instance variables, all private.

    // Other instance variables, all private.

	  private final long oneSecondOfNsL=  1000000000L;
	  private final long periodMsL=  // Time between measurements.
	  		Config.systemsMonitorPeriod1000MsL;
	  
	  private long measurementTimeMsL; // Next time to do measurements.

	  // Detail-containing child sub-objects.
		  private NamedLong measurementCountNamedLong;
      private NamedLong processorsNamedLong;
      private NamedMutable javaVersionNamedMutable; 
		  private long waitEndNsL= -1;
		  private long waitEndOldNsL= -1;
		  private NamedLong waitJitterNsNamedLong;
      // No longer measured.
		  // private long nanoTimeOverheadNsL;
		  // private NamedLong nanoTimeOverheadNamedInteger= new NamedLong( 
      //		"nanoTime() overhead (ns)", -1 
      //  	);
		  private NamedLong cpuSpeedNamedLong;
		  private long endWaitDelayMsL;
		  private NamedLong endWaitMsNamedLong;
		  private NsAsMsNamedLong eventQueueInvokeAndWaitEntryNsAsMsNamedLong;
		  private NsAsMsNamedLong eventQueueInvokeAndWaitExitNsAsMsNamedLong;
		  private NamedLong skippedTimeMsNamedLong;
		  private NamedLong reversedTimeMsNamedLong;

		// Variables for binary search to find CPU speed.
			private volatile long volatileCounterL;
			  // This is volatile to prevent over-optimizing count loops.
			  	
			private long maxCountL= 2; // Using 2 to make initial expansion pretty.
			private long minCountL= 2; // Using 2 to make initial expansion pretty.
			private long midCountL;

		private LockAndSignal theLockAndSignal= new LockAndSignal();
		  // Used for waiting.

    public SystemsMonitor()   // Constructor.
      {
      	initializeV(  // Constructing base class.
          "Systems-Monitor", // DataNode (not thread) name.
          new DataNode[]{} // Initially empty List of Peers.
          );
        }

    public void run()
      /* This method, after some initialization, repeatedly
        measures and displays various parameters once every second.
        It might seem a little confusing because 
        the method it calls in a loop is actually 
        a binary search method that determines CPU speed.
        A method it calls to do a search test also
        measures and displays other performances parameters
        and does a wait that determines the measurement cycle time.
       */
      {
    		appLogger.info( "SystemsMonitor.run() beginning." );
  
    		createAndAddChildrenV();  // Do non-dependency-injection initialization.
  
  		  measurementTimeMsL= // Setting time to do first measurement... 
  		  		System.currentTimeMillis(); //  to be immediately.
        while // Repeating measurement and display... 
          ( !EpiThread.exitingB() ) // until termination is requested.
          {
        		doBinarySearchOfCPUSpeedAndDoOtherStuffL();
        		}
        }

    private void createAndAddChildrenV()
	    {
    	  // Assign variables.
		  	measurementCountNamedLong= new NamedLong( 
	      		"Measurements", 0
	        	);
	      processorsNamedLong= new NamedLong( 
	      		"Processors", -1
	        	);
	      final String javaVersionString= "java.version"; 
	      javaVersionNamedMutable= new NamedMutable(
	          javaVersionString, System.getProperty(javaVersionString)
	          );
			  waitJitterNsNamedLong= new NsAsMsNamedLong( 
	      	"Wait-Jitter (ms)", 0 
	       	);
	      cpuSpeedNamedLong= new NamedLong( 
	      		"CPU-speed (counts / ms)", -1
	        	);
			  endWaitMsNamedLong= new NamedLong( 
	      		"End-Wait (ms)", -1
	        	);
			  eventQueueInvokeAndWaitEntryNsAsMsNamedLong= 
			  		new NsAsMsNamedLong( 
			  				"EventQueue.invokeAndWait(..) entry (ms)", 
			  				-1
			  				);
			  eventQueueInvokeAndWaitExitNsAsMsNamedLong= 
			  		new NsAsMsNamedLong( 
			  				"EventQueue.invokeAndWait(..) exit (ms)", 
			  				-1
			  				);
			  skippedTimeMsNamedLong= new NamedLong( 
	      		"Skipped-Time (ms)", 0
	        	);
			  reversedTimeMsNamedLong= new NamedLong( 
	      		"Reversed-Time (ms)", 0
	        	);

			  // Add variables to our displayed list.
	      addAtEndB( measurementCountNamedLong );
	      addAtEndB( processorsNamedLong );
	      addAtEndB( javaVersionNamedMutable );
	      //addB( nanoTimeOverheadNamedInteger );
	      addAtEndB( cpuSpeedNamedLong );
	      addAtEndB( waitJitterNsNamedLong );
	      addAtEndB( endWaitMsNamedLong );
	      addAtEndB( eventQueueInvokeAndWaitEntryNsAsMsNamedLong );
	      addAtEndB( eventQueueInvokeAndWaitExitNsAsMsNamedLong );
	      addAtEndB( skippedTimeMsNamedLong );
	      addAtEndB( reversedTimeMsNamedLong );
	      }

    private long doBinarySearchOfCPUSpeedAndDoOtherStuffL()
      /* This method does a expanding and contracting binary search 
        to measure the CPU speed, 
        which is expressed as the count value that produces
        a 1 ms delay using the method measureCPUDelayNsL(long countL).
        It calls cpuMeasureAndDisplayAndDelayNsL(..) to make that measurement,
        but that method also measure and display other performance parameters
        and does a wait to create the measurement cycle.
        It returns how far the CPU can count in a loop for 1 ms.

        The CPU speed can be affected by several factors, such as:
        * Variation of the processor clock speed by power management systems.
        * Preemption by other threads or interrupt service routines 
          running on the same processor core.
        * Contention with threads running on other processors or cores which
          share resources such as caches, data pathways, or 
          instruction sequencing logic.
          
		    CPU speed measurement takes approximately 1 ms.
		    With a measuremet cycle of 1000 ms.
		    the execution overhead is only about 0.1%.

        ///enh Maybe prevent overshoot in displayed values,
        which can happen during expanding parts of binary search.
        */
    	{
    	  final long targetNsL= 1000000; // # of ns in the 1 ms target interval. 
	
        while (true) // Expand interval up while possible.
          { 
        		final long maxNsL= cpuMeasureAndDisplayAndDelayNsL( maxCountL );
        		if ( targetNsL <= maxNsL ) break;		              	  
        		final long intervalL= (maxCountL - minCountL) + 1;
            minCountL+= intervalL;
            maxCountL+= (intervalL*2);
          	}
        while (true) // Expand interval down while possible.
          { 
        		final long minNsL= cpuMeasureAndDisplayAndDelayNsL( minCountL );
          	if ( targetNsL >= minNsL ) break;
        		final long intervalL= (maxCountL - minCountL) + 1;
            minCountL-= (intervalL*2);
            maxCountL-= intervalL;
          	}
        while (maxCountL > minCountL) // Divide interval until its one point.
	        { // Divide the interval.
	          midCountL= (maxCountL - minCountL)/2 + minCountL; // Use shift?
        		final long midNsL= cpuMeasureAndDisplayAndDelayNsL( midCountL );
	          if ( targetNsL < midNsL ) // Select half-interval based on result.
              maxCountL= midCountL; /* select lower interval half */
	            else
              minCountL= midCountL+1; /* select upper interval half */
	          }
	    	return minCountL;
      	}
		    
    private long measureCPUDelayNsL( long countMaxL )
      /* This method returns the amount of time in ns needed 
        to count from countMaxL down to 0.
        It uses a volatile counter variable to preventing over-optimizing.
       */
      { 
    		volatileCounterL= countMaxL;
	  		long startNsL= System.nanoTime();
	  		while ( volatileCounterL > 0 ) volatileCounterL--;
	  		return System.nanoTime() - startNsL;
    	  }

    private int cpuInitCountDownI= 50; // While non-0 we update only CPU speed.

    private long cpuMeasureAndDisplayAndDelayNsL( long cpuSpeedCountL )
      /* This method is called by the binary search routine.
        It displays cpuSpeedCountL, and measures and returns
        the number of ns needed to count to cpuSpeedCountL.
       */
	    {
	    	cpuSpeedNamedLong.setValueL(  // Update display of CPU speed. 
	    			cpuSpeedCountL 
	    			);
	    	
	    	if ( cpuInitCountDownI != 0 ) // Acting based on initialization counter.
	    		{ // Decrement CPU speed initialization counter and wait briefly. 
		    		cpuInitCountDownI--; // Decrement initialization counter.
		    		theLockAndSignal.waitingForInterruptOrDelayOrNotificationE(10);
		    		}
	    		else
	    		measureAndDisplayAndDelayV(); // Do everything else.
	  		
	    	// CPU speed measurement is done now because a wait ended very recently.
	    	final long cpuSpeedNsL= measureCPUDelayNsL( cpuSpeedCountL );
	  		return cpuSpeedNsL;
	  		}

    private void measureAndDisplayAndDelayV()
      /* 
        This method does several things:
        * It measures several performance parameters.
        * It delays this thread by waiting until the next measurement time.
        
        The caller of this method should do several things:
        * It should call this method repeatedly.
        * It may do other things, provided it can complete them
          within the measurement period, but it should keep these to a minimum.
        Presently the caller is binarySearchOfCPUSpeedAndDoOtherStuffL(),
        a method which does a binary search to measure CPU speed
        in terms of how far the CPU can count in 1 ms. 
		    
        ///enh? Do only simple measurements and stores in Runnable.
        ///enh? To truly measure EDT dispatch time.
          call invokeLater(..) to queue 2 Runnables which save nanoTime()
          in 2 seperate variables.  Use those to measure dispatch time.

        ///enh? Maybe randomize the measurement delays to get better measurements.
        * Pick an period p, for example, 1 second.
        * Generate wait delays evenly distributed between 0 and 2p.
        * 
        
        */
	    {
    		measurementCountNamedLong.addDeltaL(1);
    		processorsNamedLong.setValueL( // Displaying processor count. 
    				Runtime.getRuntime().availableProcessors() ); // This could change.
      	final AtomicLong inEDTNsAtomicLong= new AtomicLong(); 
      	final long beforeEDTNsL= System.nanoTime();
      	EDTUtilities.invokeAndWaitV( // Dispatching on EDT
          new Runnable() { // a Runnable which just records the time.
            @Override
            public void run() { inEDTNsAtomicLong.set( System.nanoTime() ); }  
            } );
      	final long afterEDTNsL= System.nanoTime();
      	eventQueueInvokeAndWaitEntryNsAsMsNamedLong.setValueL( 
      			inEDTNsAtomicLong.get() - beforeEDTNsL ); 
      	eventQueueInvokeAndWaitExitNsAsMsNamedLong.setValueL(
      			afterEDTNsL - inEDTNsAtomicLong.get() );
      	//nanoTimeOverheadNamedInteger.setValueL( nanoTimeOverheadNsL  );
      	waitJitterNsNamedLong.setValueL( 
      			(waitEndNsL - waitEndOldNsL) - oneSecondOfNsL );
      	waitEndOldNsL= waitEndNsL;
      	endWaitMsNamedLong.setValueL( endWaitDelayMsL );

	    	waitForNextMeasurementTimeV();

	    	// We just finished a wait until a point in time, 
	    	// so now is the best time to do interrupt-free measurements.
	    	// We will now measure end-wait time, 
	    	// and then return to let the caller measure CPU speed.
    	  waitEndNsL= System.nanoTime(); 
	    	endWaitDelayMsL= System.currentTimeMillis() - measurementTimeMsL;
      	//nanoTimeOverheadNsL= measureNanoTimeOverheadNsL();
    		return;  // The next thing in caller will be a CPU speed measurement.
		    }

  	private void waitForNextMeasurementTimeV()
  	  /* This method waits for the next measurement time.
  	    It also detects, reports, and compensates for
  	    any large time discontinuities caused by device sleeping
  	    or resetting of the system clock. 
  	    */
	    { 
			  long shiftInTimeMsL= // Testing for large time discontinuity.
				    theLockAndSignal.periodCorrectedShiftMsL(
				    		measurementTimeMsL, periodMsL
				    		);
  			if (shiftInTimeMsL > 0) // Processing skipped time, if any.
	  			skippedTimeMsNamedLong.addDeltaAndLogNonzeroL( shiftInTimeMsL );
	  		if (shiftInTimeMsL < 0) // Processing reversed time, if any.
	  			reversedTimeMsNamedLong.addDeltaAndLogNonzeroL( -shiftInTimeMsL );
	  		measurementTimeMsL+= // Adjusting base time for discontinuity, if any.
	  				shiftInTimeMsL; 

	  		logB( TRACE,
    				"SystemsMonitor.waitForNextMeasurementTimeV() before wait." );
  			theLockAndSignal.waitingForInterruptOrDelayOrNotificationE(
  					theLockAndSignal.periodCorrectedDelayMsL( 
  							measurementTimeMsL, periodMsL 
  							)
  					); // Waiting for next measurement time.
  			measurementTimeMsL+= periodMsL; // Incrementing variable to match it.
        }

    } // class SystemsMonitor
