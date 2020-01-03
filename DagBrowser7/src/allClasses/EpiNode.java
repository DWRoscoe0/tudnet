package allClasses;

import static allClasses.AppLog.theAppLog;

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
    It supports scalars, sequences, and maps.  
    It does not support null values. 
    Subclasses follow this class.
    
    Most of the code here deals with the Flow syntax style, 
    which uses braces to indicate structure.  This code is fully working.
    Some of the code here deals with the Block syntax style, 
    which uses indentation to indicate structure.  This code is is a work-in-progress.
    */

  {
  
    abstract public String extractFromEpiNodeString(int indexI) //// temporary. 
        throws IOException;
      /* This method tries to extract the String whose index is indexI from this EpiNode. 
        If it succeeds it returns the String.  If it fails it returns null, 
        meaning there is no data at the index position requested.
        The mapping between index values and Strings in the EpiNode 
        is complex, depends on the EpiNode, and may be temporary.  

        //// This method is meant to act as a temporary bridge between 
        accessing data by position and accessing data by name.
        Because of this, and the fact that the methods are temporary,
        error reporting is crude, just enough for debugging and 
        moving on to the next development phase.
        */


    public MapEpiNode getMapEpiNode()
      /* This method returns the (this) reference if this is a MapEpiNode,
        null otherwise.
        */
      {
        return null; // MapEpiNode will override this returned value.
        }

    public EpiNode getEpiNode(int indexI)
      /* This method returns the element EpiNode at index indexI,
        In this base class, it always returns null and logs an error.
        */
      {
        theAppLog.error( "EpiNode.getEpiNode(int): base class should not be called.");
        return null;
        }

    public abstract void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException;
      /* Writes this EpiNode to theEpiOutputStream.  */

    public static EpiNode tryEpiNode(RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
    /* This method tries to parse an EpiNode.
      It returns the node if the parse is successful, null otherwise.
      It tries parsing node types in the following order:
      * SequenceEpiNode
      * MapEpiNode (to be added)
      * ScalarEpiNode
     */
    { 
        EpiNode resultEpiNode= null; 
      toReturn: {
        resultEpiNode= SequenceEpiNode.trySequenceEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;
        resultEpiNode= MapEpiNode.tryMapEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;
        resultEpiNode= ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
      } // toReturn:
        return resultEpiNode;
      }

    public static EpiNode tryBlockEpiNode(
        RandomAccessInputStream theRandomAccessInputStream, int minIndentI ) 
      throws IOException
    /* This method tries to parse an EpiNode from theRandomAccessInputStream.
      It looks for the block aka indented flow syntax.
      indentI is the minimum indentation level for nested structures, like maps. 
      This method returns the node if the parse is successful, null otherwise.
      It tries parsing node types in the following order:
      * ScalarEpiNode
      * MapEpiNode (to be added)
      This method does not support sequences and map keys may be scalars only.
      */
    {
        EpiNode resultEpiNode= null; 
      toReturn: {
        resultEpiNode= ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;
        resultEpiNode= MapEpiNode.tryBlockMapEpiNode(
            theRandomAccessInputStream, minIndentI);
      } // toReturn:
        return resultEpiNode;
      }
        
    protected static void newLineAndindentV(OutputStream theOutputStream, int indentI)
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
      /* Reads a byte from theRandomAccessInputStream and compares it to desiredByteI.
        If they are equal it returns true, otherwise false.
        Failure can happen when either the byte read is not the desired byte or
        if there is no byte available.
        The stream advances only if a read byte is the desired one.
        */
      /* Tries to read desiredByteI from the stream.
        This is like getByteB(..) except that the stream position
        is not changed if desiredByteI can not be read from the stream.
        */
      {
        int positionI= theRandomAccessInputStream.getPositionI(); // Save stream position.
        boolean successB= // Read and test byte.
            getByteB(theRandomAccessInputStream,desiredByteI);
        if ( ! successB ) // If failure
          theRandomAccessInputStream.setPositionV(positionI); // rewind stream position.
        return successB;
        }

    public static boolean getByteB(
        RandomAccessInputStream theRandomAccessInputStream, int desiredByteI) 
      throws IOException
      /* Reads a byte from theRandomAccessInputStream and compares it to desiredByteI.
        If they are equal it returns true, otherwise false.
        Failure can happen when either the byte read is not the desired byte or
        if there is no byte available.
        The stream advances whether or not a read byte is the desired one.
        */
      {
        int byteI= theRandomAccessInputStream.read(); // read the byte
        boolean successB= // Test byte for correctness.
            (byteI == desiredByteI); // Fails if byteI is -1 or not desired byte.
        return successB;
        }

    } // class EpiNode

class ScalarEpiNode extends EpiNode 

  {
    private String scalarString;
    
    ScalarEpiNode(String scalarString)
      {
        this.scalarString= scalarString;
        }

    public void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException
      { 
        //// EpiNode.newLineAndindentV(theOutputStream, indentI);
        theOutputStream.write(scalarString.getBytes());
        }
    
    public String extractFromEpiNodeString(int indexI) //// temporary. 
        throws IOException 
      { return scalarString; }

    public String toString() { return scalarString; }
      
    public static ScalarEpiNode tryScalarEpiNode( 
          RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse a ScalarEpiNode (YAML subset scalar string)
        from theRandomAccessInputStream.
        If successful then it returns the ScalarEpiNode 
        and the stream is moved past the scalar characters,
        but whatever terminated the scalar remains to be read.
        The stream is moved past the last scalar character, but no further.
        If not successful then this method returns null 
        and the stream position is unchanged.
        */
      {
        ScalarEpiNode theScalarEpiNode= null;
        int byteI;
        String accumulatorString= ""; // Clear character accumulator.
        readLoop: { while (true) {
            int positionI= theRandomAccessInputStream.getPositionI();
            toAppendAcceptedChar: {
              byteI= theRandomAccessInputStream.read();
              if ( Character.isLetterOrDigit(byteI)) break toAppendAcceptedChar;
              if ( '-'==byteI ) break toAppendAcceptedChar;
              if ( '.'==byteI ) break toAppendAcceptedChar;
              theRandomAccessInputStream.setPositionV(positionI); // Restore stream position.
              ///opt Alternative way to reject final character only, outside of loop:
              //   setPositionV(getPositionI()-1);
              break readLoop; // Go try to return what's accumulated so far.
              } // toAppendAcceptedChar:
            accumulatorString+= (char)byteI; // Append accepted byte to accumulator.
            }
          } // readLoop: 
        if (accumulatorString.length() != 0) // Reject 0-length strings.
          theScalarEpiNode= new ScalarEpiNode(accumulatorString); // Override null result.
        return theScalarEpiNode; // Return result.
        }

    public String getString()
      /* Returns the String which represents the value of the scalar.  */
      { return scalarString; }

    }

class SequenceEpiNode extends EpiNode

  /* This class implements a YAML sequence, flow style only.
   
    This class was used for a while to encode packet data, but it is no longer used.
   */

  {
    private ArrayList<EpiNode> theListOfEpiNode; 

    private SequenceEpiNode(ArrayList<EpiNode> theListOfEpiNode)
      {
        this.theListOfEpiNode= theListOfEpiNode;
        }

    public void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException
      { 
        for (EpiNode elementEpiNode : theListOfEpiNode)
          {
            EpiNode.newLineAndindentV(theOutputStream, indentI);
            theOutputStream.write("- ".getBytes()); // Introduce sequence element.
            elementEpiNode.writeV( // Output element
                theOutputStream, 
                indentI + 2 // with further indenting of any element components.
                );
            }
        }

    public String extractFromEpiNodeString(int indexI)  //// temporary. 
        throws IOException
      /* See base class for documentation.  */
      { 
        String elementString= null; // Set default result to indicate failure.
        EpiNode elementEpiNode= getEpiNode(indexI);
        if (elementEpiNode != null) // If got element node, extract string and return it. 
          elementString= elementEpiNode.toString();
        return elementString;
        }

    public EpiNode getEpiNode(int indexI)
      /* This method returns the element EpiNode at index indexI,
        or null if the index is out of range.
        */
      {
        return 
          ( (indexI >= 0) && (indexI < theListOfEpiNode.size())) // Test for in range.
            ? theListOfEpiNode.get(indexI) // Value if in range.
            : null; // Value if out of range.
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
          SequenceEpiNode returnSequenceEpiNode= null; // Set default failure result.
          ArrayList<EpiNode> resultListOfEpiNodes= null;
          int initialStreamPositionI= theRandomAccessInputStream.getPositionI();
        toReturn: { toNotASequence: {
          if (! getByteB(theRandomAccessInputStream, '[')) break toNotASequence;
          resultListOfEpiNodes=  // Always succeeds.
              getListOfEpiNodes(theRandomAccessInputStream); 
          if (! getByteB(theRandomAccessInputStream, ']')) break toNotASequence;
          returnSequenceEpiNode= // We got everything needed.  Create successful result. 
              new SequenceEpiNode(resultListOfEpiNodes);
          break toReturn;
        } // toNotASequence: // Coming here means we failed to parse a complete sequence.
          theRandomAccessInputStream.setPositionV(initialStreamPositionI); // Restore position.
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
        if (! gotCommaB) // Comma not gotten yet so looking for the first element
          { if (theEpiNode == null) // but there is no first element
            break toReturn; // so exit now with an empty list.
            }
        else // Comma was gotten so we need a non-first element.
          { if (theEpiNode == null) { // but there was no element so
              theRandomAccessInputStream.setPositionV( // restore stream position to before comma.
                  preCommaPositionI);
              break toReturn; // and exit now with a non-empty list.
              }
            }
        resultListOfEpiNodes.add(theEpiNode); // Append gotten element to list.
        preCommaPositionI= theRandomAccessInputStream.getPositionI();
        if (! tryByteB(theRandomAccessInputStream,',')) break toReturn; // Exit if no comma.
        gotCommaB= true; // Got comma, so record it.
        } // while(true)
    } // toReturn:
      return resultListOfEpiNodes;
    }

    }

class MapEpiNode extends EpiNode 

  {
    private LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap; 

    public MapEpiNode getMapEpiNode()
      /* This method returns the (this) reference if this is a MapEpiNode,
        null otherwise.
        */
      {
        return this; // Return non-null this because this is a MapEpiNode.
        }

    public void writeV(OutputStream theOutputStream, int indentI ) 
        throws IOException
      { 
        //// theOutputStream.write(scalarString.getBytes());
        Map.Entry<EpiNode,EpiNode> scanMapEntry= null;
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= theSetOfMapEntrys.iterator();
        while(true) { // Iterate over all entries.
          if (! entryIterator.hasNext()) // More entries? 
            break; // No, so exit.
          EpiNode.newLineAndindentV(theOutputStream, indentI);
          scanMapEntry= entryIterator.next(); // Yes, get current entry.
          scanMapEntry.getKey().writeV(theOutputStream, indentI); // Write key.
          theOutputStream.write(": ".getBytes()); // Write map key-value separator.
          scanMapEntry.getValue().writeV( // Write value.
              theOutputStream, 
              indentI + 2); // Indent components, if any, here.
          }
        }

    public MapEpiNode(LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap) // constructor.
      {
        this.theLinkedHashMap= theLinkedHashMap;
        }

    public MapEpiNode() // constructor.
      {
        this( // Call parameter constructor with
            new LinkedHashMap<EpiNode,EpiNode>() // an initially empty LinkedHashMap.  
            );
        }

    public static MapEpiNode tryMapEpiNode( 
        RandomAccessInputStream theRandomAccessInputStream ) 
        throws IOException
      /* This method tries to parse a MapEpiNode (YAML map) from theRandomAccessInputStream.
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
          int initialStreamPositionI= theRandomAccessInputStream.getPositionI();
        toReturn: { toNotAMap: {
          if (! getByteB(theRandomAccessInputStream, '{')) break toNotAMap;
          theLinkedHashMapOfEpiNode=  // Always succeeds.
              getLinkedHashMap(theRandomAccessInputStream); 
          if (! getByteB(theRandomAccessInputStream, '}')) break toNotAMap;
          resultMapEpiNode= // We got everything needed.  Create successful result. 
              new MapEpiNode(theLinkedHashMapOfEpiNode);
          break toReturn;
        } // toNotAMap: // Coming here means we failed to parse a complete map.
          theRandomAccessInputStream.setPositionV(initialStreamPositionI); // Restore position.
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
            int preMapEntryPositionI= theRandomAccessInputStream.getPositionI();
          toEndEntry: { toNoEntry: {
            valueEpiNode= null; // Assume no value node unless one provided.
            keyEpiNode=  // Try parsing a key node.
                EpiNode.tryEpiNode(theRandomAccessInputStream);
            if (keyEpiNode == null) break toNoEntry; // Got no key so no entry.
            if (! tryByteB(theRandomAccessInputStream,':')) // No separating colon 
              break toEndEntry; // so no value, so end map entry now.
            valueEpiNode= EpiNode.tryEpiNode(theRandomAccessInputStream); // Try parsing value.
            if (valueEpiNode != null) break toEndEntry; // Got value so complete entry.
          } // toNoEntry: Being here means unable to parse an acceptable map entry.
            keyEpiNode= null; // Be certain to indicate map entry parsing failed.
            theRandomAccessInputStream.setPositionV(preMapEntryPositionI); // Rewind input steam.
          } // toEndEntry: Being here means entry parsing is done, either pass or fail.
            if (! gotCommaB) // Comma not gotten yet so we want the first map entry
              { if (keyEpiNode == null) // but there was no first map entry
                  break toReturn; // so exit now with an empty map.
                }
              else // Comma was gotten so we need a non-first map entry.
              { if (keyEpiNode == null) { // but there was no map entry so
                theRandomAccessInputStream.setPositionV( // restore input stream position 
                      preCommaPositionI); // to position before comma.
                  break toReturn; // and exit now with a non-empty map.
                  }
                }
            resultLinkedHashMap.put(keyEpiNode,valueEpiNode); // Append entry to map.
            preCommaPositionI= theRandomAccessInputStream.getPositionI(); // Save stream position.
            if (! tryByteB(theRandomAccessInputStream,',')) break toReturn; // Exit if no comma.
            gotCommaB= true; // Got comma, so record it for earlier map entry processing.
            } // while(true)  Looping to try for another non-first map entry.
      } // toReturn:
        return resultLinkedHashMap;
      }

    public static MapEpiNode tryBlockMapEpiNode( //// 
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
          int initialStreamPositionI= theRandomAccessInputStream.getPositionI();
        toReturn: {
          int mapEntryIndentI= // Try getting a good newline indentation of first entry. 
            tryNewlineIndentationI(theRandomAccessInputStream, minIndentI);
          if (mapEntryIndentI < 0) // If failed to get needed indentation
            break toReturn; // then exit with failure.
          LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap= // Try parsing indented entries.
              tryBlockLinkedHashMap(theRandomAccessInputStream, mapEntryIndentI);
          if (theLinkedHashMap == null) // If no map entries parsed
            break toReturn; // then exit with failure.
          resultMapEpiNode= // We got everything needed so 
              new MapEpiNode(theLinkedHashMap); // create successful MapEpiNode result.
        } // toReturn:
          if (resultMapEpiNode == null) // If no result to return then rewind stream. 
            theRandomAccessInputStream.setPositionV(initialStreamPositionI);
          return resultMapEpiNode; // Return result.
        }

    protected static LinkedHashMap<EpiNode,EpiNode> tryBlockLinkedHashMap( ////
        RandomAccessInputStream theRandomAccessInputStream, int mapEntryIndentI ) 
      throws IOException
      /* This method parses a set of map entries of a map.
        If successful then it returns a LinkedHashMap of the parsed map entries
        and the position of the input stream is moved past all parsed entries.
        There must be at least one entry for success.
        mapEntryIndentI is the starting indent level.
        The first entry is assumed to start immediately.
        Later entries, if any, are assumed to start on later lines at the same indent.
        A line with a smaller indent level terminates the map.
        If not successful then this method returns null and 
        the position of the input stream is unchanged. 
       */ ////
      {
        LinkedHashMap<EpiNode,EpiNode> resultLinkedHashMap= 
            new LinkedHashMap<EpiNode,EpiNode>(); // Create initially empty map.
      toReturn: {
        EpiNode keyScalarEpiNode= null; // Initially null meaning map entry is not valid.
        EpiNode valueEpiNode= null;
        while (true) { // Accumulating map entries until they end.
            int preMapEntryPositionI= theRandomAccessInputStream.getPositionI();
          toEndEntry: { toNoEntry: {
            valueEpiNode= null; // Assume no value node unless one provided.
            keyScalarEpiNode=  // Try parsing a key node, limited to scalars for now.
                ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
            if (keyScalarEpiNode == null) break toNoEntry; // Got no key so no entry.
            if (! tryByteB(theRandomAccessInputStream,':')) // No separating colon 
              break toNoEntry; // so no value, so no map entry.
            trySpacesI(theRandomAccessInputStream); // Skip spaces.
            valueEpiNode=  // Try parsing value, possibly itself an indented map
                EpiNode.tryBlockEpiNode(theRandomAccessInputStream,
                    mapEntryIndentI+1); // using a nigher minimum indentation.
            if (valueEpiNode != null) break toEndEntry; // Got value so complete entry.
          } // toNoEntry: Being here means unable to parse an acceptable map entry.
            keyScalarEpiNode= null; // Be certain to indicate map entry parsing failed.
            theRandomAccessInputStream.setPositionV(preMapEntryPositionI); // Rewind input steam.
          } // toEndEntry: Being here means entry parsing is done, either pass or fail.
            if (keyScalarEpiNode == null) // but there was no first map entry
                break toReturn; // so exit now with an empty map.
            resultLinkedHashMap.put(keyScalarEpiNode,valueEpiNode); // Append entry to map.
            int indentI= // Try getting a good newline indentation of next entry. 
                tryNewlineIndentationI(theRandomAccessInputStream, mapEntryIndentI);
            if ( indentI < 0 ) break toReturn; // Exit if insufficient indentation.
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
        and the indentation level after the final newline is at least minIndentI,
        then this method succeeds, and
        it returns a number > 0 which is the new indentation level,
        and the stream has been moved past all characters that were processed.
        If this method fails then it returns -1 and 
        the stream position is unchanged.
        
        //// Being modified to skip over comments.
        */
      {
          int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
          int resultIndentI= -1;
        loop: while(true) { // Process newlines and indentations.
          if (! tryEndLineI(theRandomAccessInputStream)) // Exit if no end of line. 
            break loop;
          while (tryEndLineI(theRandomAccessInputStream)) // Skip additional EndLines
            ; // by doing nothing for each one.
          //// int testIndentI= tryIndentationI(theRandomAccessInputStream, resultIndentI);
          resultIndentI= trySpacesI(theRandomAccessInputStream);
        } // loop:
          if (resultIndentI < minIndentI) // If indentation too small or nonexistent
            { // restore stream position and return failure.
              theRandomAccessInputStream.setPositionV(firstStreamPositionI);
              resultIndentI= -1;
              }
          return resultIndentI; 
        }

    private static boolean tryEndLineI(
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read past spaces, a comment, and trailing newline,
        in theRandomAccessInputStream.
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
          if (! successB) // If there w something besides comment before newline
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
              if (SystemSettings.NLTestB(CI)) break toReturn; // Exit if newline.
              }
        } // toReturn:
          theRandomAccessInputStream.setPositionV(finalStreamPositionI);
            // Set stream to last character seen.  Works for success or failure.
          return successB;
        }

    private static int trySpacesI( ////
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read past spaces the next group of spaces
        in theRandomAccessInputStream.
        It returns the count of spaces in the group,
        which might be 0 if there were no spaces before the next non-space.
        */
      {
        int spacesI= 0;
        int scanStreamPositionI;
        while (true) // Process all spaces.
          {
            scanStreamPositionI= theRandomAccessInputStream.getPositionI();
            int CI= theRandomAccessInputStream.read(); // Read next byte.
            if ( CI != ' ' ) { // Restore stream before byte and exit if not space.
              theRandomAccessInputStream.setPositionV(scanStreamPositionI);
              break;
              }
            spacesI++;
            }
        return spacesI; 
        }

    @SuppressWarnings("unused") ////
    private static int tryIndentationI( 
        RandomAccessInputStream theRandomAccessInputStream, int indentLevelI ) 
      throws IOException
      /* This method tries to read past indentation characters
        in theRandomAccessInputStream.
        It must be called with the stream already in an indented state,
        which means only indentation characters between 
        the present stream position and the beginning of the present line.
        This method is usually called immediately after a newline.
        The initial indentation level is assumed to be equal to indentLevelI.
        If this method is successful then 
        it returns a number > 0 which is the new indentation level,
        which is the number of spaces to the left to the beginning of the line,
        and the stream has been moved past all characters that were processed.
        If this method is not successful then 
        it returns -1 and the stream position is unchanged.
        The only way this method is not successful is if encounters no indentation.
        */
      {
        int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
        int scanStreamPositionI;
        while (true) // Process all indentation characters.
          {
            scanStreamPositionI= theRandomAccessInputStream.getPositionI();
            int CI= theRandomAccessInputStream.read(); // Read next byte.
            if ( CI != ' ' ) { // Restore stream before byte and exit if not space.
              theRandomAccessInputStream.setPositionV(scanStreamPositionI);
              break;
              }
            indentLevelI++; // Otherwise increment indentation level by one.
            }
        if (firstStreamPositionI == scanStreamPositionI) // If stream did not advance
          indentLevelI= -1; // override return value to indicate failure.
        return indentLevelI; 
        }

    private static boolean tryNewlineB( 
        RandomAccessInputStream theRandomAccessInputStream ) 
      throws IOException
      /* This method tries to read a single newline from theRandomAccessInputStream.
        If this method is successful then it returns true
        and the stream is moved past the newline.
        If this method is not successful then it returns false 
        and the stream position is unchanged.
        To skip all newline characters, call this method in a loop.
        */
      {
        int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
        int CI= theRandomAccessInputStream.read();
        boolean successB= SystemSettings.NLTestB(CI); // Test whether we got newline.
        if (! successB) // If not newline then restore stream position.
          theRandomAccessInputStream.setPositionV(firstStreamPositionI);
        return successB;
        }

    public void putV(String keyString, String valueString)
      /* This associates valueString with keyString in this MapEpiNode.
        The strings are converted to ScalarEpiNodes first.
        */
      {
        theLinkedHashMap.put(
            new ScalarEpiNode(keyString),
            new ScalarEpiNode(valueString)
            );
        }

    public EpiNode getEpiNode(EpiNode keyEpiNode)
      /* This method returns the value EpiNode associated with keyEpiNode
        in this MapEpiNode, if it exists.
        Otherwise it returns null.
        */
      {
        return theLinkedHashMap.get(keyEpiNode);
        }

    public String extractFromEpiNodeString(int indexI) //// temporary. 
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
              ///fix This could produce a ClassCastException, but it's only temporary.
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
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= theSetOfMapEntrys.iterator();
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

    public String getNextString(String keyString) 
      /* Returns key String of next entry after the one selected by keyString.
        or null if we are at end of map and there is no next entry.
        It finds the correct entry by iterating to the entry with the desired key.
        */
      { 
        String resultString= null;
        Map.Entry<EpiNode,EpiNode> scanMapEntry= null;
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= theSetOfMapEntrys.iterator();
        while(true) { // Iterate to the present entry.
          if (! entryIterator.hasNext()) // More entries? 
            break; // No, so exit with null value.
          scanMapEntry= entryIterator.next(); // Yes, get entry here.
          if  // Is this the selected entry?
            (keyString.equals(scanMapEntry.getKey().toString()))
            break; // Yes, exit with this value.
          }
        if (entryIterator.hasNext()) // Any entries after present one? 
          scanMapEntry= entryIterator.next(); // Yes, get next entry as desired entry.
        if (scanMapEntry != null) // If there is actual entry here
          resultString= scanMapEntry.getKey().toString(); // get its key string.
        return resultString;
        }

    public String getValueString(String keyString) 
      /* Returns String representation of value associated with
        a ScalarEpiNode representation of keyString,
        or null if there is no such value.
        */
      { 
        EpiNode valueEpiNode= getChildEpiNode(keyString); 
        return valueEpiNode.toString();
        }

    public int getSizeI()
      /* This method returns number of elements in the map.  */
      {
        return theLinkedHashMap.size();
        }

    public MapEpiNode getOrMakeChildMapEpiNode(String keyString)
      /* This method returns the MapEpiNode value 
        that is associated with the key keyString.  
        If there is no such MapEpiNode, then an empty one is created,
        and it is associated in this MepEpiNode with keyString.
        If this method is called, it is assumed that
        the associated EpiNode is supposed to be a MapEpiNode, 
        not something else such as a ScalarEpiNode.
       */
      {
          MapEpiNode valueMapEpiNode; // For function result. 
          EpiNode valueEpiNode= null;
        toReturnValue: { 
        toMakeMap: {
          valueEpiNode= getChildEpiNode(keyString);
          if (valueEpiNode == null) // No value EpiNode is associated with this key.
            break toMakeMap; // so go make one.
          valueMapEpiNode= valueEpiNode.getMapEpiNode(); // Try converting value to map.
          if (valueMapEpiNode == null) // The value is not a map
            break toMakeMap; // so go make a replacement which is a map.
          break toReturnValue; // Value is a map, so go return it as is.
        } // toMakeMap: 
          valueMapEpiNode= new MapEpiNode(); // Make a new empty map.
          theLinkedHashMap.put( // Associate new map with key as entry in this map.
              new ScalarEpiNode(keyString),valueMapEpiNode);
        } // toReturnValue:
          return valueMapEpiNode;
        }

    public EpiNode getChildEpiNode(String keyString)
      /* This method returns the child MapEpiNode 
        that is associated with the key keyString.
        If there is no such child then null is returned. 
       */
      {
        if ( keyString == null || keyString.isEmpty()) // Handle bad key.
          {
            keyString= "MapEpiNode.getOrMakeMapEpiNode() Missing keyString.";
            theAppLog.error(keyString);
            }
        EpiNode keyEpiNode= new ScalarEpiNode(keyString); // Convert String to EpiNode.
        EpiNode valueEpiNode= getChildEpiNode(keyEpiNode); // Lookup value of this key.
        return valueEpiNode;
        }

    public EpiNode getChildEpiNode(EpiNode keyEpiNode)
      /* This method returns the value EpiNode 
        that is associated with the keyEpiNode.
        If there is no such child then null is returned. 
       */
      {
        return theLinkedHashMap.get(keyEpiNode);
        }

    }
