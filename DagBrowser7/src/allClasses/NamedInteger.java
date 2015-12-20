package allClasses;

import static allClasses.Globals.appLogger;


public class NamedInteger // A DataNode for tracking integer attributes.

  extends NamedLeaf

  {
	  private DataTreeModel theDataTreeModel; // For reporting changes.

	  private long theL;

	  public NamedInteger( // Constructor. 
        DataTreeModel theDataTreeModel,
        String nameString, 
        int theL 
        )
		  {
		  	super(nameString);
        this.theDataTreeModel= theDataTreeModel;
		  	this.theL= theL;
        }

    public String getValueString( ) // DataNode interface method.
      {
        return Long.toString( theL );
        }

    public long getValueI( )
      {
        return theL;
        }

    public long addValueWithLoggingL( long deltaL )
	    /* This method does the same as addValueL(deltaL) and
	      it logs deltaL if it is not 0.
	      */
      {
    	  if (deltaL != 0) // Logging deltaL if it's not 0.
		  		appLogger.info( this.getNameString( )+" changed by "+deltaL );
	  	  return addValueL( deltaL ); // Doing the add.
        }

    public long addValueL( long deltaL )
	    /* This method does nothing if deltaL is 0.
	      Otherwise it adds deltaL to the value and returns the new value.
		    It also fires any associated change listeners.
		    */
      {
    	  setValueL( this.theL + deltaL ); // Adding delta to old value.
	  	  return theL; // Returning possibly different value.
        }

		//public Object setValueL( final long newL )  // Does not produce error!
    public long setValueL( final long newL )
	    /* This method does nothing if deltaL is the same value 
	      as the present value of this NamedInteger.
		    Otherwise it sets sets deltaL as the new value 
		    and returns the old unchanged value.
		    It also fires any associated change listeners.
		    */
	    {
	  	  long oldL= this.theL; // Saving present value as old one.
	  	  if ( newL != theL ) // Setting new value if it's different.
	  	    {
						theL= newL; // Setting new value.
		        theDataTreeModel.safelyReportingChangeV( this );
		  	  	}
				return oldL; // Returning old unchanged value.
		  	}

    }
