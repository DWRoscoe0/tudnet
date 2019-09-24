package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;

import static allClasses.AppLog.theAppLog;


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
		@SuppressWarnings("unused") ///tmp
  	private final Persistent thePersistent;
  	private final TCPCopier theTCPCopier; 
  
		public UnicasterManager(  // Constructor. 
	      AppGUIFactory theAppGUIFactory,
			  Persistent thePersistent,
			  TCPCopier theTCPCopier
			  )
	    {
	  		// Superclass's injections.
	      super( // Constructing MutableListWithMap superclass.
		        "Unicasters",
		        theAppGUIFactory,
			  		new DataNode[]{} // Initially empty of children.
	      		);
			  this.thePersistent= thePersistent;
			  this.theTCPCopier= theTCPCopier; 
			  
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

    public synchronized Unicaster getOrBuildAndAddUnicaster(
    		String IPString , String portString )
    	/* This method returns a Unicaster associated with
        the remote peer address in theNetcasterPacket.
        If a Unicaster doesn't already exist then it creates one.
        It does not start the Unicaster thread.
        */
	    { 
    		IPAndPort theIPAndPort= new IPAndPort(IPString, portString);
	    	Unicaster theUnicaster= getOrBuildAndAddUnicaster( theIPAndPort );
		    return theUnicaster;
		    }

    public synchronized Unicaster getOrBuildAddAndReconnectUnicaster(
    		NetcasterPacket theNetcasterPacket 
    		)
      /* This method returns a Unicaster associated with
        the remote peer address in theNetcasterPacket.
        If a Unicaster doesn't already exist then this method 
        creates one and starts its thread.
        Because a packet it triggering these events,
        the Unicaster is started in a state to cause 
        its state machine to do a connect and not a reconnect.
        */
	    { 
    		IPAndPort theIPAndPort= makeIPAndPort(theNetcasterPacket);
        Unicaster theUnicaster= tryingToGetUnicaster( theIPAndPort );
        if ( theUnicaster == null ) {
          theUnicaster= buildAndAddUnicaster( theIPAndPort );
          startV(theUnicaster); // Start the Unicaster thread.
          }
		    return theUnicaster;
		    }

    public synchronized Unicaster getOrBuildAndAddUnicaster(
    		IPAndPort theIPAndPort
    		)
      /* This method returns a Unicaster associated with
        the remote peer whose address in theIPAndPort.
        If a Unicaster doesn't already exist then it creates 
        and mostly initializes one and adds it to the tree.
        It does not start the Unicaster.
        */
	    { 
	    	Unicaster theUnicaster= // Testing whether Unicaster exists.  
	    			tryingToGetUnicaster( theIPAndPort );
	    	if ( theUnicaster == null ) // Building one if one doesn't exist.
	    		{ // Building a new Unicaster and adding it to tree.
	          theUnicaster= buildAndAddUnicaster( theIPAndPort );
	    			}
		    return theUnicaster;
		    }

    public synchronized Unicaster tryingToGetUnicaster( 
    		NetcasterPacket theNetcasterPacket
    		)
      /* This method returns the Unicaster associated with the
        address in theNetcasterPacket, if such a Unicaster exists.
        If it doesn't exist then it returns null.
        This method is called to direct incoming UDP packets to
        the appropriate Unicasters, so it should be reasonably fast.
        
        ///opt ? Rewrite for speed for the case of the Unicaster existing.  
        Increasing speed will probably require using something other than
				using the new-operator to create a new IPAndPort.
        */
      {
    		IPAndPort theIPAndPort= makeIPAndPort(theNetcasterPacket);
        return tryingToGetUnicaster( theIPAndPort );
        }

    public IPAndPort makeIPAndPort( 
    		NetcasterPacket theNetcasterPacket
    		)
      /* This method returns IPAndPort associated with theNetcasterPacket.  */
      {
        DatagramPacket theDatagramPacket=  // Getting DatagramPacket.
          theNetcasterPacket.getDatagramPacket();
        IPAndPort theIPAndPort= // Building its remote address keyK
	          AppGUIFactory.makeIPAndPort(		
	              theDatagramPacket.getAddress(), // IP and
	              theDatagramPacket.getPort()  // port #.
	          		);
        return theIPAndPort;
        }

    private synchronized Unicaster tryingToGetUnicaster(IPAndPort theIPAndPort)
      /* This method returns the Unicaster associated with the
        address in theKeyedPacket, if such a Unicaster exists.
        If it doesn't exist then it returns null.
        */
      {
        return tryingToGetDataNodeWithKeyD( theIPAndPort );
        }

    private Unicaster buildAndAddUnicaster( IPAndPort theIPAndPort )
      /* This method builds and mostly initializes a Unicaster 
        to handle communications with a peer at address theIPAndPort.
        It should be called only if the Unicaster does not already exist.
        Is not synchronized because it is called only from 
        other synchronized local methods.
        addingV(..) to tree is done last because it could trigger displaying.
        This is the lowest level method for building a Unicaster.
        It does not start the Unicaster thread.
        */
	    { 
    	  UnicasterFactory theUnicasterFactory= 
    	  		theAppGUIFactory.makeUnicasterFactory( 
    	  				theIPAndPort, theTCPCopier );
	      final UnicasterValue resultUnicasterValue=  // Getting the Unicaster. 
	      	theUnicasterFactory.getUnicasterValue();
	      Unicaster theUnicaster= resultUnicasterValue.getDataNodeD(); 
    		/// appLogger.info("buildAndAddUnicaster(..) "
	      ///     + "initializing root state machine of new Unicaster.");
        try { // Operations that might produce an IOException.
      			theUnicaster.initializeWithIOExceptionV(
      			    resultUnicasterValue.getEpiThread());
        	} catch( IOException e ) {
        		Globals.logAndRethrowAsRuntimeExceptionV( 
        				"buildAndAddUnicaster(..) IOException", e );
          }
	      addingV( // Adding the new Unicaster to data structures.
	          theIPAndPort, resultUnicasterValue );
	      return theUnicaster;
	      }

    public void startV(Unicaster theUnicaster)
      /* This method starts the thread associated with theUnicaster.  */
      {
        IPAndPort theIPAndPort= theUnicaster.getKeyK();
        UnicasterValue theUnicasterValue= childHashMap.get(theIPAndPort);
        EpiThread theEpiThread= theUnicasterValue.getEpiThread();
        theEpiThread.startV(); // Start the actual thread.
        }


    public String getValueString( )
	    /* Returns a string indicating the numbers of Unicasters being managed,
				both connected and disconnected.
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

    
    public void passToUnicastersV(String messageString)
      /* This method passes messageString to only connected Unicasters,
        each of which is expected to process it with its state machine. 
        */
      {
        theAppLog.debug("passToUnicastersV(..), to only connected Unicasters, message:"
          + messageString);
        for ( DataNode childDataNode: this )  // For every Unicaster 
          { // [ass message to it.
            Unicaster theUnicaster= ((Unicaster)childDataNode);
            if (theUnicaster.isConnectedB())
              theUnicaster.getNotifyingQueueOfStrings().
                add(messageString);
            }
        }

  	}
