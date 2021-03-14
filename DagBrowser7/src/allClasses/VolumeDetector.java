package allClasses;

import static allClasses.AppLog.theAppLog;

import java.util.concurrent.ScheduledThreadPoolExecutor;


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
        while(true) {
          queueSlowOutputV(
            "\nPlease insert the volume to be checked into a USB port.  "+
            "If you have already inserted one then please "+
            "remove it and insert it again.  ");
          String inString= promptAndGetKeyString();
          if (null == inString) // Exit if termination requested.
            break;
          queueSlowOutputV("\nThe character '"+inString+"' was typed.\n");
          } 
        }

    private void append(String theString) {}

    }
