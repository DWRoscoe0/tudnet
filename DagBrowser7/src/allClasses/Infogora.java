package allClasses;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import javax.swing.JFrame;

import static allClasses.Globals.appLogger;  // For appLogger;


/* This file is the root of this application.  
  If you want to understand the application then
  this is where you should start reading.  
  This file contains 3 classes:
 
  * Infogora: This is the class that contains the main(..) method
    which is the entry point of the application.

	* AppFactory: This is the factory for all classes with app lifetime.
	  It wires together the first level of the app.

	* AppGUIFactory: This is the factory for all classes with 
	  app GUI lifetime.  It wires together the second level of the app.

  * ConnectionsFactory: This is the factory for all classes with
    lifetimes of the ConnectionManager's lifetime or shorter.

  The factories above may not be the only factories in the app,
  but they are the top levels.  Factories serve 2 purposes:

  * They contain, or eventually will contain,  all the new-operators, 
    except for 2 uses in the top level Infogora class.
    This makes unit testing easier.

  * Their code shows how classes relate to each other in the app
    by showing all dependency injections, usually with
    constructor injection, but occasionally with setter injection.

  */


class ConnectionsFactory {

  // This is the factory for classes with connection lifetimes.

  // Injected unconditional singleton storage.
  private DataTreeModel theDataTreeModel;

  public ConnectionsFactory(   // Factory constructor. 
  		DataTreeModel theDataTreeModel 
  		)
  	// This constructor defines the unconditional singletons.
    {
      this.theDataTreeModel= theDataTreeModel; // Save injected singleton.
      }

  // Unconditional singleton getters.
  // None.

  // Conditional singleton getters and storage.
  // None.

  // Maker methods.  These construct using new operator each time called.
  public Multicaster makeMulticaster(
  		MulticastSocket theMulticastSocket,
      InputQueue<SockPacket> sendQueueOfSockPackets,
      InputQueue<SockPacket> receiveQueueOfSockPackets,
      DatagramSocket unconnectedDatagramSocket,
	  	UnicasterManager theUnicasterManager
      )
    throws IOException
    { 
  	  return new Multicaster(
  	    theDataTreeModel,
	  		new InetSocketAddress(
	  				InetAddress.getByName("239.255.0.0"),
	  				PortManager.getDiscoveryPortI()
	  				),
	  		theMulticastSocket,
        sendQueueOfSockPackets,
        receiveQueueOfSockPackets,
        unconnectedDatagramSocket,
		  	theUnicasterManager
        ); 
      }
  public Unicaster makeUnicaster(
      InetSocketAddress peerInetSocketAddress,
      PacketQueue sendPacketQueue,
      JobQueue<Unicaster> cmJobQueueOfUnicasters,
      DatagramSocket unconnectedDatagramSocket,
      ConnectionManager theConnectionManager,
      Shutdowner theShutdowner
      )
    {
      return new Unicaster(
        peerInetSocketAddress,
        sendPacketQueue,
        cmJobQueueOfUnicasters,
        unconnectedDatagramSocket,
        theDataTreeModel,
        theConnectionManager,
        theShutdowner
        );
      }
  public NetCasterValue makeUnicasterValue(
      InetSocketAddress peerInetSocketAddress,
      PacketQueue sendPacketQueue,
      JobQueue<Unicaster> cmJobQueueOfUnicasters,
      DatagramSocket unconnectedDatagramSocket,
      ConnectionManager theConnectionManager,
      Shutdowner theShutdowner
      )
    {
      Unicaster theUnicaster= makeUnicaster(
        peerInetSocketAddress,
        sendPacketQueue,
        cmJobQueueOfUnicasters,
        unconnectedDatagramSocket,
        theConnectionManager,
        theShutdowner
        );
      return new NetCasterValue(
        peerInetSocketAddress,
        theUnicaster
        );
      }

  } // class ConnectionsFactory.


