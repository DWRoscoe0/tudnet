package allClasses;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;

import javax.swing.JFrame;

public class AppGUIFactory {  // For classes with GUI lifetimes.

  /* This is the factory for all classes with GUI scope.
    Most have lifetimes close to the AppGUI lifetime, but some are shorter.
    The app has a maximum of one instance of this factory.
    
    This factory wires together the 2nd level of the application.
    The classes constructed here are mainly the ones 
    used to present a GUI to the user and must of the GUI content.
    
    There is only one AppGUI, 
    and all its code could have been put in the AppFactory,
    but a GUI is not always created,
    so it made sense to divide the code between the App and GUI factory classes.

    */

  // Injected dependencies that need saving for later.
  private final Shutdowner theShutdowner;

	// Other objects that will be needed later.
  private final UnicasterManager theUnicasterManager;
  private final LockAndSignal senderLockAndSignal;
  private final PacketQueue netcasterToSenderPacketQueue;
  private final PacketQueue unconnectedReceiverToConnectionManagerPacketQueue;
  private final DataTreeModel theDataTreeModel;
  private final AppGUI theAppGUI;
  private final NetcasterPacketManager receiverNetcasterPacketManager;
  
  public AppGUIFactory(  // Factory constructor.
  		AppFactory XtheAppFactory, // Not needed??
  		Shutdowner theShutdowner,
  		AppInstanceManager theAppInstanceManager
  		)
  	// This builds all objects that are or comprise unconditional singletons.
    // Note, no non-static maker methods are called from here.
    {
  	  LockAndSignal senderLockAndSignal= new LockAndSignal();
  	  PacketQueue netcasterToSenderPacketQueue= 
  	  		new PacketQueue( senderLockAndSignal );
      DataRoot theDataRoot= new DataRoot();
      MetaFileManager theMetaFileManager= new MetaFileManager( theDataRoot );
      MetaRoot theMetaRoot= new MetaRoot( theDataRoot, theMetaFileManager );
      MetaFileManager.Finisher theMetaFileManagerFinisher= 
      	new MetaFileManager.Finisher( theMetaFileManager, theMetaRoot );
      DataTreeModel theDataTreeModel= new DataTreeModel( 
        theDataRoot, theMetaRoot, theMetaFileManagerFinisher, theShutdowner 
        );
	    LockAndSignal cmThreadLockAndSignal= new LockAndSignal();
	    PacketQueue multicasterToConnectionManagerPacketQueue=
	      new PacketQueue(cmThreadLockAndSignal);
	    PacketQueue unconnectedReceiverToConnectionManagerPacketQueue=
	      new PacketQueue(cmThreadLockAndSignal);
      UnicasterManager theUnicasterManager= new UnicasterManager( 
        	theDataTreeModel, this
       		);
	    ConnectionManager theConnectionManager= new ConnectionManager(
        this, // the AppGuiFactory.
        theUnicasterManager,
        theDataTreeModel,
  	    cmThreadLockAndSignal,
  	    multicasterToConnectionManagerPacketQueue,
  	    unconnectedReceiverToConnectionManagerPacketQueue
  	    );
      EpiThread theConnectionManagerEpiThread=
        AppGUIFactory.makeEpiThread( theConnectionManager, "ConnMgr" );
      SystemsMonitor theSystemsMonitor= new SystemsMonitor(theDataTreeModel);
      EpiThread theCPUMonitorEpiThread=
        AppGUIFactory.makeEpiThread( theSystemsMonitor, "SystemsMonitor" );
      DataNode theInitialRootDataNode=  // Building DataNode tree.
        new InfogoraRoot( 
          new DataNode[] { // An array of all of the child DataNodes.
            new FileRoots(),
            new Outline( 0, theDataTreeModel ),
            theSystemsMonitor,
            theConnectionManager,
            new Infinitree( null, 0 )
            }
          );
      DagBrowserPanel theDagBrowserPanel= new DagBrowserPanel(
    			theAppInstanceManager, theDataTreeModel, theDataRoot, theMetaRoot
	        );
      LockAndSignal theGUILockAndSignal= new LockAndSignal();
      AppGUI.GUIDefiner theGUIDefiner= new AppGUI.GUIDefiner( 
  		  theGUILockAndSignal, 
  		  theAppInstanceManager,
  		  theDagBrowserPanel,
        this, // GUIDefiner gets to know the factory that made it. 
        theShutdowner
        );
      AppGUI theAppGUI= new AppGUI( 
        theConnectionManagerEpiThread,
        theCPUMonitorEpiThread,
        theDataTreeModel,
        theInitialRootDataNode,
        theGUILockAndSignal,
        theGUIDefiner,
        theShutdowner
        );

      receiverNetcasterPacketManager= 
      		new NetcasterPacketManager(null,0);

      // Save in instance variables injected objects that are needed later.
  	  this.theShutdowner= theShutdowner;
  	  
  	  // Save in instance variables other objects that are needed later.
      this.theUnicasterManager= theUnicasterManager;
      this.senderLockAndSignal= senderLockAndSignal;
      this.netcasterToSenderPacketQueue= netcasterToSenderPacketQueue;
      this.unconnectedReceiverToConnectionManagerPacketQueue=
      		unconnectedReceiverToConnectionManagerPacketQueue;
      this.theDataTreeModel= theDataTreeModel;
      this.theAppGUI= theAppGUI;
      }

