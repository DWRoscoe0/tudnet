package allClasses;

import static allClasses.Globals.appLogger;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
  
		public UnicasterManager(  // Constructor. 
	      AppGUIFactory theAppGUIFactory,
			  Persistent thePersistent
	      )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        "Unicasters",
		        theAppGUIFactory,
			  		new DataNode[]{} // Initially empty of children.
	      		);
			  this.thePersistent= thePersistent;

			  testPersistantV();
			  }
	
		
			private void testPersistantV() {
				//% updatePeerInfoV( "a" );
				//% updatePeerInfoV( "b" );
				//% updatePeerInfoV( "c" );

			  try {
				  updatePeerInfoV( AppGUIFactory.makeIPAndPort ( 
				  		InetAddress.getByName( "1.2.3.4" ), 5 ) );
				  updatePeerInfoV( AppGUIFactory.makeIPAndPort ( 
				  		InetAddress.getByName( "11.22.33.44" ), 55 ) );
					}
			  catch ( UnknownHostException e ) { 
        	Globals.logAndRethrowAsRuntimeExceptionV( "testPersistantV()", e );
			  	}
			  }

	    public void updatePeerInfoV( IPAndPort theIPAndPort )
		    {	
	    	  String peerIDString= theIPAndPort.toString(); // Calculate peer ID.
	    	  
	    	  // Store or update the list structure.
	    		String entryIDKeyString= thePersistent.entryInsertOrMoveToFrontString( 
	    				peerIDString, "peer" 
	    				);
	        		
	    	  // Store or update the other fields.
	    		thePersistent.putV( // IP address.
	    				entryIDKeyString + "IP", theIPAndPort.getInetAddress().toString()
	    				);
	    		thePersistent.putV( // Port.
	    				entryIDKeyString + "Port", 
	    				String.valueOf( theIPAndPort.getPortI() ) 
	    				);
	    		} 

	    /*//%
	    public void updatePeerInfoV( String peerIDString )
		    {	
	    		String entryIDKeyString= thePersistent.entryInsertOrMoveToFrontString( 
	    				peerIDString, "peer" 
	    				);
	        		
	    		// Add other fields.
	    		thePersistent.putV( entryIDKeyString + "IP", "IP-test-data" );
	    		thePersistent.putV( entryIDKeyString + "Port", "Port-test-data" );
	    		} 
	    */ //%
	
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
        */
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
    
	  protected synchronized void addingV( 
	  		IPAndPort peerIPAndPort, UnicasterValue resultUnicasterValue
	  		)
	    /* This adds to both ... to the HashMap and this MutableList
	      and adjusts the persistent Persistent structures.
	      */
	    {
	  		addingV( // Adding new Unicaster to data structures.
          peerIPAndPort, resultUnicasterValue
          );
	  		}
	  
	  ////// test actually creating peer.

  	}
