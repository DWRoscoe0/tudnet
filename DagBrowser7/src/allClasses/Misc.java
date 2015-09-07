package allClasses;

import java.awt.Component;

import static allClasses.Globals.*;  // appLogger;

public class Misc

  /* This Singleton class contains miscellaneous useful code
    for debugging and logging.
    */

  { // class Misc 
  
    // Singleton code.
    
      private Misc()  // Private constructor prevents external instantiation.
        {}

      private static final Misc theMisc =  // Internal singleton builder.
        new Misc();

      public static Misc getMisc()   // Returns singleton.
        { return theMisc; }
  
    // Reminder variables.

      public static final boolean reminderB= true;


    /* Debugging code.
      This is code which is or was useful for debugging.
      */

      public static void noOp( ) {} // Convenience for setting breakpoints.

      public static void dbgProgress() 
        /* Use this method line to display a single character
          to verify that code is being executed.  
          */
        { 
          appLogger.appendRawV( "!" );  // Debug. 
          }
      
      public static String componentInfoString( Component InComponent )
        /* This method returns a string with 
          useful information about a component.
          */
        {
          if ( InComponent == null )
            return " null ";
            
          String ResultString= "";
          //ResultString+= " " + InComponent.hashCode(); 
          ResultString+= " " + InComponent.getClass().getName();
          if ( InComponent.getName() != null )
            ResultString+= " name:"+InComponent.getName(); 
          return ResultString;
          }
      
      private static boolean dbgEventDoneB= false;  // Set true to output Debug.txt.
      
      public static void dbgEventDone()
        /* This is a convenient place to output state information
          after a user input event is processed or 
          after any other significant event occurs.
          This is useful because the Java event loop is not accessible.
          */
        { 
        if ( dbgEventDoneB )
          {
            appLogger.appendEntry( "Writing Debug.txt disabled." ); 
            //MetaFileManager.writeDebugFileV( MetaRoot.getRootMetaNode( ));
            noOp( );
            }
          }
      
      public static void dbgConversionDone()
        /* This is called after a conversion from IDNumber to MetaNode.  */
        { 
          //appendEntry( "Conv." ); 
          //MetaFile.writeDebugState( );  // ??? Display for debugging.
          noOp( );
          }

    // Sleep/Snooze code.

      /* ??? Using LockAndSignal instead.
      public static void sleepForV(long intervalMsL) 
        /* This method uses Thread.sleep(..) to wait for intervalMsL milliseconds 
          but doesn't throw an InterruptedException if 
          the Thread is interrupted.
          However if an InterruptedException is thrown then
          it does return early with the Thread interrupted status set.
          */
      /* ??? 
        { 
          try {
            Thread.sleep(intervalMsL);  // Try sleeping the requested time.
            }
          catch (InterruptedException e) { // If interrupted...
            Thread.currentThread().interrupt(); // ...restore status.
            }
          }
      ??? */

      /* ??? Using LockAndSignal instead.
      public static void XsleepUntilV(long timeMsL) 
        /* This method uses Thread.sleep(..) to wait until time timeMsL
          but doesn't throw an InterruptedException if 
          the Thread is interrupted.
          However if an InterruptedException is thrown then
          it does return early with the Thread interrupted status set.
          */
      /* ???
        { 
      		long timeToEndOfSleepL= // Converting the real-time to a delay.
            timeMsL - System.currentTimeMillis();
          if ( timeToEndOfSleepL > 0 ) // Sleeping if delay is posotive.
            {  
		          try { // Waiting for notification or time-out.
		            Thread.sleep(  // Wait for call to notify() or...
		              timeToEndOfSleepL  // ...time of next scheduled job.
		              );
		            } 
		          catch (InterruptedException e) { // Handling sleep interrupt.
		            Thread.currentThread().interrupt(); // Re-establishing for tests.
		            }
              }
          }
      */

    } // class Misc 
