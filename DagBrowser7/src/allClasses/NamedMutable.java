package allClasses;

public class NamedMutable 
  extends NamedLeaf 
  {
	
		protected DataTreeModel theDataTreeModel; // For reporting changes.

	  private Object valueObject;

	  public NamedMutable( // Constructor. 
        DataTreeModel theDataTreeModel,
	  		String nameString 
	  		)
		  {
		  	super(nameString); // Constructing base class. 

        // Storing injected values stored in this class.
        this.theDataTreeModel= theDataTreeModel;
        }

    public String getValueString( ) // DataNode interface method.
      {
        return valueObject.toString();
        }
	  
	  public Object setValueObject( Object valueObject )
	    /* This method sets a new value and returns the old one.
	      The old one is returned to simplify object pooling and recycling.
  	    */
		  {
	  	  Object oldObject= this.valueObject; // Saving present value. 
	  	  this.valueObject= valueObject; // Setting new value.
	  	  return oldObject; // Returning old value.
	  		
	      //theDataTreeModel.reportingInsertV( this, indexI, childDataNode );
		  	}

    public Object getValueObject( )
      {
        return valueObject;
        }
    
    }
