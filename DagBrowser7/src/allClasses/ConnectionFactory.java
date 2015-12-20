package allClasses;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;

public class ConnectionFactory {

  /* This is the factory for classes with connection lifetimes.
    
    ?? This factory needs work.
    It has state, but nothing permanent associated with a peer,
    so all of its methods could be moved to the AppGUIFactory
    with little if any change.
    Maybe only the Unicater and UnicasterValue code should stay here.  
    */

  // Injected unconditional singleton storage.
	private AppGUIFactory theAppGUIFactory;
	private PacketQueue netcasterToSenderPacketQueue;
	private DataTreeModel theDataTreeModel;
	private Shutdowner theShutdowner;

  public ConnectionFactory(   // Factory constructor. 
  		AppGUIFactory theAppGUIFactory,
  		PacketQueue netcasterToSenderPacketQueue,
  		DataTreeModel theDataTreeModel,
  		Shutdowner theShutdowner
  		)
  	// This constructor defines the unconditional singletons.
    {
  	  this.theAppGUIFactory= theAppGUIFactory;
      this.netcasterToSenderPacketQueue= netcasterToSenderPacketQueue; 
      this.theDataTreeModel= theDataTreeModel;
      this.theShutdowner= theShutdowner;
      }

  // Unconditional singleton getters.
  // None.

  // Conditional singleton getters and storage.
  // None.

  // Maker methods.  These construct using new operator each time called.

	public InetSocketAddress makeInetSocketAddress( int thePortI )
		{ return new InetSocketAddress( thePortI ); }

	public static InetSocketAddress makeInetSocketAddress( 
			InetAddress theInetAddress, int thePortI 
			)
		{ return new InetSocketAddress( theInetAddress, thePortI ); }

  public DatagramSocket makeDatagramSocket( SocketAddress aSocketAddress )
    throws SocketException
    { return new DatagramSocket( aSocketAddress ); }

  public MulticastSocket makeMulticastSocket( int portI )
    throws IOException
    { return new MulticastSocket( portI ); }
  
	public static LockAndSignal makeLockAndSignal()
		{ return AppGUIFactory.makeLockAndSignal(); }

  public PacketQueue makePacketQueue(LockAndSignal threadLockAndSignal)
  	{ return theAppGUIFactory.makePacketQueue(threadLockAndSignal); }

  public NamedInteger makeNamedInteger( String nameString, int valueI )
    { return new NamedInteger( theDataTreeModel, nameString, valueI ); }

  public NetInputStream makeNetInputStream(
  		PacketQueue receiverToNetCasterPacketQueue
  		)
	  {
			NamedInteger packetsReceivedNamedInteger=  
					makeNamedInteger( "Packets-Received", 0 );
	  	return new NetInputStream(
	  	  receiverToNetCasterPacketQueue, packetsReceivedNamedInteger 
	  		);
	  	}
  
  public NetOutputStream makeNetOutputStream(
  		InetAddress remoteInetAddress, int remotePortI
      )
	  {
		  NamedInteger packetsSentNamedInteger= 
		  		makeNamedInteger( "Packets-Sent", 0 );
		  return new NetOutputStream(
		  	netcasterToSenderPacketQueue, 
		  	remoteInetAddress, 
		  	remotePortI, 
		  	packetsSentNamedInteger
	      );
	    }

	public Multicaster makeMulticaster(
  		MulticastSocket theMulticastSocket,
  		PacketQueue multicasterToConnectionManagerPacketQueue,
  		InetAddress multicastInetAddress
      )
    { 
		  LockAndSignal netcasterLockAndSignal= makeLockAndSignal();  
		  PacketQueue receiverToNetCasterPacketQueue= 
		  		makePacketQueue( netcasterLockAndSignal );
			int thePortI= PortManager.getDiscoveryPortI();
  	  return new Multicaster(
  	  	netcasterLockAndSignal,
	  		makeNetInputStream( receiverToNetCasterPacketQueue ),
	  		makeNetOutputStream( multicastInetAddress, thePortI ),
	  		theDataTreeModel,
	  		makeInetSocketAddress( multicastInetAddress, thePortI ),
	  		theMulticastSocket,
        multicasterToConnectionManagerPacketQueue,
        theAppGUIFactory.getUnicasterManager()
        ); 
      }
	
  public Unicaster makeUnicaster(
  		InetSocketAddress peerInetSocketAddress
      )
    {
		  LockAndSignal netcasterLockAndSignal= makeLockAndSignal();
  		PacketQueue receiverToNetCasterPacketQueue= 
  				makePacketQueue(netcasterLockAndSignal);
  		NetInputStream aNetInputStream= 
  				makeNetInputStream( receiverToNetCasterPacketQueue );
  		NetOutputStream aNetOutputStream= makeNetOutputStream( 
  	  		peerInetSocketAddress.getAddress(), peerInetSocketAddress.getPort()
  				);
  		return new Unicaster(
        theAppGUIFactory.getUnicasterManager(),
  			netcasterLockAndSignal,
	  		aNetInputStream,
	  		aNetOutputStream,
  	  	peerInetSocketAddress,
  	  	theDataTreeModel,
  	  	theShutdowner  	  	
        );
      }
  
  public NetCasterValue makeUnicasterValue(
      InetSocketAddress peerInetSocketAddress
      )
    {
      Unicaster theUnicaster= makeUnicaster( peerInetSocketAddress );
      return new NetCasterValue( peerInetSocketAddress, theUnicaster );
      }

  } // class ConnectionFactory.
