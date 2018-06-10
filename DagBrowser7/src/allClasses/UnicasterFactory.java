package allClasses;

import java.util.Timer;

public class UnicasterFactory {

  /* This is the factory for classes with Unicaster lifetimes.
    This is not a singleton factory.  Each Unicaster has its own.

    This might eventually be converted to a scope object,
    or divided into a scope object and an injector object??
    */

  // Injected dependencies that need saving for later.
	public final AppGUIFactory theAppGUIFactory;
	private final Shutdowner theShutdowner;
	private final int queueCapacityI;
	private final Timer theTimer;
	
	// Other objects that will be needed later.
	private final UnicasterValue unicasterUnicasterValue; 
	private final SubcasterQueue subcasterToUnicasterSubcasterQueue;
	private final NamedLong retransmitDelayMsNamedLong;

	
  public UnicasterFactory(   // Factory constructor. 
  		AppGUIFactory theAppGUIFactory,
  		UnicasterManager theUnicasterManager,
  		IPAndPort unicasterIPAndPort,
  		TCPCopier.TCPClient theTCPClient,
  		Shutdowner theShutdowner,
  		int queueCapacityI,
  		Timer theTimer
  		)
  	/* This builds all objects that are or comprise 
  	  unconditional singletons relative to their Unicaster.
  	  */
    {
  	  LockAndSignal unicasterLockAndSignal= new LockAndSignal();
			NetcasterQueue receiverToUnicasterNetcasterQueue= 
					new NetcasterQueue( unicasterLockAndSignal, queueCapacityI );
			NetcasterInputStream unicasterNetcasterInputStream=
					theAppGUIFactory.makeNetcasterInputStream( 
							receiverToUnicasterNetcasterQueue, 
				  		Config.delimiterChar
							);
		  NetcasterPacketManager theNetcasterPacketManager=
		  		new NetcasterPacketManager( unicasterIPAndPort );
			NetcasterOutputStream unicasterNetcasterOutputStream= 
					theAppGUIFactory.makeNetcasterOutputStream( 
						theNetcasterPacketManager 
						);
			subcasterToUnicasterSubcasterQueue= 
					new SubcasterQueue( unicasterLockAndSignal, queueCapacityI );
      DefaultBooleanLike leadingDefaultBooleanLike=
      		new DefaultBooleanLike(false);  // Used to settle race conditions.
		  SubcasterManager theSubcasterManager= new SubcasterManager( 
		  		theAppGUIFactory, this, leadingDefaultBooleanLike 
		  		);
      NamedLong retransmitDelayMsNamedLong= new NamedLong( 
					"Retransmit-Delay (ms)",
					Config.initialRoundTripTime100MsL * 2
					);
	    Unicaster theUnicaster= new Unicaster(
	    		theUnicasterManager,
	    		theSubcasterManager,
					unicasterLockAndSignal,
      		unicasterNetcasterInputStream,
		  		unicasterNetcasterOutputStream,
		  		unicasterIPAndPort,
		  		theTCPClient,
			   	theShutdowner,
			   	subcasterToUnicasterSubcasterQueue,
			   	theTimer,
		      retransmitDelayMsNamedLong,
		      leadingDefaultBooleanLike
			  	);
  	
	    
	    UnicasterValue unicasterUnicasterValue=  
  				new UnicasterValue( unicasterIPAndPort, theUnicaster );

      // Save in instance variables injected objects that are needed later.
  		this.theAppGUIFactory= theAppGUIFactory;
	  	this.theShutdowner= theShutdowner;
  		this.queueCapacityI= queueCapacityI;
  		this.theTimer= theTimer;

	  	// Save in instance variables other objects that are needed later.
      this.unicasterUnicasterValue= unicasterUnicasterValue;
      this.retransmitDelayMsNamedLong= retransmitDelayMsNamedLong;
      }

  // Unconditional singleton getters.

  public UnicasterValue getUnicasterValue() // Returns created Unicaster.
  	{ return unicasterUnicasterValue; }
  
  // Conditional singleton getters and storage.
  // None.

  // Maker methods.  These construct using new operator each time called.
  // None.
	

  public SubcasterValue makeSubcasterValue( 
  		String keyString,
  		DefaultBooleanLike leadingDefaultBooleanLike
      )
	  { 
		  LockAndSignal subcasterLockAndSignal= new LockAndSignal();
			SubcasterPacketManager theSubcasterPacketManager=
					new SubcasterPacketManager( keyString ); 
			SubcasterQueue unicasterToSubcasterSubcasterQueue=
					new SubcasterQueue( subcasterLockAndSignal, queueCapacityI );
			SubcasterInputStream theSubcasterInputStream= 
			  	makeSubcasterInputStream( 
			  			unicasterToSubcasterSubcasterQueue, 
				  		Config.delimiterChar
			  			);
			SubcasterOutputStream theSubcasterOutputStream= 
					makeSubcasterOutputStream( 
							keyString, theSubcasterPacketManager
							);
  	  Subcaster unicasterSubcaster= new Subcaster(
  	  		subcasterLockAndSignal,
  	  		theSubcasterInputStream,
  				theSubcasterOutputStream,
  	      keyString,
  	      theShutdowner,
  	      leadingDefaultBooleanLike,
  	      retransmitDelayMsNamedLong 
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
  		  NamedLong packetsSentNamedLong= 
  					new NamedLong( "Outgoing-Packets-Sent", 0 );
  		  return new SubcasterOutputStream(
  		  	subcasterToUnicasterSubcasterQueue,
  		  	theSubcasterPacketManager,
  		  	packetsSentNamedLong,
  	  		theTimer,
  	  		Config.delimiterChar
  	      );
  	    }

  	public SubcasterInputStream makeSubcasterInputStream(
  			SubcasterQueue receiverToSubcasterSubcasterQueue,
  			char delimiterChar
				)
  	  {
  			NamedLong packetsReceivedNamedLong=  
  					new NamedLong( "Incoming-Packets-Received", 0 );
  	  	return new SubcasterInputStream(
  	  	  receiverToSubcasterSubcasterQueue, 
  	  	  packetsReceivedNamedLong, 
  	  	  delimiterChar
  				);
  	  	}

} // class UnicasterFactory.
