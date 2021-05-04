package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import static allClasses.AppLog.theAppLog;


import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class InstallerBuilder

  //// extends NamedDataNode
  //// extends VolumeDetector
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
    @SuppressWarnings("unused") ////
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
      /* This method checks the volume specified by volumeFile.
       */
      {
        theAppLog.debug("VolumeChecker.checkVolumeV(.) begins.");
        String resultString;
        queueAndDisplayOutputSlowV("\n\nInstalling to " + volumeFile + "\n");
        buildFolderFile= new File(volumeFile,"InfogoraTemp");
      goReturn: {
      goFinish: {
        resultString= FileOps.makeDirectoryAndAncestorsString(
            buildFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error creating folder", resultString);
          break goFinish;
          }
        resultString= writeFilesReturnString(buildFolderFile);
        if (! isAbsentB(resultString)) {
          resultString= combineLinesString(
              "error during writing of files", resultString);
          break goFinish;
          }
      }  // goFinish:
        if (! isAbsentB(resultString)) { // Report error.
          reportWithPromptSlowlyAndWaitForKeyV(
              "Abnormal termination:\n" + resultString);
          break goReturn;
          }
        reportWithPromptSlowlyAndWaitForKeyV(
          "The operation completed without error.");
      }  // goReturn:
        theAppLog.debug("VolumeChecker.checkVolumeV(.) ends.");
        return;
      }

    private String writeFilesReturnString(File destinationFolderFile)
      /* This method creates an installation by 
       * writing all the installation files to the folder specified by 
       * destinationFolderFile which is assumed to exist already.
       * It returns null if success, an error String if not.
       */
      {
        String errorString= null;
        File sourceFile= 
            //// AppInstanceManager.standardAppFile;
            //// FileOps.makeRelativeToAppFolderFile( 
            ////   AppSettings.exeInitiatorFile
            ////   );
            FileOps.makeRelativeToAppFolderFile(Config.appString + ".exe");
        File destinationFile= 
            new File(destinationFolderFile, sourceFile.getName());
        boolean successB= FileOps.tryCopyFileB(sourceFile, destinationFile);
        if (!successB) {
          errorString= "Error copying file "+sourceFile+" to "+destinationFile;
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
        //// private Persistent thePersistent;
        
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
