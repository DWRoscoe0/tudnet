package allClasses;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import allClasses.javafx.JavaFXGUI;
import javafx.application.Platform;

import static allClasses.AppLog.theAppLog;  // For appLogger;

public class AppGUI

  { // class AppGUI

    /* This class manages SOME OF the app's Graphical User Interface.
  
      ///org The name of this class, AppGUI, is not appropriate because:
      1. Though this class includes elements of the app's GUI, 
        it includes non-GUI elements also.
      2. Since the addition of JavaFX, some GUI interaction 
        can occur before this class becomes active.
      The GUI presently includes elements of both 
      the Java Swing and JavaFX libraries.
      A transition from Swing to JavaFX is underway.
      Eventually the Swing elements might be eliminated.
  
      For more information, see the runV(.) method.
      */
  
  
    // AppGUI's constructor injected dependency variables.
  
    private EpiThread theConnectionManagerEpiThread;
    private EpiThread theCPUMonitorEpiThread;
    private DataTreeModel theDataTreeModel;
    private DataNode theInitialRootDataNode;
    private GUIManager theGUIManager;
    private Shutdowner theShutdowner;
    private TCPCopier theTCPCopier;
    private ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;
    private AppInstanceManager theAppInstanceManager;
    private ConnectionManager theConnectionManager;
    private DataRoot theDataRoot;
  
  
    public AppGUI(   // Constructor.
        EpiThread theConnectionManagerEpiThread,
        EpiThread theCPUMonitorEpiThread,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode,
        GUIManager theGUIManager,
        Shutdowner theShutdowner,
        TCPCopier theTCPCopier,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor,
        AppInstanceManager theAppInstanceManager,
        ConnectionManager theConnectionManager,
        DataRoot theDataRoot
        )
      {
        this.theConnectionManagerEpiThread= theConnectionManagerEpiThread;
        this.theCPUMonitorEpiThread= theCPUMonitorEpiThread;
        this.theDataTreeModel= theDataTreeModel;
        this.theInitialRootDataNode= theInitialRootDataNode;
        this.theGUIManager= theGUIManager;
        this.theShutdowner= theShutdowner;
        this.theTCPCopier= theTCPCopier;
        this.theScheduledThreadPoolExecutor= theScheduledThreadPoolExecutor;
        this.theAppInstanceManager= theAppInstanceManager;
        this.theConnectionManager= theConnectionManager;
        this.theDataRoot= theDataRoot;
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
            theAppLog.info("Trying move-to-front.");
            theJFrame.toFront();
            theJFrame.repaint();
            }
  
        } // class InstanceCreationRunnable
  
    public void runV() // Completes initialization, runs, and finalizes.
  
      /* This method does the main AppGUI run phase.
       * It does 3 things:
       * 
       * * Finishes app initialization and startup, but not only the GUI.
       * * Waits for a shutdown request, which can come from various places.
       * * Does shutdown and finalization of 
       *   the things it initialized and started earlier.
       *
       * It does not return until it has finished all of these things.
       */
  
      {
    		theAppLog.info("AppGUI.runV() begins.");
        theDataTreeModel.initializeV( theInitialRootDataNode );
        theGUIManager.initializeV(); // Start the GUI.
        theConnectionManagerEpiThread.startV();
        theCPUMonitorEpiThread.startV();
        theTCPCopier.initializeV();
  
        /// theAppLog.error("AppGUI.runV() Test Anomaly Dialog.");
        
        // At this point, full interaction is possible
        // with the user and with other network devices.
        doPollingJobsWhileWaitingForShutdownV();
  
        // At this point, shutdown has been requested.
        theTCPCopier.finalizeV();
        theCPUMonitorEpiThread.stopAndJoinV();
        theDataTreeModel.logListenersV(); ///dbg
        theConnectionManagerEpiThread.stopAndJoinV( );
        theScheduledThreadPoolExecutor.shutdownNow(); // Terminate pool threads.
        theGUIManager.finalizeV(); // Stop the GUI.
        theDataTreeModel.finalizeV();
    		theAppLog.info("AppGUI.runV() ends.");
        }
  
    private void doPollingJobsWhileWaitingForShutdownV()
      /* This method polls some things that need to polled until
       * termination is requested.  Then it returns.
       * The request to terminate might or might not come from a polled job.
       * 
       * ///org Use absolute instead of relative timing in wait.
       */
      {
        while (true) {
          // theAppLog.debug(
          //     "GUIManager.doPollingTasksWhileWaitingForShutdownV()() loop.");
  
          boolean terminationRequestedB= // Wait while testing for termination. 
              theShutdowner.waitForTimeOutOf1OrTerminationB(
                  1000); // 1-second polling interval.
  
          if (terminationRequestedB) break; // Exit if termination requested.
  
          doPollingJobsV();
          }
        }
  
    private void doPollingJobsV()
      /* This method tries to do various polling jobs during each call.
       * It does at least one job, then exits.
       * It always displays changes that affect the Swing UI.
       * It may then do one additional job that is ready to be done,
       * the first job that is ready from the following list:
       * * a executable file update using the AppInstanceManager
       * * a data importation operation using the ConnectionManager
       * 
       * ///enh Add job: display to JavaFX UI.  Now it does only the Swing UI.
       */
      {
        toReturn: {
          try { // Display state changes that affect the Swing UI.
              EDTUtilities.runOrInvokeAndWaitV( () -> { // Do on EDT thread. 
                DataNode.displayChangedNodesFromV( // Display from...
                  theDataRoot.getParentOfRootTreePath( ), 
                  theDataRoot.getRootDataNode( ),
                  theDataRoot.getRootEpiTreeItem()
                  );
                });
            } catch (Exception theException) {
              theAppLog.info("!!!doPollingJobsV().");
              
            }
          if (theAppInstanceManager.tryToStartUpdateB())
            break toReturn; // Exit immediately to continue the update.
          theConnectionManager.tryProcessingImportDataB();
        } // goReturn:
      }
  
    } // class AppGUI end


