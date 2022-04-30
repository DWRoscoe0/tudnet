package allClasses.javafx;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class EpiScene extends Scene
  {
    public EpiScene(Parent rootParent) 
      {
        super(rootParent);
        setDefaultsV();
        }
  
    private void setDefaultsV()
      {
        Node rootNode= getRoot();
        JavaFXGUI.setDefaultStyle(rootNode);
        }
    
    }
