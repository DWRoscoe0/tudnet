package allClasses;

import java.net.DatagramSocket;
import java.net.MulticastSocket;

import javax.swing.JFrame;

public class AppGUIFactory {  // For classes with GUI lifetimes.

  /* This is the factory for all classes with AppGUI lifetime.
    It wires together the 2nd level of the application.
    The classes constructed here are mainly the ones 
    needed for presenting a GUI to the user,
    but it also builds other classes such as the ConnectionManager which
    is not part of the GUI, but it has the same lifetime.
    */

  // private injected dependencies.

  // public unconditional singleton storage.
  private final AppGUI theAppGUI;
  private final DataTreeModel theDataTreeModel;
  
	//private unconditional singleton storage.
  private final UnicasterManager theUnicasterManager;
  private final LockAndSignal theGUILockAndSignal;
  private final LockAndSignal senderLockAndSignal;
  private final PacketQueue netcasterToSenderPacketQueue;
  private final PacketQueue unconnectedReceiverToConnectionManagerPacketQueue;

  public AppGUIFactory(  // Factory constructor.
  		AppFactory theAppFactory,
  		Shutdowner theShutdowner,
  		AppInstanceManager theAppInstanceManager
  		)
  	// This constructor builds the unconditional singletons.
    {
  	  senderLockAndSignal= makeLockAndSignal();
	    netcasterToSenderPacketQueue=
	    		makePacketQueue(senderLockAndSignal);
      DataRoot theDataRoot= new DataRoot( );
      MetaFileManager theMetaFileManager= new MetaFileManager( theDataRoot );
      MetaRoot theMetaRoot= new MetaRoot( theDataRoot, theMetaFileManager );
      MetaFileManager.Finisher theMetaFileManagerFinisher= 
	      	new MetaFileManager.Finisher(
	          theMetaFileManager,
	          theMetaRoot
	          );
      theDataTreeModel= 
      		new DataTreeModel( 
		        theDataRoot, 
		        theMetaRoot, 
		        theMetaFileManagerFinisher, 
		        theShutdowner
		        );
      ConnectionFactory theConnectionFactory= new ConnectionFactory( 
      		this, 
      		netcasterToSenderPacketQueue, 
      		theDataTreeModel, 
      		theShutdowner
      		);
	    LockAndSignal cmThreadLockAndSignal= makeLockAndSignal();
	    PacketQueue multicasterToConnectionManagerPacketQueue=
		      makePacketQueue(cmThreadLockAndSignal);
	    unconnectedReceiverToConnectionManagerPacketQueue=
		      makePacketQueue(cmThreadLockAndSignal);
      ConnectionManager theConnectionManager= 
          new ConnectionManager(
            this, // the AppGuiFactory.
            theConnectionFactory,
            theDataTreeModel,
      	    cmThreadLockAndSignal,
      	    multicasterToConnectionManagerPacketQueue,
      	    unconnectedReceiverToConnectionManagerPacketQueue
      	    );
      EpiThread theConnectionManagerEpiThread=
          makeEpiThread( theConnectionManager, "ConnMgr" );
      theUnicasterManager= new UnicasterManager( 
      		theDataTreeModel, 
      		theConnectionFactory, 
      		theConnectionManager
      		);
      SystemsMonitor theSystemsMonitor= new SystemsMonitor(theDataTreeModel);
      EpiThread theCPUMonitorEpiThread=
          makeEpiThread( theSystemsMonitor, "SystemsMonitor" );
      DataNode theInitialRootDataNode=  // Building first legal value.
	        new InfogoraRoot( 
	          new DataNode[] { // An array of all of the child DataNodes.
	            new FileRoots(),
	            new Outline( 0, theDataTreeModel ),
	            theSystemsMonitor,
	            theConnectionManager,
	            new Infinitree( null, 0 )
	            }
	          );
      DagBrowserPanel theDagBrowserPanel= 
      		new DagBrowserPanel(
      			theAppInstanceManager,
		        theDataTreeModel,
		        theDataRoot,
		        theMetaRoot
		        );
      theGUILockAndSignal= makeLockAndSignal();
      AppGUI.GUIDefiner theGUIDefiner=  
      		new AppGUI.GUIDefiner( 
      		  theGUILockAndSignal, 
      		  theAppInstanceManager,
      		  theDagBrowserPanel,
		        this, // GUIDefiner gets to know the factory that made it. 
		        theShutdowner
		        );
      theAppGUI= 
      		new AppGUI( 
		        theConnectionManagerEpiThread,
		        theCPUMonitorEpiThread,
		        theDataTreeModel,
		        theInitialRootDataNode,
		        theGUILockAndSignal,
		        theGUIDefiner,
		        theShutdowner
		        );
      }

