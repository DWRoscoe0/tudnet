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
     */

    // Locally stored injected dependencies (none).

    // Constants.

      final int bytesPerBlockI= 512;
      final long bytesPerFileL= 64 * 1024 * 1024; // 64 MB
      final long msPerReportMsL= 100; // Trigger limit.
      final long bytesReportPeriodL= 16 * 1024 * 1024; // 16 MB trigger limit.

    // static variables.  None.

    // non-static (instance) variables.

      // Constructor injections.  None.

      // Feature scope.
      File buildFolderFile;

      // Volume scope.
      private long volumeTotalBytesL; // Partition size.
      private long initialVolumeUsedBytesL;
      private long initialVolumeFreeBytesL;
        // Note, free-space/usable-space can change wildly during file IO.

      // Volume pass scope.  Used for both write and read-compare passes.  
      private long toCheckTotalBytesL; // This should be the sum of:
        private long toCheckRemainingBytesL; // Down counter and loop control.
        private long toCheckDoneBytesL; // Up counter and # of next byte to do.

      // Volume read-and-compare pass only scope.
      private long readCheckedBytesL; // Counts bytes read AND compared.
      
      // Time measurement.
      private long checkingStartTimeMsL;
      private long presentTimeMsL;
      private long excessTimeMsL;

      // Progress Reports.
      private int offsetOfProgressReportI; // within Document.
      private int reportNumberI;
      private long byteOfNextReportL;
      private int spinnerStateI;
      private Deque<String> operationDequeOfStrings; // Describes operation.
      @SuppressWarnings("unused") ///
      private long previousReportTimeMsL;
      private long timeOfNextReportMsL;
      
      // For measuring speed of operation.
      private long speedIntervalStartTimeMsL;
      private long speedStartVolumeDoneBytesL;
      private long speedL;

      // File scope (file blocks to write or compare).
      private long volumeDoneFilesL; // Also # of next file to process.
      private File checkFile; // Name of file containing the active block.
      private long remainingFileBytesL; // Down counter and loop control.


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
        queueAndDisplayOutputSlowV(
          "This feature does simple functional testing "
          + "and capacity measurement "
          + "of storage volumes attached to this device.");
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
          if (getConfirmationKeyPressB( // Check volume if user okays it.
               "Would you like to check " + theFile + " ? ")
              )
            checkVolumeV(theFile);
          }
        }

    private void checkVolumeV(File volumeFile)
      /* This method checks the volume specified by volumeFile.
       * This includes optionally deleting all files on the volume,
       * then doing a write-read test
       * 
       * ///rnh Being modified to make write-read test optional.
       */
      {
        theAppLog.debug("VolumeChecker.checkVolumeV(.) begins.");
        String resultString;
      goReturn: {
      goFinish: {
        operationDequeOfStrings= new ArrayDeque<String>();
        readCheckedBytesL=0;
        volumeTotalBytesL= volumeFile.getTotalSpace();
        spinnerStateI= 0;
        checkingStartTimeMsL= presentTimeMsL= getTimeMsL(); // Record start of volume check.
        timeOfNextReportMsL= // Do do first report immediately.
            checkingStartTimeMsL;
        reportNumberI= 0;
        queueAndDisplayOutputSlowV("\n\nChecking " + volumeFile + "\n");
        resultString= deleteAllVolumeFilesReturnString(volumeFile);
        if (! EpiString.isAbsentB(resultString)) break goReturn;
        offsetOfProgressReportI= thePlainDocument.getLength();
        pushOperationV("VolumeChecker");
        buildFolderFile= new File(volumeFile,Config.appString + "Temp");
        initialVolumeFreeBytesL= volumeFile.getUsableSpace();
        toCheckTotalBytesL= initialVolumeFreeBytesL;
        resultString= FileOps.makeDirectoryAndAncestorsString(
            buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) {
          resultString= EpiString.combine1And2WithNewlineString(
              "error occurred while creating folder", resultString);
          break goFinish;
          }
        outputProgressSlowlyV();
        resultString= writeTestReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) {
          resultString= EpiString.combine1And2WithNewlineString(
              "error occurred during write-test", resultString);
          break goFinish;
          }
        resultString= readTestReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) {
          resultString= EpiString.combine1And2WithNewlineString(
              "error occurred during read-test", resultString);
          break goFinish;
          }
      }  // goFinish:
        pushOperationAndRefreshProgressReportV("deleting temporary files");
        theAppLog.debug("VolumeChecker.checkVolumeV(.) deleting.");
        String deleteErrorString= FileOps.deleteRecursivelyReturnString(
            buildFolderFile,FileOps.requiredConfirmationString);
        resultString= EpiString.combine1And2WithNewlineString(
            resultString, deleteErrorString);
        replaceOperationAndRefreshProgressReportV("done");
      }  // goReturn:
        if (! EpiString.isAbsentB(resultString)) // Report error or success.
          appendWithPromptSlowlyAndWaitForKeyV( // Report error.
              "The operation terminated:\n" + resultString);
          else 
          appendWithPromptSlowlyAndWaitForKeyV( // Report success.
            "The operation completed without error.");
        theAppLog.debug("VolumeChecker.checkVolumeV(.) ends.");
        return;
      } // checkVolumeV(.)

    protected String deleteAllVolumeFilesReturnString(File volumeFile)
      /* This method erases File volumeFile,
       * meaning it deletes all non-hidden files on the volume,
       * if the user gives permission.
       */
      {
        //// String resultString= "Permission to delete was refused.";
        String resultString= null;
      goReturn: {
        if (!getConfirmationKeyPressB( // Exit if file deletion is not wanted.
            //// "This operation will first erase "+volumeFile
            //// + " !\nDo you really want to do this?")
            "Do you want erase files on this volume first?")
            )
          break goReturn;
        java.awt.Toolkit.getDefaultToolkit().beep(); // Get user's attention.
        resultString= "Permission to delete was not confirmed.";
        if (!getConfirmationKeyPressB( // Exit if permission not confirmed.
            "\nAre you certain that you want to ERASE "+volumeFile+" ! ?"))
          break goReturn;
        queueAndDisplayOutputSlowV("\n\nDeleting files...");
        resultString= FileOps.deleteRecursivelyReturnString(
            volumeFile,FileOps.requiredConfirmationString);
        queueAndDisplayOutputSlowV("done.");
        resultString= null; // Signal success.
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
        initialVolumeUsedBytesL= 
            volumeTotalBytesL - testFolderFile.getUsableSpace();
        toCheckDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL;
        volumeDoneFilesL= 0; // Index of next file to write.
        byteOfNextReportL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        refreshProgressReportV(); // Initial progress report.
        toCheckRemainingBytesL= testFolderFile.getUsableSpace();
        pushOperationV("FILE-NAME");
        try {
          fileLoop: while (true) {
            accountForFreeSpaceChangesV();
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            replaceOperationV("file "+checkFile);
            pushOperationAndRefreshProgressReportV("opening............");
            try { 
              theFileOutputStream= new FileOutputStream(checkFile);
              theFileDescriptor= theFileOutputStream.getFD();
              remainingFileBytesL= bytesPerFileL;
              replaceOperationAndRefreshProgressReportV("writing-file-blocks");
              blockLoop: while (true) { // Write all blocks in file.
                refreshProgressReportMaybeV();
                errorString= testInterruptionGetConfirmation1ReturnResultString(
                    "Do you want to terminate this operation?",
                    "write operation terminated by user");
                if (! EpiString.isAbsentB(errorString)) break blockLoop;
                if (0 >= remainingFileBytesL) break blockLoop;
                try { writeBlockV(
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
                remainingFileBytesL-= bytesPerBlockI;
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
              theFileDescriptor.sync();
              theFileOutputStream.close();
              theFileOutputStream= null; // Prevent another close.
              popOperationV(); // File operation. 
              if (! EpiString.isAbsentB(errorString)) break fileLoop;
              }
            /// ? Move following into above try block?
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

    private void accountForFreeSpaceChangesV()
      /* This method makes adjustments in variables 
       * used to track the state of write passes.
       * Adjustments are needed because the amount of free space on the volume
       * can change unexpectedly because of 
       * * filesystem overhead associated with 
       *   the files created by this feature, and
       * * changes in the space being used by other processes
       * This method is needed by the write pass, not the read-compare pass.
       * 
       * ///opt It might adjust more frequently as toCheckRemainingBytesL 
       * approaches zero.
       */
      {
        boolean enabledB= true; // For debugging experiments.
        if (enabledB) {
          toCheckRemainingBytesL= buildFolderFile.getUsableSpace();
          toCheckTotalBytesL= toCheckRemainingBytesL + toCheckDoneBytesL;
          }
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
        byteOfNextReportL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        pushOperationV("read-and-compare pass");
        pushOperationV("FILE-NAME");
        try {
          fileLoop: while (true) {
            if (0 >= toCheckRemainingBytesL) // Exit if no more bytes to read. 
              break fileLoop;
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            replaceOperationV("file "+checkFile);
            pushOperationAndRefreshProgressReportV("opening");
            theFileInputStream= new FileInputStream(checkFile);
            remainingFileBytesL= Math.min(bytesPerFileL,toCheckRemainingBytesL);
            replaceOperationAndRefreshProgressReportV("reading");
          blockLoop: while (true) {
            refreshProgressReportMaybeV();
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
            replaceOperationAndRefreshProgressReportV("closing");
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
        theFileOutputStream.write(bytes);
        // theAppLog.debugClockOutV("wb");
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
        byte[] readBytes= new byte[bytesPerBlockI];
        theFileInputStream.read(readBytes);
        boolean equalB= Arrays.equals(expectedBytes,readBytes);
        if (! equalB)
          resultString= "read-back compare error";
        return resultString;
        }

    private byte[] getPatternedBlockOfBytes(long blockNumberL) 
      /* This method returns a block buffer 
       * filled with lines containing the block number.
       * 
       * ///opt Use fewer character operations and more byte moves for speed.
       * 
       * ///opt Switch from NonDirect ByteBuffer to Direct one for speed.
       * 
       * ///opt Double-buffer with 2 preallocated buffers for speed.
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
        byte[] blockBytes= 
            blockByteArrayOutputStream.toByteArray();
        return blockBytes;
        }

    protected String testInterruptionGetConfirmation1ReturnResultString(
        String confirmationQuestionString,String resultDescriptionString)
      {
        String returnString= null; // Assume no interruption.
      toReturn: {
        if  // Exit if no interruption key pressed.
          (null == tryToGetFromQueueKeyString())
          break toReturn;
        if // Exit if the interruption is not confirmed.
          (! getConfirmationKeyPressB(confirmationQuestionString))
          break toReturn;
        returnString= resultDescriptionString; // Override return value.
      } // toReturn:
        return returnString;
      }

    protected boolean getConfirmationKeyPressB(
        String confirmationQuestionString)
      {
        boolean confirmedB= false;
        String responseString= promptSlowlyAndGetKeyString(
          "\n"
          + confirmationQuestionString
          + " [y/n] "
          );
        queueAndDisplayOutputSlowV(responseString); // Echo response.
        responseString= responseString.toLowerCase();
        if ("y".equals(responseString))
          confirmedB= true;
        return confirmedB;
        }

    private void replaceOperationAndRefreshProgressReportV(
        String operationString)
      /* Replaces top element of operation stack and refreshes the display. */
      {
        replaceOperationV(operationString);
        refreshProgressReportV();
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
        refreshProgressReportV();
        }
    
    private void pushOperationV(String operationString)
      {
        operationDequeOfStrings.addLast(operationString); // Add new operation.
        }
    
    private void refreshProgressReportMaybeV()
      /* This method updates the on-screen progress report, maybe.
       * It depends on how much time has passed 
       * and how many bytes have been processed
       * since the previous report.
       */
      {
        /// Thread.yield(); // This didn't help.
      goReturn: {

        presentTimeMsL= getTimeMsL(); // [Try to] measure the time.
        long remainingTimeMsL= presentTimeMsL-timeOfNextReportMsL;
        if // Produce progress report if time remaining in period reached 0.
          (0 <= remainingTimeMsL)
          { // Produce progress report.
            excessTimeMsL= theLockAndSignal.periodCorrectedDelayMsL(
              timeOfNextReportMsL, msPerReportMsL);
            long newTimeMsL= presentTimeMsL + excessTimeMsL + msPerReportMsL;
            /// theAppLog.debug(
            ///     "VolumeChecker.updateProgressMaybeV() time triggered.");
            refreshProgressReportV();
            timeOfNextReportMsL= newTimeMsL; // Calculate next report time.
            break goReturn;
            }

        if (0 <= toCheckDoneBytesL - byteOfNextReportL)
          {
            /// theAppLog.debug(
            ///     "VolumeChecker.updateProgressMaybeV() byte triggered.");
            refreshProgressReportV();
            byteOfNextReportL+= bytesReportPeriodL;
            break goReturn;
            }

      } // goReturn:
        return;
      }

    private void refreshProgressReportV()
      {
        // theAppLog.debug(
        //   "VolumeChecker.updateProgressV() updating.");
        // theAppLog.debugClockOutV("pr");
        String outputString= getProgressReportString();
        replaceDocumentTailAt1With2V(offsetOfProgressReportI, outputString);
        }

    private void outputProgressSlowlyV()
      {
        String outputString= getProgressReportString();
        appendSlowlyV(outputString);
        }
    
    private String getProgressReportString()
      {
        long nowTimeMsL= getTimeMsL();
        String outputString= ""
            + "\n\nProgress-Report-Number: " + (++reportNumberI)
              + " " + advanceAndGetSpinnerString()
            + columnHeadingString()
            + bytesString()
            + blocksString()
            + filesString()
            + timeString()
            + speedString()
            + goodBytesString()
            + "\nOperation: " + operationDequeOfStrings
            ;
        previousReportTimeMsL= nowTimeMsL;
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
       * ///mys ///fix Sometimes displays negative remaining groups.  Fix.
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
        long safeToCheckDoneBytesL= // Prevent divide-by-zero ahead. 
            (0 == toCheckDoneBytesL) ? 1 : toCheckDoneBytesL;
        long doneTimeMsL= presentTimeMsL - checkingStartTimeMsL;
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
          //// "%dd%02dh%02dm%02ds",daysL,hoursL,minutesL,secondsL);
          "%dD%2dH%2dM%2dS",daysL,hoursL,minutesL,secondsL);
        return resultString;
        }

    private String speedString()
      {
        long speedDeltaTimeMsL= presentTimeMsL - speedIntervalStartTimeMsL;
        if (1000 <= speedDeltaTimeMsL) // If enough time has passed
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
            "\nspeed  : %8d bytes/second", speedL);
        }

    private String goodBytesString()
      {
        return 
          "\n" + (initialVolumeUsedBytesL + readCheckedBytesL)
            + " total-usable-bytes = "
          + "\n  " + initialVolumeUsedBytesL + " used + " 
            + readCheckedBytesL + " checked"
          ;
        }

    private String advanceAndGetSpinnerString()
    {
      if (4 <= (++spinnerStateI)) spinnerStateI= 0;
      return "-\\|/".substring(spinnerStateI, spinnerStateI+1);
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

    private long getTimeMsL()
      { 
        return System.currentTimeMillis();
        // return System.nanoTime()/1000;
        }

    }
