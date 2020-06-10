package allClasses.multilink;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListMultiLink<
    E extends MultiLink<E> // Element type.
    >

  implements MultiLink<E>

  {


    MultiLink<E> get(E theE) { return theE; }

    // Instance variables.
  
      protected List<E> theListOfEs= // Set to empty,
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
