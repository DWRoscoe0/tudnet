package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.swing.JFrame;

import allClasses.epinode.MapEpiNode;
import allClasses.ifile.EpiFileRoots;
import allClasses.ifile.IRoots;
import allClasses.javafx.JavaFXGUI;
import allClasses.javafx.Selections;

public class AppFactory {  // For App class lifetimes.

  /* This is the factory for all classes with App lifetime.
    The app has exactly one instance of this factory.
    It includes classes that are part of the BUI.
    This class wires together the top level of the application.
    The app has a maximum of one instance of this factory.

    Originally the GUI objects were created 
    by a different factory, AppGUIFactory, because the GUI isn't always needed.
    Eventually those classes were moved to here.

    ScheduledThreadPoolExecutor is used in this class.
    Unfortunately it appears that ScheduledThreadPoolExecutor disables
    some functionality of the ThreadPoolExecutor for controlling  
    the thread pool.  In the ScheduledThreadPoolExecutor,
    the pool size, the so-called core size, is fixed.
    It can not expand or contract as needed.

    ///enh It might be necessary to create a new class that uses 
    the ThreadPoolExecutor configured for a widely variable number of threads
    to provide a ScheduledThreadPoolExecutor-like class 
    that can provide a potentially large number of timer threads.
    */

  // Injected dependencies that will still be needed after construction.
  private final Persistent thePersistent;

	// Other objects that will be needed later.
	private PortManager thePortManager;
  private final Shutdowner theShutdowner;
  private final App theApp;
  private final TCPCopier theTCPCopier;
  private final AppGUI theAppGUI;
  
  // Variables moved from AppGUIFactory.

  // Saved while constructing singletons.
  private TextStreams2 theTextStreams2;
  private final UnicasterManager theUnicasterManager;

  ///org Saved after constructing singletons.  
  /// These could be while-constructing variables.
  private final LockAndSignal senderLockAndSignal;
  private final NetcasterQueue netcasterToSenderNetcasterQueue;
  private final NetcasterQueue 
    unconnectedReceiverToConnectionManagerNetcasterQueue;
  private final NetcasterPacketManager receiverNetcasterPacketManager;
  private final ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;
  private final NamedLong multicasterFixedTimeOutMsNamedLong;
  private final NotifyingQueue<MapEpiNode> 
    toConnectionManagerNotifyingQueueOfMapEpiNodes;
  private final ConnectionManager theConnectionManager;

