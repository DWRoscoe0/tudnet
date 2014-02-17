package allClasses;

import java.io.IOException;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Enumeration;
//import java.util.Vector;

import javax.swing.event.EventListenerList;

import static allClasses.Globals.*;  // appLogger;

public class Shutdowner

  /* This is a Singleton class which does 
    all things associated with app shut-down.

    It does some things that ShutdownHook-s could do,
    but does them with ShutdownerListener-s instead,
    to avoid the uncertainties of ordering of execution.
    ShutdownerListener-s are fired in the reverse of
    the order in which they were added.

    It manages the ShutdownerListener list and
    calls each ShutdownerListener when shut down is underway.

    It collects argStrings to be passed to a new process
    before it shuts down.

    Presently this class adds a ShutdownerHook thread to 
    the app's Runtime as a way to detect when app shut down is underway.
    However this might be changed to detect shut-down in other ways,
    such as using the windowClosing(WindodwsEvent) method,
    because some features such the Logger might not be available
    after ShutdownHook-s start running.
    
    Note, if the app exits quickly without starting another Process,
    then this class might not be loaded.
    
   */

  {
  
    // Singleton code.
    
      private Shutdowner() {}  // Prevent instantiation by other classes.

      private static final Shutdowner theShutdowner =  // Build singleton.
        new Shutdowner();

      public Shutdowner getShutdowner()   // Return singleton.
        { return theShutdowner; }

    // Shutdown hook code.

      static // Static block initialization to setup for shutdown hook thread.
        {
          ShutdownerHook theShutdownerHook =  // Create ShutdownerHook Thread.
            new ShutdownerHook();
          Runtime.getRuntime().addShutdownHook(theShutdownerHook); // Setup...
            // ... to run that ShutdownerHook Thread at shut-down time.
          }
        
      static class ShutdownerHook  // For Runtime .getRuntime().addShutdownHook().
        extends Thread 
        /* This Thread is used to detect and process 
          the app shutdown when that time comes.  */
        {
          public void run()
            {
              doShutdown();
              }
          }
    
    // Shutdowner shutdown code.
    
      public static void doShutdown()
        /* This method is called when shut down is underway
          Its purpose is to perform app shutdown operations.
          It calls each of the ShutdownerListeners in the Listener list.
          After all Listeners have been called,
          it uses ProcessBuilder to create and start a new Process
          if the argStrings has been defined for one.
          */
        {
          appLogger.info( "ShutdownerHook running." );
          //System.out.println( "ShutdownerHook running." );

          fireShutdownerListeners( );  // Call all defined listeners.

          if  // Execute command if...
            ( argStrings != null ) // ...a command was defined.
            {
              //System.out.println(
              //  "Executing argStrings: "+
              //  Arrays.toString(argStrings)
              //  );
              callAProcess(argStrings);
              }
          }
    
    // ShutdownerListener code.
    
      private static EventListenerList theEventListenerList= 
        new EventListenerList();

      public static void addShutdownerListener
        ( ShutdownerListener listener ) 
        {
          theEventListenerList.add(ShutdownerListener.class, listener);
          }

      public static void removeShutdownerListener
        ( ShutdownerListener listener ) 
        {
          theEventListenerList.remove(ShutdownerListener.class, listener);
          }

      public static void fireShutdownerListeners( )
        // ??? Maybe reverse firing order?
        {
          for 
            ( ShutdownerListener aShutdownerListener: 
              theEventListenerList.getListeners(ShutdownerListener.class)
              )
            aShutdownerListener.doMyShutdown( );
          //System.out.println("SHUTDOWN");
        }

    // Code for defining and starting other processes and ending this one.

      private static String[] argStrings =  // Command to executed at exit.
        null; 

	    public static void setCommandV( String... inArgStrings )
	      /* This method sets to inArgStrings the array of Strings which
	        defines the command Process to be created and executed 
	        at shut-down time by ProcessBuilder.
	        If inArgStrings is null then no command will be executed.
	        */
	      {
          appLogger.info(
          	"Setting shutdown command of: " 
            + Arrays.toString(inArgStrings)
            );
          
	    	  argStrings = inArgStrings; 
	    	  }

      private static void callAProcess(String... inArgStrings)
          //throws IOException
          /* This method calls a Process built with 
            a ProessBuilder operating on 
            the String argument array inArgStrings.
            It redirects that Process's stdout and stderr to 
            this Process's stdout.
            Until this redirection ends it could cause an access violation
            which would prevent replacement of the file from which 
            this Process was loaded!
            */
          {
            try {
              appLogger.info( 
                "Starting Process with these Strings:" + Arrays.toString(inArgStrings)
                );
              ProcessBuilder MyProcessBuilder = 
                new ProcessBuilder(inArgStrings);
              
              //Process MyProcess= 
              MyProcessBuilder.start();

              /*
              // ??? Redirect in ProcessBuilder instread of Process?
              StreamConsumer errorConsumer = 
                new StreamConsumer(MyProcess.getErrorStream(),"stderr");
              StreamConsumer outputConsumer = 
                new StreamConsumer(MyProcess.getInputStream(),"stdout");
              errorConsumer.start();
              outputConsumer.start();

              try { // Wait until the output copying threads terminate.
                  Thread.sleep(2000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              */
              } catch (IOException e1) {
                e1.printStackTrace();
              }
              //appLogger.info( "Monitoring done.");  // Logger unreliable.
              //System.out.println( "Monitoring done.");
          }

    }
