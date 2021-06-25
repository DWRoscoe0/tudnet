package allClasses;

import static allClasses.AppLog.theAppLog;

public class AppFactory {  // For App class lifetimes.

  /* This is the factory for all classes with App lifetime.
    The app has exactly one instance of this factory.

    This class wires together the top level of the application.
    The classes constructed here are mainly the ones 
    needed before a GUI is presented to the user.  //////
    In some cases the GUI is not presented, but if it is
    then its factory is constructed by getAppGUIFactory().
    */

  // Injected dependencies that will still be needed after construction.
  private final Persistent thePersistent;

	// Other objects that will be needed later.
	private PortManager thePortManager;
  private final Shutdowner theShutdowner;
  private final AppInstanceManager theAppInstanceManager;
  private final App theApp;
  private final TCPCopier theTCPCopier;
  
  // Storage for conditional (lazy evaluation) singletons.
  private AppGUIFactory theAppGUIFactory= null;

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

      // Save in instance variables injected objects that are needed later.
	    // None.  Already saved.

  	  // Save new objects that will be needed later 
  		// from local variables to instance variables. 
      //% this.theShutdowner= theShutdowner;
      this.theAppInstanceManager= newAppInstanceManager;
      this.theApp= newApp;

      theAppLog.info("AppFactory(.) exit.");
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
	      		thePersistent,
	      	  thePortManager,
	      		theShutdowner,
	      		theAppInstanceManager,
	      		theTCPCopier
	      		);
	    return theAppGUIFactory;
	    }

  } // AppFactory
