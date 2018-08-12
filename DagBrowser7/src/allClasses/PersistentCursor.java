package allClasses;

import static allClasses.Globals.appLogger;

import java.util.NavigableMap;

public class PersistentCursor 

  /* This class acts like a cursor into a Persistent data structure.
    It implements a pitererator (an iterator with pointer semantics).

    ///enh Switching from iterating a kludgey linked list to
      iterating entries in sorted list in TreeMap of PersistingNode.
      This will be done in semi-parellel. 
    ///org The use of the term "persistent" is unfortunate.
      According to Wikipedia, that term is used to describe data structures
      which, when they are changes, retain 
      previous versions of the data structure.
      These are used extensively in applicative languages. 

    Presently a cursor can only traverse a linked list.
    A linked list looks like this:
	
			ListName/first=EntryName1
			
			ListName/entries/EntryName1/next=EntryName2
	    ListName/entries/EntryName1/FieldKey1=FieldValue1.1
	    ...
	    ListName/entries/EntryName1/FieldKeyN=FieldValue1.N
			
			ListName/entries/EntryName2/next=EntryName3
	    ListName/entries/EntryName2/FieldKey1=FieldValue2.1
	    ...
	    ListName/entries/EntryName2/FieldKeyN=FieldValue2.N
	    
	  Code in this file is ordered by general use time.
	  
   	*/

	{

	  // Initialization.

	  // Constants.
	
		private final String EMPTY_STRING= "";
	
		
		// State, generally from most significant to least significant.

		private final Persistent thePersistent; // Underlying storage structure.

		// The follow are mostly for path-based lists.
		private String listPathString= EMPTY_STRING;
			// Path name of list of interest.
		private String entryPrefixPathString= EMPTY_STRING;
			// Prefix of all entries in list of interest.
		private String firstPathString= EMPTY_STRING;
			// Key of name of first entry in list of interest.
		private String positionPathString= EMPTY_STRING;
			// Key of name of current entry in list of interest.
		private String positionValueString= EMPTY_STRING; 
		  // Value which is name of current entry in list of interest.

	// The following are mostly for the new TreeMap node-based lists.
    ////@SuppressWarnings("unused") ////?
		////private PersistingNode thePersistingNode;
    @SuppressWarnings("unused") ////?
		private NavigableMap<String,PersistingNode> theNavigableMap;
    private String navigableMapString;

		public PersistentCursor( Persistent thePersistent ) // constructor
			{
				this.thePersistent= thePersistent;
				}


		// Service code.

		public void setListPathV( String listPathString )
		  /* This method sets the name of the list to be scanned.  
		    It does this in 2 ways:
		   	* by adding the ListNameString to the base path,
			    which is presently always the empty string for the root; and
			  * by getting ready the interface NavigableMap<K,V>
			    which holds the list of entries.
			  It also initializes other variables used to scan the list,
			  a scan starting with the first element, if there is one. 
		   	*/
			{
				appLogger.debug(
						"PersistentCursor.setListPathV("+listPathString+") begins.");
				this.listPathString= listPathString;
	  		firstPathString= listPathString + "/first";
	  		
	  		//// new
	  		theNavigableMap= thePersistent.multilevelGetNavigableMap(
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
				appLogger.debug(
						"PersistentCursor.moveToFirstV() begins.");
	  		setPositionPathV(firstPathString); // Set position to first.
	  		
	  		navigableMapString= EMPTY_STRING;
				}

		private void setPositionPathV( String pathString )
			  /* This method sets the list position path and dependencies.
			   */
  		{
				appLogger.debug(
						"PersistentCursor.setPositionPathV("+pathString+") begins.");
	  		positionPathString= pathString; // Store position key.
	  		setPositionKeyV( 
	  				thePersistent.getDefaultingToBlankString( 
	  						positionPathString ));
	  		}

		private void setPositionKeyV( String keyString )
			  /* This method sets the list position path and dependencies.
			   */
  		{
				appLogger.debug(
						"PersistentCursor.setPositionKeyV("+keyString+") begins.");
	  		positionValueString= keyString;
	  		setEntriesPrefixFromV( positionValueString );
				}

		private void setEntriesPrefixFromV( String positionNameString )
			/* This method calculates and stores the prefix path of all list entries.
		    The list base path must have been set already.
		   */
			{
				appLogger.debug(
						"PersistentCursor.setEntriesPrefixFromV("
								+positionNameString+") begins.");
				if (positionNameString.isEmpty())
						entryPrefixPathString= null; // This should never happen.
					else
						entryPrefixPathString=
								listPathString+"/entries/"+positionNameString+"/";
				}

		public String nextWithWrapElementIDString()
		  /* This does the same as nextElementIDString() but
		    tries to wrap around to beginning of list if at the end, 
		    so it returns the empty string only if the list is empty.
	      */
			{
				String idString=  // Try moving to next and getting element ID.
						nextElementIDString();
				if ( idString.isEmpty() ) // If at end of list
					idString= nextElementIDString(); // try moving one more time.
				appLogger.debug(
						"PersistentCursor.nextWithWrapElementIDString(..) returning:"+idString);
			  return idString; // Return name of this position.
			    // If string is still empty then list must be empty.
				}

		public String oldNextElementIDString()
		  /* Advances the piterator and returns the ID of the next list entry.
		    If the result is the empty string then the piterator is positioned
		    on no element.
		   	*/
			{
			  if ( getElementIDString().isEmpty() ) // At end of list.
			  		////setPositionPathV( firstPathString ); // Point to first element.
			  		moveToFirstV();
			  	else // Not at end of list.
			  		setPositionPathV( // Point to next element.
			  				entryPrefixPathString + "next"
								);
				String idString= getElementIDString();
				appLogger.debug(
						"PersistentCursor.nextElementIDString() returning:"+idString);
			  return idString; // Return name of the new position.
				}

		public String nextElementIDString() ////
		  /* Advances the piterator and returns the ID of the next list entry.
		    If the result is the empty string then the piterator is positioned
		    on no element.
		   	*/
			{
		  	navigableMapString= // Advance to next position.
		  	  Nulls.toEmptyString(
		  	  		theNavigableMap.higherKey(
		  	  				navigableMapString));
		  	setEntriesPrefixFromV( navigableMapString );
				appLogger.debug( "PersistentCursor.nextElementIDString() returning:"
						+navigableMapString);
		  	return navigableMapString; // Return name of the new position.
				}

		public String getElementIDString()
		  /* Returns the ID of the present list element.
				Returns the empty String if the piterator is positioned
				at the end of the list, or if the list is empty
				*/
			{
				appLogger.debug(
						"PersistentCursor.getElementIDString() returning:"+positionValueString);
				return positionValueString;
				}
		
		public String getFieldString( String fieldNameString )
		  /* Returns the value of the field whose name is fieldNameString
		    in the present list element.
		    Though the fields within a list element 
		    are of the same type of structure as the elements within a list,
		    we make no attempt to iterate over them.
		    */
			{ 
				String fieldValueString= thePersistent.getDefaultingToBlankString( 
					entryPrefixPathString + fieldNameString 
					);
				appLogger.debug(
						"PersistentCursor.getFieldString( "+fieldNameString+" ) returning:"+fieldValueString);
				return fieldValueString;
				}
		
		
		// Finalization code: none.
		
		}
