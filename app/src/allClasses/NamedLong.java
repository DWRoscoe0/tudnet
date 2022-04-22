package allClasses;

public class NamedLong extends NamedLeaf implements LongLike

{
  /* This class represents a long.
   * When it is displayed, it is accompanied by
   * the change from the previous value if it was defined.
   * Long.MIN_VALUE is used to indicate an undefined value.
   */
  protected long valueL;
  protected long previousValueL;

  public NamedLong( // Constructor.
      String nameString) 
  {
    this(nameString,Long.MIN_VALUE);
  }

  public NamedLong( // Constructor.
      String nameString, long valueL) 
  {
    super.setNameV(nameString);
    this.valueL = valueL;
    this.previousValueL= Long.MIN_VALUE;
  }

  public synchronized long addDeltaL(long deltaL)
  /*
   * This method does nothing if deltaL is 0. Otherwise it adds deltaL to the
   * value and returns the new value. 
   * It also reports whether there was a change.
   */
  {
    setValueL(this.valueL + deltaL); // Adding delta to old value.
    return valueL; // Returning possibly different value.
  }

  public synchronized long setValueL(final long newL)
  /*
   * This method does nothing if the new value newL is the same value as the
   * present value of this NamedLong. 
   * Otherwise it sets sets newL as the new value
   * and returns the old unchanged value. 
   * It also reports whether there was a change.
   */
  {
    boolean changedB;
    long resultL;
    { // Set the new value if different from old one.
      resultL = valueL; // Save present value for returning.
      changedB = newL != valueL;
      if (changedB) // Set new value if it's different.
      {
        previousValueL= valueL; // Save present value as previous value.
        valueL = newL; // Save new value ad present value.
      }
    }
    if (changedB) { // If value changed
      signalChangeOfSelfV(); // then report change of this node.
    }
    return resultL; // Returning old before-changed value.
  }

  public String getContentString() // DataNode interface method.
  {
    return

        (Long.MIN_VALUE == previousValueL) // If previous value was undefined

        ? String.format( // then return converted value only
            "%+d", valueL)

        : String.format( // otherwise return converted value and delta.
            "%+d [%+d]", valueL, valueL-previousValueL)

        ;
    }

  public String toString() // NamedLong breakpoint hook method.
  {
    String resultString= super.toString();
    /// System.out.print("NamedLong:"+resultString+";"); 
    return resultString;
  }

  public synchronized long getValueL() {
    return valueL;
  }

}
