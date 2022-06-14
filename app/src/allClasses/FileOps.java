package allClasses;

import java.io.File;
import java.io.FileDescriptor;
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
import java.util.function.Function;

import allClasses.bundles.BundleOf2;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class FileOps

  {
  
    /* This class contains code useful for operating on files.
     * 
     * ///enh Some code could be simplified and improved by 
     * making more use of the java.nio.file.Files and Path classes,
     *  and less of the File class.  See:
     *    https://docs.oracle.com/javase/tutorial/essential/io/legacy.html
     * 
     * ///org It should probably be organized by grouping related methods.
     *
     * ///org To better reuse code that creates and later closes
     * stream-oriented files, maybe create a method 
     * that takes a function parameter 
     * that itself takes an OutputStream parameter.
     * This could be called easily with a lambda expression.
     * 
     * ///opt As methods are restructured, some public methods
     * might become permanently unused and should be removed.
     * Good examples of this are the methods which retry operations
     * without limit. 
     */


    // Code about updating and copying.

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
          if (! containsNoSpaceMessageInB(errorString))
            theAppLog.exception("FileOps.copyStreamBytesReturnString(.) "
              + "terminated by",theIOException);
                // Any exception other than disk full is anomaly-logged.
          }
        String logString= errorString;
        if (null==logString) logString= "succeeded";
        theAppLog.info("FileOps",
            "FileOps.copyStreamBytesReturnString(.) "+ logString
            +", bytes transfered=" + byteCountI
            +", elapsed ms=" + (System.currentTimeMillis()-startTimeMsL));
        return errorString;
        }
  
    public static boolean containsNoSpaceMessageInB(String errorString)
      {
        return errorString.contains("There is not enough space on the disk");
        }


    // Methods that manipulate file attributes.
    
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


    // Code that does file deletions.

    public final static String requiredConfirmationString= 
        "I AM CERTAIN ABOUT THIS!"; 

    public static String deleteRecursivelyIfItExistsReturnString(
        File theFile,String confirmationString) 
      /* This method works like deleteRecursivelyReturnString(.)
       * but does not return an error String if subtreeFile does not exists.
       */
      {
        String errorString= null;
        if (theFile.exists()) // Try deleting only if it actually exists. 
          errorString= 
            deleteParentRecursivelyReturnString(theFile,confirmationString);
        return errorString;
        }

    private final static Function<File,String> deletionFunctionOfFileToString=
      new Function<File,String>() {
        /* This is an instantiation of the Function interface for
         * performing file deletions to be used by directory tree traversers. 
         */
        public String apply(File theFile)
          /* This method deletes theFile.
           * theFile must have NO children if it is a directory.  
           * This method returns null if it succeeds, 
           * or a String describing the failure if it fails
           */
          {
            String resultString= null;
            if (!FileOps.deleteFileB(theFile))
              resultString= "File:delete() failed on: " + theFile;
            return resultString;
            } 
        };


    public static String deleteParentRecursivelyReturnString(
        File subtreeFile,String confirmationString)
      /* This method tries to delete the file/directory subtree 
       * that is rooted at subtreeFile.
       * First it checks that confirmationString is 
       * the requiredConfirmationString.  
       * If not then it returns a String describing the error.
       * Next it deletes the file or recursively deletes the directory 
       * If an error is countered it returns a String describing the error
       * and terminates deletion of the remainder of the subtree.
       */
      {
        String errorString;
      goReturn: {
        errorString= checkConfirmationString(confirmationString);
        if (null != errorString) break goReturn; // Exit if error.
        errorString= parentPostorderTraversalReturningString(
            subtreeFile, deletionFunctionOfFileToString);
      } // goReturn:
        return errorString;
      }

    public static String deleteChildrenRecursivelyReturnString(
        File subtreeFile,String confirmationString)
      /* This method tries to delete the children of
       * the file/directory subtree that is rooted at subtreeFile.
       * It does not delete the root.
       * First it checks that confirmationString is 
       * the requiredConfirmationString.  
       * If not then it returns a String describing the error.
       * Next it deletes the children, if any, of subtreeFile.
       * If an error is countered it returns a String describing the error
       * and terminates deletion of the remainder of the subtree.
       */
      {
        String errorString;
      goReturn: {
        errorString= checkConfirmationString(confirmationString);
        if (null != errorString) break goReturn; // Exit if error.
        errorString= childrenPostorderTraversalReturningString(
            subtreeFile, deletionFunctionOfFileToString);
      } // goReturn:
        return errorString;
      }

    public static String checkConfirmationString(String confirmationString)
      /* This method returns null if confirmationString contains
       * the correct confirmation String, an error description String otherwise.
       */
      {
        String errorString= null;
        if (requiredConfirmationString != confirmationString)
          errorString= "FileOps.checkConfirmationString(.) failed";
        return errorString;
      }

    public static void deleteDeleteable(File tmpFile)
      /* This method deletes tmpFile if tmpFile is not null,
       * otherwise it does nothing.  */
      {
        if (tmpFile != null) 
          deleteFileB(tmpFile);
        }


    /* Directory tree traversal code.
     * There are 2 mutually recursive methods for doing traversals.
     * Each one is an entry point, so that traversals may be don that
     * either include or don't include the root. 
     */

    public static String childrenPostorderTraversalReturningString(
        File subtreeFile, Function<File,String> functionOfFileReturnsString)
      /* This method tries to perform fileFunctionReturningString 
       * for every child file in subtreeFile.
       * functionOfFileReturnsString does some operation on a File
       * and returns a result String, null if success, 
       * not-null describing a reason for failure.
       * Use this method if you do NOT want
       * to perform the operation on the ROOT of subtreeFile.
       * 
       * ///enh Switch to class Files to eliminate directory size limits.
       */
      {
        String resultString= null;
      goReturn: {
        FileOps.directoryDutyCycle.updateActivityV(true);
        File[] childrenListOfFiles= subtreeFile.listFiles();
        FileOps.directoryDutyCycle.updateActivityV(false);
        if (null  != childrenListOfFiles) // Process children if a directory.
          for (File childFile : childrenListOfFiles) { // for each child
            resultString= // recursively process the child.
                FileOps.parentPostorderTraversalReturningString(
                    childFile,functionOfFileReturnsString);
            if (null != resultString) break goReturn; // Exit loop if error.
            }
      } // goReturn:
        return resultString;
      }

    public static String parentPostorderTraversalReturningString(
        File subtreeFile, Function<File,String> functionOfFileReturnsString)
      /* This method tries to perform fileFunctionReturningString 
       * for every file in the children of subtreeFile,
       * AND on the root of subtreeFile.
       * functionOfFileReturnsString does some operation on a File
       * and returns a result String, null if success, 
       * not-null describing a reason for failure.
       */
      {
        String resultString= null;
      goReturn: {
        resultString= // Perform operation on child files. 
          childrenPostorderTraversalReturningString(
            subtreeFile, functionOfFileReturnsString);
        if (null != resultString) break goReturn; // Exit if there was an error.
        resultString= // Finish by performing operation on root of subtree. 
          functionOfFileReturnsString.apply(subtreeFile);
      } // goReturn:
        return resultString;
      }


    // Methods for use with lambda expressions to do writing to OutputStream.
    
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
        theAppLog.info("FileOps",
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
              errorString= EpiString.combine1And2WithNewlineString(
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

        theAppLog.debug("FileOps",
            "FileOps.writeDataReturnString(.) ends");
        return errorString;
        }

    @FunctionalInterface
    public interface WriterTo1Throws2<D, E extends Exception> 
      {
        void writeToV(D destinationD) throws E;
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


    // For measuring various OS operation times.

    public static void readBytesFromInputStreamV(
        byte[] readBytes, FileInputStream theFileInputStream) throws IOException
      /* This method reads an array of bytes from theFileInputStream
       * to the array readBytes.
       * It also monitors time in the OS. 
       * It throws an IOException if there is an error.
       */
      {
        try {
            FileOps.readingDutyCycle.updateActivityV(true);
            theFileInputStream.read(readBytes);
          } finally { // Do this regardless of exceptions.
            FileOps.readingDutyCycle.updateActivityV(false);
          }
        }

    public static void writeBytesToOutputStreamV(
        byte[] bytes, FileOutputStream theFileOutputStream) throws IOException
      /* This method writes an array of bytes to theFileOutputStream.
       * It also monitors time in the OS. 
       * It throws an IOException if there is an error.
       */
      {
        try {
            FileOps.writingDutyCycle.updateActivityV(true);
            theFileOutputStream.write(bytes);
          } finally { // Do this regardless of exceptions.
            FileOps.writingDutyCycle.updateActivityV(false);
          }
        }

    public static void syncV(FileDescriptor theFileDescriptor)
        throws IOException
      /* This method syncs the file whose FileDescriptor is theFileDescriptor.
       * It also monitors time in the OS.
       * It throws an IOException if there is an error.
       */
      {
        try {
            FileOps.syncingDutyCycle.updateActivityV(true);
            theFileDescriptor.sync();
          } finally { // Do this regardless of exceptions.
            FileOps.syncingDutyCycle.updateActivityV(false);
          }
      }

    public static void closeOutputStreamV(FileOutputStream theFileOutputStream)
        throws IOException
      /* This method closes theFileOutputStream.
       * It also monitors time in the OS. 
       * It throws an IOException if there is an error.
       */
      {
        try {
            FileOps.closingDutyCycle.updateActivityV(true);
            theFileOutputStream.close();
          } finally { // Do this regardless of exceptions.
            FileOps.closingDutyCycle.updateActivityV(false);
          }
      }

    public static boolean deleteFileB(File theFile)
      /* This method tries to delete theFile.
       * It also monitors time in the OS. 
       * It returns true if the delete was successful, false otherwise.
       */
      {
        FileOps.deletingDutyCycle.updateActivityV(true);
        boolean resultB= theFile.delete();
        FileOps.deletingDutyCycle.updateActivityV(false);
        return resultB;
        }

    static String getOSReportString()
    {
      long nowTimeNsL= System.nanoTime();
      if  // If 1/2 second has passed
        ( 500000 <= (nowTimeNsL - osLastTimeNsL) ) 
        { // update report String.
          osReportString= "";
          
          osReportString+= DutyCycle.resetAndGetOSString(
              directoryDutyCycle, " dir:", nowTimeNsL);
          osReportString+= DutyCycle.resetAndGetOSString(
              deletingDutyCycle, " del:", nowTimeNsL);
          osReportString+= DutyCycle.resetAndGetOSString(
              closingDutyCycle, " clo:", nowTimeNsL);
          osReportString+= DutyCycle.resetAndGetOSString(
              writingDutyCycle, " wrt:", nowTimeNsL);
          osReportString+= DutyCycle.resetAndGetOSString(
              readingDutyCycle, " rea:", nowTimeNsL);
          osReportString+= DutyCycle.resetAndGetOSString(
              syncingDutyCycle, " syn:", nowTimeNsL);
          osLastTimeNsL= nowTimeNsL; // Reset for next time.
          }
    
      return osReportString;
      }

    static String quotientAsPerCentString(long dividentL,long divisorL)
    {
      String resultString;
      double perCentD= (100. * dividentL) / divisorL;
      if (0.5 > perCentD) 
        resultString= ""; // Was "00%";
      else if (99.5 <= perCentD)
        resultString= "99+";
      else
        resultString= String.format("%02d%%",Math.round(perCentD));
      return resultString;
      }

    static DutyCycle directoryDutyCycle= new DutyCycle();
    static DutyCycle writingDutyCycle= new DutyCycle();
    static DutyCycle syncingDutyCycle= new DutyCycle();
    static DutyCycle closingDutyCycle= new DutyCycle();
    static DutyCycle readingDutyCycle= new DutyCycle();
    static DutyCycle deletingDutyCycle= new DutyCycle();
    static String osReportString;
    static long osLastTimeNsL;
    
    static class DutyCycle
      /* This class is used to calculate the duty-cycle of operations.
       * It was created to do this for IO operations,
       * but it could be used for anything.
       * The code is based on the assumption of a single thread.
       */
      {
        private boolean operationIsActiveB= false;
        private long timeOfLastActivityChangeNsL;
        private long timeActiveNsL;
        private long timeInactiveNsL;
      
        public void updateActivityV(boolean isActiveB)
          {
            updateActivityV(isActiveB,System.nanoTime());
            }
      
        public void updateActivityV(boolean newActivityB,long timeNowNsL)
          {
            long timeSinceLastActivityChangeNsL= 
              timeOfLastActivityChangeNsL - timeNowNsL;
            if (operationIsActiveB)
              timeActiveNsL+= timeSinceLastActivityChangeNsL;
              else
              timeInactiveNsL+= timeSinceLastActivityChangeNsL;
            operationIsActiveB= newActivityB;
            timeOfLastActivityChangeNsL= timeNowNsL;
            }
      
        public String resetAndGetOSString(long timeNowNsL)
          {
            updateActivityV( // Adjust the proper total for present time.
                operationIsActiveB,timeNowNsL);
            long totalTimeSincePreviousReportNsL= timeActiveNsL + timeInactiveNsL;
            String resultString= FileOps.quotientAsPerCentString(
                timeActiveNsL, totalTimeSincePreviousReportNsL);
      
            // Reset accumulators for next time period.
            timeActiveNsL= 0;
            timeInactiveNsL= 0;
      
            return resultString;
            }
  
        static String resetAndGetOSString(
          FileOps.DutyCycle theDutyCycle, String labelString, long timeNowNsL)
        /* Returns string representing OS%, or "" if % is 0.
         * It also resets for the next measurement.
         */
        {
          String resultString= theDutyCycle.resetAndGetOSString(timeNowNsL);
          if ("" != resultString) // % not 0
            resultString= labelString + resultString; // so append to label.
          return resultString;
          }
      
        } // DutyCycle


    // String methods.

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
