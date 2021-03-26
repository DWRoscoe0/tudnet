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

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class FileOps

  {
  
    /* This class contains code useful for operating on files.
      ///org It should probably be organized by grouping related methods.
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
        theAppLog.info("updateFromToV((..), updatingB="+updatingB);
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
        theAppLog.info("copyFileWithRetryV(..) "
          + "======== COPYING FILE ======== "
          + twoFilesString(sourceFile, destinationFile));
        boolean copySuccessB= false;
        int attemptsI= 0;
        while (true) {
          if (EpiThread.testInterruptB()) { // Termination requested.
            theAppLog.info( true,"copyFileWithRetryV(..) terminating.");
            break; }
          attemptsI++;
          copySuccessB= tryCopyFileB(sourceFile, destinationFile);
          if (copySuccessB) { // Copy completed.
            theAppLog.info( "copyFileWithRetryV(..) "
                + "Copying successful on attempt #"+attemptsI+" "
                + twoFilesString(sourceFile, destinationFile));
            break; }
          theAppLog.info("copyFileWithRetryV(..) failed attempt #"+attemptsI
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
        theAppLog.debug("tryCopyFileB(..) begins");
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
        theAppLog.info("tryCopyFileB(..) copySuccessB="+successB);
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
        theAppLog.info( "atomicRenameB(..) begins"
            + twoFilesString(sourcePath.toFile(), destinationPath.toFile()));
        while (!EpiThread.testInterruptB()) { // Retry some failure types. 
          attemptsI++;
          try {
              Files.move(sourcePath, destinationPath,
                  StandardCopyOption.ATOMIC_MOVE);
              successB= true;
              break;
            } catch (AccessDeniedException theAccessDeniedException) {
              theAppLog.warning(
                  "atomicRenameB(..) failed attempt "+attemptsI
                  +", "+theAccessDeniedException);
              if (attemptsI >= 10) {
                theAppLog.error("atomicRenameB(..) retry limit exceeded, aborting.");
                break;
                }
            } catch (IOException theIOException) {
              theAppLog.exception("atomicRenameB(..) failed with ",theIOException); 
              break;
            }
          EpiThread.interruptibleSleepB( // Pause thread to prevent CPU hogging.
              Config.errorRetryPause1000MsL
              );
          } // while
        theAppLog.info("atomicRenameB(..) ends, successB="+successB
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
            theAppLog.exception(
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
        theAppLog.info("interruptibleTryCopyFileB(..) begins.");
        InputStream theInputStream= null;
        OutputStream theOutputStream= null;
        boolean successB= false;
        try {
            theInputStream= new FileInputStream(sourcesourceFile);
            theOutputStream= new FileOutputStream(destinationFile);
            successB= copyStreamBytesB(theInputStream,theOutputStream);
          } catch (Exception e) {
              theAppLog.exception("interruptibleTryCopyFileB(..)",e); 
          } finally { // Close things, error or not.
            Closeables.closeWithErrorLoggingB(theInputStream);
            Closeables.closeWithErrorLoggingB(theOutputStream);
              // Closing the OutputStream can block temporarily.
          }
        theAppLog.info("interruptibleTryCopyFileB(..) ends, "
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
        theAppLog.info("copyStreamBytesV(..) begins.");
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
              theAppLog.info(true, 
                  "copyStreamBytesV(..) terminated by interrupted");
              break; // Exit loop without success.
              }
            }
          } 
        catch (IOException theIOException) {
          if (EpiThread.testInterruptB()) // Thread interruption.
            theAppLog.info(
              "copyStreamBytesV(..) interrupted plus "+theIOException);
            else
              theAppLog.exception("copyStreamBytesV(..) terminated by",theIOException);
          }
        theAppLog.info( "copyStreamBytesV() successB="+successB
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
          theAppLog.info( "copyTimeAttributesV(..): "
              +theLastModifiedTime+", "+theLastAccessTime+", "+theCreateTime);
    
          BasicFileAttributeView destinationBasicFileAttributeView= 
              Files.getFileAttributeView(
                  destinationPath,BasicFileAttributeView.class);
    
          destinationBasicFileAttributeView.setTimes(
              theLastModifiedTime, theLastAccessTime, theCreateTime);
          successB= true;
        } catch (IOException theIOException) {
          theAppLog.exception(
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

    public final static String requiredConfirmationString= 
        "I AM CERTAIN ABOUT THIS!"; 

    public static String deleteRecursivelyReturnString(
        File theFile,String confirmationString) 
      //// throws IOException 
      {
        String errorString= null;
      goReturn: {
        if (requiredConfirmationString != confirmationString) {
          errorString= "FileOps.deleteRecursivelyB(.) Unconfirmed request";
          break goReturn;
          }
        if (theFile.isDirectory()) // If file is a directory then
          for (File childFile : theFile.listFiles()) { // for each child
            errorString= // recursively delete the child.
                deleteRecursivelyReturnString(childFile,confirmationString);
            if (null != errorString) break goReturn; // Exit if error.
            }
        if (!theFile.delete()) // Delete what remains after children are gone.
          errorString= "Failed to delete file: " + theFile;
      } // goReturn:
        return errorString;
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

    

    // File path and directory maker methods.

    public static File makePathRelativeToAppFolderFile(String... fileRelativePathStrings )
      /* This method creates a File name path object by adding,
        to the path name for the standard app folder,
        the fileRelativePathStrings, which is an array of Strings 
        representing path elements to be appended..  
        All but the last element represents a directory,
        The last element could be either a directory or a file,
        fileRelativePathStrings may contain a single name element, such as 
          {"TCPCopierStaging" + File.separator + "Infogora.exe"} 
        or multiple elements, such as 
          {"TCPCopierStaging", "Infogora.exe"} 
        Note that the above 2 examples represent the same path
        expressed in 2 different ways.
        */
      {
        File resultFile= AppSettings.userAppFolderFile; // Start with bare app folder
      
        for // For all the elements of the relative path element array 
          (String elementString: fileRelativePathStrings) {
          resultFile= new File(resultFile, elementString); // add next element.
          // theAppLog.debug("FileOps.makePathRelativeToAppFolderFile(.) " 
          //    + elementString + " " + resultFile);
          }
      
        return resultFile; // Return the accumulated result.
        }

    public static File makeRelativeToAppFolderFile( 
        String fileRelativePathString )
      /* This method creates a File name path object by 
        calling makePathRelativeToAppFolderFile(String...)
        with fileRelativePathString as the only element of the argument array.
        */
      {
        return makePathRelativeToAppFolderFile( fileRelativePathString );
        }

    static String makeDirectoryAndAncestorsString(File directoryFile)
      /* This method tries to create the directory whose name is directoryFile,
        and any ancestor directories that do not exist.
        It returns null if successful, an error message string if not.
        */
      {
        String resultString= null; // Assume no errors.
        makeDir: { 
          try {
            if (directoryFile.exists())
              break makeDir; // Do nothing if directory already exists.
            if (! directoryFile.mkdirs())
              resultString=
                "FileOps: "+directoryFile+" mkdirs() failed.";
          } catch (Exception theException){
            resultString= "FileOps.makeDirectoryAndAncestorsString(.): "
                + directoryFile + theException;
          }
        } // makeDir:
        return resultString;
        }

    public static void makeDirectoryAndAncestorsWithLoggingV(File directoryFile)
      /* This method creates a directory and its ancestors if needed,
        but if any errors happen, it logs them.
        */
      {
        String resultString= // Try creating desired folder if it doesn't exist.
            makeDirectoryAndAncestorsString(directoryFile);
        if (resultString != null) { // If there was an error, log it.
          theAppLog.error(
              "FileOps.makeDirectoryAndAncestorsWithLoggingV(.) " + resultString);
          }
        }

    }
