package allClasses;

import static allClasses.Globals.*;  // appLogger;

public class EpiThread

  extends Thread

  // This class adds some useful features to Thread.

  {

    public EpiThread( String nameString )  // Constructor.
      {
        super( nameString ); // Name here because setName() not reliable.
        }

    public void stopAndJoinV()  // Another thread uses to stop "this" thread.
      /* This method uses stopV() to request termination of "this" thread,
        and then joinV() to wait for that termination to complete.
        */
      {
        stopV(); // Requesting termination of EpiThread thread.
        joinV();  // Waiting until that termination completes.
        }

    public void stopV()  // Requests stop of "this" thread.
      /* Thread.currentThread() calls this method to request 
        termination of the "this" (EpiThread) Thread.
        It does this by calling this.interrupt().
        Later JoinV() should be called to wait for termination to complete.
        */
      {
        appLogger.info("EpiThread(" + getName() + ").stopV() begin.");

        interrupt(); // Requesting termination of EpiThread thread.
        }

    public void joinV()  // Waits for this thread to terminate.
      /* Thread.currentThread() calls this method to wait for the
        termination of the "this" EpiThread.
        this.stopV() should already have been called.
        It will not return until the "this" EpiThread terminates.
        It uses this.join() to wait until that termination completes.
        If Thread.currentThread().interrupt() is called by another Thread
        and interrupts the join(), then Thread.currentThread().interrupt()
        is called again to set the thread's interrupt status,
        and the method loops to do another join().
        This repeats until join() returns without being interrupted,
        indicating the "this" EpiThread has terminated.
        */
      {
        for  // Looping until EpiThread thread has terminated.
          ( boolean threadTerminatedB= false ; !threadTerminatedB ; )
          try { // Blocking and handling how blocking ends.
              join();  // Blocking.
              // Being here means EpiThread thread terminated.
              threadTerminatedB= true;  // Setting flag to terminate loop.
              }
            catch (InterruptedException e) {  // Handling interrupt of block.
              // Being here means the caller's thread was interrupted.
              Thread.currentThread().interrupt(); // Record interrupt status.
              }

        appLogger.info("EpiThread(" + getName() + ").joinV() end.");
        }
    }
