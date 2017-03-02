package allClasses;

import static allClasses.Globals.appLogger;


public class NamedFloat // A DataNode for tracking floating point things.

  //// Change this and NamedInt to use generic NamedNumber.

  extends NamedLeaf

  {
	  private DataTreeModel theDataTreeModel; // For reporting changes.

	  private float theF;

	  public NamedFloat( // Constructor. 
        DataTreeModel theDataTreeModel,
        String nameString, 
        float theF 
        )
		  {
		  	////////super(nameString);
	  		super.initializeV( nameString );
        this.theDataTreeModel= theDataTreeModel;
		  	this.theF= theF;
        }

    public String getValueString( ) // DataNode interface method.
      {
        return Float.toString( theF );
        }

    public float getValueF( )
      {
        return theF;
        }

    public float addValueWithLoggingF( float deltaF )
	    /* This method does the same as addValueL(deltaF) and
	      it logs deltaF if it is not 0.
	      */
      {
    	  if (deltaF != 0) // Logging deltaF if it's not 0.
		  		appLogger.info( this.getNameString( )+" changed by "+deltaF );
	  	  return addValueL( deltaF ); // Doing the add.
        }

    public float addValueL( float deltaF )
	    /* This method does nothing if deltaF is 0.
	      Otherwise it adds deltaF to the value and returns the new value.
		    It also fires any associated change listeners.
		    */
      {
    	  setValueF( this.theF + deltaF ); // Adding delta to old value.
	  	  return theF; // Returning possibly different value.
        }

    public float setValueF( final float newL )
	    /* This method does nothing if deltaF is the same value 
	      as the present value of this NamedLong.
		    Otherwise it sets sets deltaF as the new value 
		    and returns the old unchanged value.
		    It also fires any associated change listeners.
		    */
	    {
	  	  float oldF= this.theF; // Saving present value as old one.
	  	  if ( newL != theF ) // Setting new value if it's different.
	  	    {
						theF= newL; // Setting new value.
		        theDataTreeModel.safelyReportingChangeV( this );
		  	  	}
				return oldF; // Returning old unchanged value.
		  	}

    }
