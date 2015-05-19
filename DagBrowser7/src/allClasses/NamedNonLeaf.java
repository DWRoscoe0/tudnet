package allClasses;

///import java.lang.reflect.InvocationTargetException;

///import javax.swing.SwingUtilities;

public abstract class NamedNonLeaf 

  extends AbDataNode

  {

    private String nameString;  // The name associated with this node.

    NamedNonLeaf ( String nameString )  // Constructor.
      { 
        super( ); 
        this.nameString = nameString;  // Store this node's name.
        }

    public String getNameString( )
      /* Returns String representing name of this Object.  */
      {
        return nameString;  // Simply return the name.
        }

    /* ???
    protected void runOrInvokeAndWaitV( Runnable theRunnable )
      /* This helper method runs theRunnable on the AWT thread.
        one way or another.
        It already running on the AWT thread then it just calls run().
        Otherwise it uses invokeAndWait(..).
       */
    /* ???
	    {
	      if ( SwingUtilities.isEventDispatchThread() )
	        theRunnable.run();
	      else
	        invokeAndWaitV( theRunnable );
	    	}

    protected void invokeAndWaitV( Runnable theRunnable )
      // Calls SwingUtilities.invokeAndWait(..) and handles any exceptions.
      {
		  	try  // Queuing theRunnable on AWT thread.
		  	  { SwingUtilities.invokeAndWait( theRunnable ); 			  		}
		    catch // Handling wait interrupt by
		    	(InterruptedException e) 
		      { Thread.currentThread().interrupt(); } // setting interrupt flag.
		        // Is a termination request so no need to continue waiting.
		  	catch  // Handling invocation exception by
		  	  (InvocationTargetException e) 
		  	  { throw new RuntimeException(e); } // wrapping and re-throwing.
      	}
    ??? */

    }
