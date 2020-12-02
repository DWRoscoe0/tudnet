package allClasses.javafx;

//// import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

class EpiStage 

  extends Stage
  
  /* Maybe this should be named StageUtility?
   */

  { 

    private JavaFXGUI theJavaFXGUI;

    public static EpiStage prepareEpiStage(JavaFXGUI theJavaFXGUI)
      /* */
      {
        EpiStage theEpiStage= new EpiStage();
        theEpiStage.theJavaFXGUI= theJavaFXGUI;
        return theEpiStage;
        }

    public void finishSettingsAndShowV(
        //// Stage theStage,
        Scene theScene,
        String titleString
        )
      /* This method does some of initialization that 
       * Stages have in common near the end of 
       * their construction and initialization.
       */
      {
        EpiScene.setDefaultsV(theScene);

        setScene(theScene);
        setTitle(titleString);
        
        show();
        theJavaFXGUI.recordOpenWindowV(this); // Record showing.
        }
  
  }