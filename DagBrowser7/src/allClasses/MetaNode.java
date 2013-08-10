package allClasses;


import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
//import java.util.Iterator;
// import java.util.Map;

public class MetaNode

  extends IDNumber

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
    */
    
  { // class MetaNode.
  
    // Variables.

      //public IDNumber TheIDNumber= null;  // ID #.  Moved to superclass.

      private DataNode TheDataNode= null;  /* Associated DataNode for which
        this MetaNode contains meta-data.  */
      private HashMap< String, Object > AttributesHashMap= null;  /* Attributes 
        of the associated DataNode, if any.
          The Key is a String name.
          The Value is a String value.
          */
      protected MetaChildren theMetaChildren= null;  /* Child MetaNodes 
        of this MetaNode, which are associated with child DataNodes of 
        the DataNode associated with this MetaNode.  */

    // Constructors (2).
    
      private MetaNode( )
        /* Constructor of blank MetaNodes.  
          These MetaNodes are filled in by the MetaNode loader.
          */
        {
          super( 0 );  // Set superclass ID # to 0 so it can be loaded later.
          }
    
      public MetaNode( DataNode InDataNode )
        /* This constructs a MetaNode associated with 
          an existing DataNode InDataNode.
          Initially it has no attributes or child MetaNodes.
          */
        {
          super( );  // Assign the superclass ID # to be something meaningful.

          TheDataNode=  // Save DataNode associated with this MetaNode.
            InDataNode; 

          AttributesHashMap =  // Initialize attributes to be...
            new HashMap< String, Object >( 2 );  // ...a small empty map.
          theMetaChildren =  // Initialize children to be...
            new MetaChildren( );  // ...an empty MetaChildren instance.
          }

    /* Pass-through methods which reference AttributesHashMap where 
      attributes are stored.  */

      public boolean containsKey( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.containsKey( KeyString );
          }

      public Object get( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.get( KeyString );
          }

      public Object remove( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.remove( KeyString );
          }

      public Object put( String KeyString, Object ValueObject ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          Object ResultObject= 
            AttributesHashMap.put( KeyString, ValueObject );
          return ResultObject;
          }

    // Methods for Read/Write from/to state files.

      public static MetaNode rwMetaNode
        ( MetaFile inMetaFile, MetaNode inMetaNode, DataNode ParentDataNode )
        throws IOException
        /* This rw-processes the node inMetaNode and its MetaNode children.  
          If ( inMetaNode == null ) then it creates an empty MetaNode
          and reads values into it.
          If ( inMetaNode != null ) then it writes the fields.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
          then it processes the MetaChildren as IDNumber stubs only.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.NESTED )
          then it fully processes the MetaChildren.
          In the case of Reading, ParentDataNode is used for name lookup.
          ParentDataNode is ignored during Writing.
          
          It returns the MetaNode processed.
          */
        {
          if ( inMetaNode == null ) // If there is no MetaNode then...
            inMetaNode= new MetaNode( ); // ...create one to be filled.

          inMetaNode.rw( inMetaFile, ParentDataNode );  // rw-process fields.

          Misc.DbgOut( "MetaNode.rwMetaNode(..) returning "+inMetaNode.getTheI() );  // Debug.
          return inMetaNode;  // Return the new or the original MetaNode.
          }

      private void rw( MetaFile inMetaFile, DataNode ParentDataNode )
        throws IOException
        /* This rw-processes all fields of an existing MetaNode
          using MetaFile inMetaFile.
          Empty fields are read.  Non-empty fields are written.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
          then it processes the MetaChildren as IDNumber stubs only.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.NESTED )
          then it fully processes the MetaChildren.
          In the case of Reading, ParentDataNode is used for name lookup.
          ParentDataNode is ignored during Writing.
          */
        {

          inMetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          inMetaFile.rwListBegin( );  // RW the beginning of the list.
          IDNumber.rwIDNumber( inMetaFile, this );  // Rw the ID #.
          inMetaFile.rwIndentedLiteral( "MetaNode" );  // Label as MetaNode list.
          TheDataNode= DataRw.rwDataNode(  // Rw...
            inMetaFile,  // ...with MetaFile inMetaFile...
            TheDataNode,  // ...TheDataNode using...
            ParentDataNode  // ...ParentDataNode for name lookups.
            );
          AttributesHashMap=  // Rw the attributes.
            Attributes.rwAttributesHashMap( inMetaFile, AttributesHashMap );
          theMetaChildren=  // Rw...
            MetaChildren.rwGroupMetaChildren(  // ...the children hash map...
              inMetaFile, 
              theMetaChildren, 
              TheDataNode  // ...using this DataNode for lookups.
              );
          inMetaFile.rwListEnd( );  // Mark the end of the list.
          inMetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.

          }

      public static MetaNode rwFlatOrNestedMetaNode
        ( MetaFile inMetaFile, MetaNode inMetaNode, DataNode ParentDataNode )
        throws IOException
        /* This method will read or write one or more MetaNodes 
          from the Meta state file inMetaFile.
          It will recurse into the first MetaNode's children and
          other descendants if there are any.
          It handles both:
          * NESTED mode, in which the children and other descendants 
            are nested within the first and only top level MetaNode 
            in the text file.
          * FLAT mode, in which the children and other descendants 
            follow at the top level the root first top level MetaNode.
          */
        {
          inMetaNode=  // Process possibly nested first/root MetaNode.
            rwMetaNode( inMetaFile, inMetaNode, ParentDataNode );
          if  // Recurse into the children now if in FLAT file mode.
            ( inMetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
            inMetaNode.theMetaChildren.rwRecurseFlatV(  // Process the children...
              inMetaFile,  // ...with inMetaFile...
              inMetaNode.getDataNode()  // ...using present node for name lookup.
              );

          return inMetaNode;  // Return the main MetaNode.
          }

      public static MetaNode readParticularFlatMetaNode
        ( MetaFile inMetaFile, IDNumber inIDNumber, DataNode ParentDataNode )
        throws IOException
        /* Being adapted from rwFlatOrNestedMetaNode(..).
          This method work similar to rwFlatOrNestedMetaNode(..)
          but is used only when reading a FLAT file and when
          looking for a MetaNode whose root has a particular IDNumber.
          
          First it reads one flat, single-level MetaNode.
          If its IDNumber is not equal to inIDNumber then
          it returns that MetaNode and does not recurse into the children
          by reading any more.
          If its IDNumber is equal to inIDNumber then
          it reads as many additional MetaNodes as needed
          to recurse into the children and other descendents,
          then it returns the first MetaNode it read.
          */
        {
          MetaNode resultMetaNode=  // Read one MetaNode.
            rwMetaNode( inMetaFile, null, ParentDataNode );
          if  // Recurse into the children if it has desired ID number.
            ( inIDNumber.getTheI( ) == resultMetaNode.getTheI( ) )
            resultMetaNode.theMetaChildren.  // With the nodes children...
              rwRecurseFlatV(  // ...recurse into them...
                inMetaFile,  // ...with inMetaFile...
                resultMetaNode.getDataNode()  // ...using root for name lookup.
                );

          return resultMetaNode;  // Return the main MetaNode.
          }

    // Simple getter methods.

      public DataNode getDataNode()
        /* This returns the DataNode associated with this MetaNode.  */
        {
          return TheDataNode;  // Return the user object associated with this node.
          }
    
      Collection<MetaNode> getChildrenCollectionOfMetaNode()
        /* This method returns the children of this MetaNode
          as a Collection of MetaNodes.
          */
        {
          return theMetaChildren.getCollectionOfMetaNode();
          }
    
    // Attribute tester and child searcher methods.

      MetaNode getChildWithKeyMetaNode( String InKeyString )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key InKeyString.
          It returns null if no child MetaNode attribute has that key.
          */
        {
          KeyMetaPiteratorOfMetaNode ChildKeyMetaPiteratorOfMetaNode= 
            new KeyMetaPiteratorOfMetaNode( 
              theMetaChildren.getCollectionOfMetaNode(),
              InKeyString
              );
          return ChildKeyMetaPiteratorOfMetaNode.getE();
          }

      MetaNode getChildWithAttributeMetaNode
        ( String InKeyString, Object InValueObject )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key == InKeyString and value == InValueObject.
          If no child MetaNode has this attribute then null is returned.
          
          Maybe refactor this use AttributePiterator subclasses???
          */
        {
          KeyAndValueMetaPiteratorOfMetaNode ChildKeyAndValueMetaPiteratorOfMetaNode=
            new KeyAndValueMetaPiteratorOfMetaNode( 
              theMetaChildren.getCollectionOfMetaNode(),
              InKeyString,
              InValueObject
              );
          return ChildKeyAndValueMetaPiteratorOfMetaNode.getE();
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
            if ( ! theMetaChildren.purgeTryB() )  // Children not purgable.
              break Processor;  // Exit with default no-purge result.
            OkayToPurgeB= true;  // Indicate okay for complete purge.
            }  // Purge testing and processing.
          return OkayToPurgeB;  // Return calculated purge result.
          }

      MetaNode PutChildUserObjectMetaNode( Object InObject )
        /* This method puts the Object InObject in a child MetaNode
          within this its parent MetaNode.
          It creates a new child MetaNode if one with InObject
          does not already exist.
          In either case, it returns the child MetaNode with InObject.
          */
        {
          MetaNode MapChildMetaNode=  // Try to get the MetaNode...
            theMetaChildren.get(  // ...from the MetaChildren...
              InObject   // ... from the entry containing InObject.
              );
          if ( MapChildMetaNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildMetaNode= // Create new MetaNode with desired Object.
                new MetaNode( (DataNode)InObject );
              theMetaChildren.add(   // Add...
                MapChildMetaNode  // ... the new child MetaNode.
                );
              } // Create new HashMap entry.
          return MapChildMetaNode;  // Return new/old child from map as result.
          }

    } // class MetaNode.
