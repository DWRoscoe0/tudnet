package allClasses;

import static allClasses.Globals.appLogger;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;

public class UnicasterManager

	extends MutableListWithMap<SocketAddress,NetCasterValue>

	/* This class manages the app's Unicasters.
	  It provides methods for creating them, storing references to them,
	  displaying them, testing for their existence, and destroying them.
	  Most of its methods are synchronized.
	  */

  {
		
	  /* private Map<SocketAddress,NetCasterValue> childHashMap= // Setting empty.
		  new ConcurrentHashMap<SocketAddress,NetCasterValue>();
		   
	    The above Map was moved to the superclass.
	    It is an initially empty Collection 
	    which provides fast lookup of Unicasters
      by associated IPAddress and port.
      It is used mainly to quickly determine which, if any, Unicaster
      is associated with a remote address from which a DatagramPacket
      is received.
      
			Presently only connected Unicasters are put in this Map.

			?? This map is kept synchronized with 
			this class's superclass MutableList.

      ?? This doesn't need to be a concurrent map, but it doesn't hurt.
     */

		private ConnectionFactory theConnectionFactory; // Our factory.
	
		public UnicasterManager(  // Constructor. 
	      DataTreeModel theDataTreeModel
	      ,ConnectionFactory theConnectionFactory
        ,ConnectionManager theConnectionManager
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        theDataTreeModel,
		        "Unicasters",
	          new DataNode[]{} // Initially empty of children.
	      		);

	      // This class's injections.
	  		this.theConnectionFactory= theConnectionFactory;
	      }

    public synchronized void removingV( Unicaster thisUnicaster ) 
      /* This method's job is to remove the thisUnicaster from
	      this MutableList and the HashMap.
        It is called when thisUnicaster is terminating.
        */
	    {
	    	if  // Removing from DataNode List if it's there.
	      	( ! removeB( thisUnicaster ) )
	      	appLogger.error("UnicasterManager.removingV(..): removeB(..) failed");

		  	InetSocketAddress theInetSocketAddress=
		      thisUnicaster.getInetSocketAddress();
		    childHashMap.remove( // Remove from Map.
		      theInetSocketAddress
		      );
		    }

    public String getValueString( ) // This is for displaying Unicaster count.
      {
    	  return Integer.toString(getChildCount( ));
        }

    public synchronized Unicaster tryGettingUnicaster( 
    		SockPacket theSockPacket 
    		)
      /* This method returns the Unicaster associated with the
        source address of theSockPacket, if such a Unicaster exists.
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
          theSockPacket.getDatagramPacket();
	      InetSocketAddress peerInetSocketAddress= // Building its remote address.
	          new InetSocketAddress( 
	              theDatagramPacket.getAddress(), // IP and
	              theDatagramPacket.getPort()  // port #.
	          		);
        NetCasterValue theNetCasterValue= // Testing whether Unicaster exists.
            childHashMap.get(peerInetSocketAddress);
        Unicaster theUnicaster;
        if ( theNetCasterValue != null ) 
	        theUnicaster= theNetCasterValue.getD(); 
        else
        	theUnicaster= null;
        return theUnicaster;
        }

    public synchronized Unicaster buildAddAndStartUnicaster(
        InetSocketAddress peerInetSocketAddress
	  		)
	    { 
    	  appLogger.info( "Creating new Unicaster." );
	      final NetCasterValue resultNetCasterValue=  // Building new peer. 
	        theConnectionFactory.makeUnicasterValue(
	        	peerInetSocketAddress
	          );
	      addingV( // Adding new Unicaster to data structures.
	          peerInetSocketAddress, resultNetCasterValue
	          );
	      resultNetCasterValue.getEpiThread().startV(); // Start it's thread.
	      return resultNetCasterValue.getD();
	      }

    public void stoppingPeerThreadsV()
      /* This method terminates all of the Unicaster threads in its collection.
        It does this in 2 loops: the first to request terminations,
        and a second to wait for the terminations to complete.
        This is done in anticipation of when termination will require
        a handshake, requiring significant time,
        and therefore would benefit from parallel termination.
        
        ?? Maybe this method belongs back in the ConnectionManager.
        */
      {
    	  // Creating both iterators now before 
    	  //   Unicasters begin disappearing from Map.
	      Iterator<NetCasterValue> stopIteratorOfNetCasterValues=
            childHashMap.values().iterator();
	      Iterator<NetCasterValue> joinIteratorOfNetCasterValues=
            childHashMap.values().iterator();

        while  // Requesting termination of all Unicasters.
          (stopIteratorOfNetCasterValues.hasNext())
          {
            NetCasterValue theNetCasterValue= 
            		stopIteratorOfNetCasterValues.next();
            theNetCasterValue.getEpiThread().stopV(); // Requesting termination. 
            }

        while  // Waiting for completion of termination of all Unicasters.
          (joinIteratorOfNetCasterValues.hasNext())
          {
            NetCasterValue theNetCasterValue= 
            		joinIteratorOfNetCasterValues.next();
            theNetCasterValue.getEpiThread().joinV(); // Awaiting termination 
            //joinIteratorOfNetCasterValues.remove(); // Removing from HashMap...
            }
        }

  	}
