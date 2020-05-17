package allClasses;

// import static allClasses.Globals.appLogger;

public class PersistentCursor 

  /* This class acts like a cursor into a data node hierarchy.
    The hierarchy can be thought of as a tree data structure.
    At the branches of the tree are maps whose keys are strings.  
    At the leaves are string values. 
     
    This class caches several values.
    * A string representing the iterator position.
    * An upper map representing the data structure being iterated.
    * A lower map representing the data structure at the iterator position.
      Fields within this structure can be accessed by name.

    This class combines the ability to use paths to name positions within the hierarchy,
    with fast local access relative to a selected map. 

    The iterator that this class implements is a pitererator,
    which is an iterator with pointer semantics.

    ///org The use of the term "persistent" is unfortunate.
      According to Wikipedia, that term is used to describe data structures
      which, when they are changed, retain 
      previous versions of the data structure.
      These are used extensively in applicative languages. 
	  ///pos Should this be changed to support unsorted keys?
	    Possibly, but not immediately.  Underway
	  ///pos Make PersistentNode able to return one of these
	    and eliminate absolute path support?
	    
	  Code in this file is ordered by general use time.
	  
   	*/

	{

	  // Initialization.

	  // Constants.
	
		private final String EMPTY_STRING= "";
	
		
		// State, generally from most significant to least significant.

		private final Persistent thePersistent; // Underlying dual-format storage structure.

    private String entryKeyString= EMPTY_STRING; // Key name String of 
      // presently selected entry in the upper map, the map being iterated.
      // If this is the EMPTY_STRING then no map entry is selected.
      // In this case the iterator position may be considered to be
      // before the beginning of the entries or after the end of the entries.
      // These are considered equivalent.

    // EpiNode data cursor variables.
    protected MapEpiNode upperMapEpiNode= null;
      // MapEpiNode which is the upper map which is being iterated.
    private MapEpiNode lowerMapEpiNode= null;
      // MapEpiNode which is the value of the selected entry of the upper map. 
      // It is the lower map which contains various data fields of that entry. 


		public PersistentCursor( Persistent thePersistent ) // constructor
			{
				this.thePersistent= thePersistent;
				}


		// Service code.

	  public PersistentCursor createEntryInPersistentCursor()
	    /* This method creates a new entry in this PersistentCursor 
	      with no data and no particular key.
	      The only guarantee is that the key in the new element will be unique.
	      It uses a key String that is a low-value numerical index,
	      even though existing keys might not be numerical indexes.
	      */
	    { 
	      String trialKeyString;
        int trialIndexI= // Set trial index to map size + 1; 
            upperMapEpiNode.getSizeI() + 1;
        while (true) // Search for a child index key not already in use in map.
          {
            trialKeyString= String.valueOf(trialIndexI); // Convert index to String.
            ScalarEpiNode keyScalarEpiNode= // Convert String to Scalar.
                new ScalarEpiNode(trialKeyString);
            EpiNode childEpiNode= // Try getting value node at that Scalar key.
                upperMapEpiNode.getEpiNode(keyScalarEpiNode);
            if (null == childEpiNode) // Exit if no node, meaning key is available.
              break;
            trialIndexI--; // Prepare to test next lower key index.
            }
        setEntryKeyString( trialKeyString ); // Create and store node with selected key.
	      return this; // This cursor pointing to new node.
	      } 

		public String setListFirstKeyString( String listKeyString )
		  /* This method sets the list to be scanned for 
		    scanning the list associated with listKeyString
		    from the beginning in the forward direction.  
		    It also initializes other variables used to scan the list,
			  a scan starting with the first element, if there is one.
        It returns the key of the first list entry,
        or the empty string if the list is empty.
			  */
			{
				// appLogger.debug(
				// 		"PersistentCursor.setListPathV("+listPathString+") begins.");
        upperMapEpiNode= thePersistent.getOrMakeMapEpiNode(listKeyString);
	  		return moveToFirstKeyString();
				}

    public String moveToFirstKeyString()
      /* This method prepares for iterating over the list from the beginning.
        It returns the key of the first list entry,
        or the empty string if the list is empty.
        */
      {
        // appLogger.debug(
        //    "PersistentCursor.moveToFirstV() begins.");
        entryKeyString= EMPTY_STRING; // Set to before first entry.
        return nextKeyString(); // Move to first entry unless list is empty.
        }

    public String moveToNoKeyString()
      /* This method prepares for iterating over the list by pointing to
        no element in the list.  Moving to the text element will move to the first.
        It returns the empty string.
        */
      {
        return setEntryKeyString(EMPTY_STRING);
        }

		public String nextWithWrapKeyString()
		  /* This does the same as nextKeyString() but
		    tries to wrap around to beginning of list if the end is reached, 
		    so it returns the empty string only if the list is empty.
	      */
			{
				String keyString=  // Try moving to next and getting element key.
						nextKeyString();
				if ( keyString.isEmpty() ) // If now past end of list then
					keyString= nextKeyString(); // try moving one more time to beginning.
				// appLogger.debug(
				// 	"PersistentCursor.nextWithWrapElementIDString(..) returning:"+keyString);
			  return keyString; // Return name of this position.
				}

		public String nextKeyString()
		  /* Advances the piterator and returns the key of the new list entry.
		    If the result is the empty string then the piterator is positioned
		    on no element.  Either the position moved past the end of the list,
		    or the list is empty.
		    
		    ///opt This could and should be improved by using
		    an actual Iterator in this class, instead of calling
		    MapEpiNode.getNextString(..) to using an iterator to
		    search for the next string position.    
		   	*/
			{
        String nextEntryKeyString= null;
        nextEntryKeyString= 
          upperMapEpiNode.getNextString(entryKeyString);
		    setEntryKeyString(  // Set cursor to this position
            Nulls.toEmptyString( // after converting possible null to empty string.
                nextEntryKeyString ) );
        return entryKeyString; // Return name of the new position.
				}

		public String getEntryKeyString()
		  /* This method returns the key of the presently selected list element/entry.
				It returns the empty String if the piterator is 
				not positioned on an element, which can be true either
				after the end of the list or before the beginning of the list
				or if the list is empty
				*/
			{
				// appLogger.debug(
				// 		"PersistentCursor.getEntryKeyString() returning:"+entryKeyString);
				return entryKeyString;
				}

    public void removeEntryV()
      /* This method removes the presently selected list element/entry.
        It returns with the cursor not on any element.
        */
      {
        upperMapEpiNode.removeV( entryKeyString ); // Remove present element
        setEntryKeyString( EMPTY_STRING ); // Set position on no element.
        }

    public String setEntryKeyString( String entryKeyString )
      /* Set the key of the present list element/entry to be entryKeyString
        and sets the position to that key.
        It also caches a reference to the node associated with that key
        in preparation for accessing the node's fields,
        unless the key is EMPTY_STRING, which is valid and means
        the iterator is positioned outside of the maps entries.
        If entryKeyString is not empty and no associated node exists
        then an empty node will be created with that key.
        It returns the same entry key String that was input.
        */
      {
        // appLogger.debug(
        //    "PersistentCursor.setEntryKeyV( "+entryKeyString+" )" );
        this.entryKeyString= entryKeyString; // Store the selection/position key.
        if (! entryKeyString.isEmpty()) // If there is supposed to be a node there
          this.lowerMapEpiNode= // cache the node at that position. 
              upperMapEpiNode.getOrMakeMapEpiNode(entryKeyString);
          else
          this.lowerMapEpiNode= null;
        return this.entryKeyString;
        }


		// Methods that access fields of selected map.


		/* Methods which update fields, meaning they store a value only if it is changing.
		  They also updated lastModified in that case, but only in that case.
		  */

    public void updateFieldV( 
        String fieldKeyString, boolean fieldValueB )
      /* If fieldValueB is different from the value presently associated with 
        fieldKeyString, then it replaces the stored value and
        the field "lastModified" is set to the present time.
        */
      { 
        updateFieldV( fieldKeyString, ""+fieldValueB );
        }

    private void updateFieldV( String fieldKeyString, String fieldValueString )
      /* If fieldValueString is different from the value presently associated with 
        fieldKeyString, then it replaces the stored value and
        the field "lastModified" is set to the present time.
        */
      { 
        boolean changeNeededB= // Calculate whether field needs to be changed. 
            ! fieldValueString.equals(getFieldString(fieldKeyString));
        if (changeNeededB)
          putFieldWithLastModifiedV( fieldKeyString, fieldValueString );
        }

    private void putFieldWithLastModifiedV( 
        String fieldKeyString, String fieldValueString )
      /* This method stores fieldValueString into the field whose name is fieldKeyString
        but also updates the field "lastModified" with the present time.
        */
      { 
        putFieldV( fieldKeyString, fieldValueString );
        putFieldV( "lastModified", ""+System.currentTimeMillis());
        }

    
    // Methods which unconditionally store values in fields.

    public void putFieldV( String fieldKeyString, String fieldValueString )
      /* This method stores fieldValueString into the field whose name is fieldKeyString
        in the presently selected list element's map.
        */
      { 
        lowerMapEpiNode.putV( fieldKeyString, fieldValueString );
        // appLogger.debug(
        // "PersistentCursor.putFieldV( "+fieldKeyString+"= "+fieldValueString);
        }

    public void removeFieldV( String fieldKeyString)
    /* This method remove the field whose name is fieldKeyString
      in the presently selected list element's map.
      */
    { 
      lowerMapEpiNode.removeV( fieldKeyString );
      }

    public MapEpiNode getSelectedMapEpiNode()
      /* Returns the MapEpiNode presently selected by the iterator,
        or null if no entry is selected.  */
      { 
        return lowerMapEpiNode; 
        }
    
    // Methods which test values in fields.

    public boolean testFieldIsB( String testKeyString, String testValueString )
      /* This method returns true if the field whose key is fieldKeyString
        equals testValueString, false otherwise.
        */
      {
        String valueString= getFieldString(testKeyString);
        return testValueString.equals( valueString );
        }
    
    // Methods which get values from fields.

    public boolean testB( String fieldKeyString )
      /* This method returns the boolean value of the field whose key is fieldKeyString
        in the present element's map.
        */
      {
        return lowerMapEpiNode.testB(fieldKeyString);
        }

    public String getEmptyOrString( String keyString )
    /* Returns the value String associated with keyString,
      or the empty string if there is none.
      */
    {
      return getString( keyString, "" );
      }
    
    private String getString( String keyString, String defaultValueString )
      /* Returns the value String associated with keyString,
        or defaultValueString if there is no value String stored.
        */
      {
        String childValueString= getFieldString(keyString); 
        if (childValueString == null) 
          childValueString= defaultValueString;
        return childValueString;
      }

    public String getFieldString( String fieldKeyString )
      /* This method returns the value of the field whose key is fieldKeyString
        in the present element's map.
        */
      {
        String fieldValueString= null;
        fieldValueString= lowerMapEpiNode.getString(fieldKeyString);
        // appLogger.debug( "PersistentCursor.getFieldString( "
        //    +fieldKeyString+" ) returning:"+fieldValueString);
        return fieldValueString;
        }


		// Finalization code: none.

		}
