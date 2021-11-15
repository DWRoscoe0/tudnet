package allClasses;

public class DefaultLongLike
  implements LongLike
  
  /* This class is useful for passing references to long values,
    and for passing long values from a method back to its caller.
    It also does simple addition.
    */
  
  {
	  private long theL;

    public DefaultLongLike() // Constructor. 
      {
        this(0);
        }

    public DefaultLongLike( long theL ) // Constructor. 
      {
        this.theL= theL;
        }

    public synchronized long getValueL( )
      {
        return theL;
        }

    public synchronized long addDeltaL( long deltaL )
	    /* This method adds deltaL to the value and returns the new value.
		    */
      {
    	  setValueL( this.theL + deltaL ); // Adding delta to old value.
	  	  return theL; // Returning possibly different value.
        }

    public synchronized long setValueL( final long newL )
	    /* This method sets newL as the new value 
		    and returns the previous value.
		    */
	    {
	  	  long oldL= this.theL; // Saving present value as old one.
				theL= newL; // Setting new value.
				return oldL; // Returning old unchanged value.
		  	}

    }