class AppGUIFactory {  // For GUI class lifetimes.

  /* This is the factory for all classes with AppGUI lifetime.
    It wires together the 2nd level of the application.
    The classes constructed here are mainly the ones 
    needed for presenting a GUI to the user,
    but it also builds the ConnectionManager which
    is not part of the GUI, but it has the same lifetime.
    */

  // Unconditional singleton storage.
  AppGUIManager theAppGUIManager;
  LockAndSignal theGUILockAndSignal;
  AppGUIManager.GUIDefiner theGUIDefiner;

  public AppGUIFactory(  // Factory constructor.
      Thread mainThread, 
      AppInstanceManager theAppInstanceManager,
      Shutdowner theShutdowner
      )
  	// This constructor builds the unconditional singletons.
    {
      DataRoot theDataRoot= 
      		new DataRoot( );
      MetaFileManager theMetaFileManager= 
      		new MetaFileManager( theDataRoot );
      MetaRoot theMetaRoot= 
      		new MetaRoot( theDataRoot, theMetaFileManager );
      MetaFileManager.Finisher theMetaFileManagerFinisher= 
	      	new MetaFileManager.Finisher(
	          theMetaFileManager,
	          theMetaRoot
	          );
      DataTreeModel theDataTreeModel= 
      		new DataTreeModel( 
		        theDataRoot, 
		        theMetaRoot, 
		        theMetaFileManagerFinisher, 
		        theShutdowner
		        );
      ConnectionsFactory theConnectionsFactory= 
      		new ConnectionsFactory( theDataTreeModel );
      UnicasterManager theUnicasterManager= 
      		new UnicasterManager( );
      ConnectionManager theConnectionManager= 
          new ConnectionManager( 
            theConnectionsFactory,
            theDataTreeModel,
            theUnicasterManager,
            theShutdowner
            );
      EpiThread theConnectionManagerEpiThread=
          makeEpiThread( theConnectionManager, "ConnMgr" );
      SystemMonitor theSystemMonitor= new SystemMonitor(theDataTreeModel);
      EpiThread theCPUMonitorEpiThread=
          makeEpiThread( theSystemMonitor, "SystemMonitor" );
      DataNode theInitialRootDataNode=  // Building first legal value.
	        new InfogoraRoot( 
	          new DataNode[] { // ...an array of all child DataNodes.
	            new FileRoots(),
	            new Outline( 0, theDataTreeModel ),
	            theSystemMonitor,
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
      theGUILockAndSignal= 
      		new LockAndSignal(false);
      theGUIDefiner=  
      		new AppGUIManager.GUIDefiner( 
      		  theGUILockAndSignal, 
      		  theAppInstanceManager,
      		  theDagBrowserPanel,
		        this, // GUIDefiner gets to know the factory that made it. 
		        theShutdowner
		        );
      theAppGUIManager= 
      		new AppGUIManager( 
		        theConnectionManagerEpiThread,
		        theCPUMonitorEpiThread,
		        theDataTreeModel,
		        theInitialRootDataNode,
		        theGUILockAndSignal,
		        theGUIDefiner,
		        theShutdowner
		        );
      }

  // Unconditional singleton getter methods.
  public AppGUIManager getAppGUIManager()
    { return theAppGUIManager; }

  // Conditional singleton getter methods and storage.
  // None.

  // Maker methods.  These construct using new operator each time called.
  public EpiThread makeEpiThread( Runnable aRunnable, String nameString )
	  { return new EpiThread( aRunnable, nameString ); }
  public JFrame makeJFrame( String titleString ) 
    { return new JFrame( titleString ); }
  public AppGUIManager.InstanceCreationRunnable makeInstanceCreationRunnable( 
  		JFrame aJFrame)
  	{ return theAppGUIManager.new InstanceCreationRunnable( aJFrame ); }

  } // class AppGUIFactory.


class AppFactory {  // For App class lifetimes.

  /* This is the factory for all classes with App lifetime.
    It wires together the top level of the application.
    The classes constructed here are mainly the ones 
    needed before a GUI is presented to the user.
    In some cases the GUI is not presented, but if it is
    then its factory is constructed with getAppGUIFactory().
    */

  // Unconditional singleton storage.
  private Shutdowner theShutdowner;
  private AppInstanceManager theAppInstanceManager;
  private App theApp;

  public AppFactory( String[] argStrings )  // Factory constructor.
  	// This constructor builds the unconditional singletons.
    {
      theShutdowner= new Shutdowner();
      theAppInstanceManager=
        new AppInstanceManager(argStrings,theShutdowner);
      theApp= new App(
        theShutdowner,
        theAppInstanceManager,
        this // The App gets to know the factory that made it. 
        );
      }

  // Unconditional singleton getters.
  public App getApp() 
    { return theApp; }

  // Conditional singleton getters and storage.
  AppGUIFactory theAppGUIFactory= null;
  public AppGUIFactory getAppGUIFactory()
	  {
	    if (theAppGUIFactory == null) 
	      theAppGUIFactory= new AppGUIFactory(
	          Thread.currentThread(), // Our Thread. 
	          theAppInstanceManager,
	          theShutdowner
	      		);
	    return theAppGUIFactory;
	    }

  } // AppFactory


class Infogora  // The root of this app.

	/* This class contains 2 methods:

	  * The main(..) method, which is the app's entry point.

	  * setDefaultExceptionHandlerV(), which sets the app's 
	    default handler for uncaught exceptions.

		Both these methods use the new-operator.
		Except for factories, these should [eventually] be 
		the only places where the new-operator is used.
		This can make unit testing much easier.
	  */

	{ // Infogora

	  private static void setDefaultExceptionHandlerV()
      /* This helper method for the main(..) method
        sets the default handler for uncaught exceptions.
        The purpose of this is to guarantee that every Exception
        will be handled by this application and
        at least produce a log message.
        The handler sends a message about the exception to
        both the log file and to the console.
        */
      {
        Thread.setDefaultUncaughtExceptionHandler(
          new Thread.UncaughtExceptionHandler() {
            @Override 
            public void uncaughtException(Thread t, Throwable e) {
              System.out.println( "Uncaught Exception, "+t.getName()+", "+e);

              appLogger.error( "Uncaught Exception: "+e );

              StringWriter aStringWriter= new StringWriter();
              PrintWriter aPrintWriter= new PrintWriter(aStringWriter);
              e.printStackTrace(aPrintWriter);
              appLogger.info( "Stack trace: "+aStringWriter.toString() );
              }
            }
          );

        //throw new NullPointerException(); // Uncomment to test handler.
        }

	  public static void main(String[] argStrings)
			/* This method is the app's entry point.  It does the following:
	
			  * It sets a default Exception handler.
			  * It creates the AppFactory object.
			  * It uses the AppFactory to create the App object.
			  * It calls the App object's runV() method.
	
				See the AppFactory for information about 
				this app's high-level structure.
			  */
      { // main(..)
	      appLogger.info("main Infogora main() beginning.");

	      setDefaultExceptionHandlerV(); // Preparing for exceptions 
	        // before doing anything else.
	
	      AppFactory theAppFactory=  // Constructing AppFactory.
	        new AppFactory(argStrings);
	      App theApp=  // Getting the App from the factory.
      		theAppFactory.getApp();
	      theApp.runV();  // Running the app until it finishes.
	      
	      // At this point, this thread should be the only non-daemon running.
	      // When it ends, it should trigger a JVM shutdown,
	      // unless it was triggered already, and terminate the app.
	      // Unfortunately the app doesn't terminate, so we call exit(0).

	      appLogger.info("Infogora.main() calling exit(0).");
	      System.exit(0); // Will killing any remaining unknown threads running??
	      } // main(..)
	
		} // Infogora

// End of file.
