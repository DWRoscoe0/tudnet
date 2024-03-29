package allClasses;

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
   * the files needed to create an app installation.
   * 
   * Files that it will or might write:
   * * TUDNet.exe
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

    @Override
    protected void mainThreadLogicV()
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
        buildFolderFile= new File(volumeFile,Config.appString+"Install");
      goReturn: {
      goFinish: {
        /// resultString= deleteAllVolumeFilesReturnString(volumeFile);
        theAppLog.debug("InstallerBuilder.installToVolumeV(.) "
            + "calling deleteRecursivelyIfItExistsReturnString().");
        resultString= FileOps.deleteRecursivelyIfItExistsReturnString(
          buildFolderFile,FileOps.requiredConfirmationString);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        theAppLog.debug("InstallerBuilder.installToVolumeV(.) "
            + "calling createFolderReturnString().");
        resultString= createFolderReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        theAppLog.debug("InstallerBuilder.installToVolumeV(.) "
            + "calling writeAppFileReturnString().");
        resultString= writeAppFileReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        theAppLog.debug("InstallerBuilder.installToVolumeV(.) "
            + "calling writeReadMeFileReturnString().");
        resultString= writeReadMeFileReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
        resultString= writeConfigurationFileReturnString(buildFolderFile);
        if (! EpiString.isAbsentB(resultString)) break goFinish;
      }  // goFinish:
        java.awt.Toolkit.getDefaultToolkit().beep(); // Get user's attention.
        if (! EpiString.isAbsentB(resultString)) { // Report error.
          if (FileOps.containsNoSpaceMessageInB(resultString))
            resultString+= // Append suggestion if out of space. 
              "\n\nTry deleting some unneeded files, "
              + "or switch to a disk-volume with more space, "
              + "and try again.";
          appendWithPromptSlowlyAndWaitForKeyV(
              "\n\nAbnormal termination:\n" + resultString);
          break goReturn;
          }
        appendWithPromptSlowlyAndWaitForKeyV(
          "\n\nThe operation completed without error.");
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
          resultString= EpiString.combine1And2WithNewlineString(
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
        progressReportBeginV(
            FileOps.fileOpsProgressReportSupplierOfString);
        progressReportUpdateV();
        errorString= FileOps.tryCopyFileReturnString(
            sourceFile, destinationFile);
        /// progressReportUndoV();
        progressReportEndV();
        if (! EpiString.isAbsentB(errorString))
          errorString= EpiString.combine1And2WithNewlineString(
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
        String sourceString= "To install, [double-]click on the "
            + Config.appString + " application file.";
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

    }
