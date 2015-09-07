package allClasses;

public class NamedMutable 
  extends NamedLeaf 
  {

	  // Injected dependencies.
		protected DataTreeModel theDataTreeModel; // For reporting changes.

	  private Object valueObject;

	  public NamedMutable( // Constructor. 
        DataTreeModel theDataTreeModel,
	  		String nameString,
	  		final Object newObject
	  		)
		  {
		  	super(nameString); // Constructing base class. 

        // Storing injected values stored in this class.
        this.theDataTreeModel= theDataTreeModel;
				valueObject= newObject; // Setting new value.
        }

    public String getValueString( ) // DataNode interface method.
      {
        return valueObject.toString();
        }
	  
	  public Object setValueObject( final Object newObject )
	    /* This method sets a new value and returns the old one.
	      The old one is returned to simplify object pooling and recycling.
  	    */
		  {
	  	  Object oldObject= this.valueObject; // Saving present value. 
				valueObject= newObject; // Setting new value.
	  	  theDataTreeModel.safelyReportingChangeV( this );
	  	  return oldObject; // Returning old value.
		  	}

    public Object getValueObject( )
      {
        return valueObject;
        }
    
    }
