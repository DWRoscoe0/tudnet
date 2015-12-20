package allClasses;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

import static allClasses.Globals.appLogger;  // For appLogger;

public class AppGUI

  /* This class is the top level of the app GUI.
    Presently is manages only a single JFrame window
    with a DagBrowserPanel as its content.
    Later it might manage multiple JFrames with different contents.
    */

  { // class AppGUI

    // AppGUI's constructor injected dependency variables.
    private EpiThread theConnectionManagerEpiThread;
    private EpiThread theCPUMonitorEpiThread;
    private DataTreeModel theDataTreeModel;
    private DataNode theInitialRootDataNode;
    private LockAndSignal theGUILockAndSignal;
    private GUIDefiner theGUIDefiner;
    private Shutdowner theShutdowner;

    public AppGUI(   // Constructor.
        EpiThread theConnectionManagerEpiThread,
        EpiThread theCPUMonitorEpiThread,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode,
        LockAndSignal theGUILockAndSignal,
        GUIDefiner theGUIDefiner,
        Shutdowner theShutdowner
        )
      {
	      this.theConnectionManagerEpiThread= theConnectionManagerEpiThread;
	      this.theCPUMonitorEpiThread= theCPUMonitorEpiThread;
        this.theDataTreeModel= theDataTreeModel;
        this.theInitialRootDataNode= theInitialRootDataNode;
        this.theGUILockAndSignal= theGUILockAndSignal;
        this.theGUIDefiner= theGUIDefiner;
        this.theShutdowner= theShutdowner;
        }

    public static class GUIDefiner // This EDT Runnable starts the GUI. 
      implements Runnable

      /* This nested class is used to create and start the app's GUI.
        It's run() method runs in the EDT thread.
        It signals its completion by executing doNotify() on
        the LockAndSignal object passed to it when 
        its instance is constructed.
        */

      { // GUIDefiner

    		// Injected dependency variables.
        private LockAndSignal theGUILockAndSignal;
    		private AppInstanceManager theAppInstanceManager;
    		private DagBrowserPanel theDagBrowserPanel;
    		private AppGUIFactory theAppGUIFactory;
    		private Shutdowner theShutdowner;

        // Other AppGUI instance variables.
        private JFrame theJFrame;  // App's only JFrame (now).

        GUIDefiner(   // Constructor. 
        		LockAndSignal theGUILockAndSignal, 
        		AppInstanceManager theAppInstanceManager,
        		DagBrowserPanel theDagBrowserPanel,
        		AppGUIFactory theAppGUIFactory,
        		Shutdowner theShutdowner
        		)
          {
            this.theGUILockAndSignal=   // Save lock reference.
              theGUILockAndSignal;
        		this.theAppInstanceManager= theAppInstanceManager;
        		this.theDagBrowserPanel= theDagBrowserPanel;
        		this.theAppGUIFactory= theAppGUIFactory;
        		this.theShutdowner= theShutdowner;
            }

    		public class TracingEventQueue extends EventQueue {

    			/* This class was gotten from an article at
    			  https://today.java.net/pub/a/today/2007/08/30/debugging-swing.html
    			  Its purpose is to identify when the EDT is taking too long
    			  to process events.
    			 	*/

    		   private TracingEventQueueMonitor theTracingEventQueueMonitor;

    		   public TracingEventQueue() {
   		       //this.theTracingEventQueueMonitor= 
   		  	   //  new TracingEventQueueMonitor(500);
   		  	   this.theTracingEventQueueMonitor= 
   		  	  		 new TracingEventQueueMonitor(TracingEventQueueMonitor.LIMIT);
   		       this.theTracingEventQueueMonitor.start();
    		   }

    		   @Override
    		   protected void dispatchEvent(AWTEvent event) {
    		     this.theTracingEventQueueMonitor.eventDispatchingBeginningV(event);
    		     super.dispatchEvent(event);
    		     this.theTracingEventQueueMonitor.eventDispatchingEndingV(event);
    		     }
    		} // TracingEventQueue

    		class TracingEventQueueMonitor extends Thread {

    			/* This class was gotten from an article at
    			  https://today.java.net/pub/a/today/2007/08/30/debugging-swing.html
    			  Its purpose is to help TracingEventQueue 
    			  identify when the EDT is taking too long to process events.
    			  It displays the elapsed time and the stack
    			  the first time it notices elapsed time has exceeded the threshold.
    			  The Thread.sleep(..) time defines the sampling rate.
    			  It might need to be adjusted to locate hogging code.
		     	  */

    			private long thresholdDelay;

    			private final static long PERIOD= 100;  // was 100
    			private final static long LIMIT= 500; // was 500
    			private final static boolean displayStackB= false;
    			
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
    				super("EDTMonitor");
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
	              appLogger.warning(outString);
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
	    				while (true) {
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
		                	  appLogger.warning(
		  	                	   threadInfo.getThreadName() + " / "
		  	                     + threadInfo.getThreadState()
		  	                     );
		          					if ( eventDispatchingEndingB ) 
			                	  appLogger.warning("Dispatch already ended.");
			                	  else
			                	  { // Display stack.
		  	                	  appLogger.warning("Begin Stack Trace.");
		  	                	  // /* ?? Disable stack trace logging.
		  	                    StackTraceElement[] stack = threadInfo.getStackTrace();
		  	                    for (StackTraceElement stackEntry : stack) {
		  	                       //System.out.println(
		  	                    	 appLogger.warning("\t" + stackEntry.getClassName()
		  	                       + "." + stackEntry.getMethodName() + " ["
		  	                       + stackEntry.getLineNumber() + "]");
		  	                    }
		  	                	  appLogger.warning("End Stack Trace.");
		  	                	  // ?? Disable stack trace logging. */
			          				  	}
		                 }
		              }
			    			}
	    			  }

    		} // TracingEventQueueMonitor

        public void run()
          /* This method builds the app's GUI in a new JFrame 
            and starts it.
            This method is run from the AWT thread after startingBrowserGUIV() 
            calls invokeLater(..) because AWT GUI code is not thread-safe.
            */
          {
	        	Toolkit.getDefaultToolkit().getSystemEventQueue().push(
	        	    new TracingEventQueue()); // For monitoring dispatch times.

        	  //try { // Change GUI look-and-feel to be OS instead of java.
            //  UIManager.setLookAndFeel(UIManager.
            //    getSystemLookAndFeelClassName());
            //  } catch(Exception e) {}
	        	
        		theDagBrowserPanel.initializingV(); // (post-construction).

        		theJFrame =  // construct and start the app JFrame.
        				startingJFrame();

            theAppInstanceManager.setAppInstanceListener(  // App instance events...
              theAppGUIFactory.makeInstanceCreationRunnable(theJFrame)
              );

            //appLogger.info("GUI start-up complete.");
            theGUILockAndSignal.doNotifyV();  // Signal that starting is done.
            }

        private JFrame startingJFrame()
          /* This method creates the app's JFrame and starts it.
            It is meant to be run on the EDT (Event Dispatching Thread).
            The JFrame content is set to a DagBrowserPanel 
            which contains the GUI and other code which does most of the work.
            It returns the JFrame.  
            */
          {
            JFrame theJFrame =  // Make the main application JFrame.
              theAppGUIFactory.makeJFrame( 
                AppName.getAppNameString()
                +", DAG Browser 7 Test"
                +", archived "
                +theAppInstanceManager.thisAppDateString()
                );
            theJFrame.setContentPane( theDagBrowserPanel );  // Store content.
            theJFrame.pack();  // Layout all the content's sub-panels.
            theJFrame.setLocationRelativeTo(null);  // Center JFrame on screen.
            theJFrame.setDefaultCloseOperation( // Set the close operation to be
              JFrame.DO_NOTHING_ON_CLOSE // nothing, so listener can handle. 
              );
            theJFrame.addWindowListener( // Set Listener to handle close.
	            new WindowAdapter() {
	              public void windowClosing(WindowEvent e) {
	                appLogger.info("windowClosing(..), will request shutdown.");
	                theShutdowner.requestAppShutdownV();
	                }
	            	});
            theJFrame.setVisible(true);  // Make the window visible.
            return theJFrame;
            }

        } //GUIDefiner

    class InstanceCreationRunnable
    	implements AppInstanceListener, Runnable
      /* This nested class does 2 things:
        
	      * It contains a run() method which can be used to
	        execute code on the AWT thread using invokeLater(..).
	        
	      * It acts as an AppInstanceListener because of
	        its appInstanceCreatedV() method,
	        which calls the invokeLater(..) method.

				Because this class is a non-static nested class,
				it can do anything its parent, the AppInstanceManager, can do.
				
        Presently the only thing the run() method does is
        to move the app's main JFrame to the front.
        Other actions might be appropriate if the UI were different, 
        such as there being multiple tabs and/or multiple JFrames.

        Note, in some Windows versions, move-to-front works only once,
        or only highlights the app's task-bar button, or both.
        The following code was moving the first time, 
        and highlighting after that.  Now it only highlights.
        This might be part of the focus-stealing arms race.
        Fix this to work in all cases?
        Maybe try using a JFrame method override,
        which is described by the [best] answer, with 18 votes, at
        http://stackoverflow.com/questions/309023/howto-bring-a-java-window-to-the-front

        ?? Eventually this method might also process command arguments
        as part of the software update process as part of
        the AppInstanceManager logic.

        ?? Maybe some of this should be done in DagBrowserPanel?

        */
      {
    		private JFrame theJFrame;

    		InstanceCreationRunnable( JFrame theJFrame )  // Constructor.
	    		{
    				this.theJFrame= theJFrame;
    				
	    			//throw new NullPointerException(); // Un-comment to test this path.
	      		}

        public void appInstanceCreatedV()  // AppInstanceListener method.
          /* This AppInstanceListener method handles 
            the detection of the creation of
            new running instances of this app.
            It is fired by theAppInstanceManager.
            Presently it only queues the execution of 
            the run() method on the AWT thread.
            */
          {
            java.awt.EventQueue.invokeLater( this );
            }
	    	
        public void run() 
          {
            appLogger.info("Trying move-to-front.");
            theJFrame.toFront();
            theJFrame.repaint();

            /* This broke already partially broken move-to-front.
            JOptionPane.showMessageDialog(
              null, // this, // null, 
              "The app is already running.",
              "Info",
              JOptionPane.INFORMATION_MESSAGE
              );
            */
            }

        } // class InstanceCreationRunnable

    private void startingBrowserGUIV()
      /* This method builds and starts the Graphical User Interface (GUI).
        It doesn't return until the GUI has been started.
        This is tricky because GUI operations must be performed
        on a different thread, the AWT thread, and
        we must wait for those operations to complete.
        
        ?? Simplify by using invokeAndWait().
        */
      {
        //appLogger.info("Queuing GUIDefiner.");

        java.awt.EventQueue.invokeLater(  // Queue on GUI (AWT) thread...
        	theGUIDefiner   // ...this Runnable GUIDefiner,...
          );  //  whose run() method will build and start the app's GUI.

        theGUILockAndSignal.doWaitE(); // Wait for signal
          // which means that the GUIDefiner has finished.

        //appLogger.info("GUI/AWT thread signalled GUIDefiner done.");
        }

    public void runV() // This method does the main AppGUI run phase.
      {
        theDataTreeModel.initializeV( theInitialRootDataNode );
        startingBrowserGUIV();  // Building and displaying GUI.
        theConnectionManagerEpiThread.startV();
        theCPUMonitorEpiThread.startV();

        theShutdowner.waitForAppShutdownUnderwayV();

        theDataTreeModel.logListenersV(); // [for debugging]
        // theCPUMonitorEpiThread.stopAndJoinV( ); ?? 
        theConnectionManagerEpiThread.stopAndJoinV( ); 
          // Stopping ConnectionManager thread.
        }

    } // class AppGUI
