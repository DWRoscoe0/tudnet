package allClasses;

import javax.swing.JComponent;
import javax.swing.JFrame;

import static allClasses.Globals.*;  // For appLogger;

public class DagBrowser 

  implements AppInstanceListener

  /* This class contains the main(...) method,
    which is the app's entry point.
    */

  { // class DagBrowser

    /* Beginnings of a unit tester class which takes no space unless called.  
      void f() { System.out.println("f()"); }
      public static class Object {  // Tester
        public static void main(String[] args) {
          DagBrowser t = new DagBrowser();
          t.f();
        }
      }
      */

    public static void main(String[] argStrings)
      /* main(..) is the standard starting method of a Java application.
        
        Except for some logging and exception catching setup,
        it consists of 2 phases:
        * Creation phase.  It uses a factory to create the App.
        * Running phase. It runs the App.
        */
      {
        appLogger.info("main thread beginning.");
        setDefaultExceptionHandlerV();

        AppFactory theAppFactory=  // Creating app factory.
          new AppFactory(argStrings);

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

    private static class AppFactory {

      // This is the factory for all classes with App instance lifetime.

      String[] argStrings;
      Thread mainThread;
      ///Shutdowner theShutdowner;

      public AppFactory( String[] argStrings )  // Constructor.
        {
          this.argStrings= argStrings;  // Saving app argument strings.
          this.mainThread= Thread.currentThread(); // Determine out Thread.
          ///theShutdowner=   // Getting/Making ??? singleton Shutdowner.
          ///  Shutdowner.getShutdowner();
          }

      public App makeApp() 
        {
          Shutdowner theShutdowner= new Shutdowner();
          
          AppInstanceManager theAppInstanceManager=
            new AppInstanceManager(argStrings,theShutdowner);

          return new App(
            theShutdowner,
            theAppInstanceManager,
            new DagBrowserFactory(
              mainThread, 
              theAppInstanceManager,
              theShutdowner
              )
            );
          }

      }

    private static class App {

      Shutdowner theShutdowner;
      AppInstanceManager theAppInstanceManager;
      DagBrowserFactory theDagBrowserFactory;

      private App(   // Constructor.  For app Creation phase.
          Shutdowner theShutdowner,
          AppInstanceManager theAppInstanceManager,
          DagBrowserFactory theDagBrowserFactory
          )
        {
          this.theShutdowner= theShutdowner;
          this.theAppInstanceManager= theAppInstanceManager;
          this.theDagBrowserFactory= theDagBrowserFactory;
          }
          
      public void runV()  // For app Run phase. 
        {
          if ( !theAppInstanceManager.managingInstancesThenNeedToExitB( ) ) 

            {
              DagBrowser theDagBrowser= // Creating browser.
                  theDagBrowserFactory.makeDagBrowser();

              theDagBrowser.runV(); // Running browser.
              }

          theShutdowner.doShutdown();  // Doing shutdown jobs.
          }

      }

    private static class DagBrowserFactory {

      // This is the factory for all classes with DagBrowser lifetime.

      Thread mainThread;
      AppInstanceManager theAppInstanceManager;
      Shutdowner thetheShutdowner;

      public DagBrowserFactory(    // Constructor.
          Thread mainThread, 
          AppInstanceManager theAppInstanceManager,
          Shutdowner thetheShutdowner
          )
        {
          this.mainThread= mainThread;
          this.theAppInstanceManager= theAppInstanceManager;
          this.thetheShutdowner= thetheShutdowner;
         }

      public DagBrowser makeDagBrowser() 
        {
          ConnectionManager theConnectionManager=
            new ConnectionManager();

          MetaFileManager theMetaFileManager=
            new MetaFileManager(thetheShutdowner);

          MetaRoot theMetaRoot= new MetaRoot(theMetaFileManager);

          return new DagBrowser( 
            mainThread, 
            theAppInstanceManager,
            theConnectionManager,
            theMetaRoot  /// Holding reference.
            );
          }

      }

    // DagBrowser variables.
      Thread ourThread;  // Used for shutdown coordination.
      AppInstanceManager theAppInstanceManager;
      ConnectionManager theConnectionManager;
      ///MetaFileManager theMetaFileManager;
      MetaRoot theMetaRoot;

      private JFrame appJFrame;  // App's only JFrame (now).

    private DagBrowser(   // Constructor.
        Thread ourThread, 
        AppInstanceManager theAppInstanceManager,
        ConnectionManager theConnectionManager,
        ///MetaFileManager theMetaFileManager
        MetaRoot theMetaRoot
        )
      {
        this.ourThread= ourThread;
        this.theAppInstanceManager= theAppInstanceManager;
        this.theConnectionManager= theConnectionManager;
        ///this.theMetaFileManager= theMetaFileManager;
        this.theMetaRoot= theMetaRoot;

        }

    public void runV() // This method is the DagBrowser run phase.
      {
        startingGUIV();  // Building and displaying GUI.

        theConnectionManager.start( );  // Starting ConnectionManager thread.

        awaitingShutdownV();  // Interacting with user via GUI.

        theConnectionManager.stopV( );  // Stopping ConnectionManager thread.
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

    class GUIMaker
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

    private void awaitingShutdownV()
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
      
    class TerminationShutdownThread
      extends Thread
      /* This nested shutdown hook Thread class's run() method
        requests that the main thread finalize and terminate.
        It does this by setting the main thread's interrupt() flag.
        After the main thread finishes it finalization and terminates,
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

    public void newAppInstanceCreated()
      /* This AppInstanceListener method handles the creation of
        new running instances of this app.
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
        It returns that JFrame.  
        */
      {
        JFrame theJFrame =  // construct the main application JFrame.
          new JFrame(
            AppName.getAppNameString()
            +", DAG Browser 7 Test"
            +", archived "
            +theAppInstanceManager.thisAppDateString()  // thisAppDateString()
            );

        final JComponent ContentJComponent=  // Construct content to be a...
          new DagBrowserPanel(theAppInstanceManager);  // ...DagBrowserPane.
          //new JTextArea("TEST DATA");  // ... a JTextArea for a test???
          //new TextViewer(null,"test TextViewer");  // ... ???
        theJFrame.setContentPane( ContentJComponent );  // Store content.
        theJFrame.pack();  // Layout all the sub-panels.
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
