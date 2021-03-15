package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/// import allClasses.LockAndSignal.Input;


public class VolumeChecker

  extends VolumeDetector

  {
    // Locally stored injected dependencies (none).
    
    // variables.
    
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
        queueAndDisplayOutputSlowlyV(
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
      queueAndDisplayOutputSlowlyV(
        "\n\nAdded volume[s]: "
        + addedVolumeListOfFiles.toString()
        );
      for (File theFile : addedVolumeListOfFiles) {
        String keyString= promptSlowlyAndGetKeyString(
          "\n  Would you like to check volume "
          + theFile 
          + "?"
          );
        queueAndDisplayOutputSlowlyV("\nKey typed: "+keyString);
        }
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
