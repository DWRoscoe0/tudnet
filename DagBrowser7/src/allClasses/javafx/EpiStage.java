package allClasses.javafx;

import javafx.stage.Stage;

class EpiStage 

  extends Stage
  
  /* Maybe this should be named StageUtility?
   */

  { 

    private JavaFXGUI theJavaFXGUI;

    protected EpiStage()
      {
        }

    protected EpiStage(JavaFXGUI theJavaFXGUI)
      {
        this.theJavaFXGUI= theJavaFXGUI;
        }
    
    public static EpiStage makeEpiStage(JavaFXGUI theJavaFXGUI)
      /* */
      {
        EpiStage theEpiStage= new EpiStage(theJavaFXGUI);
        return theEpiStage;
        }

    public void finishInitAndStartV(
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
        theJavaFXGUI.recordOpenWindowV(this); // Record showing.
        }
  
  }