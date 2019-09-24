package allClasses;

import static allClasses.AppLog.theAppLog;


public class NamedLong
  extends NamedLeaf
  implements LongLike

  {
	  private long theL;

	  public NamedLong( // Constructor. 
        String nameString, 
        long theL
        )
		  {
	  		super.initializeV( nameString );

		  	this.theL= theL;
        }

    public String getValueString( ) // DataNode interface method.
      {
        return Long.toString( theL );
        }

    public synchronized long getValueL( )
      {
        return theL;
        }

    public long addDeltaAndLogNonzeroL( long deltaL )
	    /* This method does the same as addDeltaL(deltaL) but
	      also logs as a warning any deltaL which not 0.
	      This is for NamedIntegers which are not supposed to change.
	      */
      {
    	  if (deltaL != 0) // Logging deltaL if it's not 0.
		  		theAppLog.debug( this.getNameString( )+" changed by "+deltaL );
	  	  return addDeltaL( deltaL ); // Doing the add.
        }

    public long addDeltaL( long deltaL )
	    /* This method does nothing if deltaL is 0.
	      Otherwise it adds deltaL to the value and returns the new value.
		    It also fires any associated change listeners.
		    */
      {
    	  setValueL( this.theL + deltaL ); // Adding delta to old value.
	  	  return theL; // Returning possibly different value.
        }

    public long setValueL( final long newL )
	    /* This method does nothing if the new value newL is the same value 
	      as the present value of this NamedLong.
		    Otherwise it sets sets newL as the new value 
		    and returns the old unchanged value.
		    It also fires any associated change listeners.
		    */
	    {
    	  boolean changedB;
	  	  long resultL;
	  	  
    	  synchronized (this) { // Set the new value if different from old one.
		  	  resultL= theL; // Saving present value for returning.
		  	  changedB= newL != theL;
		  	  if ( changedB ) // Setting new value if it's different.
		  	    {
							theL= newL; // Setting new value.
							}
    	  	}
    	  
    	  if (changedB) { // Firing change listeners.
	        reportChangeOfSelfV(); // Reporting change of this node.
    	  	}
				return resultL; // Returning old before-changed value.
		  	}

    }
