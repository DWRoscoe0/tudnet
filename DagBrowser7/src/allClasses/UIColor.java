package allClasses;

import java.awt.Color;
import java.awt.Component;

public class UIColor {

	// Colors for non-selected cells.
	
	  // Non-selected foreground colors.
			public static final Color normalForgroundColor= Color.BLACK;

		// Non-selected background colors.
			public static final Color activeColor= Color.YELLOW; // Active, non-State.
			  // This is rarely seen, but is very noticeable when used.

  	// Non-selected background state colors used in StateList class.
			public static final Color activeStateColor=
					new Color(255,191,255); // light purple.
			public static final Color inactiveStateColor=
					new Color(191,191,255); // light blue.
			

	// Colors for selected cells.

		// Set by swapping foreground and background colors in setColorsV(.) method.


  // Methods.

  public static void setColorsV
    ( Component theRenderComponent,
      Color defaultBackgroundColor,
    	DataNode theDataNode,
      boolean isSelectedB,
      boolean hasFocusB
      )
    /* Sets appropriate foreground and background colors in theRenderComponent 
      for displaying theDataNode, using the remaining parameters as context.
      */
    {
  		Color backgroundColor, foregroundColor;
      setColors: { 
  			// Calculating colors based on various conditions.
    		{ // Set most probable colors.
      	  backgroundColor= // Set default background. 
      	  		theDataNode.getBackgroundColor(defaultBackgroundColor);
      	        // This gets DataNode's preferred color, if it has one,
      	  			// or defaultBackgroundColor if not.
        	foregroundColor= // Set default foreground.
        			UIColor.normalForgroundColor;
    			}
        if ( ! hasFocusB ) break setColors; // Cell not focused, change nothing.
        if ( ! isSelectedB ) break setColors; // Same if cell not selected.
        { // Cell is both selected and focused, so set special colors.
        	// Do this by swapping the present background and foreground colors.
        	Color tmpColor= backgroundColor;
          backgroundColor= foregroundColor;
          foregroundColor= tmpColor; 
        	}
      } // setColors:
	  		// We'decided on the colors to use, now set them in component.
		    theRenderComponent.setBackground( backgroundColor );
		    theRenderComponent.setForeground( foregroundColor );
		  }

	}