  public AppFactory(   // Factory constructor.
      CommandArgs inCommandArgs, Persistent inPersistent)
    {
      theAppLog.info("AppFactory(.) entry.");

      this.thePersistent= inPersistent;

  		thePortManager= new PortManager( thePersistent );
  		theShutdowner= new Shutdowner();
  		AppInstanceManager newAppInstanceManager= new AppInstanceManager(
  		    inCommandArgs, theShutdowner, thePortManager
      		);
  		theTCPCopier= new TCPCopier( "TCPCopier", thePersistent, thePortManager );
  		App newApp= new App(
        this, // The App gets to know the factory that made it. 
        thePersistent,
        theShutdowner,
        newAppInstanceManager,
        theTCPCopier
        );


      // Code moved from AppGUIFactory.
      LockAndSignal senderLockAndSignal= new LockAndSignal();
      NetcasterQueue netcasterToSenderNetcasterQueue= 
          new NetcasterQueue( senderLockAndSignal, Config.QUEUE_SIZE, "sndp");
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
          new NotifyingQueue<String>(
              cmThreadLockAndSignal, Config.QUEUE_SIZE,"cms");
      NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes=
          new NotifyingQueue<MapEpiNode>(
              cmThreadLockAndSignal, Config.QUEUE_SIZE,"cmn");
      NetcasterQueue multicasterToConnectionManagerNetcasterQueue=
        new NetcasterQueue(cmThreadLockAndSignal, Config.QUEUE_SIZE, "mccmp");
      NetcasterQueue unconnectedReceiverToConnectionManagerNetcasterQueue=
          new NetcasterQueue(cmThreadLockAndSignal, Config.QUEUE_SIZE, "urcmp");
      ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor; {
        // Single ScheduledThreadPoolExecutor for many app threads and timers.
        theScheduledThreadPoolExecutor= new ScheduledThreadPoolExecutor(
            5, // Fixed thread pool size.
            (theRunnable,theThreadPoolExecutor) -> { theAppLog.error(
                "ScheduledThreadPoolExecutor rejected execution."); }
            ); // Minimum of 1 thread,
        }
      theTextStreams2= new TextStreams2(
          "Replication-Text-Streams",this,thePersistent,theUnicasterManager);
      theConnectionManager= new ConnectionManager(
        this, // the AppGuiFactory.
        thePersistent,
        thePortManager,
        theUnicasterManager,
        cmThreadLockAndSignal,
        multicasterToConnectionManagerNetcasterQueue,
        unconnectedReceiverToConnectionManagerNetcasterQueue,
        toConnectionManagerNotifyingQueueOfStrings,
        toConnectionManagerNotifyingQueueOfMapEpiNodes,
        theTextStreams2
        );
      EpiThread theConnectionManagerEpiThread=
        makeEpiThread( theConnectionManager, "ConnMgr" );
      SystemsMonitor theSystemsMonitor= 
        new SystemsMonitor(toConnectionManagerNotifyingQueueOfStrings);
      EpiThread theCPUMonitorEpiThread=
        makeEpiThread( theSystemsMonitor, "SystemsMonitor" );
      InstallerBuilder theInstallerBuilder= new InstallerBuilder( 
          "Installer-Builder",thePersistent,theScheduledThreadPoolExecutor);
      EpiFileRoots theEpiFileRoots= new EpiFileRoots();
      VolumeChecker theVolumeChecker = new VolumeChecker (
          "Volume-Checker",thePersistent,theScheduledThreadPoolExecutor);
      VolumeDetector theVolumeDetector= new VolumeDetector(
          "Volume-Detector",thePersistent,theScheduledThreadPoolExecutor);
      ConsoleBase theConsoleBase= new ConsoleBase(
          "Console-Base",thePersistent,theScheduledThreadPoolExecutor);
      DataNode testCenterDataNode= new NamedList(
          "Test-Center",
          theVolumeChecker,
          theVolumeDetector,
          theConsoleBase,
          theInstallerBuilder,
          theEpiFileRoots,
          theTextStreams2,
          new Infinitree( null, 0 )
          );
      IRoots theIRoot= new IRoots(); 
      DataNode theRootDataNode= new InfogoraRoot( // Building DataNode tree.
        theIRoot,
        new Outline( 0 ),
        theSystemsMonitor,
        theConnectionManager,
        testCenterDataNode
        );

      receiverNetcasterPacketManager=  //? use local? 
          new NetcasterPacketManager( (IPAndPort)null );

      NamedLong multicasterTimeOutMsNamedLong= 
          new NamedLong( 
              "Retransmit-Delay(Ms)",
              Config.initialRoundTripTime100MsL * 2
              );

      // Internals code is above this point.
      // GUI code is below this point.

      TracingEventQueueMonitor theTracingEventQueueMonitor=
          new TracingEventQueueMonitor(
                TracingEventQueueMonitor.LIMIT
                //  500
                ); 
      TracingEventQueue theTracingEventQueue=           
          new TracingEventQueue(
              theTracingEventQueueMonitor
                );

      DagBrowserPanel theDagBrowserPanel= new DagBrowserPanel(
          newAppInstanceManager,
          theDataTreeModel, 
          theDataRoot, 
          theMetaRoot
          );
      Selections theSelections= new Selections(
          thePersistent,
          theDataRoot
          );
      JavaFXGUI theJavaFXGUI= JavaFXGUI.initializeJavaFXGUI(
          theRootDataNode,
          theShutdowner,
          thePersistent,
          theDataRoot,
          theSelections
          );
      GUIManager theGUIManager= new GUIManager( 
        newAppInstanceManager,
        theDagBrowserPanel,
        this, // GUIBuilderStarter gets to know the factory that made it. 
        theShutdowner,
        theTracingEventQueue,
        theJavaFXGUI
        );
      AppGUI newAppGUI= new AppGUI( 
        theConnectionManagerEpiThread,
        theCPUMonitorEpiThread,
        theDataTreeModel,
        theRootDataNode,
        theGUIManager,
        theShutdowner,
        theTCPCopier,
        theScheduledThreadPoolExecutor,
        newAppInstanceManager,
        theConnectionManager
        );
  		
  		// Save in instance variables injected objects that are needed later.
	    // None.  Already saved.

  	  // Save new objects that will be needed later 
  		// from local variables to instance variables. 
      //% this.theShutdowner= theShutdowner;
      this.theAppGUI= newAppGUI;
      this.theApp= newApp;

      // Following were moved AppGUIFactory.
      
      // Note, some objects constructed above were saved immediately to
      // non-injected instance variables.
      // Other objects constructed above were saved to local variables,
      // and will be saved to instance variables now.
      // I think this was done to better detect use of
      // improperly initialized variables.
      this.senderLockAndSignal= senderLockAndSignal;
      this.netcasterToSenderNetcasterQueue= netcasterToSenderNetcasterQueue;
      this.unconnectedReceiverToConnectionManagerNetcasterQueue=
          unconnectedReceiverToConnectionManagerNetcasterQueue;
      this.theScheduledThreadPoolExecutor= theScheduledThreadPoolExecutor;
      this.multicasterFixedTimeOutMsNamedLong= 
          multicasterTimeOutMsNamedLong;
      this.toConnectionManagerNotifyingQueueOfMapEpiNodes=
          toConnectionManagerNotifyingQueueOfMapEpiNodes;
      
      theAppLog.info("AppFactory(.) exit.");
      }

