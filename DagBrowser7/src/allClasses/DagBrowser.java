package allClasses;

import javax.swing.JComponent;
import javax.swing.JFrame;

import static allClasses.Globals.*;  // For appLogger;

public class DagBrowser
  /* This stand-alone application lets the user browse 
    the Infogora name space, including file folders,
    and eventually files.  
    The entry point is the main(...) method.
    */
  { // class DagBrowser

    static private JFrame appJFrame;  // App's only JFrame (now).
   
    public static void main(String[] args)
      /* main(..) is the standard starting method of a Java application.

        This method checks for an older running app instance.
        If it detects it then it signals its presence and exits.
        Otherwise it queues the construction creation and activation of
        this app's JFrame window on the UI thread.
        */
      {
        if  // If older instance already running then signal and exit.
          (!AppInstanceManager.registerInstance()) 
          {
            appLogger.info("Exiting because an older app instance was running.");
            System.exit(0);
            }

        java.awt.EventQueue.invokeLater(  // Queue GUI creation and start.
          new DagBrowser.startGUIRunnable()
          );
        }

    static class startGUIRunnable implements Runnable
      /* This Runnable nested class's Run method 
        is queued by the main() method to run when the app starts.
        It creates the app's GUI in a new JFrame and starts it.
        It also sets the Listener for new app instances.
        */
      {
        public void run() 
          {
            appJFrame =  // construct and start the main application JFrame.
              startJFrame();
            
            AppInstanceManager.setAppInstanceListener(  // Activate...
              new AppInstanceListener() {  // ... listening for...
                public void newInstanceCreated() {  // new app instances.
                  java.awt.EventQueue.invokeLater(
                    new DagBrowser.InstanceCreatedRunnable()
                    );
                  }
                }
              );
            }
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
            AppName.getAppNameString()+
            ", DAG Browser 7 Test"
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

    static class InstanceCreatedRunnable implements Runnable
      /* This Runnable nested class's Run method
        is queued to run after a new app instance makes itself known.
        It tries to move the app's JFrame to the front.
        In some Windows versions it does it only once,
        or only highlights the app's task-bar button, or both.

        Fix this to work in all cases.
        Use a JFrame method override,
        which is described by the [best] answer, with 18 votes, at
        http://stackoverflow.com/questions/309023/howto-bring-a-java-window-to-the-front
        
        Do something else when there are multiple tabs 
        and/or multiple JFrames.
        */
      {
        public void run() 
          {
            appJFrame.toFront();
            appJFrame.repaint();
            }
        }

    } // class DagBrowser
