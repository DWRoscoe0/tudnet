package allClasses;


import java.io.IOException;
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

    // Constructors (2).
    
      private MetaNode( )
        /* Constructor of blank MetaNodes.  
          These MetaNodes are filled in by the MetaNode loader.
          */
        {
          super( 0 );  // Set superclass ID # to 0 so it can be loaded later.
          }
    
      public MetaNode( DataNode inDataNode )
        /* This constructs a MetaNode associated with 
          an existing DataNode inDataNode.
          Initially it has no attributes or child MetaNodes.
          */
        {
          super( );  // Assign the superclass ID # to be something meaningful.

          theDataNode=  // Save DataNode associated with this MetaNode.
            inDataNode; 

          AttributesHashMap =  // Initialize attributes to be...
            new HashMap< String, Object >( 2 );  // ...a small empty map.
          theMetaChildren =  // Initialize children to be...
            new MetaChildren( );  // ...an empty MetaChildren instance.
          }

    /* Pass-through methods which reference AttributesHashMap where 
      attributes are stored.  */

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

    // Methods for Read/Write from/to state files.

      public static MetaNode rwMetaNode
        ( MetaFile inMetaFile, MetaNode inMetaNode, MetaNode parentMetaNode )
        throws IOException
        /* This rw-processes the node inMetaNode and its MetaNode children.  
          If ( inMetaNode == null ) then it creates an empty MetaNode
          and reads values into it.
          If ( inMetaNode != null ) then it writes the fields.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
          then it expects the children to be IDNumber stubs only.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.NESTED )
          then it expects nested children.
          In the case of Reading, parentMetaNode is used for name lookup.
          parentMetaNode is ignored during Writing.
          
          It returns the MetaNode processed.
          */
        {
          //Misc.DbgOut( "MetaNode.rwMetaNode(..)" );  // Debug.
          if ( inMetaNode == null ) // If there is no MetaNode then...
            inMetaNode= new MetaNode( ); // ...create one to be filled.

          inMetaNode.rw( inMetaFile, parentMetaNode );  // rw-process fields.

          // System.out.print( " #"+inMetaNode.getTheI());  // Debug.

          return inMetaNode;  // Return the new or the original MetaNode.
          }


      private void rw( MetaFile inMetaFile, MetaNode parentMetaNode )
        throws IOException
        /* This rw-processes all fields of an existing MetaNode
          using MetaFile inMetaFile.
          Empty fields are read.  Non-empty fields are written.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
          then it processes the MetaChildren as IDNumber stubs only.
          If ( MetaFile.TheRwStructure == MetaFile.RwStructure.NESTED )
          then it fully processes the MetaChildren.
          In the case of Reading, parentMetaNode is used for name lookup.
          parentMetaNode is ignored during Writing.
          */
        {
          //Misc.DbgOut( "MetaNode.rw(..)" );  // Debug.

          inMetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          inMetaFile.rwListBegin( );  // RW the beginning of the list.
          IDNumber.rwIDNumber( inMetaFile, this );  // Rw the ID #.
          inMetaFile.rwIndentedLiteral( "MetaNode" );  // Label as MetaNode list.
          theDataNode= DataRw.rwDataNode(  // Rw...
            inMetaFile,  // ...with MetaFile inMetaFile...
            theDataNode,  // ...theDataNode using...
            parentMetaNode  // ...parentMetaNode for name lookups.
            );
          AttributesHashMap=  // Rw the attributes.
            Attributes.rwAttributesHashMap( inMetaFile, AttributesHashMap );
          theMetaChildren=  // Rw...
            MetaChildren.rwGroupMetaChildren(  // ...the children hash map...
              inMetaFile, 
              theMetaChildren, 
              this  // ...using this MetaNode for lookups.
              );
          inMetaFile.rwListEnd( );  // Mark the end of the list.
          inMetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.

          }


      public static MetaNode rwFlatOrNestedMetaNode
        ( MetaFile inMetaFile, MetaNode inMetaNode, MetaNode parentMetaNode )
        throws IOException
        /* This method will read or write one or more MetaNodes 
          from the Meta state file inMetaFile.
          It processes the parent MetaNode and its children once,
          though it might process only the ID numbers of the children
          if RwStructure is FLAT.
          It might process the child MetaNodes a second time,
          again if RwStructure is FLAT, 
          but also if Mode is NOT LAZY_LOADING.
          */
        {
          //Misc.DbgOut( "MetaNode.rwFlatOrNestedMetaNode(..)" );  // Debug.

          inMetaNode=  // Process possibly nested first/root MetaNode.
            rwMetaNode( inMetaFile, inMetaNode, parentMetaNode );
          if  // Reprocess child MetaNodes if these conditions are true.
            ( 
              ( inMetaFile.getRwStructure() == MetaFile.RwStructure.FLAT ) &&
              ( inMetaFile.getMode() != MetaFile.Mode.LAZY_LOADING )
              )
            inMetaNode.theMetaChildren.rwRecurseFlatV(  // Process the children...
              inMetaFile,  // ...with inMetaFile...
              inMetaNode  // ...using present node for name lookup.
              );

          return inMetaNode;  // Return the main MetaNode.
          }


      public static MetaNode readParticularFlatMetaNode
        ( MetaFile inMetaFile, IDNumber inIDNumber, MetaNode ParentMetaNode )
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
          * ( inMetaFile.getMode() != MetaFile.Mode.LAZY_LOADING )
          then it reads and attaches all its associated descendants.

          It returns the first MetaNode it read,
          */
        {
          //Misc.DbgOut( "MetaNode.readParticularFlatMetaNode(..)" );  // Debug.
          MetaNode resultMetaNode=  // Read one MetaNode.
            rwMetaNode( inMetaFile, null, ParentMetaNode );
          if  // Read and attach descendants if it satisfies 2 conditions.
            ( ( inIDNumber.getTheI() == resultMetaNode.getTheI() ) &&
              ( inMetaFile.getMode() != MetaFile.Mode.LAZY_LOADING )
              )
            { 
              resultMetaNode.theMetaChildren.  // With the node's children...
                rwRecurseFlatV(  // ...recurse into them...
                  inMetaFile,  // ...with inMetaFile...
                  resultMetaNode  // ...using root for name lookup.
                  );
              }

          return resultMetaNode;  // Return the main MetaNode.
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

    // Attribute tester and child searcher methods.

      MetaNode getChildWithKeyMetaNode( String InKeyString )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key InKeyString.
          It returns null if no child MetaNode attribute has that key.
          */
        {
          KeyMetaPiteratorOfMetaNode ChildKeyMetaPiteratorOfMetaNode= 
            new KeyMetaPiteratorOfMetaNode( 
              //theMetaChildren.getCollectionOfMetaNode(),
              theMetaChildren.getPiteratorOfMetaNode( this ),
              InKeyString
              );
          return ChildKeyMetaPiteratorOfMetaNode.getE();
          }

      MetaNode getChildWithAttributeMetaNode
        ( String InKeyString, Object InvalueObject )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key == InKeyString and value == InvalueObject.
          If no child MetaNode has this attribute then null is returned.
          
          Maybe refactor this use AttributePiterator subclasses???
          */
        {
          KeyAndValueMetaPiteratorOfMetaNode 
            ChildKeyAndValueMetaPiteratorOfMetaNode=
              new KeyAndValueMetaPiteratorOfMetaNode( 
                //theMetaChildren.getCollectionOfMetaNode(),
                theMetaChildren.getPiteratorOfMetaNode( this ),
                InKeyString,
                InvalueObject
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
            if ( ! theMetaChildren.purgeTryB( this ) )  // Children not purgable.
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
            theMetaChildren.getMetaNode(  // ...from the MetaChildren...
              InObject,   // ... from the entry containing InObject...
              this  // ...using this for name lookup.
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
