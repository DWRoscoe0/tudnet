package allClasses;

import static allClasses.AppLog.theAppLog;

import java.util.concurrent.ScheduledThreadPoolExecutor;


public class VolumeChecker

  extends ConsoleBase

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
      
      private void initialGreetingV()
        {
          append(
              "\nPlease insert the volume to be checked into a USB port."+
              "\nIf you have already inserted one then please "+
              "\nremove it and insert it again.");
          }

      private void awaitDeviceInsertionV()
        {
          append(
              "\n\nThis is where we wait.");
          }

      private void append(String theString) {}

    }
