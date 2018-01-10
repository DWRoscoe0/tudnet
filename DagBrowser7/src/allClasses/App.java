package allClasses;

import static allClasses.Globals.appLogger;

public class App { // The App, especially pre-GUI stuff.

  AppFactory theAppFactory;
  Data theData;
  Shutdowner theShutdowner;
  AppInstanceManager theAppInstanceManager;

  public App(   // Constructor.  For app creation.
      AppFactory theAppFactory,
      Data theData,
      Shutdowner theShutdowner,
      AppInstanceManager theAppInstanceManager
      )
    {
  		this.theAppFactory= theAppFactory;
  	  this.theData= theData; 
      this.theShutdowner= theShutdowner;
      this.theAppInstanceManager= theAppInstanceManager;
      }

  public void runV()  // This is for app Running.
    /* This method does any executable instance management it can.
      If the instance manager says it's okay then
      it presents the GUI to the user and interacts with him or her.
      At the same time normal network operations are done.
      When it's time to exit it uses the Shutdowner to
      do any final shutdown jobs.
     */
    {
  		//appLogger.info("App beginning.");
  	  theData.initializeV();
			theShutdowner.initializeV();
			theAppInstanceManager.initializeV();

  	  if ( ! theAppInstanceManager.managingInstancesWithExitB( ) ) 

        { // Presenting GUI to user and interacting.
      	  AppGUIFactory theAppGUIFactory= 
      	  		theAppFactory.lazyGetAppGUIFactory();
          AppGUI theAppGUI= theAppGUIFactory.getAppGUI();
          theAppGUI.runV(); // Running GUI until finished.
          	// Network operations happen at this time also.
          }

  	  theData.finalizeV();
  		//appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishV();  // Doing final app shutdown jobs.

  		appLogger.info("App exiting.");
      
      // After this method returns, the main thread of this app should exit.
      } // runV().

  } // class App.
