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

import allClasses.bundles.BundleOf2;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class FileOps

  {
  
    /* This class contains code useful for operating on files.
     * 
     * ///org It should probably be organized by grouping related methods.
     *
     * ///org To better reuse code that creates and later closes
     * stream-oriented files, maybe create a method 
     * that takes a function parameter 
     * that itself takes an OutputStream parameter.
     * This could be called easily with a lambda expression.
     * 
     * ///opt Some code could be simplified by making more use of
     * the java.nio.file.Files class.
     * 
     * ///opt As methods are restructured, some public methods
     * might become permanently unused and should be removed.
     * Good examples of this are the methods which retry operations
     * without limit. 
     */

    public static void updateWithRetryFromToV(File thisFile, File thatFile)
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
        
        ///org This method might eventually be replaced by 
        calls to its componennts.
        */
      {
        boolean updatableB= // Is this file is newer than that file?
            isNewerB(thisFile, thatFile);
        theAppLog.info("FileOps.updateFromToV((..), updatingB="+updatableB);
        if ( updatableB )  // Then replace that with this.
          copyFileWithRetryReturnString(thisFile, thatFile);
        }

    public static boolean isNewerB(File thisFile, File thatFile)
      /* If thisFile is newer than thatFile, 
        or thisFile exists but thatFile does not,
        then return true, otherwise return false.
        This works because File.lastModified() returns 0
        if the file does not exist.
        */
      {
        long thisFileLastModifiedL= thisFile.lastModified();
        long thatFileLastModifiedL= thatFile.lastModified();
        boolean isNewerB= ( thisFileLastModifiedL > thatFileLastModifiedL );
        // theAppLog.debug("FileOps.isNewerB(.), newerB="+isNewerB
        //     + twoFilesString(thisFile, thatFile));
        return isNewerB;
        }
  
    public static String copyFileWithRetryReturnString(
        File sourceFile, File destinationFile)
      /* This method copies the sourceFile to the destinationFile.
        It assumes that any copy failure due to an IOException
        is caused by a temporary condition such as 
        the destination file being open for reading.
        In this case it will retry the operation.
        It does not return until the copy succeeds,
        another type of error occurs, or thread termination is requested.
        It returns null if the operation succeeds.
        It returns a String describing the failure's cause if it fails.

        ///fix ///opt This should not retry forever.  Either:
         * have anomaly limit on the number of retries, or
         * do retries elsewhere, where the only possible error
           is contention which is guaranteed to go away with sufficient retries.
        */
      {
        ///enh create function to make double file path string?
        final String methodIDString= "FileOps.tryCopyFileReturnString(.) ";
        theAppLog.info(methodIDString + "======== COPYING FILE ======== "
          + twoFilesString(sourceFile, destinationFile));
        String errorString= null;
        int attemptsI= 0;
        loop: while (true) {
          if (EpiThread.testInterruptB()) { // Termination requested.
            errorString= "Terminating.";
            theAppLog.info( true,methodIDString + errorString );
            break loop; // exit loop.
            }
          attemptsI++;
          errorString= tryCopyFileReturnString(sourceFile, destinationFile);
          if (null == errorString) { // Copy completed.
            theAppLog.info( methodIDString
                + "Copying successful on attempt #"+attemptsI+" "
                + twoFilesString(sourceFile, destinationFile));
            break loop; // exit loop.
            }
          theAppLog.info( methodIDString + "Failed attempt #"
            + attemptsI + ".  Will retry after 1 second." ); 
          EpiThread.interruptibleSleepB(
              Config.fileCopyRetryPause1000MsL); // Wait 1 second.
          } // loop:
        theAppLog.debug("FileOps", methodIDString + "Exit with: "+errorString);
        if (null != errorString) { // If error occurred
          errorString= // prepend method ID
              "FileOps.copyFileWithRetryReturnString(.) " + errorString;
          theAppLog.error( errorString ); // and log it as an error.
          }
        return errorString;
        }
  
    public static boolean tryCopyFileB(
        File sourceFile, File destinationFile)
      /* This method tries to copy the sourceFile to the destinationFile.
        It returns true if the copy succeeds, false otherwise.
        It logs any exception which causes failure.
        */
      {
        return 
          ( null 
            == 
            tryCopyFileReturnString(sourceFile, destinationFile)
            );
        }

    public static String tryCopyFileReturnString( 
        File sourceFile, File destinationFile)
      /* This method tries to copy the sourceFile to the destinationFile.
        It returns null if the copy succeeds, 
        or a String describing the reason for failure if the copy fails.
        It logs any exception which causes failure.
        */
      {
        theAppLog.debug("FileOps","FileOps.tryCopyFileReturnString(.) begins");
        String errorString= null; // Assume success.
        File tmpFile= null;
        toReturn: {
          tmpFile= createWithFoldersTemporaryFile(
              "Copy",destinationFile.getParentFile());
          if (tmpFile == null) {
            errorString= "Failed to create temporary file"; 
            break toReturn;
            }
          Path sourcePath= sourceFile.toPath();
          Path tmpPath= tmpFile.toPath();
          errorString= tryRawCopyFileReturnString(sourceFile,tmpFile);
          if (null != errorString) break toReturn;
          errorString= copyTimeAttributesReturnString(sourcePath,tmpPath);
          if (null != errorString) break toReturn;
          errorString= atomicRenameReturnString(
              tmpFile.toPath(), destinationFile.toPath()); 
          if (null != errorString) break toReturn;
          } // toReturn:
        deleteDeleteable(tmpFile); // Delete possible temporary file debris.
        theAppLog.debug("FileOps","FileOps.tryCopyFileReturnString(.) "
            + "exit with: "+errorString);
        if (null != errorString) // If error, prepend method ID.
          errorString= "FileOps.tryCopyFileReturnString(.) " + errorString; 
        return errorString;
        }

    public static String atomicRenameReturnString(
        Path sourcePath, Path destinationPath)
      /* This method atomically renames a file.
        It is what is used to safely replace one file with another.
        It returns null if the rename succeeds, a String describing
        the cause of failure if failure occurs.
        If it encounters an AccessDeniedException, it will retry,
        assuming the exception was caused by the destination file
        being protected because it is temporarily open for reading.
        
        ///fix Somehow eliminate retry-polling on rename failure.
        */
      {
        String errorString= null;
        boolean successB= false;
        int attemptsI= 0;
        theAppLog.info( "FileOps","FileOps.atomicRenameB(..) begins"
            + twoFilesString(sourcePath.toFile(), destinationPath.toFile()));
        while (true) { // Loop to retry some fail types.
          if (EpiThread.testInterruptB()) break; // Exit if requested.
          attemptsI++;
          try {
              Files.move( // Try the rename.
                  sourcePath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
              successB= true; // If here then rename succeeded.
              break; // Exit loop.
            } catch (AccessDeniedException theAccessDeniedException) {
              theAppLog.warning(
                  "FileOps.atomicRenameB(..) failed attempt "+attemptsI
                  +", "+theAccessDeniedException);
              if (attemptsI >= 10) {
                theAppLog.error(
                  "FileOps.atomicRenameB(..) retry limit exceeded, aborting.");
                break; // Exit loop.
                }
            } catch (IOException theIOException) {
              theAppLog.exception(
                  "FileOps.atomicRenameB(..) failed with ",theIOException); 
              break; // Exit loop.
            }
          EpiThread.interruptibleSleepB( // Pause thread to prevent CPU hogging.
              Config.errorRetryPause1000MsL
              );
          } // while
        theAppLog.info("FileOps.atomicRenameB(.) ends, successB="+successB
            +" after "+attemptsI+" attempts, files:"
                + "\n       sourcePath: " + sourcePath
                + "\n  destinationPath: " + destinationPath);
        if (!successB) errorString= "FileOps.atomicRenameB(.) failed.";
        return errorString;
        }
  
    public static File createTemporaryFile(String nameString)
      /* This method tries to create a temporary file which contains
        nameString as part of the name and in the app folder.
        The app folder should already have been created.
        It logs any exceptions produced.
        It returns the File of the temporary file created if successful,
        or null if it failed for any reason.
        
        ///opt This method creates the temporary file 
        in the default app directory.  Because the temporary file 
        will eventually be renamed to another name, 
        possibly in distant directory,
        t might be faster to create the temporary file
        n the folder which is its ultimate destination.
        */
      {
        return createTemporaryFile(nameString, AppSettings.userAppFolderFile);
        }
  
    public static File createWithFoldersTemporaryFile(
        String nameString, File directoryFile)
      /* This method tries to create a temporary file which contains
        nameString as part of the name and in the folder directoryFile.
        It creates the folder and any ancestors if they don't already exist.
        It reports any exceptions produced.
        It returns the File of the temporary file created if successful,
        or null if it failed for any reason.
        */
      {
        File tmpFile= null; // Initially file is not created.
      toReturn: { 
      toCreateFile: {
        if (directoryFile.exists()) // If file's folder already exists
          break toCreateFile; // go make file.
        if (! directoryFile.mkdirs()) // If unable to create its folder
          break toReturn; // exit now.
      } // toCreateFile:
        tmpFile= createTemporaryFile(nameString, directoryFile);
      } // toReturn: 
        return tmpFile;
        }

    public static File createTemporaryFile(
        String nameString, File directoryFile)
      /* This method tries to create a temporary file which contains
        nameString as part of the name and in the folder directoryFile.
        It reports any errors produced.
        It returns the File of the temporary file created if successful,
        or null if it failed for any reason.
        */
      {
        BundleOf2<String,File> resultBundle= 
            createTemporaryFileReturnBundle(nameString, directoryFile);
        String errorString= resultBundle.getV1();
        if (null != errorString) 
          theAppLog.error(errorString);
        return resultBundle.getV2(); // Return file result.
        }

    public static BundleOf2<String,File> createTemporaryFileReturnBundle(
        String nameString, File directoryFile)
      /* This method tries to create a temporary file which contains
        nameString as part of the name and in the folder directoryFile.
        It returns a Bundle of the following:
        * a String describing errors encountered, or null if no errors
        * a File of the temporary file created, or null if not successful
        */
      {
        File resultFile= null;
        String errorString= null;
        try {
            resultFile= File.createTempFile( // Creates empty file.
              nameString,null,directoryFile);
          } catch (IOException theIOException) {
            errorString= "FileOps.createTemporaryFile(..) failed creating "
              + nameString + " in " + directoryFile + ", " + theIOException; 
          }
        return new BundleOf2<String,File>(errorString,resultFile);
        }

    private static String tryRawCopyFileReturnString(
        File sourcesourceFile, File destinationFile) 
      /* This method tries to copy the sourceFile to the destinationFile.
        If there is an error, the copy fails.
        If the copy is interrupted, the copy fails.
        This method returns null if the copy succeeds, 
        a String describing the failure if the copy fails.
        */
      {
        theAppLog.debug("FileOps","FileOps.tryRawCopyFileB(.) begins.");
        InputStream theInputStream= null;
        String errorString= null;
        try {
            theInputStream= new FileInputStream(sourcesourceFile);
            errorString= tryCopyingInputStreamToFileReturnString(
                theInputStream,destinationFile);
          } catch (Exception e) {
              theAppLog.exception("FileOps.tryRawCopyFileB(.)",e); 
          } finally { // Close things, error or not.
            theAppLog.debug("FileOps","FileOps.tryRawCopyFileB(.) "
                + "closing InputStream.");
            Closeables.closeWithErrorLoggingB(theInputStream);
          }
        theAppLog.debug("FileOps","FileOps.tryRawCopyFileB(.) ends, "
            +"closes done, errorString="+errorString);
        return errorString;
        }

    public static boolean tryCopyingInputStreamToFileB(
        InputStream theInputStream, File destinationFile) 
      /* This is an adaptor method for callers which expect a boolean result.  
       * */
      { 
        return 
          ( null 
            == 
            tryCopyingInputStreamToFileReturnString(
                theInputStream, destinationFile));
        }

    public static String tryCopyingInputStreamToFileReturnString(
        InputStream theInputStream, File destinationFile) 
      /* This method tries to create the destinationFile,
        copy theInputStream to it, then close it.
        If there is an error, the copy fails.
        If the copy is interrupted, the copy fails.
        This method returns null if the copy succeeds, 
        or a String describing the cause of the failure if the copy fails.
        */
      {
        theAppLog.debug("FileOps",
            "FileOps.tryCopyingInputStreamToFileReturnString(.) begins.");
        OutputStream theOutputStream= null;
        boolean successB= false;
        String errorString= null;
        try {
            theOutputStream= new FileOutputStream(destinationFile);
            errorString= copyStreamBytesReturnString(
                theInputStream,theOutputStream);
          } catch (Exception e) {
              theAppLog.exception(
                  "FileOps.tryCopyingInputStreamToFileReturnString(.)",e); 
          } finally { // Close things, error or not.
            theAppLog.debug("FileOps","FileOps."
              + "tryCopyingInputStreamToFileReturnString(.) "
              + "closing OutputStream.");
            Closeables.closeWithErrorLoggingB(theOutputStream);
              // Closing the OutputStream can block temporarily.
          }
        theAppLog.debug("FileOps","FileOps."
            + "tryCopyingInputStreamToFileReturnString(.) "
            + "ends, closes done, successB="+successB);
        return errorString;
        }
  
    public static boolean copyStreamBytesB( 
        InputStream theInputStream, OutputStream theOutputStream)
      /* This is an adaptor method for callers which expect a boolean result.  
       * */
      { 
        return 
          ( null 
            == 
            copyStreamBytesReturnString(theInputStream, theOutputStream)
            );
        }
  
    public static String copyStreamBytesReturnString(
        InputStream theInputStream, OutputStream theOutputStream)
      /* This method copies all [remaining] bytes
        from theInputStream to theOutputStream.
        The streams are assumed to be open at entry and will be open at exit.
        It returns null if the copy of all data finished without error.
        It returns a String if the copy does not finish,
        and the String describes the reason.
        A Thread interrupt will interrupt the copy operation.
        */
      {
        theAppLog.debug(
            "FileOps","FileOps.copyStreamBytesReturnString(.) begins.");
        String errorString;
        long startTimeMsL= System.currentTimeMillis();
        int byteCountI= 0;
        try {
          byte[] bufferAB= new byte[1024];
          int lengthI;
          while (true) {
            lengthI= theInputStream.read(bufferAB);
            if (lengthI <= 0) // Transfer completed.
              { errorString= null; break; } // Record success and exit loop.
            theOutputStream.write(bufferAB, 0, lengthI);
            byteCountI+= lengthI;
            if (EpiThread.testInterruptB()) { // Thread interruption.
              errorString= "terminated by thread interrupt";
              break; // Exit loop without success.
              }
            }
          } 
        catch (IOException theIOException) {
          errorString= "failed because of "+theIOException;
          theAppLog.exception("FileOps.copyStreamBytesReturnString(.) "
              + "terminated by",theIOException);
          }
        String logString= errorString;
        if (null==logString) logString= "succeeded";
        theAppLog.info("FileOps",
            "FileOps.copyStreamBytesReturnString(.) "+ logString
            +", bytes transfered=" + byteCountI
            +", elapsed ms=" + (System.currentTimeMillis()-startTimeMsL));
        return errorString;
        }
  
    public static String copyTimeAttributesReturnString(
        Path sourcePath, Path destinationPath)
      /* This method copies the 3 time attributes from the source file
        to the destination file.
        It returns null if successful, 
        a String describing the cause of the failure if not.
        */
      {
        String errorString= null;
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
          theAppLog.debug("FileOps","FileOps.copyTimeAttributesV(..): "
              +theLastModifiedTime+", "+theLastAccessTime+", "+theCreateTime);
    
          BasicFileAttributeView destinationBasicFileAttributeView= 
              Files.getFileAttributeView(
                  destinationPath,BasicFileAttributeView.class);
    
          destinationBasicFileAttributeView.setTimes(
              theLastModifiedTime, theLastAccessTime, theCreateTime);
        } catch (IOException theIOException) {
          errorString= "FileOps.copyTimeAttributesB(..) failed with "; 
          theAppLog.exception(errorString,theIOException);
          errorString+= theIOException;
        }
      return errorString;
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

    public static String deleteRecursivelyIfItExistsReturnString(
        File theFile,String confirmationString) 
      {
        String errorString= null;
        if (theFile.exists()) // Try deleting only if it actually exists. 
          errorString= 
            deleteRecursivelyReturnString(theFile,confirmationString);
        return errorString;
        }

    public static String deleteRecursivelyReturnString(
        File theFile,String confirmationString) 
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

    public static File makePathRelativeToAppFolderFile(
        String... fileRelativePathStrings )
      /* This method creates a File name path object by adding,
        to the path name for the standard app folder,
        the fileRelativePathStrings, which is an array of Strings 
        representing path elements to be appended..  
        All but the last element represents a directory,
        The last element could be either a directory or a file,
        fileRelativePathStrings may contain a single name element, such as 
          {"TCPCopierStaging" + File.separator + "TUDNet.exe"} 
        or multiple elements, such as 
          {"TCPCopierStaging", "TUDNet.exe"} 
        Note that the above 2 examples represent the same path
        expressed in 2 different ways.
        */
      {
        File resultFile= AppSettings.userAppFolderFile; // Start with bare app folder
      
        for // For all the elements of the relative path element array 
          (String elementString: fileRelativePathStrings) {
          resultFile= new File(resultFile, elementString); // add next element.
          // theAppLog.debug("FileOps","FileOps.makePathRelativeToAppFolderFile(.) " 
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
        String errorString= null; // Assume no errors.
        makeDir: { 
          try {
            if (directoryFile.exists())
              break makeDir; // Do nothing if directory already exists.
            if (! directoryFile.mkdirs())
              errorString= " mkdirs() failed with " + directoryFile;
          } catch (Exception theException){
            errorString= "failure with " + directoryFile + " " + theException;
          }
        if (null != errorString) // Prepend any error with method name. 
          errorString= 
            "FileOps.makeDirectoryAndAncestorsString(.):"+errorString;
        } // makeDir:
        return errorString;
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

  
    // Methods for use with lambda expressions to do writing to OutputStream.

    @FunctionalInterface
    public interface WriterTo1Throws2<D, E extends Exception> 
      {
        void writeToV(D destinationD) throws E;
        }
    
    public static String writeDataReturnString( 
        WriterTo1Throws2<OutputStream,IOException> 
          sourceWriterTo1Throws2, 
        File destinationFileFile
        )
      /* This method writes data from sourceWriterTo1Throws2
       * using an OutputStream with the possibility of an IOException,
       * to a new text file with a pathname of destinationFileFile.
       * If the write is successful then it returns null,
       * otherwise it returns a String describing the failure.
       * 
       * Most of the code here is about initialization, exception handling,
       * and finalization of the file destinationFileFile.
       * sourceWriterTo1Throws2 does all the data writing.
       */
      {
        theAppLog.info(// "FileOps",
            "FileOps.writeDataReturnString(.) begins/called, file: "
            + destinationFileFile);

        String errorString= null;
        FileOutputStream sourceFileOutputStream= null;
        try {
            sourceFileOutputStream= new FileOutputStream( // Create OutputStream 
                destinationFileFile); // to the file with this pathname.
            theAppLog.debug("FileOps",
              "FileOps.writeDataReturnString(.) write begins.");
            sourceWriterTo1Throws2.writeToV( // Write all source data 
                sourceFileOutputStream); // to OutputStream.
            theAppLog.debug("FileOps",
              "FileOps.writeDataReturnString(.) write ends.");
            }
          catch (Exception theException) {
            errorString= "write error: "+theException;
            }
          finally {
            try {
              if ( sourceFileOutputStream != null ) 
                sourceFileOutputStream.close(); 
              }
            catch ( Exception theException ) { 
              EpiString.combine1And2WithNewlineString(
                  errorString, "close error: "+theException);
              }
            }
        if (null != errorString) { // If error occurred, add prefix.
          errorString= EpiString.combine1And2WithNewlineString(
              "FileOps.writeDataReturnString(.), "
                  + "destinationFileFile= " + destinationFileFile,
              errorString
              );
          theAppLog.error(errorString);
          }
        theAppLog.debug("FileOps",
            "FileOps.writeDataReturnString(.) ends, file: "
            + destinationFileFile);

        theAppLog.info("FileOps.writeDataReturnString(.) ends");
        return errorString;
        }
    
    }
