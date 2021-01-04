package allClasses.epinode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import allClasses.RandomAccessInputStream;

public class SequenceEpiNode extends EpiNode

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
