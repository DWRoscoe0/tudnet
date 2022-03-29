package allClasses;

public class DefaultBooleanLike

  implements BooleanLike

  {
    private boolean theB;

    public DefaultBooleanLike( boolean theB ) // Constructor. 
      {
        this.theB= theB;
        }

    public boolean getValueB() 
      {
        return theB;
        }

    public boolean setValueB( final boolean newB )
      /* This method sets newB as the new value 
        and returns the previous value.
        */
      {
        boolean oldL= this.theB; // Saving present value as old one.
        theB= newB; // Setting new value.
        return oldL; // Returning old unchanged value.
        }

    }
