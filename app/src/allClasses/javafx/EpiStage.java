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

    public void setCommonSettingsV(String titleString)
      /* This method does initialization that Stages have in common,
       * consisting of:
       * * the setting of the titleString
       * * default Stage size
       * * default Scene Font style and size.
       *
       */
      {
        setTitle(titleString);
        setHeight(700);
        setWidth(600);
        EpiScene.setDefaultsV(getScene());
        }
  
  }