package allClasses;

public class DefaultLongLike
  implements LongLike

  {
	  private long theL;

	  public DefaultLongLike( long theL ) // Constructor. 
		  {
		  	this.theL= theL;
        }

    public long getValueL( )
      {
        return theL;
        }

    public long addDeltaL( long deltaL )
	    /* This method adds deltaL to the value and returns the new value.
		    */
      {
    	  setValueL( this.theL + deltaL ); // Adding delta to old value.
	  	  return theL; // Returning possibly different value.
        }

    public long setValueL( final long newL )
	    /* This method sets newL as the new value 
		    and returns the previus value.
		    */
	    {
	  	  long oldL= this.theL; // Saving present value as old one.
				theL= newL; // Setting new value.
				return oldL; // Returning old unchanged value.
		  	}

    }
