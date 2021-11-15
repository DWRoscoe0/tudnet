package allClasses;

import static allClasses.AppLog.theAppLog;

import allClasses.epinode.MapEpiNode;

// import static allClasses.Globals.appLogger;

public class PersistentCursor 

  /* This class acts like a cursor into a Persistent data node hierarchy,
    consisting now of EpiNodes, mostly MapEpiNodes.
    The hierarchy can be thought of as a tree data structure.
    At the branches of the tree are maps whose keys are strings.  
    At the leaves are string values. 
     
    This class caches several values.
    * A string representing the iterator position.
    * An outer map representing the data structure being iterated.
    * A inner map representing the data structure at the iterator position.
      Fields within this structure can be accessed by name.

    This class combines the ability to use paths to name positions within the hierarchy,
    with fast local access relative to a selected map. 

    The iterator that this class implements is a pitererator,
    which is an iterator with pointer semantics.

    ///org The use of this class and PeersCursor are being deprecated.
      The plan is to use more conventional map iterator techniques.
      See PeersCursor.  Eliminate the use of that first.

    ///org Stop using this class's field-access methods.
      Use the MapEpiNode methods that these field-access methods call.
      This will mean that the callers will need to first call 
      getSelectedMapEpiNode() to get the value MapEpiNode.
      
    ///org The use of the term "persistent" is unfortunate.
      According to Wikipedia, that term is used to describe data structures
      which, when they are changed, retain 
      previous versions of the data structure.
      These are used extensively in applicative languages.
       
	  ///pos Should this be changed to support unsorted keys?
	    Possibly, but not immediately.  Underway
	    
	  ///pos Make PersistentNode able to return one of these
	    and eliminate absolute path support?
	    
	  Within each of the 2 major code groups, code is ordered by general use time.
	  
   	*/

	{

	  // Initialization.

	  // Constants.
	
		private final String EMPTY_STRING= "";
	
		
		// State, generally from most significant to least significant.

		protected final Persistent thePersistent; // Underlying dual-format storage structure.

    private String entryKeyString= EMPTY_STRING; // Key name String of 
      // presently selected entry in the outer map, the map being iterated.
      // If this is the EMPTY_STRING then no map entry is selected.
      // In this case the iterator position may be considered to be
      // before the beginning of the entries or after the end of the entries.
      // These are considered equivalent.

    // EpiNode data cursor variables.
    protected MapEpiNode parentMapEpiNode= null;
      // MapEpiNode which is the outer map which is being iterated.
    protected MapEpiNode childMapEpiNode= null;
      // MapEpiNode which is the value of the selected entry of the outer map. 
      // It is the inner map which contains various data fields of that entry. 


		public PersistentCursor( Persistent thePersistent ) // constructor
			{
				this.thePersistent= thePersistent;
				}


		// Service code.

	  public PersistentCursor createEntryInPersistentCursor()
	    /* This method creates a new entry in this PersistentCursor parent map 
	      with no particular key but an empty MapEpiNode as its value.
	      The key is low-value numerical key converted to a String,
	      no greater than the size of the map.
	      even though other keys in the map might not be numerical. 
	      The PeersCursor is positioned to the new entry.
	      */
	    { 
        theAppLog.debug("PersistentCursor.createEntryInPersistentCursor() called.");
        String newKeyString= parentMapEpiNode.createEmptyMapWithNewKeyString();
        setEntryKeyString( newKeyString ); // Point to the new entry.
	      return this;
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
        parentMapEpiNode= 
          thePersistent.getRootMapEpiNode().getOrMakeMapEpiNode(listKeyString);
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
          parentMapEpiNode.getNextString(entryKeyString); /// This is slow.
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
        parentMapEpiNode.removeV( entryKeyString ); // Remove present element
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
          this.childMapEpiNode= // cache the node at that position. 
              parentMapEpiNode.getOrMakeMapEpiNode(entryKeyString);
          else
          this.childMapEpiNode= null;
        return this.entryKeyString;
        }

    public MapEpiNode getSelectedMapEpiNode()
      /* Returns the MapEpiNode presently selected by the iterator,
        or null if no entry is selected.  */
      { 
        return childMapEpiNode; 
        }

		}
