package allClasses;


public class UnicasterFactory {

  /* This is the factory for classes with Unicaster lifetimes.
    This is not a singleton factory.  Each Unicaster has its own.

    This might eventually be converted to a scope object,
    or divided into a scope object and an injector object??
    */

  // Injected dependencies that need saving for later.
	private final DataTreeModel theDataTreeModel;
	public final AppGUIFactory theAppGUIFactory;
	private final Shutdowner theShutdowner;
	
	// Other objects that will be needed later.
	private final UnicasterValue unicasterUnicasterValue; 
	private final SubcasterQueue subcasterToUnicasterSubcasterQueue;
	
  public UnicasterFactory(   // Factory constructor. 
  		AppGUIFactory theAppGUIFactory,
  		UnicasterManager theUnicasterManager,
  		IPAndPort unicasterIPAndPort,
  		DataTreeModel theDataTreeModel,
  		Shutdowner theShutdowner
  		)
  	// This builds all objects that are or comprise unconditional singletons
    // relative to their Unicaster.
    {
		  LockAndSignal unicasterLockAndSignal= new LockAndSignal();
			NetcasterQueue receiverToUnicasterNetcasterQueue= 
					new NetcasterQueue( unicasterLockAndSignal );
			NetcasterInputStream unicasterNetcasterInputStream=
					theAppGUIFactory.makeNetcasterInputStream( 
							receiverToUnicasterNetcasterQueue 
							);
		  NetcasterPacketManager theNetcasterPacketManager=
		  		new NetcasterPacketManager( unicasterIPAndPort );
			NetcasterOutputStream unicasterNetcasterOutputStream= 
					theAppGUIFactory.makeNetcasterOutputStream( 
						theNetcasterPacketManager 
						);
			subcasterToUnicasterSubcasterQueue= 
					new SubcasterQueue( unicasterLockAndSignal );
		  SubcasterManager theSubcasterManager= 
					new SubcasterManager( theDataTreeModel, theAppGUIFactory, this );
	    Unicaster theUnicaster= new Unicaster(
	    		theUnicasterManager,
	    		theSubcasterManager,
					unicasterLockAndSignal,
      		unicasterNetcasterInputStream,
		  		unicasterNetcasterOutputStream,
		  		unicasterIPAndPort,
			  	theDataTreeModel,
			   	theShutdowner,
			   	subcasterToUnicasterSubcasterQueue
			  	);
  	
	    UnicasterValue unicasterUnicasterValue=  
  				new UnicasterValue( unicasterIPAndPort, theUnicaster );

      // Save in instance variables injected objects that are needed later.
  		this.theAppGUIFactory= theAppGUIFactory;
	  	this.theDataTreeModel= theDataTreeModel;
	  	this.theShutdowner= theShutdowner;

	  	// Save in instance variables other objects that are needed later.
      this.unicasterUnicasterValue= unicasterUnicasterValue;
			//this.subcasterToUnicasterPacketQueue= subcasterToUnicasterPacketQueue; 
      }

  // Unconditional singleton getters.

  public UnicasterValue getUnicasterValue() // Returns created Unicaster.
  	{ return unicasterUnicasterValue; }
  
  // Conditional singleton getters and storage.
  // None.

  // Maker methods.  These construct using new operator each time called.
  // None.
	

  public SubcasterValue makeSubcasterValue( 
  		String keyString
      )
	  { 
		  LockAndSignal subcasterLockAndSignal= new LockAndSignal();
			SubcasterPacketManager theSubcasterPacketManager=
					new SubcasterPacketManager( keyString ); 
			SubcasterQueue unicasterToSubcasterSubcasterQueue=
					new SubcasterQueue( subcasterLockAndSignal );
			SubcasterInputStream theSubcasterInputStream= 
			  	makeSubcasterInputStream( unicasterToSubcasterSubcasterQueue );
			SubcasterOutputStream theSubcasterOutputStream= 
					makeSubcasterOutputStream( 
							keyString, theSubcasterPacketManager
							);
  	  Subcaster unicasterSubcaster= new Subcaster(
  	  		subcasterLockAndSignal,
  	  		theSubcasterInputStream,
  				theSubcasterOutputStream,
  	      theDataTreeModel,
  	      keyString,
  	      theShutdowner
  	      );
	    SubcasterValue unicasterSubcasterValue=  
  				new SubcasterValue( keyString, unicasterSubcaster );
	    return unicasterSubcasterValue;
	  	}

  	private SubcasterOutputStream makeSubcasterOutputStream( 
  			String keyString, 
  			SubcasterPacketManager theSubcasterPacketManager
  		  )
  	  {
  		  NamedInteger packetsSentNamedInteger= 
  					new NamedInteger( theDataTreeModel, "Packets-Sent", 0 );
  		  return new SubcasterOutputStream(
  		  	subcasterToUnicasterSubcasterQueue,
  		  	theSubcasterPacketManager,
  		  	packetsSentNamedInteger
  	      );
  	    }

  	public SubcasterInputStream makeSubcasterInputStream(
  			SubcasterQueue receiverToSubcasterSubcasterQueue
  			)
  	  {
  			NamedInteger packetsReceivedNamedInteger=  
  					new NamedInteger( theDataTreeModel, "Packets-Received", 0 );
  	  	return new SubcasterInputStream(
  	  	  receiverToSubcasterSubcasterQueue, packetsReceivedNamedInteger 
  	  		);
  	  	}

} // class UnicasterFactory.
