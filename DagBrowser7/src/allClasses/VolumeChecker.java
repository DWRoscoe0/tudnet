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

  extends VolumeDetector

  {
    // Locally stored injected dependencies (none).
    
    // variables.

      final int bytesPerBlockI= 512;
      final long bytesPerFileL= 1024 * 1024 * 1024; // 1 Gb
      final long periodMsL= 1000; // 1 second.

      // static variables.

      // instance variables.
  
        // Constructor-injected variables.

        // Other instance variables.
          private int reportNumberI;
          private long reportTimeMsL; // Next time to do report.
          private long previousReportTimeMsL;
          private File testFile;
          private int progressReportOffsetI; // Offset in thePlainDocument.
          private long diskBlockNumberL;
          private long fileBytesToWriteL;
          private long volumeBytesToWriteL;
          private int spinnerI;

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
      // This should be overridden by subclasses. 
      {
        queueAndDisplayOutputSlowV(
          "This feature does simple functional testing of storage volumes "
          + "attached to this device.");
        List<File> addedVolumeListOfFiles;
        while(true) {
          addedVolumeListOfFiles= getAddedVolumeListOfFiles();
          if (0 >= addedVolumeListOfFiles.size()) // If no volumes added
            break; // treat as exit request and exit.
          checkVolumeListV(addedVolumeListOfFiles);
          } 
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
        //// queueAndDisplayOutputSlowlyV("\nKey typed: "+keyString);
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
        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.
        queueAndDisplayOutputSlowV(
            "\nWriting done, deleting temporary files...");
        errorString= FileOps.deleteRecursivelyReturnString(
            testFolderFile,FileOps.requiredConfirmationString);
        if (null != errorString) {
          reportWithPromptSlowlyAndGetKeyString(errorString);
          break goReturn;
          }
        queueAndDisplayOutputSlowV(" Done.\n");
      }  // goReturn:
        return;
      }

    private String writeTestReturnString(File testFolderFile)
      /* This method does a write test by writing files in
       * the folder specified by testFolderFile.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        FileOutputStream theFileOutputStream= null;
        spinnerI= 0;
        //// volumeBytesToWriteL; ////// = 8*bytesPerBlockI; ////// test value.
        fileBytesToWriteL= bytesPerFileL;
        //// long byteI= 0; // Index of next byte to write.
        diskBlockNumberL= 0; // Next block to write.
        int fileI= 0; // Index of next file to write.
        progressReportOffsetI= thePlainDocument.getLength();
        reportTimeMsL= // Setting time to do first report... 
            System.currentTimeMillis(); //  to be immediately.
        previousReportTimeMsL= reportTimeMsL;
        reportNumberI= 0;
        updateProgressV(); // Initial progress report.
        try {
          fileLoop: while (true) {
            volumeBytesToWriteL= // Update remaining space to fill.
                testFolderFile.getUsableSpace();
            if (0 >= volumeBytesToWriteL) // Exit if no more bytes to write. 
              break fileLoop;
            testFile= new File(testFolderFile,""+fileI);
            theAppLog.debug(
              myToString()+"VolumeChecker.writeTestReturnString(.) "
              + "opening file= " + testFile);
            theFileOutputStream= new FileOutputStream(testFile);
            fileBytesToWriteL= Math.min(bytesPerFileL,volumeBytesToWriteL);
          blockLoop: while (true) {
            updateProgressMaybeV();
            if (0 >= fileBytesToWriteL) // Exit if no more bytes to write.
              break blockLoop;
            theAppLog.debugClockOutV("bb"); // Begin block write.
            writeBlockV(theFileOutputStream,diskBlockNumberL);
            theAppLog.debugClockOutV("be"); // End block write.
            diskBlockNumberL++;
            volumeBytesToWriteL-= bytesPerBlockI;
            fileBytesToWriteL-= bytesPerBlockI;
          } // blockLoop:
          theAppLog.debug(
            myToString()+"VolumeChecker.writeTestReturnString(.) "
            + "closing file= " + testFile);
          theFileOutputStream.close();
          queueAndDisplayOutputFastV("f");
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
        updateProgressV(); // Final progress report.
        testFolderFile.delete();
        return errorString;
        }

    private void updateProgressMaybeV()
      {
        //// System.out.print("-");
        long nowTimeMsL= System.currentTimeMillis();
        /*  /// 
        long excessTimeMsL= nowTimeMsL-reportTimeMsL;
        if (excessTimeMsL >= 0)
        */  ///
        long remainingTimeMsL= nowTimeMsL-reportTimeMsL;
          //// theLockAndSignal.realTimeWaitDelayMsL(
          ////     reportTimeMsL, periodMsL);
        if // Update progress report if time remaining in period reached 0.
          //// (0 == remainingTimeMsL)
          (0 <= remainingTimeMsL)
          {
            updateProgressV();
            //// long deltaTimeMsL= theLockAndSignal.periodCorrectedDelayMsL(
            long deltaTimeMsL= theLockAndSignal.periodCorrectedShiftMsL(
              reportTimeMsL, periodMsL);
            /// long deltaTimeMsL= periodMsL;
            //// long newTimeMsL= reportTimeMsL + deltaTimeMsL + periodMsL;
            long newTimeMsL= nowTimeMsL + deltaTimeMsL + periodMsL;
            theAppLog.debug("VolumeChecker.updateProgressMaybeV() times:"
                /* ///
                +" now="+nowTimeMsL
                +" excess="+excessTimeMsL
                */  ///
                +" old="+reportTimeMsL
                +", delta="+deltaTimeMsL
                +", new="+newTimeMsL
                );
            reportTimeMsL= newTimeMsL;
            }
        }

    private void updateProgressV()
      {
        // theAppLog.debug(
        //   "VolumeChecker.updateProgressV() updating.");
        theAppLog.debugClockOutV("pr");
        long nowTimeMsL= System.currentTimeMillis();
        String outputString= ""
            + "\nReport-Number: " + reportNumberI
              + " " + "-\\|/".substring(spinnerI, spinnerI+1)
            + "\nDelta-Time: " + (nowTimeMsL - previousReportTimeMsL) 
            + "\nFile: " + testFile
            + "\nDisk-Block: " + diskBlockNumberL
            + "\nFile-Byte: " + fileBytesToWriteL
            + "\nVolume-Byte: " + volumeBytesToWriteL
            ;
        reportNumberI++;
        if (4 <= (++spinnerI)) spinnerI= 0;
        previousReportTimeMsL= nowTimeMsL;
        replaceDocumentTailAt1With2V(progressReportOffsetI, outputString);
        /// EpiThread.interruptibleSleepB(1); //////
        }

    private void writeBlockV(FileOutputStream theFileOutputStream,long blockL) 
        throws IOException
      /* This method writes a block containing the block number blockI.
       * A block is bytesPerBlockI bytes.
       * It throws IOException if there is an error.
       * 
       * ///opt For now use NonDirect ByteBuffer.  Use Direct later for speed.
       */
      {
        ////// String blockString= 
        byte[] bytes= new byte[bytesPerBlockI];
        Arrays.fill(bytes, (byte)'x');
        //// ByteBuffer theByteBuffer= ByteBuffer.wrap(bytes); 
        //// for (int i=0 ; i<bytesPerBlockI; i++)
          //// theFileOutputStream.write("x".getBytes());
        theFileOutputStream.write(bytes);
        //// queueAndDisplayOutputFastV("b ");
        }

    protected List<File> getAddedVolumeListOfFiles()
      /* This method returns an array of added volumes
       * the next time one or more is added to the set of attached volumes,
       * or an empty array if thread termination is requested.
       */
      {
        ArrayList<File> addedVolumeListOfFiles= new ArrayList<File>();
        File[] oldVolumeFiles= getVolumeFiles();
        while (true) {
          File[] newVolumeFiles=
              waitForTerminationOrChangeOfVolumeFiles(oldVolumeFiles);
          if (null == newVolumeFiles) break; // Exit if termination requested.
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