class GUIManager

  implements KeyEventDispatcher 

  { // GUIManager

    /* This nested class is used to manage the app's GUI.
     * Originally this included only a Swing GUI.
     * Later a JavaFX GUI component was added. 
     */

  
		// Injected dependency variables.
		private AppInstanceManager theAppInstanceManager;
		private DagBrowserPanel theDagBrowserPanel;
		private AppFactory theAppFactory;
		private Shutdowner theShutdowner;
		private TracingEventQueue theTracingEventQueue;

    // Other AppGUI instance variables.
    private JFrame theJFrame;  // App's only JFrame (now).
    private JavaFXGUI theJavaFXGUI;


    GUIManager(   // Constructor. 
    		AppInstanceManager theAppInstanceManager,
    		DagBrowserPanel theDagBrowserPanel,
    		AppFactory theAppFactory,
    		Shutdowner theShutdowner,
    		TracingEventQueue theTracingEventQueue,
    		JavaFXGUI theJavaFXGUI
  		)
      {
    		this.theAppInstanceManager= theAppInstanceManager;
    		this.theDagBrowserPanel= theDagBrowserPanel;
    		this.theAppFactory= theAppFactory;
    		this.theShutdowner= theShutdowner;
    		this.theTracingEventQueue= theTracingEventQueue;
    		this.theJavaFXGUI= theJavaFXGUI;
        }

    public void initializeV()
      /* This method does the GUI initialization that 
        could not be done with constructor dependency injection.
        It does it for both the Swing and JavaFX GUIs.
        
        It initializes JavaFX first, but this is no longer necessary,
        because the JavaFX runtime was started earlier manually
        for use in delivering message dialogs to the user.
        */
      {
        // Start JavaFX GUI.
        theJavaFXGUI.startJavaFXLaunchV(); // Start thread that presents
          // JavaFX GUI window.

        ///fix? Might need to wait, either here, or in the above method,
        /// to guarantee that JavaFX has completed initialization
        /// before continuing?  What would be nice to have and use
        /// an equivalent to Swing's runAndWait(.) for this.

        // Start Swing GUI.
        EDTUtilities.invokeAndWaitV( // Dispatching on EDT
            new Runnable() {
              @Override
              public void run() { initializeOnEDTV(); }  
              } );

        }

    
    // Swing GUI start and stop methods.
    
    public void initializeOnEDTV() // GUIManager.
      /* This method does initialization of the Swing GUI.  
        It must be run on the EDT. 
        It builds the app's GUI in a new JFrame and starts it.
        */
      {
    		theAppLog.info("GUIManager.initializeOnEDTV() begins.");

        theTracingEventQueue.initializeV(); 
          // Start monitor thread is our customized Event dispatcher.
        Toolkit.getDefaultToolkit().getSystemEventQueue().push( // Replace queue
          theTracingEventQueue); // and dispatcher with our customized ones.

    	  //try { // Change GUI look-and-feel to be OS instead of java.
        //  UIManager.setLookAndFeel(UIManager.
        //    getSystemLookAndFeelClassName());
        //  } catch(Exception e) {}

      	PlatformUI.setUIFont( // Select a fixed width font. 
      	  new javax.swing.plaf.FontUIResource(
      			Font.MONOSPACED,Font.PLAIN,12
      			));

    		theDagBrowserPanel.initializeV(); // Initialize main window.

    		buildJFrameV(); // Build and display the app JFrame.

        // theDagBrowserPanel.showCommandHelpV(); // Build and 
    		  // display the Help dialog.

    		// Add KeyListener for app-level keyboard pre-processing.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
          addKeyEventDispatcher( this );

        theAppInstanceManager.setAppInstanceListener(
          theAppFactory.makeInstanceCreationRunnable(theJFrame)
          ); // For dealing with other running app instances.

    		theAppLog.info("GUIManager.initializeOnEDTV() ends.");
        }

    public void finalizeV()
      /* This method does finalization of the Swing GUI and JavaFXGUI.  
       * It is called during app shutdown.
       * It switches to the appropriate threads to do the work.
       */
      {
        theAppLog.info("GUIManager.finalizeV() called, for Swing and JavaFX.");

        EDTUtilities.invokeAndWaitV( // Dispatching on EDT
            new Runnable() {
              @Override
              public void run() { 
                // appLogger.info("GUIManager.finalizeOnV() invokeAndWaitV() run() begins.");
                finalizeOnEDTV();  
                // appLogger.info("GUIManager.finalizeOnV() invokeAndWaitV() run() ends.");
                } } );

        Platform.runLater( () -> theJavaFXGUI.finalizeV() );

        // appLogger.info("GUIManager.finalizeOnV() ends.");
        }
    
    public void finalizeOnEDTV()
      /* This method does finalization.  It must be run on the EDT.
        */
      { 
        theAppLog.debug("GUIManager.finalizeOnEDTV() called.");

        theDagBrowserPanel.finalizationV(); // No longer fails on EDT!
        for // Log and dispose all windows. 
          (Window aWindow : Window.getWindows()) 
          { // Log and dispose one window.
            String windowTypeString= "unknown type of";
            String titleString= "(unknown)";
            if (aWindow instanceof Frame) {
                windowTypeString= "Frame";
                titleString= ((Frame)aWindow).getTitle();
                }
            if (aWindow instanceof JDialog) {
                windowTypeString= "JDialog";
                titleString= ((JDialog)aWindow).getTitle();
                }
            String messageString=
                "GUIManager.finalizeOnEDTV() disposing "
                + windowTypeString
                + " Window titled: "
                + titleString;
            theAppLog.info(messageString);
            aWindow.dispose(); // Do this so Event Dispatch Thread terminates.
            }
        }
    
		public boolean dispatchKeyEvent(KeyEvent theKeyEvent)
		  // Processes KeyEvent keyboard input before being passed to KeyListeners.
			{ 
			  boolean processedKeyB= true;
				int idI= theKeyEvent.getID();
			  int keyI= theKeyEvent.getKeyCode();
			  boolean controlB= theKeyEvent.isControlDown();
			  boolean shiftB= theKeyEvent.isShiftDown();
			  /* ///dbg appLogger.debug( "dispatchKeyEvent(..) "+
				  idI+" "+
				  keyI+" "+
				  keyC+" "+
				  controlB+" "+
				  shiftB+" "+
				  KeyEvent.getKeyModifiersText(theKeyEvent.getModifiers())+" "+
				  KeyEvent.getKeyText(keyI)
				  );
				  */
				{
				  if ( (idI==KeyEvent.KEY_PRESSED) && (keyI == KeyEvent.VK_MINUS) &&
				  		 controlB && !shiftB
				  		 )
			  	  PlatformUI.adjustUIFont( -1); // Make font smaller.
				  if ( (idI==KeyEvent.KEY_PRESSED) && (keyI == KeyEvent.VK_EQUALS) &&
			  		 controlB && shiftB
			  		 )
			  	  PlatformUI.adjustUIFont( +1 ); // Make font bigger.
			  	else
			    	processedKeyB= false;
					}
	      return processedKeyB;
				}

    private void buildJFrameV()
      /* This method creates the app's JFrame.
        It is meant to be run on the EDT (Event Dispatching Thread).
        The JFrame content is set to a DagBrowserPanel 
        which contains the GUI and other code which does most of the work.
        */
      {
        theJFrame=  // Make the main application JFrame.
          theAppFactory.makeJFrame( 
            Config.appString
            +", version "
            +theAppInstanceManager.thisAppDateString()
            );
        theJFrame.setContentPane( theDagBrowserPanel );  // Store content.
        theJFrame.setDefaultCloseOperation( // Set the close operation to be
          JFrame.DO_NOTHING_ON_CLOSE // nothing, so listener can handle it all. 
          );
        theJFrame.addWindowListener( // Set Listener to handle close events
          new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
              theAppLog.info(
             		"windowClosing(..) ======== REQUESTING APP SHUTDOWN =========");
              theShutdowner.requestAppShutdownV();
              }
          	});
        theAppLog.info(
          	"GUIManager.theJFrame.setVisible(true) done."
          	);
        SwingUtilities.invokeLater(new Runnable() { // Queue GUI event...
          @Override  
          public void run() 
            {  
              theDagBrowserPanel.restoreFocusV(); // Setting initial focus.
              }  
          }); /* Done this way because in Java 8 
            Compoent.requestFocusInWindow() will cause 
            NullPointerException before the first dispatched message.
            */
        theJFrame.pack();  // Layout all the content's sub-panels, then
        Dimension screenDimension= Toolkit.getDefaultToolkit().getScreenSize();
        theJFrame.setSize( // Use 3/4 of the screen extent vertically and horizontally.
            (int)(screenDimension.getWidth() * 0.75), 
            (int)(screenDimension.getHeight() * 0.75)
            ); // << not working!!!
        theJFrame.setLocationRelativeTo(null);  // center JFrame on screen.
        theJFrame.setState(Frame.ICONIFIED); // Initially minimize it.
        theJFrame.setVisible(true);  // Make the window visible.
        }

    
    // JavaFX GUI start and stop methods are in other files.  See JavaFXGUI.


  } // GUIManager


