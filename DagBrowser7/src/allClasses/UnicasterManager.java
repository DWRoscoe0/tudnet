package allClasses;

import static allClasses.Globals.appLogger;

import java.net.DatagramPacket;

public class UnicasterManager

	extends StreamcasterManager<
		IPAndPort, // Key for map.
		UnicasterValue, // Value for map. 
		Unicaster // DataNode in Value.
		>

	/* This class manages the app's Unicasters.
	  It provides methods for creating them, storing references to them,
	  displaying them, testing for their existence, and destroying them.
	  Most of its methods are synchronized.
	  */

  {
		
		private final AppGUIFactory theAppGUIFactory;
	
		public UnicasterManager(  // Constructor. 
	      DataTreeModel theDataTreeModel
	      ,AppGUIFactory theAppGUIFactory
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        theDataTreeModel,
		        "Unicasters",
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's injections.
	      this.theAppGUIFactory= theAppGUIFactory;
	      }

    public synchronized Unicaster tryGettingUnicaster( 
    		NetcasterPacket theNetcasterPacket 
    		)
      /* This method returns the Unicaster associated with the
        source address of theNetcasterPacket, if such a Unicaster exists.
        If it doesn't exist then it returns null.
        
        ?? Rewrite for speed.  
        This method is called for every packet received by 
        the app from a peer, so it should be very fast.
        Most of the time it returns an indication that
        there exists a Unicaster to receive and process that packet.
        Increasing speed will probably require using something other than
        InetSocketAddress as the HashMap key. 
				This different key object would allow the IPAddress and the port 
				to be stored into an existing single instance, and be reused, 
				instead of using the new-operator to create a new InetSocketAddress.
				The raw IP, as a byte array, must be stored instead of InetAddress 
				because though InetAddress has no public constructors, 
        it probably uses them internally,
        and also has no way to store the IPAddress.
        */
      {
        DatagramPacket theDatagramPacket=  // Getting DatagramPacket.
          theNetcasterPacket.getDatagramPacket();
        IPAndPort peerIPAndPort= // Building its remote address.
	          AppGUIFactory.makeIPAndPort(		
	              theDatagramPacket.getAddress(), // IP and
	              theDatagramPacket.getPort()  // port #.
	          		);
        UnicasterValue theUnicasterValue= // Testing whether Unicaster exists.
            childHashMap.get(peerIPAndPort);
        Unicaster theUnicaster;
        if ( theUnicasterValue != null ) 
	        theUnicaster= theUnicasterValue.getDataNodeD(); 
        else
        	theUnicaster= null;
        return theUnicaster;
        }

    public synchronized Unicaster buildAddAndStartUnicaster(
        IPAndPort peerIPAndPort
	  		)
	    { 
    	  appLogger.info( "Creating new Unicaster." );
    	  UnicasterFactory theUnicasterFactory=
    	  		theAppGUIFactory.makeUnicasterFactory( peerIPAndPort );
	      final UnicasterValue resultUnicasterValue=  // Getting the Unicaster. 
	      	theUnicasterFactory.getUnicasterValue();
	      addingV( // Adding new Unicaster to data structures.
	          peerIPAndPort, resultUnicasterValue
	          );
	      resultUnicasterValue.getEpiThread().startV(); // Start its thread.
	      return resultUnicasterValue.getDataNodeD();
	      }

  	}
