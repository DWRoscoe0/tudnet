package allClasses;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;

import static allClasses.AppLog.theAppLog;


public class MetaFileManager {

  /* This class manages the file(s) that contains the external 
    representation of this app's meta-data.
    Related classes are MetaFile and MetaFileManager.Finisher.

    This meta-data is stored internally in MetaNode-s 
    organized as a tree rooted at MetaRoot.
    Each MetaNode is associated with, 
    and contains meta-data about, one DataNode.  
    DataNodes are organized as a tree rooted at the DataRoot.
    The application reads from the file(s) after the application starts,
    and it writes to the file(s) before the application terminates.

    Normally when an internal MetaNode is created from external meta-data text,
    it contains a reference to the DataNode with which it is associated.
    Sometime this can't happen, because that DataNode has not been created yet,
    for example the DataNode associated with a P2P connection which
    has not been reestablished.  In these cases, a reference too 
    a special DataNode subclass, an UnknownDataNode is used in its place.
    That reference should be replaced when the corrected DataNode is created.

    This class got most of its code from 
    an earlier version of the MetaFile class.

    ///org Might need to put finish() in a separate class to avoid
    infinite recursion from finish() to MetaRoot to start().
    This might not be possible because call to finish() is
    in a Listener referenced in start().

    ///org Refactor to eliminate the many public members added during
    division for dependency injection.

    */

  // Constants.

    private static final String flatFileNameString= "Flat.txt";
    private static final String nestedFileNameString= "Nested.txt";

    private static final String flatHeaderTokenString= 
      Config.appString + "-Flat-Meta-Data-File";
    private static final String nestedHeaderTokenString= 
      Config.appString + "-Nested-Meta-Data-File";
      
  // Instance variables injected through constructor.

    private DataRoot theDataRoot;

  // Other instance variables.

    private MetaFile lazyLoadMetaFile= null;  // For lazy-loading.
      // It stores a reference to the MetaFile used to load
      // the root MetaNode in lazy-loading mode.
      // If more MetaNodes need to be loaded later then is used again.
      // finish() closes its associated file at app termination.
    private boolean forcedLoadingEnabledB= false;  // Enable/disable.

  public MetaFileManager(  // Constructor for use by factory.
      DataRoot theDataRoot // ?? should go directly to Finisher. 
      )
    {
      //appLogger.info( "MetaFileManager constructor starting.");

      this.theDataRoot= theDataRoot;
      }
  
  public void methodToPreventUnusedIdentifierCompilerWarnings() 
    /* The following are references to methods I used during debugging.
      This method is not actually called.
      It exists, and is public, to prevent warnings.
      The methods it calls have not been tested in a long time.
      */
    {
      readFlatFileMetaNode( );
      readNestedFileMetaNode( );
      }

  // Enums.

    public enum RwStructure {  // Structure of the Meta state file text.
      // Although different, both of the following modes represent
      // a depth-first pre-order traversal.

      NESTED,  // Relationships are encoded by syntactic nesting.  
               // ID numbers are present but redumdant. 
               // Representing a general DAG is impossible.
               // Eventually this mode will be used only for debugging.

      FLAT,    // Relationships are encoded by ID number references.
               // Representing a general DAG is at least possible.

      };

    public enum Mode {  // Mode of access to file.

      READING,      // Reading entire file.
      WRITING,      // Writing entire file.
      LAZY_LOADING  // Reading nodes only when needed.

      };

  // Start/finish methods and associated shutdown hook class.

    public MetaNode start()
      /* Starts activity in this MetaFile class.
        It tries to read at least the root MetaNode from external file(s).
        It returns the root MetaNode gotten by reading, 
        or null if reading failed.
        
        ?? During development it might read and write several files
        in different format to aid testing and debugging.
        Only one written file is needed for normal operations.
        
        ?? This should probably be renamed to loadV().
        */
      { // start()
        MetaNode loadedMetaNode= null;  // Place for result root MetaNode.

        try { // Try doing a lazy-load of flat file root node.
          //appLogger.debug("MetaFileManager.start() attemptying load.");
        	loadedMetaNode= lazyLoadWholeMetaNode( );
        	}
        catch ( NumberFormatException e ) {  // Logging any errors.
          theAppLog.error(
              "MetaFileManager.start() Exception: "+e
              );
          }

        writeDebugFileV( loadedMetaNode );

        /*
        loadedMetaNode= // Read state from flat file.  
          readFlatFileMetaNode( );

        writeDebugFileV( loadedMetaNode );

        loadedMetaNode= // Read state from hierarchical file.   
          readNestedFileMetaNode( );
        */

        return loadedMetaNode;  // Return the last value, if any, as result.
        } // start()

