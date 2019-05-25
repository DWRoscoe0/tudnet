package allClasses;

import java.util.NavigableMap;

// import static allClasses.Globals.appLogger;

public class PersistentCursor 

  /* This class acts like a cursor into a Persistent data structure.
    It provides a means for
    * iterating through parts of a list of similar named items
      represented by a NavigableMap.
    * accessing the named parts (fields) within the list elements.

    It combines the ability to use paths,
    with fast local access relative to an existing PersistingNode. 

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

		private PersistingNode entriesPersistingNode;
		  // PersistingNode which contains the NavigableMap which
		  // represents the list of interest.
    private String entryKeyString; // Key name String of 
      // presently selected NavigableMap entry, 
      // or null if no entry is selected. 
		private PersistingNode entryPersistingNode; // Cached value 
		  // PersistingNode in presently selected NavigableMap entry. 


		public PersistentCursor( Persistent thePersistent ) // constructor
			{
				this.thePersistent= thePersistent;
				}


		// Service code.

    public void setListAndEntryV(  
        String listPathString, String entryKeyString )
      {
        // appLogger.debug(
        // "PersistentCursor.setListAndEntryV("
        //    +listPathString+", "+entryKeyString+") begins.");
        setListV( listPathString ); // First define list. 
        
        setEntryKeyV( entryKeyString ); // Next define position in list.
        }

    public NavigableMap<String, PersistingNode> getNavigableMap()
      {
        NavigableMap<String, PersistingNode> theNavigableMap= entriesPersistingNode.getNavigableMap();
        return theNavigableMap;
      }

		public void setListV( String listPathString )
		  /* This method sets the list to be scanned.  It does this in 2 ways:
		   	* by adding the ListNameString to the base path,
			    which is presently always the empty string for the root; and
			  * by getting ready the interface NavigableMap<K,V>
			    which holds the list of entries.
			  It also initializes other variables used to scan the list,
			  a scan starting with the first element, if there is one. 
		   	*/
			{
				// appLogger.debug(
				// 		"PersistentCursor.setListPathV("+listPathString+") begins.");
	  		entriesPersistingNode= 
	  				thePersistent.getOrMakePersistingNode(
	  						listPathString + Config.pathSeperatorC + "entries");
	  		moveToFirstV();
				}

		private void moveToFirstV()
		  /* This method prepares for iterating over the list 
		    by moving to the beginning.
		    Presently it just stores a path String.
		    Later it will reset the PersistingNode TreeMap node-based iterator.
		   */
			{
				// appLogger.debug(
				// 		"PersistentCursor.moveToFirstV() begins.");
	  		entryKeyString= EMPTY_STRING; // Set to before first entry.
	  		nextKeyString(); // Move to first entry unless list is empty.
	  		}

		public String nextWithWrapKeyString()
		  /* This does the same as nextKeyString() but
		    tries to wrap around to beginning of list if at the end, 
		    so it returns the empty string only if the list is empty.
	      */
			{
				String keyString=  // Try moving to next and getting element key.
						nextKeyString();
				if ( keyString.isEmpty() ) // If now at end of list then
					keyString= nextKeyString(); // try moving one more time to beginning.
				// appLogger.debug(
				// 	"PersistentCursor.nextWithWrapElementIDString(..) returning:"+keyString);
			  return keyString; // Return name of this position.
				}

		public String nextKeyString()
		  /* Advances the piterator and returns the key of the next list entry.
		    If the result is the empty string then the piterator is positioned
		    on no element.
		   	*/
			{
		    String nextEntryKeyString= // Get next position key.
			  	  Nulls.toEmptyString( // Convert null to empty string.
			  	  		entriesPersistingNode.getNavigableMap().higherKey(
			  	  				entryKeyString));
		  	if (! entryKeyString.isEmpty()) // There is a next...
          //// entryKeyString= nextEntryKeyString;
  		    setEntryKeyV( nextEntryKeyString ); // cache it.
        return entryKeyString; // Return name of the new position.
				}

		public void setEntryKeyV( String entryKeyString )
		  /* Set the key of the present list element/entry
		    and sets the position to that key.
		    It gets the PersistentNode associated with that key
		    in preparation for accessing the nodes fields.
				*/
			{
				// appLogger.debug(
				// 		"PersistentCursor.setEntryKeyV( "+entryKeyString+" )" );
				this.entryKeyString= entryKeyString; // Store position.
				this.entryPersistingNode= // Cache node at that position. 
						entriesPersistingNode.getOrMakePersistingNode(entryKeyString);
				}

		public String getEntryKeyString()
		  /* Returns the key of the present list element/entry.
				Returns the empty String if the piterator is positioned
				at the end of the list, or if the list is empty
				*/
			{
				// appLogger.debug(
				// 		"PersistentCursor.getEntryKeyString() returning:"+entryKeyString);
				return entryKeyString;
				}

    public PersistingNode getEntryPersistingNode()
      /* Returns PersistingNode associated with the list. */
      {
        return entriesPersistingNode;
        }
		
		public String getFieldString( String fieldKeyString )
		  /* Returns the value of the field whose key is fieldKeyString
		    in the present list element.
		    Though the fields within a list element 
		    are of the same type of structure as the elements within a list,
		    we make no attempt to iterate over them.
		    */
			{ 
				String fieldValueString= 
						entryPersistingNode.getString(fieldKeyString);
				// appLogger.debug( "PersistentCursor.getFieldString( "
				// 		+fieldKeyString+" ) returning:"+fieldValueString);
				return fieldValueString;
				}
		
		public void putFieldV( String fieldKeyString, String fieldValueString )
		  /* StoresReturns the value of the field whose name is fieldNameString
		    in the present list element.
		    Though the fields within a list element 
		    are of the same type of structure as the elements within a list,
		    we make no attempt to iterate over them.
		    */
			{ 
				entryPersistingNode.putV( fieldKeyString, fieldValueString );
				// appLogger.debug(
				// "PersistentCursor.putFieldV( "+fieldKeyString+"= "+fieldValueString);
				}
		
		
		// Finalization code: none.
		
		}