  // Unconditional singleton getter methods with null checking.
  // There shouldn't be more than one.
  // More than one indicates cyclic dependencies, which are undesirable.

  public AppGUI getAppGUI() // This is the main getter of this factory.
    { 
      return Nulls.fastFailNullCheckT( theAppGUI );
      }

  // Unconditional singleton getters, allowed because it's for the top level.
  public App getApp() 
    { return theApp; }

  /* Maker methods which construct a new object each time they are called.
    [ These were imported from AppGUIFactory. ]

    These are for classes for which there are 
    multiple instances in space or time.
    They have lifetimes that are shorter than AppGUI.
    
    ///org Maybe they should be put into their own factory?
      This would eliminate the need to pass "this" AppGUIFactory as a parameter.
      
    ///opt fastFailNullCheckT(..) might no longer be needed.
     */

  public TextStream2 makeTextSteam2(String theRootIdString)
    { 
      return new TextStream2(
        theRootIdString,thePersistent,theTextStreams2);
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
          makeEpiThread( theSender, "Sender" );
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
          makeEpiThread( theUnconnectedReceiver, "UcRcvr" );
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
              "Local-Packets-Received", 0
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
              "Local-Packets-Sent", 0
              );
      return new NetcasterOutputStream(
        netcasterToSenderNetcasterQueue,
        theNetcasterPacketManager,
        packetsSentNamedLong,
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
        new NetcasterQueue(multicasterLockAndSignal, Config.QUEUE_SIZE, "mrmc");
      int multicastPortI= thePortManager.getMulticastPortI();
      IPAndPort theIPAndPort= makeIPAndPort(
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
        theScheduledThreadPoolExecutor,
        thePersistent,
        toConnectionManagerNotifyingQueueOfMapEpiNodes,
        theConnectionManager
        );
      }
  
  } // AppFactory