    private void finish( MetaRoot theMetaRoot )
      /* Finishes activity in this MetaFileManager class.
        All new or modified MetaNode-s are saved to external file(s).
        This should be called before application termination.  

        Presently it writes all nodes, whether modified or not.
        Also presently it recursively writes the entire MetaNode tree.
        So if lazy-loading was active all unloaded nodes will be loaded.

        This will change??  Eventually only one format will be saved,
        but presently, for development and debugging,
        both nested and flat files are written.
        These should contain equivalent information.
        The order of the writes is important because
        the lazy load file Flat.txt remains for lazy loading.
        Nested.txt must be written first, 
        so that all MetaNodes will be loaded.
        Then Flat.txt can be closed.
        Then a new Flat.txt can be written.

        ?? This should probably be renamed to saveV().
        */
      { // finish()
        theAppLog.debug("MetaFileManager",
            "MetaFileManager.finish() begin.  This could take a while.");

        forcedLoadingEnabledB= true;  // Turn on forced loading.

        writeNestedFileV(   // Writing Nested file.
          theMetaRoot.getRootMetaNode( ) 
          );  // This simultaneously 
          // causes the lazy loading of all unloaded nodes, and
          // provides a convenient human-readable dump of all MetaNodes.
        //appLogger.debug( "MetaFileManager.finish() nested file written.");

        if   // Closing lazy-loading file if open.
          ( lazyLoadMetaFile != null )
          try { // Closing it.
          	lazyLoadMetaFile.closeV( );
            //appLogger.debug( "MetaFileManager.finish() lazyLoadMetaFile closed.");
            }
          catch ( IOException e ) {  // Processing any errors.
            e.printStackTrace();
            }

        writeFlatFileV(   // Saving complete meta-data state to Flat file.
          theMetaRoot.getRootMetaNode( ) 
          );  // This file is what will be lazy-loaded during next run.

        theAppLog.debug("MetaFileManager", "MetaFileManager.finish() end.");
        } // finish()

  public static class Finisher implements ShutdownerListener {
  
    /* This class must be constructed after MetaFileManager.
      It is used to post-process the MetaFile system at shutdown.
      */

    private MetaFileManager theMetaFileManager;
    private MetaRoot theMetaRoot;

    public Finisher(  // Constructor.
        MetaFileManager theMetaFileManager,
        MetaRoot theMetaRoot
        )
      {
        this.theMetaRoot= theMetaRoot;
        this.theMetaFileManager= theMetaFileManager;
        }
        
    public void doMyShutdown() {  // ShutdownerListener method.
      theAppLog.debug("MetaFileManager",
          "MetaFileManager.Finisher.doMyShutdown() calling finish()"
      		);

      theMetaFileManager.finish(   // Finishing MetaFile operations.
        theMetaRoot
        );
      }

    } // Finisher.

  // Factory methods.

    public SingleChildMetaNode makeSingleChildMetaNode(
        MetaNode inChildMetaNode, 
        DataNode inDataNode 
        )
      {
        return new SingleChildMetaNode( this, inChildMetaNode, inDataNode );
        }

    public ListLiteratorOfIDNumber makeListLiteratorOfIDNumber( 
        ListIterator<IDNumber> theListIteratorOfIDNumber, 
        DataNode theParentDataNode
        )
      {
        return new ListLiteratorOfIDNumber( 
          this,
          theListIteratorOfIDNumber, 
          theParentDataNode
          );
        }

    public MetaNode makeMetaNode() 
      {
        return new MetaNode( this );
        }

    public MetaNode makeMetaNode( DataNode inDataNode )
      {
        return new MetaNode( this, inDataNode );  
        }

    public MetaChildren makeMetaChildren( )
      {
        return new MetaChildren( this );
        }

