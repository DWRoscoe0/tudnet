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
        For this to work the EDT must be running normally.

        This thread's interrupt status will be true 
        on return from this method if it was true on entry, 
        or it became true during processing.
        It will be false otherwise.
        
        An earlier version of this method did nothing if 
        shutdown was underway, 
        because using the EDT (Event Dispatch Thread) 
        was thought to be safe if shutdown is underway.  
        
        //// ?? Rewrite to make a single loop do both
        the jobRunnable and any possible null Runnables 
        if InterruptedException happens.

        //// ?? Or rewrite to reflect that thread interrupt probably means
        abort and jobRunnable need not or can not be completed.
        */
      {
    		//appLogger.debug( "DataTreeModel.invokeAndWaitV(..) begins.");

    	  boolean interruptedB= // Saving and disabling interrupted status. 
    	  		Thread.interrupted(); // (in case it was already true)

    	  try  // Processing jobRunnable on EDT thread.
		  	  { 
    	  	  //appLogger.debug( "DataTreeModel.invokeAndWaitV(..) before invokeAndWait(jobRunnable).");
    	  	  SwingUtilities.invokeAndWait( jobRunnable ); 			  		
			  		//appLogger.debug( "DataTreeModel.invokeAndWaitV(..) after invokeAndWait(jobRunnable).");
    	  	  }
		    catch // Handling wait interrupt.  Avoid.  Sometimes hangs?! 
		    	(InterruptedException e) 
		      { // Flush: Executing null Runnable to guarantee jobRunnable done.
	      	  appLogger.debug( "DataTreeModel.invokeAndWaitV(..) jobRunnable interrupted.");
	      	  while (true) {  
  	      	  appLogger.debug( "DataTreeModel.invokeAndWaitV(..) begin loop.");
						  try  // Queuing and waiting for null Runnable on EDT thread.
					  	  { 
						  		appLogger.debug( "DataTreeModel.invokeAndWaitV(..) before invokeAndWait(null Runnable).");
						  		SwingUtilities.invokeAndWait( new Runnable() { 
			              @Override  
			              public void run() { 
							    	  appLogger.debug( "DataTreeModel.invokeAndWaitV(..) null run()");
			              	} // Doing nothing. 
						  	    } );  
						  		appLogger.debug( "DataTreeModel.invokeAndWaitV(..) after invokeAndWait(null Runnable).");
						  		break;  // Exiting because wait ended normally.
					  	  	}
					    catch // Handling wait interrupt.
					    	(InterruptedException e1) 
					      { 
					    	  appLogger.debug( "DataTreeModel.invokeAndWaitV(..) null run() interrupted.");
					    	  
				      	  interruptedB= true; // Record interrupt for later.
					      	}
					  	catch  // Handling invocation exception by re-throwing.
					  	  (InvocationTargetException e1) 
					  	  { 
					  			Globals.logAndRethrowAsRuntimeExceptionV( 
					  					"DataTreeModel.invokeAndWaitV(..)", e1
					  					);
					  			} // wrapping and re-throwing.
  	      	  appLogger.debug( "DataTreeModel.invokeAndWaitV(..) end loop.");
	      	  	}
		      	}
		  	catch  // Handling invocation exception by re-throwing.
		  	  (InvocationTargetException e) 
		  	  { 
			  		appLogger.error( "DataTreeModel.invokeAndWaitV(..):"+e );
		    	  throw new RuntimeException(e); 
		  			} // wrapping and re-throwing.
    	  if (interruptedB) // Setting interrupted status if interrupt occurred. 
    	  	Thread.currentThread().interrupt(); 
      	}
    
    protected static boolean checkNotRunningEDTB()
      /* This method returns whether we are running the EDT thread.
        Returns true if not on EDT thread, false otherwise.
        If not on EDT thread it logs an error.
       */
	    {
	      boolean isNotEDTB= ! SwingUtilities.isEventDispatchThread();
	      if ( isNotEDTB ) appLogger.error(" checkNotRunningEDTB() true");
	      return isNotEDTB;
	    	}

}
