package allClasses;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.swing.JFrame;

import allClasses.javafx.JavaFXGUI;
import javafx.application.Platform;

import static allClasses.AppLog.theAppLog;  // For appLogger;


/* This file contains the following classes:
 * * InnerApp : inner app sequencer.
 *   * InstanceCreationRunnable : Listener for signals about other
 *     app instances
 *     //////org Simplify this, and maybe eliminate this nested class.
 * 
 * The following were eliminated:
 * * PlatformUI : contains [Swing] font control, merged into SwingUI.
 * * SwingUI : moved to its own file.
 */


public class InnerApp

  { // class InnerApp

    /* 
     * This class contains the sequencer for the InnerApp.
     * This includes the initialization and finalization of 
     * the 2 main user interfaces:
     * * a Swing UI
     * * a JavaFX UI, some of it, not all of it.
     *   By this time, the the JavaFX runtime startup has already occurred
     *   in case it is needed to report early errors.
     * Eventually the Swing UI might be eliminated.
     * 
     * For more information about the sequencer, see the runV(.) method.
     */
  
  
    // InnerApp's constructor injected dependency variables.
  
    private EpiThread theConnectionManagerEpiThread;
    private EpiThread theCPUMonitorEpiThread;
    private DataTreeModel theDataTreeModel;
    private DataNode theInitialRootDataNode;
    private SwingUI theSwingUI;
    private Shutdowner theShutdowner;
    private TCPCopier theTCPCopier;
    private ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;
    private AppInstanceManager theAppInstanceManager;
    private ConnectionManager theConnectionManager;
    private DataRoot theDataRoot;
    private JavaFXGUI theJavaFXGUI;
  
  
    public InnerApp(   // Constructor.
        EpiThread theConnectionManagerEpiThread,
        EpiThread theCPUMonitorEpiThread,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode,
        SwingUI theSwingUI,
        Shutdowner theShutdowner,
        TCPCopier theTCPCopier,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor,
        AppInstanceManager theAppInstanceManager,
        ConnectionManager theConnectionManager,
        DataRoot theDataRoot,
        JavaFXGUI theJavaFXGUI
        )
      {
        this.theConnectionManagerEpiThread= theConnectionManagerEpiThread;
        this.theCPUMonitorEpiThread= theCPUMonitorEpiThread;
        this.theDataTreeModel= theDataTreeModel;
        this.theInitialRootDataNode= theInitialRootDataNode;
        this.theSwingUI= theSwingUI;
        this.theShutdowner= theShutdowner;
        this.theTCPCopier= theTCPCopier;
        this.theScheduledThreadPoolExecutor= theScheduledThreadPoolExecutor;
        this.theAppInstanceManager= theAppInstanceManager;
        this.theConnectionManager= theConnectionManager;
        this.theDataRoot= theDataRoot;
        this.theJavaFXGUI= theJavaFXGUI;
        }
  
  
    class InstanceCreationRunnable // Listens for other local app instances.
  
      implements AppInstanceListener, Runnable
  
      {
        /* This nested class does 2 things:
            
          * It acts as an AppInstanceListener because of
            its appInstanceCreatedV() method,
            which calls the invokeLater(..) method.
          
          * It contains a run() method for executing code on the AWT thread.
    
          Because this class is a non-static nested class,
          it can do anything its parent, the AppInstanceManager, can do.
          
          Presently the only thing the run() method does is
          try to move the app's main JFrame to the front and display a dialog.
          Other actions might be appropriate if the UI were different, 
          such as there being multiple tabs and/or multiple JFrames.
    
          ///enh
          Note, move-to-front is problematic.
          In some Windows versions, move-to-front works only once,
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
  
        private JFrame theJFrame; // The app's main Swing window. 
  
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
            theAppLog.info("Trying Swing app window move-to-front.");
            theJFrame.toFront();
            theJFrame.repaint();
            }
  
        } // class InstanceCreationRunnable
  
    public void runV() // Completes initialization, runs, and finalizes.
  
      /* This method is the InnerApp sequencer.
       * It does 3 things:
       * 
       * * Finishes InnerApp initialization and startup.
       * * Waits for a shutdown request, which can come from various places.
       * * Does shutdown and finalization of 
       *   the things it initialized and started earlier.
       *
       * It does not return until it has finished all of the above.
       */
  
      {
        theAppLog.info("InnerApp.runV() begins.");
        theDataTreeModel.initializeV( theInitialRootDataNode );
        initializeUIV();
        theConnectionManagerEpiThread.startV();
        theCPUMonitorEpiThread.startV();
        theTCPCopier.initializeV();
  
        /// theAppLog.error("InnerApp.runV() Test Anomaly Dialog.");
        
        // At this point, full interaction is possible
        // with the user and with other network devices.
        doPollingJobsWhileWaitingForShutdownV();
  
        // At this point, shutdown has been requested.
        theTCPCopier.finalizeV();
        theCPUMonitorEpiThread.stopAndJoinV();
        theDataTreeModel.logListenersV(); ///dbg
        theConnectionManagerEpiThread.stopAndJoinV( );
        theScheduledThreadPoolExecutor.shutdownNow(); // Terminate pool threads.
        finalizeUIV();
        theDataTreeModel.finalizeV();
        theAppLog.info("InnerApp.runV() ends.");
        }

    private void initializeUIV()
      /* This method finishes initialization of the UI.
       * It does both the Swing UI and the JavaFX UI.  
       * It is called during inner app startup.
       * It switches to the appropriate threads as needed.
       */
      {
        theAppLog.info("InnerApp.initializeUIV() called.");

        // Start JavaFX UI.  
        theJavaFXGUI.startJavaFXLaunchV(); // Start thread that presents
          // JavaFX GUI window.
        // This must be done before Swing is initialized.
        ///fix? Might need to wait, either here, or in the above method,
        /// to guarantee that JavaFX has completed initialization
        /// before continuing?  What would be nice to have and use
        /// an equivalent to Swing's runAndWait(.) for this.

        theSwingUI.initializeV(); // Start the Swing UI.

        // appLogger.info("InnerApp.initializeUIV() exiting.");
        }

    public void finalizeUIV()
      /* This method does finalization of the Swing GUI and JavaFXGUI.  
       * It is called during app shutdown.
       * It switches to the appropriate threads to do its work.
       */
      {
        theAppLog.info("InnerApp.finalizeUIV() called.");

        theSwingUI.finalizeV();

        Platform.runLater( () -> theJavaFXGUI.finalizeV() );

        // appLogger.info("InnerApp.finalizeUIV() ends.");
        }
  
    private void doPollingJobsWhileWaitingForShutdownV()
      /* This method polls and processes 
       * some things that need to polled until termination is requested.  
       * Then it returns.
       * The request to terminate might or might not come from a polled job.
       * 
       * ///org Use absolute instead of relative timing in wait.
       */
      {
        while (true) {
          // theAppLog.debug(
          //     "InnerApp.doPollingTasksWhileWaitingForShutdownV()() loop.");
  
          boolean terminationRequestedB= // Wait while testing for termination. 
              theShutdowner.waitForTimeOutOf1OrTerminationB(
                  1000); // 1-second polling interval.
  
          if (terminationRequestedB) break; // Exit if termination requested.
  
          doSomePollingJobsV();
          }
        }
  
    private void doSomePollingJobsV()
      /* This method tries to do various polling jobs during each call.
       * It does at least one job, then exits.
       * It always displays changes that affect the user interface,
       * both Swing and JavaFX.
       * It may then do one additional job that is ready to be done,
       * the first job that is ready from the following list:
       * * a executable file update using the AppInstanceManager
       * * a data importation operation using the ConnectionManager
       * 
       * ///enh Add job: display to JavaFX UI.  Now it does only the Swing UI.
       */
      {
        toReturn: {
          try { // Display state changes that affect the UI.
              DataNode.displayPossiblyChangedNodesFromV(
                theDataRoot.getParentOfRootTreePath( ), 
                theDataRoot.getRootDataNode( ),
                theDataRoot.getRootEpiTreeItem()
                );
            } catch (Exception theException) {
              theAppLog.exception("InnerApp.doSomePollingJobsV()",theException);
            }
          if (theAppInstanceManager.tryToStartUpdateB())
            break toReturn; // Exit immediately to continue the update.
          theConnectionManager.tryProcessingImportDataB();
        } // goReturn:
      }
  
    } // class InnerApp end
