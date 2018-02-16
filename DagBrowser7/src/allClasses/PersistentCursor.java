package allClasses;

public class PersistentCursor 

  /* This class acts like a cursor into a Persistent data structure,
    which is used mainly as a set of linked lists.
    It implements a pitererator (an iterator with pointer semantics).
    Presently it assumes that persistent storage is 
    a named collection of linked lists which are not nested.
    A liked list looks like this:

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
		private final Persistent thePersistent;
		
		private String listNameString= null;
		private String listEntryPrefixString= null;
		private String listFirstKeyString= null; 
		private String listScanKeyString= null;
		private String listScanValueString= null;
		
		public PersistentCursor( Persistent thePersistent ) // constructor
			{
				this.thePersistent= thePersistent;
				}

		public void setListNameStringV( String listNameString )
		  /* This method sets the list to be scanned by
		    adding the ListNameString to the base path,
		    which is presently always the empty string for the root.
		    It also initializes variables used to scan the list,
		    starting with the first element, if any. 
		   	*/
			{
				this.listNameString= listNameString;
	  		listFirstKeyString= listNameString + "/first";
	  		setPiteratorKeyV(listFirstKeyString);
				}
		
		private void setPiteratorKeyV( String piteratorKeyString )
		  /* This method sets the list piterator key and dependencies.
		    The list name must have been set already.
		   */
			{
	  		listScanKeyString= piteratorKeyString;
	  		listScanValueString= 
	  				thePersistent.getDefaultingToBlankString( listScanKeyString );
	  		setPiteratorValueV( listScanValueString );
				}

		private void setPiteratorValueV( String listEntryNameString )
			/* This method sets the list piterator value and dependencies.
		    The list name must have been set already.
		   */
			{
			  if (listEntryNameString == null) listEntryNameString= "";
				if (listEntryNameString.isEmpty())
						listEntryPrefixString= null;
					else
						listEntryPrefixString=
								listNameString+"/entries/"+listEntryNameString+"/";
				Misc.noOp(); ///dbg
				}

		public String nextString()
		  // Advances the piterator and returns the name of the next element.
			{
			  if ( getEntryIDNameString() == null)
			  		setPiteratorKeyV( listFirstKeyString ); // Point to first element.
			  	else
			  		setPiteratorKeyV( // Point to next element.
			  				listEntryPrefixString + "next"
								);
			  return getEntryIDNameString();
				}

		public String getEntryIDNameString()
		  // This is a window into the piterator.
			{
				return listScanValueString;
				}
		
		public String getFieldString( String fieldNameString )
			{ 
				return thePersistent.getDefaultingToBlankString( 
						listEntryPrefixString + fieldNameString 
						);
				}
		
		}
