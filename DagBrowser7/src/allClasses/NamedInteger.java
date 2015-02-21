package allClasses;

public class NamedInteger // A DataNode for tracking integer attributes.

  extends NamedLeaf 

  {
	  private DataTreeModel theDataTreeModel; // For reporting changes.

	  private int theI;

	  public NamedInteger( // Constructor. 
        DataTreeModel theDataTreeModel,
        String nameString, 
        int theI 
        )
		  {
		  	super(nameString);
        this.theDataTreeModel= theDataTreeModel;
		  	this.theI= theI;
        }

    public String getValueString( ) // DataNode interface method.
      {
        return Integer.toString( theI );
        }

    public int getValueI( )
      {
        return theI;
        }

    public int addValueI( int deltaI )
	    /* This method adds deltaI to the value and returns the new value.
		    It also begins the process of firing associated change listeners.
		    */
      {
    	  setValueI( this.theI + deltaI ); // Adding delta to old value.
	  	  return theI; // Returning new value.
        }

	  public Object setValueI( int theI )
	    /* This method sets a new value and returns the old one.
	      It also begins the process of firing associated change listeners.
	      */
	    {
	  	  int oldI= this.theI; // Saving present value as old one. 
	  	  this.theI= theI; // Setting new value.
	  	  theDataTreeModel.reportingChangeV( this ); // Fire associated listeners.
	  	  return oldI; // Returning old value.
		  	}
    
    }
