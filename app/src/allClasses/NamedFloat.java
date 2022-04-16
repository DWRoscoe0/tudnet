package allClasses;

import static allClasses.AppLog.theAppLog;


public class NamedFloat // A DataNode for tracking floating point things.

  ///? Change this and NamedInt to use generic NamedNumber.

  extends NamedLeaf

  {
    private float theF;

    public NamedFloat( // Constructor. 
        String nameString, 
        float theF 
        )
      {
        super.setNameV( nameString );
        this.theF= theF;
        }

    public String getContentString( ) // DataNode interface method.
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
          theAppLog.info( this.getNameString( )+" changed by "+deltaF );
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

    public float setValueF( final float newF )
      /* This method does nothing if the new value newF is the same value 
        as the present value of this NamedFloat.
        Otherwise it sets sets newF as the new value 
        and returns the old unchanged value.
        It also fires any associated change listeners.
        */
    {
      boolean changedB;
      float resultF;
      
      synchronized (this) { // Set the new value if different from old one.
        resultF= theF; // Saving present value for returning.
        changedB= newF != theF;
        if ( changedB ) // Setting new value if it's different.
          {
            theF= newF; // Setting new value.
            }
        }
      
      if (changedB) { // Firing change listeners.
        signalChangeOfSelfV(); // Reporting change of this node.
        }
      return resultF; // Returning old before-changed value.
      }

    }
