package allClasses;

import java.net.InetSocketAddress;


public class NetCasterValue

	extends DataNodeWithKeyAndThreadValue< Unicaster, InetSocketAddress >

  // Shouldn't this be UnicasterValue???

  {

    public NetCasterValue(  // Constructor. 
        InetSocketAddress remoteInetSocketAddress,
        Unicaster theUnicaster
        )
      {
    	  super( 
    	  		theUnicaster,
        		new EpiThread( 
  		          theUnicaster,
  		          "Unicaster-"+remoteInetSocketAddress
  		          )
    	  		);
        }

    }
