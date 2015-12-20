package allClasses;

//import static allClasses.Globals.appLogger;

public class App { // The App, especially pre-GUI stuff.  See runV(..) for details.

  Shutdowner theShutdowner;
  AppInstanceManager theAppInstanceManager;
  AppFactory theAppFactory;

  public App(   // Constructor.  For app creation.
      Shutdowner theShutdowner,
      AppInstanceManager theAppInstanceManager,
      AppFactory theAppFactory
      )
    {
      this.theShutdowner= theShutdowner;
      this.theAppInstanceManager= theAppInstanceManager;
      this.theAppFactory= theAppFactory;
      }

  public void runV()  // This is for app Running.
    /* This method does any executable instance management it can.
      If the instance manager says it's okay then
      it presents the GUI to the user and interacts with him or her.
      When it's time to exit it uses the Shutdowner to
      do any final shutdown jobs.
     */
    {
  		//appLogger.info("App beginning.");
  		theShutdowner.initializeV(); // Preparing for future app shutdown.

  	  if ( ! theAppInstanceManager.managingInstancesThenNeedToExitB( ) ) 

        { // Presenting GUI to user and interacting.
      	  AppGUIFactory theAppGUIFactory= theAppFactory.lazyGetAppGUIFactory();
          AppGUI theAppGUI= // Getting GUI manager singleton.
              theAppGUIFactory.getAppGUI();
          theAppGUI.runV(); // Running GUI manager until finished.
          }

  		//appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishV();  // Doing final app shutdown jobs.

  		//appLogger.info("App exiting.");
      
      // After this method returns, the main thread of this app should exit.
      } // runV().

  } // class App.
