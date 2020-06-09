package allClasses.multilink;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListMultiLink<
    L // Link type.   //// Should this be ? extends MultiLink
    > 

  implements MultiLink<L>

  {
  
    // Instance variables.
  
      protected List<L> theListOfLs= // Set to empty,
          new ArrayList<L>(); // ArrayList.
      
      public ListMultiLink() // Constructor.
        { 
          }

      
    // interface MultiLink methods.
      
      @Override
      public boolean hasNoLinks() 
        {
          return (getLinkCountI() == 0);
          }

      @Override
      public int getLinkCountI() 
        {
          return theListOfLs.size();
          }

      @Override
      public L getLinkL(int indexI) 
        {
          L resultL;  // Allocating result space.

          if  // Handling index which is out of range.
            ( (indexI < 0) || (indexI >= theListOfLs.size()) )
            resultL= null;  // Setting result to null.
          else  // Handling index which is in range.
            resultL=   // Setting result to be child from...
              theListOfLs.get(   // ...L List...
                indexI  // ...at the desired position.
                );

          return resultL;
          }

      @Override
      public int getIndexOfLinkI(L theL) 
        {
          return theListOfLs.indexOf(theL);
          }

      public void addV(int indexI, L theLink)
        {
          theListOfLs.add(
              indexI,
              theLink
              );
          }

      public void removeV(int indexI)
        {
          theListOfLs.remove(
              indexI
              );
          }

      // Other methods.
      
      public Iterator<L> iterator() 
        {
          return theListOfLs.iterator();
          }

    }
