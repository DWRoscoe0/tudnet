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
          return (getCountI() == 0);
          }

      @Override
      public int getCountI() 
        {
          return theListOfEs.size();
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

      public void removeV(int indexI)
        {
          theListOfEs.remove(
              indexI
              );
          }
      
      public Iterator<E> iterator() 
        {
          return theListOfEs.iterator();
          }

    }
