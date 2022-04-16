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

  /* 
    The purpose of this class and its associated class TracingEventQueueMonitor, 
    is to report when Swing's Event Dispatch Thread (EDT) 
    is using excessive time to process events.  ///ano
    It tests this both during a dispatch, and after every dispatch completes.
    The dispatch time does not include the time spent waiting in the queue.

    This class was based on one gotten from an article at
    https://today.java.net/pub/a/today/2007/08/30/debugging-swing.html
    which is now a bad link.
    
    ///enh Make not use polling.
      ///enh? Integrate with or replace by a general watch-dog timer.
    
    ///enh Add event counter and use to identify events processed.
       
     ? 
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
      theTracingEventQueueMonitor.start(); // Start polling monitor thread.
      }

  @Override
  protected void dispatchEvent(AWTEvent event)
    /* This method wraps its superclass version such that
     * it is preceded and followed by calls to methods that
     * together check that the dispatch time is not too long.
     */
    {
      this.theTracingEventQueueMonitor.eventDispatchBeginningV(event);

      super.dispatchEvent(event);

      this.theTracingEventQueueMonitor.eventDispatchEndingV(event);
      }
  
  } // TracingEventQueue

class TracingEventQueueMonitor extends Thread {

  /* The purpose of this class is to help TracingEventQueue 
    identify when the EDT is taking too long to process events.
    It helps in two ways:
    * It provides methods callable from TracingEventQueue to
      measure and display dispatch time if it exceeds a threshold.
    * It provides a thread which does the same threshold test
      and displays a stack trace if the threshold is exceeded
      to help identify the CPU-hogging code.
    The Thread.sleep(..) time defines the sampling rate.
    It might need to be adjusted to locate CPU-hogging code.
    
    ///enh Maybe do without polling.  See note elsewhere about Watchdog timer.
     */

  private long thresholdDelay;

  public static final long PERIOD= 100;  // was 100
  public static final long LIMIT= 500; // was 500
  private static final boolean displayStackB= false;
  
  class EventValue 
    /* This class stores information about event being dispatched. */
    {
      long startTimeL; // When a dispatch began. 
      boolean reportedB; // true if limit exceeding happened.
      
      EventValue(long startTimeL) { // constructor.
        this.startTimeL= startTimeL;
        }
      }

  private Map<AWTEvent, EventValue> eventTimeMap;
  private boolean eventDispatchingEndingB= false;

  public TracingEventQueueMonitor(long thresholdDelay) {
    super("TracingEventQueueMonitor");
    this.thresholdDelay = thresholdDelay;
    this.eventTimeMap = new HashMap<AWTEvent, EventValue>();
    setDaemon(true);
    }

  public synchronized void eventDispatchBeginningV(AWTEvent event)
    // Processes the beginning of event dispatch by recording in map.
    {
      this.eventTimeMap.put(
          event, new EventValue(System.currentTimeMillis())
          );
      }

  public void eventDispatchEndingV(AWTEvent event) 
    /* Processes the end of an event dispatch by
      doing a time check and removing the associated map entry.
      It does not do a stack trace, which should already have been done
      by an earlier check.
      */
    {
      eventDispatchingEndingB= true;
      synchronized(this) {
        this.checkEventTimeB(
            "completed",
            event, 
            System.currentTimeMillis(),
            this.eventTimeMap.get(event).startTimeL);
        this.eventTimeMap.remove(event);
        }
      eventDispatchingEndingB= false;
      //appLogger.debug(
      //    "TracingEventQueueMonitor.eventDispatchingEndingV(..)"
      //    );
      }

  private boolean checkEventTimeB(
      String underwayOrCompletedString, 
      AWTEvent dispatchedAWTEvent, 
      long currTime, 
      long startTime
      ) 
    /* Reports whether an event dispatch has been running to long,
      longer that thresholdDelay. 
      It returns true if it has, false otherwise. 
      It is called
      * by eventDispatchingEndingV(..) after 
        an an event dispatch completes to check 
        its total processing time.
      * by run() to check whether there are 
        any events have been dispatched but not yet completed,
        and the time since dispatch exceeds the maximum time.
      If the limited is exceeded then it reports it 
      in the log and in a dialog.
      */
    {
      long currProcessingTime = currTime - startTime;
      boolean thresholdExceededB= 
          (currProcessingTime > this.thresholdDelay);
      if (thresholdExceededB) // If excessive time used for dispatch, report it.
        { ///ano Report excessive time used for event dispatch.
          String summaryIDLineString= 
              "Excessive time for EDT dispatch, "+underwayOrCompletedString;
          String detailsString= "In EDT dispatch of "
              + dispatchedAWTEvent.getClass().getName()
              //// + ", now "
              //// + underwayOrCompletedString
              + ", processing time of " + currProcessingTime
              + "ms exceeds limit of " + this.thresholdDelay;
          //System.out.println(outString);
          theAppLog.warning(summaryIDLineString, detailsString);
          }
      return thresholdExceededB; 
      }

  @Override
  public void run() 
    /* This method periodically tests whether an EDT dispatch
      has taken too long and reports it if so.
      The report includes a stack trace of the AWT-EventQueue thread.
      */
    {
      theAppLog.info( "daemon run() starting." );
      while (true) { // Repeat periodic tests.
        long currTime = System.currentTimeMillis();
        synchronized (this) {
          for (Map.Entry<AWTEvent, EventValue> entry : this.eventTimeMap
              .entrySet()) {
            AWTEvent event = entry.getKey();
            if (entry.getValue() == null) // Skipping if no entry.
              continue;
            if  // Skipping if this entry reported earlier.
              (entry.getValue().reportedB)
              continue;
            long startTime = entry.getValue().startTimeL;
            boolean thresholdExceededB= // Displaying if too long.
                this.checkEventTimeB(
                    "underway",event, currTime, startTime
                    );
            if  // Displaying stack also if too long.
              ( thresholdExceededB )
              {
                displayStackTraceV();
                entry.getValue().reportedB= true; // Recording output.
                }

          }
        }
        try { Thread.sleep(PERIOD); // Waiting for the sample time.   
          } 
        catch (InterruptedException ie) { }
      }
    }

  private void displayStackTraceV()
    {
      if ( displayStackB ) // Displaying stack if enabled.
        {
          ThreadMXBean threadBean= 
              ManagementFactory.getThreadMXBean();
          long threadIds[] = threadBean.getAllThreadIds();
          for (long threadId : threadIds) {
             ThreadInfo threadInfo = threadBean.getThreadInfo(threadId,
                   Integer.MAX_VALUE);
             if (threadInfo.getThreadName().startsWith("AWT-EventQueue")) {
                //System.out.println(
                theAppLog.warning(
                     threadInfo.getThreadName() + " / "
                     + threadInfo.getThreadState()
                     );
                if ( eventDispatchingEndingB ) 
                  theAppLog.warning("Dispatch already ended.");
                  else
                  { // Display stack.
                    theAppLog.warning("Begin Stack Trace.");
                    // /* ?? Disable stack trace logging.
                    StackTraceElement[] stack = threadInfo.getStackTrace();
                    for (StackTraceElement stackEntry : stack) {
                       //System.out.println(
                       theAppLog.warning("\t" + stackEntry.getClassName()
                       + "." + stackEntry.getMethodName() + " ["
                       + stackEntry.getLineNumber() + "]");
                    }
                    theAppLog.warning("End Stack Trace.");
                    // ?? Disable stack trace logging. */
                    }
             }
          }
        }
      }

  } // TracingEventQueueMonitor
