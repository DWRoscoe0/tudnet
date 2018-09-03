package allClasses;


import java.io.IOException;
import java.util.HashMap;
//import java.util.Iterator;
// import java.util.Map;

public class MetaNode extends IDNumber

  /* This class represents DataNode metadata, information about DagNodes.
    
    Previously metadata was stored in superclass DefaultMutableTreeNode.
    Information about previous selections was represented by
    the order of their child MetaNodes.
    
    Next the selection order information was represented by 
    the order of entries in the LinkedHashaaMap which contained the children.
    Also the remaining metadata was moved to attributes stored in
    instance variable AttributesHashMap.
    
    Next the selection order information was moved to attributes,
    and the order-preserving feature of LinkedHashMaps was no longer used.
    
    Next the children was moved to its own class, MetaChildren.
    
    ?? The MetaFileManager is a dependency because
    it is used in the construction of MetaNodes
    so that their children can be lazy-loaded.
    The is not efficient use of storage, but for now it works.
    */
    
  { // class MetaNode.
  
    // Variables.

      private MetaFileManager theMetaFileManager;

      private DataNode theDataNode= null;  /* Associated DataNode for which
        this MetaNode contains meta-data.  */
      private HashMap< String, Object > AttributesHashMap= null;  /* Attributes 
        of the associated DataNode, if any.
          The Key is a String name.
          The Value is a String value.
          */
      protected MetaChildren theMetaChildren= null;  /* Child MetaNodes 
        of this MetaNode, which are associated with child DataNodes of 
        the DataNode associated with this MetaNode.  */

    /* ?? This static version was for MetaNode-DataNode confusion.
    public static DataNode getDataNode( MetaNode aMetaNode )
      { 
        if (aMetaNode == null) 
          return null;
        else 
          return aMetaNode.theDataNode;
        }
      */

    // Constructors.

      public MetaNode( MetaFileManager theMetaFileManager )
        /* Constructor of blank MetaNodes.  
          These MetaNodes are filled in by the MetaNode loader.
          */
        {
          super( 0 );  // Set superclass IDNumber to 0 for loading.

          this.theMetaFileManager= theMetaFileManager;
          }

      public MetaNode( 
          MetaFileManager theMetaFileManager, DataNode inDataNode 
          )
        /* This constructs a MetaNode associated with 
          an existing DataNode inDataNode.
          Initially it has no attributes or child MetaNodes.
          */
        {
          super( );  // Define the superclass IDNumber to non-zero.

          this.theMetaFileManager= theMetaFileManager;

          theDataNode=  // Save DataNode associated with this MetaNode.
            inDataNode; 

          AttributesHashMap =  // Initialize attributes to be...
            new HashMap< String, Object >( 2 );  // ...a small empty map.
          theMetaChildren =  // Initialize children to be...
            theMetaFileManager.makeMetaChildren( );  // ...an empty MetaChildren instance.
          }

    /* Pass-through methods to reference AttributesHashMap 
      where attributes are stored.  
      */

      public boolean containsKey( String keyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.containsKey( keyString );
          }

      public Object get( String keyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.get( keyString );
          }

      public Object remove( String keyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.remove( keyString );
          }

      public Object put( String keyString, Object valueObject ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          Object ResultObject= 
            AttributesHashMap.put( keyString, valueObject );
          return ResultObject;
          }

    // Methods for Read/Write from/to meta-data state files.

      public void rw( MetaFile inMetaFile, DataNode parentDataNode )
        throws IOException
        /* This rw-processes all fields of an existing MetaNode
          using MetaFile inMetaFile.
          Empty fields are read.  Non-empty fields are written.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
          then it processes the MetaChildren as IDNumber stubs only.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.NESTED )
          then it fully processes the MetaChildren.
          In the case of Reading, parentDataNode is used for name lookup.
          parentDataNode is ignored during Writing.
          */
        {
          inMetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          inMetaFile.rwListBegin( );  // RW the beginning of the list.
          IDNumber.rwIDNumber( inMetaFile, this );  // Rw this node's ID #.
          inMetaFile.rwIndentedLiteral( "MetaNode" ); // Label as MetaNode list.
          theDataNode= DataRw.rwDataNode(  // Rw...
            inMetaFile,  // ...with MetaFile inMetaFile...
            theDataNode,  // ...theDataNode using...
            parentDataNode // ...parentDataNode for name lookups.
            );
          AttributesHashMap=  // Rw the attributes.
            Attributes.rwAttributesHashMap( inMetaFile, AttributesHashMap );
          theMetaChildren=  // Rw...
            theMetaFileManager.rwGroupMetaChildren( // ...the children hash map...
              inMetaFile, 
              theMetaChildren, 
              this.getDataNode()  // ...using this's DataNode for lookups.
              );
          inMetaFile.rwListEnd( );  // Mark the end of the list.
          inMetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          }

    // Simple getter methods.

      public DataNode getDataNode()
        /* This returns the DataNode associated with this MetaNode.  */
        {
          return theDataNode;  // Return the user object associated with this node.
          }
    
      public MetaChildren getMetaChildren()
        /* This method returns the MetaChildren object of this MetaNode.  */
        {
          return theMetaChildren;
          }

    // Iterator factory methods.

      KeyMetaPiteratorOfMetaNode makeKeyMetaPiteratorOfMetaNode( 
      		String inKeyString 
      		)
        /* This method makes and returns a KeyMetaPiteratorOfMetaNode
          for iterating through the attributes of this MetaNode
          searching for an attribute with key inKeyString.
          */
        {
          return 
            new KeyMetaPiteratorOfMetaNode( 
              //theMetaChildren.getCollectionOfMetaNode(),
              theMetaChildren.getPiteratorOfMetaNode( 
                this.getDataNode()
                ),
              inKeyString
              );
          }

    // Attribute tester and child searcher methods.

      MetaNode getChildWithKeyMetaNode( String inKeyString )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key inKeyString.
          It returns null if no child MetaNode attribute has that key.
          */
        {
          KeyMetaPiteratorOfMetaNode // Creating iterator which does the search.  
            childKeyMetaPiteratorOfMetaNode= 
              makeKeyMetaPiteratorOfMetaNode( inKeyString );

          return  // Return first child which has the desired key.
          		childKeyMetaPiteratorOfMetaNode.getE(); 
          }

      MetaNode getChildWithAttributeMetaNode
        ( String inKeyString, Object inValueObject )
        /* This method returns the first child MetaNode, 
          if any, with an attribute with 
          key == inKeyString and value == inValueObject.
          If no child MetaNode has this attribute then null is returned.
          
          Maybe refactor this using AttributePiterator subclasses??
          */
        {
          KeyAndValueMetaPiteratorOfMetaNode 
            childKeyAndValueMetaPiteratorOfMetaNode=
              new KeyAndValueMetaPiteratorOfMetaNode( 
                //theMetaChildren.getCollectionOfMetaNode(),
                theMetaChildren.getPiteratorOfMetaNode( 
                  this.getDataNode()
                  ),
                inKeyString,
                inValueObject
                );
          return childKeyAndValueMetaPiteratorOfMetaNode.getE();
          }

    // Miscellaneous methods.

      public boolean purgeTryB()
        /* This method is used to purge MetaNode-s which contain
          no useful information, meaning no attributes.
          If this MetaNode has no attributes then it
          recursively tries purging its child MetaNode-s.
          Some descendent nodes might be purged even though
          this node can not be purged.
          
          It returns true if this MetaNode can be deleted.
          It returns false if it finds attributes in any MetaNode
          meaning the node can't be deleted.
          */
        {
          boolean OkayToPurgeB= false;  // Set default no-purge result.
          Processor: {  // Purge testing and processing.
            if ( ! AttributesHashMap.isEmpty() )  // There are attributes.
              break Processor;  // Exit with default no-purge result.
            if // Children not purgeable.
              ( ! theMetaChildren.purgeTryB( this.getDataNode() ) )
              break Processor;  // Exit with default no-purge result.
            OkayToPurgeB= true;  // Indicate okay for complete purge.
            }  // Purge testing and processing.
          return OkayToPurgeB;  // Return calculated purge result.
          }

      MetaNode addV( DataNode theDataNode )
        /* This method theDataNode to this MetaNode 
          in a child MetaNode unless it is already there.
          It creates a new child MetaNode if necessary. 
          In any case, it returns the child MetaNode referencing theDataNode.
          */
        {
          MetaNode childMetaNode=  // Try to get the MetaNode...
            theMetaChildren.getMetaNode(  // ...from the MetaChildren...
              theDataNode, // ... from entry containing theDataNode...
              this.getDataNode()  // ...using this for name lookup.
              );
          if  // Creating and adding new child MetaNode if not there already.
            ( childMetaNode == null )
            { // Creating new child MetaNode.
              childMetaNode= // Create new MetaNode with theDataNode.
                theMetaFileManager.makeMetaNode( theDataNode );
              theMetaChildren.add(   // Add...
                childMetaNode  // ... the new child MetaNode.
                );
              }
          return childMetaNode;  // Return new/old child as result.
          }

      boolean compareNamesWithSubstitutionB( DataNode inDataNode )
        /* This tests for a match between the names of inDataNode
          and the DataNode of this MetaNode.
          It returns the result of this test.
          Also, if there is a match, 
          and this MetaNode's DataNode is an UnknownDataNode, 
          which means it's probably a temporary
          place-holder for MetaData loaded from a file,
          then it replaces the UnknownDataNode with inDataNode. 
          */
        {
      	  DataNode thisDataNode= getDataNode();
  	  		String thisString =thisDataNode.getNameString( ); 
  	  		String inString =inDataNode.getNameString( );
      	  boolean matchB= // Determining whether names match.
      	  		thisString.equals( inString ); 
      	  if ( matchB )  // Attempting DataNode substitution if match.
      	  	if // Substitute if it's a temporary UnknownDataNode.  
      	  	  ( UnknownDataNode.isOneB( thisDataNode ))
      	  	  this.theDataNode= inDataNode;
      	  return matchB;
          }

      boolean eliminateAndTestForUnknownDataNodeB( 
      		MetaTool parentHoldingMetaTool
      		)
        /* This method replaces an UnknownDataNode,
          if present, in this MetaNode, with a valid DataNode.
          It assumes that parentHoldingMetaTool references
          the parent MetaNode and DataNode of this MetaNode,
          and uses its DataNode children to find one
          with the same name as this MetaNode's UnknownDataNode
          if needed.
          It returns true if an UnknownDataNode remains in this MetaNode,
          false otherwise.
          */
        {
      	  boolean resultB= false; // Assuming UnknownDataNode will be gone.

    	  	if // Replacing UnknownDataNode if present.  
	  	  	  ( UnknownDataNode.isOneB( theDataNode ))
	    	  	{ // Replacing UnknownDataNode if replacement exists.
    	  			String unknownDataNodeString= theDataNode.getNameString( );
        	  	DataNode parentDataNode=
        	  			parentHoldingMetaTool.getMetaNode().theDataNode;  
            	DataNode replacementDataNode= // Searching for name match.
            			parentDataNode.getNamedChildDataNode( unknownDataNodeString );
    	  		  if ( replacementDataNode != null ) // Replacing if match found.
    	  		    theDataNode= replacementDataNode; // Replacing.
    	  		  	else
    	  		    resultB= true; // Indicating UknownDataNode remains.
    	  		  }

    	  	return resultB;
          }

    } // class MetaNode.
