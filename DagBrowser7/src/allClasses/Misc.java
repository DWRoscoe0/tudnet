package allClasses;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;

import static allClasses.Globals.*;  // appLogger;

public class Misc

  /* This Singleton class contains miscellaneous useful code.
    The code here is usually either temporary or
    code useful for debugging and logging.
    Temporary code sometimes becomes permanent and
    is moved to its own classes.
    
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
	            appLogger.logV( "Writing Debug.txt disabled." ); 
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

	    // File copying and updating.
	    
		  public static void updateFromToV(File thisFile, File thatFile)
		    /* If thisFile is newer than thatFile, then
		      thatFile is replaced by thisFile by copying.
		      The time-stamp of the written file is updated
		      In the end, thisFile will equal thatFile,
		      in all ways except for their path-names.
		      NoSuchFileException happens if 
		      the destination folder does not exist.
		      
		      Copying will be aborted if the thread is terminated.
		      In that case the threads Interrupted status will be set.
		      
		      This method does not update in the reverse direction.
		      */
		    {
	        long thisFileLastModifiedL= thisFile.lastModified();
	        long thatFileLastModifiedL= thatFile.lastModified();
          boolean updatingB= // Is this file is newer than that file?
              ( thisFileLastModifiedL > thatFileLastModifiedL );
          appLogger.info("updateFromToV((..), updatingB="+updatingB);
	        if ( updatingB )  // Then replace that with this.
	          copyFileWithRetryV(thisFile, thatFile);
					}

      public static void copyFileWithRetryV(
          File sourceFile, File destinationFile)
        /* This method copies the sourceFile to the destinationFile.
          It does not return until the copy succeeds or 
          thread termination is requested.
          It assumes that a copy failure due to an IOException
          was caused by a temporary condition such as 
          the destination file being open for reading. 
          */
        {
          ///enh create function to make double file path string.
          appLogger.info("copyFileWithRetryV(..) "
            + "======== COPYING FILE ======== "
            + twoFilesString(sourceFile, destinationFile));
          boolean copySuccessB= false;
          int attemptsI= 0;
          while (true) {
            if (EpiThread.testInterruptB()) { // Termination requested.
              appLogger.info( true,"copyFileWithRetryV(..) terminating.");
              break; }
            attemptsI++;
            copySuccessB= Misc.tryCopyFileB(sourceFile, destinationFile);
            if (copySuccessB) { // Copy completed.
              appLogger.info( "copyFileWithRetryV(..) "
                  + "Copying successful on attempt #"+attemptsI+" "
                  + twoFilesString(sourceFile, destinationFile));
              break; }
            appLogger.info("copyFileWithRetryV(..) failed attempt #"+attemptsI
              +".  Will retry after 1 second."); 
            EpiThread.interruptibleSleepB(
                Config.fileCopyRetryPause1000MsL); // Wait 1 second.
            }
          }

      public static boolean tryCopyFileB(
          File sourceFile, File destinationFile)
        /* This method tries to copy the sourceFile to the destinationFile.
          It returns true if the copy succeeds, false otherwise.
          It logs any exception which causes failure.
          */
        {
          appLogger.debug("tryCopyFileB(..) begins");
          File tmpFile= null;
          boolean successB= false; // Assume we will not be successful.
          toReturn: {
            tmpFile= createTemporaryFile("Copy");
            if (tmpFile == null) break toReturn;
            Path sourcePath= sourceFile.toPath();
            Path tmpPath= tmpFile.toPath();
            if (!interruptibleTryCopyFileB(sourceFile,tmpFile)) 
              break toReturn;
            if (!copyTimeAttributesB(sourcePath,tmpPath)) 
              break toReturn;
            if (!atomicRenameB(tmpFile.toPath(), destinationFile.toPath())) 
              break toReturn;
            successB= true;
            } // toReturn:
          deleteDeleteable(tmpFile); // Delete possible temporary file debris.
          appLogger.info("tryCopyFileB(..) copySuccessB="+successB);
          return successB;
          }

      
      public static boolean atomicRenameB(
          Path sourcePath, Path destinationPath)
        /* This method atomically renames a file.
          It is what is used to safely replace one file with another.
          It returns true if the rename succeeds, false otherwise.
          If it encounters an AccessDeniedException, it will retry,
          assuming the exception was caused by the destination file
          being protected because it is temporarily open for reading.
          
          ///fix Somehow eliminate retry-polling on rename failure.
          */
        {
          boolean successB= false;
          int attemptsI= 0;
          appLogger.info( "atomicRenameB(..) begins"
              + twoFilesString(sourcePath.toFile(), destinationPath.toFile()));
          while (!EpiThread.testInterruptB()) { // Retry some failure types. 
            attemptsI++;
            try {
                Files.move(sourcePath, destinationPath,
                    StandardCopyOption.ATOMIC_MOVE);
                successB= true;
                break;
              } catch (AccessDeniedException theAccessDeniedException) {
                appLogger.warning(
                    "atomicRenameB(..) failed attempt "+attemptsI
                    +", retrying, "+theAccessDeniedException); 
              } catch (IOException theIOException) {
                appLogger.exception(
                    "atomicRenameB(..) failed with ",theIOException); 
                break;
              }
            EpiThread.interruptibleSleepB( // Don't hog CPU in retry loop.
                Config.errorRetryPause1000MsL
                );
            } // while
          appLogger.info("atomicRenameB(..) ends, successB="+successB
              +" after "+attemptsI+" attempts.");
          return successB;
          }

      public static File createTemporaryFile(String nameString)
        /* This method tries to create a temporary file which contains
          nameString as part of the name and in the app folder.
          It logs any exceptions produced.
          It returns the File of the temporary file created if successful,
          or null if it failed for any reason.
          */
        {
          File tmpFile= null;
          try {
              tmpFile= File.createTempFile( // Creates empty file.
                nameString,null,AppSettings.userAppFolderFile);
            } catch (IOException theIOException) {
              appLogger.exception(
                  "createTemporaryFile(..) failed with",theIOException); 
            }
          return tmpFile;
          }

      private static boolean interruptibleTryCopyFileB(
          File sourcesourceFile, File destinationFile) 
        /* This method tries to copy the sourceFile to the destinationFile.
          If there is an error, the copy fails.
          If the copy is interrupted, the copy fails.
          This method returns true if the copy succeeds, false otherwise.
          */
        {
          appLogger.info("interruptibleTryCopyFileB(..) begins.");
          InputStream theInputStream= null;
          OutputStream theOutputStream= null;
          boolean successB= false;
          try {
              theInputStream= new FileInputStream(sourcesourceFile);
              theOutputStream= new FileOutputStream(destinationFile);
              successB= copyStreamBytesB(theInputStream,theOutputStream);
            } catch (Exception e) {
                appLogger.exception("interruptibleTryCopyFileB(..)",e); 
            } finally { // Close things, error or not.
              Closeables.closeWithErrorLoggingB(theInputStream);
              Closeables.closeWithErrorLoggingB(theOutputStream);
                // Closing the OutputStream can block temporarily.
            }
          appLogger.info("interruptibleTryCopyFileB(..) ends, "
              +"closes done, successB="+successB);
          return successB;
          }
      
      public static boolean copyStreamBytesB( 
          InputStream theInputStream, OutputStream theOutputStream)
        /* This method copies all [remaining] bytes
          from theInputStream to theOutputStream.
          The streams are assumed to be open at entry 
          and they will remain open at exit.
          It returns true if the copy of all data finished, 
          false if it does not finish for any reason.
          A Thread interrupt will interrupt the copy.
          */
        {
          appLogger.info("copyStreamBytesV(..) begins.");
          long startTimeMsL= System.currentTimeMillis();
          int byteCountI= 0;
          boolean successB= false; // Assume we fill fail.
          try {
            byte[] bufferAB= new byte[1024];
            int lengthI;
            while (true) {
              lengthI= theInputStream.read(bufferAB);
              if (lengthI <= 0) // Transfer completed.
                { successB= true; break; } // Indicate success and exit loop.
              theOutputStream.write(bufferAB, 0, lengthI);
              byteCountI+= lengthI;
              if (EpiThread.testInterruptB()) { // Thread interruption.
                appLogger.info(true, 
                    "copyStreamBytesV(..) interrupted");
                break; // Exit loop without success.
                }
              }
            } 
          catch (IOException theIOException) {
            if (EpiThread.testInterruptB()) // Thread interruption.
              appLogger.info(
                "copyStreamBytesV(..) interrupted with "+theIOException);
              else
                appLogger.exception("copyStreamBytesV(..)",theIOException);
            }
          appLogger.info( "copyStreamBytesV() successB="+successB
              +", bytes transfered=" + byteCountI
              +", elapsed ms=" + (System.currentTimeMillis()-startTimeMsL));
          return successB;
          }

    public static boolean copyTimeAttributesB(
        Path sourcePath, Path destinationPath)
      /* This method copies the 3 time attributes from the source file
        to the destination file.
        It returns true if successful, false otherwise.
        */
      {
        boolean successB= false;
        try {
          BasicFileAttributeView sourceBasicFileAttributeView= 
              Files.getFileAttributeView(
                  sourcePath,BasicFileAttributeView.class);
          BasicFileAttributes sourceBasicFileAttributes= 
              sourceBasicFileAttributeView.readAttributes();

          FileTime theLastModifiedTime= 
              sourceBasicFileAttributes.lastModifiedTime();
          FileTime theLastAccessTime= 
              sourceBasicFileAttributes.lastAccessTime();
          FileTime theCreateTime= 
              sourceBasicFileAttributes.creationTime();
          appLogger.info( "copyTimeAttributesV(..): "
              +theLastModifiedTime+", "+theLastAccessTime+", "+theCreateTime);

          BasicFileAttributeView destinationBasicFileAttributeView= 
              Files.getFileAttributeView(
                  destinationPath,BasicFileAttributeView.class);

          destinationBasicFileAttributeView.setTimes(
              theLastModifiedTime, theLastAccessTime, theCreateTime);
          successB= true;
        } catch (IOException theIOException) {
          appLogger.exception(
              "copyTimeAttributesB(..) failed with",theIOException); 
        }
      return successB;
      }

    public static void touchV(File theFile, long timeStampL) 
      /* This method sets theFile's LastModified time to timeStampL. 
        It was created mainly to trigger update file copies for testing.  */
      {
        theFile.setLastModified(timeStampL);
        }   

    public static void touchV(File theFile)
      /* This method sets theFile's LastModified time to the present time. 
        It was created mainly to trigger update file copies for testing.  */
      {
        long timestamp = System.currentTimeMillis();
        theFile.setLastModified(timestamp);
        }   

    public static void deleteDeleteable(File tmpFile)
      {
        if (tmpFile != null) tmpFile.delete();
        }
    
    public static String twoFilesString(
        File sourceFile, File destinationFile)
    {
      return 
          "\n    sourceFile:      " + Misc.fileDataString(sourceFile) + 
          "\n    destinationFile: " + Misc.fileDataString(destinationFile)
          ;
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

  } // class Misc 
