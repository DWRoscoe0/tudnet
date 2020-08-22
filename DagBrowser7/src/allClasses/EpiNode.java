package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import static allClasses.SystemSettings.NL;

public abstract class EpiNode 

  /* This is the base class for 
    classes meant to represent YAML-like data.
    It supports scalars, sequences, and maps, though not fully. 
    It does not support null values. 
    Subclasses follow this class.
    
    Most of the code here deals with the Flow syntax style, 
    which uses braces to indicate structure.  This code is mostly working.
    Some of the code here deals with the Block syntax style, 
    which uses indentation to indicate structure.  
    This code is less complete, but is complete enough to be useful.
    Flow and Block syntaxes can not be mixed now, but may be later.

    ///enh Change to immediately cast bytes into characters
    to make debugging [parsers] easier by eliminating the need
    to convert integers to characters.
     
    ///enh If a RandomAccessReader abstract class is created,
    similar to RandomAccessInputStream, then the parsers in this file
    could be rewritten to be able to deal with characters instead of bytes.
    */

  {
  
    abstract public String extractFromEpiNodeString(int indexI) 
        throws IOException;
      /* This method tries to extract the String 
        whose index is indexI from this EpiNode. 
        If it succeeds it returns the String.  If it fails it returns null, 
        meaning there is no data at the index position requested.
        The mapping between index values and Strings in the EpiNode 
        is complex, depends on the EpiNode, and may be temporary.  

        ///tmp This method is meant to act as a temporary bridge between 
        accessing data by position and accessing data by name.
        Because of this, and the fact that the methods are temporary,
        error reporting is crude, just enough for debugging and 
        moving on to the next development phase.
        */


    public EpiNode getEpiNode(int indexI)
      /* This method returns the element EpiNode at index indexI,
        In this base class, it always returns null and logs an error.
        */
      {
        theAppLog.error( 
            "EpiNode.getEpiNode(int): base class should not be called.");
        return null;
        }

    public String toString()
      /* This method returns the String equivalent of this EpiNode
        converted to Flow style text.
        */
      {
        ByteArrayOutputStream theByteArrayOutputStream= 
            new ByteArrayOutputStream();
        try {
            writeV(theByteArrayOutputStream);
          } catch(IOException theIOException) {
            throw new RuntimeException(
              "Should not happen because writing to storage.", 
              theIOException);
          }
        String resultString= 
            new String(theByteArrayOutputStream.toByteArray());
        return resultString;
        }

    public String toString(int indentI)
      /* This method returns the String equivalent of this EpiNode
        converted to Block style text, starting at indentation indentI.
        */
      {
        ByteArrayOutputStream theByteArrayOutputStream= 
            new ByteArrayOutputStream();
        try {
            writeV(theByteArrayOutputStream, indentI);
          } catch(IOException theIOException) {
            throw new RuntimeException(
              "Should not happen because writing to storage.", 
              theIOException);
          }
        String resultString= 
            new String(theByteArrayOutputStream.toByteArray());
        return resultString;
        }

    public String toRawString()
      /* This method provides a way to access the raw string value of
       * Scalars without quotes or escape characters.
       * ScalarEpiNode redefines this to return its String value.
       */
      { 
        return "(RAW-STRING)"; 
        }
    
    public abstract void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException;
      /* Writes this EpiNode to theEpiOutputStream using Block style.
        indentI is the indent level.
        */

    public abstract void writeV(OutputStream theOutputStream) 
        throws IOException;
      /* Writes this EpiNode to theEpiOutputStream using Flow style,
        meaning no new-lines and now indenting.
        */

    public static EpiNode tryEpiNode(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
    /* This method tries to parse an EpiNode.
      It returns the node if the parse is successful, null otherwise.
      It tries parsing node types in the following order:
      * SequenceEpiNode
      * MapEpiNode
      * ScalarEpiNode
     */
    { 
        EpiNode resultEpiNode= null; 
      toReturn: {
        resultEpiNode= 
            SequenceEpiNode.trySequenceEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;
        resultEpiNode= MapEpiNode.tryMapEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;
        resultEpiNode= 
            ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
      } // toReturn:
        return resultEpiNode;
      }

    public static EpiNode tryBlockEpiNode(
        RandomAccessInputStream theRandomAccessInputStream, int minIndentI )
      throws IOException
    /* This method tries to parse an EpiNode from theRandomAccessInputStream.
      It looks for the block aka indented flow syntax.
      indentI is the minimum indentation level for nested structures, 
      like maps. 
      
      This method returns the node if the parse is successful, null otherwise.
      It tries parsing node types in the following order:
      * ScalarEpiNode
      * MapEpiNode (to be added)
      This method does not support sequences and map keys may be scalars only.
      */
    {
        EpiNode resultEpiNode= null; 
      toReturn: {
        resultEpiNode= 
            ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;
        resultEpiNode= MapEpiNode.tryBlockMapEpiNode(
            theRandomAccessInputStream, minIndentI);
      } // toReturn:
        return resultEpiNode;
      }
        
    protected static void newLineAndIndentV(
        OutputStream theOutputStream, int indentI)
      throws IOException
      {
        theOutputStream.write(NL.getBytes()); // Write a newline.
        while (indentI > 0) {
          theOutputStream.write(" ".getBytes()); // Write a space.
          indentI--; // Down count.
          }
        }
    
    public static boolean tryByteB(
        RandomAccessInputStream theRandomAccessInputStream, int desiredByteI) 
      throws IOException
      /* Reads a byte from theRandomAccessInputStream 
        and compares it to desiredByteI.
        If they are equal it returns true, otherwise false.
        Failure can happen when either the byte read 
        is not the desired byte or if there is no byte available.
        The stream advances only if a read byte is the desired one.
        */
      /* Tries to read desiredByteI from the stream.
        This is like getByteB(..) except that the stream position
        is not changed if desiredByteI can not be read from the stream.
        */
      {
        int positionI= 
            theRandomAccessInputStream.getPositionI(); // Save stream position.
        boolean successB= // Read and test byte.
            getByteB(theRandomAccessInputStream,desiredByteI);
        if ( ! successB ) // If failure, rewind stream position.
          theRandomAccessInputStream.setPositionV(positionI);
        return successB;
        }

    public static boolean getByteB(
        RandomAccessInputStream theRandomAccessInputStream, int desiredByteI) 
      throws IOException
      /* Reads a byte from theRandomAccessInputStream and 
        compares it to desiredByteI.
        If they are equal it returns true, otherwise false.
        Failure can happen when either the byte read 
        is not the desired byte or if there is no byte available.
        The stream advances whether or not a read byte is the desired one.
        */
      {
        int byteI= theRandomAccessInputStream.read(); // read the byte
        boolean successB= // Test byte for correctness.
          (byteI == desiredByteI); // Fails if byteI is -1 or not desired byte.
        return successB;
        }

    public MapEpiNode tryOrLogMapEpiNode()
      /* This method acts the same as tryMapEpiNode() except that
       * it logs an error if it can not return a MapEpiNode.
       */
      {
        MapEpiNode theMapEpiNode= tryMapEpiNode();
        if (null == theMapEpiNode)
          theAppLog.debug("MapEpiNode.tryOrLogMapEpiNode() not MapEpiNode.");
        return theMapEpiNode;
        }

    public MapEpiNode tryMapEpiNode()
      /* This method returns the (this) reference if this is a MapEpiNode,
        null otherwise.
        * This method returns null from this class.
        * MapEpiNode will override the null with its this reference.
        */
      {
        return null;
        }

    } // class EpiNode

class ScalarEpiNode extends EpiNode

  /* This class approximately implements the YAML Scalar.
   * 
   * This class is a little kludgey with respect to 
   * ways of dealing with which characters 
   * are or not legal within a Scalar and how they are represented.  
   * This will probably change in the future.
   */

  {
    private String scalarString;
    
    ScalarEpiNode(String scalarString)
      {
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
          byte theB;
          String accumulatorString= ""; // Clear character accumulator.
        readLoop: { while (true) {
          int positionI= theRandomAccessInputStream.getPositionI();
          toAppendAcceptedChar: {
            theB= theRandomAccessInputStream.readB(); //// loss of precision.
            if (Character.isLetterOrDigit(theB)) // Common character shortcut.
              break toAppendAcceptedChar;
            if (needsNoDoubleQuotesB(theB)) // Anything else.
              break toAppendAcceptedChar;
            theRandomAccessInputStream.setPositionV(positionI);
              // Restore previous stream position.
            ///opt Alternative way to reject final character only, 
            /// outside of loop: setPositionV(getPositionI()-1);
            break readLoop; // Go try to return what's accumulated so far.
            } // toAppendAcceptedChar:
          accumulatorString+= (char)theB; // Append byte to accumulator.
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
          if (accumulatorString.length() == 0) // Reject 0-length strings?//// 
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

class SequenceEpiNode extends EpiNode

  /* This class implements a YAML sequence, flow style only.
   
    This class was used for a while to encode packet data, 
    but it is no longer used.
   */

  {
    private ArrayList<EpiNode> theListOfEpiNode; 

    private SequenceEpiNode(ArrayList<EpiNode> theListOfEpiNode)
      {
        this.theListOfEpiNode= theListOfEpiNode;
        }

    public void writeV(OutputStream theOutputStream) 
        throws IOException
      { 
        boolean afterElementB= false; // At first no comma need be written.
        theOutputStream.write("[".getBytes()); // Introduce sequence.
        for (EpiNode elementEpiNode : theListOfEpiNode) // Write all elements.
          { // Write one element possibly preceded by comma.
            if (afterElementB) // Has an element been written yet?
              theOutputStream.write(",".getBytes()); // Yes, write comma.
            elementEpiNode.writeV(theOutputStream); // Output element
            afterElementB= true;
            }
        theOutputStream.write("]".getBytes()); // Terminate sequence.
        }

    public void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException
      { 
        for (EpiNode elementEpiNode : theListOfEpiNode)
          {
            EpiNode.newLineAndIndentV(theOutputStream, indentI);
            theOutputStream.write("- ".getBytes()); // Introduce element.
            elementEpiNode.writeV( // Output element
                theOutputStream, 
                indentI + 2 // with further indenting of element components.
                );
            }
        }

    public String extractFromEpiNodeString(int indexI) 
        throws IOException
      /* See base class for documentation.  */
      { 
        String elementString= null; // Set default result to indicate failure.
        EpiNode elementEpiNode= getEpiNode(indexI);
        if (elementEpiNode != null) // If got element node 
          elementString= elementEpiNode.toString(); // extract string.
        return elementString;
        }

    public EpiNode getEpiNode(int indexI)
      /* This method returns the element EpiNode at index indexI,
        or null if the index is out of range.
        */
      {
        return 
          ( (indexI >= 0) && (indexI < theListOfEpiNode.size())) // In range?
            ? theListOfEpiNode.get(indexI) // Yes, so return stored value.
            : null; // No, so return null.
        }

    public static SequenceEpiNode trySequenceEpiNode( 
        RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse a SequenceEpiNode 
        (YAML sequence of scalars) from theRandomAccessInputStream.
        If successful then it returns the SequenceEpiNode
        and the stream is moved past the sequence characters,
        but whatever terminated the SequenceEpiNode remains to be read.
        If not successful then this method returns null 
        and the stream position is unchanged.
        */
      {
          SequenceEpiNode returnSequenceEpiNode= null; // Set failure result.
          ArrayList<EpiNode> resultListOfEpiNodes= null;
          int initialStreamPositionI= 
              theRandomAccessInputStream.getPositionI();
        toReturn: { toNotASequence: {
          if (! getByteB(theRandomAccessInputStream, '[')) 
            break toNotASequence;
          resultListOfEpiNodes=  // Always succeeds.
              getListOfEpiNodes(theRandomAccessInputStream); 
          if (! getByteB(theRandomAccessInputStream, ']')) 
            break toNotASequence;
          returnSequenceEpiNode= // We got everything needed.  Create result. 
              new SequenceEpiNode(resultListOfEpiNodes);
          break toReturn;
        } // toNotASequence: // Coming here means parse of sequence failed.
          theRandomAccessInputStream.setPositionV(initialStreamPositionI);
            // Restore initial stream position.
        } // toReturn:
          return returnSequenceEpiNode; // Return result.
        }

    protected static ArrayList<EpiNode> getListOfEpiNodes(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
    /* This method parses and returns a List of 
      0 or more elements of a sequence of scalar nodes.  
      It always succeeds, though it might return an empty list.
      The stream is advanced past all characters that were processed,
      which might be none if the returned list is empty.
      */
    {
        int preCommaPositionI=0;
        boolean gotCommaB= false; // Becomes true when comma seen.
        ArrayList<EpiNode> resultListOfEpiNodes= 
            new ArrayList<EpiNode>(); // Create initially empty result list.
      toReturn: {
        while (true) { // Accumulating list elements until sequence ends.
          EpiNode theEpiNode=  // Try getting a list element.
              EpiNode.tryEpiNode(theRandomAccessInputStream);
          if (! gotCommaB) // Comma not gotten yet so we need first element
            { if (theEpiNode == null) // but there is no first element
                break toReturn; // so exit now with an empty list.
              }
            else // Comma was gotten so we need a non-first element.
            { if (theEpiNode == null) { // but there was no element so
                theRandomAccessInputStream.setPositionV( // rewind stream
                    preCommaPositionI); // to before comma.
                break toReturn; // and exit now with a non-empty list.
                }
              }
          resultListOfEpiNodes.add(theEpiNode); // Append element to list.
          preCommaPositionI= theRandomAccessInputStream.getPositionI();
          if (! tryByteB(theRandomAccessInputStream,',')) // Exit if no comma.
            break toReturn;
          gotCommaB= true; // Got comma, so record it.
          } // while(true)
      } // toReturn:
        return resultListOfEpiNodes;
      }

    } // SequenceEpiNode 

class MapEpiNode extends EpiNode 

  /* Note, to avoid ConcurrentModificationException,
    methods that iterate over or modify the object are synchronized.
    */

  {
    private LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap; // Map data. 
      // Will reference map with default of insertion-order.  
      // Rejected access-order map.  

    
    // Methods that output to OutputStreams.
    
    public synchronized void writeV(OutputStream theOutputStream) 
        throws IOException
      { 
        Map.Entry<EpiNode,EpiNode> scanMapEntry= null;
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
            theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
            theSetOfMapEntrys.iterator();
        boolean afterElementB= false; // Initially no comma need be written.
        theOutputStream.write("{".getBytes()); // Introduce map.
        while(true) { // Iterate over all entries.
          if (! entryIterator.hasNext()) // More entries? 
            break; // No, so exit.
          if (afterElementB) // If an element been written
            theOutputStream.write(",".getBytes()); // write separating comma.
          scanMapEntry= entryIterator.next(); // Yes, get current entry.
          scanMapEntry.getKey().writeV(theOutputStream); // Write key.
          theOutputStream.write(":".getBytes()); // Write key-value separator.
          scanMapEntry.getValue().writeV(theOutputStream); // Write value.
          afterElementB= true;
          }
        theOutputStream.write("}".getBytes()); // Terminate sequence.
        }

    public synchronized void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException
      { 
        Map.Entry<EpiNode,EpiNode> scanMapEntry= null;
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
            theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
            theSetOfMapEntrys.iterator();
        while(true) { // Iterate over all entries.
          if (! entryIterator.hasNext()) // More entries? 
            break; // No, so exit.
          EpiNode.newLineAndIndentV(theOutputStream, indentI);
          scanMapEntry= entryIterator.next(); // Yes, get current entry.
          scanMapEntry.getKey().writeV(theOutputStream, indentI); // Write key.
          theOutputStream.write(": ".getBytes()); // Write key-value separator.
          scanMapEntry.getValue().writeV( // Write value.
              theOutputStream, 
              indentI + 2); // Indent components, if any, here.
          }
        }


    // Methods that input from InputStreams.

    public static MapEpiNode tryMapEpiNode( 
        RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse a MapEpiNode (YAML map) 
        from theRandomAccessInputStream.
        If successful then it returns the MapEpiNode
        and the stream is moved past the map characters,
        but whatever terminated the MapEpiNode remains to be read.
        If not successful then this method returns null 
        and the stream position is unchanged.

        
        Parsing maps is tricky because, though they contain entries,
        and entries are always parsed as if a single entity, 
        entries do not exist outside of maps.
        Only their component key and value exist outside of maps.
        */
      {
          MapEpiNode resultMapEpiNode= null; // Set default failure result.
          LinkedHashMap<EpiNode,EpiNode> theLinkedHashMapOfEpiNode= null;
          int initialStreamPositionI= 
              theRandomAccessInputStream.getPositionI();
        toReturn: { toNotAMap: {
          if (! getByteB(theRandomAccessInputStream, '{')) break toNotAMap;
          theLinkedHashMapOfEpiNode=  // Always succeeds.
              getLinkedHashMap(theRandomAccessInputStream); 
          if (! getByteB(theRandomAccessInputStream, '}')) break toNotAMap;
          resultMapEpiNode= // We got everything needed so create result. 
              new MapEpiNode(theLinkedHashMapOfEpiNode);
          break toReturn;
        } // toNotAMap: // Coming here means parse of a complete map failed.
          theRandomAccessInputStream.setPositionV( // Restore position.
              initialStreamPositionI);
        } // toReturn:
          return resultMapEpiNode; // Return result.
        }

    protected static LinkedHashMap<EpiNode,EpiNode> getLinkedHashMap(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
    /* This method parses and returns a LinkedHashMap of  
      0 or more <MapEpiNode,MapEpiNode> <key,value> map elements.
      It always succeeds, though it might return an empty map
      if no parse-able map entry was found.
      The stream is advanced past all characters 
      that were processed into the map returned without error.
      which might be none if the returned map is empty.
      It allows keys to have null values, which can be used to implement sets. 
      */
    {
        int preCommaPositionI=0;
        boolean gotCommaB= false; // Becomes true when comma seen.
        LinkedHashMap<EpiNode,EpiNode> resultLinkedHashMap= 
          new LinkedHashMap<EpiNode,EpiNode>(); // Create initially empty map.
      toReturn: {
        EpiNode keyEpiNode= null; // If null then map entry is not valid.
        EpiNode valueEpiNode= null; // Optional value, null for now.
        while (true) { // Accumulating map entries until they end.
            int preMapEntryPositionI= 
                theRandomAccessInputStream.getPositionI();
          toEndEntry: { toNoEntry: {
            valueEpiNode= null; // Assume no value node unless one provided.
            keyEpiNode=  // Try parsing a key node.
                EpiNode.tryEpiNode(theRandomAccessInputStream);
            /// theAppLog.debug("MapEpiNode.getLinkedHashMap() "
            ///     + "keyEpiNode="+keyEpiNode);
            if (keyEpiNode == null) break toNoEntry; // Got no key so no entry.
            if (! tryByteB(theRandomAccessInputStream,':')) // No separator ":"
              break toEndEntry; // so no value, so end map entry now.
            valueEpiNode= // Try parsing value.
                EpiNode.tryEpiNode(theRandomAccessInputStream);
            /// theAppLog.debug("MapEpiNode.getLinkedHashMap() "
            ///     + "valueEpiNode="+valueEpiNode);
            if (valueEpiNode != null) break toEndEntry; // Got value so complete entry.
          } // toNoEntry: Being here means unable to parse an acceptable map entry.
            keyEpiNode= null; // Be certain to indicate map entry parsing failed.
            theRandomAccessInputStream.setPositionV( // Rewind input steam.
                preMapEntryPositionI);
          } // toEndEntry: Entry parsing either passed or failed, but is done, .
            if (! gotCommaB) // No comma yet so we want the first map entry
              { if (keyEpiNode == null) // but there was no first map entry
                  break toReturn; // so exit now with an empty map.
                }
              else // Comma was gotten so we need a non-first map entry.
              { if (keyEpiNode == null) { // but there was no map entry so
                theRandomAccessInputStream.setPositionV( // restore stream
                      preCommaPositionI); // to position before comma.
                  break toReturn; // and exit now with a non-empty map.
                  }
                }
            resultLinkedHashMap.put(keyEpiNode,valueEpiNode); // Append entry.
            preCommaPositionI= // Save this stream position.
                theRandomAccessInputStream.getPositionI();
            if (! tryByteB(theRandomAccessInputStream,',')) // If no comma 
              break toReturn; // Exit.
            gotCommaB= true; // Got comma, so record it.
            } // while(true)  Looping to try for another non-first map entry.
      } // toReturn:
        return resultLinkedHashMap;
      }

    public static MapEpiNode tryBlockMapEpiNode( 
        RandomAccessInputStream theRandomAccessInputStream, int minIndentI ) 
        throws IOException
      /* This method tries to parse a MapEpiNode (YAML map) 
        from theRandomAccessInputStream.
        It looks for the block aka indented flow syntax.
        minIndentI is the minimum indentation level for map entries. 
        If successful then it returns the MapEpiNode
        and the stream is moved past the map characters,
        but before the newline-indentation that terminated the map.
        If not successful then this method returns null 
        and the stream position is unchanged.
        */
      {
          MapEpiNode resultMapEpiNode= null; // Set default failure result.
          int initialStreamPositionI= 
              theRandomAccessInputStream.getPositionI();
        toReturn: {
          int mapEntryIndentI= // Try for newline indentation of first entry. 
            tryNewlineIndentationI(theRandomAccessInputStream, minIndentI);
          if (mapEntryIndentI < 0) // If failed to get needed indentation
            break toReturn; // then exit with failure.
          LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap=
            tryBlockLinkedHashMap(theRandomAccessInputStream, mapEntryIndentI);
              // Try parsing indented entries.
          if (theLinkedHashMap == null) // If no map entries parsed
            break toReturn; // then exit with failure.
          resultMapEpiNode= // We got everything needed so 
              new MapEpiNode(theLinkedHashMap); // create MapEpiNode result.
        } // toReturn:
          if (resultMapEpiNode == null) // If no result then rewind stream. 
            theRandomAccessInputStream.setPositionV(initialStreamPositionI);
          return resultMapEpiNode; // Return result.
        }

    protected static LinkedHashMap<EpiNode,EpiNode> tryBlockLinkedHashMap(
        RandomAccessInputStream theRandomAccessInputStream, 
        int mapEntryIndentI ) 
      throws IOException
      /* This method parses a set of map entries of a map.
        If successful then it returns a LinkedHashMap of 
        the parsed map entries and the position of the input stream 
        is moved past all parsed entries.
        There must be at least one entry for success.
        mapEntryIndentI is the starting indent level.
        The first entry is assumed to start immediately.
        Later entries, if any, are assumed to start 
        on later lines at the same indent.
        A line with a smaller indent level terminates the map.
        If not successful then this method returns null and 
        the position of the input stream is unchanged. 
       */
      {
        LinkedHashMap<EpiNode,EpiNode> resultLinkedHashMap= 
          new LinkedHashMap<EpiNode,EpiNode>(); // Create initially empty map.
      toReturn: {
        EpiNode keyScalarEpiNode= null; // Used as got-first-map-entry flag.
        EpiNode valueEpiNode= null;
        while (true) { // Accumulating map entries until they end.
            int preMapEntryPositionI= 
                theRandomAccessInputStream.getPositionI();
          toEndEntry: { toNoEntry: {
            valueEpiNode= null; // Assume no value node unless one provided.
            keyScalarEpiNode=  // Try parsing a key ScalarEpiNode.
                ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
            if (keyScalarEpiNode == null) // Got no key 
              break toNoEntry; // so no entry.
            if (! tryByteB(theRandomAccessInputStream,':')) // No separator ":"
              break toNoEntry; // so no value, so no map entry.
            trySpacesI(theRandomAccessInputStream); // Skip spaces.
            valueEpiNode=  // Try parsing value, possibly an indented map
                EpiNode.tryBlockEpiNode(theRandomAccessInputStream,
                    mapEntryIndentI+1); // using a nigher minimum indentation.
            if (valueEpiNode != null) // If got value, got complete entry.
              break toEndEntry;
          } // toNoEntry: Being here means unable to parse a map entry.
            keyScalarEpiNode= null; // Indicate map entry parsing failed.
            theRandomAccessInputStream.setPositionV(preMapEntryPositionI);
          } // toEndEntry: Entry parsing is done, either pass or fail.
            if (keyScalarEpiNode == null) // If there was no first map entry
                break toReturn; // go exit with an empty map.
            resultLinkedHashMap.put( // Append entry to map.
                keyScalarEpiNode,valueEpiNode);
            int indentI= // Try getting newline indentation of next entry. 
                tryNewlineIndentationI(
                  theRandomAccessInputStream, mapEntryIndentI);
            if ( indentI < 0 ) // If insufficient indentation 
              break toReturn; // exit.
        } // while(true)  Looping to try for another non-first map entry.
      } // toReturn:
        if ( resultLinkedHashMap.isEmpty()) // Convert empty map result
          resultLinkedHashMap= null; // to null map result.
        return resultLinkedHashMap;
      }

    private static int tryNewlineIndentationI(
        RandomAccessInputStream theRandomAccessInputStream, int minIndentI ) 
      throws IOException
      /* This method tries to read past newlines and indentation characters
        in theRandomAccessInputStream.
        This method is usually called when a node boundary is expected.
        If only newlines and indentation characters are seen,
        and the indentation level after the final newline 
        is at least minIndentI, then this method succeeds, and
        it returns a number > 0 which is the new indentation level,
        and the stream has been moved past all characters that were processed.
        If this method fails then it returns -1 and 
        the stream position is unchanged.
        
        ///enh Modified to skip over comments?
        */
      {
          int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
          int resultIndentI= -1;
        loop: while(true) { // Process newlines and indentations.
          if (! tryEndLineI(theRandomAccessInputStream)) // If no end of line 
            break loop; // exit loop.
          while (tryEndLineI(theRandomAccessInputStream)) // Skip any extras
            ; // by doing nothing for each one.
          resultIndentI= trySpacesI(theRandomAccessInputStream);
        } // loop:
          if (resultIndentI < minIndentI) // If indentation too small
            { // restore stream position and return failure.
              theRandomAccessInputStream.setPositionV(firstStreamPositionI);
              resultIndentI= -1;
              }
          return resultIndentI; 
        }

    private static boolean tryEndLineI(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read past spaces, a comment, 
        and trailing newline, in theRandomAccessInputStream.
        It returns true if it succeeds
        and the stream has been moved past all characters that were processed.
        If this method fails then it returns false and 
        the stream position is unchanged.
        It fails if there is anything other than spaces or a comment before
        the end of the present line. 
        */
      {
          int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
          trySpacesI(theRandomAccessInputStream); // Skip optional spaces.
          tryCommentB(theRandomAccessInputStream); // Skip optional comment.
          boolean successB= (tryNewlineB(theRandomAccessInputStream)); 
          if (! successB) // If no newline
            { // restore stream position and return failure.
              theRandomAccessInputStream.setPositionV(firstStreamPositionI);
              }
          return successB; 
        }

    private static boolean tryCommentB(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read from theRandomAccessInputStream
        past a comment, but not past the newline that terminates it.
        It returns true if successful, false if there was no comment.
        */
      {
          int finalStreamPositionI= theRandomAccessInputStream.getPositionI();
          boolean successB;
        toReturn: {
          successB= tryByteB(theRandomAccessInputStream,'#');
          if (! successB) break toReturn; // Exit if no comment introducer. 
          while (true) // Skip all characters to end of line.
            {
              finalStreamPositionI= theRandomAccessInputStream.getPositionI();
              int CI= theRandomAccessInputStream.read(); // Read next byte.
              if (SystemSettings.NLTestB(CI)) // Exit if character is newline. 
                break toReturn;
              }
        } // toReturn:
          theRandomAccessInputStream.setPositionV(finalStreamPositionI);
            // Set stream to last character seen.  Works for success or failure.
          return successB;
        }

    private static int trySpacesI(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read past spaces the next group of spaces
        in theRandomAccessInputStream.
        It returns the count of spaces in the group,
        which might be 0 if there were no spaces before the next non-space.
        */
      {
        int numberOfSpacesI= 0;
        int scanStreamPositionI;
        while (true) // Process all spaces.
          {
            scanStreamPositionI= theRandomAccessInputStream.getPositionI();
            int CI= theRandomAccessInputStream.read(); // Read next byte.
            if ( CI != ' ' ) //  If byte is not space
              { // restore stream before byte and exit.
                theRandomAccessInputStream.setPositionV(scanStreamPositionI);
                break;
                }
            numberOfSpacesI++;
            }
        return numberOfSpacesI; 
        }

    private static boolean tryNewlineB( 
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read a single newline 
        from theRandomAccessInputStream.
        If this method is successful then it returns true
        and the stream is moved past the newline.
        If this method is not successful then it returns false 
        and the stream position is unchanged.
        To skip all sequential newline characters, call this method in a loop.
        */
      {
        int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
        int CI= theRandomAccessInputStream.read();
        boolean successB=  // Test whether we got newline.
            SystemSettings.NLTestB(CI);
        if (! successB) // If not newline then restore stream position.
          theRandomAccessInputStream.setPositionV(firstStreamPositionI);
        return successB;
        }

    
    // Methods that get or make instances with entries.

    public synchronized String createEmptyMapWithNewKeyString()
      /* This method creates a new map entry in this map with
        a new unique key and a nested empty map as the value.
        The only guarantee about the key is that it will be unique.
        It creates a key which is a small integer converted to a String,
        even though other keys in the map might not be numerical indexes.
        It returns the key String of the created entry.
        */
      { 
        theAppLog.debug("MapEpiNode.createEmptyMapWithNewKeyString() called.");
        String scanKeyString;
        int scanIndexI= getSizeI()+1; // Set trial index to map size + 1; 
            
        while (true) // Search map for a key that is not already in use.
          {
            scanKeyString= // Convert index to key String.
                String.valueOf(scanIndexI);
            EpiNode valueEpiNode= // Try getting a value for that key.
                getEpiNode(scanKeyString);
            if (null == valueEpiNode) // If no value then key is available 
              break; // so exit.
            scanIndexI--; // Prepare to test next lower key index.
            }
        putV( // Create entry with the found key, and an empty map as value.
            scanKeyString, new MapEpiNode() );
        return scanKeyString; // Return key of the created entry.
        } 

    public synchronized MapEpiNode getOrMakeMapEpiNode(String keyString)
      /* This method returns the MapEpiNode value 
        that is associated with the key keyString.  
        If there is no such MapEpiNode, then an empty one is created,
        and it is associated in this MapEpiNode with keyString.
        If this method is called, it is assumed that
        the associated EpiNode is supposed to be a MapEpiNode, 
        not something else such as a ScalarEpiNode.
       */
      {
          // theAppLog.debug("MapEpiNode.getOrMakeMapEpiNode(String) called.");
          MapEpiNode valueMapEpiNode; // For function result. 
          EpiNode valueEpiNode= null;
        toReturnValue: { 
        toMakeMap: {
          valueEpiNode= getEpiNode(keyString);
          if (valueEpiNode == null) // No value is associated with this key.
            break toMakeMap; // so go make one.
          valueMapEpiNode= // Try converting value to map.
              valueEpiNode.tryOrLogMapEpiNode();
          if (valueMapEpiNode == null) // The value is not a map
            break toMakeMap; // so go make a replacement which is a map.
          break toReturnValue; // Value is a map, so go return it as is.
        } // toMakeMap: 
          valueMapEpiNode= new MapEpiNode(); // Make a new empty map.
          theLinkedHashMap.put( // Associate new map with key in this map.
              new ScalarEpiNode(keyString),valueMapEpiNode);
        } // toReturnValue:
          return valueMapEpiNode;
        }

    public static MapEpiNode makeSingleEntryMapEpiNode(
        String keyString, EpiNode valueEpiNode)
      /* This method returns a new MapEpiNode which contains 
        a single entry consisting of keyString and valueEpiNode.
        This is useful for creating EpiNode messages consisting of 
        key keyString which indicates a message type, 
        and a value valueEpiNode.
        */
      {
        return makeSingleEntryMapEpiNode(
            new ScalarEpiNode(keyString), // Convert String to EpiNode.
            valueEpiNode
            );
        }

    public static MapEpiNode makeSingleEntryMapEpiNode(
        EpiNode keyEpiNode, EpiNode valueEpiNode)
      /* This method returns a new MapEpiNode which contains 
        a single entry consisting of keyEpiNode and valueEpiNode.
        */
      {
        MapEpiNode resultMapEpiNode= // Make a new empty map. 
            new MapEpiNode();
        resultMapEpiNode.putV( // Add it's single entry.
            keyEpiNode,
            valueEpiNode
            );
        return resultMapEpiNode;
        }


    // Special method that renames keys.

    public synchronized void renameKeysV(
        String oldKeyString, String newKeyString)
      /* This method replaces instances of map keys 
        with value oldKeyString to NewKeyString.
        It is meant to be used for Persistent.txt file format changes.
        */
      { 
        //// theAppLog.debug( "MapEpiNode.renameKeys(\""
        ////   + oldKeyString + "\",\"" + newKeyString 
        ////   + "\") called.");
        for // First, recursively rename keys in entry values which are maps. 
          (EpiNode valueEpiNode: theLinkedHashMap.values()) 
          { // Process one value.
            MapEpiNode valueMapEpiNode= valueEpiNode.tryMapEpiNode();
            if (null != valueMapEpiNode) // If value is a map
              valueMapEpiNode.renameKeysV( // recursively rename within it.
                oldKeyString, newKeyString);
            }
        
        // Now rename key in this map, if present.
        EpiNode oldValueEpiNode= // Try removing old key from this map.
            removeEpiNode(oldKeyString);
        if (null != oldValueEpiNode) // If old key was removed then
          putV( // associate value with new key.
            newKeyString, oldValueEpiNode);
        }


    // Methods that store various types of data in a map.

    public synchronized void putV(String keyString, long valueL)
      /* This associates integer valueI with keyString in this MapEpiNode.
        */
      {
        putV(
            new ScalarEpiNode(keyString),
            new ScalarEpiNode(""+valueL)
            );
        }

    public synchronized void putV(String keyString, String valueString)
      /* This associates valueString with keyString in this MapEpiNode.
        The strings are converted to ScalarEpiNodes first.
        */
      {
        putV(
            new ScalarEpiNode(keyString),
            new ScalarEpiNode(valueString)
            );
        }

    public synchronized void putV(String keyString, EpiNode valueEpiNode)
      /* This associates valueString with keyString in this MapEpiNode.
        The strings are converted to ScalarEpiNodes first.
        */
      {
        putV(
            new ScalarEpiNode(keyString),
            valueEpiNode
            );
        }

    public synchronized void putV(EpiNode keyEpiNode, EpiNode valueEpiNode)
      /* This associates valueEpiNode with keyEpiNode in this MapEpiNode.
        It does this by making an entry in theLinkedHashMap.
        */
      {
        theLinkedHashMap.put(
            keyEpiNode,
            valueEpiNode
            );
        }

    public synchronized void removeV( String keyString)
    /* This method removes the field whose name is fieldKeyString.
      */
    { 
      removeEpiNode(keyString);
      }

    public synchronized EpiNode removeEpiNode(String keyString)
    /* This method removes the field whose name is fieldKeyString.
      It returns the previous EpiNode value, or null if there was none.
      */
    { 
      // theAppLog.debug("MapEpiNode.removeEpiNode(\""+keyString+"\") called.");
      return theLinkedHashMap.remove( new ScalarEpiNode(keyString ));
      }

    
    // Methods that get keys, values, entries, or other functions of the map.

    public String extractFromEpiNodeString(int indexI) 
        throws IOException
      /* See base abstract class for documentation.  */
      { 
          EpiNode resultEpiNode;
        toReturn: { toReturnFail: {
          Map.Entry<EpiNode,EpiNode> firstMapEntry= getMapEntry(0);
          if (firstMapEntry == null) // First entry exists? 
            break toReturnFail; // No, so return with fail.
          if (indexI == 0) { // First string desired? 
            resultEpiNode= firstMapEntry.getKey(); // So entry key is result.
            break toReturn;
            }
          MapEpiNode nestedMapEpiNode= // Get nested map which should be 
            (MapEpiNode)firstMapEntry.getValue(); // value of first entry.
              ///fix This could produce a ClassCastException, 
              // but it's only temporary.
          if (nestedMapEpiNode == null) // Is there a value, itself a map? 
            break toReturnFail; // No, so return with fail.
          Map.Entry<EpiNode,EpiNode> nestedMapEntry= // From nested map
              nestedMapEpiNode.getMapEntry(indexI-1); // get desired entry.
          if (nestedMapEntry == null) // Is an entry there? 
            break toReturnFail; // No, so return with fail.
          resultEpiNode= nestedMapEntry.getValue(); // Get entry value.
          if (resultEpiNode != null) // Is a value there? 
            break toReturn; // Yes, so use it.
        } // toReturnFail: // Come here to return null indicating failure.
          resultEpiNode= null; // Set result indicating extraction failure.
        } // toReturn:
          return  // Returned desiredEpiNode converted to String or null.
            (resultEpiNode != null) ? resultEpiNode.toString() : null;
        }

    private Map.Entry<EpiNode,EpiNode> getMapEntry(int indexI) 
        throws IOException
      /* Returns Map.Entry at position indexI, 
        or null if indexI is out of range, or the entry itself is null.
        It finds the correct entry by iterating to the desired position 
        and returning the Map.Entry there.
        The map order is the insertion order.
        */
      { 
        Map.Entry<EpiNode,EpiNode> resultMapEntry= null;
        Map.Entry<EpiNode,EpiNode> scanMapEntry= null;
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
            theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
            theSetOfMapEntrys.iterator();
        while(true) { // Iterate to the desired entry.
          if (! entryIterator.hasNext()) // More entries? 
            break; // No, so exit with null value.
          scanMapEntry= entryIterator.next(); // Yes, get current entry.
          if (indexI == 0) { // Is this the entry we want?
            resultMapEntry= scanMapEntry; // Yes, set the entry as result
            break; // and exit.
            }
          indexI--; // Decrement entry down-counter.
          }
        return resultMapEntry;
        }

    public synchronized String getNextString(String keyString) 
      /* Returns key String of next entry after the one selected by keyString.
        or null if we are at end of map and there is no next entry.
        It finds the correct entry by iterating to the entry 
        with the desired key, then moving one more step.
        */
      { 
          String resultString= null; // Default String result of null.
          Map.Entry<EpiNode,EpiNode> scanMapEntry= null;
          Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
              theLinkedHashMap.entrySet();
          Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
              theSetOfMapEntrys.iterator();
        goReturn: {
          if (keyString.isEmpty()) // On an actual entry now? 
            { // No, so return first entry if there is one. 
              if (! entryIterator.hasNext()) // Is there a first entry? 
                break goReturn; // No, so go return default null result.
              scanMapEntry=  // Yes, so use first entry as result.
                  entryIterator.next();
              break goReturn; // Go return it.
              }
          while(true) { // Find entry with keyString and return its successor.
            if (! entryIterator.hasNext()) // Is there a first or next entry?
              { // No, so
                scanMapEntry= null; // clear entry
                break goReturn; // so we will return null string result.
                }
            scanMapEntry= entryIterator.next(); // Yes, get the next entry.
            if  // Is keyString its key?
              (keyString.equals(scanMapEntry.getKey().toString()))
              { // Yes, so return the entry after it if there is one.
                if (! entryIterator.hasNext()) // Is there a next entry? 
                  scanMapEntry= null; // No, so set null result.
                  else
                  scanMapEntry= // Yes, so use it as result.
                    entryIterator.next();
                break goReturn; // Go return result.
                }
            }
        } // goReturn:
          if (scanMapEntry != null) // If we found an entry
            resultString=  // override null result string with entry key.
              scanMapEntry.getKey().toString();
          return resultString;
        }

    public boolean testB( String keyString )
      /* This method returns true if value of the field whose key is keyString
        is non-null "true", ignoring case, false otherwise.
        ///org Rename to getB(.).
        */
      {
        String valueString= getString(keyString);
        return Boolean.parseBoolean( valueString );
        }

    public boolean testKeyForValueB(String keyString, String testValueString) 
      /* Returns true if testValueString is the value 
        associated with keyString, false otherwise.
        */
      { 
        String valueString= getString(keyString);
        boolean resultB= testValueString.equals(valueString);
        return resultB;
        }
  
    public int getZeroOrI(String keyString)
      /* Returns the integer value associated with keyString.
        If the value is missing, or not parse-able as an integer,
        it returns 0.
        */
      {
        int valueI= 0; // Assume there will be an error.
        String valueString= getEmptyOrString(keyString);
        try 
          { 
            valueI= Integer.parseInt(valueString); 
            }
        catch (NumberFormatException theNumberFormatException) 
          { 
            ; // Ignore exception.  Return value is already set to 0.
            }
        return valueI;
        }
  
    public String getEmptyOrString( String keyString )
      /* Returns the value String associated with keyString,
        or the empty string if there is none.
        */
      {
        return getString( keyString, "" );
        }
    
    private String getString( String keyString, String defaultValueString )
      /* Returns the value String associated with keyString,
        or defaultValueString if there is no value String stored.
        */
      {
        String childValueString= getString(keyString); 
        if (childValueString == null) 
          childValueString= defaultValueString;
        return childValueString;
      }

    public String getString(String keyString) 
      /* Returns String representation of value associated with keyString,
        or null if there is no such value.
        ///fix to not use toString() so that non-Scalar values
          produce an error string, to prevent long string results. 
        */
      { 
        String resultString= null;
        EpiNode valueEpiNode= getEpiNode(keyString);
        if (valueEpiNode != null)
          resultString= valueEpiNode.toString();
        return resultString;
        }

    public MapEpiNode getMapEpiNode(String keyString)
      /* This method returns the MapEpiNode 
        that is associated with the key keyString.
        If there is no such node then null is returned. 
       */
      {
        MapEpiNode valueMapEpiNode= null;
        toReturn: {
          EpiNode valueEpiNode= // Get associated value.
              getEpiNode(keyString);
          if (valueEpiNode == null) break toReturn;
          valueMapEpiNode=  // Try converting EpiNode to MapEpiNode.
              valueEpiNode.tryOrLogMapEpiNode();
          } // toReturn:
        return valueMapEpiNode;
        }

    public EpiNode getEpiNode(String keyString)
      /* This method returns the EpiNode 
        that is associated with the key keyString.
        If there is no such node then null is returned. 
       */
      {
        if ( keyString == null || keyString.isEmpty()) // Handle bad key.
          {
            keyString= "MapEpiNode.getOrMakeMapEpiNode() Missing keyString.";
            theAppLog.error(keyString);
            }
        EpiNode keyEpiNode= // Convert String to EpiNode.
            new ScalarEpiNode(keyString);
        EpiNode valueEpiNode= // Lookup value of this key.
            getEpiNode(keyEpiNode);
        return valueEpiNode;
        }


    public EpiNode getEpiNode(EpiNode keyEpiNode)
      /* This method returns the value EpiNode 
        that is associated with the keyEpiNode.
        If there is no such node then null is returned. 
       */
      {
        return theLinkedHashMap.get(keyEpiNode);
        }


    // Special getters or calculated values.
    
    public MapEpiNode tryMapEpiNode()
      /* This method returns the (this) reference if this is a MapEpiNode,
        null otherwise.
        */
      {
        return this; // Return non-null this because this is a MapEpiNode.
        }

    public int getSizeI()
      /* This method returns number of elements in the map.  */
      {
        return theLinkedHashMap.size();
        }

    public LinkedHashMap<EpiNode,EpiNode> getLinkedHashMap()
      /* This method returns the maps LinkedHashMap.  */
      {
        return theLinkedHashMap;
        }
    


    // Constructors.

    public MapEpiNode( // constructor.
        LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap)
      {
        this.theLinkedHashMap= theLinkedHashMap;
        }
    

    public MapEpiNode() // constructor.
      {
        this( // Call parameter constructor with
            new LinkedHashMap<EpiNode,EpiNode>() // an empty LinkedHashMap.  
            );
        }

    } // MapEpiNode
