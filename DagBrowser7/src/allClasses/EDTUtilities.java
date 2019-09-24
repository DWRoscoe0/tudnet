package allClasses;


import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import static allClasses.AppLog.theAppLog;


public class EDTUtilities {

  /* EDT Utility code.

    Because Swing is not thread-safe, all code which uses Swing
    must run on the EDT (Event Dispatch Thread)
    
    The methods in this class provide the means to execute on the EDT.
   
   */
    
    protected static void runOrInvokeAndWaitV( Runnable jobRunnable )
      /* This helper method runs jobRunnable on the EDT thread.
        It already running on the EDT thread then it just calls run().
        Otherwise it uses invokeAndWait(..) to switch threads.
       */
	    {
	      if ( SwingUtilities.isEventDispatchThread() )
	        jobRunnable.run();
	      else
	        invokeAndWaitV( jobRunnable );
	    	}

    protected static void invokeAndWaitV( Runnable theRunnable )
      /* This method calls SwingUtilities.invokeAndWait(..) 
        to execute theRunnable.run() on the Event Dispatch Thread (EDT).
        It also handles any exceptions invokeAndWait(..) might throw.

        If, either before or during execution of this method, 
        the current thread is interrupted, which results in 
        an InterruptedException from invokeAndWait(..), 
        then this method will clear the interrupt and 
        call invokeAndWait(..) again with a do-nothing Runnable.
        If an InterruptedException happens again it will do this again.
        It will continue to do this until invokeAndWait(..) returns normally.
        The purpose of doing this is to guarantees that 
        when this method finally returns,
        the processing of jobRunnable will be complete.
        This works because Runnables are executed and completed in order.
        
        This thread's interrupt status will be true 
        on return from this method if it was true on entry, 
        or it became true during processing.
        It will be false on return otherwise.
        
        If an unhandled exception occurs in theRunnable,
        this method will throw a RuntimeException which wraps an
        InvocationTargetException which wraps the
        unhandled exception which occured in theRunnable.
        ///fix This might not work if the exception happens after
        an InterruptedException occurs.
        */
      {
    		//appLogger.info( "EDTUtilities.invokeAndWaitV(..) begins.");
    	  boolean interruptedB= // Saving and disabling interrupted status. 
    	  		Thread.interrupted(); // (in case it was already true)
    	  while (true) { // Keep trying until an invokeAndWait(..) finishes.
				  try  // Queuing and waiting for theRunnable on EDT thread.
			  	  { 
	            //appLogger.info( "EDTUtilities.invokeAndWaitV(..) before invokeAndWait(jobRunnable).");
	            SwingUtilities.invokeAndWait( theRunnable );            
	            //appLogger.info( "EDTUtilities.invokeAndWaitV(..) after invokeAndWait(jobRunnable).");
				  		break;  // Exiting because invokeAndWait(..) ended normally.
			  	  	}
			    catch // Wait was interrupted.
			      (InterruptedException theInterruptedException)
			      { 
			        interruptedB= true; // Record interrupt for restoring later.
              theRunnable= // Replace theRunnable with a null one for retry.
                new Runnable() {
                  @Override
                  public void run() { 
                    theAppLog.info( "EDTUtilities.invokeAndWaitV(..) null run()");
                    } // Doing nothing. 
                  };  
			      	}
			  	catch  // Unhandled exception occurred on Event Dispatch Thread.
			  	  (InvocationTargetException theInvocationTargetException)
			  	  {
				  	  Globals.logAndRethrowAsRuntimeExceptionV( 
			  	      "EDTUtilities.invokeAndWaitV(..) exception", 
			  	      theInvocationTargetException);
				  	  }
      	  theAppLog.info( "EDTUtilities.invokeAndWaitV(..) looping.");
    	  	}
    	  if (interruptedB) // Setting interrupted status if interrupt occurred. 
    	  	Thread.currentThread().interrupt(); 
      	}

    protected static boolean testAndLogIfNotRunningEDTB()
      /* This method returns whether we are not running the EDT thread.
        Returns true if not on EDT thread, false otherwise.
        If not on EDT thread it logs an error.
       */
	    {
	      boolean isNotEDTB= ! SwingUtilities.isEventDispatchThread();
	      if ( isNotEDTB ) 
	      	theAppLog.error(" testAndLogIfNotRunningEDTB() true");
	      return isNotEDTB;
	    	}

    protected static boolean testAndLogIfRunningEDTB()
      /* This method returns whether we are running the EDT thread.
        Returns true if on EDT thread, false otherwise.
        If on EDT thread it logs an error.
       */
	    {
	      boolean isEDTB= SwingUtilities.isEventDispatchThread();
	      if ( isEDTB ) 
	      	theAppLog.error(" testAndLogIfRunningEDTB() true");
	      return isEDTB;
	    	}

}
