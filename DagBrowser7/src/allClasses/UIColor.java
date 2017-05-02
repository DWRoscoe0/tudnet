package allClasses;

import java.awt.Color;
import java.awt.Component;

public class UIColor {

	public static final Color activeColor= Color.YELLOW;
	public static final Color selectedBackgroundColor= Color.BLACK;
	public static final Color selectedForegroundColor= Color.WHITE;

	public static final Color stateColor= Color.CYAN;
	public static final Color andStateColor= Color.MAGENTA;
	public static final Color orStateColor= Color.ORANGE;
	public static final Color normalForgroundColor= Color.BLACK;


  public static void setColorsV
    ( Component theRenderComponent,
      Color cachedJListBackgroundColor,
    	DataNode theDataNode,
      boolean isSelectedB,
      boolean hasFocusB
      )
    /* Sets theRenderComponent for displaying theDataNode,
      using the other parameters as context.
      It sets text and colors. 
      */
    {
  		Color backgroundColor, foregroundColor;
      setColors: { // Setting colors based on various conditions.
    		{ // Set most common colors.
      	  backgroundColor= // Set default background. 
      	  		theDataNode.getBackgroundColor(cachedJListBackgroundColor);
      	        // This gets DataNode's preferred color, if it has one,
      	  			// or the JList's background color if not.
        	foregroundColor= // Set default foreground.
        			UIColor.normalForgroundColor;
    			}
        if ( ! hasFocusB ) break setColors; // Cell not focused, change nothing.
        if ( ! isSelectedB ) break setColors; // Same if cell not selected.
        { // Cell is both selected and focused, so set special colors.
          backgroundColor= UIColor.selectedBackgroundColor;
      	  foregroundColor= UIColor.selectedForegroundColor;
        	}
        } // adjustColors:
  	  // We've decided on colors, now set them.
    	theRenderComponent.setBackground( backgroundColor );
    	theRenderComponent.setForeground( foregroundColor );
      }

}
