package allClasses.javafx;

import javafx.scene.Scene;
import javafx.stage.Stage;

class EpiStage 

  extends Stage
  
  /* Maybe this should be named StageUtility?
   */

  { 

    public EpiStage()
      /* */
      {}

    public static void finishSettingsAndShowV(
        Stage theStage,
        JavaFXGUI theJavaFXGUI,
        Scene theScene,
        String titleString
        )
      /* This method does some of initialization that 
       * Stages have in common near the end of 
       * their construction and initialization.
       */
      {
        theStage.setScene(theScene);
        theStage.setTitle(titleString);
        
        theStage.show();
        theJavaFXGUI.recordOpenWindowV(theStage); // Record showing.
        }
  
  }