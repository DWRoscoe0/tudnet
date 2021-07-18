package allClasses.javafx;

//// import static allClasses.AppLog.theAppLog;

import allClasses.Shutdowner;
import javafx.stage.Stage;

class EpiStage 

  extends Stage
  
  /* Maybe this should be named StageUtility?
   */

  { 

    //// private JavaFXGUI theJavaFXGUI;
    protected Shutdowner theShutdowner;

    /*  ////
    protected EpiStage()
      {
        }
    */  ////

    //// protected EpiStage(JavaFXGUI theJavaFXGUI, Shutdowner theShutdowner)
    protected EpiStage(Shutdowner theShutdowner)
      {
        //// this.theJavaFXGUI= theJavaFXGUI;
        this.theShutdowner= theShutdowner;
        }
    
    /*  ////
    public static EpiStage makeEpiStage(JavaFXGUI theJavaFXGUI)
      {
        EpiStage theEpiStage= new EpiStage(theJavaFXGUI);
        return theEpiStage;
        }
    */  ////

    public void finishStateInitAndStartV(
        String titleString
        )
      /* This method finishes initialization that Stages have in common,
       * consisting of:
       * * the setting of the titleString
       * * default Stage size
       * * default Scene Font style and size.
       * Then it shows the Stage and records that fact.
       */
      {
        setTitle(titleString);
        setHeight(700);
        setWidth(600);
        EpiScene.setDefaultsV(getScene());

        show();
        JavaFXGUI.recordOpenWindowV(null,this,null); // Record showing.
        }
  
  }