package allClasses;

import static allClasses.AppLog.theAppLog;

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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

class SwingUI

  implements KeyEventDispatcher 

  { // SwingUI

    // This class is used to manage the app's Swing user interface.


    // Injected dependency variables.
    private AppInstanceManager theAppInstanceManager;
    private DagBrowserPanel theDagBrowserPanel;
    private AppFactory theAppFactory;
    private Shutdowner theShutdowner;
    private TracingEventQueue theTracingEventQueue;

    // Other InnerApp instance variables.
    private JFrame theJFrame;  // App's only JFrame (now).


    SwingUI(   // Constructor. 
        AppInstanceManager theAppInstanceManager,
        DagBrowserPanel theDagBrowserPanel,
        AppFactory theAppFactory,
        Shutdowner theShutdowner,
        TracingEventQueue theTracingEventQueue
      )
      {
        this.theAppInstanceManager= theAppInstanceManager;
        this.theDagBrowserPanel= theDagBrowserPanel;
        this.theAppFactory= theAppFactory;
        this.theShutdowner= theShutdowner;
        this.theTracingEventQueue= theTracingEventQueue;
        }

    public void initializeV()
      /* This method does the GUI initialization that 
        could not be done with constructor dependency injection.
        See initializeOnEDTV() for details.
        */
      {
        // Start Swing GUI by initializing on EDT.
        EDTUtilities.invokeAndWaitV( () -> { initializeOnEDTV(); } );
        }
    
    private void initializeOnEDTV()
      /* This method does initialization of the Swing GUI.  
        It must be run on the EDT. 
        It builds the app's GUI in a new JFrame and shows it.
        */
      {
        theAppLog.info("SwingUI.initializeOnEDTV() begins.");

        theTracingEventQueue.initializeV(); 
          // Start monitor thread is our customized Event dispatcher.
        Toolkit.getDefaultToolkit().getSystemEventQueue().push( // Replace queue
          theTracingEventQueue); // and dispatcher with our customized ones.

        //try { // Change GUI look-and-feel to be OS instead of java.
        //  UIManager.setLookAndFeel(UIManager.
        //    getSystemLookAndFeelClassName());
        //  } catch(Exception e) {}

        setUIFont( // Select a fixed width font. 
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

        theAppLog.info("SwingUI.initializeOnEDTV() ends.");
        }
    
    public void finalizeV()
      /* This method does finalization.  */
      { 
        EDTUtilities.invokeAndWaitV( () -> finalizeOnEDTV() );
        }

    private void finalizeOnEDTV()
      /* This method does finalization.  It must be run on the EDT.  */
      { 
        theAppLog.debug("SwingUI.finalizeOnEDTV() called.");

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
                "SwingUI.finalizeOnEDTV() disposing "
                + windowTypeString
                + " Window titled: "
                + titleString;
            theAppLog.info(messageString);
            aWindow.dispose(); // Do this so Event Dispatch Thread terminates.
            }
        }
    
    public boolean dispatchKeyEvent(KeyEvent theKeyEvent)
      /* Processes KeyEvent keyboard input before being passed to KeyListeners.
       * It only purpose now is to do increasing or decreasing of the font sie.
       */
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
            adjustUIFont( -1); // Make font smaller.
          if ( (idI==KeyEvent.KEY_PRESSED) && (keyI == KeyEvent.VK_EQUALS) &&
             controlB && shiftB
             )
            adjustUIFont( +1 ); // Make font bigger.
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
        theAppLog.info("SwingUI.theJFrame.setVisible(true) done.");
        SwingUtilities.invokeLater( () -> {
          theDagBrowserPanel.restoreFocusV(); // Setting initial focus. 
          } ); /* Done this way because in Java 8 
            Component.requestFocusInWindow() will cause 
            NullPointerException before the first dispatched message.
            */
        theJFrame.pack();  // Layout all the content's sub-panels, then
        Dimension screenDimension= Toolkit.getDefaultToolkit().getScreenSize();
        theJFrame.setSize( // Use 3/4 of the screen extent vertically and horizontally.
            (int)(screenDimension.getWidth() * 0.75), 
            (int)(screenDimension.getHeight() * 0.75)
            ); // << not working!!!
        theJFrame.setLocationRelativeTo(null);  // center JFrame on screen.
        //////// theJFrame.setState(Frame.ICONIFIED); // Initially minimize it.
        theJFrame.setVisible(true);  // Make the window visible.
        }


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

        setUIFont( newFont ); // Set the new font.
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
