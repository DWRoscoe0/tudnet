package allClasses;

public class EpiString {

  public static String combine1And2WithNewlineString(
      String the1String,String the2String)
    {
      return combine1And2With3String(
          the1String,
          the2String,
          ",\n" // a line separator between them.
          );
      }

  public static String combine1And2With3String(
      String the1String, String the2String, String bridgeString)
    {
      String valueString;
    toReturn: {
      if (isAbsentB(the1String)) // If there is no string 1
        { valueString= the2String; break toReturn; } // return string 2.
      if (isAbsentB(the2String)) // If there is no string 2
        { valueString= the1String; break toReturn; } // return string 1.
      valueString= // Neither string is null so return a combination of both:
        the1String 
        + bridgeString
        + the2String; // 
    } // toReturn:
      return valueString;
    }


  protected static boolean isAbsentB(String theString)
    /* This method returns true if theString is null or "", 
     * false otherwise. 
     */
    {
      boolean valueB;
    toReturn: {
      if (null == theString) // If string is null
        { valueB= true; break toReturn; } // return true.
      if (theString.isEmpty()) // If non-null string is empty
        { valueB= true; break toReturn; } // return true.
      valueB= false; // Otherwise return false. 
    } // toReturn:
      return valueB;
    }
  
  }
