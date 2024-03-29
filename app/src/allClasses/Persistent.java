package allClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import allClasses.epinode.EpiNode;
import allClasses.epinode.MapEpiNode;
import allClasses.epinode.ScalarEpiNode;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class Persistent 

  /* This class implements persistent data for an app.  It is stored in 2 ways:

    * It is stored in main memory as a tree of EpiNode instances

    * It is stored on non-volatile external storage as a text file.
      EpiNode data is expressed in a YAML subset.

    Each node can be either:
    * a ScalarEpiNode value, representing a simple text string, or
    * a MapEpiNode, a nested map of key-value pairs, 
      with each value being another node.

    Unlike MapEpiNode, which does not understand paths within a tree, 
    this class does.

    However the internal use of paths 
    to identify persistent data has been deprecated. 
    Although some multiple element path capability exists, 
    it is not presently used,
    meaning that paths parameter are all single element map keys.

    If paths are ever used again:

      A path can be:
      * relative to a given map node, or
      * absolute, meaning relative to the root map node.

      A path expressed as a String is a list of elements separated by a "/".
      A path can contain 0, 1, 2, or more elements.

      In earlier versions of this class, "key" was synonymous with "full path".
      Now "key" is only a single element of a path.

      A path does not end in a slash.  A prefix ends in a slash.

    ///opt Paths used internally might eventually be eliminated completely.

    ///org Many service methods below are in the process 
    of being moved or eliminated, so that this class 
    will do nothing more than load from, and store data to, disk.
    All the methods which actually access individual fields will be in either
    * MapEpiNode methods, or
    * Methods of classes which perform a particular purpose but use 
      a subtree of EpiNodes for storage.
    This process is being done gradually because 
    the methods are called from many places.
    When all references to these service methods have been eliminated,
    the methods will be removed from this class. 

     */

  {
    private MapEpiNode rootMapEpiNode= null; // Root of tree data to persist.


    // Initialization and loading methods.

    public Persistent initializePersistent()
      /* This method loads the persistent data from an external file.
        It also does some temporary data conversions and extra file outputs
        as part of the conversion from PersistentNode data to EpiNode data. 
        It returns a reference to the initialized Persistent instance.

        It is possible to eliminate this method, 
        and trigger things by lazy loading,
        triggered by the first call that needs theMap variable defined.
        But because finalizeV() must be called to write any changes,
        we might as well just call initializeV() as well.
        */
      {
        rootMapEpiNode=  // Translate external text to internal EpiNodes
          loadMapEpiNode(Config.persistentFileString); // by loading text file.

        if  // Define root to be an empty map if the load failed.
          (rootMapEpiNode == null) 
          rootMapEpiNode= new MapEpiNode();
        
        updateFormatV();
        
        return this;
        }

    private void updateFormatV()
      /* This method was created to deal with 
       * Persistent.txt file format changes.
       * Except for logging, the code in this method should be temporary.
       */
      {
        theAppLog.debug("Persistent","Persistent.updateFormatV() begins.");
        
        renameKeysV("reconnectType", "connectBy");
        
        /// Disabled these when UnicasterIndexes kept disappearing.
        renameKeysV("peers", "Unicasters");
        /// renameKeysV("Unicasters","UnicasterIndexes");
         // Disabled because it was interfering with SelectionHistory data. 
        /// This have always been enabled.
        renameKeysV("PeerIdentity", "OwnerId");
        renameKeysV("OwnerId","UserId");
        renameKeysV("NormalPort","Port");
        
        theAppLog.debug("Persistent","Persistent.updateFormatV() ends.");
        }

    private void renameKeysV(String oldKeyString, String newKeyString)
      /* This helper method replaces instances of map keys 
        with value oldKeyString to NewKeyString.
        It is meant to be used for Persistent.txt file format changes.
        */
      { 
        theAppLog.debug("Persistent","Persistent.renameKeys(\""
          + oldKeyString + "\",\"" + newKeyString 
          + "\") called.");
        rootMapEpiNode.recursivelyRenameKeysV(oldKeyString, newKeyString);
        }

    private MapEpiNode loadMapEpiNode( String fileString )
      /* This method translates external text to internal EpiNodes
       * by loading the text file whose name is fileString
       * that is located in the standard folder.
       * It returns the resulting MapEpiNode or null if the load fails.
       */
      {
        File fileFile= FileOps.makeRelativeToAppFolderFile( fileString );
        return loadMapEpiNode(fileFile);
      }

    public static MapEpiNode loadMapEpiNode( File fileFile )
      /* This method translates external text to internal EpiNodes
       * by loading the text file whose pathname is fileFile.
       * It returns the resulting MapEpiNode or null if the load fails.
       */
      {
        MapEpiNode resultMapEpiNode= null; 

        RandomAccessInputStream theRandomAccessInputStream= null;
        RandomAccessFile theRandomAccessFile= null;

        try { 
            theRandomAccessFile= new RandomAccessFile(fileFile,"r");
            theRandomAccessInputStream= 
                new RandomFileInputStream(theRandomAccessFile);
            resultMapEpiNode= MapEpiNode.getBlockMapEpiNode(
                theRandomAccessInputStream, 0 );
            } 
          catch (FileNotFoundException theFileNotFoundException) { 
            theAppLog.warning(
                "Persistent.loadEpiNodeDataV(..)"+theFileNotFoundException);
            }
          catch (Exception theException) { 
            theAppLog.exception(
                "Persistent.loadEpiNodeDataV(..)", theException);
            }
          finally { 
            try { 
              if ( theRandomAccessInputStream != null ) 
                Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theRandomAccessInputStream);
              if ( theRandomAccessFile != null ) 
                Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theRandomAccessFile);
              }
            catch (Exception theException) { 
              theAppLog.exception(
                  "Persistent.loadEpiNodeDataV(..)", theException);
              }
            }
        return resultMapEpiNode; 
        }


    /* Change saving methods.
     * These methods are used to save Persistent data when 
     * changes are made long before the data is saved at app termination.
     */

    public void signalDataChangeV()
      /* This method should be called AFTER 
       * a change has been made that needs to be saved on disk.
       * 
       *  ///enh Change to save state less often, but often enough.
       *  Save at times that will satisfy both of the following constraints:
       *  * Do a save within 5 seconds of every change.
       *    The time of the last unsaved change must be saved.
       *  * Do a save within 1 second of the last change.
       *    The time of the last change must be saved.
       *  This logic can be placed in a method which is called
       *  * at every change and 
       *  * when the timer is triggered.
       *  A timer is set for the next scheduled save,
       *  which will often be overridden as changes are triggered.
       */
      {
        storeEpiNodeDataV();
        }

    
    // Finalization methods.

    public void finalizeV()
      {
        storeEpiNodeDataV();
        }
  
    private void storeEpiNodeDataV()
      /* This method stores the persistent data to the external file.
  
        Before it writes the data, it might do some reordering of entries.
        It does this by removing and adding particular entries.
        The most recently added entries will appear last.
        This ordering is done to reduce manual search time 
        during debugging and testing.
  
        ///enh Maybe use a similar technique to put 
          all entries whose values are MapEpiNodes last.
        */
      {
        /// This is done to put UnicasterIndexes last, done by get and put.
        /// Disabled these when UnicasterIndexes kept disappearing.
        /// EpiNode theEpiNode= rootMapEpiNode.removeEpiNode("UnicasterIndexes");
        /// rootMapEpiNode.putV("UnicasterIndexes",theEpiNode);
        
        storeEpiNodeDataV( // Write 
            rootMapEpiNode, // all EpiNode data 
            Config.persistentFileString // to this file.
            );
        }

    private void storeEpiNodeDataV( EpiNode theEpiNode, String fileString )
      /* This method stores the Persistent data that is in main memory to 
        the external text file whose name is fileString
        and is in the standard folder.
        */
      {
        File fileFile= FileOps.makeRelativeToAppFolderFile(fileString);
        storeEpiNodeDataReturnString( theEpiNode, fileFile );
        }


    public static String storeEpiNodeDataReturnString( 
        EpiNode theEpiNode, File fileFile )
      /* This method stores the Persistent data that is in main memory to 
        the external text file whose pathname is fileFile.
        It returns null if the write succeeded.
        If it failed it returns a String describing the failure. 
        */
      {
        theAppLog.debug(
            "Persistent","Persistent.storeEpiNodeDataV(.) begins.");
        theAppLog.debug("Persistent","Persistent.storeEpiNodeDataV(.) "
          + "theEpiNode=\n" + theEpiNode.toString(4));
        String errorString= FileOps.writeDataReturnString(
            (theOutputStream) -> {
              theAppLog.debug(
                  "Persistent","Persistent.storeEpiNodeDataV(.) "
                  + "write to OutputStream begins.");
              theOutputStream.write( // Write leading comment.
                  "#---YAML-like EpiNode data follows---".getBytes());
              theEpiNode.writeV( // Write all of theEpiNode tree
                theOutputStream, 
                0 // starting at indent level 0.
                );
              theOutputStream.write( // Write trailing comment.
                  (NL+"#--- end of file ---"+NL).getBytes());
              theAppLog.debug(
                  "Persistent","Persistent.storeEpiNodeDataV(.) "
                  + "write to OutputStream ends.");
              }, // source WriterTo1Throws2<OutputStream,IOException>
            fileFile // destination file File
            );
        theAppLog.debug(
            "Persistent","Persistent.storeEpiNodeDataV(.) ends.");
        return errorString;
        }
  
    public void writeInstallationSubsetComponentsV(
        OutputStream theOutputStream )
      throws IOException
      /* This method writes the components of 
       * the subset of storage needed for installations
       * to theOutputStream. */
      {
        writeSubsetComponentV( // Source of data.
            "UserId",theOutputStream); 
        writeSubsetComponentV( // Neighbor data.
            "UnicasterIndexes",theOutputStream);
        }
  
    private void writeSubsetComponentV(
        String keyString,OutputStream theOutputStream)
      throws IOException
      /* This method writes the subset component named by keyString
       * to theOutputStream. */
      {
        EpiNode keyEpiNode= new ScalarEpiNode(keyString);
        EpiNode valueEpiNode= rootMapEpiNode.getEpiNode(keyEpiNode);
        
        MapEpiNode.writeV( // Write all of theEpiNode tree
          keyEpiNode,
          valueEpiNode,
          theOutputStream, 
          0 // starting at indent level 0.
          );
        }

    public void writingCommentLineV( 
        PrintWriter thePrintWriter, String commentString ) 
      throws IOException
      /* This method writes theString followed by a newline
        to the text file OutputStream.
        */
      {
        thePrintWriter.print('#'); // Write comment character.
        writingLineV( // Write comment content as line.
            thePrintWriter, commentString);
        }
  
    public void writingLineV( PrintWriter thePrintWriter, String lineString ) 
      /* This method writes theString followed by a newline
        to the text file OutputStream.
        */
      {
        writingV(thePrintWriter, lineString); // Write string.
        thePrintWriter.println(); // Terminate line.
        }
  
    public void writingV( PrintWriter thePrintWriter, String theString ) 
      // This method writes theString to the text file OutputStream.
      {
        thePrintWriter.print(theString);
        }


    /* Service methods for get and put operations.
      
      Many of these are basically pass-through wrappers for 
      operations on the rootMapEpiNode.
      ///org They can probably be eliminated after 
      references to thePersisten are replaced by thePersistent.rootMapEpiNode
      in other classes.
      
      Some public methods could be rewritten now to eliminate private methods,
      but that would be a waste.  The callers should be rewritten
      so that both the the public and private methods can be eliminated
      simultaneously.
     */

    public MapEpiNode getOrMakeMapEpiNode(String keyString)
      /* This is equivalent to
              getOrMakeFromPathMapEpiNode(keyString)
        but is meant to be used with single element paths.
        It reports an error if keyString contains 2 or more elements.
        */
      {
        return rootMapEpiNode.getOrMakeMapEpiNode(keyString);
        }

    public MapEpiNode getRootMapEpiNode() 
      {
        return rootMapEpiNode;
        }

  }

