package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class EpiNode 

  /* This is the base class for 
    classes meant to represent YAML-like data.
    It supports scalars, sequences, and maps.  
    It does not support null values. 
    Subclasses follow this one.
    */
  {

  public String extractFromEpiNodeString(int indexI) 
      throws IOException
    /* This method tries to extract the String whose index is indexI from this EpiNode. 
      If it succeeds it returns the String.  
      If it fails it returns null, meaning the index is out of range.
      Presently the EpiNode must be a SequenceEpiNode but this is temporary.
      The mapping between index values and Strings in the EpiNode is complex, 
      depends on the EpiNode, and may be temporary.  
      
      In this base class, it always returns null and logs an error.
      */
    { 
      theAppLog.error( ""
          + "EpiNode.extractFromEpiNodeString(int): base class should not be called.");
      return null;
      }

    public EpiNode getEpiNode(int indexI)
      /* This method returns the element EpiNode at index indexI,
        In this base class, it always returns null and logs an error.
        */
      {
        theAppLog.error( "EpiNode.getEpiNode(int): base class should not be called.");
        return null;
        }

    public static EpiNode tryEpiNode(EpiInputStream<?,?,?,?> theEpiInputStream ) 
      throws IOException
      /* This method tries to parse an EpiNode.
        It returns the node if the parse successful, null otherwise.
        It tries parsing node types in the following order:
        * SequenceEpiNode
        * ScalarEpiNode
        * MapEpiNode (to be added)
       */
      { 
        EpiNode resultEpiNode= 
          SequenceEpiNode.trySequenceEpiNode(theEpiInputStream);
        if (resultEpiNode == null) 
          resultEpiNode= ScalarEpiNode.tryScalarEpiNode(theEpiInputStream); 
        return resultEpiNode;
        }

    }

class ScalarEpiNode extends EpiNode 

  {
    private String scalarString;
    
    ScalarEpiNode(String scalarString)
      {
        this.scalarString= scalarString;
        }

    public String toString() { return scalarString; }
      
    public static ScalarEpiNode tryScalarEpiNode( 
          EpiInputStream<?,?,?,?> theEpiInputStream ) 
        throws IOException
      /* This method tries to parse a EpiInputStream (YAML subset scalar string)
        from theEpiInputStream.
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
            int positionI= theEpiInputStream.getPositionI();
            toAppendAcceptedChar: {
              byteI= theEpiInputStream.read();
              if ( Character.isLetterOrDigit(byteI)) break toAppendAcceptedChar;
              if ( '-'==byteI ) break toAppendAcceptedChar;
              if ( '.'==byteI ) break toAppendAcceptedChar;
              theEpiInputStream.setPositionV(positionI); // Restore stream position.
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

  {
    private ArrayList<EpiNode> theListOfEpiNode; 

    private SequenceEpiNode(ArrayList<EpiNode> theListOfEpiNode)
      {
        this.theListOfEpiNode= theListOfEpiNode;
        }

    public String extractFromEpiNodeString(int indexI) 
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
          EpiInputStream<?,?,?,?> theEpiInputStream ) 
        throws IOException
      /* This method tries to parse a SequenceEpiNode 
        (YAML sequence of scalars) from theEpiInputStream.
        If successful then it returns the SequenceEpiNode
        and the stream is moved past the sequence characters,
        but whatever terminated the SequenceEpiNode remains to be read.
        If not successful then this method returns null 
        and the stream position is unchanged.
        */
      {
          SequenceEpiNode returnSequenceEpiNode= null; // Set default failure result.
          ArrayList<EpiNode> resultListOfEpiNodes= null;
          int initialStreamPositionI= theEpiInputStream.getPositionI();
        toReturn: { toNotASequence: {
          if (! theEpiInputStream.getByteB('[')) break toNotASequence;
          resultListOfEpiNodes=  // Always succeeds.
              getListOfEpiNodes(theEpiInputStream); 
          if (! theEpiInputStream.getByteB(']')) break toNotASequence;
          returnSequenceEpiNode= // We got everything needed.  Create successful result. 
              new SequenceEpiNode(resultListOfEpiNodes);
          break toReturn;
        } // toNotASequence: // Coming here means we failed to parse a complete sequence.
          theEpiInputStream.setPositionV(initialStreamPositionI); // Restore position.
        } // toReturn:
          return returnSequenceEpiNode; // Return result.
        }

  protected static ArrayList<EpiNode> OLDgetListOfEpiNodes( ////
      EpiInputStream<?,?,?,?> theEpiInputStream ) 
    throws IOException
  /* This method parses and returns a List of 
    0 or more elements of a sequence of scalar nodes.  
    It always succeeds, though it might return an empty list.
    The stream is advanced past all characters that were processed,
    which might be none if the returned list is empty.
    */
  {
      ArrayList<EpiNode> resultListOfEpiNodes= 
          new ArrayList<EpiNode>(); // Create initially empty result list.
    toReturn: {
      EpiNode theEpiNode=  // Try getting a first list element.
          EpiNode.tryEpiNode(theEpiInputStream);
      if (theEpiNode == null) break toReturn; // Exit if no first element.
      while (true) { // Accumulating list elements until sequence ends.
        resultListOfEpiNodes.add(theEpiNode); // Append element to list.
        int preCommaPositionI= theEpiInputStream.getPositionI();
        if (! theEpiInputStream.tryByteB(',')) break toReturn; // Exit if no comma.
        theEpiNode=  // Got comma. so try getting a next element.
            EpiNode.tryEpiNode(theEpiInputStream);
        if (theEpiNode == null)  { // No next element so restore position and exit.
          theEpiInputStream.setPositionV(preCommaPositionI);
          break toReturn;
          }
        }
    } // toReturn:
      return resultListOfEpiNodes;
    }

  protected static ArrayList<EpiNode> getListOfEpiNodes(
      EpiInputStream<?,?,?,?> theEpiInputStream ) 
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
            EpiNode.tryEpiNode(theEpiInputStream);
        if (! gotCommaB) // Comma not gotten yet so looking for the first element
          { if (theEpiNode == null) // but there is no first element
            break toReturn; // so exit now with an empty list.
            }
        else // Comma was gotten so we need a non-first element.
          { if (theEpiNode == null) { // but there was no element so
              theEpiInputStream.setPositionV( // restore stream position to before comma.
                  preCommaPositionI);
              break toReturn; // and exit now with a non-empty list.
              }
            }
        resultListOfEpiNodes.add(theEpiNode); // Append gotten element to list.
        preCommaPositionI= theEpiInputStream.getPositionI();
        if (! theEpiInputStream.tryByteB(',')) break toReturn; // Exit if no comma.
        gotCommaB= true; // Got comma, so record it.
        } // while(true)
    } // toReturn:
      return resultListOfEpiNodes;
    }

    }

