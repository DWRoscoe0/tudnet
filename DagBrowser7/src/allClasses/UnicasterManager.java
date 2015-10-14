package allClasses;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnicasterManager
  {
	  private Map<SocketAddress,NetCasterValue> 
    peerSocketAddressConcurrentHashMap= // Setting map empty initially.
	    new ConcurrentHashMap<SocketAddress,NetCasterValue>();
      // This probably doesn't need to be a concurrent map,
      // but it probably doesn't hurt.
	    /* This initially empty Collection provides lookup of Unicasters
	      by associated SocketAddress.
	      It is used mainly to determine which, if any, Unicaster
	      is the source of a newly received packet.
				Presently only connected Unicasters are put in this Map.

				?? This map is kept synchronized with 
				the ConnectionManager's superclass MutableList,
				but that synchronization is not done by this class.
	      */
	  
    private Map<SocketAddress,NetCasterValue> getMap()
      {
    	  return peerSocketAddressConcurrentHashMap;
        }
    
    public void putV( // Pass-through to map.
    		InetSocketAddress theInetSocketAddress,
    		NetCasterValue theNetCasterValue
        )
    { 
    	peerSocketAddressConcurrentHashMap.put( 
    		theInetSocketAddress, theNetCasterValue
    		);
    	}
	  
    public NetCasterValue getNetCasterValue(  // Pass-through to map. 
    		InetSocketAddress theInetSocketAddress )
      {
    	  return peerSocketAddressConcurrentHashMap.get(theInetSocketAddress);
        }

    public Unicaster tryGettingExistingUnicaster( SockPacket theSockPacket )
      /* This method returns the Uniaster associated with the
         source address of theSockPacket, if such as Unicaster exists.
         It returns null otherwise.
        
        ?? Rewrite for speed.  This method is called for every packet,
        and most of the time it returns an indication that
        there is a Unicaster at the remote address of the SockPacket,
        it should be very fast.
        */
      {
        DatagramPacket theDatagramPacket=  // Get DatagramPacket.
          theSockPacket.getDatagramPacket();
        return tryGettingExistingUnicaster( 
            theDatagramPacket.getAddress(), // IP and
            theDatagramPacket.getPort()  // port #.
        		);
        }

    public Unicaster tryGettingExistingUnicaster( 
    		InetAddress theInetAddress, int portI 
    		)
	    {
	      InetSocketAddress peerInetSocketAddress= // Build remote address
	          new InetSocketAddress( theInetAddress, portI );
        NetCasterValue theNetCasterValue= // Testing whether peer is already stored.
            getMap().get(peerInetSocketAddress);
        Unicaster theUnicaster= // Calculating the Unicaster based on
          ( theNetCasterValue != null ) // whether value was found, 
	        	? theNetCasterValue.getUnicaster() // value's Unicaster if found, 
	        	: null; // or null if it wasn't.
        return theUnicaster;
        }
  		
    public void removeV( Unicaster thisUnicaster )
      /* This method removes thisUnicaster from the map.  */
	    {
		  	InetSocketAddress theInetSocketAddress=
		      thisUnicaster.getInetSocketAddress();
		    getMap().remove( // Remove from Map.
		      theInetSocketAddress
		      );
		    }
    
    public void stoppingPeerThreadsV()
      /* This method terminates all of the Unicaster threads 
        that were created to communicate with discovered Unicaster nodes.
        It does this in 2 loops: the first to request terminations,
        and a second to wait for terminations to complete; 
        because faster concurrent terminations are possible 
        when there are multiple Peers.

				?? If Unicasters need to exchange packets during termination,
				then this can not be called from the same thread as the
				ConnectionManager processing loop, unless none of those packets
				goes through the ConnectionManager processing loop.
        */
      {
    	  // Prepare both iterators now before Unicasters disappear from Map.
	      Iterator<NetCasterValue> stopIteratorOfNetCasterValues=
            getMap().values().iterator();
	      Iterator<NetCasterValue> joinIteratorOfNetCasterValues=
            getMap().values().iterator();

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
            joinIteratorOfNetCasterValues.remove(); // Removing from HashMap...
            }
        }

  	}
