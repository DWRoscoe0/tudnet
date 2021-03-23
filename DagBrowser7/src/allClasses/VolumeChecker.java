package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
      final long msPerReportMsL= 100; //// 1000; // 1 second.
      /// final long msPerReportMsL= 1000000; // 1 second. for ns.
      final long bytesReportPeriodL= 16 * 1024 * 1024; // 16 MB

    // static variables.

    // instance variables.

      // Constructor-injected variables.

      // Other instance variables.
      
        private String operationString;
        private long presentTimeMsL;
        private long excessTimeMsL;
        private int reportNumberI;
        private long timeOfNextReportMsL;
        private long byteOfNextReportMsL;
        private long previousReportTimeMsL;
        private File testFile;
        private int offsetOfProgressReportI;
        private long volumeBlockNumberL;
        private long remainingFileBytesL;
        private long remainingVolumeBytesL;
        private long nextVolumeByteL;
        private int spinnerStateI;

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
          checkVolumeListV(addedVolumeListOfFiles); // check only those volumes.
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
          "\n\nAdded volume[s]: "
          + addedVolumeListOfFiles.toString()
          );
        for (File theFile : addedVolumeListOfFiles) {
          String keyString= promptSlowlyAndGetKeyString(
            "\nWould you like to check volume "
            + theFile 
            + " [y/n] ? "
            );
          queueAndDisplayOutputSlowV(keyString);
          if ("y".equals(keyString))
            checkVolumeV(theFile);
          }
        }

    private void checkVolumeV(File volumeFile)
      /* This method checks the volume specified by volumeFile.
       * 
       */
      {
        theAppLog.debug("VolumeChecker.checkVolumeV(.) begins.");
        String errorString; 
        queueAndDisplayOutputSlowV("\n\nChecking " + volumeFile + "\n");
        File testFolderFile= new File(volumeFile,"InfogoraTest");
      goReturn: {
      goFinish: {
        errorString= FileOps.makeDirectoryAndAncestorsString(testFolderFile);
        if (null != errorString) {
          reportWithPromptSlowlyAndGetKeyString(
              "\n\nError creating folder: " + errorString);
          break goFinish;
          }
        errorString= writeTestReturnString(testFolderFile);
        if (null != errorString) {
          reportWithPromptSlowlyAndGetKeyString(
              "\n\nError doing write-test: " + errorString);
          break goFinish;
          }
      }  // goFinish:
        if (null != errorString) {
          reportWithPromptSlowlyAndGetKeyString(errorString);
          break goReturn;
          }
        theAppLog.debug("VolumeChecker.checkVolumeV(.) deleting done.");
        queueAndDisplayOutputSlowV(" Done.\n");
      }  // goReturn:
        return;
      } // checkVolumeV(._

    private String writeTestReturnString(File testFolderFile)
      /* This method does a write test by writing files in
       * the folder specified by testFolderFile.
       * It returns null if success, an error String if not.
       * 
       *  ///fix Return error string in all cases of errors.
       */
      {
        String errorString= null;
        FileOutputStream theFileOutputStream= null;
        spinnerStateI= 0;
        remainingFileBytesL= bytesPerFileL;
        volumeBlockNumberL= 0; // Next block to write.
        int fileI= 0; // Index of next file to write.
        offsetOfProgressReportI= thePlainDocument.getLength();
        timeOfNextReportMsL= getTimeMsL(); // Do do first report immediately.
        byteOfNextReportMsL= 0;
        previousReportTimeMsL= timeOfNextReportMsL;
        reportNumberI= 0;
        setAndDisplayOperationV("starting");
        updateProgressDisplayV(); // Initial progress report.
        try {
          fileLoop: while (true) {
            remainingVolumeBytesL= // Update remaining space to fill.
                testFolderFile.getUsableSpace();
            if (0 >= remainingVolumeBytesL) // Exit if no more bytes to write. 
              break fileLoop;
            testFile= new File(testFolderFile,""+fileI);
            setAndDisplayOperationV("opening file ");
            theFileOutputStream= new FileOutputStream(testFile);
            remainingFileBytesL= Math.min(bytesPerFileL,remainingVolumeBytesL);
            setAndDisplayOperationV("writing file blocks");
          blockLoop: while (true) {
            updateProgressMaybeV();
            errorString= testForKeyReturnString();
            if (null != errorString) break blockLoop;
            if (0 >= remainingFileBytesL) break blockLoop;
            writeBlockV(theFileOutputStream,volumeBlockNumberL);
            volumeBlockNumberL++;
            remainingVolumeBytesL-= bytesPerBlockI;
            remainingFileBytesL-= bytesPerBlockI;
            nextVolumeByteL+= bytesPerBlockI;
          } // blockLoop:
            setAndDisplayOperationV("closing file");
            theFileOutputStream.close();
            fileI++;
          } // fileLoop:
        } catch (Exception theException) { 
          theAppLog.exception(
              "VolumeCheck.writeTestString(.)", theException);
        } finally {
          try {
            if ( theFileOutputStream != null ) theFileOutputStream.close(); 
          } catch ( Exception theException ) { 
            theAppLog.exception(
                "VolumeCheck.writeTestString(.)", theException);
          } // catch
        } // finally
        setAndDisplayOperationV("deleting temporary files");
        theAppLog.debug("VolumeChecker.writeTestReturnString(.) deleting.");
        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.
        errorString= FileOps.deleteRecursivelyReturnString(
            testFolderFile,FileOps.requiredConfirmationString);
        setAndDisplayOperationV("done");
        return errorString;
        }

    private String testForKeyReturnString()
      { 
        String keyString= tryToGetFromQueueKeyString();
        if (null != keyString) {
          reportWithPromptSlowlyAndGetKeyString("Interrupted.");
          }
        return null; // Always return null for now.  ////////// 
        }
    
    private void setAndDisplayOperationV(String operationString)
      {
        this.operationString= operationString;
        updateProgressDisplayV();
        theAppLog.debug(
            "VolumeChecker.setAndDisplayOperationV(.): " + operationString);
        }
    
    private void updateProgressMaybeV()
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
            theAppLog.debug(
                "VolumeChecker.updateProgressMaybeV() time triggered.");
            updateProgressDisplayV();
            timeOfNextReportMsL= newTimeMsL; // Calculate next report time.
            break goReturn;
            }

        if (0 <= nextVolumeByteL - byteOfNextReportMsL)
          {
            theAppLog.debug(
                "VolumeChecker.updateProgressMaybeV() byte triggered.");
            updateProgressDisplayV();
            byteOfNextReportMsL+= bytesReportPeriodL;
            break goReturn;
            }

      } // goReturn:
        return;
      }

    private void updateProgressDisplayV()
      {
        // theAppLog.debug(
        //   "VolumeChecker.updateProgressV() updating.");
        theAppLog.debugClockOutV("pr");
        long nowTimeMsL= getTimeMsL();
        String outputString= ""
            + "\nReport-Number: " + (++reportNumberI)
              + " " + "-\\|/".substring(spinnerStateI, spinnerStateI+1)
            + "\nOperation: " + operationString
            + "\nDelta-Time: " + (nowTimeMsL - previousReportTimeMsL) 
            + "\nFile: " + testFile
            + "\nDisk-Block: " + volumeBlockNumberL
            + "\nFile-Byte: " + remainingFileBytesL
            + "\nVolume-Byte: " + remainingVolumeBytesL
            ;
        if (4 <= (++spinnerStateI)) spinnerStateI= 0;
        previousReportTimeMsL= nowTimeMsL;
        replaceDocumentTailAt1With2V(offsetOfProgressReportI, outputString);
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
        byte[] bytes= new byte[bytesPerBlockI];
        Arrays.fill(bytes, (byte)'x');
        theFileOutputStream.write(bytes);
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
