package allClasses;

///import java.io.File;
import java.io.IOException;

import static allClasses.Globals.*;  // For appLogger;

public class MetaFileManager {

  /* This class got most of its code from MetaFile 
  
    ??? Eliminate static variables and methods using DependencyInjecction.

    ??? Might need to put finish() in a separate class to avoid
    infinite recursion from finish() to MetaRoot to start().

    ??? Refactor to eliminate the many public members added during
    division for dependency injection.

    */

  /* Psuedo-Singleton code.    ???
    This is made thread-safe and fast with the
    Initialization on Demand Holder (IODH) idom.
    */
  
    ///private MetaFileManager() {}  // Prevent instantiation by other classes.

    ///private static MetaFileManager theMetaFileManager= null; // ???

    public MetaFileManager(  // Constructor for use by factory.
        Shutdowner theShutdowner
        )
      {
        appLogger.info( "MetaFileManager constructor starting.");

        this.theShutdowner= theShutdowner;
        
        ///theMetaFileManager= this;
        }

    // Instance variables.

      Shutdowner theShutdowner;

    /* ???
    private static class LazyHolder {
      private static final MetaFileManager INSTANCE = new MetaFileManager();
      }

    public static MetaFileManager XgetMetaFileManager()   // Return singleton instance.
      //{ return LazyHolder.INSTANCE; }
      { return theMetaFileManager; }
    */

  // Constants.

    private static final String FlatFileNameString= "Flat.txt";
    private static final String NestedFileNameString= "Nested.txt";

    private static final String FlatHeaderTokenString= 
      "Infogora-Flat-Meta-Data-File";
    private static final String NestedHeaderTokenString= 
      "Infogora-Nested-Meta-Data-File";

  // Static variables.

    public static MetaFile lazyLoadMetaFile= null;  // For lazy-loading.
      // It stores a reference to the MetaFile used to load
      // the root MetaNode in lazy-loading mode.
      // If more MetaNodes need to be loaded later then is used again.
      // finish() closes its associated file at app termination.
    private static boolean forcedLoadingEnabledB=   // Enable/disable.
      false;

  public void methodToPreventUnusedIdentifierCompilerWarnings() 
    // The following are references to methods I used during debugging.
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

  public static MetaFile getLazyLoadMetaFile()
    { return lazyLoadMetaFile; }

  // Start/finish methods and associated shutdown hook class.

    ///public static MetaNode start()
    public MetaNode start()
      /* Starts activity in this MetaFile class.
        It tries to read at least the root MetaNode from external file(s).
        It also prepares to run finish() at shutdown.
        It returns the root MetaNode gotten by reading, 
        or null if reading failed.
        
        ???During development it might read and write several files
        in different format to aid testing and debugging.
        */
      { // start()
        // System.out.println( "MetaFile.start()");

        MetaNode loadedMetaNode= null;  // Place for result root MetaNode.

        //Misc.DbgOut( "MetaFile.start(..) lazyLoadWholeMetaNode()");  // Debug.
        loadedMetaNode=  // Try doing a lazy-load of flat file root node.
          lazyLoadWholeMetaNode( );

        // ??? Test by above read by writing special Debug file.
        //Misc.DbgOut( "MetaFile.start(..) writeDebugFileV(..)");  // Debug.
        writeDebugFileV( loadedMetaNode );

        /*
        //Misc.DbgOut( "MetaFile.start(..) readFlatFileMetaNode()");  // Debug.
        loadedMetaNode= // Read state from flat file.  
          readFlatFileMetaNode( );

        // ??? Test by above read by writing special Debug file.
        //Misc.DbgOut( "MetaFile.start(..) writeDebugFileV(..)");  // Debug.
        writeDebugFileV( loadedMetaNode );
        */

        /*
        //Misc.DbgOut( "MetaFile.start(..) readNestedFileMetaNode(..)");  // Debug.
        loadedMetaNode= // Read state from hierarchical file.   
          readNestedFileMetaNode( );
        */

        { // Prepare to run finish() when app terminates.
          //ShutdownHook theShutdownHook = new MetaFile.ShutdownHook();
          //Runtime.getRuntime().addShutdownHook(theShutdownHook);

          ///Shutdowner.getShutdowner().addShutdownerListener(
          theShutdowner.addShutdownerListener( // ???
            new ShutdownerListener() {
              public void doMyShutdown() 
              {
                MetaFileManager.finish();  // Write any changed state information.
                }
              });
          } // Prepare to run finish() when app terminates.

        //Misc.DbgOut( "MetaFile.start(..) end.");  // Debug.

        return loadedMetaNode;  // Return the last value, if any, as result.
        } // start()

