package allClasses;

import java.net.InetSocketAddress;


public class NetCasterValue

   extends DataNodeAndThreadValue< Unicaster >

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
