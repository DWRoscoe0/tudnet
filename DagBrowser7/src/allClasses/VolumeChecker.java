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
      final long bytesPerFileL= 64 * 1024 * 1024; // 1 GB /// 1024 * 1024 * 1024; // 1 GB
      final long msPerReportMsL= 100;
      /// final long msPerReportMsL= 1000000; // 1 second. for ns.
      final long bytesReportPeriodL= 16 * 1024 * 1024; // 16 MB

    // static variables.

    // instance variables.

      // Constructor-injected variables.

      // Other instance variables.
      
        private long presentTimeMsL;
        private long excessTimeMsL;
        private long presentReportTimeMsL;
        private long previousReportTimeMsL;
        private long timeOfNextReportMsL;
        private long byteOfNextReportMsL;

        private int reportNumberI;

        private long volumeTotalBytesL; // Total partition size.
        private long initialVolumeFreeBytesL;
        private long initialVolumeUsedBytesL;
        private long volumeToDoBytesL;
        private long volumeDoneBytesL; // Also # of next byte to process.
        private long previousVolumeDoneBytesL;
        private long readCheckedBytesL;

        private long volumeToDoBlocksL;
        private long volumeDoneBlocksL;
        private long previousVolumeDoneBlocksL;

        private long volumeDoneFilesL; // Also # of next file to process.
        private long previousvolumeDoneFilesL;
        private File checkFile; // The file being processed.

        private int offsetOfProgressReportI;
        private long remainingFileBytesL;
        private int spinnerStateI;
        private String operationString;

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
       * 
       */
      {
        theAppLog.debug("VolumeChecker.checkVolumeV(.) begins.");
        String resultString;
        readCheckedBytesL=0;
        volumeTotalBytesL= volumeFile.getTotalSpace();
        initialVolumeFreeBytesL= volumeFile.getUsableSpace(); 
        spinnerStateI= 0;
        timeOfNextReportMsL= getTimeMsL(); // Do do first report immediately.
        reportNumberI= 0;
        queueAndDisplayOutputSlowV("\n\nChecking " + volumeFile + "\n");
        offsetOfProgressReportI= thePlainDocument.getLength();
        File testFolderFile= new File(volumeFile,"InfogoraTemp");
      goReturn: {
      goFinish: {
        resultString= FileOps.makeDirectoryAndAncestorsString(
            testFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error creating folder", resultString);
          break goFinish;
          }
        resultString= writeTestReturnString(testFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error during write-test", resultString);
          break goFinish;
          }
        //// volumeToDoBytesL= volumeDoneBytesL;
        resultString= readTestReturnString(testFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error during read-test", resultString);
          break goFinish;
          }
      }  // goFinish:
        setAndDisplayOperationV("deleting temporary files");
        theAppLog.debug("VolumeChecker.writeTestReturnString(.) deleting.");
        //// java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.
        String deleteErrorString= FileOps.deleteRecursivelyReturnString(
            testFolderFile,FileOps.requiredConfirmationString);
        resultString= combineLinesString(resultString, deleteErrorString);
        setAndDisplayOperationV("completed");
        if (! isAbsentB(resultString)) { // Report error.
          reportWithPromptSlowlyAndWaitForKeyV(
              "Abnormal termination:\n" + resultString);
          break goReturn;
          }
        //// queueAndDisplayOutputSlowV(
        reportWithPromptSlowlyAndWaitForKeyV(
          "The operation completed without error.");
      }  // goReturn:
        return;
      } // checkVolumeV(._

    private String writeTestReturnString(File testFolderFile)
      /* This method does a write test by writing files in
       * the folder specified by testFolderFile.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        FileOutputStream theFileOutputStream= null;
        //// spinnerStateI= 0;
        initialVolumeUsedBytesL= volumeTotalBytesL - testFolderFile.getUsableSpace();
        volumeDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL;
        volumeDoneBlocksL= 0; // Next block to write.
        volumeDoneFilesL= 0; // Index of next file to write.
        //// timeOfNextReportMsL= getTimeMsL(); // Do do first report immediately.
        byteOfNextReportMsL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        //// reportNumberI= 0;
        setAndDisplayOperationV("starting");
        displayProgressV(); // Initial progress report.
        try {
          fileLoop: while (true) {
            volumeToDoBytesL= // Update remaining space to fill.
                testFolderFile.getUsableSpace();
            if (0 >= volumeToDoBytesL) // Exit if no more bytes to write. 
              break fileLoop;
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            setAndDisplayOperationV("opening file ");
            theFileOutputStream= new FileOutputStream(checkFile);
            remainingFileBytesL= Math.min(bytesPerFileL,volumeToDoBytesL);
            setAndDisplayOperationV("writing file blocks");
          blockLoop: while (true) {
            displayProgressMaybeV();
            errorString= testInterruptionGetConfirmation1ReturnResultString(
                "Do you want to terminate this operation?",
                "write operation terminated by user");
            if (! isAbsentB(errorString)) break blockLoop;
            if (0 >= remainingFileBytesL) break blockLoop;
            writeBlockV(theFileOutputStream,volumeDoneBlocksL);
            volumeDoneBlocksL++;
            volumeToDoBytesL-= bytesPerBlockI;
            remainingFileBytesL-= bytesPerBlockI;
            volumeDoneBytesL+= bytesPerBlockI;
          } // blockLoop:
            setAndDisplayOperationV("closing file");
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

    private String readTestReturnString(File testFolderFile)
      /* This method does a read test by reading files in
       * the folder specified by testFolderFile.
       * It compares the data in each block that it reads 
       * with the pattern that should be there.
       * Any mismatch is considered an error.
       * It returns null if success, an error String if not.
       */
      {
        String resultString= null;
        FileInputStream theFileInputStream= null;
        volumeToDoBytesL= volumeDoneBytesL; // Set to read what we wrote.
        volumeDoneBytesL=0;
        remainingFileBytesL= bytesPerFileL;
        volumeDoneBlocksL= 0;
        volumeDoneFilesL= 0;
        byteOfNextReportMsL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        setAndDisplayOperationV("starting read-back test");
        displayProgressV(); // Initial progress report.
        try {
          fileLoop: while (true) {
            //// volumeToDoBytesL= // Update remaining space.
            ////     testFolderFile.getUsableSpace();
            if (0 >= volumeToDoBytesL) // Exit if no more bytes to read. 
              break fileLoop;
            checkFile= new File(testFolderFile,"tmp"+volumeDoneFilesL+".txt");
            setAndDisplayOperationV("opening file ");
            //// theFileOutputStream= new FileOutputStream(checkFile);
            theFileInputStream= new FileInputStream(checkFile);
            remainingFileBytesL= Math.min(bytesPerFileL,volumeToDoBytesL);
            setAndDisplayOperationV("reading file blocks");
          blockLoop: while (true) {
            displayProgressMaybeV();
            resultString= testInterruptionGetConfirmation1ReturnResultString(
                "Do you want to terminate this operation?",
                "read operation terminated by user");
            if (! isAbsentB(resultString)) break blockLoop;
            if (0 >= remainingFileBytesL) break blockLoop;
            resultString= readBlockReturnString(
                theFileInputStream,volumeDoneBlocksL);
            if (! isAbsentB(resultString)) break blockLoop;
            volumeDoneBlocksL++;
            volumeToDoBytesL-= bytesPerBlockI;
            remainingFileBytesL-= bytesPerBlockI;
            volumeDoneBytesL+= bytesPerBlockI;
            readCheckedBytesL= volumeDoneBytesL; 
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
        //// byte[] bytes= new byte[bytesPerBlockI];
        //// Arrays.fill(bytes, (byte)'x');
        theFileOutputStream.write(bytes);
        }

    private String readBlockReturnString(
        FileInputStream theFileInputStream,long blockL) 
      throws IOException
      /* This method reads a block containing the block number blockI.
       * A block is bytesPerBlockI bytes.
       * It throws IOException if there is an error.
       * It returns true if the data read is incorrect, false otherwise.
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

    private byte[] getPatternedBlockOfBytes(long blockL) 
      /* This method returns a block buffer containing the block number blockI.
       * 
       * ///opt For now use NonDirect ByteBuffer.  Use Direct later for speed.
       */
      {
        ByteArrayOutputStream blockByteArrayOutputStream= 
            new ByteArrayOutputStream(bytesPerBlockI);
        //// PrintWriter blockPrintWriter=
        ////     new PrintWriter(blockByteArrayOutputStream);
        PrintStream blockPrintStream=
          new PrintStream(blockByteArrayOutputStream);
        for (int i= 0; i < (bytesPerBlockI / 32); i++) { // Repeat to fill block
          //// blockPrintWriter.printf( // with lines of text showing block number.
          blockPrintStream.printf( // with lines of text showing block number.
            "block number = %15d\r\n", blockL);
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
        displayProgressV();
        theAppLog.debug(
            "VolumeChecker.setAndDisplayOperationV(.): " + operationString);
        }
    
    private void displayProgressMaybeV()
      {
        /// Thread.yield(); // This didn't help.
      goReturn: {

        presentTimeMsL= getTimeMsL(); // [Try to] measure the time.
        long remainingTimeMsL= presentTimeMsL-timeOfNextReportMsL;
        if // Produce progress report if time remaining in period reached 0.
          (0 <= remainingTimeMsL)
          { // Produce progress report.
            presentReportTimeMsL= presentTimeMsL;
            excessTimeMsL= theLockAndSignal.periodCorrectedDelayMsL(
              timeOfNextReportMsL, msPerReportMsL);
            long newTimeMsL= presentTimeMsL + excessTimeMsL + msPerReportMsL;
            theAppLog.debug(
                "VolumeChecker.updateProgressMaybeV() time triggered.");
            displayProgressV();
            timeOfNextReportMsL= newTimeMsL; // Calculate next report time.
            break goReturn;
            }

        if (0 <= volumeDoneBytesL - byteOfNextReportMsL)
          {
            theAppLog.debug(
                "VolumeChecker.updateProgressMaybeV() byte triggered.");
            displayProgressV();
            byteOfNextReportMsL+= bytesReportPeriodL;
            break goReturn;
            }

      } // goReturn:
        return;
      }

    private void displayProgressV()
      {
        // theAppLog.debug(
        //   "VolumeChecker.updateProgressV() updating.");
        // theAppLog.debugClockOutV("pr");
        long nowTimeMsL= getTimeMsL();
        String outputString= ""
            + "\nProgress-Report-Number: " + (++reportNumberI)
              + " " + advanceAndGetSpinnerString()
            + goodBytesString()
            + columnHeadingString()
            + bytesString()
            + blocksString()
            + filesString()
            + "\nFile: " + checkFile
            + "\nVolume-Blocks-Done: " + volumeDoneBlocksL
            + "\nFile-Bytes-Remaining: " + remainingFileBytesL
            + "\nVolume-Bytes-Remaining: " + volumeToDoBytesL
            + "\nDelta-Time: " + (nowTimeMsL - previousReportTimeMsL) 
            + "\nOperation: " + operationString
            ;
        previousReportTimeMsL= nowTimeMsL;
        replaceDocumentTailAt1With2V(offsetOfProgressReportI, outputString);
        }

    private String columnHeadingString()
      { 
        return String.format("\nUnit-- ----To-Do -----Done");
        }

    private String bytesString()
      {
        String resultString= String.format(
            "\nbytes- %9d %9d", 
            volumeToDoBytesL, volumeDoneBytesL);
        previousVolumeDoneBytesL= volumeDoneBytesL;
        return resultString;
        }

    private String blocksString()
      {
        String resultString= String.format(
          "\nblocks %9d %9d",
            volumeToDoBlocksL, volumeDoneBlocksL);
        previousVolumeDoneBlocksL= volumeDoneBlocksL;
        return resultString;
        }

    private String goodBytesString()
      {
        return 
          "\n" + (initialVolumeUsedBytesL + readCheckedBytesL)
            + " good-bytes = "
          + "\n  " + initialVolumeUsedBytesL + " used + " 
            + readCheckedBytesL + " checked"
          ;
        }

    private String filesString()
      {
        /*  ////
        long deltaFilesL= volumeDoneFilesL - previousvolumeDoneFilesL;
        String resultString= 
          String.format("\nfiles       %4d =-   %4d +=   %4d", 
            previousvolumeDoneFilesL, deltaFilesL, volumeDoneFilesL);
        previousvolumeDoneFilesL= volumeDoneFilesL;
        return resultString;
        */  ////
        return "\nFILES WILL GO HERE.";
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
