package allClasses;

import static allClasses.Globals.appLogger;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

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

    protected static void invokeAndWaitV( Runnable jobRunnable )
      /* This method calls SwingUtilities.invokeAndWait(..) 
        to execute jobRunnable.run().
        It also handles any exceptions invokeAndWait(..) might throw.
        
        If, either before or during execution of this method, 
        its thread is interrupted, which results in 
        an InterruptedException from SwingUtilities.invokeAndWait(..), 
        then this method will clear the interrupt and 
        call invokeAndWait(..) again with a do-nothing Runnable.
        If an InterruptedException happens again it will do this again.
        It will continue to do this until invokeAndWait(..) ends 
        without being interrupted.
        The purpose of doing this is to guarantees that 
        when this method finally returns,
        the processing of jobRunnable will be complete.
        This works because Runnables are executed and completed in order.
        For this to work the EDT must be running normally.

        This thread's interrupt status will be true 
        on return from this method if it was true on entry, 
        or it became true during processing.
        It will be false on return otherwise.
        
        An earlier version of this method did nothing if 
        shutdown was underway, 
        because using the EDT (Event Dispatch Thread) 
        was thought to be safe if shutdown is underway.  

        A hanging problem should now be fixed when
          interruptedB= true; // Record interrupt for restoring later.
        was added to the first catch clause.
        
        ///opt? Rewrite to make a single loop do both
        the jobRunnable and any possible null Runnables 
        if InterruptedException happens.

        */
      {
    		//appLogger.info( "EDTUtilities.invokeAndWaitV(..) begins.");

    	  boolean interruptedB= // Saving and disabling interrupted status. 
    	  		Thread.interrupted(); // (in case it was already true)

    	  try  // Processing jobRunnable on EDT thread.
		  	  { 
    	  	  //appLogger.info( "EDTUtilities.invokeAndWaitV(..) before invokeAndWait(jobRunnable).");
    	  	  SwingUtilities.invokeAndWait( jobRunnable ); 			  		
			  		//appLogger.info( "EDTUtilities.invokeAndWaitV(..) after invokeAndWait(jobRunnable).");
    	  	  }
		    catch // Handling wait interrupt.  Avoid.  Sometimes hangs?! 
		    	(InterruptedException e) 
		      { // Flush: Executing null Runnable to guarantee jobRunnable done.
		        interruptedB= true; // Record interrupt for restoring later.
	      	  appLogger.info( "EDTUtilities.invokeAndWaitV(..) jobRunnable interrupted.");
	      	  while (true) {  
  	      	  appLogger.info( "EDTUtilities.invokeAndWaitV(..)"
  	      	      + " begin Runnable completion loop.");
						  try  // Queuing and waiting for null Runnable on EDT thread.
					  	  { 
						  		appLogger.info( "EDTUtilities.invokeAndWaitV(..) before invokeAndWait(null Runnable).");
						  		SwingUtilities.invokeAndWait( new Runnable() { 
			              @Override  
			              public void run() { 
							    	  appLogger.info( "EDTUtilities.invokeAndWaitV(..) null run()");
			              	} // Doing nothing. 
						  	    } );  
						  		appLogger.info( "EDTUtilities.invokeAndWaitV(..) after invokeAndWait(null Runnable).");
						  		break;  // Exiting because wait ended normally.
					  	  	}
					    catch // Handling wait interrupt.
					    	(InterruptedException e1) 
					      { 
					        interruptedB= true; // Record interrupt for restoring later.
					    	  appLogger.info( "EDTUtilities.invokeAndWaitV(..) null run() interrupted.");
					      	}
					  	catch  // Handling invocation exception by re-throwing.
					  	  (InvocationTargetException e1) 
					  	  { 
					  			Globals.logAndRethrowAsRuntimeExceptionV( 
					  					"EDTUtilities.invokeAndWaitV(..)", e1
					  					);
					  			} // wrapping and re-throwing.
  	      	  appLogger.info( "EDTUtilities.invokeAndWaitV(..) end loop.");
	      	  	}
		      	}
		  	catch  // Handling invocation exception by re-throwing.
		  	  (InvocationTargetException e) 
		  	  { 
			  		appLogger.error( "EDTUtilities.invokeAndWaitV(..):"+e );
		    	  throw new RuntimeException(e); 
		  			} // wrapping and re-throwing.
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
	      	appLogger.error(" testAndLogIfNotRunningEDTB() true");
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
	      	appLogger.error(" testAndLogIfRunningEDTB() true");
	      return isEDTB;
	    	}

}
