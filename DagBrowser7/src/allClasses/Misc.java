package allClasses;

import java.awt.Component;
import java.awt.KeyboardFocusManager;

import static allClasses.Globals.*;  // appLogger;

public class Misc

  /* This Singleton class contains miscellaneous useful code.
    The code here is usually either temporary or
    code useful for debugging and logging.
    
    /////////////////////////////////// Need a better way of
    noting and prioritizing things-to-do in the code.
    ? //% to mark old code to be deleted.
    ? //.... to mark other things to do,
      with the number of slashes indicating the priority.
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
  		    unless the focus owner null before Component.requestFocusInWindow()
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
							appLogger.warning(
									"DagBrowserPanel.requestFocusV() of "+Misc.componentInfoString(
											theComponent
											) 
					    		+" focus-owner="+Misc.componentInfoString(focusedComponent)
									+ " requestFocusInWindow()=" + successB
									);
		    			while ( theComponent != null ) {
		      			appLogger.warning(
		      					"  "+Misc.componentInfoString(theComponent)
		    		    		+" visible="+theComponent.isVisible() 
		    		    		+" focusable="+theComponent.isFocusable() 
		    		    		+" enabled="+theComponent.isEnabled()
		      					);
		      			theComponent= theComponent.getParent();
		    				}
		    		  }
	  			}

    } // class Misc 
