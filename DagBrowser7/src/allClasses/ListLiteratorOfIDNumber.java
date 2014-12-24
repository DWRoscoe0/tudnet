package allClasses;

import java.io.IOException;
import java.util.ListIterator;

import static allClasses.Globals.*;  // For appLogger;

public class ListLiteratorOfIDNumber 
  implements ListIterator<IDNumber> 
  
    /* This is a lazy-loading ListIterator.
      It is constructed by adding lazy loading capability to a ListIterator.

      The ListIterator was used because:
      * It can go forward and backward.
      * It can do replacement of elements.

      It mostly forwards method calls to the nested ListIterator,
      but in the case of the methods next() and previous() 
      it checks to see whether the IDNumber element retrieved 
      is not an IDNumber subclass, and if it is not then 
      it loads the MetaNode equivalent from the lazy-load state MetaFile 
      and stores that in place of the IDNumber before returning it.
      */

    {
      // Instance variables.

        MetaFileManager theMetaFileManager;

        private ListIterator<IDNumber> theListIteratorOfIDNumber;

        private DataNode theParentDataNode;  // For name lookup.

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

        private IDNumber checkLoadAndReplaceIDNumber( IDNumber inIDNumber )
          /* This method checks the inIDNumber and loads it unless
            it has already been loaded.
            It also replaces the current iterator element,
            assuming it to be the last read.
            If inIDNumber is a MetaNode instance then it returns inIDNumber.
            If it is an IDNumber instance then it goes to the MetaFile, 
            finds the MetaNode text associated with that IDNumber, 
            loads it into a MetaNode, and replaces the IDNumber reference
            in the ListIterator by a reference to the new MetaNode.
            */
          {
            IDNumber returnIDNumber=  // Set default result to raw input.
              inIDNumber; 

            { // Decoding all the pertinant conditions.
              if   // Doing nothing if already converted from IDNumber.
                ( inIDNumber.getClass() != IDNumber.class )
                ; // Doing nothing.
              /*
              else if  // Doing nothing if lazy loading disabled.
                ( ! MetaFile.getLazyLoadingEnabledB() ) 
                ; // Doing nothing.
              */
              else  // Doing conversion loading of IDNumber.
                returnIDNumber= ConvertIDNumber( inIDNumber );
              } // Decode all the pertinant conditions.
            return returnIDNumber;
            }

        private IDNumber ConvertIDNumber( IDNumber inIDNumber )
          /* This helper method tries to replace inIDNumber with 
            a lazy-loaded MetaNode equivalent.
            If it succeeds then it returns the loaded replacement value,
            otherwise it returns the original inIDNumber value.
            */
          { // Try to replace IDNumber with loaded MetaNode equivalent.
            try {
              inIDNumber=  // ...MetaNode equivalent...
                theMetaFileManager.getLazyLoadMetaFile().readAndConvertIDNumber(  // ...
                  inIDNumber,  // ...of IDNumber using...
                  theParentDataNode  // ...provided parent for lookup.
                  );
              }
            catch ( IOException TheIOException ) {
              appLogger.error( "ListLiteratorOfIDNumber.IOException." );
              // returnIDNumber already set to inIDNumber.
                // ??? Use error MetaNode instead?
              };
            set( // Replace the child by the loaded MetaNode.
              inIDNumber
              );
            return inIDNumber;
            } // Try to replace IDNumber with loaded MetaNode.


      // ListIterator methods that are forwarded, and 2 that do lazy-loading.

        @Override
        public void add(IDNumber inIDNumber) 
          {
            theListIteratorOfIDNumber.add(inIDNumber);
            }

        @Override
        public boolean hasNext() 
          {
            return theListIteratorOfIDNumber.hasNext();
            }  

        @Override
        public boolean hasPrevious() 
          {
            return theListIteratorOfIDNumber.hasPrevious();
            }  

        @Override
        public IDNumber next() 
          {
            IDNumber outIDNumber= theListIteratorOfIDNumber.next();
            outIDNumber= checkLoadAndReplaceIDNumber( outIDNumber );
            return outIDNumber;
            }  

        @Override
        public int nextIndex() 
          {
            return theListIteratorOfIDNumber.nextIndex();
            }  

        @Override
        public IDNumber previous() 
          {
            IDNumber outIDNumber= theListIteratorOfIDNumber.previous();
            outIDNumber= checkLoadAndReplaceIDNumber( outIDNumber );
            return outIDNumber;
            }  

        @Override
        public int previousIndex() 
          {
            return theListIteratorOfIDNumber.previousIndex();
            }  

        @Override
        public void remove() 
          {
            theListIteratorOfIDNumber.remove();
            }  

        @Override
        public void set(IDNumber inIDNumber) 
          {
            theListIteratorOfIDNumber.set(inIDNumber);
            }  

    }