  // Unconditional singleton getter methods with null checking.
  // There shouldn't be more than one.
  // More than one indicates cyclic dependencies, which are undesirable.
  
  public AppGUI getAppGUI() // This is the main getter of this factory.
  	{ 
  		return Nulls.fastFailNullCheckT( theAppGUI );
  	  }

  // Conditional singleton getter methods and storage.
  // None.

  
  // Maker methods which construct something with new-operator each time called.
  // These are for classes with multiple instances in space or time.
  // ?? fastFailNullCheckT(..) might no longer be needed.

	public static EpiThread makeEpiThread( Runnable aRunnable, String nameString )
	  { return new EpiThread( aRunnable, nameString ); }
  
	public JFrame makeJFrame( String titleString ) 
    { return new JFrame( titleString ); }
  
	public AppGUI.InstanceCreationRunnable makeInstanceCreationRunnable( 
  		JFrame aJFrame
  		)
	  // This should be a singleton because aJFrame is always only one??
  	{ return theAppGUI.new InstanceCreationRunnable( aJFrame ); }

  public EpiThread makeSenderEpiThread( 
  		DatagramSocket unconnectedDatagramSocket
  		)
	  {
	    Sender theSender= makeSender( unconnectedDatagramSocket );
	    EpiThread theSenderEpiThread= 
	    		AppGUIFactory.makeEpiThread( theSender, "Sender" );
	    return theSenderEpiThread;
	    }
  
	private Sender makeSender(
			DatagramSocket unconnectedDatagramSocket 
			)
	  {
			Nulls.fastFailNullCheckT( unconnectedDatagramSocket );
			return new Sender( 
	      unconnectedDatagramSocket,
	      netcasterToSenderPacketQueue,
	      senderLockAndSignal
	      );
			}
  
  public EpiThread makeUnconnectedReceiverEpiThread(
  		DatagramSocket unconnectedDatagramSocket
  		)
	  {
			UnconnectedReceiver theUnconnectedReceiver= 
					makeUnconnectedReceiver( unconnectedDatagramSocket );
			EpiThread theUnconnectedReceiverEpiThread= 
					AppGUIFactory.makeEpiThread( theUnconnectedReceiver, "UcRcvr" );
	    return theUnconnectedReceiverEpiThread;
	    }
	  
  private UnconnectedReceiver makeUnconnectedReceiver(
  		DatagramSocket unconnectedDatagramSocket
  		)
	  {
  		Nulls.fastFailNullCheckT( unconnectedDatagramSocket );
			return new UnconnectedReceiver( 
	          unconnectedDatagramSocket,
	          unconnectedReceiverToConnectionManagerPacketQueue,
	          theUnicasterManager,
	          receiverNetcasterPacketManager
	          );
	    }
  
