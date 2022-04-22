package allClasses;

public class NsAsMsNamedLong

  extends NamedLong
  
  /* This class is basically a NamedLong which displays its value
    as if divided by one million.
    It was created to display nanosecond time values in milliseconds.
    */
  
  {
    public NsAsMsNamedLong( // Constructor. 
        String nameString, 
        long theL
        )
      {
        super( nameString, theL );
        }

    public String getContentString() // DataNode interface method.
      {
        return
    
            (Long.MIN_VALUE == previousValueL) // If previous value was undefined
    
            ? String.format( // then return converted value only
                "%+9.6f", valueL / 1000000. )
    
            : String.format( // otherwise return converted value and delta.
                "%+9.6f [%+9.6f]", 
                valueL / 1000000., 
                (valueL-previousValueL) / 1000000.
                )
    
            ;
        }

    }
