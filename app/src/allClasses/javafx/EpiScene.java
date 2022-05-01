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
  
    public void setDefaultsV() /// deprecated
      {
        Node rootNode= getRoot();
        JavaFXGUI.setDefaultStyleV(rootNode);
        }
    
    }
