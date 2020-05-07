package allClasses;

import java.awt.Color;
import java.awt.Component;

public class UIColor {

	// Colors for non-selected cells.
	
	  // Non-selected foreground colors.
			public static final Color normalForgroundColor= Color.BLACK;

		// Non-selected background colors.
      public static final Color inactiveColor= 
          new Color(160,160,160); // dark gray.
      public static final Color activeColor= 
          new Color(192,192,192); // light gray.
        //  new Color(255,191,255); // light purple.
			  // Color.YELLOW; // Active, non-State.
			  // This is rarely seen, but is very noticeable when used.

  	// Non-selected background state colors used in StateList class.
			// public static final Color inactiveStateColor=
      //    new Color(191,191,255); // light blue.
			public static final Color inactiveStateColor=
			    inactiveColor;
          // new Color(191,191,191); // light gray, or is this actually dark gray?
			// Active colors.
      public static final Color activeStateColor=
          activeColor;
          /// new Color(255,191,255); // light purple.
          /// Color.WHITE;
      public static final Color activeWaitingWithoutLimitStateColor=
          new Color(191,191,255); // light blue.
            // blue: waiting for input, but there is no time limit.
            // Example: Disconnected intentionally.
      public static final Color activeBeforeTimeOutStateColor=
          new Color(191,255,191); // light green.
            // green: waiting for input, there is a time limit, but a time-out
            // has not yet occurred.  Example: Connected, doing normal work.
      public static final Color activeAfterTimeOutStateColor=
          new Color(255,255,191); // yellow.
            // yellow: waiting for input, there is a time limit, 
            // and a time-out has occurred, exponential retrying is underway.
      public static final Color activeInErrorStateColor=
          new Color(255,191,191); // red.
            // red: waiting for input, non-exponential slow retrying is underway.
            // Example: Disconnected unintentionally, trying to reestablish.
      

	// Colors for selected cells.

		// Set by swapping foreground and background colors in setColorsV(.) method.


  // Methods.

  public static void setColorsV
    ( Component theRenderComponent,
      Color defaultBackgroundColor,
      Object valueObject,
    	//// DataNode theDataNode,
      boolean isSelectedB,
      boolean hasFocusB
      )
    /* Sets appropriate foreground and background colors in theRenderComponent 
      for displaying theDataNode, using the remaining parameters as context.
      theDataNode.getBackgroundColor(defaultBackgroundColor) is called go get
      the preferred background color, which might depend on the DataNode state.
      */
    {
  		Color backgroundColor, foregroundColor;
      setColors: { 
  			// Calculating colors based on various conditions.
    		{ // Set most probable colors.
          backgroundColor= Color.YELLOW; //// temporary default for testing.
          if (valueObject instanceof DataNode) {
            DataNode theDataNode= (DataNode)valueObject;
        	  backgroundColor= // Set default background. 
        	  		theDataNode.getBackgroundColor(defaultBackgroundColor);
        	        // This gets DataNode's preferred color, if it has one,
        	  			// or defaultBackgroundColor if not.
            }
        	foregroundColor= // Set default foreground.
        			UIColor.normalForgroundColor;
    			}
        if ( ! hasFocusB ) break setColors; // Cell is not focused so change nothing.
        if ( ! isSelectedB ) break setColors; // Same if cell is not selected.
          { // Cell is both selected and focused, so set special colors.
          	// Do this by swapping the present background and foreground colors.
          	Color tmpColor= backgroundColor;
            backgroundColor= foregroundColor;
            foregroundColor= tmpColor; 
          	}
      } // setColors:
	  		// We've decided on the colors to use, now set them in component.
		    theRenderComponent.setBackground( backgroundColor );
		    theRenderComponent.setForeground( foregroundColor );
		  }

	}
