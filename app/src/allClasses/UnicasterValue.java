package allClasses;


public class UnicasterValue

	extends DataNodeWithKeyAndThreadValue< Unicaster, IPAndPort >

  {

    public UnicasterValue(  // Constructor. 
        IPAndPort remoteIPAndPort,
        Unicaster theUnicaster
        )
      {
    	  super( 
    	  		theUnicaster,
        		new EpiThread( 
  		          theUnicaster,
  		          "Unicaster-"+remoteIPAndPort
  		          )
    	  		);
        }

    }
