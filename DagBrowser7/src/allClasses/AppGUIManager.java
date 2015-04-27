package allClasses;

import javax.swing.JFrame;

import static allClasses.Globals.appLogger;  // For appLogger;

public class AppGUIManager // Top level of the app's GUI, the window manager.

  /* This class is the top level of the app GUI.
    Presently is manages only a single JFrame window
    with a DagBrowserPanel as its content.
    Later it might manage multiple JFrames with different contents.
    */

  { // class AppGUIManager

    // AppGUIManager's constructor injected dependency variables.
    private EpiThread theConnectionManagerEpiThread;
    private DataTreeModel theDataTreeModel;
    private DataNode theInitialRootDataNode;
    private TerminationShutdownThread theTerminationShutdownThread;
    private LockAndSignal theGUILockAndSignal;
    private GUIDefiner theGUIDefiner;

    public AppGUIManager(   // Constructor.
        EpiThread theConnectionManagerEpiThread,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode,
        TerminationShutdownThread theTerminationShutdownThread,
        LockAndSignal theGUILockAndSignal,
        GUIDefiner theGUIDefiner
        )
      {
        this.theConnectionManagerEpiThread= theConnectionManagerEpiThread;
        this.theDataTreeModel= theDataTreeModel;
        this.theInitialRootDataNode= theInitialRootDataNode;
        this.theTerminationShutdownThread= theTerminationShutdownThread;
        this.theGUILockAndSignal= theGUILockAndSignal;
        this.theGUIDefiner= theGUIDefiner;
        }

    public static class GUIDefiner // This Runnable starts the GUI in the AWT thread.
      implements Runnable

      /* This nested class is used to create and start the app's GUI.
        It's run() method runs in the AWT thread.
        It signals its completion by executing doNotify() on
        the LockAndSignal object passed to it when 
        its instance is constructed.
        */

      { // GUIDefiner

    		// Injected dependency variables.
        LockAndSignal theGUILockAndSignal;
    		private AppInstanceManager theAppInstanceManager;
    		private DagBrowserPanel theDagBrowserPanel;
    		private AppGUIFactory theAppGUIFactory;

        // Other AppGUIManager instance variables.
        private JFrame theJFrame;  // App's only JFrame (now).
        
        GUIDefiner(   // Constructor. 
        		LockAndSignal theGUILockAndSignal, 
        		AppInstanceManager theAppInstanceManager,
        		DagBrowserPanel theDagBrowserPanel,
        		AppGUIFactory theAppGUIFactory
        		)
          {
            this.theGUILockAndSignal=   // Save lock reference.
              theGUILockAndSignal;
        		this.theAppInstanceManager= theAppInstanceManager;
        		this.theDagBrowserPanel= theDagBrowserPanel;
        		this.theAppGUIFactory= theAppGUIFactory;
            }

        public void run()
          /* This method builds the app's GUI in a new JFrame 
            and starts it.
            This method is run from the AWT thread after startingBrowserGUIV() 
            calls invokeLater(..) because AWT GUI code is not thread-safe.
            */
          {
            //try { // Change GUI look-and-feel to be OS instead of java.
            //  UIManager.setLookAndFeel(UIManager.
            //    getSystemLookAndFeelClassName());
            //  } catch(Exception e) {}

        		theJFrame =  // construct and start the app JFrame.
        				startingJFrame();

            theAppInstanceManager.setAppInstanceListener(  // App instance events...
              theAppGUIFactory.makeInstanceCreationRunnable(theJFrame)
              );

            appLogger.info("GUI start-up complete.");
            theGUILockAndSignal.doNotifyV();  // Signal that starting is done.
            }

        private JFrame startingJFrame()
          /* This method creates the app's JFrame and starts it.
            It is meant to be run on the UI (AWT) thread.
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
            theDagBrowserPanel.initializingV(); // Initializing post-construction.
            theJFrame.setContentPane( theDagBrowserPanel );  // Store content.
            theJFrame.pack();  // Layout all the content's sub-panels.
            theJFrame.setLocationRelativeTo(null);  // Center JFrame on screen.
            theJFrame.setDefaultCloseOperation(  // Set the close operation...
              JFrame.EXIT_ON_CLOSE );  // ...to be exit, since it's the only frame.
            theJFrame.setVisible(true);  // Make the app visible.
            return theJFrame;
            }

        } //GUIDefiner
      
    static class TerminationShutdownThread  // For terminating main() thread.
      extends Thread
      /* This nested shutdown hook Thread class's run() method
        requests that the main thread finalize and terminate.
        It does this by setting the main thread's interrupt() flag.
        After the main thread finishes its finalization and terminates,
        this shutdown hook thread terminates also,
        eventually allowing the entire app to terminate.
        */
      { // TerminationShutdownThread
        private Thread mainThread;  // Other thread to terminate.

        public TerminationShutdownThread(Thread mainThread) // Constructor.
          { this.mainThread= mainThread; }
        
        public void run()
          {
            mainThread.interrupt();  // Request termination of main thread.

            try {  // Wait for main thread to terminate.
              mainThread.join();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }

            // At this point, main thread has been terminated.
            }

        } // TerminationShutdownThread

    private void awaitingShutdownV()  // While interacting with user.
      // This method blocks until shutdown is underway..
      {
        appLogger.info("AppGUIManager.awaitingShutdownV(): begining wait.");

        while  // Sleeping in a loop until notification by interrupt().
          (! Thread.interrupted() )  // Test and clear thread interrupt flag.
          try { // Block for 30 seconds or end early if interrupt() occurs.
              Thread.sleep(30*1000);  // Sleep 30 seconds.  Any amount works.
            } catch (InterruptedException anInterruptedException) {
              Thread.currentThread().interrupt();  // Re-interrupt for exit.
            }

        appLogger.info("AppGUIManager.awaitingShutdownV(): ending wait.");

        // At this point shutdown is underway.
        
        }

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

        ??? Eventually this method might also process command arguments
        as part of the software update process as part of
        the AppInstanceManager logic.

        ??? Maybe some of this should be done in DagBrowserPanel?

        */
      {
    		private JFrame theJFrame;

    		InstanceCreationRunnable ( JFrame theJFrame )
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

        } // InstanceCreationRunnable

    private void startingBrowserGUIV()
      /* This method builds and starts the Graphical User Interface (GUI).
        It doesn't return until the GUI has been started.
        This is tricky because GUI operations must be performed
        on a different thread, the AWT thread, and
        we must wait for those operations to complete.
        */
      {
        appLogger.info("Queuing GUIDefiner.");

        java.awt.EventQueue.invokeLater(  // Queue on GUI (AWT) thread...
        	theGUIDefiner   // ...this Runnable GUIDefiner,...
          );  //  whose run() method will build and start the app's GUI.

        theGUILockAndSignal.doWaitE(); // Wait for signal
          // which means that the GUIDefiner has finished.
        
        
        appLogger.info("GUI/AWT thread signalled GUIDefiner done.");
        }

    public void runV() // This method does the main AppGUIManager run phase.
      {
        theDataTreeModel.initializeV( theInitialRootDataNode );

        startingBrowserGUIV();  // Building and displaying GUI.

        Runtime.getRuntime().addShutdownHook(
        		theTerminationShutdownThread
        		); // Adding...
          // ...it to Runtime to be run at shut-down time.

        theConnectionManagerEpiThread.start( );
          // Starting ConnectionManager thread.

        awaitingShutdownV();  // Interacting with user via GUI until shutdown.

        theDataTreeModel.logListenersV();

        theConnectionManagerEpiThread.stopAndJoinV( ); 
          // Stopping ConnectionManager thread.
        }

    } // class AppGUIManager