class MapEpiNode extends EpiNode //// this class is under construction. 

  {
    @SuppressWarnings("unused") ////
    private LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap; 
  
    private MapEpiNode(LinkedHashMap<EpiNode,EpiNode> theLinkedHashMap)
      {
        this.theLinkedHashMap= theLinkedHashMap;
        }

    public static MapEpiNode tryMapEpiNode( 
          EpiInputStream<?,?,?,?> theEpiInputStream ) 
        throws IOException
      /* This method tries to parse a MapEpiNode (YAML map) from theEpiInputStream.
        If successful then it returns the MapEpiNode
        and the stream is moved past the map characters,
        but whatever terminated the MapEpiNode remains to be read.
        If not successful then this method returns null 
        and the stream position is unchanged.
        
        Parsing maps is tricky because, though they contain entries,
        and entries are always parsed as if a single entrity, 
        entries do not exist outside of maps.
        Only their component key and value exist outside of maps.
        */
      {
          MapEpiNode resultMapEpiNode= null; // Set default failure result.
          LinkedHashMap<EpiNode,EpiNode> theLinkedHashMapOfEpiNode= null;
          int initialStreamPositionI= theEpiInputStream.getPositionI();
        toReturn: { toNotAMap: {
          if (! theEpiInputStream.getByteB('{')) break toNotAMap;
          theLinkedHashMapOfEpiNode=  // Always succeeds.
              getLinkedHashMap(theEpiInputStream); 
          if (! theEpiInputStream.getByteB('}')) break toNotAMap;
          resultMapEpiNode= // We got everything needed.  Create successful result. 
              new MapEpiNode(theLinkedHashMapOfEpiNode);
          break toReturn;
        } // toNotAMap: // Coming here means we failed to parse a complete map.
          theEpiInputStream.setPositionV(initialStreamPositionI); // Restore position.
        } // toReturn:
          return resultMapEpiNode; // Return result.
        }

    protected static LinkedHashMap<EpiNode,EpiNode> getLinkedHashMap(
        EpiInputStream<?,?,?,?> theEpiInputStream ) 
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
        EpiNode keyEpiNode= null; // If not null then map entry is not valid.
        EpiNode valueEpiNode= null; // Optional value, null for now.
        while (true) { // Accumulating map entries until they end.
            int preMapEntryPositionI= theEpiInputStream.getPositionI();
          toEndEntry: { toNoEntry: {
            valueEpiNode= null; // Assume no value node unless one provided.
            keyEpiNode= EpiNode.tryEpiNode(theEpiInputStream); // Try parsing a key node.
            if (keyEpiNode == null) break toNoEntry; // Got no key so no entry.
            if (! theEpiInputStream.tryByteB(':')) // No separating colon 
              break toEndEntry; // so no value, so end map entry now.
            valueEpiNode= EpiNode.tryEpiNode(theEpiInputStream); // Try parsing value.
            if (valueEpiNode != null) break toEndEntry; // Got value so complete entry.
          } // toNoEntry: Being here means unable to parse an acceptable map entry.
            keyEpiNode= null; // Be certain to indicate map entry parsing failed.
            theEpiInputStream.setPositionV(preMapEntryPositionI); // Rewind input steam.
          } // toEndEntry: Being here means entry parsing is done, either pass or fail.
            if (! gotCommaB) // Comma not gotten yet so we want the first map entry
              { if (keyEpiNode == null) // but there was no first map entry
                  break toReturn; // so exit now with an empty map.
                }
              else // Comma was gotten so we need a non-first map entry.
              { if (keyEpiNode == null) { // but there was no map entry so
                  theEpiInputStream.setPositionV( // restore input stream position 
                      preCommaPositionI); // to position before comma.
                  break toReturn; // and exit now with a non-empty map.
                  }
                }
            resultLinkedHashMap.put(keyEpiNode,valueEpiNode); // Append entry to map.
            preCommaPositionI= theEpiInputStream.getPositionI(); // Save stream position.
            if (! theEpiInputStream.tryByteB(',')) break toReturn; // Exit if no comma.
            gotCommaB= true; // Got comma, so record it for earlier map entry processing.
            } // while(true)  Looping to try for another non-first map entry.
      } // toReturn:
        return resultLinkedHashMap;
      }

  }
