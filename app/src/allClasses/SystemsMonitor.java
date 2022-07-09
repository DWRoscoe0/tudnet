package allClasses;

import java.util.concurrent.atomic.AtomicLong;

import static allClasses.AppLog.theAppLog;
import static allClasses.AppLog.LogLevel.*;
import static allClasses.SystemSettings.NL;


public class SystemsMonitor 

  extends MutableList

  implements Runnable

  { // class SystemsMonitor
  
    /* ///ano This class was created in anticipation of,
     * ///ano and in response to, various app performance anomalies.
     * ///ano It measures and displays various system parameters to help with
     * ///ano the measurement, analysis, and mitigation of those anomalies.
     * 
     *
     * ///ano One group of parameters is for monitoring Java garbage collection.
     * Monitors of these parameter were originally added to 
     * the app's Navigation window but were later moved to here.
     * This group of parameters was added when some performance anomalies 
     * appeared which were similar to what happens when an app 
     * spends most of its time garbage collecting.
     * The following is a summary of events sarounding these additions.
     * * The anomalous behavior began when the Eclipse IDE, which is also
     *   written in Java, became sluggish after this app was started from it.  
     *   Eclipse became completely non-responsive within a few minutes.
     *   but the TUDNet app seemed to work okay and remained responsive.
     * * This happened when the app was run in both regular and Debug modes.
     * * Terminating the TUDNet app would not immediately make Eclipse responsive.
     *   Eclipse needed to be terminated using the Windows Task Manager
     *   and restarted.  This behavior was replicated several times.
     * * The decision was made to add garbage collection monitors to the app,
     *   but before they could be added the anomalous behavior changed.
     *   Eclipse would still become sluggish and non-responsive, but gradually.
     *   More than an hour was needed before it was mostly non-responsive.
     * * Monitors were added to display the following:
     *   * Runtime.freeMemory()
     *   * Runtime.totalMemory()
     *   * The number of garbage collection passes after app start
     * * The monitors showed that the time between garbage collections 
     *   was at least a minute, and they happened when free memory was low
     *   but nowhere near being exhausted.
     * * Eclipse responsiveness could now be restored immediately 
     *   by terminating the TUDNet app. 
     *   Terminating and restarting Eclipse was no longer necessary.
     * * It was noticed that BackupTerminator, 
     *   a class which had been added to deal with a termination anomaly,
     *   was no longer being activated to terminate the app.
     * * It was noticed that if the app's JavaFX window is minimized,
     *   the garbage collection execution frequency increased 
     *   from running once every couple of minutes
     *   to running between 1 and 2 times per second,
     *   occasionally as high as 5 times per second.
     *
     *
     *  ///enh? Maybe completely rewrite to measure everything in 2 ms periods
     *  without binary search, as follows:
     *  * in first ms
     *    * Start with a wait for ms boundary.
     *    * do some measurements, new and more robust.
     *    * do all displays 
     *    * busy wait while reading ms time to next ms boundary.
     *  * in second ms
     *    * do a simple CPU counting loop to measure CPU performance. 
     *    * Loop if reading ms time shows next ms boundary not reached yet.
     *
     *
     *  ///enh Measure and display CPU and memory usage using 
     *    https://github.com/jezhumble/javasysmon/ or something similar.
     *    
     */
  
    // Injected instance variables, all private.
    
    private final NotifyingQueue<String> toConnectionManagerNotifyingQueueOfStrings;
    
    // Other instance variables, all private.

    private final long oneSecondOfNsL=  1000000000L;
    private final long periodMsL=  // Time between measurements.
        Config.systemsMonitorPeriod1000MsL;
    
    private long measurementTimeMsL; // Next time to do measurements.

    // Detail-containing child sub-objects.
      private NamedLong measurementCyclesNamedLong;
      private static long garbageCollectionsL= 0;
      private static NamedLong garbageCollectionsNamedLong; 
      { // Create initial immediately garbage-collectible object. 
        new GarbageObject(); //Construct it but don't save a reference to it.
        }
      private static class GarbageObject // For counting garbage collections.
        extends Object 
        {
          @Override
          protected void finalize() { // Called once per garbage collection.
            garbageCollectionsL++; // Increment GC count.
            new GarbageObject(); // Construct and discard replacement object.
            }
          };
      private NamedLong freeMemoryNamedLong;
      private NamedLong totalMemoryNamedLong;
      private NamedLong processorsNamedLong;
      private NamedMutable javaVersionNamedMutable; 
      private long waitEndNsL= -1;
      private long waitEndOldNsL= -1;
      private NamedLong waitJitterNsNamedLong;
      // No longer measured.
      // private long nanoTimeOverheadNsL;
      // private NamedLong nanoTimeOverheadNamedInteger= new NamedLong( 
      //    "nanoTime() overhead (ns)", -1 
      //    );
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

    public SystemsMonitor( // Constructor.
        NotifyingQueue<String> toConnectionManagerNotifyingQueueOfStrings)
      {
        initializeV(  // Constructing base class.
          "Systems-Monitor", // DataNode (not thread) name.
          new DataNode[]{} // Initially empty List of Peers.
          );
        this.toConnectionManagerNotifyingQueueOfStrings= 
            toConnectionManagerNotifyingQueueOfStrings;
  
        createAndAddChildrenV(); // Do non-dependency-injection initialization.
        }

    public void run()
      /* This method, after some initialization, repeatedly
        measures and displays various parameters once every second.
        It might seem a little confusing because 
        the method it calls in a loop is actually 
        a binary search method that determines CPU speed.
        A method it calls to do a search test also
        measures and displays other performances parameters
        and does a wait that determines the measurement period time.
       */
      {
        theAppLog.info( "run() begins." );
  
        measurementTimeMsL= // Setting time to do first measurement... 
            System.currentTimeMillis(); //  to be immediately.
        while // Repeating measurement and display... 
          ( !EpiThread.testInterruptB() ) // until termination is requested.
          {
            doBinarySearchOfCPUSpeedAndDoOtherStuffL();
            }
        
        theAppLog.info( "run() ends." );
        }

    private void createAndAddChildrenV()
      {
        // Assign variables.
        measurementCyclesNamedLong= new NamedLong( 
            "Measurement-Cycles", 0
            );
        garbageCollectionsNamedLong= new NamedLong( 
            "Garbage-Collections", 0
            );
        freeMemoryNamedLong= new NamedLong( 
            "Free-Memory", 0
            );
        totalMemoryNamedLong= new NamedLong( 
            "Total-Memory", 0
            );
        processorsNamedLong= new NamedLong( 
            "Processors"
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
        addAtEndV( measurementCyclesNamedLong );
        addAtEndV( garbageCollectionsNamedLong );
        addAtEndV( freeMemoryNamedLong );
        addAtEndV( totalMemoryNamedLong );
        addAtEndV( processorsNamedLong );
        addAtEndV( javaVersionNamedMutable );
        //addB( nanoTimeOverheadNamedInteger );
        addAtEndV( cpuSpeedNamedLong );
        addAtEndV( waitJitterNsNamedLong );
        addAtEndV( endWaitMsNamedLong );
        addAtEndV( eventQueueInvokeAndWaitEntryNsAsMsNamedLong );
        addAtEndV( eventQueueInvokeAndWaitExitNsAsMsNamedLong );
        addAtEndV( skippedTimeMsNamedLong );
        addAtEndV( reversedTimeMsNamedLong );
        }

    private long doBinarySearchOfCPUSpeedAndDoOtherStuffL()
      /* This method does a expanding and contracting binary search 
        to measure the CPU speed, 
        which is expressed as the count value that produces
        a 1 ms delay using the method measureCPUDelayNsL(long countL).
        It calls cpuMeasureAndDisplayAndDelayNsL(..) to make that measurement,
        but that method also measures and displays other performance parameters
        and does a wait to create the measurement cycle.
        It returns how far the CPU can count in a loop for 1 ms.
        It aborts the search if the thread is interrupted.

        The CPU speed can be affected by several factors, such as:
        * Variation of the processor clock speed by power management systems.
        * Preemption by other threads or interrupt service routines 
          running on the same processor core.
        * Contention with threads running on other processors or cores which
          share resources such as caches, data pathways, or 
          instruction sequencing logic.
          
        CPU speed measurement takes approximately 1 ms.
        With a measurement cycle of 1000 ms.
        the execution overhead is only about 0.1%.

        ///enh Maybe prevent overshoot in displayed values,
        which can happen during expanding parts of binary search.
        */
      {
        final long targetNsL= 1000000; // # of ns in the 1 ms target interval. 
  
        while (!EpiThread.testInterruptB()) // Expand interval up while possible.
          { 
            final long maxNsL= cpuMeasureAndDisplayAndDelayNsL( maxCountL );
            if ( targetNsL <= maxNsL ) break;                      
            final long intervalL= (maxCountL - minCountL) + 1;
            minCountL+= intervalL;
            maxCountL+= (intervalL*2);
            }
        while (!EpiThread.testInterruptB()) // Expand interval down while possible.
          { 
            final long minNsL= cpuMeasureAndDisplayAndDelayNsL( minCountL );
            if ( targetNsL >= minNsL ) break;
            final long intervalL= (maxCountL - minCountL) + 1;
            minCountL-= (intervalL*2);
            maxCountL-= intervalL;
            }
        while  // Divide interval until its one point.
          (!EpiThread.testInterruptB())
          {
            if (maxCountL <= minCountL) break; // Interval is single point.
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
      ///enh Maybe replace this fast search with 
        // storing and retrieving previous value in Persistent storage.

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
        measurementCyclesNamedLong.addDeltaL(1);
        freeMemoryNamedLong.setValueL(Runtime.getRuntime().freeMemory());
        totalMemoryNamedLong.setValueL(Runtime.getRuntime().totalMemory());
        garbageCollectionsNamedLong.setValueL(garbageCollectionsL);
        processorsNamedLong.setValueL( // Displaying processor count. 
            Runtime.getRuntime().availableProcessors() ); // This could change.
        new Thread( () -> { // Do EDT measurements on separate thread. 
          final AtomicLong inEDTNsAtomicLong= new AtomicLong(); 
          final long beforeEDTNsL= System.nanoTime();
          EDTUtilities.invokeAndWaitV( // Measure time on EDT and return.
              () -> { inEDTNsAtomicLong.set(System.nanoTime()); } 
              );
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
          }).start();

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
        if (shiftInTimeMsL > 0) { // Processing skipped time, if any.
          addDeltaAndLogNonzeroL(skippedTimeMsNamedLong, shiftInTimeMsL ); // Record it.
          toConnectionManagerNotifyingQueueOfStrings.put( // Passing to CM for Unicasters.
              "Skipped-Time");
          }
        if (shiftInTimeMsL < 0) // Processing reversed time, if any.
          addDeltaAndLogNonzeroL(reversedTimeMsNamedLong, shiftInTimeMsL );
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

    public long addDeltaAndLogNonzeroL( NamedLong theNamedLong, long deltaL )
    /* This method does the same as addDeltaL(deltaL) 
      but also logs any deltaL which not 0.
      This is for NamedLongs which are not supposed to change.
      
      This has been tested and works.
      * Skipped-Time changes is tested every time the computer wakes from sleep.
      * Reversed-Time changes was tested on a computer that had been sleeping
        for a long time with the app not running.  Apparently the computer clock
        drifted behind of actual time 8 seconds and at the next Internet update
        when the clock was corrected it was detected and reported as an anomaly.
      */
    {
      if (deltaL != 0) // Logging deltaL if it's not 0.
        {
          long timeNowL= System.currentTimeMillis();
          theAppLog.warning(
              theNamedLong.getNameString( ),
              theNamedLong.getNameString( ) + " changed by " + deltaL
                + NL + NL 
                + "  time-then : " + Misc.dateAndTimeAkaMsString(
                                                          timeNowL - deltaL) 
                + NL + NL 
                + "  time-now  : " + Misc.dateAndTimeAkaMsString(timeNowL) 
                );
          }
      return theNamedLong.addDeltaL( deltaL ); // Doing the add.
      }

    } // class SystemsMonitor