  // Unconditional getter methods with null checking.
  // Some might be singletons.

  public AppGUI getAppGUI()
  	{ 
  		return (AppGUI)Globals.fastFailNullCheckObject( theAppGUI );
  	  }
  
  public UnicasterManager getUnicasterManager()
		{ 
	  	return (UnicasterManager)Globals.fastFailNullCheckObject( 
	  			theUnicasterManager
	  			);
	  	}

  public PacketQueue getNetcasterToSenderPacketQueue()
		{ 
	  	return (PacketQueue)Globals.fastFailNullCheckObject( 
	  			netcasterToSenderPacketQueue 
	  			);
		  }

  // Conditional singleton getter methods and storage.
  // None.

  
  // Maker methods which construct something with new-operator each time called.
  // These are for classes with multiple instances in space or time.
  
	public static LockAndSignal makeLockAndSignal()
		{ return 	new LockAndSignal(false); }

  public PacketQueue makePacketQueue(LockAndSignal threadLockAndSignal)
  	{ return new PacketQueue(threadLockAndSignal); }

	public static EpiThread makeEpiThread( Runnable aRunnable, String nameString )
	  { return new EpiThread( aRunnable, nameString ); }
  
	public JFrame makeJFrame( String titleString ) 
    { return new JFrame( titleString ); }
  
	public AppGUI.InstanceCreationRunnable makeInstanceCreationRunnable( 
  		JFrame aJFrame
  		)
	  // This should be a singleton because aJFrame is always the same??
  	{ return theAppGUI.new InstanceCreationRunnable( aJFrame ); }

  public EpiThread makeSenderEpiThread( 
  		DatagramSocket unconnectedDatagramSocket
  		)
	  {
	    Sender theSender= makeSender( unconnectedDatagramSocket );
	    EpiThread theSenderEpiThread= makeEpiThread( theSender, "Sender" );
	    return theSenderEpiThread;
	    }
  
	private Sender makeSender(
			DatagramSocket unconnectedDatagramSocket 
			)
	  {
			Globals.fastFailNullCheckObject(unconnectedDatagramSocket);
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
					makeEpiThread( theUnconnectedReceiver, "UcRcvr" );
	    return theUnconnectedReceiverEpiThread;
	    }
	  
  private UnconnectedReceiver makeUnconnectedReceiver(
  		DatagramSocket unconnectedDatagramSocket
  		)
	  {
			Globals.fastFailNullCheckObject(unconnectedDatagramSocket);
			return new UnconnectedReceiver( 
	          unconnectedDatagramSocket,
	          unconnectedReceiverToConnectionManagerPacketQueue,
	          theUnicasterManager
	          );
	    }
  
	public static EpiThread makeMulticastReceiverEpiThread(
			PacketQueue multicastReceiverToMulticasterPacketQueue,
			MulticastSocket theMulticastSocket
			)
	  {
		
			Multicaster.MulticastReceiver theMulticastReceiver=  // Constructing runnable.
		    new Multicaster.MulticastReceiver( 
			   		multicastReceiverToMulticasterPacketQueue,
			   		theMulticastSocket
			  		);
		  EpiThread theMulticastReceiverEpiThread= // Constructing thread.
		  	new EpiThread( theMulticastReceiver, "McRcvr" );
		  return theMulticastReceiverEpiThread;
	    }

  } // class AppGUIFactory.
