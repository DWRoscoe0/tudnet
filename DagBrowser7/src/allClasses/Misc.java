package allClasses;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;

import static allClasses.Globals.*;  // appLogger;

public class Misc

  /* This Singleton class contains miscellaneous useful code.
    The code here is usually either temporary or
    code useful for debugging and logging.
    
    Here are some notes to mark possible work to do:
    ///tmp : a temporary kluge, possibly to prevent an Exception.
    ///dbg : code used for debugging.
    ///fix : a problem to fix.
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

      public static void dbgProgress() 
        /* Use this method line to display a single character
          to verify that code is being executed.  
          */
        { 
          appLogger.appendV( "!" );  // Debug. 
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
	            appLogger.logEntryV( "Writing Debug.txt disabled." ); 
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

	    public static String fileDataString( File theFile )
	    	{ 
	    	  return ( theFile == null )
	    	  		? "null" 
	    	  		: dateString( theFile ) + ", " + theFile.toString()
	    	  		;
	    		}

	    public static String dateString( File theFile )
	      /* This method returns the time-stamp String associated with
	        the file theFile in an easy to read format.
	        */
	      {
	    		return dateString( theFile.lastModified() );
	        }


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

		  public static void updateFromToV(File thisFile, File thatFile)
		    /* If thisFile is newer than thatFile, then
		      thatFile is replaced by thisFile by copying.
		      The time-stamp of the written file is updated
		      In the end, thisFile will equal thatFile,
		      in all ways except for their path-names.
		      NoSuchFileException happens if the destination folder does not exist.
		      
		      ///fix Does copy need to be atomic?  It is not.
		      */
		    {
	        long thisFileLastModifiedL= thisFile.lastModified();
	        long thatFileLastModifiedL= thatFile.lastModified();
	        if // This file is newer than that file replace that with this.
	          ( thisFileLastModifiedL > thatFileLastModifiedL )
				    try {
					      appLogger.info( "updateFromToV(..) Copying started."
					      		+ "\n  This file = " + thisFile
					      		+ "\n  That file = " + thatFile
					      		);
					      Files.copy(
						      thisFile.toPath()
						      ,thatFile.toPath()
						      ,StandardCopyOption.COPY_ATTRIBUTES
						      ,StandardCopyOption.REPLACE_EXISTING
						      //,StandardCopyOption.ATOMIC_MOVE is illegal.
						      );
							  appLogger.info( "updateFromToV(..) Copying finished." );
					  } catch (Exception e) { 
					    appLogger.info( 
					      "updateFromToV(..) failed, \n  "
					      +e.toString()
					      ); 
					    }
					}

  	} // class Misc 