    private static void finish()
      /* Finishes activity in this MetaFile class.
        All new or modified MetaNode-s are saved to external file(s).
        This should be called before application termination.  

        Presently it writes all nodes, whether modified or not.
        Also presently it recursively writes the entire MetaNode tree.
        This will change.  ???

        Eventually only one format will be saved,
        but presently, for development and debugging,
        both nested and flat files are written.
        These should contain equivalent information.
        The order of the writes is important because
        the lazy load file Flat.txt remains for lazy loading.
        Nested.txt must be written first, 
        so that all MetaNodes will be loaded.
        Then Flat.txt can be closed.
        Then a new Flat.txt can be written.
        */
      { // finish()
        appLogger.info( "MetaFile.finish() begin.");

        //Misc.DbgOut( "MetaFile.finish(..) forcedLoadingEnabledB= true");  // Debug.
        forcedLoadingEnabledB= true;  // Turn on forced loading.

        //Misc.DbgOut( "MetaFile.finish(..) writeNestedFileV()");  // Debug.
        writeNestedFileV( MetaRoot.getRootMetaNode( ) );  // Write Nested file.
          // This provides an easy read dump of all MetaNodes.

        if ( lazyLoadMetaFile != null )  // Lazy loading file open.
          try { // Close it.
            //Misc.DbgOut( "MetaFile.finish() closing lazy-loading Flat.txt.");
            lazyLoadMetaFile.theRandomAccessFile.close( );  // Close it.
            } // Close it.
          catch ( IOException e ) {  // Process any errors.
            e.printStackTrace();
            }  // Process any errors.

        //Misc.DbgOut( "MetaFile.finish(..) writeFlatFileV()");  // Debug.
        writeFlatFileV( MetaRoot.getRootMetaNode( ) );  // Write Flat file.
          // This complete dump is what is lazy-loaded during next run.

        appLogger.info( "MetaFile.finish() end.");
        } // finish()

  public static boolean getForcedLoadingEnabledB()
    { return forcedLoadingEnabledB; }

  // Whole-file read methods.

    private static MetaNode readFlatFileMetaNode( )
      /* Reads all MetaNodes from a single Flat state file.  
        It returns the root MetaNode.

        This does the same thing as readNestedFileMetaNode( )
        except it does it with a flat file instead of a hierarchical one.
        Though having all state in a single flat state file
        is not the way flat state files will eventually be used,
        this is a good first step to going there.
        */
      {
        MetaFile theMetaFile= new MetaFile();
        // Set some appropriate mode variables.
        theMetaFile.TheRwStructure= RwStructure.FLAT;
        theMetaFile.FileNameString= FlatFileNameString;
        theMetaFile.HeaderTokenString= FlatHeaderTokenString;
        MetaNode loadedMetaNode=   // Do the actual read.
          theMetaFile.readFileMetaNode( );
        
        return loadedMetaNode;
        }

    private static MetaNode readNestedFileMetaNode( )
      /* Reads all MetaNodes from Nested state file.  
        It returns the root MetaNode.
        This will eventually exist only for debugging.
        */
      {
        MetaFile theMetaFile= new MetaFile();
        // Set some appropriate mode variables.
        theMetaFile.TheRwStructure= RwStructure.NESTED;
        theMetaFile.FileNameString= NestedFileNameString;
        theMetaFile.HeaderTokenString= NestedHeaderTokenString;
        MetaNode loadedMetaNode=   // Do the actual read.
          theMetaFile.readFileMetaNode( );
        
        return loadedMetaNode;
        }

  // Whole-file write methods.

    private static void writeFlatFileV( MetaNode inMetaNode )
      /* Writes all MetaNodes rooted at MetaNode inMetaNode
        to Flat file in FLAT format.
        If inMetaNode == null then it does nothing.
        Otherwise tt creates or overwrites the appropriate file.
        */
      {
        MetaFile theMetaFile= new MetaFile();
        // Set some appropriate mode variables.
        theMetaFile.TheRwStructure= RwStructure.FLAT;
        theMetaFile.FileNameString= FlatFileNameString;
        theMetaFile.HeaderTokenString= FlatHeaderTokenString;
        theMetaFile.writeRootedFileV( inMetaNode );
        }

    private static void writeNestedFileV( MetaNode inMetaNode )
      /* This method writes all MetaNodes rooted at MetaNode inMetaNode
        to Nested file in NESTED format.
        If inMetaNode == null then it does nothing.
        Otherwise tt creates or overwrites the appropriate file.
        */
      {
        MetaFile theMetaFile= new MetaFile();
        // Set some appropriate mode variables.
        theMetaFile.TheRwStructure= RwStructure.NESTED;
        theMetaFile.FileNameString= NestedFileNameString;
        theMetaFile.HeaderTokenString= NestedHeaderTokenString;
        theMetaFile.writeRootedFileV( inMetaNode );
        }

    public static void writeDebugFileV( MetaNode inMetaNode )
      /* This method is used for debugging this class.
        It is used for checking the result of a previous load.
        If inMetaNode == null then it does nothing.
        Otherwise tt creates or overwrites the Debug file containing
        all MetaNodes in Nested format rooted with inMetaNode.
        */
      {
        MetaFile theMetaFile= new MetaFile();
        // Set some appropriate mode variables.
        theMetaFile.TheRwStructure= RwStructure.NESTED;
        theMetaFile.FileNameString= "Debug.txt";
        theMetaFile.HeaderTokenString= "Debug-State";
        theMetaFile.writeRootedFileV( inMetaNode );  // Do the actual write.
        }

  // Method for lazy loading of FLAT files.

    private static MetaNode lazyLoadWholeMetaNode( )
      /* Reads and returns the root MetaNode from a Flat meta file.  
        Its children are stored as IDNumber instances.
        Other nodes are lazy-loaded later when, and if, needed,
        by replacing IDNumber children by equivalent MetaNode children.
        */
      {
        MetaFile theMetaFile= new MetaFile();

        theMetaFile.TheRwStructure= RwStructure.FLAT;
        theMetaFile.FileNameString= FlatFileNameString;
        theMetaFile.HeaderTokenString= FlatHeaderTokenString;
        theMetaFile.TheMode= Mode.LAZY_LOADING;
        MetaNode loadedMetaNode=   // Do the actual read.
          theMetaFile.lazyLoadFileMetaNode( );
        
        return loadedMetaNode;
        }

  }
