package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import allClasses.LockAndSignal.Input;


public class VolumeDetector

  extends ConsoleBase

  {
    // Locally stored injected dependencies (none).
    
    // variables.
    
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        
    // Constructors and constructor-related methods.
  
    public VolumeDetector( // constructor
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
          myToString()+"VolumeDetector.VolumeChecker(.) ends, nameString='"+nameString+"'");
        }

    protected void mainThreadLogicV()
      // This should be overridden by subclasses. 
      {
        queueAndDisplayOutputSlowlyV(
          "This feature tests the detection of storage volumes "
          + "attached to this device.\n");
        File[] newVolumeFiles;
        File[] oldVolumeFiles= getVolumeFiles(); 
        while(true) {
          if (Input.INTERRUPTION == theLockAndSignal.testingForInterruptE())
            break;
          queueAndDisplayOutputSlowlyV(
            "\nPresent volumes available are:\n  "
            + Arrays.toString(oldVolumeFiles)
            + "\n\nPlease insert or connect "
            + "the next USB or other storage volume "
            + "to be used in this operation.  "
            + "If you have already done this then please "
            + "remove or disconnect it, "
            + "then insert or connect it again.  ");
          while (true) {
            newVolumeFiles= getVolumeFiles();
            if // If volume list has changed
              (! Arrays.equals(oldVolumeFiles,newVolumeFiles)) 
              break; // exit loop.
            EpiThread.interruptibleSleepB(20);
            if (Input.INTERRUPTION == theLockAndSignal.testingForInterruptE())
              break;
            }
          java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.
          queueSlowOutputV("\n\nVolumes changed.");
          oldVolumeFiles= newVolumeFiles; 
          } 
        }

        private File[] getVolumeFiles()
          {
            File[] resultFiles= File.listRoots();
            return resultFiles;
            }

    }
