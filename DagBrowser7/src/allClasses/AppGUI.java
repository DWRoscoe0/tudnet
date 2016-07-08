package allClasses;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

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
    		private TracingEventQueue theTracingEventQueue;
      	private BackgroundEventQueue theBackgroundEventQueue;

        // Other AppGUI instance variables.
        private JFrame theJFrame;  // App's only JFrame (now).

        GUIDefiner(   // Constructor. 
        		LockAndSignal theGUILockAndSignal, 
        		AppInstanceManager theAppInstanceManager,
        		DagBrowserPanel theDagBrowserPanel,
        		AppGUIFactory theAppGUIFactory,
        		Shutdowner theShutdowner,
        		TracingEventQueue theTracingEventQueue,
	        	BackgroundEventQueue theBackgroundEventQueue
        		)
          {
            this.theGUILockAndSignal=   // Save lock reference.
              theGUILockAndSignal;
        		this.theAppInstanceManager= theAppInstanceManager;
        		this.theDagBrowserPanel= theDagBrowserPanel;
        		this.theAppGUIFactory= theAppGUIFactory;
        		this.theShutdowner= theShutdowner;
        		this.theTracingEventQueue= theTracingEventQueue;
	        	this.theBackgroundEventQueue= theBackgroundEventQueue;
            }

        public void run() // GUIDefiner.
          /* This method builds the app's GUI in a new JFrame and starts it.
            This method is run on the AWT thread by startingBrowserGUIV() 
            because AWT GUI code is not thread-safe.
            */
          {
        		theTracingEventQueue.initializeV(); // to start monitor thread.

	        	Toolkit.getDefaultToolkit().getSystemEventQueue().push(
	        			theTracingEventQueue
	        			); // For monitoring dispatch times.
	        	Toolkit.getDefaultToolkit().getSystemEventQueue().push(
	        			theBackgroundEventQueue
	        			); // For doing low-priority window creation.

        	  //try { // Change GUI look-and-feel to be OS instead of java.
            //  UIManager.setLookAndFeel(UIManager.
            //    getSystemLookAndFeelClassName());
            //  } catch(Exception e) {}
	        	
        		theDagBrowserPanel.initializingV();

        		theJFrame =  // Construct and start the app JFrame.
        				startingJFrame();

            theAppInstanceManager.setAppInstanceListener(
              theAppGUIFactory.makeInstanceCreationRunnable(theJFrame)
              ); // For dealing with other running app instances.

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
                +", version "
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
            appLogger.info(
              	"GUIDefiner.theJFrame.setVisible(true) done."
              	);
            theDagBrowserPanel.restoreFocusV(); // Setting initial focus.
            return theJFrame;
            }

        } //GUIDefiner

    class InstanceCreationRunnable
    	implements AppInstanceListener, Runnable
      /* This nested class does 2 things:
	        
	      * It acts as an AppInstanceListener because of
	        its appInstanceCreatedV() method,
	        which calls the invokeLater(..) method.
        
	      * It contains a run() method for executing code on the AWT thread.

				Because this class is a non-static nested class,
				it can do anything its parent, the AppInstanceManager, can do.
				
        Presently the only thing the run() method does is
        try to move the app's main JFrame to the front.
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

        public void run() // Method executed by appInstanceCreatedV(). 
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
        This is tricky because:
        * GUI operations must be performed on a different thread, 
          the AWT thread.  EventQueue.invokeLater(..) is used to do this.
        * We must wait for those operations to complete before returning.
        
        ?? Simplify by using invokeAndWait(..) instead of 
        invokeLater(..) and doWaitE().
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

    public void runV() // This method does the main AppGUI run phase.
      {
        theDataTreeModel.initializeV( theInitialRootDataNode );
        startingBrowserGUIV();  // Building and displaying GUI on AWT thread.
        theConnectionManagerEpiThread.startV();
        theCPUMonitorEpiThread.startV();

        // Now the app is running and interacting with the user.
        theShutdowner.waitForAppShutdownUnderwayV();
        // Now the app is shutting down.
        
        theDataTreeModel.logListenersV(); // [for debugging]
        // theCPUMonitorEpiThread.stopAndJoinV( ); ?? 
        theConnectionManagerEpiThread.stopAndJoinV( ); 
          // Stopping ConnectionManager thread, ending all connections.
        }

    } // class AppGUI
