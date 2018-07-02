package allClasses;

import java.awt.Dialog;
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

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

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
    private GUIBuilderStarter theGUIBuilderStarter;
    private Shutdowner theShutdowner;

    public AppGUI(   // Constructor.
        EpiThread theConnectionManagerEpiThread,
        EpiThread theCPUMonitorEpiThread,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode,
        GUIBuilderStarter theGUIBuilderStarter,
        Shutdowner theShutdowner
        )
      {
	      this.theConnectionManagerEpiThread= theConnectionManagerEpiThread;
	      this.theCPUMonitorEpiThread= theCPUMonitorEpiThread;
        this.theDataTreeModel= theDataTreeModel;
        this.theInitialRootDataNode= theInitialRootDataNode;
        this.theGUIBuilderStarter= theGUIBuilderStarter;
        this.theShutdowner= theShutdowner;
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

        Note, move-to-front can be problematic.
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
            appLogger.info("Trying move-to-front.");
            theJFrame.toFront();
            theJFrame.repaint();
            }

        } // class InstanceCreationRunnable

    public void runV() // This method does the main AppGUI run phase.
      {
    		appLogger.info("AppGUI.run() begins.");
        theDataTreeModel.initializeV( theInitialRootDataNode );
        EDTUtilities.invokeAndWaitV(  // Queue on GUI (AWT) thread...
            theGUIBuilderStarter   // ...this Runnable GUIBuilderStarter,...
            );  //  whose run() method will build and start the app's GUI.
        theConnectionManagerEpiThread.startV();
        theCPUMonitorEpiThread.startV();

        // Now the app is running and interacting with the user.

        theShutdowner.waitForAppShutdownUnderwayV();

        // Now the app is shutting down.
        
        theDataTreeModel.logListenersV(); // [for debugging]
        // theCPUMonitorEpiThread.stopAndJoinV( ); ?? 
        theConnectionManagerEpiThread.stopAndJoinV( ); 
          // Stopping ConnectionManager thread, ending all connections.
    		appLogger.info("AppGUI.run() ends.");
        }

    } // class AppGUI

class GUIBuilderStarter // This EDT-Runnable starts GUI. 
  implements Runnable, KeyEventDispatcher 

  /* This nested class is used to create and start the app's GUI.
    It's run() method runs in the EDT thread.
    It signals its completion by executing doNotify() on
    the LockAndSignal object passed to it when 
    its instance is constructed.
    */

  { // GUIBuilderStarter

		// Injected dependency variables.
		private AppInstanceManager theAppInstanceManager;
		private DagBrowserPanel theDagBrowserPanel;
		private AppGUIFactory theAppGUIFactory;
		private Shutdowner theShutdowner;
		private TracingEventQueue theTracingEventQueue;
  	private BackgroundEventQueue theBackgroundEventQueue;

    // Other AppGUI instance variables.
    private JFrame theJFrame;  // App's only JFrame (now).

    GUIBuilderStarter(   // Constructor. 
    		AppInstanceManager theAppInstanceManager,
    		DagBrowserPanel theDagBrowserPanel,
    		AppGUIFactory theAppGUIFactory,
    		Shutdowner theShutdowner,
    		TracingEventQueue theTracingEventQueue,
      	BackgroundEventQueue theBackgroundEventQueue
    		)
      {
    		this.theAppInstanceManager= theAppInstanceManager;
    		this.theDagBrowserPanel= theDagBrowserPanel;
    		this.theAppGUIFactory= theAppGUIFactory;
    		this.theShutdowner= theShutdowner;
    		this.theTracingEventQueue= theTracingEventQueue;
      	this.theBackgroundEventQueue= theBackgroundEventQueue;
        }

    public void run() // GUIBuilderStarter.
      /* This method builds the app's GUI in a new JFrame and starts it.
        This method is run on the AWT thread by startingBrowserGUIV() 
        because AWT GUI code is not thread-safe.
        */
      {
    		appLogger.info("GUIBuilderStarter.run() beginning.");
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
      	
      	PlatformUI.setUIFont( new javax.swing.plaf.FontUIResource(
      			Font.MONOSPACED,Font.PLAIN,12
      			));

    		theDagBrowserPanel.initializeV();

    		theJFrame =  // Construct and start the app JFrame.
    				startingJFrame();

        theAppInstanceManager.setAppInstanceListener(
          theAppGUIFactory.makeInstanceCreationRunnable(theJFrame)
          ); // For dealing with other running app instances.

        KeyboardFocusManager.getCurrentKeyboardFocusManager().
          addKeyEventDispatcher( this );

        //appLogger.info("GUI start-up complete.");
    		appLogger.info("GUIBuilderStarter.run() ending.");
        }
    
		public boolean dispatchKeyEvent(KeyEvent theKeyEvent)
		  // Processes KeyEvent keyboard input before being passed to KeyListeners.
			{ 
			  boolean processedKeyB= true;
				int idI= theKeyEvent.getID();
			  int keyI= theKeyEvent.getKeyCode();
			  ///dbg char keyC= theKeyEvent.getKeyChar();
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
            Config.appString
            +", version "
            +theAppInstanceManager.thisAppDateString()
            );
        theJFrame.setContentPane( theDagBrowserPanel );  // Store content.
        theJFrame.pack();  // Layout all the content's sub-panels.
        theJFrame.setLocationRelativeTo(null);  // Center JFrame on screen.
        theJFrame.setDefaultCloseOperation( // Set the close operation to be
          JFrame.DO_NOTHING_ON_CLOSE // nothing, so listener can handle it all. 
          );
        theJFrame.addWindowListener( // Set Listener to handle close events
          new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
              appLogger.info(
             		"windowClosing(..) ======== REQUESTING APP SHUTDOWN =========");
              theShutdowner.requestAppShutdownV();
              }
          	});
        theJFrame.setVisible(true);  // Make the window visible.
        appLogger.info(
          	"GUIBuilderStarter.theJFrame.setVisible(true) done."
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
        return theJFrame;
        }

    } //GUIBuilderStarter


class PlatformUI 
	{ // This is where platform UI code goes.

		static int fontSizeI= 12;  // Initial font size. 
		static int minFontSizeI= 3;  // minimum font size. 

	  public static void adjustUIFont(int sizeChangeI)
	    /* This method adjsts the default font for all UI component types
	      by changing the font size by sizeChangeI.
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
