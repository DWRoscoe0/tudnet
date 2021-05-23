package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import static allClasses.AppLog.theAppLog;


import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class InstallerBuilder

  extends VolumeChecker

  /* This class is used to write a mass storage volume with
   * the files needed to create an Infogora installation.
   * 
   * Files that it will or might write:
   * * Infogora.exe
   * * ReadMe.txt
   * * Persistent.txt
   * * User Content
   */

  {

    // Locally stored injected dependencies.
    //// private String viewerClassString;
    //// @SuppressWarnings("unused") ////
    private Persistent thePersistent;
  
    public InstallerBuilder( // constructor
        String nameString, // Node name.
        Persistent thePersistent,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor
        )
      {
        //// super.initializeV(nameString);
        super( // constructor
          nameString, // Node name.
          thePersistent,
          theScheduledThreadPoolExecutor
          );
        
        this.thePersistent= thePersistent;
        }

    protected void mainThreadLogicV()
      // This overrides the superclass method. 
      {
        queueAndDisplayOutputSlowV(
          "This feature installs the app to storage volumes "
          + "attached to this device.");
        List<File> addedVolumeListOfFiles;
      theLoop: while(true) {
        addedVolumeListOfFiles= getTerminationOrKeyOrAddedVolumeListOfFiles();
        if (LockAndSignal.isInterruptedB()) break; // Process exit request.
        if (0 < addedVolumeListOfFiles.size()) { // If any volumes added then
          installToVolumeListV( // install to each with user's consent.
              addedVolumeListOfFiles);
          continue theLoop;
          }
        installToVolumeListV( // Install to each with user's consent.
            Arrays.asList(getVolumeFiles()));
      } // theLoop:
        return;
      }
    
    protected void installToVolumeListV(List<File> addedVolumeListOfFiles)
      /* This method installs to each volume in addedVolumeListOfFiles
       * after getting confirmation from the user.  */
      {
        queueAndDisplayOutputSlowV(
          "\n\nAvailable volume[s]: "
          + addedVolumeListOfFiles.toString()
          );
        for (File theFile : addedVolumeListOfFiles) {
          if (getConfirmationKeyPressB( // Install if user okays it.
              "Would you like to install to " + theFile + " ? ")
              )
            installToVolumeV(theFile);
          }
        }

    private void installToVolumeV(File volumeFile)
      /* This method installs to the volume specified by volumeFile.  */
      {
        theAppLog.debug("InstallerBuilder.installToVolumeV(.) begins.");
        String resultString;
        queueAndDisplayOutputSlowV("\n\nInstalling to " + volumeFile);
        buildFolderFile= new File(volumeFile,"InfogoraInstall");
      goReturn: {
      goFinish: {
        resultString= deleteAllVolumeFilesReturnString(volumeFile);
        if (! isAbsentB(resultString)) break goFinish;
        resultString= createFolderReturnString(buildFolderFile);
        if (! isAbsentB(resultString)) break goFinish;
        resultString= writeAppFileReturnString(buildFolderFile);
        if (! isAbsentB(resultString)) break goFinish;
        resultString= writeReadMeFileReturnString(buildFolderFile);
        if (! isAbsentB(resultString)) break goFinish;
        resultString= writeConfigurationFileReturnString(buildFolderFile);
        if (! isAbsentB(resultString)) break goFinish;
      }  // goFinish:
        if (! isAbsentB(resultString)) { // Report error.
          reportWithPromptSlowlyAndWaitForKeyV(
              "Abnormal termination:\n" + resultString);
          break goReturn;
          }
        reportWithPromptSlowlyAndWaitForKeyV(
          "The operation completed without error.");
      }  // goReturn:
        theAppLog.debug("InstallerBuilder.installToVolumeV(.) ends.");
        return;
      }

    private String deleteAllVolumeFilesReturnString(File volumeFile)
      /* This method erases File volumeFile,
       * meaning it deletes all non-hidden files on the volume,
       * if the user gives permission.
       */
      {
        String resultString= "Permission to delete was refused.";
      goReturn: {
        if (!getConfirmationKeyPressB(
            "This operation will first erase "+volumeFile
            + " !\nDo you really want to do this?") 
            )
          break goReturn;
        java.awt.Toolkit.getDefaultToolkit().beep(); // Get user's attention.
        if (!getConfirmationKeyPressB(
            "Are you certain that you want to ERASE "+volumeFile+" ! ?"))
          break goReturn;
        queueAndDisplayOutputSlowV("\nDeleting files...");
        resultString= FileOps.deleteRecursivelyReturnString(
            volumeFile,FileOps.requiredConfirmationString);
        queueAndDisplayOutputSlowV("done.");
        resultString= null; // Signal success.
      } // goReturn:
      return resultString;
      }

    private String createFolderReturnString(File buildFolderFile)
      // This method creates the installation folder.
      {
        queueAndDisplayOutputSlowV("\nCreating folder "+buildFolderFile);
        String resultString= FileOps.makeDirectoryAndAncestorsString(
            buildFolderFile);
        if (! isAbsentB(resultString))
          resultString= combineLinesString(
              "error creating folder", resultString);
        return resultString;
        }

    private String writeAppFileReturnString(File destinationFolderFile)
      /* This method writes the app file to the folder specified by 
       * destinationFolderFile which is assumed to exist already.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        queueAndDisplayOutputSlowV("\nWriting app file.");
        File sourceFile=
            FileOps.makeRelativeToAppFolderFile(Config.appString + ".exe");
        File destinationFile= 
            new File(destinationFolderFile, sourceFile.getName());
        boolean successB= FileOps.tryCopyFileB(sourceFile, destinationFile);
        if (!successB) {
          errorString= "Error copying file "+sourceFile+" to "+destinationFile;
          }
        return errorString;
        }

    private String writeReadMeFileReturnString(File destinationFolderFile)
      /* This method writes the app file to the folder specified by 
       * destinationFolderFile which is assumed to exist already.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        queueAndDisplayOutputSlowV("\nWriting ReadMe.txt file.");
        File destinationFile= 
            new File(destinationFolderFile, "ReadMe.txt");
        String sourceString= "To install, [double-]click on "
            + "the Infogora application file.";
        InputStream sourceInputStream= 
            new ByteArrayInputStream(sourceString.getBytes());
        boolean successB= FileOps.tryCopyingInputStreamToFileB(
            sourceInputStream, destinationFile);
        if (!successB) {
          errorString= "Error writing file "+destinationFile;
          }
        return errorString;
        }

    private String writeConfigurationFileReturnString(
        File destinationFolderFile)
      /* This method writes the configuration file to the folder specified by 
       * destinationFolderFile which is assumed to exist already.
       * It returns null if success, an error String if not.
       */
      {
        boolean successB= false;
        String errorString= null;
        queueAndDisplayOutputSlowV("\nWriting PersistentEpiNode.txt file.");
        File destinationFile= 
            new File(destinationFolderFile, "PersistentEpiNode.txt");
        try {
          PipedOutputStream thePipedOutputStream= new PipedOutputStream();
          PipedInputStream thePipedInputStream= 
            new PipedInputStream(thePipedOutputStream,1024);
          Future<Boolean> theFutureOfBoolean= 
            theScheduledThreadPoolExecutor.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                  return FileOps.tryCopyingInputStreamToFileB(
                      thePipedInputStream, destinationFile);
                  }});
          thePersistent.writeInstallationSubsetV(thePipedOutputStream);
          thePipedOutputStream.close(); // This will terminate above thread.
          try { successB = theFutureOfBoolean.get(); } catch (Exception e) { 
              successB= false; // Translate Exception to failure.
            }
          } catch (IOException e1) {
            /// TODO Auto-generated catch block
            e1.printStackTrace();
          }
        if (!successB) {
          errorString= "Error writing file "+destinationFile;
          }
        return errorString;
        }

    public String getSummaryString()
      {
        return "";
        }

    public JComponent getDataJComponent( 
        TreePath inTreePath, DataTreeModel inDataTreeModel 
        ) 
      /* Returns a JComponent of type whose name is //////////doc
       * which should be a viewer capable of displaying 
       * this DataNode and executing the command associated with it.
       * The DataNode to be viewed should be 
       * the last element of inTreePath,
       */
      {
          JComponent theJComponent;
          
          theJComponent= 
            new TitledTextViewer( inTreePath, inDataTreeModel,"Huh????????????");
          return theJComponent;
        }
    
    // variables.
    
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        
    // Constructors and constructor-related methods.

      public enum State
        {
          INITIAL_GREETING,
          AWAIT_DEVICE_INSERTION
          }

      State theState= State.INITIAL_GREETING;
      State nextState= null;
      
      @SuppressWarnings("unused") ////
      private void cycleStateMachineV()
        {
          while (true) {
            switch (theState) {
              case INITIAL_GREETING: initialGreetingV(); break;
              case AWAIT_DEVICE_INSERTION: awaitDeviceInsertionV(); break;
              }
            if (null == nextState) break;  // Exit loop and return.
            theState= nextState;
            nextState= null;
            }
        }
      
      private void initialGreetingV()
        {
          append(
              "\nTo begin building a volume with installation files,"+
              "\nplease insert the device to use into a USB port."+
              "\nIf you have already inserted one then please "+
              "\nremove it and insert it again.");
          nextState= State.AWAIT_DEVICE_INSERTION;
          }

      private void awaitDeviceInsertionV()
        {
          append(
              "\n\nThis is where we wait.");
          }

      private void append(String theString) {}

      @SuppressWarnings("unused") ////
      private void processKeyPressedV(KeyEvent theKeyEvent)
        //// Thread safety might be a problem.  
        //// Only insertString(.) is thread safe.
        {
          //// if(theKeyEvent.getKeyCode() == KeyEvent.VK_ENTER){
          theAppLog.debug( "InstallerBuilder.processKeyPressedV(.) called.");
          //// theKeyEvent.consume(); // Prevent further processing.
          //// ioIJTextArea.append("\nA key was pressed.\n");
          }

      @SuppressWarnings("unused") ////
      private void putCursorAtEndDocumentV()
        {
          ////
          }

    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper theTreeHelper;

      public TreeHelper getTreeHelper() { return theTreeHelper; }

    }
