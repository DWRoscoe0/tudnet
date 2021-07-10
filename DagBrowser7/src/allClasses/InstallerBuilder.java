package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class InstallerBuilder

  extends VolumeChecker

  /* This class is used to write a mass storage volume with
   * the files needed to create an Infogora installation.
   * 
   * Files that it will or might write:
   * * Infogora.exe
   * * ReadMe.txt
   * * Persistent.txt
   * * User Content (///enh comes later)
   * * Non-User Content (///enh comes later)
   */

  {

    // Locally stored injected dependencies.
    private Persistent thePersistent;
  
    public InstallerBuilder( // constructor
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
        /// resultString= deleteAllVolumeFilesReturnString(volumeFile);
        resultString= FileOps.deleteRecursivelyIfItExistsReturnString(
          buildFolderFile,FileOps.requiredConfirmationString);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        resultString= createFolderReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        resultString= writeAppFileReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        resultString= writeReadMeFileReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        resultString= writeConfigurationFileReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
      }  // goFinish:
        if (! EpiString.isAbsentB(resultString)) { // Report error.
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

    private String createFolderReturnString(File buildFolderFile)
      // This method creates the installation folder.
      {
        queueAndDisplayOutputSlowV("\nCreating folder "+buildFolderFile);
        String resultString= FileOps.makeDirectoryAndAncestorsString(
            buildFolderFile);
        if (! EpiString.isAbsentB(resultString))
          resultString= EpiString.combineLinesString(
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
        errorString= FileOps.tryCopyFileReturnString(
            sourceFile, destinationFile);
        if (! EpiString.isAbsentB(errorString))
          errorString= EpiString.combineLinesString(
            "Error copying file "+sourceFile+" to "+destinationFile,
            errorString
            );
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
      /* This method writes the installation configuration file 
       * to the folder specified by destinationFolderFile 
       * which is assumed to exist already.
       * It returns null if success, 
       * an error String describing the problem if not.
       */
      {
        theAppLog.debug(
          "InstallerBuilder.writeConfigurationFileReturnString(.) begin.");
        queueAndDisplayOutputSlowV("\nWriting PersistentEpiNode.txt file.");
        File destinationFile= 
            new File(destinationFolderFile, Config.persistentFileString);
        String errorString= FileOps.writeDataReturnString(
            (theOutputStream) -> {
              theAppLog.debug(
                "InstallerBuilder.writeConfigurationFileReturnString(.) "
                + "write to OutputStream begins.");
              theOutputStream.write( // Write leading comment.
                "#---YAML-like installation subset data follows---".getBytes());
              thePersistent.writeInstallationSubsetComponentsV(theOutputStream);
              theOutputStream.write( // Write trailing comment.
                (NL+"#--- end of installation subset data ---"+NL).getBytes());
              theAppLog.debug(
                "InstallerBuilder.writeConfigurationFileReturnString(.) "
                + "write to OutputStream ends.");
              }, // source WriterTo1Throws2<OutputStream,IOException>
            destinationFile // destination file File
            );
        theAppLog.debug(
          "InstallerBuilder.writeConfigurationFileReturnString(.) ends.");
        return errorString;
        }

    public String getSummaryString()
      {
        return "";
        }

    public JComponent getDataJComponent( 
        TreePath inTreePath, DataTreeModel inDataTreeModel 
        ) 
      /* Returns a JComponent which should be a viewer capable of displaying 
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

    }
