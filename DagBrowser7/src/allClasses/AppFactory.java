package allClasses;

public class AppFactory {  // For App class lifetimes.

  /* This is the factory for all classes with App lifetime.
    The app has exactly one instance of this factory.
    
    This class wires together the top level of the application.
    The classes constructed here are mainly the ones 
    needed before a GUI is presented to the user.
    In some cases the GUI is not presented, but if it is
    then its factory is constructed by getAppGUIFactory().
    */

  // Injected dependencies that need saving for later.
	// None.

	// Other objects that will be needed later.
  private final Persistent thePersistent;
  private final Shutdowner theShutdowner;
  private final AppInstanceManager theAppInstanceManager;
  private final App theApp;
  private final TCPCopier.TCPServer theTCPServer;
  private final TCPCopier.TCPClient theTCPClient;
  
  // Storage for conditional (lazy evaluation) singletons.
  private AppGUIFactory theAppGUIFactory= null;

  public AppFactory( String[] argStrings )  // Factory constructor.
    {
  	  thePersistent= new Persistent();
  		theShutdowner= new Shutdowner();
  		AppInstanceManager theAppInstanceManager= new AppInstanceManager(
      		argStrings, theShutdowner
      		);
  		theTCPServer= new TCPCopier.TCPServer( "TCPServer" );
  		theTCPClient= new TCPCopier.TCPClient( "TCPClient" );
  		App theApp= new App(
        this, // The App gets to know the factory that made it. 
        thePersistent,
        theShutdowner,
        theAppInstanceManager,
        theTCPServer,
        theTCPClient
        );

      // Save in instance variables injected objects that are needed later.
	    // None.

  	  // Save in instance variables other objects that are needed later.
      //% this.theShutdowner= theShutdowner;
      this.theAppInstanceManager= theAppInstanceManager;
      this.theApp= theApp;
      }

  // Unconditional singleton getters, allowed because it's for the top level.
  public App getApp() 
    { return theApp; }

  // Conditional (lazy evaluation) singleton getter-constructors.

  public AppGUIFactory lazyGetAppGUIFactory()
    // This makes and gets the factory for the next smaller scope for this app.
	  {
	    if (theAppGUIFactory == null) // Constructing lazily and only one time.
	      theAppGUIFactory= new AppGUIFactory(
	      		this, // AppGUIFactory gets to know this, its factory.
	      		thePersistent,
	      		theShutdowner,
	      		theAppInstanceManager
	      		);
	    return theAppGUIFactory;
	    }

  } // AppFactory
