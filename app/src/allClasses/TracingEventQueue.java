package allClasses;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

import static allClasses.AppLog.theAppLog;


public class TracingEventQueue extends EventQueue {

  /* ///ano The purpose of this class,
    and its associated class TracingEventQueueMonitor, 
    is to report the anomaly of when Swing's Event Dispatch Thread (EDT) 
    uses excessive time to process events.
    It tests this for every dispatch, in 2 ways:
    * during the dispatch, by polling, and 
    * after the dispatch completes.
    The dispatch time does not include the time in the queue 
    spent waiting to be dispatched.

    This class was based on one gotten from an article at
    https://today.java.net/pub/a/today/2007/08/30/debugging-swing.html
    which is now a bad link.

    ///enh Rewrite to not use a polling loop in a thread.
      It could be rewritten to use ScheduledThreadPoolExecutor.
      On the other hand, a polling thread is a good alternative for
      malfunctions by ScheduledThreadPoolExecutor.

    ///enh Add event counter and use to identify events processed.

    Dialog windows are now displayed using JavaFX, not Swing,
    so Swing/EDT dispatches could be completely blocked,
    but reports could continue.
    An earlier version used Swing/EDT dialog windows.
    In this case, the display of dialog windows depended on
    EDT dispatching eventually being unblocked.
    */

  private TracingEventQueueMonitor theTracingEventQueueMonitor;

  public TracingEventQueue( // Constructor. 
     TracingEventQueueMonitor theTracingEventQueueMonitor
     ) 
   {
     this.theTracingEventQueueMonitor= theTracingEventQueueMonitor;  
     }

  public void initializeV() // Post-constructor initialization.
    {
      theTracingEventQueueMonitor.start(); // Start monitor thread polling.
      }

  @Override
  protected void dispatchEvent(AWTEvent event)
    /* This method wraps its superclass version of the same method
     * such that it is preceded and followed by calls to methods that
     * together check that the dispatch time is not too long.
     */
    {
      this.theTracingEventQueueMonitor.eventDispatchBeginV(event);
      super.dispatchEvent(event);
      this.theTracingEventQueueMonitor.eventDispatchEndV(event);
      }
  
  } // TracingEventQueue


class TracingEventQueueMonitor extends Thread {

  /* The purpose of this class is to help TracingEventQueue 
    identify when the EDT is taking too long to process events.
    It helps in two ways:
    * It provides methods callable from TracingEventQueue to
      measure and display the total dispatch time if it exceeded a threshold.
    * It provides a thread which does the same threshold test
      and displays a stack trace if the threshold is exceeded 
      during the dispatch to help identify the CPU-hogging code.
      The Thread.sleep(..) time defines the sampling rate.
      It might need to be adjusted to locate CPU-hogging code.
    
    ///enh Maybe do without polling.  See note elsewhere about Watchdog timer.
     */

  private static final long pollingPeriodMsL= 250; // 1/4 second.
  private static final boolean displayStackB= true; /// false;
  private final long dispatchTimeMaximumMsL= 500; // 1/2 second.
  
  class EventValue 
    /* This class stores information about events being dispatched. */
    {
      long dispatchStartTimeMsL; 
      boolean overLimitReportedB; /// No longer needed.

      EventValue(long dispatchStartTimeMsL) { // constructor.
        this.dispatchStartTimeMsL= dispatchStartTimeMsL;
        }
      }

  private Map<AWTEvent, EventValue> eventTimeMap; /* Where event data is stored.
    If should not contain more than one event, but using a map can't hurt.  */

  public TracingEventQueueMonitor() // constructor
    {
      super("TracingEventQueueMonitor"); // Set thread name.
      this.eventTimeMap = new HashMap<AWTEvent, EventValue>();
      setDaemon(true);
      }

  public synchronized void eventDispatchBeginV(AWTEvent theAWTEvent)
    /* This method processes the beginning of event dispatch by
     * creating an EventValue in the map associated with theEvent.
     */
    {
      this.eventTimeMap.put(
          theAWTEvent, new EventValue(System.currentTimeMillis())
          );
      }

  public void eventDispatchEndV(AWTEvent theAWTEvent) 
    /* Processes the end of an event dispatch by
      doing a time check and removing the associated map entry.
      It does not do a stack trace because it's too late.
      Only the polling thread does stack traces.
      */
    {
      synchronized(this) {
        this.checkEventTimeB(
            "completed",
            theAWTEvent, 
            System.currentTimeMillis(),
            this.eventTimeMap.get(theAWTEvent).dispatchStartTimeMsL
            );
        this.eventTimeMap.remove(theAWTEvent);
        }
      //appLogger.debug(
      //    "TracingEventQueueMonitor.eventDispatchingEndingV(..)"
      //    );
      }

