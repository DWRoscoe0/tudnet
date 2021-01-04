package allClasses.epinode;

import static allClasses.AppLog.theAppLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import allClasses.RandomAccessInputStream;

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

    ///enh Maybe add the capability to parse special entities,
    for example:
    * A single-element map.
    * A single-element map whose value is a nested map.
    * A map whose keys are scalars only (already exists).
    * A single element map with a nested empty as a value
      from a string without a ":" or "{}".
    Selections can be made with an options argument 
    which is passed to general-purpose parsers.
    This allows general-purpose parser code 
    to be reused for special purposes.
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
      such as maps, in case there are any. 
      
      This method returns the EpiNode if the parse is successful, null otherwise.
      It tries parsing node types in the following order:
      * ScalarEpiNode
      * MapEpiNode
      This method does not support sequences, and map keys must be scalars only.
      */
    {
        EpiNode resultEpiNode= null;

      toReturn: {

        resultEpiNode= 
            ScalarEpiNode.tryScalarEpiNode(theRandomAccessInputStream);
        if (resultEpiNode != null) break toReturn;

        resultEpiNode= MapEpiNode.getBlockMapEpiNode(
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

    public MapEpiNode tryOrLogMapEpiNode(String idString)
      /* This method acts the same as tryMapEpiNode() except that
       * it makes a log entry if it can not return a MapEpiNode.
       * The message includes messageString.
       */
      {
        MapEpiNode theMapEpiNode= tryMapEpiNode();
        if (null == theMapEpiNode)
          theAppLog.debug(
            "MapEpiNode.tryOrLogMapEpiNode() not MapEpiNode: "+idString);
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