    private MetaFile makeMetaFile( 
        MetaFileManager.RwStructure theRwStructure, 
        String fileNameString, 
        String headerTokenString,
        MetaFileManager.Mode theMode
        ) 
      {
        return new MetaFile( 
          this,
          theRwStructure, 
          fileNameString, 
          headerTokenString,
          theMode
          );
        }

  // Whole-file read methods.
  
    private MetaNode readFlatFileMetaNode( )
      /* Reads all MetaNodes from a single Flat state file.  
        It returns the root MetaNode.

        This does the same thing as readNestedFileMetaNode( )
        except it does it with a flat file instead of a hierarchical one.
        Though having all state in a single flat state file
        is not the way flat state files will eventually be used,
        this is a good first step to going there.
        */
      {
        MetaFile theMetaFile= makeMetaFile(
          RwStructure.FLAT,
          flatFileNameString,
          flatHeaderTokenString,
          Mode.READING
          );

        MetaNode loadedMetaNode=   // Do the actual read.
          theMetaFile.readFileMetaNode( );
        
        return loadedMetaNode;
        }

    private MetaNode readNestedFileMetaNode( )
      /* Reads all MetaNodes from Nested state file.  
        It returns the root MetaNode.
        This will eventually exist only for debugging.
        */
      {
        MetaFile theMetaFile= makeMetaFile(
          RwStructure.NESTED,
          nestedFileNameString,
          nestedHeaderTokenString,
          Mode.READING
          );

        MetaNode loadedMetaNode=   // Do the actual read.
          theMetaFile.readFileMetaNode( );
        
        return loadedMetaNode;
        }

  // Whole-file write methods.

    private void writeFlatFileV( MetaNode inMetaNode )
      /* If inMetaNode == null then it does nothing.
        Otherwise tt creates or overwrites the appropriate file and
        writes to it all MetaNodes rooted at inMetaNode in FLAT format.
        */
      {
        MetaFile theMetaFile= makeMetaFile(
          RwStructure.FLAT, 
          flatFileNameString, 
          flatHeaderTokenString,
          Mode.WRITING
          );
        theMetaFile.writeRootedFileV( inMetaNode );
        }

    private void writeNestedFileV( MetaNode inMetaNode )
      /* If inMetaNode == null then it does nothing.
        Otherwise tt creates or overwrites the appropriate file and
        writes to it all MetaNodes rooted at inMetaNode in NESTED format.
        */
      {
        MetaFile theMetaFile= makeMetaFile(
          RwStructure.NESTED, 
          nestedFileNameString, 
          nestedHeaderTokenString,
          Mode.WRITING
          );
        theMetaFile.writeRootedFileV( inMetaNode );
        }

    public void writeDebugFileV( MetaNode inMetaNode )
      /* If inMetaNode == null then it does nothing.
        Otherwise tt creates or overwrites a file useful for debugging.
        The file contains all MetaNodes in Nested format 
        rooted at inMetaNode.
        */
      {
        MetaFile theMetaFile= makeMetaFile(
          RwStructure.NESTED, 
          "Debug.txt", 
          "Debug-StateList",
          Mode.WRITING
          );
        theMetaFile.writeRootedFileV( inMetaNode );  // Do the actual write.
        }

  // Method for lazy loading of FLAT files.

    private MetaNode lazyLoadWholeMetaNode( )
      /* Reads and returns the root MetaNode from a Flat meta file.  
        Its children are stored as IDNumber instances.
        Other nodes are lazy-loaded later when, and if, needed,
        by replacing IDNumber children by equivalent MetaNode children.
        It returns null if there was an error.
        */
      {
        MetaFile theMetaFile= makeMetaFile(
          RwStructure.FLAT, 
          flatFileNameString, 
          flatHeaderTokenString,
          Mode.LAZY_LOADING
          );
        lazyLoadMetaFile= theMetaFile;  // Save this for later.
        MetaNode loadedMetaNode=   // Do the actual read.
          theMetaFile.lazyLoadFileMetaNode( );
        return loadedMetaNode;
        }

  // Miscellaneous instance methods.

    public DataRoot getTheDataRoot()
      { return theDataRoot; }

    public boolean getForcedLoadingEnabledB()
      { return forcedLoadingEnabledB; }

    public MetaFile getLazyLoadMetaFile()
      { return lazyLoadMetaFile; }

