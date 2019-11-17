package allClasses;

import java.io.IOException;
import java.util.ArrayList;

public class EpiNode 

  /* This is the base class for 
    classes meant to represent YAML-like data.
    Subclasses follow this one.
    */
  {

    }

class ScalarEpiNode extends EpiNode 

  {
    private String scalarString;
    
    ScalarEpiNode(String scalarString)
      {
        this.scalarString= scalarString;
        }

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
    private ArrayList<ScalarEpiNode> theListOfScalarEpiNode; 
    
    private SequenceEpiNode(ArrayList<ScalarEpiNode> theListOfScalarEpiNode)
      {
        this.theListOfScalarEpiNode= theListOfScalarEpiNode;
        }

    public int sizeI()
      {
        return theListOfScalarEpiNode.size();
        }
    
    public ScalarEpiNode getScalarEpiNode(int indexI)
      /* This method returns the element at index indexI,
        or null if the index is out of range.
         */
      {
        return 
            ( (indexI >= 0) && (indexI < theListOfScalarEpiNode.size()))
            ? theListOfScalarEpiNode.get(indexI)
            : null;
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
          ArrayList<ScalarEpiNode> resultListOfScalarEpiNodes= null;
          int initialStreamPositionI= theEpiInputStream.getPositionI();
        toReturn: { toNotASequence: {
          if (! theEpiInputStream.getByteB('[')) break toNotASequence;
          resultListOfScalarEpiNodes=  // Always succeeds.
              getListOfScalarEpiNodes(theEpiInputStream); 
          if (! theEpiInputStream.getByteB(']')) break toNotASequence;
          returnSequenceEpiNode= // We got everything needed.  Create successful result. 
              new SequenceEpiNode(resultListOfScalarEpiNodes);
          break toReturn;
        } // toNotASequence: // Coming here means we failed to parse a complete sequence.
          theEpiInputStream.setPositionV(initialStreamPositionI); // Restore position.
        } // toReturn:
          return returnSequenceEpiNode; // Return result.
        }

  protected static ArrayList<ScalarEpiNode> getListOfScalarEpiNodes(
      EpiInputStream<?,?,?,?> theEpiInputStream ) 
    throws IOException
  /* This method parsea and returns a List of 
    0 or more elements of a sequence of scalar nodes.  
    It always succeeds, though it might return an empty list.
    The stream is advanced past all characters that were processed,
    which might be none if the returned list is empty.
    */
  {
      ArrayList<ScalarEpiNode> resultListOfScalarEpiNodes= 
          new ArrayList<ScalarEpiNode>(); // Create initially empty result list.
    toReturn: {
      ScalarEpiNode theScalarEpiNode=  // Try getting a first list element.
          ScalarEpiNode.tryScalarEpiNode(theEpiInputStream);
      if (theScalarEpiNode == null) break toReturn; // Exit if no first element.
      while (true) { // Accumulating list elements until sequence ends.
        resultListOfScalarEpiNodes.add(theScalarEpiNode); // Append element to list.
        int preCommaPositionI= theEpiInputStream.getPositionI();
        if (! theEpiInputStream.tryByteB(',')) break toReturn; // Exit if no comma.
        theScalarEpiNode=  // Got comma. so try getting a next element.
            ScalarEpiNode.tryScalarEpiNode(theEpiInputStream);
        if (theScalarEpiNode == null)  { // No next element so restore position and exit.
          theEpiInputStream.setPositionV(preCommaPositionI);
          break toReturn;
          }
        }
    } // toReturn:
      return resultListOfScalarEpiNodes;
    }

    }
