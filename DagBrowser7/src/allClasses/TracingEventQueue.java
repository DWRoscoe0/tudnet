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

	/* This simple class was based on one gotten from an article at
	  https://today.java.net/pub/a/today/2007/08/30/debugging-swing.html
	  Its purpose is to identify when the EDT is taking too long
	  to process events.
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
			theTracingEventQueueMonitor.start();
	  	}

  @Override
  protected void dispatchEvent(AWTEvent event) {
    this.theTracingEventQueueMonitor.eventDispatchingBeginningV(event);
    super.dispatchEvent(event);
    this.theTracingEventQueueMonitor.eventDispatchingEndingV(event);
    }
  
  } // TracingEventQueue

class TracingEventQueueMonitor extends Thread {

	/* This class was based on one gotten from an article at
	  https://today.java.net/pub/a/today/2007/08/30/debugging-swing.html
	  Its purpose is to help TracingEventQueue 
	  identify when the EDT is taking too long to process events.
	  It helps in two ways:
	  * It provides methods callable from TracingEventQueue to
	    measure and display dispatch time if it exceeds a threshold.
	  * It provides a thread which does the same threshold test
	    and displays a stack trace if the threshold is exceeded
	    to help identify the CPU-hogging code.
	  The Thread.sleep(..) time defines the sampling rate.
	  It might need to be adjusted to locate CPU-hogging code.
 	  */

	private long thresholdDelay;

	public static final long PERIOD= 100;  // was 100
	public static final long LIMIT= 500; // was 500
	private static final boolean displayStackB= false;
	
	class EventValue { 
		long startTimeL; 
		boolean outputtedB; 
		EventValue(long startTimeL) {
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

	public synchronized void eventDispatchingBeginningV(AWTEvent event)
	  // Processes the beginning of event dispatching by recording in map.
		{
			this.eventTimeMap.put(
					event, new EventValue(System.currentTimeMillis())
					);
		  }

	public void eventDispatchingEndingV(AWTEvent event) 
	  /* Processes the ending of event dispatching by
      doing a time check and removing the associated map entry.
      It does not do a stack trace, which should already have been done.
      */
		{
			eventDispatchingEndingB= true;
		  synchronized(this) {
				this.checkEventTimeB(
						"Total",
						event, 
						System.currentTimeMillis(),
						this.eventTimeMap.get(event).startTimeL);
				this.eventTimeMap.remove(event);
		  	}
			eventDispatchingEndingB= false;
      //appLogger.debug(
      //		"TracingEventQueueMonitor.eventDispatchingEndingV(..)"
      //		);
		  }

	private boolean checkEventTimeB(
			String labelString, AWTEvent event, long currTime, long startTime
			) 
	  /* Reports whether an event dispatch has been running to long,
	    longer that thresholdDelay. 
	    It returns true if it has, false otherwise. 
	    It is called by:
	    * run() when excessive dispatch time is first detected.
	      The EDT stack is displayed at this time also.
	    * eventDispatchingEndingV(..) later to 
	      display total dispatch time.
	    */
		{
			long currProcessingTime = currTime - startTime;
			boolean thresholdExceededB= 
					(currProcessingTime >= this.thresholdDelay);
			if (thresholdExceededB) {
				String outString= "EDT "
						//Event [" + event.hashCode() + "] "
						+ labelString
						+ " "
						+ event.getClass().getName()
						+ " has taken too much time (" + currProcessingTime
						+ ")";
				//System.out.println(outString);
        theAppLog.warning(outString);
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
						if  // Skipping if this entry output earlier.
						  (entry.getValue().outputtedB)
							continue;
						long startTime = entry.getValue().startTimeL;
    				boolean thresholdExceededB= // Displaying if too long.
    						this.checkEventTimeB(
    								"Partial",event, currTime, startTime
    								);
    				if  // Displaying stack also if too long.
    				  ( thresholdExceededB )
	    				{
	    					displayStackTraceV();
	  						entry.getValue().outputtedB= true; // Recording output.
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
