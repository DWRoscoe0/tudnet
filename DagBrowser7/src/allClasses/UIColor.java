package allClasses;

import java.awt.Color;
import java.awt.Component;

public class UIColor {

	public static final Color activeColor= Color.YELLOW; ///? Phase out.
	
	public static final Color selectedBackgroundColor= Color.BLACK;
	public static final Color selectedForegroundColor= Color.WHITE;
	public static final Color normalForgroundColor= Color.BLACK;

  // Color set by StateList class initializer code.
	public static final Color initializerStateColor= Color.YELLOW; // Color.WHITE;
  // Colors set by the different class initializer methods.
	public static final Color initialStateColor= Color.BLUE;
	public static final Color initialAndStateColor= Color.MAGENTA;
	public static final Color initialOrStateColor= Color.ORANGE;
  // Colors set by class handler methods.
	public static final Color runningStateColor= Color.GREEN;
	public static final Color runnableStateColor= new Color(255,127,127); 
			// LIGHT_GREEN, was using Color.CYAN;
	public static final Color inactiveStateColor= Color.CYAN; // was LGHT_GRAY;
	public static final Color waitingStateColor= Color.PINK;


  public static void setColorsV
    ( Component theRenderComponent,
      Color cachedJListBackgroundColor,
    	DataNode theDataNode,
      boolean isSelectedB,
      boolean hasFocusB
      )
    /* Sets appropriate forground and background colors in theRenderComponent 
      for displaying theDataNode, using the other parameters as context.
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
