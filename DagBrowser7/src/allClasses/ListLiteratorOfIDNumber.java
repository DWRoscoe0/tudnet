package allClasses;

import java.io.IOException;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static allClasses.AppLog.theAppLog;

public class ListLiteratorOfIDNumber

  implements ListIterator<IDNumber> 
  
  /* This is a lazy-loading ListIterator of IDNumber,
    which is the superclass of the MetaNode.
    This class is constructed by 
    adding lazy loading functionality to a ListIterator.

    The ListIterator was used for underlying iterator functionality because:
    * It can go forward and backward.
    * It can do replacement and deletions of elements.

    It mostly forwards method calls to the nested ListIterator,
    but in the case of the methods next() and previous() 
    it checks to see whether the IDNumber element retrieved 
    is not an IDNumber subclass, and if it is not then 
    it loads the MetaNode equivalent from the lazy-load state MetaFile 
    and stores that in place of the IDNumber before returning it.
    
    This class uses ListIterator semantics, not Piterator (pointer) semantics.

    ///enh Methods which perform operations associated with the previous
      elements are implemented but simply throw an Error exception. 
    */

  {
	
    // Instance variables.

    MetaFileManager theMetaFileManager;

    private ListIterator<IDNumber> theListIteratorOfIDNumber; 
      // Nested iterator.
    private IDNumber cachedIDNumber= null; // If not null then this is
      // the node gotten from the last next() call to nested iterator.
    private DataNode theParentDataNode;  // For name lookup during loads.

    
    // Constructors.

    public ListLiteratorOfIDNumber(
        MetaFileManager theMetaFileManager,
        ListIterator<IDNumber> theListIteratorOfIDNumber, 
        DataNode theParentDataNode
        )
      /* This constructs a lazy-loading ListIterator from 
        a regular ListIterator.
        inParentMetaNode is used for name lookups if
        lazy-loading needs to be done.
        */
      {
        this.theMetaFileManager= theMetaFileManager;
        this.theListIteratorOfIDNumber= theListIteratorOfIDNumber;
        this.theParentDataNode= theParentDataNode;
        }

    
    // Method that does the node checking and loading.

    private IDNumber tryLoadingIDNumber( IDNumber inIDNumber )
      /* This helper method tries to replace inIDNumber with 
        a lazy-loaded MetaNode equivalent.
        If it succeeds then it returns the loaded replacement value,
        otherwise it returns the original inIDNumber value.
        It handles IOExceptions as load failures and returns
        the original IDNumber.
        */
      {
    	  IDNumber resultIDNumber;
        try {
          resultIDNumber=  // ...MetaNode equivalent...
              theMetaFileManager.getLazyLoadMetaFile().
              	readAndConvertIDNumber(  // ...
              		inIDNumber,  // ...of IDNumber using...
                  theParentDataNode  // ...provided parent for lookup.
                  );
          }
        catch ( IOException theIOException ) {
          theAppLog.error(
          	theIOException, "ListLiteratorOfIDNumber.tryLoadingIDNumber().");
      	  resultIDNumber= inIDNumber;
          };
      return resultIDNumber;
      }

    
    // ListIterator methods that are forwarded, and 2 that do lazy-loading.

    public void add(IDNumber inIDNumber) 
      {
        theListIteratorOfIDNumber.add(inIDNumber);
        }

    public boolean hasNext()
      /* This method does the standard hasNext() function, 
        using the nested iterator as its base data, but also
        * lazy loads any node that needs it,
        * replaces IDNumber place-holders by their loaded MetaNode equivalents,
        * ignores nodes that can't be loaded by skipping over them,
        * removes skipped IDNumber place-holders from the nested iterator.
       	*/
      {
      	  boolean resultB; IDNumber iterIDNumber; IDNumber loadedIDNumber;
    	  toReturn: { toSuccess: { toCache: { toLoaded: { toFail: { // labels. 
      	  while (true) { // Loop until node gotten or end of iterator reached.
            if (cachedIDNumber!=null) break toSuccess; // Have cached node.
      	  	if (! theListIteratorOfIDNumber.hasNext()) break toFail; // At end.
      	  	iterIDNumber= theListIteratorOfIDNumber.next(); // Get next node.
      	  	if (iterIDNumber.getClass() != IDNumber.class) // Iterator node is   
      	  		break toCache; // already a loaded node, not a place holder.
            loadedIDNumber= tryLoadingIDNumber(iterIDNumber); // Try loading.
            if (iterIDNumber!=loadedIDNumber) break toLoaded; // Load success.
            theListIteratorOfIDNumber.remove(); // Remove node that failed load.
      	  	} /* while(true) */ // Try for another node by looping.
        } /* toFail: */ resultB= false; break toReturn; // Return failure value.
        } // toLoaded:  // Success with loaded node.
          theListIteratorOfIDNumber.set(loadedIDNumber); // Store loaded node.
      	  iterIDNumber= loadedIDNumber;
        } /* toCache : */ cachedIDNumber= iterIDNumber; // Cache iterator node.
        } /* toSuccess: */ 	resultB= true; // Set Success return value.
        } /* toReturn: */ return resultB;
        }  

    public IDNumber next() 
      {
    	  toReturn: {
       		if ( cachedIDNumber != null ) // Element already cached. 
       			break toReturn; // Return the cached element.
       		if (hasNext()) // Use hadNext() to do any needed loading and caching. 
       			break toReturn; // Return newly loaded cached element.
          throw new NoSuchElementException(); // Load failure.
    	  } // toReturn:
	    		IDNumber resultIDNumber= // Copy cached element to result. 
	    				cachedIDNumber;
	  			cachedIDNumber= null; // Clear cache.
	        return resultIDNumber;
        }  

    public int nextIndex()
      {
    		unsupportedMethodV(); 
        return 0;
        }  

    public boolean hasPrevious() 
      {
    		unsupportedMethodV();
    		return false;
        }  

    public IDNumber previous() 
      {
    		unsupportedMethodV();
    		return null;
        }  

    public int previousIndex() 
      {
    		unsupportedMethodV(); 
    		return 0;
        }  

    private void unsupportedMethodV()
      { 
    		throw new Error(
    		  "ListLiteratorOfIDNumber.unsupportedMethodV()." 
    			);
      	}
    
    public void remove() 
      {
        theListIteratorOfIDNumber.remove();
        }  

    public void set(IDNumber inIDNumber) 
      {
        theListIteratorOfIDNumber.set(inIDNumber);
        }  

		}
