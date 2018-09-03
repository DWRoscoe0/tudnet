package allClasses;

import java.util.Collection;
import java.util.Iterator;

public class Piterator<E> 

  /* This is an iterator with pointer semantics, as in the C language,
    which makes it more useful for passing state into and out of methods.
    The regular Java Iterator is not useful in this way.
    In a Piterator, the pointer is considered to be on an elements.
    In an Iterator, the pointer is considered to be between elements.

    Piterator adapts the functionality of a regular Java Iterator 
    to creates its own functionality.
    */

  { // class Piterator<E>

    // Instance variables.

      private Iterator<E> nestedIterator;  // Iterator acting the pointer.
      private E theE;  // Element reference acting as the pointer window.

    // Constructors.

      public Piterator( Iterator<E> inNestedIterator )
        /* This constructor creates a Piterator from 
          an existing Iterator inNestedIterator.  
          inNestedIterator need not be initially positioned
          at its first element, but if it is then so will the Piterator.
          */
        {
          nestedIterator= inNestedIterator;  // Save nested Iterator.
          nextE();  // Initialize the pointer window by advancing the pointer.
          }

      public Piterator( Collection<E> inCollectionOfE )
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
          return theE;
          }

      public E nextE()
        /* This advances the pointer to the next element, if any,
          and sets the pointer window appropriately. */
        {
          if ( nestedIterator.hasNext() )
            theE= nestedIterator.next();
            else
            theE= null;
          return theE;
          }

      public E removeE()  
        /* Does a remove by calling remove() in the nested iterator. 
          It returns the new E, if any, that moves into the window
          as a result of the removal, or null if there is none.
          */
        {
          nestedIterator.remove();  // Remove from nested iterator.
          return this.nextE();  // Use local nextE() to adjust pointer window.
          }
      
    } // class Piterator<E>
