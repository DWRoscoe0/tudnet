package allClasses;

import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
// import javax.swing.JOptionPane;


import javax.swing.UIManager;

import static allClasses.Globals.*;  // For appLogger;

public class DagBrowser 

  implements AppInstanceListener

  /* This class contains the main(...) method,
    which is the app's entry point.
    */

  { // class DagBrowser

    /* Beginnings of a tester class which takes no space unless called.  
      void f() { System.out.println("f()"); }
      public static class Object {  // Tester
        public static void main(String[] args) {
          DagBrowser t = new DagBrowser();
          t.f();
        }
      }
    */

    Thread thisThread;  // Used for shutdown coordination.

    static private JFrame appJFrame;  // App's only JFrame (now).

    private DagBrowser( Thread aThread )  // Constructor.
      {
        thisThread= aThread;
        }
   
    public static void main(String[] argStrings)
      /* main(..) is the standard starting method of a Java application.
        It can be considered the top level state machine.

        It creates a single instance of DagBrowser to minimize statics.
        Then it checks for another running instance of the app
        and exits if needed after passing information to that instance.
        If it doesn't exit then it continues to run the app normally.
        
        ??? Presently its thread terminates after 
        the GUI and some background threads are started.
        But this will change to wait for app termination.
        */
      {
        // DoingBasicInitialization.
          appLogger.info("main thread beginning.");
          DagBrowser theDagBrowser= // Create main class instance.
            new DagBrowser( Thread.currentThread() );
          theDagBrowser.setDefaultExceptionHandlerV();

        if ( !AppInstanceManager.managingInstancesThenNeedToExitB( 
            argStrings  // contains information about other instances.
            ) ) 
          theDagBrowser.continuningThisAppInstanceV();

        Shutdowner.doShutdown();  // Doing accumulated shutdown jobs.
        
        // ExitingThisAppInstance.
          appLogger.info("main thread ending.");
        }

    public void continuningThisAppInstanceV()
      /* This method is run after it is determined that
        no other instance of the app is running,
        so this app instance should continue running normally,
        which means starting up the GUI, the background threads, etc.
        */
      {
        startingGUIV();

        // startingConnectionManager.
          ConnectionManager theConnectionManager= null;
          try {  // Construct and start ConnectionManager thread(s).
              theConnectionManager= new ConnectionManager();
              theConnectionManager.start( );  // Start its main thread.
            } catch (IOException e) {
              e.printStackTrace();
            }

          awaitingShutdownV();  // Most GUI interaction happens here.
        
        // stoppingConnectionManager.
          appLogger.info("DagBrowser: triggering Connections termination.");
          theConnectionManager.interrupt();  // Request thread terminatation.
          for  // Waiting for termination of ConnectionManager to complete.
            ( boolean threadTerminatedB= false ; !threadTerminatedB ; )
            try { // Waiting for ConnectionManager thread to terminate.
                theConnectionManager.join();  // Blocking until terminated.
                threadTerminatedB= true;  // Recording termination complete.
                } 
              catch (InterruptedException e) {  // Handling interrupt().
                Thread.currentThread().interrupt();
                }
        }

    private void setDefaultExceptionHandlerV()
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
              }
            }
          );

        // String nullString= null;  // Create null pointer.
        // nullString.length();  // Test handler.
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

        AppInstanceManager.setAppInstanceListener(  // App instance events...
          this  // ...will be heard by this main object's GUI.
          );

        appLogger.info("GUI/AWT thread signalled start-up done.");
        }

    static class GUIMaker
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
            try { // Change GUI look-and-feel to be that of OS.
              UIManager.setLookAndFeel(UIManager.
                getSystemLookAndFeelClassName());
              } catch(Exception e) {}
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
          new TerminationShutdownThread(thisThread);

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
      
    static class TerminationShutdownThread
      extends Thread
      /* This nested shutdown hook Thread class's run() method
        requests that the main thread finalize and terminate.
        It does this by setting the main thread's interrupt() flag.
        After the main thread finishes it finalization and terminates,
        this shutdown hook thread terminates also,
        eventually allowing the entire app to terminate.
        */
      {
        private Thread mainThread;

        public TerminationShutdownThread(Thread mainThread)
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

    private static JFrame startingJFrame()
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
            +AppInstanceManager.thisAppDateString()  // thisAppDateString()
            );

        final JComponent ContentJComponent=  // Construct content to be...
          new DagBrowserPanel();  // ... a DagBrowserPane.
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

    static class InstanceCreatedRunnable
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
