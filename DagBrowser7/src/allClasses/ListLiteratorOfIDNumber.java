package allClasses;

import java.io.IOException;
import java.util.ListIterator;

public class ListLiteratorOfIDNumber 
  implements ListIterator<IDNumber> 
  
    /* This is a lazy-loading ListIterator.
      It is constructed from a ListIterator.
      It mostly forwards method calls to the nested ListIterator,
      but in the case of the methods next() and previous() 
      it checks to see whether the IDNumber element retrieved 
      is not an IDNumber subclass, and if it is not then 
      it loads the MetaNode equivalent from the state lazy-load MetaFile 
      and stores that in place of the IDNumber before returning it.
      */

    {
      // Instance variables.

        private ListIterator<IDNumber> NestedListIteratorOfIDNumber;
        private MetaNode theParentMetaNode;  // Parent MetaNode...
          // ...for name lookup.

      // Constructors.

        public ListLiteratorOfIDNumber
          ( 
            ListIterator<IDNumber> inListIteratorOfIDNumber, 
            MetaNode inParentMetaNode // , int DbgI  // ???
            )
          /* This constructs a lazy-loading ListIterator from 
            a regular ListIterator.
            inParentMetaNode is used for name lookups if
            lazy-loading needs to be done.
            */
          {
            NestedListIteratorOfIDNumber= inListIteratorOfIDNumber;
            theParentMetaNode= inParentMetaNode;
            }

      // Method that does the node checking and loading.

        private IDNumber checkLoadAndReplaceIDNumber( IDNumber inIDNumber )
          /* This method checks the inIDNumber and loads it unless
            it has already been loaded.
            It also replaces the current iterator element,
            assuming it to be the last read.
            If InIDNumber is a MetaNode instance then it returns inIDNumber.
            If it is an IDNumber instance then it goes to the MetaFile, 
            finds the MetaNode text associated with that IDNumber, 
            loads it into a MetaNode, and replaces the IDNumber reference
            in the ListIterator by a reference to the new MetaNode.
            */
          {
            IDNumber returnIDNumber=  // Set default result to raw input.
              inIDNumber; 

            { // Decode all the pertinant conditions.
              if   // Already converted from IDNumber.
                ( inIDNumber.getClass() != IDNumber.class )
                {
                  //System.out.print( " #"+inIDNumber.getTheI() );
                  ; // Otherwise do nothing.
                  }
              /*
              else if  // Lazy loading disabled.
                ( ! MetaFile.getLazyLoadingEnabledB() ) 
                ;  // Do nothing.
              */
              else  // It is an IDNumber that needs conversion loading.
                returnIDNumber=   // Convert/load.
                  ConvertIDNumber( inIDNumber );
              } // Decode all the pertinant conditions.
            return returnIDNumber;
            }

        private IDNumber ConvertIDNumber( IDNumber inIDNumber )
          /* This helper method tries to replace IDNumber with 
            loaded MetaNode equivalent.
            */
          { // Try to replace IDNumber with loaded MetaNode equivalent.
            //Misc.DbgOut( 
            //  "ListLiteratorOfIDNumber.ConvertIDNumber(#"+
            //  inIDNumber.getTheI()+
            //  ") replacing." 
            //  );
            try {
              inIDNumber=  // ...MetaNode equivalent...
                MetaFile.getLazyLoadMetaFile().readAndConvertIDNumber(  // ...
                  inIDNumber,  // ...of IDNumber using...
                  theParentMetaNode  // ...provided parent for lookup.
                  );
              }
            catch ( IOException TheIOException ) {
              Misc.dbgOut( "ListLiteratorOfIDNumber.IOException." );
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
            NestedListIteratorOfIDNumber.add(inIDNumber);
            }

        @Override
        public boolean hasNext() 
          {
            return NestedListIteratorOfIDNumber.hasNext();
            }  

        @Override
        public boolean hasPrevious() 
          {
            return NestedListIteratorOfIDNumber.hasPrevious();
            }  

        @Override
        public IDNumber next() 
          {
            IDNumber outIDNumber= NestedListIteratorOfIDNumber.next();
            outIDNumber= checkLoadAndReplaceIDNumber( outIDNumber );
            return outIDNumber;
            }  

        @Override
        public int nextIndex() 
          {
            return NestedListIteratorOfIDNumber.nextIndex();
            }  

        @Override
        public IDNumber previous() 
          {
            IDNumber outIDNumber= NestedListIteratorOfIDNumber.previous();
            outIDNumber= checkLoadAndReplaceIDNumber( outIDNumber );
            return outIDNumber;
            }  

        @Override
        public int previousIndex() 
          {
            return NestedListIteratorOfIDNumber.previousIndex();
            }  

        @Override
        public void remove() 
          {
            NestedListIteratorOfIDNumber.remove();
            }  

        @Override
        public void set(IDNumber inIDNumber) 
          {
            NestedListIteratorOfIDNumber.set(inIDNumber);
            }  

    }
