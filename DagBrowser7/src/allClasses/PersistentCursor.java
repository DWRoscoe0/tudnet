package allClasses;

// import static allClasses.Globals.appLogger;

public class PersistentCursor 

  /* This class acts like a cursor into a PersistentNode hierarchy.
    It provides a means for
    * iterating through parts of the list of similar named items
      represented by a NavigableMap.  These items are sometimes called
      "entries" and sometimes "elements".
    * accessing the named fields within the individually selected list elements.

    It combines the ability to use paths to name positions within the hierarchy,
    with fast local access relative to a selected PersistingNode. 

    The iterator this class implements is a pitererator,
    which is an iterator with pointer semantics.

    ///org The use of the term "persistent" is unfortunate.
      According to Wikipedia, that term is used to describe data structures
      which, when they are changed, retain 
      previous versions of the data structure.
      These are used extensively in applicative languages. 
	  ///pos Should this be changed to support unsorted keys?
	    Possibly, but not immediately.
	  ///pos Make PersistentNode able to return one of these
	    and eliminate absolute path support?
	    
	  Code in this file is ordered by general use time.
	  
   	*/

	{

	  // Initialization.

	  // Constants.
	
		private final String EMPTY_STRING= "";
	
		
		// State, generally from most significant to least significant.

		private final Persistent thePersistent; // Underlying storage structure.

		protected PersistingNode entriesPersistingNode= null;
		  // PersistingNode which contains the NavigableMap which
		  // represents the list of interest.
    private String entryKeyString= null; // Key name String of 
      // presently selected NavigableMap list entry, 
      // or null if no entry is selected.
      // The positions before the beginning of the list and  after the end of the list
      // are considered equivalent and are represented by the EMPTY_STRING.
      
		private PersistingNode entryPersistingNode= null; // Cached value of PersistingNode 
		  // in the presently selected NavigableMap list entry. 


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
	      int trialIndexI= // Set trial index to list size + 1; 
	          entriesPersistingNode.getNavigableMap().size() + 1;
	      String trialKeyString;
	      while (true) // Search for a child index key not already in use in list.
	        {
	          trialKeyString= String.valueOf(trialIndexI); // Convert key index to String.
	          PersistingNode childPersistingNode= // Try getting value node at that key.
	              entriesPersistingNode.getChildPersistingNode(trialKeyString);
	          if (null == childPersistingNode) // Exit if no node, meaning key is available.
	            break;
	          trialIndexI--; // Prepare to test next lower key index.
	          }
	      setEntryKeyString( trialKeyString ); // Create and store node with selected key.
	      return this; // This cursor pointing to new node.
	      } 

		public String setListFirstKeyString( String listPathString )
		  /* This method sets the list to be scanned for 
		    scanning from the beginning in the forward direction.  
		    It does this in 2 ways:
		   	* by adding the ListNameString to the base path,
			    which is presently always the empty string for the root; and
			  * by getting ready the interface NavigableMap<K,V>
			    which holds the list of entries.
			  It also initializes other variables used to scan the list,
			  a scan starting with the first element, if there is one.
        It returns the key of the first list entry,
        or the empty string if the list is empty.
			  ///org Call general method that can handle absolute or relative path. 
		   	*/
			{
				// appLogger.debug(
				// 		"PersistentCursor.setListPathV("+listPathString+") begins.");
	  		entriesPersistingNode= 
	  				thePersistent.getOrMakePersistingNode(listPathString);
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
		   	*/
			{
		    String nextEntryKeyString= // Get next position key.
			  	  Nulls.toEmptyString( // Convert null to empty string.
			  	  		entriesPersistingNode.getNavigableMap().higherKey(
			  	  				entryKeyString));
		    setEntryKeyString( nextEntryKeyString ); // Set cursor to this position.
        return entryKeyString; // Return name of the new position.
				}

		public String setEntryKeyString( String entryKeyString )
		  /* Set the key of the present list element/entry to be entryKeyString
		    and sets the position to that key.
		    It also caches the PersistentNode associated with that key
		    in preparation for accessing the nodes fields.
		    If entryKeyString is not null and no associated node exists
		    then an empty one will be created.
		    It return the selected entry key String, 
		    or the empty String if no entry is selected.
				*/
			{
				// appLogger.debug(
				// 		"PersistentCursor.setEntryKeyV( "+entryKeyString+" )" );
				this.entryKeyString= entryKeyString; // Store the position key.
        if (! entryKeyString.isEmpty()) // If there is supposed to be a node there
  				this.entryPersistingNode= // cache the node at that position. 
  						entriesPersistingNode.getOrMakeChildPersistingNode(entryKeyString);
          else
            this.entryPersistingNode= null;
        return this.entryKeyString;
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

    public PersistingNode getEntryPersistingNode()
      /* This method returns the parent PersistingNode,
        the one associated with the list,
        not the one associated with the presently selected list element.
        */
      {
        return entriesPersistingNode;
        }
		
    
    // Methods that access fields of selected PersistingNode.
    
    public boolean getFieldB( String fieldKeyString )
      /* This method returns the boolean value of the field whose key is fieldKeyString
        in the present list element's PersistingNode.
        */
      {
        String fieldValueString= entryPersistingNode.getChildString(fieldKeyString);
        return Boolean.parseBoolean( fieldValueString );
        }
    
    public String getFieldString( String fieldKeyString )
      /* This method returns the value of the field whose key is fieldKeyString
        in the present list element's PersistingNode.
        */
      {
        String fieldValueString= entryPersistingNode.getChildString(fieldKeyString);
        // appLogger.debug( "PersistentCursor.getFieldString( "
        //    +fieldKeyString+" ) returning:"+fieldValueString);
        return fieldValueString;
        }

    public void putFieldV( String fieldKeyString, boolean fieldValueB )
      /* This method stores fieldValueB into the field whose name is fieldKeyString
        in the presently selected list element's PersistingNode.
        */
      { 
        entryPersistingNode.putChildV( fieldKeyString, ""+fieldValueB );
        }

    public void putFieldV( String fieldKeyString, String fieldValueString )
      /* This method stores fieldValueString into the field whose name is fieldKeyString
        in the presently selected list element's PersistingNode.
        */
      { 
        entryPersistingNode.putChildV( fieldKeyString, fieldValueString );
        // appLogger.debug(
        // "PersistentCursor.putFieldV( "+fieldKeyString+"= "+fieldValueString);
        }
		
		
		// Finalization code: none.
		
		}
