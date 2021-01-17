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
    private GUIManager theGUIManager;
    private Shutdowner theShutdowner;
    private TCPCopier theTCPCopier;
    private ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;

    public AppGUI(   // Constructor.
        EpiThread theConnectionManagerEpiThread,
        EpiThread theCPUMonitorEpiThread,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode,
        GUIManager theGUIManager,
        Shutdowner theShutdowner,
        TCPCopier theTCPCopier,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor
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
        }
    
    class InstanceCreationRunnable // Listens for other local app instances.
    	implements AppInstanceListener, Runnable
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
            theAppLog.info("Trying move-to-front.");
            theJFrame.toFront();
            theJFrame.repaint();
            }

        } // class InstanceCreationRunnable

    public void runV()
      /* This method does the main AppGUI run phase.
       * It does 3 things:
       * 
       * * Finishes app initialization and startup, including the GUI.
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

        theShutdowner.waitForAppShutdownRequestedV();

        theTCPCopier.finalizeV();
        theCPUMonitorEpiThread.stopAndJoinV();
        theDataTreeModel.logListenersV(); ///dbg
        theConnectionManagerEpiThread.stopAndJoinV( );
        theScheduledThreadPoolExecutor.shutdownNow(); // Terminate pool threads.
        theGUIManager.finalizeV(); // Stop the GUI.
        theDataTreeModel.finalizeV();
    		theAppLog.info("AppGUI.runV() ends.");
        }

    } // class AppGUI

class GUIManager 
  implements KeyEventDispatcher 

  /* This nested class is used to manage the app's GUI.
   * Originally this included only a Swing GUI.
   * Later a JavaFX GUI component was added. 
   */

  { // GUIManager

		// Injected dependency variables.
		private AppInstanceManager theAppInstanceManager;
		private DagBrowserPanel theDagBrowserPanel;
		private AppGUIFactory theAppGUIFactory;
		private Shutdowner theShutdowner;
		private TracingEventQueue theTracingEventQueue;

    // Other AppGUI instance variables.
    private JFrame theJFrame;  // App's only JFrame (now).
    private JavaFXGUI theJavaFXGUI;

    GUIManager(   // Constructor. 
    		AppInstanceManager theAppInstanceManager,
    		DagBrowserPanel theDagBrowserPanel,
    		AppGUIFactory theAppGUIFactory,
    		Shutdowner theShutdowner,
    		TracingEventQueue theTracingEventQueue,
    		JavaFXGUI theJavaFXGUI
  		)
      {
    		this.theAppInstanceManager= theAppInstanceManager;
    		this.theDagBrowserPanel= theDagBrowserPanel;
    		this.theAppGUIFactory= theAppGUIFactory;
    		this.theShutdowner= theShutdowner;
    		this.theTracingEventQueue= theTracingEventQueue;
    		this.theJavaFXGUI= theJavaFXGUI;
        }

    public void initializeV()
      /* This method does the GUI initialization that 
        could not be done with constructor dependency injection.
        It does it for both Swing and JavaFX.
        */
      {

        // Start Swing GUI.
        EDTUtilities.invokeAndWaitV( // Dispatching on EDT
            new Runnable() {
              @Override
              public void run() { initializeOnEDTV(); }  
              } );

        // Start JavaFX GUI.
        theJavaFXGUI.startJavaFXLaunchV(); // Start thread that presents
          // JavaFX GUI window.

        }

    
    // Swing GUI start and stop methods.
    
    public void initializeOnEDTV() // GUIManager.
      /* This method does initialization of the Swing GUI.  It must be run on the EDT. 
        It builds the app's GUI in a new JFrame and starts it.
        */
      {
    		theAppLog.info("GUIManager.initializeOnEDTV() begins.");

    		theTracingEventQueue.initializeV(); 
    		  // Start monitor thread is our customized Event dispatcher.
      	Toolkit.getDefaultToolkit().getSystemEventQueue().push(
      			theTracingEventQueue); // Replace Event dispatcher with our customized one.

    	  //try { // Change GUI look-and-feel to be OS instead of java.
        //  UIManager.setLookAndFeel(UIManager.
        //    getSystemLookAndFeelClassName());
        //  } catch(Exception e) {}
      	
      	PlatformUI.setUIFont( new javax.swing.plaf.FontUIResource(
      			Font.MONOSPACED,Font.PLAIN,12
      			));

    		theDagBrowserPanel.initializeV();

    		buildJFrameV(); // Build and display the app JFrame.

        theDagBrowserPanel.showCommandHelpV(); // Build and display the Help dialog.

        KeyboardFocusManager.getCurrentKeyboardFocusManager().
          addKeyEventDispatcher( this );

        theAppInstanceManager.setAppInstanceListener(
          theAppGUIFactory.makeInstanceCreationRunnable(theJFrame)
          ); // For dealing with other running app instances.

    		theAppLog.info("GUIManager.initializeOnEDTV() ends.");
        }

    public void finalizeV()
      /* This method does finalization of the Swing GUI.  
       * It is called during shutdown.
       * It switches to the AWT thread EDT to do its work.
       */
      {
        theAppLog.info("GUIManager.finalizeOnV() called, doing on EDT.");
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
          theAppGUIFactory.makeJFrame( 
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
        theJFrame.setVisible(true);  // Make the window visible.
        }

    
    // JavaFX GUI start and stop methods are in other files.  See JavaFXGUI.


  } // GUIManager


class PlatformUI 
	{ // This is where platform UI code goes.

		static int fontSizeI= 12;  // Initial font size. 
		static int minFontSizeI= 3;  // minimum font size. 

	  public static void adjustUIFont(int sizeChangeI)
	    /* This method adjusts the default font for all UI component types
	      by changing the font size by sizeChangeI.
	      
	      ///enh Fix to not change the color of JTextAreas to white
	        when the Font is adjusted.
	      */
	    {
	  	  fontSizeI+= sizeChangeI;
	  	  if ( fontSizeI < minFontSizeI ) fontSizeI= minFontSizeI;
      	FontUIResource newFont= new FontUIResource(
      			Font.MONOSPACED,Font.PLAIN,fontSizeI
      			);
      	PlatformUI.setUIFont( newFont );
	      } 

	  public static void setUIFont(javax.swing.plaf.FontUIResource f)
	    // This method sets the default font for all UI component types.
	  	//
	  	///fix components that use default font.
	  	///doc : Better document variable names.
	    {
	      Enumeration<Object> keys = UIManager.getDefaults().keys();
	      while (keys.hasMoreElements()) {
	        Object key = keys.nextElement();
	        Object value = UIManager.get (key);
	        if (value instanceof javax.swing.plaf.FontUIResource)
	          UIManager.put (key, f);
	        }
	      allWindowsUpdateUIV(); // Make font change take effect.
	      } 
		
	  public static void allWindowsUpdateUIV()
	    /* This method updates every component's UI from the look and feel
	      including all descendant components, of every app window.
	      Also, any app windows that are visible are redisplayed
	      using the new UI state.
	      */
	    	///doc Better document variable names.
	    {
	    	for (Window w : Window.getWindows()) {
	        SwingUtilities.updateComponentTreeUI(w);
	        if (w.isDisplayable() &&
	            (w instanceof Frame ? !((Frame)w).isResizable() :
	            w instanceof Dialog ? !((Dialog)w).isResizable() :
	            true)) w.pack();
	    		}
	      } 
	
		}
