package allClasses;

import static allClasses.Globals.appLogger;

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
      it presents the GUI to the user.
      When this is all done it uses the Shutdowner to
      do any final shutdown jobs.
     */
    {
  		appLogger.info("App beginning.");
  		theShutdowner.initializeV(); // Preparing for future app shutdown.

  	  if ( ! theAppInstanceManager.managingInstancesThenNeedToExitB( ) ) 

        {
      	  AppGUIFactory theAppGUIFactory= theAppFactory.getAppGUIFactory();
          AppGUIManager theAppGUIManager= // Getting GUI manager singleton.
              theAppGUIFactory.getAppGUIManager();
          theAppGUIManager.runV(); // Running GUI manager until finished.
          }

  		appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishV();  // Doing final app shutdown jobs.

  		appLogger.info("App exiting.");
      // After this method returns, the main thread of this app should exit.
      } // runV().

  } // class App.
