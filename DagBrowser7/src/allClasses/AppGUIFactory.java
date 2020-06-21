package allClasses;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Timer;

import javax.swing.JFrame;

public class AppGUIFactory {  // For classes with GUI lifetimes.

  /* This is the factory for all classes with GUI scope.
    Most have lifetimes close to the AppGUI lifetime, but some are shorter.
    The app has a maximum of one instance of this factory.

    This factory wires together the 2nd level of the application.
    The classes constructed here are mainly the ones 
    used to present a GUI (Graphical User Interface) to the user,
    and must of the GUI content.

    There is only one AppGUI. 
    All of its code could have been put in the AppFactory,
    but a GUI is not always needed,
    so it made sense to divide the code between the App and GUI factory classes.

    */

  // Injected dependencies that need saving for later.
  private final Shutdowner theShutdowner;
  private final PortManager thePortManager;

	// Other objects that will be needed later.

  // Saved while constructing singletons.
  private TextStreams theTextStreams;
  private TextStreams2 theTextStreams2;
  private final UnicasterManager theUnicasterManager;

  ///org Saved after constructing singletons.  These could be while-constructing variables.
  private final LockAndSignal senderLockAndSignal;
  private final NetcasterQueue netcasterToSenderNetcasterQueue;
  private final NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue;
  private final AppGUI theAppGUI;
  private final NetcasterPacketManager receiverNetcasterPacketManager;
	private final Timer theTimer;
  private final NamedLong multicasterFixedTimeOutMsNamedLong;
  private final Persistent thePersistent;
  private NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes;
  
