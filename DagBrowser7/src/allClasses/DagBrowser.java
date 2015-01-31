package allClasses;

import javax.swing.JFrame;

import static allClasses.Globals.*;  // For appLogger;

public class DagBrowser 

  implements AppInstanceListener

  /* This class contains the main(...) method,
    which is the app's entry point.
    */

  { // class DagBrowser

    // Injected dependency variables.
      private Thread ourThread;  // Used for shutdown coordination.
      private AppInstanceManager theAppInstanceManager;
      private ConnectionManager theConnectionManager;
      private DagBrowserPanel theDagBrowserPanel;
      private DataTreeModel theDataTreeModel;
      private DataNode theInitialRootDataNode;

    // Other instance variables.
      private JFrame appJFrame;  // App's only JFrame (now).

    /* Beginnings of a unit tester nested class ??
      It takes no space unless called.  

      void f() { System.out.println("f()"); }
      
      public static class Test {

        public static void main( String[] args ) {

          DagBrowser t = new DagBrowser();

          t.f();

        }
      }
      */

    public static void main(String[] argStrings)
      /* main(..) is the standard starting method of a Java application.
        
        Except for some logging and exception catching setup,
        it consists of 2 phases:
        * Creation phase.  It uses an AppFactory to create the App.
        * Running phase. It runs the App by calling runV().
        */
      {
        appLogger.info("main thread beginning.");
        setDefaultExceptionHandlerV();

        AppFactory theAppFactory=  // Creating AppFactory.
          new AppFactory(argStrings); // This is the only "new" that is
            // not in a Factory.

        DagBrowser.App theApp= theAppFactory.makeApp(); // Creating app.

        theApp.runV();  // Running app.
        
        appLogger.info("main thread ending.");
        }

    private static void setDefaultExceptionHandlerV()
      /* This method sets the default handler for uncausht exceptions.
        The handler sends a message about the exception to
        the log file and to the console.
        */
      {
        Thread.setDefaultUncaughtExceptionHandler(
          new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
              System.out.println(t.getName()+": "+e);
              appLogger.error(
                "Thread: "+t.getName()+". Uncaught Exception: "+e
                );
              e.printStackTrace();
              }
            }
          );

        // String nullString= null;  // Uncomment to Create null pointer...
        // nullString.length();  //...and test uncaught exception handler.
        }

    private static class AppFactory {  // For App class lifetimes.

      // This is the factory for all classes with App lifetime.

      String[] argStrings;
      Thread mainThread;

      public AppFactory( String[] argStrings )  // Constructor.
        {
          this.argStrings= argStrings;  // Saving app argument strings.
          this.mainThread= Thread.currentThread(); // Determine out Thread.
          }

      public App makeApp() 
        // Makes several objects and returns one referencing them all.
        {
          Shutdowner theShutdowner= new Shutdowner();
          
          AppInstanceManager theAppInstanceManager=
            new AppInstanceManager(argStrings,theShutdowner);

          return new App(
            theShutdowner,
            theAppInstanceManager,
            new AppGUIFactory(
              mainThread, 
              theAppInstanceManager,
              theShutdowner
              )
            );
          }

      }

    private static class App { // The App, including pre-UI stuff.

      Shutdowner theShutdowner;
      AppInstanceManager theAppInstanceManager;
      AppGUIFactory theAppGUIFactory;

      private App(   // Constructor.  For app Creation phase.
          Shutdowner theShutdowner,
          AppInstanceManager theAppInstanceManager,
          AppGUIFactory theAppGUIFactory
          )
        {
          this.theShutdowner= theShutdowner;
          this.theAppInstanceManager= theAppInstanceManager;
          this.theAppGUIFactory= theAppGUIFactory;
          }
          
      public void runV()  // This is the App Run phase. 
        {
          if ( ! theAppInstanceManager.managingInstancesThenNeedToExitB( ) ) 

            {
              DagBrowser theDagBrowser= // Getting browser singleton.
                  theAppGUIFactory.getDagBrowser();

              theDagBrowser.runV(); // Running browser.
              }

          theShutdowner.doShutdown();  // Doing shutdown jobs.
          }

      } // class App.

    private static class AppGUIFactory {  // For DagBrowser class lifetimes.

      /* This is the factory for all classes with AppGUI lifetime,
        The AppGUI lifetime is shorter that the App lifetime,
        because some App operations happen before or without
        presentation of the AppGUI.
        The DagBrowser class is the top level of the AppGUI.
        DagBrowser should probably be renamed to AppGUI ???
        */

      // Storage for the singletons that will be returned.
      DagBrowser theDagBrowser;

      public AppGUIFactory(  // Constructor.
          Thread mainThread, 
          AppInstanceManager theAppInstanceManager,
          Shutdowner theShutdowner
          )
      	// Builds the singletons in one possible order.
        {
          DataRoot theDataRoot= new DataRoot( );
          MetaFileManager theMetaFileManager= 
        		new MetaFileManager( theDataRoot );
          MetaRoot theMetaRoot= 
            new MetaRoot( theDataRoot, theMetaFileManager );
          MetaFileManager.Finisher theMetaFileManagerFinisher= 
          	new MetaFileManager.Finisher(
              theMetaFileManager,
              theMetaRoot
              );
          DataTreeModel theDataTreeModel= new DataTreeModel( 
            theDataRoot, theMetaRoot, theMetaFileManagerFinisher, theShutdowner
            );
          ConnectionManager.Factory theConnectionManagerFactory= 
            new ConnectionManager.Factory( theDataTreeModel );
          ConnectionManager theConnectionManager= 
            theConnectionManagerFactory.getConnectionManager();
          DataNode theInitialRootDataNode=  // Building first legal value.
            new InfogoraRoot( 
              new DataNode[] { // ...an array of all child DataNodes.
                new FileRoots(),
                new Outline( 0 ),
                theConnectionManager,
                new Infinitree( null, 0 )
                }
              );
          DagBrowserPanel theDagBrowserPanel= new DagBrowserPanel(
            theAppInstanceManager,
            theDataTreeModel,
            theDataRoot,
            theMetaRoot
            );
          theDagBrowser= new DagBrowser( 
            mainThread, 
            theAppInstanceManager,
            theConnectionManager,
            theDagBrowserPanel,
            theDataTreeModel,
            theInitialRootDataNode
            );
         }

      public DagBrowser getDagBrowser() 
        {
          return theDagBrowser;
          }

      } // class AppGUIFactory.

    private DagBrowser(   // Constructor.
        Thread ourThread, 
        AppInstanceManager theAppInstanceManager,
        ConnectionManager theConnectionManager,
        DagBrowserPanel theDagBrowserPanel,
        DataTreeModel theDataTreeModel,
        DataNode theInitialRootDataNode
        )
      {
        this.ourThread= ourThread;
        this.theAppInstanceManager= theAppInstanceManager;
        this.theConnectionManager= theConnectionManager;
        this.theDagBrowserPanel= theDagBrowserPanel;
        this.theDataTreeModel= theDataTreeModel;
        this.theInitialRootDataNode= theInitialRootDataNode;
        }

    public void runV() // This method is the main DagBrowser run phase.
      {
        theDataTreeModel.initializeV(
        	theInitialRootDataNode
          );

        startingGUIV();  // Building and displaying Graphical User Interface.

        EpiThread theConnectionManagerEpiThread=
          new EpiThread( theConnectionManager, "ConnectionManager" );
        theConnectionManagerEpiThread.start( );
          // Starting ConnectionManager thread.

        awaitingShutdownV();  // Interacting with user via GUI.

        theConnectionManagerEpiThread.stopAndJoinV( ); 
          // Stopping ConnectionManager thread.
        }

    private void startingGUIV()
      /* This method builds and starts the Graphical User Interface (GUI).
        It doesn't return until the GUI has been started.
        This is tricky because the GUI runs on a different thread,
        the AWT thread.
        */
      {
        LockAndSignal guiLockAndSignal=  // For completion signalling.
          new LockAndSignal(false);

        appLogger.info("Queuing GUI start-up.");

        java.awt.EventQueue.invokeLater(  // Queue on GUI (AWT) thread...
          new GUIMaker(guiLockAndSignal)  // ...this object,...
          );  //  whose run() will build and start the app's GUI.

        guiLockAndSignal.doWaitE(); // Wait for signal that GUI is running.

        theAppInstanceManager.setAppInstanceListener(  // App instance events...
          this  // ...will be heard by this main object's GUI.
          );

        appLogger.info("GUI/AWT thread signalled start-up done.");
        }

    class GUIMaker  // Runs in the AWT thread.
      implements Runnable

      /* This nested class is used to create and start the app's GUI.
        It's run() method runs in the AWT thread.
        It signals its completion by executing doNotify() on
        the LockAndSignal object passed to it when 
        its instance is constructed.
        */

      {
        LockAndSignal guiLockAndSignal;
        
        GUIMaker( LockAndSignal guiLockAndSignal )  // Constructor.
          {
            this.guiLockAndSignal=   // Save lock reference.
              guiLockAndSignal;
            }

        public void run()
          /* This method builds the app's GUI in a new JFrame 
            and starts it.
            This method is run from the AWT thread after startingGUIV() 
            calls invokeLater(..) because AWT GUI code is not thread-safe.
            */
          {
            //try { // Change GUI look-and-feel to be that of OS.
            //  UIManager.setLookAndFeel(UIManager.
            //    getSystemLookAndFeelClassName());
            //  } catch(Exception e) {}
            appJFrame =  // construct and start the main application JFrame.
              startingJFrame();
            appLogger.info("GUI start-up complete.");

            guiLockAndSignal.doNotifyV();  // Signal everything is done.
            }

        }

    private void awaitingShutdownV()  // While interacting with user.
      // This method blocks until shutdown is underway..
      {
        Thread theShutdownThread =  // Creating ShutdownThread Thread.
          new TerminationShutdownThread(ourThread);

        Runtime.getRuntime().addShutdownHook(theShutdownThread); // Adding...
          // ...it to Runtime to be run at shut-down time.

        appLogger.info("DagBrowser.awaitingShutdownV(): begining wait.");

        while  // Sleeping in a loop until notification by interrupt().
          (! Thread.interrupted() )  // Test and clear thread interrupt flag.
          try { // Block for 30 seconds or end early if interrupt() occurs.
              Thread.sleep(30*1000);  // Sleep 30 seconds.  Any amount works.
            } catch (InterruptedException anInterruptedException) {
              Thread.currentThread().interrupt();  // Re-interrupt for exit.
            }

        appLogger.info("DagBrowser.awaitingShutdownV(): ending wait.");

        // At this point shutdown is underway.
        
        }
      
    class TerminationShutdownThread  // For terminating main() thread.
      extends Thread
      /* This nested shutdown hook Thread class's run() method
        requests that the main thread finalize and terminate.
        It does this by setting the main thread's interrupt() flag.
        After the main thread finishes its finalization and terminates,
        this shutdown hook thread terminates also,
        eventually allowing the entire app to terminate.
        */
      {
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
            // After return thread has terminated.
            }
        }

    public void newAppInstanceCreated()  // New instance listener method.
      /* This AppInstanceListener method handles the creation of
        new running instances of this app.
        It is called by theAppInstanceManager.
        */
      {
        java.awt.EventQueue.invokeLater(
          new DagBrowser.InstanceCreatedRunnable()
          );
        }

    private JFrame startingJFrame()
      /* This method creates the app's JFrame and starts it.
        It is meant to be run on the UI (AWT) thread.
        The JFrame content is set to a DagBrowserPanel 
        which contains the GUI and other code which does most of the work.
        It returns the JFrame.  
        */
      {
        JFrame theJFrame =  // construct the main application JFrame.
          new JFrame(
            AppName.getAppNameString()
            +", DAG Browser 7 Test"
            +", archived "
            +theAppInstanceManager.thisAppDateString()
            );
        theDagBrowserPanel.initializeV(); // Initializing post-construction.
        theJFrame.setContentPane( theDagBrowserPanel );  // Store content.
        theJFrame.pack();  // Layout all the content's sub-panels.
        theJFrame.setLocationRelativeTo(null);  // Center JFrame on screen.
        theJFrame.setDefaultCloseOperation(  // Set the close operation...
          JFrame.EXIT_ON_CLOSE );  // ...to be exit, since it's only frame.
        theJFrame.setVisible(true);  // make the app visible.
        return theJFrame;
        }

    class InstanceCreatedRunnable
      implements Runnable
      /* This nested class contains a run() method which
        is used to process triggerings of the
        AppInstanceListener method newInstanceCreated() on the AWT thread.

        This method tries to move the app's JFrame to the front.
        Other actions might be appropriate if UI were different, 
        such as there being multiple tabs and/or multiple JFrames.
        Also maybe some of this should be done in DagBrowserPanel?

        ??? Eventually it might also process command arguments
        as part of the software update process as part of
        the AppInstanceManager logic.
        
        Note, in some Windows versions, move-to-front works only once,
        or only highlights the app's task-bar button, or both.
        The following code was moving the first time, 
        and highlighting after that.  Now it only highlights.
        This might be part of the focus-stealing arms race.
        Fix this to work in all cases?
        Maybe try using a JFrame method override,
        which is described by the [best] answer, with 18 votes, at
        http://stackoverflow.com/questions/309023/howto-bring-a-java-window-to-the-front
        */
      {
        public void run() 
          {
            /* This breaks already partially broken move-to-front.
            JOptionPane.showMessageDialog(
              null, // this, // null, 
              "The app is already running.",
              "Info",
              JOptionPane.INFORMATION_MESSAGE
              );
            */
            appLogger.info("Trying move-to-front.");
            appJFrame.toFront();
            appJFrame.repaint();
            }
        }

    } // class DagBrowser
