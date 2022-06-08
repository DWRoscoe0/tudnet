package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;


/// import allClasses.LockAndSignal.Input;


public class VolumeChecker

  extends VolumeDetector

  {
    /* This class is used to check a mass storage volume and report
     * the amount of free space.

     * Presently it does the following tests:
     * 
     * * Measures usable writable space.
     * 
     * * Verifies that each free block can store unique data,
     *   testing for volumes with fake sizes.
     *
     * ///enh Update the Progress-Report and Spinner 
     *   while doing FileDescriptor.sync() 
     *   so that the spinner spins and times are updated.
     *   This will probably require a separate thread.
     *  
     */

    // Locally stored injected dependencies (none).

    // Constants.

      final int bytesPerBlockI= 512;
      final long bytesPerFileL= 64 * 1024 * 1024; // 64 MB
      final long bytesReportPeriodL= 16 * 1024 * 1024; // 16 MB trigger limit.

    // static variables.  None.

    // non-static (instance) variables.

      // Constructor injections.  None.

      // Feature scope.
      File buildFolderFile;
      private File volumeFile;

      // Volume scope.
      private long volumeFreeSpaceL;
      private long volumeTotalBytesL; // Partition size.
      private long initialVolumeFreeBytesL;
        // Note, free-space/usable-space can change wildly during file IO.
      private long doneTimeMsL;

      // Volume pass scope.  Used for both write and read-compare passes.  
      private long toCheckTotalBytesL; // This should be the sum of:
        private long toCheckRemainingBytesL; // Down counter and loop control.
        private long toCheckDoneBytesL; // Up counter and # of next byte to do.

      // Volume write pass only scope.
      private long writtenBytesL; // Counts written.

      // Volume read-and-compare pass only scope.
      private long readCheckedBytesL; // Counts bytes read AND compared.
      private byte[] readBytes= // Buffer to be used for all reads. 
          new byte[bytesPerBlockI];
      
      // Time measurement.
      private long passStartTimeMsL;

      // For measuring speed of operation.
      private long speedIntervalStartTimeMsL;
      private long speedStartVolumeDoneBytesL;
      private long speedL;

      // File scope (file blocks to write or compare).
      private long volumeDoneFilesL; // Also # of next file to process.
      private File checkFile; // Name of file containing the active block.
      private long remainingFileBytesL; // Down counter and loop control.

      private int spinnerStepStateI;
      private long timeOfLastSpinnerStepMsL;


    // Constructors and constructor-related methods.

    public VolumeChecker( // constructor
        String nameString, // Node name.
        Persistent thePersistent,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor
        )
      {
        super( // constructor
          nameString, // Node name.
          thePersistent,
          theScheduledThreadPoolExecutor
          );
        theAppLog.debug(myToString() +
            "VolumeChecker.VolumeChecker(.) ends, nameString='"+nameString+"'");
        }

    @Override
    protected void mainThreadLogicV()
      {
        appendWithPromptSlowlyAndWaitForKeyV(
          "This feature does functional testing and capacity measurement "
          + "of storage volumes attached to your device.  "
          + "\n\n"
          + "Note, for the read-back test to be valid, "
          + "your test volume size must be larger than "
          + "the amount of your device's memory "
          + "used as a disk-page cache."
          );
        List<File> volumeListOfFiles;
      while(true) {
        volumeListOfFiles= getTerminationOrKeyOrAddedVolumeListOfFiles();
        if (LockAndSignal.isInterruptedB()) break; // Exit if requested.
        if (0 < volumeListOfFiles.size()) 
          ; // At least one volume was added, so use volume list as is.
          else
          volumeListOfFiles= // No volumes added, so use
            Arrays.asList(getVolumeFiles()); // all volumes.
        checkVolumeListV( // Check each volume in list, with user's consent
            volumeListOfFiles);
      } // theLoop:
        return;
      }

    protected void checkVolumeListV(List<File> addedVolumeListOfFiles)
      {
        queueAndDisplayOutputSlowV(
          "\n\nAvailable volume[s]: "
          + addedVolumeListOfFiles.toString()
          );
        for (File theFile : addedVolumeListOfFiles) {
          volumeFile= theFile;
          if (getConfirmationKeyPressB( // Check volume if user okays it.
               "Would you like to check " + volumeFile + " ? ")
              )
            checkVolumeV();
          }
        }

    private void checkVolumeV()
      /* This method checks the volume specified by volumeFile.
       * This includes optionally deleting all files on the volume,
       * then doing a write-read test.
       * 
       * ///enh Being modified to make write-read test optional.
       */
      {
        theAppLog.debug("VolumeChecker.checkVolumeV(.) begins.");
        String resultString;
      goReportErrorOrSuccessAndReturn: {
      goUpdateProgressAndReturn: {
      goFinishPassAndReturn: {
        operationDequeOfStrings= new ArrayDeque<String>();
        writtenBytesL= 0;
        readCheckedBytesL=0;
        doneTimeMsL= 0;
        passStartTimeMsL= getTimeMsL();
        volumeTotalBytesL= volumeFreeSpaceL= volumeFile.getTotalSpace();
        spinnerStepStateI= 0;
        queueAndDisplayOutputSlowV("\n\nChecking " + volumeFile + "\n");
        resultString= maybeDeleteAllVolumeFilesReturnString(volumeFile);
        if (! EpiString.isAbsentB(resultString)) 
          break goUpdateProgressAndReturn;
        buildFolderFile= new File(volumeFile,Config.appString + "Temp");
        initialVolumeFreeBytesL= volumeTotalBytesL;
        toCheckTotalBytesL= initialVolumeFreeBytesL;
        if (!getConfirmationKeyPressB( // Exit if write-read check not wanted.
            "\nDo you want to write-read-compare check this volume?")
            )
          {
            appendSlowlyV(bytesResultsString());
            break goReportErrorOrSuccessAndReturn;
            }
        progressReportSetV( () -> getVolumeWriteReadReportString() );
        pushOperationV("Volume-Check");
        resultString= deleteTemporaryFilesReturnString();
        // Ignore error, which probably means no deletion needed.
        resultString= FileOps.makeDirectoryAndAncestorsString(
            buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) {
          resultString= EpiString.combine1And2WithNewlineString(
              "error occurred while creating folder", resultString);
          break goFinishPassAndReturn;
          }
        progressReportSetV( () -> getVolumeWriteReadReportString() );
        progressReportUpdateV(); // First, slow progress report.
        resultString= writeTestReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) {
          resultString= EpiString.combine1And2WithNewlineString(
              "error occurred during write-test", resultString);
          break goFinishPassAndReturn;
          }
        resultString= readTestReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) {
          resultString= EpiString.combine1And2WithNewlineString(
              "error occurred during read-test", resultString);
          break goFinishPassAndReturn;
          }
      }  // goFinishPassAndReturn:
        pushOperationAndRefreshProgressReportV("deleting temporary files");
        String deleteErrorString= deleteTemporaryFilesReturnString();
        resultString= EpiString.combine1And2WithNewlineString(
            resultString, deleteErrorString);
        synchronizeWithFreeSpaceV();
        replaceOperationAndRefreshProgressReportV("done");
      }  // goUpdateProgressAndReturn:
        progressReportUpdateV();
      }  // goReportErrorOrSuccessAndReturn:
        progressReportResetV(); // End progress reports.
        java.awt.Toolkit.getDefaultToolkit().beep(); // Get user's attention.
        if (! EpiString.isAbsentB(resultString)) // Report error or success.
          appendWithPromptSlowlyAndWaitForKeyV( // Report error.
              "\n\nThe operation terminated:\n" + resultString);
          else 
          appendWithPromptSlowlyAndWaitForKeyV( // Report success.
            "\n\nThe operation completed without error.");
        theAppLog.debug("VolumeChecker.checkVolumeV(.) ends.");
        return;
      } // checkVolumeV(.)

    private String deleteTemporaryFilesReturnString()
      {
        String deleteErrorString= FileOps.deleteParentRecursivelyReturnString(
            buildFolderFile,FileOps.requiredConfirmationString);
        return deleteErrorString;
        }

    private String bytesResultsString()
      {
        return "\n"
          + bytesResultString( "in volume", volumeTotalBytesL)
          + bytesResultString( "free", volumeFreeSpaceL)
          + bytesResultString( "written", writtenBytesL)
          + bytesResultString( "read okay", readCheckedBytesL)
          ;
        }

    private String bytesResultString(String typeString, long countL)
      {
        return String.format("\n%-15s :%,15d", "bytes "+typeString, countL);
        }


    // For measuring OS time, starting first with writing time.

    private static class DutyCycle {
  
      private boolean isActiveB= false;
      private long lastActivityChangeTimeNsL;
      private long activeNsL;
      private long inactiveNsL;

      /// public void updateStatusV(long timeNowNsL) 
      ///   { updateStatusV(isActiveB,timeNowNsL); }

      public void updateStatusV(boolean isActiveB)
        {
          updateStatusV(isActiveB,System.nanoTime());
          }

      public void updateStatusV(boolean isActiveB,long timeNowNsL)
        {
          long timeSinceLastChangeNsL= lastActivityChangeTimeNsL - timeNowNsL;
          if (this.isActiveB)
            activeNsL+= timeSinceLastChangeNsL;
            else
            inactiveNsL+= timeSinceLastChangeNsL;
          this.isActiveB= isActiveB;
          lastActivityChangeTimeNsL= timeNowNsL;
          }

      private String resetAndGetOSString(long timeNowNsL)
        {
          updateStatusV(isActiveB,timeNowNsL);
          long totalSinceReportNsL= activeNsL + inactiveNsL;
          String resultString= String.format(
              "%s",
              quotientAsPerCentString(activeNsL, totalSinceReportNsL)
              );
    
          // Reset accumulators for next time.
          activeNsL= 0;
          inactiveNsL= 0;
    
          return resultString;
          }
  
      } // DutyCycle

    private DutyCycle writingDutyCycle= new DutyCycle();
    private DutyCycle syncingDutyCycle= new DutyCycle();
    private DutyCycle closingDutyCycle= new DutyCycle();
    private DutyCycle readingDutyCycle= new DutyCycle();
    private long osLastTimeNsL;
    private String osReportString;

    private String getOSReportString()
      {
        long nowTimeNsL= System.nanoTime();
        if  // If 1/2 second has passed
          ( 500000 <= (nowTimeNsL - osLastTimeNsL) ) 
          { // update report String.
            osReportString= "";
            osReportString+=
                " wrt:"+writingDutyCycle.resetAndGetOSString(nowTimeNsL);
            osReportString+=
                " syn:"+syncingDutyCycle.resetAndGetOSString(nowTimeNsL);
            osReportString+=
                " rea:"+readingDutyCycle.resetAndGetOSString(nowTimeNsL);
            osReportString+=
                " clo:"+closingDutyCycle.resetAndGetOSString(nowTimeNsL);
            osLastTimeNsL= nowTimeNsL; // Reset for next time.
            }
  
        return osReportString;
        }

    private static String quotientAsPerCentString(long dividentL,long divisorL)
      {
        String resultString;
        double perCentD= (100. * dividentL) / divisorL;
        if (0.5 > perCentD) 
          resultString= "00%";
        else if (99.5 <= perCentD)
          resultString= "99+";
        else
          resultString= String.format("%02d%%",Math.round(perCentD));
        return resultString;
        }


    // Deletion code.
    
    private File deletionScanFile;
    private long deletionScanByteCountL;
    private int deletionScanFileCountI;
    private int deletionScanDirectoryCountI;
    private Function<File,String> deletionScanFunctionOfFileToString=
      new Function<File,String>() {
        public String apply(File subtreeFile){
          String errorString= null;
          deletionScanFile= subtreeFile;
          if (subtreeFile.isDirectory())
            {
              deletionScanDirectoryCountI++;
              errorString= testInterruptionGetConfirmation1ReturnResultString(
                "Do you want to terminate this file scan?",
                "file scan terminated by user");
              
          }
            else
            {
              deletionScanFileCountI++;
              deletionScanByteCountL+= subtreeFile.length();
              progressReportUpdateMaybeV();
              }
          return errorString;
          } 
        };
    Supplier<String> deletionScanProgressReportSupplierOfString= 
        new Supplier<String>() {
          public String get() { 
            return "\nBytes: "+deletionScanByteCountL
                +"\nFiles: "+deletionScanFileCountI
                +"\nDirectories: "+deletionScanDirectoryCountI
                +"\nName: "+deletionScanFile ; 
            }
          };

    protected String maybeDeleteAllVolumeFilesReturnString(File volumeFile)
      /* This method erases File volumeFile,
       * meaning it deletes all non-hidden files on the volume,
       * if the user gives permission.
       */
      {
        String resultString= null;
      goReturn: {
        deletionScanFile= null;
        deletionScanByteCountL= 0;
        deletionScanFileCountI= 0;
        deletionScanDirectoryCountI= 0;
        queueAndDisplayOutputSlowV(
            "\nPress any key to terminate the following file scan.\n");
        progressReportSetV( deletionScanProgressReportSupplierOfString );
        resultString= FileOps.parentPostorderTraversalReturningString(
          volumeFile, deletionScanFunctionOfFileToString);
        progressReportUpdateV();
        progressReportResetV(); // End progress reports.
        if (null != resultString) // Handle possible traversal termination.
          { // Handle traversal termination.
            progressReportResetV(); // Make last report permanent. 
            if ("file scan terminated by user" == resultString)
              resultString= null; // Consider this not an error.
              else // This is a real error.
              { // Construct larger error String.
                resultString= EpiString.combine1And2WithNewlineString(
                    "directory tree traversal returned error",resultString
                    );
                break goReturn;
                }
            }
        if (!getConfirmationKeyPressB( // Exit if file deletion is not wanted.
            "\nDo you want to ERASE ALL files and subdirectories "
            + "on this volume first?")
            )
          break goReturn;
        java.awt.Toolkit.getDefaultToolkit().beep(); // Get user's attention.
        resultString= "Permission to delete was not confirmed.";
        if (!getConfirmationKeyPressB( // Exit if permission not confirmed.
            "\nAre you CERTAIN that you want to erase the files!?"))
          break goReturn;
        queueAndDisplayOutputSlowV("\n\nDeleting files...");
        resultString= FileOps.deleteChildrenRecursivelyReturnString(
            volumeFile,FileOps.requiredConfirmationString);
        queueAndDisplayOutputSlowV("done.");
      } // goReturn:
      return resultString;
      }

    private String writeTestReturnString(File testFolderFile)
      /* This method does a write test by writing files in
       * the folder specified by buildFolderFile.
       * It returns null if success, an error String if not.
       * 
       * Note that FileDescriptor.sync() is done on each file written
       * before closing.  This seems to have the effect of
       * flushing all data to the OS and writing all OS buffers to 
       * the physical volume.  
       * I'm not certain about the close() after that.
       */
      {
        pushOperationV("write pass");
        String errorString= null;
        speedStartVolumeDoneBytesL= 0;
        FileOutputStream theFileOutputStream= null;
        FileDescriptor theFileDescriptor= null;
        toCheckDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL;
        volumeDoneFilesL= 0; // Index of next file to write.
        passStartTimeMsL= getTimeMsL();
        progressReportUpdateV(); // Initial progress report of write test.
        toCheckRemainingBytesL= testFolderFile.getUsableSpace();
        pushOperationV("FILE-NAME");
        try {
          fileLoop: while (true) {
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            replaceOperationV("file "+checkFile);
            pushOperationAndRefreshProgressReportV("opening............");
            try { 
              theFileOutputStream= new FileOutputStream(checkFile);
              theFileDescriptor= theFileOutputStream.getFD();
              remainingFileBytesL= bytesPerFileL;
              replaceOperationAndRefreshProgressReportV("writing-file-blocks");
              blockLoop: while (true) { // Write all blocks in file.
                progressReportUpdateMaybeV();
                errorString= testInterruptionGetConfirmation1ReturnResultString(
                    "Do you want to terminate this operation?",
                    "write operation terminated by user");
                if (! EpiString.isAbsentB(errorString)) break blockLoop;
                if (0 >= remainingFileBytesL) break blockLoop;
                try { 
                  writeBlockV(
                    theFileOutputStream,toCheckDoneBytesL / bytesPerBlockI); }
                catch (IOException theIOException) {
                  if (deviceFullB(theIOException)) // Convert device-full
                    { theAppLog.debug("writeTestReturnString(.): "
                        + "device-full during file write.");
                      break fileLoop; //  to loop termination.
                      }
                  throw theIOException; // Re-throw other exception subclasses.
                  }
                toCheckDoneBytesL+= bytesPerBlockI;
                toCheckRemainingBytesL-= bytesPerBlockI;
                volumeFreeSpaceL-= bytesPerBlockI;
                remainingFileBytesL-= bytesPerBlockI;
                writtenBytesL= toCheckDoneBytesL;
                } // blockLoop:
              }
            catch (IOException theIOException) {
              if (deviceFullB(theIOException)) // Convert device-full exception
                { theAppLog.debug(
                    "writeTestReturnString(.): device-full during file open.");
                  break fileLoop; //  to loop termination.
                  }
              throw theIOException; // Re-throw other exception subclasses.
              }
            finally {
              replaceOperationAndRefreshProgressReportV("syncing-and-closing");
              volumeDoneFilesL++;
              syncingDutyCycle.updateStatusV(true);
              theFileDescriptor.sync();
              syncingDutyCycle.updateStatusV(false);
              closingDutyCycle.updateStatusV(true);
              theFileOutputStream.close();
              closingDutyCycle.updateStatusV(false);
              theFileOutputStream= null; // Prevent another close.
              popOperationV(); // File operation. 
              if (! EpiString.isAbsentB(errorString)) break fileLoop;
              }
            /// ? Move following into above try block?
          synchronizeWithFreeSpaceV();
          toCheckRemainingBytesL= volumeFreeSpaceL;
          toCheckTotalBytesL= toCheckRemainingBytesL + toCheckDoneBytesL;
          } // fileLoop:
        } catch (Exception theException) {
          errorString= EpiString.combine1And2WithNewlineString(errorString, 
            "VolumeCheck.writeTestReturnString(.) in normal close "
            + theException);
          theAppLog.exception(errorString, theException);
        } finally {
          try {
            if ( theFileOutputStream != null ) theFileOutputStream.close(); 
          } catch ( Exception theException ) {
            errorString= EpiString.combine1And2WithNewlineString(errorString,
              "VolumeCheck.writeTestReturnString(.) in error close "
              + theException);
            theAppLog.exception(errorString, theException);
          } // catch
        } // finally
        popOperationV(); // "FILE-NAME"
        popOperationV("write pass"); 
        return errorString;
        }

    protected boolean deviceFullB(IOException theIOException)
      /* Returns true if theIOException was caused because
       * a volume was or became full.
       */
      {
        return theIOException.getLocalizedMessage().contains(
            "There is not enough space on the disk");
        }

    private void synchronizeWithFreeSpaceV()
      /* This method refreshes the variable 
       * that tracks total volume disk free space. 
       * This is needed because * the amount of free space on the volume
       * depends on more than space occupied by file data.
       * It is also affected by
       * * rounding-up for partially filled blocks,
       * * filesystem overhead associated such as allocation maps
       * * changes in the space being used by other processes
       * This method is called during the write pass and after deleting files.
       */
      {
        volumeFreeSpaceL= volumeFile.getUsableSpace();
        }

    private String readTestReturnString(File testFolderFile)
      /* This method does a read test by reading files in
       * the folder specified by buildFolderFile.
       * It compares the data in each block that it reads 
       * with the pattern that should be there.
       * Any mismatch is considered an error.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        speedStartVolumeDoneBytesL= 0;
        FileInputStream theFileInputStream= null;
        toCheckRemainingBytesL= toCheckDoneBytesL; // Set to read what we wrote.
        toCheckDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL;
        volumeDoneFilesL= 0;
        passStartTimeMsL= getTimeMsL();
        pushOperationV("read-and-compare pass");
        pushOperationV("FILE-NAME");
        try {
          fileLoop: while (true) {
            if (0 >= toCheckRemainingBytesL) // Exit if no more bytes to read. 
              break fileLoop;
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            replaceOperationV("file "+checkFile);
            pushOperationAndRefreshProgressReportV("opening\n");
            theFileInputStream= new FileInputStream(checkFile);
            remainingFileBytesL= Math.min(bytesPerFileL,toCheckRemainingBytesL);
            replaceOperationAndRefreshProgressReportV("reading-and-comparing");
          blockLoop: while (true) {
            progressReportUpdateMaybeV();
            errorString= testInterruptionGetConfirmation1ReturnResultString(
                "Do you want to terminate this operation?",
                "read operation terminated by user");
            if (! EpiString.isAbsentB(errorString)) break blockLoop;
            if (0 >= remainingFileBytesL) break blockLoop;
            errorString= readBlockReturnString(
                theFileInputStream,toCheckDoneBytesL / bytesPerBlockI);
            if (! EpiString.isAbsentB(errorString)) break blockLoop;
            toCheckRemainingBytesL-= bytesPerBlockI;
            remainingFileBytesL-= bytesPerBlockI;
            toCheckDoneBytesL+= bytesPerBlockI;
            readCheckedBytesL= toCheckDoneBytesL;
          } // blockLoop:
            replaceOperationAndRefreshProgressReportV("closing\n");
            theFileInputStream.close();
            popOperationV(); // "opening"
            if (! EpiString.isAbsentB(errorString)) break fileLoop;
            volumeDoneFilesL++;
          } // fileLoop:
        } catch (Exception theException) { 
          errorString= EpiString.combine1And2WithNewlineString(errorString, 
            "VolumeCheck.readTestReturnString(.) in normal close "
            + theException);
          theAppLog.exception(errorString, theException);
        } finally {
          try {
            if ( theFileInputStream != null ) theFileInputStream.close(); 
          } catch ( Exception theException ) { 
            errorString= EpiString.combine1And2WithNewlineString(errorString, 
              "VolumeCheck.readTestReturnString(.) in error close "
              + theException);
            theAppLog.exception(errorString, theException);
          } // catch
        } // finally
        popOperationV(); // "FILE-NAME"
        popOperationV("read-and-compare pass");
        return errorString;
        }

    private void writeBlockV(FileOutputStream theFileOutputStream,long blockL) 
        throws IOException
      /* This method writes a block containing the block number blockI.
       * A block is bytesPerBlockI bytes.
       * It throws IOException if there is an error.
       * ///ehn blockL will eventually be used to write a pattern
       * into the block that is unique to the block. 
       * 
       * ///opt For now use NonDirect ByteBuffer.  Use Direct later for speed.
       */
      {
        byte[] bytes= getPatternedBlockOfBytes(blockL);
        try {
            writingDutyCycle.updateStatusV(true);
            theFileOutputStream.write(bytes);
          } finally {
            writingDutyCycle.updateStatusV(false);
          }
        }

    private String readBlockReturnString(
        FileInputStream theFileInputStream,long blockL) 
      throws IOException
      /* This method reads a block containing the block number blockI.
       * A block is bytesPerBlockI bytes.
       * It throws IOException if there is an error.
       * It returns an error String if the data read is incorrect, 
       * null otherwise.
       */
      {
        String resultString= null;
        byte[] expectedBytes= getPatternedBlockOfBytes(blockL);
        //// readBytes= new byte[bytesPerBlockI];
        readingDutyCycle.updateStatusV(true);
        theFileInputStream.read(readBytes);
        readingDutyCycle.updateStatusV(false);
        boolean equalB= Arrays.equals(expectedBytes,readBytes);
        if (! equalB)
          resultString= "read-back compare error";
        return resultString;
        }


    // Code for Operation part of progress report.
    
    private Deque<String> operationDequeOfStrings; // Describes operation.

    private void replaceOperationAndRefreshProgressReportV(
        String operationString)
      /* Replaces top element of operation stack and refreshes the display. */
      {
        replaceOperationV(operationString);
        progressReportUpdateV();
        }

    private void replaceOperationV(String operationString)
      /* Replaces top element of operation stack. */
      {
        popOperationV();
        pushOperationAndRefreshProgressReportV(operationString);
        }

    private void popOperationV()
      {
        popOperationV(null); // Remove last element if present.
        }

    private void popOperationV(String theString)
      /* Pops the last element from stack and verifies its value.  */
      {
        String removedString= // Remove last element if present.
            operationDequeOfStrings.pollLast();
        if (null != theString)
          if (! theString.equals(removedString))
            theAppLog.debug("VolumeChecker.popOperationV((.)mismatch! " 
                + theString + ", " + removedString);
        }
    
    private void pushOperationAndRefreshProgressReportV(String operationString)
      {
        pushOperationV(operationString);
        progressReportUpdateV();
        }
    
    private void pushOperationV(String operationString)
      {
        operationDequeOfStrings.addLast(operationString); // Add new operation.
        }


    // Pattern volume block code.

    private byte[] patternedBlockOfBytes= new byte[512]; // Initially all zeros.
    private long lastBlockNumberL= Integer.MAX_VALUE;

    private byte[] getPatternedBlockOfBytes(long blockNumberL) 
      /* This method returns a block buffer 
       * filled with lines containing the block number.
       * 
       * ///opt Switch from NonDirect ByteBuffer to Direct one for speed.
       * 
       * ///opt Double-buffer with 2 preallocated buffers for speed.
       */
      {
        if // Calculate new block if block number might have become shorter.
          (blockNumberL < lastBlockNumberL)
          initializePatternedBlockV(blockNumberL);

        // Patch the existing block with desired block number.
        String blockNumberAsString= Long.toString(blockNumberL);
        byte[] blockNumberAsBytes= blockNumberAsString.getBytes();
        int blockNumberAsBytesLengthI= blockNumberAsBytes.length;
        int patchOffsetInBytesI= // Offset to first block # patch is
          32 // the length of one line in bytes
          -2 // minus bytes in line terminator
          -blockNumberAsBytesLengthI; // minus length of block number in bytes.
        for (int i= 0; i < (bytesPerBlockI / 32); i++) {
          System.arraycopy(
              blockNumberAsBytes, 
              0, // Beginning of block number as bytes 
              patternedBlockOfBytes, 
              patchOffsetInBytesI, 
              blockNumberAsBytesLengthI);
          patchOffsetInBytesI+= 32; // Adjust patch offset to next line. 
          }
        lastBlockNumberL= blockNumberL;
        
        return patternedBlockOfBytes;
        }

    private void initializePatternedBlockV(long blockNumberL) 
      /* This method returns a new block buffer.
       * The format for the line that is repeated to the end of the block is:
       *   unique block # nnnnnnnnnnnnnnn
       * These bytes plus 2 bytes for newline make a total of 32 bytes.
       * This is repeated 16 times to fill an entire 512-byte disk block.
       * The "n"s are the character representation of blockNumberL.
       */
      {
        ByteArrayOutputStream blockByteArrayOutputStream= 
            new ByteArrayOutputStream(bytesPerBlockI);
        PrintStream blockPrintStream=
          new PrintStream(blockByteArrayOutputStream);
        for (int i= 0; i < (bytesPerBlockI / 32); i++) { // Repeat to fill block
          blockPrintStream.printf( // with lines of text showing block number.
            "unique block # %15d\r\n", blockNumberL); // Each line is 32 bytes.
          }
        patternedBlockOfBytes= blockByteArrayOutputStream.toByteArray();
        }


    // Volume write-read progress report.

    private String getVolumeWriteReadReportString()
      /* This method builds and returns a complete volume write-read pass
       * progress report string.
       * 
       * ///opt Move this code into a functional interface instance
       * to eliminate redundant lambda expressions referencing this.
       */
      {
        String outputString= ""
            + "\n\nChecking volume..."
              + " " + advanceAndGetSpinnerString() + "\n"
            + columnHeadingString()
            + bytesString()
            + blocksString()
            + filesString()
            + timeString()
            + speedString()
            + osIoString()
            + bytesResultsString()
            + "\n\nOperation: " + operationDequeOfStrings
            + " " + advanceAndGetSpinnerString()
            ;
        return outputString;
        }

    private String columnHeadingString()
      { 
        return String.format("\n        ---Remaining --------Done");
        }

    private String bytesString()
      {
        return groupString("bytes", 1);
        }

    private String blocksString()
      {
        return groupString("blocks", bytesPerBlockI);
        }

    private String filesString()
      {
        return groupString("files", bytesPerFileL);
        }

    /* The following method deals with the problem of translating
      a number of bytes to a number of groups of bytes,
      especially dealing with the last possibly partial group.
      The process is illustrated below with a range of examples
      assuming a group size, bytesPerGroupL, of 3.

      BBBBBB-  totalBytesL == 6, totalGroupsL= 2
      0123456  bytesDoneL
      0001112  groupsDoneL
      2221110  groupsToDoL

      BBBBBBB-  totalBytesL == 7, totalGroupsL= 3
      01234567  bytesDoneL
      00011123  groupsDoneL
      33322210  groupsToDoL

      BBBBBBBB-  totalBytesL == 8, totalGroupsL= 3
      012345678  bytesDoneL
      000111223  groupsDoneL
      333222110  groupsToDoL

      BBBBBBBBB-  totalBytesL == 9, totalGroupsL= 3
      0123456789  bytesDoneL
      0001112223  groupsDoneL
      3332221110  groupsToDoL
     */
    private String groupString( String groupTypeString, long bytesPerGroupL )
      /* This method generates a string for use as 
       * a line in the progress report.
       * groupTypeString is the type of group.
       * bytesPerGroup is self-explanatory.
       * The method produces a line containing the group name,
       * the number of groups that remain to be processed,
       * and the number of groups that have been completely processed.
       * 
       * ///enh RemainingTimeEstimation
       *   Critically Damp the estimate of remaining time to smooth it.
       *   See https://en.wikipedia.org/wiki/Damping
       *   The difficult part is choosing formula time-constants.
       *   They depend on the characteristics of the curve to be smoothed.
       */
      {
        long totalGroupsL= 
            (toCheckTotalBytesL + (bytesPerGroupL-1)) / bytesPerGroupL;
        long groupsDoneL= 
            toCheckDoneBytesL / bytesPerGroupL; // Partial calculation.
        if // Adjust for possible final group.
          (toCheckDoneBytesL >= toCheckTotalBytesL)
          if (toCheckTotalBytesL != (bytesPerGroupL * totalGroupsL))
            groupsDoneL++;
        long groupsToDoL= totalGroupsL - groupsDoneL;
        String resultString= String.format(
          "\n%-7s:%12d:%12d", groupTypeString, groupsToDoL, groupsDoneL );
        return resultString;
        }

    private String timeString()
      {
        long presentTimeMsL= getTimeMsL(); // [Try to] measure the time.
        long safeToCheckDoneBytesL= // Prevent divide-by-zero ahead. 
            (0 == toCheckDoneBytesL) ? Integer.MAX_VALUE : toCheckDoneBytesL;
        doneTimeMsL= presentTimeMsL - passStartTimeMsL;
        long remainingTimeMsL= 
            (doneTimeMsL * toCheckRemainingBytesL) / safeToCheckDoneBytesL ;
        String remainingTimeString= timeToString(remainingTimeMsL);
        String doneTimeString= timeToString(doneTimeMsL);
        String resultString= String.format(
          "\n%-7s:%12s:%12s", 
          "time",
          remainingTimeString, 
          doneTimeString
          );
        return resultString;
        }

    String timeToString(long timeMsL)
      /* This method converts a time timeMsL to a String which it returns. */
      {
        long tL= timeMsL/1000; // Convert ms to seconds.
        long secondsL= tL % 60; // Extracting minute fraction in seconds.
        tL /= 60; // Calculating whole minutes.
        long minutesL = tL % 60; // Extracting hour fraction in minutes.
        tL /= 60; // Calculating whole hours.
        long hoursL= tL % 24; // Extracting day fraction in hours.
        tL /= 24; // Calculating whole days.
        long daysL = tL; // The remainder is days.

        String resultString= String.format(
          "%dD%2dH%2dM%2dS",daysL,hoursL,minutesL,secondsL);
        return resultString;
        }

    private String speedString()
      {
        long presentTimeMsL= getTimeMsL(); // [Try to] measure the time.
        long speedDeltaTimeMsL= presentTimeMsL - speedIntervalStartTimeMsL;
        if (250 <= speedDeltaTimeMsL) // If enough time has passed
          { // report new speed.
            speedL= // Recalculate speed.
              1000 *
              ( (toCheckDoneBytesL - speedStartVolumeDoneBytesL) / 
                speedDeltaTimeMsL)
              ;
            speedStartVolumeDoneBytesL= toCheckDoneBytesL;
            speedIntervalStartTimeMsL= presentTimeMsL;
            }
        return String.format(
            "\nspeed  : %,11d bytes/second", speedL);
        }

    private String osIoString()
      /* This method returns a string containing
       * an indication of how much time the app is waiting for OS IO. 
       */
      { 
        return "\nOS%    : " + getOSReportString(); 
        }


    private String advanceAndGetSpinnerString()
      /* This method steps the spinner if it's time, not too fast,
       * and returns a single character string showing the position.
       */
      {
        final long stepPeriodMsL= 125;
        long presentTimeMsL= getTimeMsL(); // [Try to] measure the time.
        long timeSinceLastStepMsL= presentTimeMsL - timeOfLastSpinnerStepMsL;
        if // Step spinner if it's time.
          ((timeSinceLastStepMsL < 0) || (timeSinceLastStepMsL > stepPeriodMsL))
          { // Step the spinner.
            if (4 <= (++spinnerStepStateI)) spinnerStepStateI= 0; // Step spinner.
            timeOfLastSpinnerStepMsL= presentTimeMsL; // Save time.
            }
        return "-\\|/".substring(spinnerStepStateI, spinnerStepStateI+1);
        }

    protected List<File> getTerminationOrKeyOrAddedVolumeListOfFiles()
      /* This method waits for one of several inputs and then returns.
       * The inputs which terminate the wait are:
       * * termination request: can be tested with
       *   LockAndSignal.testingForInterruptE().
       * * key pressed: can be tested with
       *   testGetFromQueueKeyString() or related methods. 
       * * attached volumes changed: returns the list of volumes added,
       *   can be tested returned (List.size() > 0).
       * Any inputs that happen BEFORE this method is called are ignored.
       */
      {
        ArrayList<File> addedVolumeListOfFiles= new ArrayList<File>();
        File[] oldVolumeFiles= getVolumeFiles();
        while (true) {
          File[] newVolumeFiles= // Get next input.
              getTerminationOrKeyOrChangeOfVolumeFiles(oldVolumeFiles);
          if (null == newVolumeFiles) // If the input was not a volume change 
            break; // its one of the other 2 inputs, so exit.
          for (File outerFile : newVolumeFiles) 
            { // Copy all additions to result list.
              boolean addedB= true; // Assume it was added.
              for (File innerFile : oldVolumeFiles) // Try disproving with each.
                if (innerFile.equals(outerFile)) addedB= false;
              if (addedB) addedVolumeListOfFiles.add(outerFile);
              }
          if (0 < addedVolumeListOfFiles.size()) // Exit if any added volumes.
            break;
          oldVolumeFiles= newVolumeFiles;
          }
        return addedVolumeListOfFiles;
        }

    }
