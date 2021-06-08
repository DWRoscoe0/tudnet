package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.AppLog.LogLevel.INFO;
import static allClasses.AppLog.LogLevel.WARNING;
import static allClasses.SystemSettings.NL;

import java.util.Set;

public class BackupTerminator extends Thread

  /*  
    ///ano This class was created to deal with an anomaly.
    The anomaly was that the app was not always terminating when it should.

    According to the documentation of the Thread class:

      When a Java Virtual Machine starts up, 
      there is usually a single non-daemon thread (which typically calls 
      the method named main of some designated class). 
      The Java Virtual Machine continues to execute threads until 
      either of the following occurs:
      * The exit method of class Runtime has been called and 
        the security manager has permitted the exit operation to take place.
      * All threads that are not daemon threads have died, 
        either by returning from the call to the run method or 
        by throwing an exception that propagates beyond the run method. 

    Even though care was taken to satisfy all the requirements for termination,
    including disposing of all GUI windows, terminating all non-daemon threads,
    including the last one by terminating the main(.) method,
    sometimes the app would not terminate.

    This class was created to deal with this anomaly.
    If used correctly, it will detect failure to terminate.  
    In that case it will:
    * log any remaining active threads, and
    * force app termination by calling System.exit(.).

    When this class was tested by intentionally inserting
    an infinite loop at the end of main(.), 
    the still-active logged threads were:
      WAITING       10  Daemon  Java2D Disposer            
      WAITING       10  Daemon  Reference Handler          
      RUNNABLE       5  Daemon  Thread-2                   
      RUNNABLE       5  Daemon  Attach Listener            
      BLOCKED        5  Daemon  AppLog                     
      WAITING        5  Daemon  Thread-1                   
      RUNNABLE       5  Normal  main                       
      WAITING        5  Daemon  TimerQueue                 
      RUNNABLE       5  Daemon  BackupTerminator           
      RUNNABLE       9  Daemon  Signal Dispatcher          
      TIMED_WAITING  5  Daemon  TracingEventQueueMonitor   
      WAITING        8  Daemon  Finalizer                  
      RUNNABLE       6  Daemon  AWT-Windows                
      WAITING       10  Daemon  Prism Font Disposer        
      WAITING        5  Daemon  AppTimer
    Note that in the above list, the only non-Daemon thread was "main", 
    as expected.
      RUNNABLE       5  Normal  main                       

    When termination failures happened, and termination needed to be forced,
    the logged still-active threads included the following non-daemon threads:
      TIMED_WAITING  5  Normal  AWT-Shutdown               
      RUNNABLE       5  Normal  DestroyJavaVM              
      WAITING        6  Normal  AWT-EventQueue-1           
    No reason for these threads to continue to run could be found.

    */

  {

    boolean terminationUnderwayB=false; // This flag is used 
      // to deal with spurious wake ups.

    public static BackupTerminator makeBackupTerminator()
      // This method makes and returns a ready and running BackupTerminator.
      {
        BackupTerminator theBackupTerminator= new BackupTerminator();
        theBackupTerminator.setName("BackupTerminator");
        theBackupTerminator.setDaemon(true); // Don't be the problem.
        theBackupTerminator.start(); // Start its thread.
        return theBackupTerminator;
        }

    public synchronized void run() // Our thread logic.
      {
        while (true) { // Wait for signal that termination is underway.
          if (terminationUnderwayB) break; // Done waiting.
          waitV( // Wait for wake-up caused by notification from main(.).  
              0 // 0 means no time-out, wait as long as needed.
              );
          } // while(true)

        int secondsI= 5;
        theAppLog.logB( INFO, true, null,
            "run() Starting "+secondsI+"-second backup exit timer");
        while (secondsI-- > 0) { // Count down time while displaying dots.
          waitV(1000); // Waiting 1 second.
          System.out.print("."); // Display one dot per second.
          }

        // If control reaches this point then 
        // time has expired and normal termination probably failed.
        // Termination will now be forced.

        synchronized(theAppLog) { // Log the following together.
          theAppLog.logB( WARNING, true, null,
              "run() ======== FORCING LATE APP TERMINATION ========");
          theAppLog.doStackTraceV(null);
          logThreadsV(); // Include all threads that are still active.
          theAppLog.debug(
              "run() closing log file and executing System.exit(1)." );
          theAppLog.closeFileIfOpenB(); // Close log before exit.
          }
        System.exit(1); // Force app termination with an error code of 1.
        }

    private void waitV(long msL)
      /* This helper method waits and handles any InterruptedException.
        The wait ends after msL milliseconds, or when a notification happens,
        or an interrupt happens, which ever happens first.
        An interrupt shouldn't happen, but if it does, it is logged.
        */
      {
        try {
          wait(msL);
          }
        catch (InterruptedException e) {
          theAppLog.debug("BackupTerminator.waitV() wait(..), interrupted."); 
          }
      
      }

    public synchronized void setTerminationUnderwayV() 
      /* This method signals the thread that termination should
        be imminent.  See run() for what happens next.
        */
    {
      terminationUnderwayB= true;
      notify(); // Unblock the thread.
      }


    public static void logThreadsV()
      /* Logs active threads.             
        This method was based on code from a web article.
        */
      {
        synchronized (theAppLog) { // Output thread list as single log entry.
          theAppLog.info("Infogora.logThreadsV(), remaining active threads:"); 
          Set<Thread> threadSet= Thread.getAllStackTraces().keySet();
          for (Thread t : threadSet) {
              Thread.State threadState= t.getState();
              int priorityI= t.getPriority();
              String typeString= t.isDaemon() ? "Daemon" : "Normal";
              String nameString= t.getName();
              theAppLog.getPrintWriter().printf(
                  NL+ "    %-13s %2d  %s  %-25s  ", 
                  threadState, priorityI, typeString, nameString);
              }
          }
    
      }

    }
