package allClasses;

public class PersistentCursor 

  /* This class acts like a cursor into a Persistent data structure,
    which is used mainly as a set of linked lists.
    It implements a pitererator (an iterator with pointer semantics).
    Presently it assumes that persistent storage is 
    a named collection of linked lists which are not nested.
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

    ///enh Expand the use of this to simplify user code.
   	*/

	{

		private final String EMPTY_STRING= "";
		
		
		// State, generally from most significant to least significant.
		
		private final Persistent thePersistent; // Underlying storage structure.

		private String listNameString= EMPTY_STRING;
			// Name of list of interest.
		private String entryPrefixString= EMPTY_STRING;
			// Prefix of all entries in list of interest.
		private String firstKeyString= EMPTY_STRING;
			// Key of name of first entry in list of interest.
		private String positionKeyString= EMPTY_STRING;
			// Key of name of current entry in list of interest.
		private String positionValueString= EMPTY_STRING; 
		  // Value which is name of current entry in list of interest.

		public PersistentCursor( Persistent thePersistent ) // constructor
			{
				this.thePersistent= thePersistent;
				}

		public void setListNameV( String listNameString )
		  /* This method sets the name of the list to be scanned by
		    adding the ListNameString to the base path,
		    which is presently always the empty string for the root.
		    It also initializes other variables used to scan the list,
		    a scan starting with the first element, if there is one. 
		   	*/
			{
				this.listNameString= listNameString;
	  		firstKeyString= listNameString + "/first";
	  		setPositionKeyV(firstKeyString);
				}

		private void setPositionKeyV( String keyString )
		  /* This method sets the list position key and dependencies.
		   */
			{
	  		positionKeyString= keyString; // Store position key.
	  		positionValueString= // Cache the position value which is its name.
	  				thePersistent.getDefaultingToBlankString( positionKeyString );
	  		setEntriesPrefixFromV( positionValueString );
				}

		private void setEntriesPrefixFromV( String positionNameString )
			/* This method calculates and stores the prefix of all list entries.
		    The list name must have been set already.
		   */
			{
				if (positionNameString.isEmpty())
						entryPrefixString= null; // This should never happen.
					else
						entryPrefixString=
								listNameString+"/entries/"+positionNameString+"/";
				}

		public String nextWithWrapElementIDString()
		  /* This does the same as nextElementIDString() but 
		    only returns the empty string if the list is empty.
	      */
			{
				String idString=  // Try moving to next and getting element ID.
						nextElementIDString();
				if ( idString.isEmpty() ) // If at end of list
					idString= nextElementIDString(); // try moving one more time.
			  return idString; // Return name of this position.
			    // If string is still empty then list must be empty.
				}

		public String nextElementIDString()
		  /* Advances the piterator and returns the ID of the next list entry.
		    If the result is the empty string then the piterator is positioned
		    on the last element.
		   	*/
			{
			  if ( getElementIDString().isEmpty() ) // At end of list.
			  		setPositionKeyV( firstKeyString ); // Point to first element.
			  	else // Not at end of list.
			  		setPositionKeyV( // Point to next element.
			  				entryPrefixString + "next"
								);
			  return getElementIDString(); // Return name of the new position.
				}

		public String getElementIDString()
		  /* Returns the ID of the present list element.
				Returns the empty String if the piterator is positioned
				at the end of the list, or if the list is empty
				*/
			{
				return positionValueString;
				}
		
		public String getFieldString( String fieldNameString )
		  /* Returns the value of the field whose name is fieldNameString
		    in the present list element.
		    */
			{ 
				return thePersistent.getDefaultingToBlankString( 
						entryPrefixString + fieldNameString 
						);
				}
		
		}
