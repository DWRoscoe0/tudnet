package allClasses;

public class Subcaster

	extends MutableList

	/* ?? This is not being used, yet.
	  Eventually it will handle nested protocols for Unicaster.
	  */

	{
	  public LockAndSignal theLockAndSignal;
			// LockAndSignal for inputs to this thread.  It is used in
	    // the construction of the following queue. 
	  
	  protected NetOutputStream theNetOutputStream;
		protected NetInputStream theNetInputStream;

	  public Subcaster(  // Constructor. 
	      LockAndSignal theLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetOutputStream theNetOutputStream,
	      DataTreeModel theDataTreeModel,
	      String namePrefixString,
	      String namePostfixString
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableList superclass.
		        theDataTreeModel,
		        namePrefixString + namePostfixString,
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's injections.
	      this.theLockAndSignal= theLockAndSignal;
	      this.theNetInputStream= theNetInputStream;
	      this.theNetOutputStream= theNetOutputStream;
	      }

    protected void initializeV()
	    {
		    addB( 	theNetOutputStream.getCounterNamedInteger() );
		    addB( 	theNetInputStream.getCounterNamedInteger());
	    	}

		}
