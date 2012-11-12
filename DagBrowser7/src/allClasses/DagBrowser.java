package allClasses;

// import java.awt.Color;
// import java.awt.event.WindowAdapter;
// import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
// import javax.swing.UIDefaults;
// import javax.swing.UIManager;
//import javax.swing.JTextArea;


public class DagBrowser
  /* This stand-alone application lets the user browse folders,
    and eventually files.  The main program just opens 
    a AppJFrame that contains a DagBrowserPanel, 
    which does all the work.
    */
  { // class DagBrowser
   
    public static void main(String[] args)
      /* This is the standard main method of an application.
        It creates the main window and activates it.
        */
      {

        JFrame AppJFrame =  // construct the main application JFrame.
          new JFrame("Infogora DAG Browser 7 Test");  
        //final DagBrowserPanel ContentDagBrowserPanel =  // construct the content panel.
        final JComponent ContentJComponent=  // construct the content to be...
          new DagBrowserPanel();  // ... a DagBrowserPane.
          //new JTextArea("TEST DATA");  // ... a JTextArea for a test???
          //new TextViewer(null,"test TextViewer");  // ... ???
        AppJFrame.setContentPane( ContentJComponent );  // set content.
        AppJFrame.pack();  // layout all the subpanels.
        AppJFrame.setLocation(50,50);  // set where app will display on desktop.
        // for some reason this eliminates JTree panel: AppJFrame.setSize(1000,400);  // set app window size.
        AppJFrame.setDefaultCloseOperation(  // set the close operation...
          JFrame.EXIT_ON_CLOSE );  // ...to be exit.

        /*
        AppJFrame.addWindowFocusListener
          // restore internal focus when window gets focus.
          ( new WindowAdapter()
            {
              public void windowGainedFocus(WindowEvent e) {
                System.out.println( "windowGainedFocus(), 1");
                ContentDagBrowserPanel.RestoreFocusV();
                }
              }
            );
        */
        
        AppJFrame.setVisible(true);  // make the app visible.
        
        // include a dispatch loop?  no, that's SWT.
        // use EventQueue.InvokeLater() if needed.
        }
        
    } // class DagBrowser
