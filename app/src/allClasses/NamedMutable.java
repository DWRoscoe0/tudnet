package allClasses;

public class NamedMutable 
  extends NamedLeaf 
  {

	  // Injected dependencies.
	  private Object valueObject;

	  public NamedMutable( // Constructor. 
	  		String nameString,
	  		final Object newObject
	  		)
		  {
	  		super.setNameV( nameString );

        // Storing injected values stored in this class.
				valueObject= newObject; // Setting new value.
        }

    public String getContentString( ) // DataNode interface method.
      {
        return valueObject.toString();
        }

    public Object setValueObject( final Object newObject )
	    /* This method does nothing if the new value newObject is the same value 
	      as the present value of this NamedMuteable.
		    Otherwise it sets sets newObject as the new value 
		    and returns the old unchanged value.
		    It also fires any associated change listeners.
		    */
	    {
    	  boolean changedB;
	  	  Object resultObject;
	  	  
    	  synchronized (this) { // Set the new value if different from old one.
		  	  resultObject= valueObject; // Saving present value for returning.
		  	  changedB= newObject != valueObject;
		  	  if ( changedB ) // Setting new value if it's different.
		  	    {
		  	  		valueObject= newObject; // Setting new value.
							}
    	  	}
    	  
    	  if (changedB) { // Firing change listeners.
	        signalChangeOfSelfV(); // Reporting change of this node.
    	  	}
				return resultObject; // Returning old before-changed value.
		  	}

    public Object getValueObject( )
      {
        return valueObject;
        }
    
    }
