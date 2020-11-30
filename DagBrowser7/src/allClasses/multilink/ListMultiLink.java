package allClasses.multilink;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListMultiLink<
    E extends MultiLink<E> // Element type.
    >

  implements MultiLink<E>

  {

    /* This class implements a fully functional MultiLink class
     * that is backed by an ArrayList.
     * It is meant to be referenced by a node, not inherited by a node.
     * It can hold any number of elements,
     * but to optimize space it should be dereferenced and replaced
     * by special purpose MultiLink instances if that number goes below 2. 
     */

    // Instance variables.

      protected List<E> theListOfEs= // Set initially to empty
          new ArrayList<E>(); // ArrayList.

      public ListMultiLink() // Constructor.
        { 
          }

    // interface MultiLink methods.
      
      @Override
      public boolean isEmptyB() 
        {
          return (getCountI() == 0); // Is empty if there are 0 elements.
          }

      @Override
      public int getCountI() 
        {
          return theListOfEs.size(); // Count is size of list.
          }

      @Override
      public E getE(int indexI) 
        {
          E resultL;  // Allocating result space.

          if  // Handling index which is out of range.
            ( (indexI < 0) || (indexI >= theListOfEs.size()) )
            resultL= null;  // Setting result to null.
          else  // Handling index which is in range.
            resultL=   // Setting result to be child from...
              theListOfEs.get(   // ...E List...
                indexI  // ...at the desired position.
                );

          return resultL;
          }

      @Override
      public int getIndexOfI(E theE) 
        {
          return theListOfEs.indexOf(theE);
          }

      public void addV(int indexI, E theE)
        {
          theListOfEs.add(
              indexI,
              theE
              );
          }

      public E removeE(int indexI)
        {
          return theListOfEs.remove(
              indexI
              );
          }

      public E setE(int indexI, E theE)
        {
          return theListOfEs.set(
              indexI,
              theE
              );
          }
      
      public Iterator<E> iterator() 
        {
          return theListOfEs.iterator();
          }
      
      public Iterable<E> getSelfIterable() //// new
        {
          Iterable<E> theIterableOfE= getIterableOfE();
          return theIterableOfE;
          }

      //// @Override
      public Iterable<E> getIterableOfE()
        // Returns the already Iterable List of links.
        {
          return theListOfEs;
          }

      public Iterable<E> getLinksIterable()  //// new
        { 
          return getIterableOfE(); 
          }

      }
