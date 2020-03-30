package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;

import static allClasses.AppLog.theAppLog;
//// import static allClasses.SystemSettings.NL;


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
    		String IPString , String portString, String theIdString )
    	/* This method returns a Unicaster associated with
        the remote peer address in theNetcasterPacket.
        If a Unicaster doesn't already exist then it creates one.
        It does not start the Unicaster thread.
        */
	    { 
    		IPAndPort theIPAndPort= new IPAndPort(IPString, portString);
	    	Unicaster theUnicaster= getOrBuildAddAndStartUnicaster( theIPAndPort, theIdString );
		    return theUnicaster;
		    }

    public synchronized Unicaster getOrBuildAddAndStartUnicaster(
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
        /// theAppLog.debug( 
        ///  "ConnectionManager.getOrBuildAddAndStartUnicaster(NetcasterPacket) called.");
    		IPAndPort theIPAndPort= makeIPAndPort(theNetcasterPacket);

		    return getOrBuildAddAndStartUnicaster(theIPAndPort,null);
		    }

    public synchronized Unicaster getOrBuildAddAndStartUnicaster(IPAndPort theIPAndPort, String theIdString)
      /* This method returns a Unicaster associated with
        the remote peer whose address is theIPAndPort and ID is theIdString
        If a Unicaster doesn't already exist then it creates and initializes one, 
        adds it to the tree, and starts its thread.
        */
      { 
        /// theAppLog.debug( 
        ///    "ConnectionManager.getOrBuildAddAndStartUnicaster(IPAndPort) called.");
        Unicaster theUnicaster= // Testing whether Unicaster exists.  
            tryingToGetUnicaster( theIPAndPort );
        if ( theUnicaster == null ) // Build, add, and start one if one doesn't exist.
          theUnicaster= buildAddAndStartUnicaster( theIPAndPort, theIdString );
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
        */
      {
    		IPAndPort theIPAndPort= makeIPAndPort(theNetcasterPacket);
        return tryingToGetUnicaster( theIPAndPort );
        }


    public IPAndPort makeIPAndPort(NetcasterPacket theNetcasterPacket)
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



    public synchronized Unicaster buildAddAndStartUnicaster( 
          IPAndPort theIPAndPort, String theIdString )
      /* This method returns a Unicaster to handle communications 
        with a peer at address theIPAndPort.
        It should be called only if the Unicaster does not already exist.
        If it does exist then it logs an error.
        If it does not exist then it builds one, adds it to 
        the appropriate data structures, and starts the Unicaster's thread.
        addingV(..) to tree is done near the end because it could trigger displaying.
        It returns the Unicaster, whether it existed already or was built by this method.
        */
	    { 
        /// theAppLog.debug( "ConnectionManager.buildAndAddUnicaster(IPAndPort) called.");
        Unicaster theUnicaster= tryGettingAndLoggingPreexistingUnicaster( theIPAndPort );
        if ( theUnicaster == null ) // Unicaster does not yet exist.
          { // So build, add, and start the non-existent Unicaster.
        	  UnicasterFactory theUnicasterFactory= theAppGUIFactory.makeUnicasterFactory(
        	    theIPAndPort, theIdString, theTCPCopier);
    	      final UnicasterValue resultUnicasterValue=  // Getting the Unicaster. 
    	      	theUnicasterFactory.getUnicasterValue();
    	      theUnicaster= resultUnicasterValue.getDataNodeD(); 
            try { // Operations that might produce an IOException.
          			theUnicaster.initializeWithIOExceptionV(
          			    resultUnicasterValue.getEpiThread());
            	} catch( IOException e ) {
            		Misc.logAndRethrowAsRuntimeExceptionV( 
            		  "UnicasterManager.buildAddAndStartUnicaster(IPAndPort) IOException", e );
              }
    	      addingV( // Adding the new Unicaster to tree data structures.
    	          theIPAndPort, resultUnicasterValue ); // This might trigger display.
            startV(theUnicaster); // Start Unicaster's thread.
            }
	      return theUnicaster;
	      }


    public synchronized Unicaster tryGettingAndLoggingPreexistingUnicaster(
        IPAndPort theIPAndPort)
      /* This method tests whether the Unicaster associated with 
        the address in theKeyedPacket exists.
        If it exists then it logs an error and returns the Unicaster.
        If it doesn't exist then it returns null.
        */
      {
        Unicaster theUnicaster= // Testing whether Unicaster exists.  
            tryingToGetUnicaster( theIPAndPort );
        if ( theUnicaster != null ) // This Unicaster already exists.
            theAppLog.error( // Log this as an error.
              "UnicasterManager.tryGettingAndLoggingPreexistingUnicaster(IPAndPort) "
              + theIPAndPort + " already exists!");
        return theUnicaster;
        }

    public synchronized Unicaster tryingToGetUnicaster(IPAndPort theIPAndPort)
      /* This method returns the Unicaster associated with the
        address in theKeyedPacket, if such a Unicaster exists.
        If it doesn't exist then it returns null.
        */
      {
        return tryingToGetDataNodeWithKeyD( theIPAndPort );
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
                //// add(messageString);
                put(messageString);
            }
        }

  	}
