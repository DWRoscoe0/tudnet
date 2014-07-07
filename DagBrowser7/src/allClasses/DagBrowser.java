package allClasses;

import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
// import javax.swing.JOptionPane;


import static allClasses.Globals.*;  // For appLogger;

public class DagBrowser 

  implements Runnable, AppInstanceListener

  /* This stand-alone application lets the user browse 
    the Infogora name space, including file folders,
    and eventually files.  
    The entry point is the main(...) method.
    */
  { // class DagBrowser

    /* Beginnings of a tester class which takes no space unless called.  */
      void f() { System.out.println("f()"); }
      public static class /* Tester */ Object {
        public static void main(String[] args) {
          DagBrowser t = new DagBrowser();
          t.f();
        }
      }

    static private JFrame appJFrame;  // App's only JFrame (now).
   
    public static void main(String[] argStrings)
      /* main(..) is the standard starting method of a Java application.
        It creates a single instance of DagBrowser
        and calls a helper method to do all the work.
        */
      {
        DagBrowser.setDefaultExceptionHandlerV();
        DagBrowser theDagBrowser= // Create instance to be Listener.
          new DagBrowser();
        theDagBrowser.mainHelperV(argStrings); // Make new instance...
          // ...do all the work.
        }

    private static void setDefaultExceptionHandlerV()
      /* This method sets the default handler for uncausht exceptions.
        It sents a message about the exception to
        the log file and to the console.
        */
      {
        Thread.setDefaultUncaughtExceptionHandler(
          new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
              appLogger.error(
                "Thread: "+t.getName()+". Uncaught Exception: "+e 
                );
              //System.out.println(t.getName()+": "+e);
              }
            }
          );

        // String nullString= null;  // Create null pointer.
        // nullString.length();  // Test handler.
        }

    public void mainHelperV(String[] argStrings)
      /* This is a non-static helper method 
        called by static method main(..).

        This method checks and does any necessary app instance management.
        If necessary to complete the instance management then it exits.
        Otherwise it queues the construction and activation of
        this app's JFrame window on the UI thread.
        */
      {
        appLogger.info(
          "App jar file dated " + AppInstanceManager.thisAppDateString()
          );
        if  // Exit if older instance already running and signalled.
          ( AppInstanceManager.manageInstancesAndExitB( 
          		argStrings
              ) 
            ) 
          { // Finish instance management by exiting this app instance.
            appLogger.info("Exiting because AppInstanceManager needs it.");
            System.exit(0);
            }

        appLogger.info("Queuing normal GUI start-up.");
        java.awt.EventQueue.invokeLater(  // Queue on GUI (AWT) thread...
          this // ...this object, whose run() will build and start app GUI.
          );
        }

      public void run()
        /* This method builds the app's GUI.in a new JFrame and starts it.
          It also activates some other code which might provide inputs.
          
          This method is run from the AWT thread after mainHelper() 
          calls invokeLater(..) because AWT GUI code is not thread-safe.
          */
        {
          appJFrame =  // construct and start the main application JFrame.
            startJFrame();
          
          AppInstanceManager.setAppInstanceListener(  // Link...
            this  // ...to AppInstance events.
            );
          try {
              NetworkThread.getTheNetworkThread().start( );
              ConnectionMaker.getTheConnectionMaker().activateV( );
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

    public void newInstanceCreated()
      /* This AppInstanceListener method handles the creation of
        new running instances of this app.
        */
      {  
        java.awt.EventQueue.invokeLater(
          new DagBrowser.InstanceCreatedRunnable()
          );
        }

    private static JFrame startJFrame()
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
            // ??? Add archive date string.
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
        This class is needed because DagBrowser.run() 
        is already used for another purpose.

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
