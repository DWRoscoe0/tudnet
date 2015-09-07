package allClasses;

import java.io.IOException;
import java.net.InetSocketAddress;

public class NetCaster 
	
	extends MutableList

	// This class is the superclass of Unicaster and Multicaster.
	
	{
		protected InetSocketAddress remoteInetSocketAddress;  // Address of peer.
	  
    // Detail-containing child sub-objects.
	    protected NamedMutable addressNamedMutable;
	    protected NamedMutable portNamedMutable;
	    protected NamedInteger packetsSentNamedInteger;
	    protected NamedInteger packetsReceivedNamedInteger;

	  public NetCaster(  // Constructor. 
	      DataTreeModel theDataTreeModel,
	      InetSocketAddress remoteInetSocketAddress,
	      String namePrefixString
	      )
	    {
	      super( // Constructing MutableList.  
		        theDataTreeModel,
		        namePrefixString + 
    	          remoteInetSocketAddress.getAddress() +
    	          ":" + remoteInetSocketAddress.getPort(),
	          new DataNode[]{} // Initially empty of children.
	      		);
	
	      this.remoteInetSocketAddress= remoteInetSocketAddress;
	      }

    protected void initializeV()
	    throws IOException
	    {
    		addB( 	addressNamedMutable= new NamedMutable( 
		        theDataTreeModel, 
		        "IP-Address", 
		        "" + remoteInetSocketAddress.getAddress()
		      	)
					);
		    
		    addB( 	portNamedMutable= new NamedMutable( 
			      		theDataTreeModel, "Port", "" + remoteInetSocketAddress.getPort()
			      		)
		    			);
		
		    addB( 	packetsSentNamedInteger= new NamedInteger( 
			      		theDataTreeModel, "Packets-Sent", 0 
			      		)
		    			);
		
		    addB( 	packetsReceivedNamedInteger= new NamedInteger( 
			      		theDataTreeModel, "Packets-Received", 0 
			      		)
		    			);
	    	}
    
		InetSocketAddress getInetSocketAddress()
			{ return remoteInetSocketAddress; }
	
		}