    private MetaNode rwMetaNode( 
        MetaFile inMetaFile, MetaNode inMetaNode, int idI, DataNode parentDataNode 
        )
      throws IOException
      /* This rw-processes the node inMetaNode and its MetaNode children.  
        If ( inMetaNode == null ) then it creates an empty MetaNode
        and reads values into it.
        If ( inMetaNode != null ) then it writes the fields in that node.
        If ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
        then it expects the children to be IDNumber stubs only.
        If ( MetaFile.TheRwStructure == MetaFile.RwStructure.NESTED )
        then it expects nested child MetaNodes.
        In the case of Reading, 
        parentDataNode is used finding associated DataNodes.
        parentDataNode is ignored during Writing.
        If idI == 0 then the call  is for a MetaNode with any ID number,
        otherwise the call is for a MetaNode with only a particular ID number.
        
        This method returns the MetaNode loaded.
        It might be, or it might not be the desired one.
        It might, or it might not have non ID number stub children attached.
        */
      {
        if ( inMetaNode == null ) // If there is no MetaNode then...
          inMetaNode= makeMetaNode( ); // ...create one to be filled.

        inMetaNode.rw( inMetaFile, idI, parentDataNode );  // rw-process fields.

        return inMetaNode;  // Return the new or the original MetaNode.
        }

    public MetaNode rwFlatOrNestedMetaNode( 
        MetaFile inMetaFile, MetaNode inMetaNode, DataNode parentDataNode 
        )
      throws IOException
      /* This method will read or write one or more MetaNodes 
        from the Meta state file inMetaFile.
        It processes the parent MetaNode and its children once,
        though it might process only the ID numbers of the children
        if RwStructure is FLAT.
        It might process the child MetaNodes a second time,
        again if RwStructure is FLAT, 
        but also if Mode is NOT LAZY_LOADING.
        In the case of Reading, parentDataNode is used for name lookup.
        parentDataNode is ignored during Writing.
        */
      {
        inMetaNode=  // Process possibly nested first/root MetaNode.
          rwMetaNode( inMetaFile, inMetaNode, 0, parentDataNode );
        if  // Reprocess child MetaNodes if these conditions are true.
          ( 
            ( inMetaFile.getRwStructure() == MetaFileManager.RwStructure.FLAT ) &&
            ( inMetaFile.getMode() != MetaFileManager.Mode.LAZY_LOADING )
            )
          inMetaNode.theMetaChildren.rwRecurseFlatV(  // Process the children...
            inMetaFile,  // ...with inMetaFile...
            inMetaNode.getDataNode()  // ...using DataNode for name lookup.
            );

        return inMetaNode;  // Return the main MetaNode.
        }

    public MetaNode readParticularFlatMetaNode( 
        MetaFile inMetaFile, IDNumber inIDNumber, DataNode parentDataNode 
        )
      throws IOException
      /* This method works similar to rwFlatOrNestedMetaNode(..)
        but is used only when reading a FLAT file and when
        searching for a MetaNode with a particular IDNumber.
        It is a component of MetaNode searching,
        called by MetaFile.readWithWrapFlatMetaNode(..).
        It is used for both:
        * Recursive greedy loading of entire FLAT files, and
        * Lazy-loading of single MetaNodes.

        It works as follows.
        First it reads one flat, single-level MetaNode,
        the next one in the file.
        What is does next depends on context.
        If the following condition is met:
        * The MetaNode's IDNumber is equal to inIDNumber, and
        * ( inMetaFile.getMode() != MetaFileManager.Mode.LAZY_LOADING )
        then it reads and attaches all its descendant MetaNodes.

        It returns the first MetaNode it read,

        parentDataNode is used finding associated DataNodes.
        */
      {
        MetaNode resultMetaNode=  // Read one MetaNode.
          rwMetaNode( inMetaFile, null, inIDNumber.getTheI(), parentDataNode );
        if  // Read and attach descendants if it satisfies 2 conditions.
          ( ( inIDNumber.getTheI() == resultMetaNode.getTheI() ) &&
            ( inMetaFile.getMode() != MetaFileManager.Mode.LAZY_LOADING )
            )
          { 
            resultMetaNode.theMetaChildren.  // With the node's children...
              rwRecurseFlatV(  // ...recurse into them...
                inMetaFile,  // ...with inMetaFile and...
                resultMetaNode.getDataNode() // ...DataNode for name lookup.
                );
            }

        return resultMetaNode;  // Return the main MetaNode.
        }

