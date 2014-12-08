package allClasses;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
//import java.util.HashMap;
//import java.util.Map;
import java.util.ArrayList;

//public class MetaChildren<K,V>
public class MetaChildren
  
  /* This class implements a Collection of child MetaNodes.
    It is used as a member of the MetaNode class.

    Presently is uses an ArrayList to store the child MetaNodes.
    Before that is used a HashMap and before that a LinkedHashMap,
    in which the MetaNode's DataNode was the key 
    and the MetaNode was the value.
    */

  { // class MetaChildren 

    // Instance variables.

      private ArrayList< IDNumber > TheArrayList;  // Container for children.

		// Constructors.

    // Constructor.
      
      MetaChildren() 
        /* This constructor creates an empty MetaChildren instance.  */
        {
          TheArrayList=  // Construct the child MetaNode container as...
            new ArrayList< IDNumber >( ); // ...an ArrayList of IDNumbe-s.
          }

    // Getter methods.

      public MetaNode getMetaNode( 
          Object KeyObject, 
          //MetaNode inParentMetaNode 
          DataNode inParentDataNode 
          )
        /* This method returns the child MetaNode 
          which is associated with DataNode KeyObject,
          or null if there is no such MetaNode.
          inParentMetaNode is used for name lookup in case
          lazy-loading is needed.
          */
        {
          MetaNode scanMetaNode;
          Piterator < MetaNode > ChildPiterator= 
            getPiteratorOfMetaNode( 
              ///inParentMetaNode 
              ///MetaNode.getDataNode(inParentMetaNode)
              inParentDataNode 
              );
          while (true) {
            scanMetaNode= ChildPiterator.getE();  // Cache present candidate. 
            if ( scanMetaNode == null )  // Exit if past end.
              break; 
            if  // Exit if found.
              ( KeyObject.equals(scanMetaNode.getDataNode()) )
              break; 
            ChildPiterator.nextE();  // Advance Piterator to next candidate.
            }
          return scanMetaNode;
          }

      /* Methods for getting iterators.  This needs organizing because
        there should be one base iterator which does 
        lazy loading of child MetaNodes on which all other code,
        including other special iterators, relies.
        There are 4 of them now!  There should be fewer, maybe only one.
        */

        public Piterator<MetaNode> XgetPiteratorOfMetaNode( //
            MetaNode inParentMetaNode 
            )
          /* This method returns a Piterator for this MetaNode's 
            child MetaNodes.  
            It DOES, or WILL DO, lazy loading.
            It has parameter inParentMetaNode for use in name lookup
            in case lazy loading is triggered.
            */
          { 
            //Misc.DbgOut( "NgetPiteratorOfMetaNode()" );
            return  // Piterator built from iterator.
              new Piterator<MetaNode>( 
                ///getLoadingListIteratorOfMetaNode( inParentMetaNode ) 
                getLoadingListIteratorOfMetaNode( 
                  MetaNode.getDataNode(inParentMetaNode)
                  )
                );
            }

        public Piterator<MetaNode> getPiteratorOfMetaNode( // ???
            ///MetaNode inParentMetaNode 
            DataNode inParentDataNode 
            )
          /* This method returns a Piterator for this MetaNode's 
            child MetaNodes.  
            It DOES, or WILL DO, lazy loading.
            It has parameter inParentMetaNode for use in name lookup
            in case lazy loading is triggered.
            */
          { 
            //Misc.DbgOut( "NgetPiteratorOfMetaNode()" );
            return  // Piterator built from iterator.
              new Piterator<MetaNode>( 
                ///getLoadingListIteratorOfMetaNode( inParentMetaNode ) 
                getLoadingListIteratorOfMetaNode( 
                  ///MetaNode.getDataNode(inParentMetaNode)
                  inParentDataNode 
                  )
                );
            }

        public ListIterator<IDNumber> XgetMaybeLoadingIteratorOfMetaNode  ///
          ( MetaNode inParentMetaNode )
          /* This method returns an iterator for the child MetaNodes. 
            Whether the iterator is able to do lazy loading depends on 
            whether lazy loading is enabled when this method is called.
            It uses parameter inParentMetaNode for name lookup
            in case a lazy load is triggered.
            */
          { 
            ListIterator<IDNumber> aListIiteratorOfIDNumber;
            if  // Lazy loading enabled.
              ( MetaFileManager.getForcedLoadingEnabledB() ) 
              { // Get lazy loading ListIterator.
                @SuppressWarnings("unchecked")
                ListIterator<IDNumber> anotherListIiteratorOfIDNumber= 
                  (ListIterator<IDNumber>)
                  (ListIterator<?>)
                  getLoadingListIteratorOfMetaNode( 
                    ///inParentMetaNode 
                    MetaNode.getDataNode(inParentMetaNode)
                    );
                aListIiteratorOfIDNumber= anotherListIiteratorOfIDNumber; 
                } // Get lazy loading ListIterator.
              else
              aListIiteratorOfIDNumber=  // Get non-loading ListIterator.
                getListIteratorOfIDNumber();
            return aListIiteratorOfIDNumber;
            }

        public ListIterator<IDNumber> getMaybeLoadingIteratorOfMetaNode /// ???
          ///( MetaNode inParentMetaNode )
          ( DataNode inParentDataNode )
          /* This method returns an iterator for the child MetaNodes. 
            Whether the iterator is able to do lazy loading depends on 
            whether lazy loading is enabled when this method is called.
            It uses parameter inParentMetaNode for name lookup
            in case a lazy load is triggered.
            */
          { 
            ListIterator<IDNumber> aListIiteratorOfIDNumber;
            if  // Lazy loading enabled.
              ( MetaFileManager.getForcedLoadingEnabledB() ) 
              { // Get lazy loading ListIterator.
                @SuppressWarnings("unchecked")
                ListIterator<IDNumber> anotherListIiteratorOfIDNumber= 
                  (ListIterator<IDNumber>)
                  (ListIterator<?>)
                  getLoadingListIteratorOfMetaNode( 
                    ///inParentMetaNode 
                    ///MetaNode.getDataNode(inParentMetaNode)
                    inParentDataNode
                    );
                aListIiteratorOfIDNumber= anotherListIiteratorOfIDNumber; 
                } // Get lazy loading ListIterator.
              else
              aListIiteratorOfIDNumber=  // Get non-loading ListIterator.
                getListIteratorOfIDNumber();
            return aListIiteratorOfIDNumber;
            }

        public ListIterator<MetaNode> XgetLoadingListIteratorOfMetaNode( ///
            MetaNode inParentMetaNode 
            )
          /* This method returns a ListIterator for the child MetaNodes. 
            It does lazy loading.
            It uses parameter inParentMetaNode for name lookup
            in case a lazy load is triggered.
            */
          { 
        	  //Misc.DbgOut( "MetaChildren.getIteratorOfMetaNode(inParentMetaNode)" );
            ListIterator<IDNumber> aListIteratorOfIDNumber= 
              getListIteratorOfIDNumber();
            ListLiteratorOfIDNumber aListLiteratorOfIDNumber= 
              new ListLiteratorOfIDNumber( 
                aListIteratorOfIDNumber,
                ///inParentMetaNode
                MetaNode.getDataNode(inParentMetaNode)
                );
        	  
            @SuppressWarnings("unchecked")
            ListIterator<MetaNode> aListIiteratorOfMetaNode=  // Caste it.
              (ListIterator<MetaNode>)
              (ListIterator<?>)
              aListLiteratorOfIDNumber;
            
            return aListIiteratorOfMetaNode;
            }

        public ListIterator<MetaNode> getLoadingListIteratorOfMetaNode( /// ???
            ///MetaNode inParentMetaNode 
            DataNode inParentDataNode 
            )
          /* This method returns a ListIterator for the child MetaNodes. 
            It does lazy loading.
            It uses parameter inParentMetaNode for name lookup
            in case a lazy load is triggered.
            */
          { 
        	  //Misc.DbgOut( "MetaChildren.getIteratorOfMetaNode(inParentMetaNode)" );
            ListIterator<IDNumber> aListIteratorOfIDNumber= 
              getListIteratorOfIDNumber();
            ListLiteratorOfIDNumber aListLiteratorOfIDNumber= 
              new ListLiteratorOfIDNumber( 
                aListIteratorOfIDNumber,
                ///inParentMetaNode
                ///MetaNode.getDataNode(inParentMetaNode)
                inParentDataNode
                );
        	  
            @SuppressWarnings("unchecked")
            ListIterator<MetaNode> aListIiteratorOfMetaNode=  // Caste it.
              (ListIterator<MetaNode>)
              (ListIterator<?>)
              aListLiteratorOfIDNumber;
            
            return aListIiteratorOfMetaNode;
            }

        private ListIterator<IDNumber> getListIteratorOfIDNumber()
          /* This private method returns a ListIterator for the children.
            It is the lowest level and simplest iterator this class returns.
            This iterator returns IDNumber-s, not MetaNodes. 
            It does NOT do lazy-loading of MetaNodes in flat Meta files,
            so it is called from only places that don't want lazy loading
            or does loading of their own.
            */
          { 
            return TheArrayList.listIterator();
            }

    // rw (Read/Write) processors.

      public static MetaChildren rwGroupMetaChildren( 
          MetaFile inMetaFile, 
          MetaChildren inMetaChildren,
          MetaNode parentMetaNode
          )
        throws IOException
        /* This rw-processes the MetaChildren.
          If inMetaChildren != null then it writes the children
            to the MetaFile, and parentMetaNode is ignored.
          If inMetaChildren == null then it reads the children
            using parentMetaNode to look up DataNode names,
            and returns a new MetaChildren instance as the function value.
          */
        {
          //Misc.DbgOut( "MetaChildren.rwGroupMetaChildren(..)" );  // Debug.

          inMetaFile.rwListBegin( );
          inMetaFile.rwLiteral( " MetaChildren" );

          if ( inMetaChildren == null )
            ///inMetaChildren= readMetaChildren( inMetaFile, parentMetaNode );
            inMetaChildren= 
              readMetaChildren( inMetaFile, MetaNode.getDataNode(parentMetaNode) );
            else
            writeMetaChildren( 
              ///inMetaFile, inMetaChildren, parentMetaNode 
              inMetaFile, inMetaChildren, MetaNode.getDataNode(parentMetaNode)
              );

          inMetaFile.rwListEnd( );
          return inMetaChildren;
          }

      public static MetaChildren XreadMetaChildren( ///
          MetaFile inMetaFile, MetaNode parentMetaNode 
          )
        throws IOException
        /* This reads a MetaChildren from MetaFile inMetaFile
          and returns it as the result.  
          It uses parentMetaNode for name lookups.  
          */
        {
          //Misc.DbgOut( "MetaChildren.readMetaChildren()" );
          MetaChildren newMetaChildren =    // Initialize newMetaChildren to be...
            new MetaChildren( ); // ...an empty default instance.
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
                      MetaNode.rwFlatOrNestedMetaNode(  // ???
                        inMetaFile, 
                        null, 
                        ///parentMetaNode
                        MetaNode.getDataNode(parentMetaNode) 
                        );
                    break;
                  }
              newMetaChildren.add(  // Store...
                newIDNumber // ...the new child MetaNode.
                );
              } // Read a child or exit.
          return newMetaChildren;  // Return resulting MetaChildren instance.
          }

      private static MetaChildren readMetaChildren( 
          ///MetaFile inMetaFile, MetaNode parentMetaNode 
          MetaFile inMetaFile, DataNode parentDataNode 
          )
        throws IOException
        /* This reads a MetaChildren from MetaFile inMetaFile
          and returns it as the result.  
          It uses parentMetaNode for name lookups.  
          */
        {
          //Misc.DbgOut( "MetaChildren.readMetaChildren()" );
          MetaChildren newMetaChildren =    // Initialize newMetaChildren to be...
            new MetaChildren( ); // ...an empty default instance.
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
                      MetaNode.rwFlatOrNestedMetaNode(  // ???
                        inMetaFile, 
                        null, 
                        ///parentMetaNode
                        //MetaNode.getDataNode(parentMetaNode) 
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

      public void add( IDNumber InIDNumber )
        /* This method adds child InIDNumber to this MetaChildren instance.
          IDNumber is the superclass of MetaNode,
          and might be added as a MetaNode place-holder 
          during reading from disk.
          If the new child is an actual MetaNode then there should not 
          already be a MetaNode child with the same DataNode.
          */
        { 
          TheArrayList.add( InIDNumber );  // Add the child object.
          }

      public static void XwriteMetaChildren( ///
          MetaFile inMetaFile, 
          MetaChildren inMetaChildren, 
          MetaNode inParentMetaNode
          )
        throws IOException
        /* This writes the MetaChildren instance inMetaChildren
          to MetaFile inMetaFile.
          If MetaFile.TheRwStructure == FLAT then it writes 
          a flat file containing IDNumber nodes only,
          otherwise it recursively writes the complete MetaNodes hierarchy.
          inParentMetaNode is used for name lookup if MetaNodes
          must be lazy-loaded before being written.
          */
        {
          //Misc.DbgOut( "MetaChildren.writeMetaChildren(..)" );
          Iterator<IDNumber> theIteratorOfIDNumber=  // Create child iterator...
            inMetaChildren.getMaybeLoadingIteratorOfMetaNode(
              ///inParentMetaNode
              MetaNode.getDataNode(inParentMetaNode)
              );
          while // Write all the children.
            ( theIteratorOfIDNumber.hasNext() ) // There is a next child.
            { // Write one child.
              IDNumber TheIDNumber=   // Get the child.
                theIteratorOfIDNumber.next();
              switch // Write child based on RwStructure.
                ( inMetaFile.getRwStructure() )
                {
                  case FLAT:
                    TheIDNumber.rw( inMetaFile );  // Write ID # only.
                    break;
                  case NESTED:
                    if (TheIDNumber instanceof MetaNode)
                      MetaNode.rwFlatOrNestedMetaNode(   // Write MetaNode.
                        ///inMetaFile, (MetaNode)TheIDNumber, (MetaNode)null );
                        inMetaFile, (MetaNode)TheIDNumber, (DataNode)null );
                      else
                      IDNumber.rwIDNumber( inMetaFile, TheIDNumber );
                    break;
                  }
              } // Write one child.
          }

      private static void writeMetaChildren( /// ???
          MetaFile inMetaFile, 
          MetaChildren inMetaChildren, 
          ///MetaNode inParentMetaNode
          DataNode inParentDataNode
          )
        throws IOException
        /* This writes the MetaChildren instance inMetaChildren
          to MetaFile inMetaFile.
          If MetaFile.TheRwStructure == FLAT then it writes 
          a flat file containing IDNumber nodes only,
          otherwise it recursively writes the complete MetaNodes hierarchy.
          inParentMetaNode is used for name lookup if MetaNodes
          must be lazy-loaded before being written.
          */
        {
          //Misc.DbgOut( "MetaChildren.writeMetaChildren(..)" );
          Iterator<IDNumber> theIteratorOfIDNumber=  // Create child iterator...
            inMetaChildren.getMaybeLoadingIteratorOfMetaNode(
              ///inParentMetaNode
              ///MetaNode.getDataNode(inParentMetaNode)
              inParentDataNode
              );
          while // Write all the children.
            ( theIteratorOfIDNumber.hasNext() ) // There is a next child.
            { // Write one child.
              IDNumber TheIDNumber=   // Get the child.
                theIteratorOfIDNumber.next();
              switch // Write child based on RwStructure.
                ( inMetaFile.getRwStructure() )
                {
                  case FLAT:
                    TheIDNumber.rw( inMetaFile );  // Write ID # only.
                    break;
                  case NESTED:
                    if (TheIDNumber instanceof MetaNode)
                      MetaNode.rwFlatOrNestedMetaNode(   // Write MetaNode.
                        ///inMetaFile, (MetaNode)TheIDNumber, (MetaNode)null );
                        inMetaFile, (MetaNode)TheIDNumber, (DataNode)null );
                      else
                      IDNumber.rwIDNumber( inMetaFile, TheIDNumber );
                    break;
                  }
              } // Write one child.
          }

      public void XrwRecurseFlatV(  /// ???
          MetaFile inMetaFile, MetaNode parentMetaNode
          )
        throws IOException
        /* This method is used to recursively read-write process
          the children of this MetaChildren instance,
          using the MetaFile inMetaFile.
          It should be called only in FLAT mode, meaning when
          ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT ).

          The difference between this method and rwGroupMetaChildren(..)
          is that this method processes the children in the text file
          as part of a flat sequence of child MetaNodes
          not as a syntactically enclosed MetaChildren-group of MetaNodes.
          The text sequence should be a flat list of flat children, 
          using IDNumbers to refer to nested children.
          
          For each child:
          * If writing then it writes the MetaNode or IDNumber,
            whichever is the class of the child.
            If the child has descendants of its own then
            these will also be written, recursively,
            following the child.
          * If reading then the present child should be 
            an IDNumber instance whose IDNumber value was read earlier.
            In this case readAndConvertIDNumber(..) will be called to 
            search the file text for the unique MetaNode with 
            the same IDNumber value.  The IDNumber instance 
            will be replaced with a new constructed MetaNode instance
            with same IDNumber value.
            This is not lazy-loading, but the replacement process 
            is the same, so the Iterator it uses returns IDNumbers, 
            not MetaNodes.

          As usual, MetaNode parentMetaNode is used for name lookup 
          during reading, but is ignored during writing.
          */
        {
          //Misc.DbgOut( "MetaChildren.rwRecurseFlatV(..)" );
          ListIterator < IDNumber > ChildListIterator=   // Get iterator.
            getListIteratorOfIDNumber();
            
          while // rw-process all the children.
            ( ChildListIterator.hasNext() ) // There is a next child.
            { // Process this child.
              IDNumber TheIDNumber=   // Get the child.
                ChildListIterator.next();
              if // Process according to direction.
                ( inMetaFile.getMode() == MetaFileManager.Mode.WRITING )  // Writing.
                if ( TheIDNumber instanceof MetaNode )  // Is MetaNode.
                  MetaNode.rwFlatOrNestedMetaNode(   // Write MetaNode.
                    ///inMetaFile, (MetaNode)TheIDNumber, (MetaNode)null );
                    inMetaFile, (MetaNode)TheIDNumber, (DataNode)null );
                  else  // Is IDNumber.
                  IDNumber.rwIDNumber(    // Write IDNumber.
                    inMetaFile, TheIDNumber );
                else  // Reading.
                ChildListIterator.set( // Replace the child by the...
                  inMetaFile.readAndConvertIDNumber( // ...MetaNode equivalent...
                    TheIDNumber,  // ...of IDNumber using...
                    ///parentMetaNode  // ...provided parent for lookup.
                    MetaNode.getDataNode(parentMetaNode)  // ...provided parent for lookup.
                    )
                  ); // read.
              } // Process this child.
            }

      public void rwRecurseFlatV( 
          ///MetaFile inMetaFile, MetaNode parentMetaNode
          MetaFile inMetaFile, DataNode parentDataNode
          )
        throws IOException
        /* This method is used to recursively read-write process
          the children of this MetaChildren instance,
          using the MetaFile inMetaFile.
          It should be called only in FLAT mode, meaning when
          ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT ).

          The difference between this method and rwGroupMetaChildren(..)
          is that this method processes the children in the text file
          as part of a flat sequence of child MetaNodes
          not as a syntactically enclosed MetaChildren-group of MetaNodes.
          The text sequence should be a flat list of flat children, 
          using IDNumbers to refer to nested children.
          
          For each child:
          * If writing then it writes the MetaNode or IDNumber,
            whichever is the class of the child.
            If the child has descendants of its own then
            these will also be written, recursively,
            following the child.
          * If reading then the present child should be 
            an IDNumber instance whose IDNumber value was read earlier.
            In this case readAndConvertIDNumber(..) will be called to 
            search the file text for the unique MetaNode with 
            the same IDNumber value.  The IDNumber instance 
            will be replaced with a new constructed MetaNode instance
            with same IDNumber value.
            This is not lazy-loading, but the replacement process 
            is the same, so the Iterator it uses returns IDNumbers, 
            not MetaNodes.

          As usual, MetaNode parentMetaNode is used for name lookup 
          during reading, but is ignored during writing.
          */
        {
          //Misc.DbgOut( "MetaChildren.rwRecurseFlatV(..)" );
          ListIterator < IDNumber > ChildListIterator=   // Get iterator.
            getListIteratorOfIDNumber();
            
          while // rw-process all the children.
            ( ChildListIterator.hasNext() ) // There is a next child.
            { // Process this child.
              IDNumber TheIDNumber=   // Get the child.
                ChildListIterator.next();
              if // Process according to direction.
                ( inMetaFile.getMode() == MetaFileManager.Mode.WRITING )  // Writing.
                if ( TheIDNumber instanceof MetaNode )  // Is MetaNode.
                  MetaNode.rwFlatOrNestedMetaNode(   // Write MetaNode.
                    ///inMetaFile, (MetaNode)TheIDNumber, (MetaNode)null );
                    inMetaFile, (MetaNode)TheIDNumber, (DataNode)null );
                  else  // Is IDNumber.
                  IDNumber.rwIDNumber(    // Write IDNumber.
                    inMetaFile, TheIDNumber );
                else  // Reading.
                ChildListIterator.set( // Replace the child by the...
                  inMetaFile.readAndConvertIDNumber( // ...MetaNode equivalent...
                    TheIDNumber,  // ...of IDNumber using...
                    ///parentMetaNode  // ...provided parent for lookup.
                    ///MetaNode.getDataNode(parentMetaNode)  // ...provided parent for lookup.
                    parentDataNode  // ...provided parent for lookup.
                    )
                  ); // read.
              } // Process this child.
            }

      public boolean purgeTryB( MetaNode inParentMetaNode )
        /* This method tries to purge child MetaNode-s which contain
          no useful information, meaning no attributes in them
          or any of their descendants.
          inParentMetaNode is the parent MetaNode used for 
          name lookup in case of lazy-loading. 
          
          It returns true if no child MetaNode-s survived the purge,
            meaning that if the node containing these children
            had no attributes of its own then it may be purged.
          It returns false otherwise, meaning that the node 
            containing these children should not be purged,
            whether is has any of it's own attributes or not.
          */
        {
          boolean childrenPurgedB=  // Set default result of purge failure.
            false;
          Processor: {
            Iterator < MetaNode > ChildIterator= 
            	///getLoadingListIteratorOfMetaNode( inParentMetaNode );
              getLoadingListIteratorOfMetaNode( 
                MetaNode.getDataNode(inParentMetaNode) 
                );
            Scanner: while (true) { // Try scanning all  children for purging. 
              if ( ! ChildIterator.hasNext() )  //  There are no more children.
                break Scanner;  // Exit child scanner loop.
              MetaNode ChildMetaNode=  // Get a reference to...
                (MetaNode)  // ...the child MetaNode which is...
                ChildIterator.next();  // ...the next one.
              if ( ! ChildMetaNode.purgeTryB() )  // The child is not purgable.
                break Processor;  // Exit with default no-purge indication.
              ChildIterator.remove();  // Remove child from MetaChildren.
              } // Try scanning all  children for purging. 
            childrenPurgedB= true; // Override result for purge success.
            } // Processor.
          return childrenPurgedB;  // Return whether all children were purged.
          }

    } // class MetaChildren 
