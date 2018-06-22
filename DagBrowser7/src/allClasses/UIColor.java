package allClasses;

import java.awt.Color;
import java.awt.Component;

public class UIColor {

	// Selected item colors.  ///elim?
	public static final Color selectedBackgroundColor= Color.BLACK;
	public static final Color selectedForegroundColor= Color.WHITE;
	
	// Non-selected item colors.
	  // Non-selected foreground colors.
		public static final Color normalForgroundColor= Color.BLACK;

		// Non-selected background colors.
			public static final Color activeColor= Color.YELLOW; // Active, non-State.

  	// Non-selected background state colors, used in StateList class.
		  // Color set by StateList class initializer code.
			public static final Color initializerStateColor= Color.YELLOW; // Color.WHITE;
		  // Colors set by the different class initializer methods.
			public static final Color initialStateColor= new Color(127,127,255);
					// LIGHT_BLUE.  wAS Color.BLUE;
			  // This is a state that has not yet been entered.
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
    /* Sets appropriate foreground and background colors in theRenderComponent 
      for displaying theDataNode, using the other parameters as context.
      */
    {
  		Color backgroundColor, foregroundColor;
      setColors: { // Setting colors based on various conditions.
    		{ // Set most probable colors.
      	  backgroundColor= // Set default background. 
      	  		theDataNode.getBackgroundColor(cachedJListBackgroundColor);
      	        // This gets DataNode's preferred color, if it has one,
      	  			// or the JList's background color if not.
        	foregroundColor= // Set default foreground.
        			UIColor.normalForgroundColor;
    			}
        if ( ! hasFocusB ) break setColors; // Cell not focused, change nothing.
        if ( ! isSelectedB ) break setColors; // Same if cell not selected.
        { // Cell is both selected and focused, so set special colors for that.
        	// Swap the present background and foreground colors.
        	Color tmpColor= backgroundColor;
          backgroundColor= foregroundColor;
          foregroundColor= tmpColor; 
        	}
        } // adjustColors:
  		{ // We've decided on the colors to use, now set them.
	    	theRenderComponent.setBackground( backgroundColor );
	    	theRenderComponent.setForeground( foregroundColor );
	  		}
      }

}