  public AppGUIFactory(  // Factory constructor.
  	  Persistent thePersistent,
  	  PortManager thePortManager,
  		Shutdowner theShutdowner,
  		AppInstanceManager theAppInstanceManager,
  		TCPCopier theTCPCopier
  		)
  	/* This method builds all objects that are, or comprise, unconditional singletons.
  	   
       ///org Note, no non-static maker methods are called from here, otherwise 
         it could go undetected by the compiler and result in NullPointerExceptions.
         Find a better way to organize this factory so that the compiler will detect
         this type of error. 
       */
    {

      // Save injected dependencies for immediate use by factory methods.
      this.theShutdowner= theShutdowner;
      this.thePortManager= thePortManager;
      this.thePersistent= thePersistent;

      LockAndSignal senderLockAndSignal= new LockAndSignal();
  	  NetcasterQueue netcasterToSenderNetcasterQueue= 
  	  		new NetcasterQueue( senderLockAndSignal, Config.QUEUE_SIZE );
      DataRoot theDataRoot= new DataRoot();
      MetaFileManager theMetaFileManager= new MetaFileManager( theDataRoot );
      MetaRoot theMetaRoot= new MetaRoot(theDataRoot, theMetaFileManager);
      MetaFileManager.Finisher theMetaFileManagerFinisher= 
      	new MetaFileManager.Finisher( theMetaFileManager, theMetaRoot );
      DataTreeModel theDataTreeModel= new DataTreeModel( 
        theDataRoot, theMetaRoot, theMetaFileManagerFinisher, theShutdowner 
        );
      theUnicasterManager= 
          new UnicasterManager( this, thePersistent, theTCPCopier );
	    LockAndSignal cmThreadLockAndSignal= new LockAndSignal();
      NotifyingQueue<String> toConnectionManagerNotifyingQueueOfStrings=
          new NotifyingQueue<String>(cmThreadLockAndSignal, Config.QUEUE_SIZE);
      NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes=
          new NotifyingQueue<MapEpiNode>(cmThreadLockAndSignal, Config.QUEUE_SIZE);
	    NetcasterQueue multicasterToConnectionManagerNetcasterQueue=
	      new NetcasterQueue(cmThreadLockAndSignal, Config.QUEUE_SIZE);
      NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue=
          new NetcasterQueue(cmThreadLockAndSignal, Config.QUEUE_SIZE);
      theTextStreams= new TextStreams(
          "Text-Replication-Streams",this,thePersistent,theUnicasterManager);
      theTextStreams2= new TextStreams2(
          "Text-Streams",this,thePersistent);
	    ConnectionManager theConnectionManager= new ConnectionManager(
        this, // the AppGuiFactory.
    	  thePersistent,
    	  thePortManager,
        theUnicasterManager,
  	    cmThreadLockAndSignal,
  	    multicasterToConnectionManagerNetcasterQueue,
  	    unconnectedReceiverToConnectionManagerNetcasterQueue,
  	    toConnectionManagerNotifyingQueueOfStrings,
        toConnectionManagerNotifyingQueueOfMapEpiNodes,
        theTextStreams,
        theTextStreams2
  	    );
      EpiThread theConnectionManagerEpiThread=
        AppGUIFactory.makeEpiThread( theConnectionManager, "ConnMgr" );
      SystemsMonitor theSystemsMonitor= 
        new SystemsMonitor(toConnectionManagerNotifyingQueueOfStrings);
      EpiThread theCPUMonitorEpiThread=
        AppGUIFactory.makeEpiThread( theSystemsMonitor, "SystemsMonitor" );
      DataNode testCenterDataNode= new NamedList(
          "Test-Center",
          theTextStreams2,
          theTextStreams,
          new Infinitree( null, 0 )
          );
      DataNode theInitialRootDataNode= new InfogoraRoot( // Building DataNode tree.
        new FileRoots(),
        new Outline( 0 ),
        theSystemsMonitor,
        theConnectionManager,
        testCenterDataNode
        );
  		TracingEventQueueMonitor theTracingEventQueueMonitor=
  				new TracingEventQueueMonitor(
	  	  				TracingEventQueueMonitor.LIMIT
 		  	  		  //  500
	  	  				); 
  		TracingEventQueue theTracingEventQueue=        	  
    	    new TracingEventQueue(
    	    		theTracingEventQueueMonitor
		  	  		  );
    	BackgroundEventQueue theBackgroundEventQueue=
    			new BackgroundEventQueue();
      DagBrowserPanel theDagBrowserPanel= new DagBrowserPanel(
    			theAppInstanceManager, 
    			theDataTreeModel, 
    			theDataRoot, 
    			theMetaRoot,
    			theBackgroundEventQueue
	        );
      GUIManager theGUIBuilderStarter= new GUIManager( 
  		  theAppInstanceManager,
  		  theDagBrowserPanel,
        this, // GUIBuilderStarter gets to know the factory that made it. 
        theShutdowner,
    		theTracingEventQueue,
      	theBackgroundEventQueue
        );
      AppGUI theAppGUI= new AppGUI( 
        theConnectionManagerEpiThread,
        theCPUMonitorEpiThread,
        theDataTreeModel,
        theInitialRootDataNode,
        theGUIBuilderStarter,
        theShutdowner,
        theTCPCopier
        );

      receiverNetcasterPacketManager=  //? use local? 
      		new NetcasterPacketManager( (IPAndPort)null );

  		Timer theTimer= new Timer(  // Single timer for entire app.
  				"AppTimer", true
  				);

      NamedLong multicasterTimeOutMsNamedLong= 
					new NamedLong( 
							"Retransmit-Delay(Ms)",
							Config.initialRoundTripTime100MsL * 2
							);

      // Note, some non-injected instance variables were saved when calculated.
      // Save in instance variables other non-injected objects that are needed later.
      this.senderLockAndSignal= senderLockAndSignal;
      this.netcasterToSenderNetcasterQueue= netcasterToSenderNetcasterQueue;
      this.unconnectedReceiverToConnectionManagerNetcasterQueue=
      		unconnectedReceiverToConnectionManagerNetcasterQueue;
      this.theAppGUI= theAppGUI;
  		this.theTimer= theTimer;
  		this.multicasterFixedTimeOutMsNamedLong= 
  				multicasterTimeOutMsNamedLong;
      this.toConnectionManagerNotifyingQueueOfMapEpiNodes=
          toConnectionManagerNotifyingQueueOfMapEpiNodes;
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

  
  /* Maker methods which construct something with the new-operator each time called.
    These are for classes for which there are multiple instances in space or time.
    They have lifetimes that are shorter than AppGUI.
    
    ///org Maybe they should be put into their own factory?
      This would eliminate the need to pass "this" AppGUIFactory as a parameter.
      
    ///opt fastFailNullCheckT(..) might no longer be needed.
     */

  public TextStream makeTextSteam(String thePeerIdentityString)
    { 
      return new TextStream(
        thePeerIdentityString,thePersistent,theTextStreams);
      }

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
	      netcasterToSenderNetcasterQueue,
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
	          unconnectedReceiverToConnectionManagerNetcasterQueue,
	          theUnicasterManager,
	          receiverNetcasterPacketManager
	          );
	    }
  
	public static EpiThread makeMulticastReceiverEpiThread(
			NetcasterQueue multicastReceiverToMulticasterNetcasterQueue,
			MulticastSocket theMulticastSocket,
   		NetcasterPacketManager receiverNetcasterPacketManager
			)
	  {
		
			Multicaster.MulticastReceiver theMulticastReceiver=  // Constructing runnable.
		    new Multicaster.MulticastReceiver( 
			   		multicastReceiverToMulticasterNetcasterQueue,
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

	public NetcasterInputStream makeNetcasterInputStream(
			NetcasterQueue receiverToNetcasterNetcasterQueue,
			char delimiterChar
			)
	  {
			NamedLong packetsReceivedNamedLong=  
					new NamedLong( 
							"Incoming-Packets-Received", 0 
							);
	  	return new NetcasterInputStream(
	  	  receiverToNetcasterNetcasterQueue, 
	  	  packetsReceivedNamedLong,
	  	  delimiterChar
				);
	  	}

	public NetcasterOutputStream makeNetcasterOutputStream(
			NetcasterPacketManager theNetcasterPacketManager
		  )
	  {
		  NamedLong packetsSentNamedLong= 
					new NamedLong( 
							"Outgoing-Packets-Sent", 0 
							);
		  return new NetcasterOutputStream(
		  	netcasterToSenderNetcasterQueue,
		  	theNetcasterPacketManager,
		  	packetsSentNamedLong,
	  		theTimer,
	  		Config.delimiterC
	      );
	    }
	
	public Multicaster makeMulticaster(
			MulticastSocket theMulticastSocket,
			NetcasterQueue multicasterToConnectionManagerNetcasterQueue,
			InetAddress multicastInetAddress
	    )
	  { 
		  LockAndSignal multicasterLockAndSignal= new LockAndSignal();  
		  NetcasterQueue multicastReceiverToMulticasterNetcasterQueue= 
		  		new NetcasterQueue(multicasterLockAndSignal, Config.QUEUE_SIZE);
			int multicastPortI= thePortManager.getMulticastPortI();
			IPAndPort theIPAndPort= AppGUIFactory.makeIPAndPort(
  				multicastInetAddress, multicastPortI 
  				);
		  NetcasterPacketManager theNetcasterPacketManager=
		  		new NetcasterPacketManager( theIPAndPort );

		  return new Multicaster(
		  	multicasterLockAndSignal,
	  		makeNetcasterInputStream( 
	  				multicastReceiverToMulticasterNetcasterQueue, 
	  	  		Config.delimiterC
	  				),
	  		makeNetcasterOutputStream( theNetcasterPacketManager ),
        theShutdowner,
	  		theIPAndPort,
	  		theMulticastSocket,
	      multicasterToConnectionManagerNetcasterQueue,
	      receiverNetcasterPacketManager,
	      multicasterFixedTimeOutMsNamedLong
	      ); 
	    }
	
	public UnicasterFactory makeUnicasterFactory(
	    IPAndPort peerIPAndPort, String unicasterIdString, TCPCopier theTCPCopier
	    )
	  {
			return new UnicasterFactory( 
				this,
				theUnicasterManager,
				peerIPAndPort,
	      unicasterIdString,
				theTCPCopier,
				theShutdowner, 
				Config.QUEUE_SIZE,
	  		theTimer,
	  		thePersistent,
        toConnectionManagerNotifyingQueueOfMapEpiNodes
				);
	  	}

  } // class AppGUIFactory.
