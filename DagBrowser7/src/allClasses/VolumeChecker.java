package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/// import allClasses.LockAndSignal.Input;


public class VolumeChecker

  /* This class is used to check a mass storage volume.
   * Presently the only test is does is to write all remaining free space.
   * 
   * ///enh Eventually it should also:
   * * Verify that each block can store unique data,
   *   testing for volumes with fake sizes.
   * 
   * When checking large storage devices, long pauses can happen in the process.
   * The pauses are probably caused by flushing large sections of disk cache
   * to the storage device.  Pauses can happen:
   * * At file close time.
   * * In the middle of a write of one of the files.
   */
  extends VolumeDetector

  {
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
      File temporaryFolderFile;

      // Volume scope.
      private long volumeTotalBytesL; // Partition size.
      private long initialVolumeUsedBytesL;
      private long initialVolumeFreeBytesL;
        // Note, free-space/usable-space can change wildly during file IO.

      // Volume pass scope.  Used for both write and read-compare passes.  
      private long toCheckTotalBytesL;
      private long toCheckRemainingBytesL; // Down counter.
      private long toCheckDoneBytesL; // Up counter and # of next byte to do.
      private long changeInFreeSpaceL;

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
      private String operationString; // Describes present operation.
      private long previousReportTimeMsL;
      private long timeOfNextReportMsL;
      
      // For measuring speed of operation.
      private long speedIntervalStartTimeMsL;
      private long speedStartVolumeDoneBytesL;
      private long speedL;

      // File scope.
      private long volumeDoneFilesL; // Also # of next file to process.
      private File checkFile; // Name of file containing the active block.
      private long remainingFileBytesL; // Remaining bytes to write or compare.


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
        theAppLog.debug(
          myToString()+"VolumeChecker.VolumeChecker(.) ends, nameString='"+nameString+"'");
        }

    protected void mainThreadLogicV()
      // This overrides the superclass method. 
      {
        queueAndDisplayOutputSlowV(
          "This feature does simple functional testing of storage volumes "
          + "attached to this device.");
        List<File> addedVolumeListOfFiles;
      theLoop: while(true) {
        addedVolumeListOfFiles= getTerminationOrKeyOrAddedVolumeListOfFiles();
        if (LockAndSignal.isInterruptedB()) break; // Process exit request.
        if (0 < addedVolumeListOfFiles.size()) { // If any volumes added
          checkVolumeListV( // check each added volume that has user's consent
              addedVolumeListOfFiles);
          continue theLoop;
          }
        checkVolumeListV( // Check each attached volume that has user's consent.
            Arrays.asList(getVolumeFiles()));
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
       */
      {
        theAppLog.debug("VolumeChecker.checkVolumeV(.) begins.");
        String resultString;
        readCheckedBytesL=0;
        volumeTotalBytesL= volumeFile.getTotalSpace();
        spinnerStateI= 0;
        checkingStartTimeMsL= getTimeMsL(); // Record start of volume check.
        timeOfNextReportMsL= checkingStartTimeMsL; // Do do first report immediately.
        reportNumberI= 0;
        queueAndDisplayOutputSlowV("\n\nChecking " + volumeFile + "\n");
        offsetOfProgressReportI= thePlainDocument.getLength();
        temporaryFolderFile= new File(volumeFile,"InfogoraTemp");
        initialVolumeFreeBytesL= volumeFile.getUsableSpace();
        toCheckTotalBytesL= initialVolumeFreeBytesL;
      goReturn: {
      goFinish: {
        resultString= FileOps.makeDirectoryAndAncestorsString(
            temporaryFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error creating folder", resultString);
          break goFinish;
          }
        outputProgressSlowlyV();
        resultString= writeTestReturnString(temporaryFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error during write-test", resultString);
          break goFinish;
          }
        resultString= readTestReturnString(temporaryFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error during read-test", resultString);
          break goFinish;
          }
      }  // goFinish:
        setAndDisplayOperationV("deleting temporary files");
        theAppLog.debug("VolumeChecker.writeTestReturnString(.) deleting.");
        String deleteErrorString= FileOps.deleteRecursivelyReturnString(
            temporaryFolderFile,FileOps.requiredConfirmationString);
        resultString= combineLinesString(resultString, deleteErrorString);
        setAndDisplayOperationV("completed");
        if (! isAbsentB(resultString)) { // Report error.
          reportWithPromptSlowlyAndWaitForKeyV(
              "Abnormal termination:\n" + resultString);
          break goReturn;
          }
        reportWithPromptSlowlyAndWaitForKeyV(
          "The operation completed without error.");
      }  // goReturn:
        return;
      } // checkVolumeV(._

    private String writeTestReturnString(File testFolderFile)
      /* This method does a write test by writing files in
       * the folder specified by temporaryFolderFile.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        FileOutputStream theFileOutputStream= null;
        initialVolumeUsedBytesL= volumeTotalBytesL - testFolderFile.getUsableSpace();
        toCheckDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL; //// ?
        volumeDoneFilesL= 0; // Index of next file to write.
        byteOfNextReportL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        setAndDisplayOperationV("starting");
        refreshProgressV(); // Initial progress report.
        toCheckRemainingBytesL= testFolderFile.getUsableSpace();
        try {
          fileLoop: while (true) {
            accountForFreeSpaceChangesV();
            //// toCheckRemainingBytesL= temporaryFolderFile.getUsableSpace();
            //// if (0 >= toCheckRemainingBytesL) // Exit if no more bytes to write. 
            ////   break fileLoop;
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            setAndDisplayOperationV("opening file "+checkFile);
            try { theFileOutputStream= new FileOutputStream(checkFile); }
            catch (IOException theIOException) {
              if (deviceFullB(theIOException)) // Convert device-full exception
                { theAppLog.debug(
                    "writeTestReturnString(.): device-full during file open.");
                  break fileLoop; //  to loop termination.
                  }
              throw theIOException; // Re-throw other exception subclasses.
              }
            //// remainingFileBytesL= Math.min(bytesPerFileL,toCheckRemainingBytesL);
            remainingFileBytesL= bytesPerFileL;
            setAndDisplayOperationV("writing file blocks, "+volumeDoneFilesL); ///////
          blockLoop: while (true) {
            //// accountForFreeSpaceChangesV();
            refreshProgressMaybeV();
            errorString= testInterruptionGetConfirmation1ReturnResultString(
                "Do you want to terminate this operation?",
                "write operation terminated by user");
            if (! isAbsentB(errorString)) break blockLoop;
            if (0 >= remainingFileBytesL) break blockLoop;
            //// if (0 >= toCheckRemainingBytesL) // Exit if no more bytes to write. 
            ////   break blockLoop;
            try { writeBlockV(
              theFileOutputStream,toCheckDoneBytesL / bytesPerBlockI); }
            catch (IOException theIOException) {
              if (deviceFullB(theIOException)) // Convert device-full exception
                { theAppLog.debug(
                    "writeTestReturnString(.): device-full during file write.");
                  break blockLoop; //  to loop termination.
                  }
              throw theIOException; // Re-throw other exception subclasses.
              }
            toCheckDoneBytesL+= bytesPerBlockI;
            toCheckRemainingBytesL-= bytesPerBlockI;
            remainingFileBytesL-= bytesPerBlockI;
          } // blockLoop:
            setAndDisplayOperationV("closing file "+checkFile);
            theFileOutputStream.close();
            if (! isAbsentB(errorString)) break fileLoop;
            volumeDoneFilesL++;
          } // fileLoop:
        } catch (Exception theException) { 
          theAppLog.exception(
              "VolumeCheck.writeTestString(.)", theException);
        } finally {
          try {
            if ( theFileOutputStream != null ) theFileOutputStream.close(); 
          } catch ( Exception theException ) { 
            theAppLog.exception(
                "VolumeCheck.writeTestReturnString(.)", theException);
          } // catch
        } // finally
        return errorString;
        }

    private boolean deviceFullB(IOException theIOException)
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
          toCheckRemainingBytesL= temporaryFolderFile.getUsableSpace();
          toCheckTotalBytesL= toCheckRemainingBytesL + toCheckDoneBytesL;
          }
        }

    private String readTestReturnString(File testFolderFile)
      /* This method does a read test by reading files in
       * the folder specified by temporaryFolderFile.
       * It compares the data in each block that it reads 
       * with the pattern that should be there.
       * Any mismatch is considered an error.
       * It returns null if success, an error String if not.
       */
      {
        String resultString= null;
        FileInputStream theFileInputStream= null;
        toCheckRemainingBytesL= toCheckDoneBytesL; // Set to read what we wrote.
        toCheckDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL;
        volumeDoneFilesL= 0;
        byteOfNextReportL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        setAndDisplayOperationV("starting read-back test");
        refreshProgressV(); // Initial progress report.
        try {
          fileLoop: while (true) {
            if (0 >= toCheckRemainingBytesL) // Exit if no more bytes to read. 
              break fileLoop;
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            setAndDisplayOperationV("opening file ");
            theFileInputStream= new FileInputStream(checkFile);
            remainingFileBytesL= Math.min(bytesPerFileL,toCheckRemainingBytesL);
            setAndDisplayOperationV("reading file blocks");
          blockLoop: while (true) {
            refreshProgressMaybeV();
            resultString= testInterruptionGetConfirmation1ReturnResultString(
                "Do you want to terminate this operation?",
                "read operation terminated by user");
            if (! isAbsentB(resultString)) break blockLoop;
            if (0 >= remainingFileBytesL) break blockLoop;
            resultString= readBlockReturnString(
                theFileInputStream,toCheckDoneBytesL / bytesPerBlockI);
            if (! isAbsentB(resultString)) break blockLoop;
            toCheckRemainingBytesL-= bytesPerBlockI;
            remainingFileBytesL-= bytesPerBlockI;
            toCheckDoneBytesL+= bytesPerBlockI;
            readCheckedBytesL= toCheckDoneBytesL; 
          } // blockLoop:
            setAndDisplayOperationV("closing file");
            theFileInputStream.close();
            if (! isAbsentB(resultString)) break fileLoop;
            volumeDoneFilesL++;
          } // fileLoop:
        } catch (Exception theException) { 
          theAppLog.exception(
              "VolumeCheck.readTestReturnString(.)", theException);
        } finally {
          try {
            if ( theFileInputStream != null ) theFileInputStream.close(); 
          } catch ( Exception theException ) { 
            theAppLog.exception(
                "VolumeCheck.readTestReturnString(.)", theException);
          } // catch
        } // finally
        return resultString;
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
        theAppLog.debugClockOutV("wb");
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

    private String combineLinesString(
        String the1String,String the2String)
      {
        String valueString;
      toReturn: {
        if (isAbsentB(the1String)) // If there is no string 1
          { valueString= the2String; break toReturn; } // return string 2.
        if (isAbsentB(the2String)) // If there is no string 2
          { valueString= the1String; break toReturn; } // return string 1.
        valueString= // Neither string is null so return a combination of both:
          the1String 
          + ",\n" // with a line separator between them.
          + the2String; // 
      } // toReturn:
        return valueString;
      }

    private static boolean isAbsentB(String theString)
      /* This method returns true if theString is null or "", 
       * false otherwise. 
       */
      {
        boolean valueB;
      toReturn: {
        if (null == theString) // If string is null
          { valueB= true; break toReturn; } // return true.
        if (theString.isEmpty()) // If non-null string is empty
          { valueB= true; break toReturn; } // return true.
        valueB= false; // Otherwise return false. 
      } // toReturn:
        return valueB;
      }

    private String testInterruptionGetConfirmation1ReturnResultString(
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

    private boolean getConfirmationKeyPressB(
        String confirmationQuestionString)
      {
        boolean confirmedB= false;
        String responseString= promptSlowlyAndGetKeyString(
          "\n\n"
          + confirmationQuestionString
          + " [y/n] "
          );
        queueAndDisplayOutputSlowV(responseString); // Echo response.
        responseString= responseString.toLowerCase();
        if ("y".equals(responseString))
          confirmedB= true;
        return confirmedB;
        }
    
    private void setAndDisplayOperationV(String operationString)
      {
        this.operationString= operationString;
        refreshProgressV();
        theAppLog.debug(
            "VolumeChecker.setAndDisplayOperationV(.): " + operationString);
        }
    
    private void refreshProgressMaybeV()
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
            refreshProgressV();
            timeOfNextReportMsL= newTimeMsL; // Calculate next report time.
            break goReturn;
            }

        if (0 <= toCheckDoneBytesL - byteOfNextReportL)
          {
            /// theAppLog.debug(
            ///     "VolumeChecker.updateProgressMaybeV() byte triggered.");
            refreshProgressV();
            byteOfNextReportL+= bytesReportPeriodL;
            break goReturn;
            }

      } // goReturn:
        return;
      }

    private void refreshProgressV()
      {
        // theAppLog.debug(
        //   "VolumeChecker.updateProgressV() updating.");
        theAppLog.debugClockOutV("pr");
        String outputString= getProgressReportString();
        replaceDocumentTailAt1With2V(offsetOfProgressReportI, outputString);
        }

    private void outputProgressSlowlyV()
      {
        String outputString= getProgressReportString();
        outputSlowlyV(outputString);
        }
    
    private String getProgressReportString()
      {
        long nowTimeMsL= getTimeMsL();
        String outputString= ""
            + "\nProgress-Report-Number: " + (++reportNumberI)
              + " " + advanceAndGetSpinnerString()
            + columnHeadingString()
            + bytesString()
            + blocksString()
            + filesString()
            + timeString()
            + speedString()
            + "\nFile: " + checkFile
            + "\nchangeInFreeSpaceL: " + changeInFreeSpaceL
            + "\nDelta-Time: " + (nowTimeMsL - previousReportTimeMsL) 
            + goodBytesString()
            + "\nOperation: " + operationString
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
          ////// This ends being non-zero!  
          ////// Because toCheckTotalBytesL is not adjusted down?
        String resultString= String.format(
          "\n%-7s:%12d %12d", groupTypeString, groupsToDoL, groupsDoneL );
        return resultString;
        }

    private String timeString()
      {
        String resultString= String.format(
          "\n%-7s:%12s %12s", 
          "time", 
          "?", 
          (presentTimeMsL - checkingStartTimeMsL)+" ms"
          );
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
            "\nspeed : %d bytes/second", speedL);
        }

    private String goodBytesString()
      {
        return 
          "\n" + (initialVolumeUsedBytesL + readCheckedBytesL)
            + " total-good-bytes = "
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
