package allClasses;

public class AppFactory {  // For App class lifetimes.

  /* This is the factory for all classes with App lifetime.
    It wires together the top level of the application.
    The classes constructed here are mainly the ones 
    needed before a GUI is presented to the user.
    In some cases the GUI is not presented, but if it is
    then its factory is constructed with getAppGUIFactory().
    */

  // public unconditional singleton storage.
  public final AppInstanceManager theAppInstanceManager;
  public final Shutdowner theShutdowner;

  // private unconditional singleton storage.
  private final App theApp;

  public AppFactory( String[] argStrings )  // Factory constructor.
  	// This constructor builds the unconditional singletons for this scope.
    {
      theShutdowner= new Shutdowner();
      theAppInstanceManager= new AppInstanceManager(argStrings,theShutdowner);
      theApp= new App(
        theShutdowner,
        theAppInstanceManager,
        this // The App gets to know the factory that made it. 
        );
      }

  // Unconditional singleton getters, allowed because if's for the top level.
  public App getApp() 
    { return theApp; }

  // Conditional singleton getters and storage, for lazy evaluation.
  private AppGUIFactory theAppGUIFactory= null;
  public AppGUIFactory lazyGetAppGUIFactory()
    // This makes and gets the factory for the next smaller scope for this app.
	  {
	    if (theAppGUIFactory == null) // Constructing lazily and only one time.
	      theAppGUIFactory= new AppGUIFactory(
	      		this, // AppGUIFactory gets to know this, its factory.
	      		theShutdowner,
	      		theAppInstanceManager
	      		);
	    return theAppGUIFactory;
	    }

  } // AppFactory
