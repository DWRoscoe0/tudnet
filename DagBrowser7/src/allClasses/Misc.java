package allClasses;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.text.SimpleDateFormat;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class Misc

  /* This Singleton class contains miscellaneous useful code.
    The code here is usually either temporary or
    code useful for debugging and logging.
    Temporary code sometimes becomes permanent and
    is moved to its own classes.
    ///org Code needs moving now.
    
    Here are some notes to mark possible work to do:
    ///tmp : temporary code, for debugging or a development kluge.
    ///dbg : code used for debugging, for code or comment disabling code.
    ///fix : a problem to fix.
    ///tst : code that needs to be [better] tested.
    ///pla : a planned change.
    ///pos : possible change.
    ///opt : an optimization change.
    ///org : an organizational enhancement.
    ///err : error checking enhancement
    ///enh : other miscellaneous enhancement.
    ///elim : code that might not be needed and may be eliminated.
    ///doc : fix documentation.
    ///rev : to review for correctness. 
    //% : marks old code to be deleted.  Old.
    //// (4 or more slashes) : very temporary something else to do.
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
	            theAppLog.logV( "Writing Debug.txt disabled." ); 
	            //MetaFileManager.writeDebugFileV( MetaRoot.getRootMetaNode( ));
	            noOp( );
	            }
          }
      
      
    // Logging and logging utility code.
      
      public static void dbgConversionDone()
        /* This is called after a conversion from IDNumber to MetaNode.  */
        { 
          //appendEntry( "Conv." ); 
          //MetaFile.writeDebugState( );  // Display for debugging.
          noOp( );
          }
      
      public static String componentInfoString( Component InComponent )
        /* This method returns a string with 
          useful information about a component.
          */
        {
          if ( InComponent == null )
            return " null ";
            
          String ResultString= "";
          ResultString+= InComponent.hashCode(); 
          ResultString+= "/" + InComponent.getClass().getName();
          if ( InComponent.getName() != null )
            ResultString+= " name:"+InComponent.getName(); 
          return ResultString;
          }

  		public static void requestFocusV(Component theComponent)
  		  /* This is a logging wrapper for requestFocusInWindow(),
  		    added mainly for debugging.  It logs when
  		    Component.requestFocusInWindow() returns false,
  		    unless the focus owner is null before Component.requestFocusInWindow()
  		    is called, which in this case a false is returned,
  		    even though the call probably will succeed.
  		    */
	  		{
  			  Component focusedComponent= // Getting focus owner.
            KeyboardFocusManager.
              getCurrentKeyboardFocusManager().getFocusOwner();
		  	  boolean successB= theComponent.requestFocusInWindow();
		  		if (  // Logging any definite focus failures,
		  				!successB // when request fails,
		  				&& focusedComponent!=null // and there was a focus owner.
		  				)
		    		{ // Reporting failure and possible causes.
							theAppLog.warning(
									"Misc.requestFocusV() of "+Misc.componentInfoString(theComponent) 
					    		+ NL + "  previous focus-owner="+Misc.componentInfoString(focusedComponent)
									+ " requestFocusInWindow() successB=" + successB
									);
		    			while ( theComponent != null ) {
		      			theAppLog.debug(
		    		    		" ancestor: visible="+theComponent.isVisible() 
		    		    		+" focusable="+theComponent.isFocusable() 
		    		    		+" enabled="+theComponent.isEnabled()
                    +" "+Misc.componentInfoString(theComponent)
		      					);
		      			theComponent= theComponent.getParent();
		    				}
		    		  }
	  			}

  	  
      // Exception handling and logging.

      public static void logAndRethrowAsRuntimeExceptionV( 
        String aString, Throwable theThrowable 
        )
      /* Writes an error to the log, then
        Throws a RuntimeException wrapped around theThrowaable.
        This method should never be called, except maybe during debugging.
        */
      {
        theAppLog.exception( "Misc.logAndRethrowAsRuntimeExceptionV(..)"
            + aString + ":" + NL + "  ", theThrowable );
        throw new RuntimeException( theThrowable );
        }

    
    // Miscellaneous.
    
    public static String dateString( long theL )
      /* This method returns the a time-stamp theL converted to a String.
        */
      {
        SimpleDateFormat aSimpleDateFormat= 
          new SimpleDateFormat("yyyyMMdd.HHmmss");
        String resultDateString= 
          theL + " aka " +
          aSimpleDateFormat.format( theL );
        return resultDateString;
        }

  } // class Misc 
