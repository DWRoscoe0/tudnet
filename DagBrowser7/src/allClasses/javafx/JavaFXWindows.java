package allClasses.javafx;

import java.util.HashMap;
import java.util.Map;
import javafx.stage.Stage;
import javafx.stage.Window;

public class JavaFXWindows 

  {
  
    private static Map<Window,Boolean> windowMap= 
        new HashMap<Window,Boolean>();
    
    public static Stage makeStage() 
      {
        Stage theStage= new Stage(); // Construct Stage.
        recordWindow(theStage); // Record it in map.
        return theStage;
        }
    
    public static void recordWindow(Window theWindow)
      {
        windowMap.put(theWindow, true); // Record it in map.
        }
    
    public static void closeWindows() 
      {
        for (Window theWindow : windowMap.keySet())
          theWindow.hide();
        }
    
    }