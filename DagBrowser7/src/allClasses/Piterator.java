package allClasses;

import java.util.Iterator;

public class Piterator<E> 

  /* This is an Iterator with pointer semantics.
    It makes it possible to pass iterators as parameters and
    return them as values.
    */

  {

    Iterator<E> NestedIterator;
    E TheE;
    
    public Piterator( Iterator<E> InNestedIterator )
      {
        NestedIterator= InNestedIterator;
        next();
        }

    public void next() 
      {
        if ( NestedIterator.hasNext() )
          TheE= NestedIterator.next();
          else
          TheE= null;
        }

    public E getE() 
      {
        return TheE;
        }
          
    public void remove()
      {
        NestedIterator.remove();
        }
      
    }
