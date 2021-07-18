package allClasses.javafx;

import static allClasses.AppLog.theAppLog;

import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.application.PlatformImpl;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.LockAndSignal;
import allClasses.Persistent;
import allClasses.Shutdowner;
import allClasses.bundles.BundleOf2;
import allClasses.LockAndSignal.Input;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Window;

public class JavaFXGUI

  /* This class is used to manage JavaFX operations,
   * including:
   * * the JavaFX runtime
   * * the launching of a JavaFX Application
   * 
   * ///klu This class contains much kludgy JavaFX code because 
   * I was new to JavaFX and I wanted to transition gradually 
   * from the Swing libraries in this app to the JavaFX libraries,
   * so I wanted to have code from both sets of libraries coexist.
   * I tried to do it by adapting example code
   * normally offered to show how to start a JavaFX-only app.
   * I succeeded, but the result was definitely a kludge.
   * 
   * Since that time I learned how to start the JavaFX runtime 
   * without involving an Application subclass instance.
   * This is done by calling startup(.) from startJavaFXAndReturnString(),
   * which is called from Infogora.main(.).
   * 
   * ///org It should now be possible to eliminate much of the kludginess
   * in the code that follows it, and maybe move all the remaining code into
   * JavaFXApp, the Application subclass.
   */

  {

    // Injected dependencies.

    private Shutdowner theShutdowner;
    private DataNode theRootDataNode;
    private Persistent thePersistent;
    private DataRoot theDataRoot;
    private Selections theSelections;

    // Other variables

    private static Map<String,BundleOf2<Window,Alert>> windowMap= 
      new HashMap<String,BundleOf2<Window,Alert>>();
  
    // Startup confirmation variables.
    private static LockAndSignal runningLockAndSignal= new LockAndSignal();
    private static volatile JavaFXGUI theJavaFXGUI= null; // Only instance. 
    public static volatile boolean runtimeIsActiveB= false; 

    static { // Used to log when this class is loaded.
        theAppLog.info("JavaFXGUI loaded.");
        }


    // Methods

    public static String startRuntimeAndReturnString()
      /* This method manually starts the JavaFX runtime.  This can be used to 
       * start the runtime before it would normally be started
       * when the JavaFX Application class is launched,
       * or if the app doesn't use the Application class at all.
       * 
       * This might be useful, for example,
       * for reporting with JavaFX dialogs boxes 
       * errors that might occur early in app startup,
       * before the normal JavaFX Application GUI is displayed.
       * 
       * ///ano Only a few lines of the code in this method 
       * are actually needed to start the JavaFX runtime.
       * Most of the code deals with an anomaly,
       * a mysterious failure of a startup confirmation wait to end.
       * Debug logging was added to debug the anomaly,
       * and a timeout was added to mitigate it.  
       * After that code was added, the failures stopped,
       * but the mitigation code was left in case they returned.
       * The code was tested with a breakpoint to simulate the failure.
       * 
       * This method returns a String describing the anomalous behavior
       * if the failure happened, or null if it didn't happen.
       * Failure can happen in 2 ways:
       * * The runtime failed to start.
       * * The runtime started but confirmation was not received.
       * Either will be reported as failure.
       * 
       * ///enh Use 2 time-out values instead of only 1:
       * * A value above which is considered a failure.
       * * A value above which this method will not wait.
       *   In this case the runtime might not have started.
       * 
       * There is no guarantee that the JavaFX runtime is running
       * when this method returns.
       * 
       */
      {
        String resultString= null; ///ano
        theAppLog.debug("JavaFXGUILog", ///ano
            "JavaFXGUI.startAndReturnString() begins."); ///ano
        long javaFXStartTimeMsL= System.currentTimeMillis(); ///ano
        // theAppLog.error("Test 1, before runtime start."); ///ano
        PlatformImpl.startup( // Start FX runtime with confirmation Runnable. 
          () -> {
            // EpiThread.interruptibleSleepB(5000); // Test.
            theAppLog.info("JavaFXGUI.startAndReturnString() " ///ano
                + "notify begins, RUNTIME IS UP!"); ///ano
            runtimeIsActiveB= true; // Confirm that JavaFX queue is active.
            runningLockAndSignal.notifyingV(); // Notify about confirmation.
            theAppLog.debug( "JavaFXGUILog", ///ano
               "JavaFXGUI.startAndReturnString() notify ended."); ///ano
            } );
        // theAppLog.warning("Test 2, before runtime wait."); ///ano
        final long maxWaitL= 2000; // Maximum wait loop time.
        long waitStartTimeMsL= System.currentTimeMillis(); ///ano
        theAppLog.debug("JavaFXGUILog", ///ano
            "JavaFXGUI.startAndReturnString() wait begins."); ///ano
        Input theInput= Input.NOTIFICATION; // Assume fast confirmation.
        while (true) { // Loop until one of the exit conditions is satisfied.
          theAppLog.debug("JavaFXGUILog", ///ano
              "JavaFXGUI.startAndReturnString() "
              + "theJavaFXGUI=="+theJavaFXGUI
              + ", runtimeIsActiveB=="+runtimeIsActiveB);
          if (runtimeIsActiveB) break; // Exit if runtime is active.
          if (Input.INTERRUPTION == theInput) break; // Exit if interrupted.
          if (Input.TIME == theInput) break; ///ano Exit if time-out.
          theInput=  // Wait for runtime startup.  ///ano With timeout.
            runningLockAndSignal.waitingForInterruptOrIntervalOrNotificationE(
              javaFXStartTimeMsL, ///ano Mitigation, time-out interval start. 
              maxWaitL); ///ano Mitigation, time-out interval length.
          } // while(true)
        // theAppLog.error("Test 3, after runtime wait."); ///ano

        String waitResultString= ///ano
            "JavaFXGUI.startAndReturnString() wait ended because of "
            +theInput+".\n  Used total of "
            +(waitStartTimeMsL-javaFXStartTimeMsL)
            +"+"+(System.currentTimeMillis()-waitStartTimeMsL)
            +" ms.";
        theAppLog.debug("JavaFXGUILog", ///ano Report how wait ended.
            waitResultString); ///ano
        if (theInput == Input.TIME) ///ano If wait time-out anomaly happened,
          resultString= waitResultString; ///ano return description string.
        return resultString; ///ano
        }


    public static JavaFXGUI initializeJavaFXGUI(
          DataNode theRootDataNode,
          Shutdowner theShutdowner,
          Persistent thePersistent,
          DataRoot theDataRoot,
          Selections theSelections
          )
      /* This method constructs, initializes, and returns
       * what will become the only instance of this class.
       */
    {
      theAppLog.debug("JavaFXGUILog","initializeJavaFXGUI(.) begins.");
      if (null != theJavaFXGUI)
        theAppLog.error("initializeJavaFXGUI(.) "
            + "Instance already constructed!");
        else
        { // Create instance and store injected dependencies.
          JavaFXGUI aJavaFXGUI= new JavaFXGUI();
          aJavaFXGUI.theShutdowner= theShutdowner;
          aJavaFXGUI.theRootDataNode= theRootDataNode;
          aJavaFXGUI.thePersistent= thePersistent;
          aJavaFXGUI.theDataRoot= theDataRoot;
          aJavaFXGUI.theSelections= theSelections;
          theJavaFXGUI= aJavaFXGUI; // Finish by storing in static variable.
          runningLockAndSignal.notifyingV(); // Inform caller of definition.
          theAppLog.debug("JavaFXGUILog", "Infogora.initializeJavaFXGUI(.) "
              + "defining theJavaFXGUI=="+theJavaFXGUI);
          }
      theAppLog.debug("JavaFXGUILog","initializeJavaFXGUI(.) ends.");
      return theJavaFXGUI;
      }

    private JavaFXGUI() // private constructor guarantees a single instance.
      {
        theAppLog.debug("JavaFXGUILog","JavaFXGUI() constructor called.");
        }

    public static JavaFXGUI getInstanceJavaFXGUI()
      /* This is the instance getter method.
       * This method should not be called until 
       * initializeJavaFXGUI(.) has been called.
       * It exists mainly so that JavaFXApp can call back to this class.
       */
      {
        theAppLog.debug("JavaFXGUILog",
            "JavaFXGUI.getInstanceJavaFXGUI() called.");
        if (null == theJavaFXGUI) { // If not instantiated yet
          throw new IllegalStateException( // throw exception.
              "JavaFXGUI Instance not constructed yet!");
          }
        return theJavaFXGUI;
        }
    
    public void startJavaFXLaunchV()
    
      /* This method creates and starts a thread 
       * which launches the JavaFX sub-Application.
       * It must not be called until initialization method above,
       * initializeJavaFXGUI(.), is called.
       * 
       * It queues this job on the JavaFX runtime queue,
       * then it returns.
       * 
       * This method works by creating a Runnable containing a call to
       * the Application.launch(.) method, makes a Thread from that,
       * then starts the thread.
       * If the launch(.) method returns, it means 
       * the JavaFX GUI and JavaFX part of the app has closed,
       * so a complete shutdown is requested
       * which will shutdown the Swing GUI also.
       * 
       * This method initiates what is normally shown as being done by
       * the main(.) method in example JavaFX applications.
       * This method was created so that a JavaFX GUI 
       * could be developed in parallel to an existing Java Swing GUI,
       * both being part of the same app. 
       */
      {
        theAppLog.debug("JavaFXGUILog",
            "JavaFXGUI.startJavaFXLaunchV() begins.");
        Runnable javaFXRunnable= () -> { // Create launcher Runnable. 
            theAppLog.debug("JavaFXGUILog","javaFXRunnable.run() begins,"
              + " calling JavaFXApp.launch(JavaFXApp.class, (String[])null);.");
            JavaFXApp.launch( // Launch sub-App as JavaFX Application.
                JavaFXApp.class, (String[])null);
            theAppLog.debug("JavaFXGUILog","javaFXRunnable.run(), "
                + "returned from JavaFXApp.launch(.),"
                + "calling theShutdowner.requestAppShutdownV().");
            theShutdowner.requestAppShutdownV();
            theAppLog.debug("JavaFXGUILog","javaFXRunnable.run() "
                + "returned from theShutdowner.requestAppShutdownV(),"
                + "ending.");
            };
        Thread javaFXLauncherThread= // Create launcher thread from Runnable.
          new Thread(javaFXRunnable, "JavaFXLauncher");
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.startJavaFXLaunchV() calling "
            + "javaFXLauncherThread.start().");
        javaFXLauncherThread.start(); // Start launcher thread.
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.startJavaFXLaunchV() ends.");
        }
    
    public void nestedStartV()
      /* This method continues the launch begun by 
       * the Application subclass start(Stage) method.
       * It creates and starts one or more GUI Stages for the application, 
       * then exits.
       * 
       * This method is run on the JavaFX application thread. 
       */
      {
        TreeStage.makeInitializeAndStartV( // Start tree demo stage.
            this,theShutdowner);
        DemoStage.makeInitializeAndStartV( // Start button demo stage.
            this,theShutdowner);

        theAppLog.debug("JavaFXGUILog","JavaFXGUI.nestedStartV() begins,"
            + " calling new Navigation(.).initializeAndStartV().");
        new Navigation( // Create and start the main JavaFX UI Stage.
            theJavaFXGUI, 
            theRootDataNode, 
            thePersistent, 
            theDataRoot, 
            theSelections,
            theShutdowner
            ).initializeAndStartV();

        theAppLog.debug("JavaFXGUILog","JavaFXGUI.nestedStartV(), "
            + "returned from new Navigation(.).initializeAndStartV(), "
            + "ending.");

        // This method will now return to the Application subclass
        // JavaFXApp's start(Stage) method.
        }

    public void finalizeV()
      /* This method finalizes the JavaFX GUI
       * in preparation for app exit.  It is called during app shutdown.
       * 
       * This method works by hiding (closing) all showing JavaFX windows.
       * This is a signal to JavaFX to stop all GUI operations 
       * assuming other termination conditions are satisfied.  
       */
      {
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.JavaFXGUI.finalizeV() begins.");
        hideAllWindowsV();
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.JavaFXGUI.finalizeV() ends.");
        }

    public static void setDefaultStyle(Node theNode)
      /* Sets the default style of theNode. */
      {
        theNode.setStyle(
            "-fx-font-size: 22; -fx-font-family: \"monospace\"; ");
        }


    // JavaFX Window management.

    public void hideAllWindowsV()
      /* This method hides all windows.
       * This is used in preparation for shutdown.  
       * */
      {
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.hideAllWindowsV() begins.");
        for (String keyString : windowMap.keySet()) {
          Window theWindow= windowMap.get(keyString).getV1();
          theAppLog.debug("JavaFXGUILog","JavaFXGUI.hideAllWindowsV() "
              + "key= " + keyString + ", theWindow= " + theWindow);
          theWindow.hide();
          }
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.hideAllWindowsV() ends.");
        }

    public static void recordOpenWindowV(
        String theIDString, Window theWindow, Alert theAlert)
      /* This method records an opening (showing) of a new window.
       * It defines some parameters if they are null.
       * 
       * Note, there is no associated method for closing/hiding windows.
       * So windows will be reused even after being hidden.
       * ///enh Change this?
       */
      {
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.recordOpenWindowV() begins.");
        if (null == theWindow) theWindow= // Get Window from Alert if needed. 
            theAlert.getDialogPane().getScene().getWindow();
        if (null == theIDString) // Create ID String from Window if needed. 
          theIDString= theWindow.toString();
        JavaFXGUI.windowMap.put( // Record it all in map.
            theIDString, // The key.
            new BundleOf2<Window,Alert>(theWindow,theAlert) // The value.
            );
        theAppLog.debug("JavaFXGUILog","JavaFXGUI.recordOpenWindowV() ends.");
        }

    public static Alert testForAlert(String windowIDString)
      /* This method returns the Alert dialog object
       * associated with windowsIDString, or null if there is none.  
       */
      {
        Alert resultAlert= null;
        BundleOf2<Window,Alert> theBundleOf2= 
            JavaFXGUI.windowMap.get(windowIDString);
        if (null != theBundleOf2) resultAlert= theBundleOf2.getV2();
        return resultAlert; 
        }

    }
