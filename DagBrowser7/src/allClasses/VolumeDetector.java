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
        queueAndDisplayOutputSlowV(
          "This feature tests the detection of storage volumes "
          + "attached to this device.\n");
        File[] oldVolumeFiles= getVolumeFiles(); 
        while(true) {
          File[] newVolumeFiles= 
              getTerminationOrKeyOrChangeOfVolumeFiles(oldVolumeFiles);
          if (Input.INTERRUPTION == theLockAndSignal.testingForInterruptE())
            break;
          if (null != testGetFromQueueKeyString()) { // If key pressed
            flushKeysV();
            //// java.awt.Toolkit.getDefaultToolkit().beep(); // create audible Beep.
            continue;
            }
          queueOutputV("\n\nVolumes changed.");
          oldVolumeFiles= newVolumeFiles;
          } 
        }

    protected File[] getTerminationOrKeyOrChangeOfVolumeFiles(
        File[] oldVolumeFiles)
      /* This method waits for one of several inputs and then returns.
       * The inputs which terminate the wait are:
       * * termination request: returns null, can be tested with
       *   LockAndSignal.testingForInterruptE().
       * * key pressed: returns null, can be tested with
       *   testGetFromQueueKeyString() or related methods. 
       * * attached volumes changed: returns the new list of volumes,
       *   can be tested with null!=return.
       */
      {
        File[] newVolumeFiles;
        queueAndDisplayOutputSlowV(
          "\n\nPresent volumes attached are:\n  "
          + Arrays.toString(oldVolumeFiles)
          + "\n\nPlease insert or connect "
          + "the next USB or other storage volume "
          + "to be used in this operation.  "
          + "If you have already done this then please "
          + "remove or disconnect it, "
          + "then insert or connect it again.  ");
        Input theInput= Input.TIME; // Set to do polling test first.
        theLoop: while (true) {
          newVolumeFiles= null;
          switch (theInput) {
            case INTERRUPTION: break theLoop; // Exit if termination requested.
            case NOTIFICATION: { // Exit if key pressed.
              if (null != testGetFromQueueKeyString()) break theLoop;
              break;
              }
            case TIME: { // It's time to test whether volume list has changed.
              newVolumeFiles= getVolumeFiles(); // Read attached volumes.
              if (! Arrays.equals(oldVolumeFiles,newVolumeFiles)) // Compare 
                break theLoop; // and exit if any volumes have changed.
              break;
              }
            default: break;
            }
          /*  ////
          if // Exit loop with null if termination is requested.
            (Input.INTERRUPTION == theLockAndSignal.testingForInterruptE())
            break; // Exit loop;
          if (null != testGetFromQueueKeyString()) // If a key was pressed
            break; // exit loop.
          newVolumeFiles= getVolumeFiles();
          if // If volume list has changed
            (! Arrays.equals(oldVolumeFiles,newVolumeFiles)) 
            break; // exit loop.
          */  ////
          theInput= 
              theLockAndSignal.waitingForInterruptOrDelayOrNotificationE(20);
          }
        return newVolumeFiles;
        }

    protected File[] getVolumeFiles()
      /* This method returns an array of Files describing 
       * storage volumes available at this time. 
       */
      {
        File[] resultFiles= File.listRoots();
        return resultFiles;
        }

    }
