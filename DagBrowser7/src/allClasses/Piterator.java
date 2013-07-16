package allClasses;

import java.util.Iterator;

public class Piterator<E> 

  /* This is a crude Iterator with pointer semantics.
    It does not have all regular Iterator functionality,
    but it has enough to be useful.
    Also has pointer semantics, which makes it more useful for 
    passing into and out of methods.
    Regular Iterators are not useful in this way.
    */

  { // class Piterator.

    private Iterator<E> NestedIterator;  // Iterator acting as pointer.
    private E TheE;  // Element reference acting as the pointer window.
    
    public Piterator( Iterator<E> InNestedIterator )
      /* This constructor creates a Piterator from 
        the existing Iterator InNestedIterator.  
        InNestedIterator need not be initially positioned
        at its first element, but if it is then so will the Piterator.
        */
      {
        NestedIterator= InNestedIterator;  // Save nested Iterator.
        next();  // Initialize the pointer window by advancing the pointer.
        }

    public void next()
      /* This advances the pointer to the next element, if any,
        and sets the pointer window appropriately. */
      {
        if ( NestedIterator.hasNext() )
          TheE= NestedIterator.next();
          else
          TheE= null;
        }

    public E getE() 
      /* This returns a reference to the element in the pointer window.  
        If the returned value is null then there isn't any in the window,
        and there are no more elements to process.
        */
      {
        return TheE;
        }
          
    public void remove()  
      /* Does a remove by passing it to the nested iterator. */
      {
        NestedIterator.remove();  // Remove from nested iterator.
        this.next();  // Use local next() to set pointer window.
        }
      
    } // class Piterator.
