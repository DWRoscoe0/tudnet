package allClasses;

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

import static allClasses.Globals.*;  // appLogger;

public class FileOps

  {
  
    /* This class contains code useful for operating on files.
      This class was created by moving file-related code from the class Misc.
      */
    
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
          copySuccessB= tryCopyFileB(sourceFile, destinationFile);
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
              appLogger.exception("atomicRenameB(..) failed with ",theIOException); 
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
              { successB= true; break; } // Record success and exit loop.
            theOutputStream.write(bufferAB, 0, lengthI);
            byteCountI+= lengthI;
            if (EpiThread.testInterruptB()) { // Thread interruption.
              appLogger.info(true, 
                  "copyStreamBytesV(..) terminated by interrupted");
              break; // Exit loop without success.
              }
            }
          } 
        catch (IOException theIOException) {
          if (EpiThread.testInterruptB()) // Thread interruption.
            appLogger.info(
              "copyStreamBytesV(..) interrupted plus "+theIOException);
            else
              appLogger.exception("copyStreamBytesV(..) terminated by",theIOException);
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
            NL + "    sourceFile:      " + fileDataString(sourceFile) + 
            NL + "    destinationFile: " + fileDataString(destinationFile)
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
        return Misc.dateString( theFile.lastModified() );
        }

    }
