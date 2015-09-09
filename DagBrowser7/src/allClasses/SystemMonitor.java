package allClasses;

import static allClasses.Globals.appLogger;

import java.util.Random;

//import static allClasses.Globals.*;  // appLogger;

public class SystemMonitor 

  extends MutableList

  implements Runnable

  { // class SystemMonitor
  
    // Injected instance variables, all private.

    // Other instance variables, all private.
		  private long targetTimeMsL= System.currentTimeMillis();
		  
	  // Detail-containing child sub-objects.
		  private NamedInteger measurementsNamedInteger= 
		  	new NamedInteger( 
      		theDataTreeModel, "Measurements", 0
        	);
      private NamedInteger processorsNamedInteger= new NamedInteger( 
      		theDataTreeModel, "Processors", -1
        	);
		  private long nanoTimeOverheadL;
		  private NamedInteger nanoTimeOverheadNamedInteger= new NamedInteger( 
      		theDataTreeModel, "nanoTime() overhead (ns)", -1 
        	);
		  private NamedInteger cpuSpeedNamedInteger= new NamedInteger( 
      		theDataTreeModel, "CPU-speed (counts / ms)", -1
        	);
		  private NamedInteger endWaitMsNamedInteger= new NamedInteger( 
      		theDataTreeModel, "End-Wait (ms)", -1
        	);
		  private NamedInteger edtDispatchMsNamedInteger= new NamedInteger( 
      		theDataTreeModel, "EDT-Dispatch (ms)", -1
        	);
		  private NamedInteger skippedTimeMsNamedInteger= new NamedInteger( 
      		theDataTreeModel, "Skipped-Time (ms)", 0
        	);
		  private NamedInteger reversedTimeMsNamedInteger= new NamedInteger( 
      		theDataTreeModel, "Reversed-Time (ms)", 0
        	);

		private volatile long counterL; // Here because volatiles must be object field.
  	
		private long maxCountL= 2; // Using 2 to make initial expansion pretty.
		private long minCountL= 2; // Using 2 to make initial expansion pretty.
		private long midCountL;

		private LockAndSignal theLockAndSignal= new LockAndSignal(false);

    Random theRandom= new Random(0);  // Initialize random # generator.

    public SystemMonitor(   // Constructor.
    		DataTreeModel theDataTreeModel
    		)
      {
        super(  // Constructing base class.
          theDataTreeModel, // For receiving tree change notifications.
          "SystemMonitor", // DataNode (not thread) name.
          new DataNode[]{} // Initially empty List of Peers.
          );

        // Storing other dependencies injected into this class.
        }

    public void run()
      /* This method, after some initialization, repeatedly
        measures and displays various CPU values once every second.
       */
      {
    		//appLogger.info( "SystemMonitor.run() beginning." );

    		initializeV();  // Do non-dependency injection initialization.

        while   // Repeating until termination is requested.
          ( !Thread.currentThread().isInterrupted() )
          {
	        	updateSpeedEtcV( measureCPUSpeedL() );
        		}
        }

    private void initializeV()
	    {
      addB( measurementsNamedInteger );
      addB( processorsNamedInteger );
      addB( nanoTimeOverheadNamedInteger );
      addB( cpuSpeedNamedInteger );
      addB( endWaitMsNamedInteger );
      addB( edtDispatchMsNamedInteger );
      addB( skippedTimeMsNamedInteger );
      addB( reversedTimeMsNamedInteger );
      }

    private void updateSpeedEtcV( long cpuSpeedL )
      /* This method does several things:
        * It updates the CPU speed display from cpuSpeedL.
        * It measures and displays other parameters.
        * Delays this thread until the next measuring point,
          which are 1000 ms apart.
		    CPU speed measurement takes approximately 1 ms,
		    so the overhead is only about 0.1%.
		    
        ?? Maybe randomize the sample times to get better measurements.
        * Pick an period p, for example, 1 second.
        * Generate wait delays evenly distributed between 0 and 2p.
        * Don't do this for blinker.  Use one second for that.
        
        */
	    {
    		cpuSpeedNamedInteger.setValueL( cpuSpeedL );
    		
    		measurementsNamedInteger.addValueL(1);
    		processorsNamedInteger.setValueL( 
    				Runtime.getRuntime().availableProcessors() 
    				); // Keep measuring because this could change.
      	final long endWaitTimeMsL= System.currentTimeMillis();
      	theDataTreeModel.invokeAndWaitV( // Executing on EDT...
          new Runnable() {
            @Override  
            public void run() { 
            	long inEDTTimeMsL= System.currentTimeMillis();
            	endWaitMsNamedInteger.setValueL( endWaitTimeMsL-targetTimeMsL );
            	edtDispatchMsNamedInteger.setValueL( 
            			inEDTTimeMsL-endWaitTimeMsL 
            			);
        		  }  
            } 
          );
      	nanoTimeOverheadNamedInteger.setValueL( nanoTimeOverheadL  );
      	advanceTimeV();

      	// We just finished a wait, so now is the best time to do 
      	// computations or measurements without interruption.
    		nanoTimeOverheadL= measureNanoTimeL(); // Measure and save now.

    		return;  // The next thing in caller will be a CPU speed measurement.
		    }

  	private void advanceTimeV()
  	  /* This method waits for the next measurement time,
  	    and records any anomalous data associated with it. 
  	    */
	    {
			  final long periodMsL= 1000;
			  long shiftInTimeMsL= // Determining any unexpected time shift.
			  		theLockAndSignal.correctionMsL(targetTimeMsL, periodMsL);
  			targetTimeMsL+= shiftInTimeMsL; // Adjusting target time for shift.
  			theLockAndSignal.doWaitWithTimeOutE( // Waiting for next mark.
  					theLockAndSignal.timeOutForMsL( targetTimeMsL, periodMsL )
  					);
  			targetTimeMsL+= periodMsL; // Advancing target time.
  			if (shiftInTimeMsL > 0) // Processing skipped time, if any.
	    		skippedTimeMsNamedInteger.addValueL( shiftInTimeMsL );
  			if (shiftInTimeMsL < 0) // Processing reversed time, if any.
	  			reversedTimeMsNamedInteger.addValueL( -shiftInTimeMsL );
        }

  	private long measureNanoTimeL()
	    /* This method measures the execution time in ns of System.nanoTime().
	      It returns strange values: 
	      * Some times, such as when Firefox is running and has many windows
	        and tabs open, it returns 540 or 1080.
	        At other times, when Firefox is not running, 
	        it returns mostly 0 or 540.
	        More Firefox windows and tabs means longer times.
	        Once in a while it returns outside values of 0 or 1080.
	      * Sometimes it returns 541 or 1081 instead of 540 or 1080.
	      ! Sometimes it returns negative values.
	        This usually happens after the app has been running a while.
	      I suspect that nanoTime() is calculated by 
        scaling clock with a period of close to 540 ns,
        but not exactly, so 1 must be added sometimes.
        I think nanoTime() takes between 0 and 540 ns,
        but sometimes interrupt service routines 
        cause it to take longer.
        */
	    {
	  		long startNsL= System.nanoTime();
	  		long stopNsL= System.nanoTime();
	  		long differenceNsL= stopNsL - startNsL;
	  		if ( differenceNsL < 0 )  // This should never happen.
	    		appLogger.error( 
	    				"SystemMonitor.measureNanoTimeL()="+stopNsL+"-"+startNsL 
	    				);
	  		return differenceNsL;
	    	}

    private long measureCPUSpeedL()
      /* This method measures the CPU speed.
        It returns how far the CPU can count in a loop for 1 ms.
        It does not measure this count directly.
        Instead it uses an expanding and contracting binary search
        to find the count value that produces
        a 1 ms delay using the method measureCPUDelayNsL(long countL).

        The CPU speed can be affected by several factors, such as:
        * Variation of the processor clock speed by power management systems.
        * Preemption by other threads or interrupt service routines 
          running on the same processor core.
        * Contention with threads running on other processors or cores which
          share resources such as caches, data pathways, or 
          instruction sequencing logic.

        ?? Do rapid measurements until value converges, 
        then drop back to one measurement per second.  
        Define converged as position of highest one-bit in count 
        does not change?  Use Integer.highestOneBit(int i)
        */
    	{
    	  final long targetNsL= 1000000; // This many ns is 1 ms count time. 
	
        while (true) // Expand interval up if possible.
          { 
        		updateSpeedEtcV( maxCountL );
        		final long maxNsL= measureCPUDelayNsL( maxCountL );
        		if ( targetNsL <= maxNsL ) break;		              	  
        		final long intervalL= (maxCountL - minCountL) + 1;
            minCountL+= intervalL;
            maxCountL+= (intervalL*2);
          	}
        while (true) // Expand interval down if possible.
          { 
        		updateSpeedEtcV( minCountL );
        		final long minNsL= measureCPUDelayNsL( minCountL );
          	if ( targetNsL >= minNsL ) break;
        		final long intervalL= (maxCountL - minCountL) + 1;
            minCountL-= (intervalL*2);
            maxCountL-= intervalL;
          	}
        while (maxCountL > minCountL) // Divide interval until its one point.
	        { // Divide the interval.
	          midCountL= (maxCountL - minCountL)/2 + minCountL; // Use shift?
	          updateSpeedEtcV( midCountL );
	          final long midNsL= measureCPUDelayNsL( midCountL );
	          if // Select half-interval based on result */
	          	( targetNsL < midNsL )
	            {
	              maxCountL= midCountL; /* select lower interval half */
	            	}
	            else
	            {
	              minCountL= midCountL+1; /* select upper interval half */
	              }
	          }
	    	return minCountL;
      	}
		    
    private long measureCPUDelayNsL( long countMaxL )
      /* This method returns the amount of time in ns needed 
        to count from countMaxL down to 0.
       */
      { 
    		counterL= countMaxL;
	  		long startNsL= System.nanoTime();
	  		while ( counterL > 0 ) counterL--;
	  		return System.nanoTime() - startNsL;
    	  }

    } // class SystemMonitor
