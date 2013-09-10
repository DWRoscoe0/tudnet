package allClasses;

import java.util.Collection;
import java.util.Iterator;

public class Piterator<E> 
  
  /* This is an ierator with pointer semantics,
    which makes it more useful for passing into and out of methods.
    Iterators without pointer semantics are not useful in this way.
    It gets it functionality from a regular Iterator,
    but it is NOT compatible with Iterator, meaning
    it can be used which an Iterator is required.
    */

  { // class Piterator<E>

    // Instance variables.

      private Iterator<E> NestedIterator;  // Iterator acting as pointer.
      private E TheE;  // Element reference acting as the pointer window.

    // Constructors.

      public Piterator( Iterator<E> inNestedIterator
        //, int DbgI 
        )
        /* This constructor creates a Piterator from 
          an existing Iterator inNestedIterator.  
          inNestedIterator need not be initially positioned
          at its first element, but if it is then so will the Piterator.
          */
        {
          NestedIterator= inNestedIterator;  // Save nested Iterator.
          nextE();  // Initialize the pointer window by advancing the pointer.
          }

      public Piterator( Collection<E> inCollectionOfE
        // , int DbgI 
        )
        /* This constructor creates a Piterator from
          the Collection<E> inCollectionOfE.
          */
        {
          this( inCollectionOfE.iterator() );
          }

    // Overridden standard Iterator methods.

    // New Piterator methods.

      public E getE() 
        /* This returns a reference to the element in the pointer window.  
          If the returned value is null then there isn't any in the window,
          and there are no more elements to process.
          */
        {
          //Misc.DbgOut( "Piterator.getE()");
          return TheE;
          }

      public E nextE()
        /* This advances the pointer to the next element, if any,
          and sets the pointer window appropriately. */
        {
          //Misc.DbgOut( "Piterator.nextE()");
          if ( NestedIterator.hasNext() )
            TheE= NestedIterator.next();
            else
            TheE= null;
          return TheE;
          }

      public void removeV()  
        /* Does a remove by calling remove() in the nested iterator. */
        {
          //Misc.DbgOut( "Piterator.removeV()");
          NestedIterator.remove();  // Remove from nested iterator.
          this.nextE();  // Use local nextE() to adjust pointer window.
          }
      
    } // class Piterator<E>