    public MetaChildren rwGroupMetaChildren( 
        MetaFile inMetaFile, 
        MetaChildren inMetaChildren,
        DataNode parentDataNode
        )
      throws IOException
      /* This rw-processes the MetaChildren.
        If inMetaChildren != null then it writes the children
          to the MetaFile, and parentDataNode is ignored.
        If inMetaChildren == null then it reads the children
          using parentDataNode to look up DataNode names,
          and returns a new MetaChildren instance as the function value.
        */
      {
        //Misc.DbgOut( "MetaChildren.rwGroupMetaChildren(..)" );  // Debug.

        inMetaFile.rwListBegin( );
        inMetaFile.rwLiteral( " MetaChildren" );

        if ( inMetaChildren == null )
          inMetaChildren= readMetaChildren( inMetaFile, parentDataNode );
          else
          writeMetaChildren( inMetaFile, inMetaChildren, parentDataNode );

        inMetaFile.rwListEnd( );
        return inMetaChildren;
        }

    private MetaChildren readMetaChildren( 
        MetaFile inMetaFile, DataNode parentDataNode 
        )
      throws IOException
      /* This reads a MetaChildren from MetaFile inMetaFile
        and returns it as the result.  
        It uses parentDataNode for name lookups.  
        */
      {
        //Misc.DbgOut( "MetaChildren.readMetaChildren()" );
        MetaChildren newMetaChildren =    // Initialize newMetaChildren to be...
          makeMetaChildren( ); // ...an empty default instance.
        while ( true )  // Read all children.
          { // Read a child or exit.
            IDNumber newIDNumber= null; // Variable for use in reading ahead.
            inMetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
            if  // Exit loop if end character present.
              ( inMetaFile.testTerminatorI( ")" ) != 0 )
              break;  // Exit loop.
            switch // Read child based on RwStructure.
              ( inMetaFile.getRwStructure() )
              {
                case FLAT:
                  newIDNumber= // Read a single IDNumber.
                    MetaNode.rwIDNumber( inMetaFile, null );
                  break;
                case NESTED:
                  newIDNumber=  // Read the possibly nested MetaNode.
                    rwFlatOrNestedMetaNode(
                      inMetaFile, 
                      null, 
                      parentDataNode
                      );
                  break;
                }
            newMetaChildren.add(  // Store...
              newIDNumber // ...the new child MetaNode.
              );
            } // Read a child or exit.
        return newMetaChildren;  // Return resulting MetaChildren instance.
        }

    private void writeMetaChildren(
        MetaFile inMetaFile, 
        MetaChildren inMetaChildren, 
        DataNode inParentDataNode
        )
      throws IOException
      /* This writes the MetaChildren instance inMetaChildren
        to MetaFile inMetaFile.
        If MetaFile.TheRwStructure == FLAT then it writes 
        a flat file containing IDNumber nodes only,
        otherwise it recursively writes the complete MetaNodes hierarchy.
        inParentDataNode is used for name lookup if MetaNodes
        must be lazy-loaded before being written.
        */
      {
        Iterator<IDNumber> theIteratorOfIDNumber=  // Create child iterator...
          inMetaChildren.getMaybeLoadingIteratorOfMetaNode(
            inParentDataNode
            );
        while // Write all the children.
          ( theIteratorOfIDNumber.hasNext() ) // There is a next child.
          { // Write one child.
            IDNumber theIDNumber=   // Get the child.
              theIteratorOfIDNumber.next();
            switch // Write child based on RwStructure.
              ( inMetaFile.getRwStructure() )
              {
                case FLAT:
                  theIDNumber.rw( inMetaFile );  // Write ID # only.
                  break;
                case NESTED:
                  if (theIDNumber instanceof MetaNode)
                    rwFlatOrNestedMetaNode(   // Write MetaNode.
                      inMetaFile, (MetaNode)theIDNumber, (DataNode)null );
                    else
                    IDNumber.rwIDNumber( inMetaFile, theIDNumber );
                  break;
                }
            } // Write one child.
        }

  }
