package allClasses.epinode;

import static allClasses.AppLog.theAppLog;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import allClasses.RandomAccessInputStream;
import allClasses.SystemSettings;

public class MapEpiNode extends EpiNode 

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
            if (keyEpiNode == null) // Got no key so no entry 
              break toNoEntry; // so fail this entry.
            if (! tryByteB(theRandomAccessInputStream,':')) // No separator ":"
              break toNoEntry; // so fail this entry.
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

    public static MapEpiNode getBlockMapEpiNode( 
        RandomAccessInputStream theRandomAccessInputStream, int minIndentI ) 
        throws IOException
      /* This method is like tryBlockMapEpiNode(.) except instead of returning null,
        it returns and empty MapEpiNode if it finds no map entries. 
        */
      { 
        MapEpiNode resultMapEpiNode= // Try for a map with at least one entry. 
            tryBlockMapEpiNode( theRandomAccessInputStream, minIndentI );
        if (null == resultMapEpiNode) // If failure
          resultMapEpiNode= new MapEpiNode(); // set result to be an empty map.
        return resultMapEpiNode;
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
              // Try parsing indented entries into a HashMap.
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
                    mapEntryIndentI+1); // using a higher minimum indentation.
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
      /* This method tries to skip past spaces, comments, newlines and 
        indentation characters in theRandomAccessInputStream.
        This method is called when the beginning of a new node is expected.
        * If this method succeeds then:
          * It means that at least one newline was processed and
            the indentation level after the last newline at least minIndentI.
          * It returns a number > 0 which is the new indentation level.
          * The stream is moved past all characters that were processed.
        * If this method fails then:
          * It means that no newlines were seen or
            the indentation level after the last newline was less than minIndentI.
          * It returns -1.
          * The stream position is unchanged.
        
        ///enh Modified to skip over comments?
        */
      {
          int firstStreamPositionI= theRandomAccessInputStream.getPositionI();
          int resultIndentI= -1;
        loop: while(true) { // Skip all spaces, comments, newlines, and indentations.
          if (! tryEndLineI(theRandomAccessInputStream)) // If no end of line 
            break loop; // exit loop.
          while (tryEndLineI(theRandomAccessInputStream)) // Skip any extras
            ; // by doing nothing for each one.
          resultIndentI= trySpacesI(theRandomAccessInputStream); // Skip indentation.
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
              valueEpiNode.tryOrLogMapEpiNode(keyString);
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

    public static MapEpiNode makeSingleEntryEmptyMapEpiNode(
          String keyString)
      {
        MapEpiNode fieldsMapEpiNode= new MapEpiNode(); // Make empty map.
        MapEpiNode messageMapEpiNode=  // Wrap in keyString map.
            MapEpiNode.makeSingleEntryMapEpiNode(keyString, fieldsMapEpiNode);
        return messageMapEpiNode;
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


    // Special methods.

    public synchronized void renameKeysV(
        String oldKeyString, String newKeyString)
      /* This method replaces instances of map keys 
        with value oldKeyString to NewKeyString.
        It is meant to be used for Persistent.txt file format changes.
        */
      { 
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

    public synchronized void moveToEndOfListV(String keyString)
      /* This method moves to the end of the map's entry list
       * the entry whose key is keyString.
       * If that entry does not exist then this method does nothing.
       */
      { 
        EpiNode valueEpiNode= // Try removing from this map the 
            removeEpiNode(keyString); // entry with the desired key.
        if (null != valueEpiNode) // If entry was removed then
          putV( // create and add a new entry, which puts it at end of list,
            keyString, valueEpiNode); // using same key and value.
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

    public EpiNode getMapEpiNode(int indexI)
      /* This method returns the value MapEpiNode at index position indexI,
        or null if there is no value or it is another type.

        ///org This could be rewritten to use an additional nested method:
           EpiNode getValueEpiNode(int indexI)
        */
      {
          MapEpiNode resultMapEpiNode= null; // Default result indicating fail.
        main: {
          Map.Entry<EpiNode,EpiNode> theMapEntry= // Try getting  
            getMapEntry(indexI); // map entry at desired index position.
          if (null == theMapEntry) // If no map entry there
            break main; // exit.
          EpiNode valueEpiNode= theMapEntry.getValue(); // Get value from entry.
          if (null == valueEpiNode) // If no value there
            break main; // exit.
          resultMapEpiNode=  // Try overriding result with 
              valueEpiNode.tryMapEpiNode(); // value converted to MapEpiNode.
          // At this point, if resultMapEpiNode != null, 
          // then it's the desired MapEpiNode value,
          // otherwise value is another type.
        } // main:
          return resultMapEpiNode;
        }

    public String getKeyString(int indexI)
      /* This method returns the String value of the key 
       * at index position indexI,
       * or null if there is no value there.

        ///org This could be rewritten to use an additional nested method:
           EpiNode getKeyEpiNode(int indexI)
       */
      {
          String resultString= null; // Default result indicating fail.
        main: {
          Map.Entry<EpiNode,EpiNode> theMapEntry= // Try getting  
            getMapEntry(indexI); // map entry at desired index position.
          if (null == theMapEntry) // If no map entry there
            break main; // exit.
          EpiNode keyEpiNode= theMapEntry.getKey(); // Get key from entry.
          if (null == keyEpiNode) // If no key there
            break main; // exit.
          resultString=  // Try overriding result with 
              keyEpiNode.toString(); // key converted to String.
          // At this point, if resultMapEpiNode != null, 
          // then it's the desired String.
        } // main:
          return resultString;
        }

    private Map.Entry<EpiNode,EpiNode> getMapEntry(int indexI) 
      /* Returns Map.Entry at position indexI, 
        or null if the indexI is out of range, or the entry itself is null.
        It finds the correct entry by iterating to the desired position 
        and returning the Map.Entry there.
        This is not fast for large maps.
        The map order is the insertion order.
        
        This method can be used as the basis for getting 
        anything from a map based on index.
        */
      { 
        Map.Entry<EpiNode,EpiNode> resultMapEntry= null;
        Set<Map.Entry<EpiNode,EpiNode>> theSetOfMapEntrys= 
            theLinkedHashMap.entrySet();
        Iterator<Map.Entry<EpiNode,EpiNode>> entryIterator= 
            theSetOfMapEntrys.iterator();
        Map.Entry<EpiNode,EpiNode> scanMapEntry;
        while(true) { // Iterate to the desired entry.
          if (! entryIterator.hasNext()) // More entries? 
            break; // No, so exit with null value.
          scanMapEntry= entryIterator.next(); // Yes, get current entry.
          if (indexI == 0) { // Is this the entry we want?
            resultMapEntry= scanMapEntry; // Yes, override result with entry.
            break; // and exit.
            }
          indexI--; // Decrement entry index down-counter.
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

    public boolean isTrueB(String keyString)
      /* This method returns true if the String value associated with keyString
        is a representation of true, false otherwise.
        */
      {
        return ("true".equalsIgnoreCase(getString(keyString)));
        }

    public boolean isFalseB(String keyString)
      /* This method returns true if the String value associated with keyString
        is a representation of false, false otherwise.
        */
      {
        return ("false".equalsIgnoreCase(getString(keyString)));
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
        or null if there is no such value or keyString is null.
        
        ///fix to not use toString() so that non-Scalar values
          produce an error string, to prevent long string results. 
        */
      { 
          String resultString= null;
        
        goReturn: {
          if (null == keyString) // If key is null
            break goReturn; // exit with null.
          
          EpiNode valueEpiNode= getEpiNode(keyString);
          if (null == valueEpiNode) // If value with that key is null
            break goReturn; // exit with null.
          
          resultString= valueEpiNode.toString();
        } // goReturn:
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
              valueEpiNode.tryOrLogMapEpiNode(keyString);
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