  @Override
  public void run() 
    /* This method periodically tests whether any EDT dispatches
      that are stored in the map have taken too long.
      If any have, then it reports them.
      The report may include a stack trace of the AWT-EventQueue thread
      to help to locate CPU-hogging code.
      */
    {
      theAppLog.info( "daemon run() starting." );
      while (true) // Do the checking forever. 
        { // Check all events in map, then wait for a while.
          long timeNowMsL = System.currentTimeMillis();
          synchronized (this) { // Prevent new dispatches while checking.
            for // Check every map entry.
              ( Map.Entry<AWTEvent,EventValue> theMapEntry : 
                this.eventTimeMap.entrySet() )
              { // Check one map entry.
                AWTEvent theAWTEvent= theMapEntry.getKey();
                if (theMapEntry.getValue() == null) // Skipping if no value.
                  continue;
                /// if  // Skipping if this entry was reported earlier.
                ///   (theMapEntry.getValue().overLimitReportedB)
                ///   continue;
                long startTime = theMapEntry.getValue().dispatchStartTimeMsL;
                boolean delayLimitExceededB= // Report if delay too long.
                  this.checkEventTimeB(
                    "underway",theAWTEvent, timeNowMsL, startTime);
                if  // Skipping if not too long and report was not made.
                  ( ! delayLimitExceededB )
                  continue;
                logStackTraceV(); // Display stack as well.
                theMapEntry.getValue().overLimitReportedB= true; // Record act of report.
                }
            }
          EpiThread.interruptibleSleepB(pollingPeriodMsL); // Waiting a while.   
          }
    }

  private boolean checkEventTimeB(
      String dispatchStateString, 
      AWTEvent dispatchedAWTEvent, 
      long timeNowMsL, 
      long dispatchStartTimeMsL
      ) 
    /* This method checks whether an event dispatch 
     * has been, or is running, to long.
     * If true then it reports that fact using a dialog box and logging,
     * and it returns true.  Otherwise it does nothing and returns false.
     * 
     * This method is called
     * * by eventDispatchingEndingV(..) after an an event dispatch completes
     *   to check that particular event, and
     * * by the polling thread run() method to check whether there are 
     *   any events have been dispatched but not yet completed and
     *   already exceed their maximum time.
     *   
     * If the dispatch time limit is exceeded in either case
     * then it reports it in the log file and in a dialog.
     */
    {
      long timeSinceDispatchStartMsL = timeNowMsL - dispatchStartTimeMsL;
      boolean limitExceededB= 
          (timeSinceDispatchStartMsL > dispatchTimeMaximumMsL);
      if (limitExceededB) // If excessive time used for dispatch, report it.
        { ///ano Report excessive time used for event dispatch.
          String summaryIDLineString= 
              "Excessive time for EDT dispatch.";
          String detailsString= "In EDT dispatch of "
              + dispatchedAWTEvent.getClass().getName()
              + ", " + dispatchStateString
              + ", processing time of " + timeSinceDispatchStartMsL
              + "ms exceeds limit of " + dispatchTimeMaximumMsL;
          Anomalies.displayDialogReturnString(
              summaryIDLineString, detailsString, true);
          }
      return limitExceededB; 
      }

  private void logStackTraceV()
    /* This method logs the stack trace of the AWT-EventQueue thread.
     * It also does the TracingEventQueueMonitor.
     * 
     * The purpose displaying the stack trace is to help to discover
     * the reason for the slow dispatch time.
     * Unfortunately, by the time the stack is logged
     * the dispatch is complete and the cause is lost.
     * 
     * There does not appear to be an easy way to get ThreadInfo
     * from a Thread except to get all the thread IDs,
     * get the ThreadInfo for each ID, and check for the desired thread name.
     * 
     */
    {
      if ( displayStackB ) // Displaying stack if enabled.
        {
          ThreadMXBean threadBean= 
              ManagementFactory.getThreadMXBean();
          long threadIds[] = threadBean.getAllThreadIds();
          for (long threadId : threadIds) { // Log more only if desired thread.
            ThreadInfo threadInfo= 
              threadBean.getThreadInfo(threadId,Integer.MAX_VALUE);
            String threadNameString= threadInfo.getThreadName(); 
            if // Log additional information only if it's the thread we want.
              (threadNameString.startsWith("AWT-EventQueue")) 
              /// (threadNameString.startsWith("TracingEventQueueMonitor")) )
              { // Log additional information.
                StackTraceElement[] stack= threadInfo.getStackTrace();
                theAppLog.debug( "Thread " + threadInfo.getThreadName() 
                  + " state=" + threadInfo.getThreadState() );
                theAppLog.debug("Begin Stack Trace.");
                for (StackTraceElement stackEntry : stack) {
                  theAppLog.debug("\t" + stackEntry.getClassName()
                    + "." + stackEntry.getMethodName() + " ["
                    + stackEntry.getLineNumber() + "]");
                  }
                theAppLog.debug("End Stack Trace.");
                // ?? Disable stack trace logging. */
                }
            }
        }
      }

  } // TracingEventQueueMonitor
