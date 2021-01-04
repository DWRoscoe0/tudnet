package allClasses.epinode;

import java.io.IOException;
import java.io.OutputStream;

import allClasses.Nulls;
import allClasses.RandomAccessInputStream;

public class ScalarEpiNode extends EpiNode

  /* This class approximately implements the YAML Scalar.
   * 
   * This class is a little kludgey with respect to 
   * ways of dealing with which characters 
   * are or not legal within a Scalar and how they are represented.  
   * This will probably change in the future.
   */

  {
    private String scalarString;
    
    public ScalarEpiNode(String scalarString)
      {
        Nulls.fastFailNullCheckT( scalarString );
        this.scalarString= scalarString;
        }

    public void writeV(OutputStream theOutputStream) 
        throws IOException
      /* Same as writeV(theOutputStream,0).  Indent of 0 is ignored.
       */
      {
        writeV(theOutputStream,0);
        }

    public void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException
      /* Writes the scalar string to theOutputSteam, 
       * enclosed by double quotes if needed.
       * indentI is ignored.
       */
      {
        boolean noQuotesNeededB= needsNoDoubleQuotesB(scalarString);
        if (noQuotesNeededB)
          theOutputStream.write(scalarString.getBytes()); // write bare String.
          else
          { // write quoted-escaped String.
            theOutputStream.write('"'); // Leading quote.
            int indexI = 0;
            while (true) { // Write entire string with any needed back-slashes.
              if (indexI >= scalarString.length()) // If no more characters
                break; // exit
              byte theB= (byte)scalarString.charAt(indexI); // Get next.
              if (needsBackSlashB(theB)) // Write back-slash if needed.
                theOutputStream.write('\\');
              theOutputStream.write(theB);
              indexI++;
              }
            theOutputStream.write('"'); // Trailing quote.
            }
        }

    private static boolean needsNoDoubleQuotesB(String theString)
      /* Returns true if theString contains no special characters
       * that require quoting. 
       */
      {
        boolean resultB= true; // Assume no quotes needed until we find need.
        int indexI = 0;
        while (true) { // For up to the entire string...
          if (indexI >= theString.length()) // If no more characters
            break; // exit with true result.
          byte theB= (byte)theString.charAt(indexI); // Get next character.
          if (! needsNoDoubleQuotesB(theB)) // If it needs quoting
            { resultB= false; break; } // set false result and exit.
          indexI++; // Increment character index.
          }
        return resultB;
        }

    public String extractFromEpiNodeString(int indexI) 
        throws IOException 
      { return scalarString; }
    
    public void storeStringV(String scalarString) 
      {
        this.scalarString= scalarString;
        }

    public String toRawString() 
      /* This method provides a way to access the raw string value of
       * Scalars without quotes or escape characters.
       * ScalarEpiNode redefines this to return its String value.
       */
      { 
        return scalarString; 
        }
    
    public static ScalarEpiNode tryScalarEpiNode( 
          RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse a ScalarEpiNode 
        (YAML subset scalar string) from theRandomAccessInputStream.
        If successful then it returns the ScalarEpiNode 
        and the stream is moved past the scalar characters,
        but whatever terminated the scalar remains to be read.
        The stream is moved past the last scalar character, but no further.
        If not successful then this method returns null 
        and the stream position is unchanged.
        */
      {
          ScalarEpiNode theScalarEpiNode;
        goReturn: {
          theScalarEpiNode= 
              tryUnquotedScalarEpiNode( theRandomAccessInputStream );
          if (theScalarEpiNode != null) break goReturn;
          theScalarEpiNode= 
              tryQuotedScalarEpiNode( theRandomAccessInputStream );
        } // goReturn:
          return theScalarEpiNode; // Return result.
        }
    
    private static ScalarEpiNode tryUnquotedScalarEpiNode( 
          RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse an unquoted ScalarEpiNode.  */
      {
          ScalarEpiNode theScalarEpiNode= null;
          byte theByte;
          String accumulatorString= ""; // Clear character accumulator.
        readLoop: { while (true) {
          int positionI= theRandomAccessInputStream.getPositionI();
          toAppendAcceptedChar: {
            theByte= theRandomAccessInputStream.readB();
            if (Character.isLetterOrDigit(theByte)) // Common character shortcut.
              break toAppendAcceptedChar;
            if (needsNoDoubleQuotesB(theByte)) // Anything else.
              break toAppendAcceptedChar;
            theRandomAccessInputStream.setPositionV(positionI);
              // Restore previous stream position.
            ///opt Alternative way to reject final character only, 
            /// outside of loop: setPositionV(getPositionI()-1);
            break readLoop; // Go try to return what's accumulated so far.
            } // toAppendAcceptedChar:
          accumulatorString+= (char)theByte; // Append byte to accumulator.
          }
        } // readLoop: 
          if (accumulatorString.length() != 0) // Reject 0-length strings.
            theScalarEpiNode= 
              new ScalarEpiNode(accumulatorString); // Override null result.
          return theScalarEpiNode; // Return result.
        }
    
    private static ScalarEpiNode tryQuotedScalarEpiNode( 
          RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse a quoted ScalarEpiNode.  */
      {
          ScalarEpiNode theScalarEpiNode= null;
          int byteI;
          int initialPositionI= theRandomAccessInputStream.getPositionI();
        goReturn: {
          byteI= theRandomAccessInputStream.read();
          if ( '"'!=byteI ) break goReturn; // Exit if no leading quote.
          String accumulatorString= ""; // Clear String character accumulator.
        readLoop: while (true) {
            byteI= theRandomAccessInputStream.read();
            if ( '"'==byteI ) break readLoop; // Exit if trailing quote found.
            if ('\\' == byteI) // Is escape character replace with next byte.
              byteI= theRandomAccessInputStream.read();
            accumulatorString+= (char)byteI; // Append byte to accumulator.
        } // readLoop: 
          if (accumulatorString.length() == 0) // Reject 0-length strings? 
            break goReturn;
          theScalarEpiNode= 
              new ScalarEpiNode(accumulatorString); // Override null result.
        } // goReturn:
          if (theScalarEpiNode == null) // Rewind stream if no result.
            theRandomAccessInputStream.setPositionV(initialPositionI);
          return theScalarEpiNode; // Return result.
        }

    private static boolean needsNoDoubleQuotesB(byte theC)
      /* Returns true if character in theC does not need to be
       * inside a double-quoted string in a Scalar name. 
       */
      {
        return !
          (
            cIsInStringB(theC, " ,:={}")
            ||
            needsBackSlashB(theC)
            );
        }

    private static boolean needsBackSlashB(byte theB)
      /* Returns true if character in theB needs to be preceded by
       * '\' when in a quoted string in a Scalar name.
       * The end-of-stream indicator (-1) is included
       * as a kludge to prevent infinite loop elsewhere.
       */
      {
        return 
            cIsInStringB(theB, "\\\"")
            ||
            Character.isISOControl(theB)
            ||
            (theB == -1) // Include end-of-stream.
            ;
        }

    private static boolean cIsInStringB(byte theB,String theString)
      /* Returns true if character in theC is in theString, false otherwise.
       * It does this by searching for the position of theC in theString.
       * If the position index is >= 0, then it is present.
       */
      {
        return (
            theString // In this String
              .indexOf(theB) // find the position of theB
            >= // and if the returned position is greater than
            0 // zero
            ); // then the character was found.
        }

    public String getString()
      /* Returns the String which represents the value of the scalar.  */
      { return scalarString; }

    public boolean equals(Object otherObject) 
      // This is the standard equals() method.  
      {
        boolean resultB;
        returnResult: { returnTrue: { returnFalse: {
          if (this == otherObject) break returnTrue; 
          if (this.getClass() != otherObject.getClass()) break returnFalse; 
          ScalarEpiNode otherScalarEpiNode= // Create field-access variable.
            (ScalarEpiNode)otherObject;
          if ( ! this.scalarString.equals(otherScalarEpiNode.scalarString) )
            break returnFalse;
          break returnTrue;
        } // returnFalse: 
          resultB= false; break returnResult;
        } // returnTrue: 
          resultB= true; break returnResult;
        } // returnResult: 
          return resultB;
        }

    public int hashCode() 
      // This is the standard hashCode() method.  
      {
        return  scalarString.hashCode(); // Returning hash of the only field.
        }
    
    } // ScalarEpiNode
