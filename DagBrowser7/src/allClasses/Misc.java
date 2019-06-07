package allClasses;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
		      
		      ////fix Does copy need to be atomic?  It is not.
		      */
		    {
	        long thisFileLastModifiedL= thisFile.lastModified();
	        long thatFileLastModifiedL= thatFile.lastModified();
	        if // This file is newer than that file then replace that with this.
	          ( thisFileLastModifiedL > thatFileLastModifiedL )
	          copyFileWithRetryV(thisFile, thatFile);
	          /*  ////
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
            */  ////
					}

      public static void copyFileWithRetryV(
          File sourceFile, File destinationFile)
        /* This method copies the sourceFile to the destinationFile.
          It does not return until the copy succeeds.
           */
        {
          ///enh create function to make double file path string.
          appLogger.info("copyFileWithRetryV(..) "
            + "======== COPYING FILE ======== "
            + twoFilesString(sourceFile, destinationFile));
          boolean copySuccessB= false;
          int attemptsI= 0;
          while (true) {
            if (EpiThread.exitingB()) { // Termination requested.
              appLogger.info( "copyFileWithRetryV(..) terminated.");
              break; }
            attemptsI++;
            copySuccessB= Misc.tryCopyFileB(sourceFile, destinationFile);
            if (copySuccessB) { // Copy completed.
              appLogger.info( "copyFileWithRetryV(..) "
                  + "Copying successful after "+attemptsI+" attempts:"
                  + twoFilesString(sourceFile, destinationFile));
              break; }
            appLogger.info("copyFileWithRetryV(..) failed attempt "+attemptsI
              +".  Will retry after 1 second."); 
            EpiThread.interruptableSleepB(
                Config.fileCopyRetryPause1000MsL); // Wait 1 second.
            }
          }

      public static boolean tryCopyFileB(
          File sourceFile, File destinationFile)
        /* This method tries to copy the sourceFile to the destinationFile.
          It returns true if the copy succeeds, false otherwise.
          It logs any exception which causes failure.
          
          ///fix It uses Files.copy(..), which unfortunately 
            is not interruptible.  A replacement is being created.
          */
        {
          File tmpFile= null;
          boolean copySuccessB= false; // Assume we will not be successful.
          while  // Keep trying until copy success or exit requested.
            (!EpiThread.exitingB() && !copySuccessB)
            try {
                tmpFile= File.createTempFile( // Creates empty file.
                  "Infogora",null,AppSettings.userAppFolderFile);
                Path sourcePath= sourceFile.toPath();
                Path tmpPath= tmpFile.toPath();
                //// copyTimeAttributesV(sourceFile.toPath(),tmpFile.toPath());
                //// Files.copy( sourceFile.toPath()
                 ////     ,tmpFile.toPath()
                 ////     ,StandardCopyOption.COPY_ATTRIBUTES
                 ////     ,StandardCopyOption.REPLACE_EXISTING
                 ////     );
                interruptableTryCopyFileV(sourceFile,tmpFile);
                copyTimeAttributesV(sourcePath,tmpPath);
                appLogger.info(
                    "tryCopyFileB(..) atomically renaming temp file.");
                Files.move(tmpFile.toPath(), destinationFile.toPath() 
                    ,StandardCopyOption.ATOMIC_MOVE);  
                copySuccessB= true;
                }
            //// catch (InterruptedException e) {
             ////   Thread.currentThread().interrupt();
             ////     }
            catch (Exception e) { // Other instance probably still running.
                appLogger.exception("tryCopyFileB(..) ",e); 
                copySuccessB= false; // Record failure to cause retry.
                deleteDeleteable(tmpFile); // Delete lost temporary file.
                }
          appLogger.info("tryCopyFileB(..) copySuccessB="+copySuccessB);
          return copySuccessB;
          }

      private static void deleteDeleteable(File tmpFile)
        {
          if (tmpFile != null) tmpFile.delete();
          }

      private static void interruptableTryCopyFileV(
          File sourcesourceFile, File destinationFile) 
        throws InterruptedException, IOException
        /* This method tries to copy the sourceFile to the destinationFile.
          If the copy is interrupted, the copy fails,
          and an InterruptedException is thrown.
          If there is an error, the copy fails,
          and an IOException is thrown.
          */
        {
          appLogger.info("interruptableTryCopyFileV(..) begins.");
          InputStream theInputStream= null;
          OutputStream theOutputStream= null;
          try {
              theInputStream= new FileInputStream(sourcesourceFile);
              theOutputStream= new FileOutputStream(destinationFile);
              byte[] bufferAB= new byte[1024];
              int lengthI;
              while (true) {
                lengthI= theInputStream.read(bufferAB);
                if (lengthI <=  0) break; // Copy done.
                theOutputStream.write(bufferAB, 0, lengthI);
                if (EpiThread.interruptingB()) 
                  throw new InterruptedException("copy interrupted");
                }
            } finally {
              Closeables.closeWithErrorLoggingB(theInputStream);
              Closeables.closeWithErrorLoggingB(theOutputStream);
              appLogger.info("interruptableTryCopyFileV(..) finally ends.");
            }
          }

    public static void copyTimeAttributesV(
        Path sourcePath, Path destinationPath)
      throws IOException
      /* This method copies the 3 time attributes from the source file
        to the destination file.
        */
      {
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

          //// System.out.format("Size:%s bytes %n", sourceBasicFileAttributes.size());
          //// System.out.format("Creation  Time:%s %n", sourceBasicFileAttributes.creationTime());
          //// System.out.format("Last Access  Time:%s %n", sourceBasicFileAttributes.lastAccessTime());

          BasicFileAttributeView destinationBasicFileAttributeView= 
              Files.getFileAttributeView(
                  destinationPath,BasicFileAttributeView.class);
          //// BasicFileAttributes destinationBasicFileAttributes= 
          ////     destinationBasicFileAttributeView.readAttributes();

          destinationBasicFileAttributeView.setTimes(
              theLastModifiedTime, theLastAccessTime, theCreateTime);
        } finally { ////  catch (IOException e) {
          //// e.printStackTrace();
        }        
      } /////
    
    public static String twoFilesString(
        File sourceFile, File destinationFile)
    {
      return 
         //// "\n  sourceFile= "+sourceFile
         //// +"\n  destinationFile= "+destinationFile
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
