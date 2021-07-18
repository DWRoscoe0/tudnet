package allClasses.javafx;

import allClasses.Shutdowner;
import javafx.stage.Stage;

class EpiStage 

  extends Stage
  
  /* Maybe this should be named StageUtility?
   */

  { 

    protected Shutdowner theShutdowner;

    protected EpiStage(Shutdowner theShutdowner)
      {
        this.theShutdowner= theShutdowner;
        }

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