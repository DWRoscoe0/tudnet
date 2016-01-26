package allClasses;

import java.net.InetAddress;

public class UnicasterFactory {

  /* This is the factory for classes with Unicaster lifetimes.
    This is not a singleton factory.  Each Unicaster has one.

    This might eventually be converted to a scope object,
    or divided into a scope object and an injector object??
    */

  // Injected dependencies that need saving for later.
	private final DataTreeModel theDataTreeModel;
	private final AppGUIFactory theAppGUIFactory;
	private final Shutdowner theShutdowner;
	
	// Other objects that will be needed later.
	private final UnicasterValue unicasterUnicasterValue; 
	//private final PacketQueue subcasterToUnicasterPacketQueue;
	
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
			PacketQueue receiverToUnicasterPacketQueue= 
					new PacketQueue( unicasterLockAndSignal );
			NetInputStream unicasterNetInputStream= 
					theAppGUIFactory.makeNetcasterNetInputStream( receiverToUnicasterPacketQueue );
			InetAddress unicasterInetAddress= unicasterIPAndPort.getInetAddress(); 
			int unicasterPortI= unicasterIPAndPort.getPortI();
			NetOutputStream unicasterNetOutputStream= 
					theAppGUIFactory.makeNetcasterNetOutputStream( 
						unicasterInetAddress, unicasterPortI
						);
		  SubcasterManager theSubcasterManager= 
					new SubcasterManager( theDataTreeModel, this );
	    Unicaster theUnicaster= new Unicaster(
	    		theUnicasterManager,
	    		theSubcasterManager,
					unicasterLockAndSignal,
		  		unicasterNetInputStream,
		  		unicasterNetOutputStream,
		  		unicasterIPAndPort,
			  	theDataTreeModel,
			   	theShutdowner
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
			PacketQueue unicasterToSubcasterPacketQueue= 
					new PacketQueue( subcasterLockAndSignal );
			NetInputStream subcasterNetInputStream= 
					theAppGUIFactory.makeNetcasterNetInputStream( 
							unicasterToSubcasterPacketQueue 
							);
		  InetAddress unicasterInetAddress= null; 
			int unicasterPortI= 0;
			NetOutputStream subcasterNetOutputStream= 
					theAppGUIFactory.makeNetcasterNetOutputStream( 
						unicasterInetAddress, unicasterPortI
						  // subcasterToUnicasterPacketQueue: use this instead??
						);
  	  Subcaster unicasterSubcaster= new Subcaster(
  	  		subcasterLockAndSignal,
  				subcasterNetInputStream, 
  				subcasterNetOutputStream, 
  	      theDataTreeModel,
  	      keyString,
  	      theShutdowner
  	      );
	    SubcasterValue unicasterSubcasterValue=  
  				new SubcasterValue( keyString, unicasterSubcaster );
	    return unicasterSubcasterValue;
	  	}

  } // class UnicasterFactory.
