package allClasses;

public class NsAsMsNamedLong

	extends NamedLong
	
	/* This class is basically a NamedLong which displays its value
	  as if divided by one million.
	  It was created to display nanosecond time values in milliseconds.
	  */
	
	{
	  public NsAsMsNamedLong( // Constructor. 
        DataTreeModel theDataTreeModel,
        String nameString, 
        long theL
        )
		  {
		  	super( theDataTreeModel, nameString, theL );
        }

    public String getValueString( ) // DataNode interface method.
      {
        return String.format("%9.6f", getValueL() / 1000000. );
        }

    }
