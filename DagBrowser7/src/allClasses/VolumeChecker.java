package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/// import allClasses.LockAndSignal.Input;


public class VolumeChecker

  extends VolumeDetector

  {
    // Locally stored injected dependencies (none).
    
    // variables.

      final int bytesPerBlockI= 512;
      final long bytesPerFileL= 1024 * 1024; /// 2 * bytesPerBlockI;
      
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        
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
          + "[y/n]?"
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
        queueAndDisplayOutputSlowV("\n\nChecking " + volumeFile);
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
        errorString= FileOps.deleteRecursivelyReturnString(
            testFolderFile,FileOps.requiredConfirmationString);
        if (null != errorString) {
          reportWithPromptSlowlyAndGetKeyString(errorString);
          }
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
        long volumeBytesToWriteL; ////// = 8*bytesPerBlockI; ////// test value.
        long fileBytesToWriteL= bytesPerFileL;
        //// long byteI= 0; // Index of next byte to write.
        long blockL= 0; // Index of next block to write.
        int fileI= 0; // Index of next file to write.
        try {
          fileLoop: while (true) {
            volumeBytesToWriteL= // Update remaining space to fill.
                testFolderFile.getUsableSpace();
            if (0 >= volumeBytesToWriteL) // Exit if no more bytes to write. 
              break fileLoop;
            File testFile= new File(testFolderFile,""+fileI);
            theFileOutputStream= new FileOutputStream(testFile);
            fileBytesToWriteL= Math.min(bytesPerFileL,volumeBytesToWriteL);
          blockLoop: while (true) {
            if (0 >= fileBytesToWriteL) // Exit if no more bytes to write.
              break blockLoop;
            writeBlockV(theFileOutputStream,blockL);
            blockL++;
            volumeBytesToWriteL-= bytesPerBlockI;
            fileBytesToWriteL-= bytesPerBlockI;
          } // blockLoop:
          theFileOutputStream.close();
          queueAndDisplayOutputFastV("f   ");
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
        testFolderFile.delete();
        return errorString;
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
        for (int i=0 ; i<bytesPerBlockI; i++)
          theFileOutputStream.write("x".getBytes());
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