	public static EpiThread makeMulticastReceiverEpiThread(
			PacketQueue multicastReceiverToMulticasterPacketQueue,
			MulticastSocket theMulticastSocket,
   		NetcasterPacketManager receiverNetcasterPacketManager
			)
	  {
		
			Multicaster.MulticastReceiver theMulticastReceiver=  // Constructing runnable.
		    new Multicaster.MulticastReceiver( 
			   		multicastReceiverToMulticasterPacketQueue,
			   		theMulticastSocket,
			   		receiverNetcasterPacketManager
            );
		  EpiThread theMulticastReceiverEpiThread= // Constructing thread.
		  	new EpiThread( theMulticastReceiver, "McRcvr" );
		  return theMulticastReceiverEpiThread;
	    }

	public static InetSocketAddress makeInetSocketAddress( int remotePortI )
	  // This is needed for the unconnected DatagramSocket.
	  // It cannot use an IPAndPort.
		{ return new InetSocketAddress( remotePortI ); }

	/* ???
	  public static InetSocketAddress makeInetSocketAddress( 
	 	InetAddress theInetAddress, int remotePortI 
			)
	  // This SocketAddress is for the Multicaster.
		//{ return new InetSocketAddress( theInetAddress, remotePortI ); }
		{ return makeIPAndPort( theInetAddress, remotePortI ); }
  ??? */

	public static IPAndPort makeIPAndPort( 
			InetAddress theInetAddress, int remotePortI 
			)
	  // This SocketAddress is for the Multicaster.
		{ return new IPAndPort( theInetAddress, remotePortI ); }
	
  public DatagramSocket makeDatagramSocket( SocketAddress aSocketAddress )
    throws SocketException
    { return new DatagramSocket( aSocketAddress ); }

  public MulticastSocket makeMulticastSocket( int portI )
    throws IOException
    { return new MulticastSocket( portI ); }

	public NetInputStream makeNetcasterNetInputStream(
			PacketQueue receiverToNetCasterPacketQueue
			)
	  {
			NamedInteger packetsReceivedNamedInteger=  
					new NamedInteger( theDataTreeModel, "Packets-Received", 0 );
	  	return new NetInputStream(
	  	  receiverToNetCasterPacketQueue, packetsReceivedNamedInteger 
	  		);
	  	}
	
	public NetOutputStream makeNetcasterNetOutputStream(
			InetAddress remoteInetAddress, int remotePortI
	    )
	  {
		  NamedInteger packetsSentNamedInteger= 
					new NamedInteger( theDataTreeModel, "Packets-Sent", 0 );
		  NetcasterPacketManager theNetcasterPacketManager=
		  		new NetcasterPacketManager( remoteInetAddress, remotePortI );
		  return new NetOutputStream(
		  	netcasterToSenderPacketQueue,
		  	theNetcasterPacketManager,
		  	packetsSentNamedInteger
	      );
	    }
	
	public Multicaster makeMulticaster(
			MulticastSocket theMulticastSocket,
			PacketQueue multicasterToConnectionManagerPacketQueue,
			InetAddress multicastInetAddress
	    )
	  { 
		  LockAndSignal multicasterLockAndSignal= new LockAndSignal();  
		  PacketQueue multicastReceiverToMulticasterPacketQueue= 
		  		new PacketQueue( multicasterLockAndSignal );
			int multicastPortI= PortManager.getDiscoveryPortI();
			
		  return new Multicaster(
		  	multicasterLockAndSignal,
	  		makeNetcasterNetInputStream( multicastReceiverToMulticasterPacketQueue ),
	  		makeNetcasterNetOutputStream( multicastInetAddress, multicastPortI ),
	  		theDataTreeModel,
	  		AppGUIFactory.makeIPAndPort(
	  				multicastInetAddress, multicastPortI 
	  				),
	  		theMulticastSocket,
	      multicasterToConnectionManagerPacketQueue,
	      theUnicasterManager,
	      receiverNetcasterPacketManager
	      ); 
	    }
	
	public UnicasterFactory makeUnicasterFactory(
	    IPAndPort peerIPAndPort
	    )
	  {
			return new UnicasterFactory( 
				this,
				theUnicasterManager,
				peerIPAndPort,
				theDataTreeModel, 
				theShutdowner 
				);
	  	}

  } // class AppGUIFactory.
