package allClasses;

import static allClasses.Globals.appLogger;

import java.io.IOException;
import java.net.DatagramPacket;

public class UnicasterManager

	extends StreamcasterManager<
		IPAndPort, // Key for map.
		Unicaster, // DataNode in Value.
		UnicasterValue // Value for map. 
		>

	/* This class manages the app's Unicasters.
	  It provides methods for creating them, storing references to them,
	  displaying them, testing for their existence, and destroying them.
	  Most of its methods are synchronized.
	  */

  {
  	private final Persistent thePersistent;
  	private final TCPCopier.TCPClient theTCPClient; 
  
		public UnicasterManager(  // Constructor. 
	      AppGUIFactory theAppGUIFactory,
			  Persistent thePersistent,
			  TCPCopier.TCPClient theTCPClient
			  )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        "Unicasters",
		        theAppGUIFactory,
			  		new DataNode[]{} // Initially empty of children.
	      		);
			  this.thePersistent= thePersistent;
			  this.theTCPClient= theTCPClient; 
			  
			  testPersistentV(); ///dbg
			  }
	
		
			private void testPersistentV() {
				//% updatePeerInfoV( "a" );
				//% updatePeerInfoV( "b" );
				//% updatePeerInfoV( "c" );

				//updatePeerInfoV( "IDTest1", "1.2.3.4", "5" );
				//updatePeerInfoV( "IDTest2", "11.22.33.44", "55" );

				// This is the important one for test TCPCopier.
				//updatePeerInfoV( "IDLocalHost", "127.0.0.1", "11111" );
			  }

	    public void updatePeerInfoV( 
	    		String peerIDString, String ipString, String portString)
		    {	
			  	// Store or update the list structure.
	    		String entryIDKeyString= thePersistent.entryInsertOrMoveToFrontString( 
	    				peerIDString, "peers" 
	    				);

	    	  // Store or update the other fields.
	    		thePersistent.putV( // IP address.
	    				entryIDKeyString + "IP", ipString
	    				);
	    		thePersistent.putV( // Port.
	    				entryIDKeyString + "Port", portString
	    				);
	    		} 

	    public synchronized Unicaster getOrBuildAddAndStartUnicaster(
      		NetcasterPacket theNetcasterPacket 
      		)
        /* This method returns a Unicaster associated with
          the remote peer address in theNetcasterPacket.
          If a Unicaster doesn't already exist then it creates one.
          */
  	    { 
	    	Unicaster theUnicaster= // Testing whether Unicaster exists.  
	    			tryingToGetUnicaster( theNetcasterPacket );
	    	if ( theUnicaster == null ) // Building one if one doesn't exist.
	    		{ // Building a new Unicaster.
	        	DatagramPacket theDatagramPacket=  // Getting DatagramPacket.
	            theNetcasterPacket.getDatagramPacket();
	        	IPAndPort peerIPAndPort= // Building its remote address.
		          AppGUIFactory.makeIPAndPort(		
		              theDatagramPacket.getAddress(), // IP and
		              theDatagramPacket.getPort()  // port #.
		          		);
	          theUnicaster= buildAddAndStartUnicaster( peerIPAndPort );
	    		}
		    return theUnicaster;
		    }

    public synchronized Unicaster tryingToGetUnicaster( 
    		NetcasterPacket theNetcasterPacket 
    		)
      /* This method returns the Unicaster associated with the
        address in theKeyedPacket, if such a Unicaster exists.
        If it doesn't exist then it returns null.
        
        ?? Rewrite for speed for the case of the Unicaster existing.  
        Increasing speed will probably require using something other than
				using the new-operator to create a new IPAndPort.
        */
      {
        DatagramPacket theDatagramPacket=  // Getting DatagramPacket.
          theNetcasterPacket.getDatagramPacket();
        IPAndPort peerIPAndPort= // Building its remote address keyK
	          AppGUIFactory.makeIPAndPort(		
	              theDatagramPacket.getAddress(), // IP and
	              theDatagramPacket.getPort()  // port #.
	          		);
        return tryingToGetDataNodeWithKeyD( peerIPAndPort );
        }

    private Unicaster buildAddAndStartUnicaster( IPAndPort peerIPAndPort )
      /* This method builds a Unicaster to handle 
        communications with a peer at address peerIPAndPort.
        It should be called only if the Unicaster does not exist.
        Is not synchronized because it is called only from 
        other synchronized local methods.
        addingV(..) to tree is done last because it could trigger display.
        */
	    { 
    	  appLogger.info( 
    	  		"buildAddAndStartUnicaster(..) Creating new Unicaster." );
    	  UnicasterFactory theUnicasterFactory= 
    	  		theAppGUIFactory.makeUnicasterFactory( 
    	  				peerIPAndPort, theTCPClient );
	      final UnicasterValue resultUnicasterValue=  // Getting the Unicaster. 
	      	theUnicasterFactory.getUnicasterValue();
	      Unicaster theUnicaster= resultUnicasterValue.getDataNodeD(); 
    		appLogger.info(
    				"buildAddAndStartUnicaster(..) initializing root state machine.");
        try { // Operations that might produce an IOException.
      			theUnicaster.initializeWithIOExceptionV();
        	} catch( IOException e ) {
        		appLogger.debug( ///dbg
        				"buildAddAndStartUnicaster(..) IOException" + e );
        		Globals.logAndRethrowAsRuntimeExceptionV( 
        				"buildAddAndStartUnicaster(..) IOException", e );
          }
	      addingV( // Adding the new Unicaster to data structures.
	          peerIPAndPort, resultUnicasterValue );
	      resultUnicasterValue.getEpiThread().startV(); // Start its thread.
	      return theUnicaster;
	      }

    public String getValueString( )
	    /* Returns a string indicating 
	      the numbers of Unicasters connected and disconnected.
       	*/
      {
    	  int connectedI= 0;
    	  int disconnectedI= 0;
    	  for ( DataNode childDataNode: this)
    	  	if (((Unicaster)childDataNode).isConnectedB())
    	  		connectedI++; 
    	  		else 
    	  		disconnectedI++;
    	  return "("+connectedI+" connected, "+disconnectedI+" disconnected)";
        }


  	}