class PlatformUI 
	{ // This is where platform UI code goes.

		static int fontSizeI= 12;  // Initial font size. 
		static int minFontSizeI= 3;  // minimum font size. 

	  public static void adjustUIFont(int sizeChangeI)
	    /* This method adjusts the font sizes throughout the app.
	     * It changes the default font for all UI component types,
	     * updates all the apps windows, and redisplays displayed windows.
	     * The font that is used is a fixed width font.
	     * 
	     * ///enh Fix to not change the color of JTextAreas to white
	     * when the Font is adjusted.
	     */
	    {
	  	  fontSizeI+= sizeChangeI; // Calculate tentative new font size.

        // Limit the minimum font size.
	  	  if ( fontSizeI < minFontSizeI ) fontSizeI= minFontSizeI;

      	FontUIResource newFont= new FontUIResource( // Construct new font.
      			Font.MONOSPACED,Font.PLAIN,fontSizeI
      			);

      	PlatformUI.setUIFont( newFont ); // Set the new font.
	      } 

	  public static void setUIFont(javax.swing.plaf.FontUIResource f)
	    /* This method sets the default font for all UI component types,
	     * updates all windows, and redisplays the displayable ones..
	     * 
	     * ///fix components that use default font.
	     * ///doc : Better document variable names.
	     */
	    {
	      Enumeration<Object> keys= // Get keys for all component types. 
	          UIManager.getDefaults().keys();

	      while (keys.hasMoreElements()) { // For every key
	        Object key = keys.nextElement();
	        Object value = UIManager.get (key);
	        if // if key selects a font 
	          (value instanceof javax.swing.plaf.FontUIResource)
	          UIManager.put (key, f); // replace it with new font.
	        }

	      allWindowsUpdateUIV(); // Make default font changes take effect.
	      } 
		
	  public static void allWindowsUpdateUIV()
	    /* This method updates every component's UI from the look and feel
	      including all descendant components, of every app window.
	      Also, any app windows that are visible are redisplayed
	      using the new UI state.
	      */
	    	///doc Better document variable names.
	    {
	    	for (Window theWindow : Window.getWindows()) // Process every window. 
  	    	{ // Process one window.
	    	  
	    	    // Update all components in this window.
  	        SwingUtilities.updateComponentTreeUI(theWindow);

  	        if // Redisplay the window if appropriate. 
  	          ( theWindow.isDisplayable() &&
  	            ( theWindow instanceof Frame 
  	              ? !((Frame)theWindow).isResizable() 
  	              : ( theWindow instanceof Dialog 
    	                ? !((Dialog)theWindow).isResizable() 
    	                : true
    	                )
    	            )
  	            ) 
  	          theWindow.pack(); // Redisplay window.
  	    		}

	      } 
	
		}
