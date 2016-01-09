package allClasses;

public class SubcasterValue

	extends DataNodeWithKeyAndThreadValue< Subcaster, String >

  {

    public SubcasterValue(  // Constructor. 
        String subcasterString,
        Subcaster theSubcaster
        )
      {
    	  super( 
    	  		theSubcaster,
        		new EpiThread( 
  		          theSubcaster,
  		          "SubcasterValue-"+subcasterString
  		          )
    	  		);
        }

    }
