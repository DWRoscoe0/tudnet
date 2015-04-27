package allClasses;

public class NamedMutable 
  extends NamedLeaf 
  {

	  // Injected dependencies.
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
	  
	  public Object setValueObject( final Object newObject )
	    /* This method sets a new value and returns the old one.
	      The old one is returned to simplify object pooling and recycling.
  	    */
		  {
	  		final DataNode thisDataNode= this; // Converting this pointer.
	  	  Object oldObject= this.valueObject; // Saving present value. 

	  	  runOrInvokeAndWaitV( // Do following on AWT thread. 
	    		new Runnable() {
	    			@Override  
	          public void run() {
	    				valueObject= newObject; // Setting new value.
	    	  	  theDataTreeModel.reportingChangeV(  // Fire associated listeners. 
	    	  	  		thisDataNode 
	    	  	  		);
	            }
	          } 
	        );

	  	  return oldObject; // Returning old value.
		  	}

    public Object getValueObject( )
      {
        return valueObject;
        }
    
    }
