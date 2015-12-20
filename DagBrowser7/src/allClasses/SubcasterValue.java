package allClasses;

public class SubcasterValue

  extends DataNodeAndThreadValue< Unicaster >

  {

    public SubcasterValue(  // Constructor. 
        String subcasterString,
        Unicaster theUnicaster
        )
      {
    	  super( 
    	  		theUnicaster,  /// change this to Subcaster.
        		new EpiThread( 
  		          theUnicaster,
  		          "SubcasterValue-"+subcasterString
  		          )
    	  		);
        }

    }
