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

    Here are some notes codes (tags) to mark possible work to do
    or other information of information of interest.
    // : normal single line comment.
    //% : marks old code to be deleted.  Old.
    ///... : an issue that needs resolving, work of some type.
    ///ano : an anomaly for which there might or might not be mitigating code.
    ///que : question that need an answers.
    ///mys : a mystery, something that happens that needs an explanation.
    ///klu : kludge code created to deal with some mysterious behavior. 
    ///new : new code to be created or adapted.
    ///tmp : temporary code that should eventually be eliminated or changed.
    ///dbg : code used for debugging, for code or comment disabling code.
    ///fix : a problem that should be fixed.
    ///tst : code that needs to be [better] tested.
    ///pla : a planned change.
    ///pos : possible change.
    ///opt : an optimization change.
    ///org : an organizational enhancement.
    ///err : an error-checking enhancement
    ///enh : other miscellaneous enhancement.
    ///elim : code that might not be needed and may be eliminated.
    ///doc : documentation that should be fixed or improved.
    ///rev : to review for correctness. 
    //// (4 slashes) : old code, commended out, to be deleted soon.
    ////// (6 or more slashes) : very temporary something to do soon.
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

  		public static void requestFocusAndLogV(
  		    Component theComponent)
  		  /* This is a logging wrapper for requestFocusInWindow(),
  		    added mainly for debugging.  It logs when
  		    Component.requestFocusInWindow() returns false,
  		    unless the focus owner is null before Component.requestFocusInWindow()
  		    is called, which in this case a false is returned,
  		    even though the call probably will succeed.
  		    ///fix? Maybe another case should be checked, whether 
  		      the old and new components are the same.  
  		      This seems to be associated with failure. 
  		    */
	  		{
  		    toReturn: {
    			  Component focusedComponent= // Getting focus owner.
              KeyboardFocusManager.
                getCurrentKeyboardFocusManager().getFocusOwner();
            /// theAppLog.debug("Misc.requestFocusAndLogV() before request" 
        		///   +Misc.componentFocusInfoString(
        		///    NL+"  "+"owner:",focusedComponent)
        		///  +Misc.componentFocusInfoString(
        		///    NL+"  requesterowner:",focusedComponent) );

  		  	  boolean successB= theComponent.requestFocusInWindow();

      		  /// theAppLog.debug("Misc.requestFocusAndLogV() after request"
      		  ///     +", success="+successB
      		  ///     +Misc.componentFocusInfoString(
      		  ///       NL+"  "+"owner:",focusedComponent)
      		  ///     +Misc.componentFocusInfoString(
      		  ///       NL+"  requesterowner:",focusedComponent) );
  		  	  if (successB) break toReturn; // Exit if success.
            if (focusedComponent == null) // If no previous focus owner,
              ; ///  break toReturn; // exit.
            if (focusedComponent == theComponent) // Same components.
              { theAppLog.debug( Misc.componentFocusInfoString(
                   "Misc.requestFocusAndLogV(), both components are ",
                   theComponent));
                break toReturn; // This may be considered success.
                }
		    		{ // Reporting failure and possible clues about the causes.
							// theAppLog.warning( // Use this to get stack dump.
		    		  theAppLog.debug(
									"Misc.requestFocusAndLogV() of "+Misc.componentInfoString(theComponent) 
					    		+ NL + "  focus-owner="+Misc.componentInfoString(focusedComponent)
									+ " requestFocusInWindow() successB=" + successB
									);
		    		  boolean showPathB= false; // Make true to show path.
		    			while ( showPathB && (theComponent != null) ) {
		      			theAppLog.debug(
		      			    Misc.componentFocusInfoString(
		      			        "Misc.requestFocusAndLogV(): ",
		      			        theComponent
		      			        ));
		      			theComponent= theComponent.getParent();
		    				}
		    		  }
  		      } // toReturn:
	  			}

  		private static String componentFocusInfoString(
  		    String headerString, Component theComponent)
    		{
          return headerString +
            (
              (theComponent == null)
              ? " null"
              : " visible="+theComponent.isVisible() 
                +" focusable="+theComponent.isFocusable() 
                +" enabled="+theComponent.isEnabled()
                +" focus-owner="+(theComponent == KeyboardFocusManager.
                  getCurrentKeyboardFocusManager().getFocusOwner())
                +" "+Misc.componentInfoString(theComponent)
              );
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
