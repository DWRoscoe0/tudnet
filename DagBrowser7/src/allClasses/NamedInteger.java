package allClasses;

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

    public long addValueL( long deltaL )
	    /* This method adds deltaL to the value and returns the new value.
		    It also begins the process of firing associated change listeners.
		    */
      {
    	  setValueL( this.theL + deltaL ); // Adding delta to old value.
	  	  return theL; // Returning new value.
        }

	  public Object setValueL( long theL )
	    /* This method sets a new value and returns the old one.
	      It also begins the process of firing associated change listeners.
	      */
	    {
	  	  long oldL= this.theL; // Saving present value as old one. 
	  	  this.theL= theL; // Setting new value.
	  	  theDataTreeModel.reportingChangeV( this ); // Fire associated listeners.
	  	  return oldL; // Returning old value.
		  	}

	  public void run( )
	    /* This method runs on the AWT thread.
	      */
	    {
		  	}

    }
