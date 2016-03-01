package allClasses;

public class SubcasterValue

	extends DataNodeWithKeyAndThreadValue< Subcaster, String >
  // 2 of 5 errors disappeared!  All disappeared when I did a Project/Clean.   

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
