package allClasses;

import java.io.IOException;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Enumeration;
//import java.util.Vector;

import javax.swing.event.EventListenerList;

import static allClasses.Globals.*;  // appLogger;

public class Shutdowner

  /* This is a Singleton class which manages
    things that are done during app shut-down.
    It does 3 things:

    1. It manages a list of ShutdownerListeners 
      which are called in a controllable order
      at shutdown time.  This is an alternative to
      Java's ShutdownHook threads, which execute in 
      uknown order.  The purpose for these 
      Listenersis and threads is cleanup.
    
    2. It creates and starts a user-definable Process
      just before this app terminates.
      An intended use for this is app updating.

    3. It detects when shutdown is happening in order to
      start the above operations.

    ??? The above functions should probably be separated
    with each function into its own class.

    ??? Singletons generally, and this one in particular,
    are globals, which are discouraged for various reasons,
    such as making testing difficult.  
    Their use should be limited or tempoaray.
    If they must be used, they should be non-global,
    and referenced with DependencyInjection.
    
   */

  {
    
    // Shutdowner shutdown code.
    
      public void doShutdown()
        /* This method is called in the ShutdownHook thread
          when shut down is underway
          Its purpose is to perform app shutdown operations,
          of which there are two, which it does in the following order:
          
          1. It calls each of the ShutdownerListeners in the Listener list.
            It does this in the reverse of the order they were added.
          
          2. It uses ProcessBuilder to create and start a new Process.
            if the argStrings has been defined for one.
            This is for chaining from this app instance a newer one.

          Parts of the app might have set other ShutdownHooks
          to run their own shutdown code not included here.
          */
        {
          appLogger.info( "Shutdowner: shutdown beginning." );
          //System.out.println( "Shutdowner running." );

          //fireShutdownerListeners();  // Call all defined listeners.
          reverseFireShutdownerListeners();  // Call all listeners in reverse.

          // At this point there should be nothing remaining for the app
          //  to do except start the next app as an external command
          //  and terminate.

          if  // Execute an external command if...
            ( argStrings != null ) // ...a command was defined.
            {
              //System.out.println(
              //  "Executing argStrings: "+
              //  Arrays.toString(argStrings)
              //  );
              callAProcess(argStrings);
              }

          appLogger.info( "Shutdowner: shutdown ending." );
          }
    
    // ShutdownerListener code.  Maintains and calls ShutdownListeners.
    
      private EventListenerList theEventListenerList= 
        new EventListenerList();

      public synchronized void addShutdownerListener
        ( ShutdownerListener listener ) 
        {
          theEventListenerList.add(ShutdownerListener.class, listener);
          }

      public synchronized void removeShutdownerListener
        ( ShutdownerListener listener ) 
        {
          theEventListenerList.remove(ShutdownerListener.class, listener);
          }

      /*
      private synchronized void fireShutdownerListeners( )
        // Fire listeners in the same order they were added.
        {
          for 
            ( ShutdownerListener aShutdownerListener: 
              theEventListenerList.getListeners(ShutdownerListener.class)
              )
            aShutdownerListener.doMyShutdown( );
        }
      */

      private synchronized void reverseFireShutdownerListeners( )
        // Fire listeners in the reverse of the order they were added.
        {
          ShutdownerListener theShutdownerListeners[]=
            theEventListenerList.getListeners(ShutdownerListener.class);
          for (int i = theShutdownerListeners.length-1; i>=0; i-=1)
            { 
              ShutdownerListener aShutdownerListener= 
                theShutdownerListeners[i];
              aShutdownerListener.doMyShutdown( );
              }
        }

    // Code for defining and starting other processes and ending this one.

      private String[] argStrings =  // Command to executed at exit.
        null; 

	    public void setCommandV( String... inArgStrings )
	      /* This method sets to inArgStrings the array of Strings which
	        defines the command Process to be created and executed 
	        at shut-down time by ProcessBuilder.
	        If at shutdown time inArgStrings is null 
          then no command will be executed.
	        */
	      {
          appLogger.info(
          	"Setting shutdown command of: " 
            + Arrays.toString(inArgStrings)
            );
          
	    	  argStrings = inArgStrings; 
	    	  }

      private void callAProcess(String... inArgStrings)
          /* This method calls a Process built with 
            a ProessBuilder operating on 
            the String argument array inArgStrings.
            
            ??? This could use some work.
            In previous version it redirected 
            the Process's stdout and stderr to 
            this Process's stdout.
            Until this redirection ended it could cause an access violation
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
              
              MyProcessBuilder.start();

              } catch (IOException e1) {
                e1.printStackTrace();
              }
              //appLogger.info( "Monitoring done.");  // Logger unreliable.
              //System.out.println( "Monitoring done.");
          }

    }
