package allClasses;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
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

      private MetaFileManager theMetaFileManager;

      private ArrayList< IDNumber > TheArrayList;  // Container for children.

    MetaChildren( MetaFileManager theMetaFileManager ) // Constructor.
      // This constructor creates an empty MetaChildren instance.
      {
        this.theMetaFileManager= theMetaFileManager;

        TheArrayList=  // Construct the child MetaNode container as...
          new ArrayList< IDNumber >( ); // ...an ArrayList of IDNumbe-s.
        }

    // Getter methods.

      public MetaNode getMetaNode( 
          DataNode keyDataNode, 
          DataNode inParentDataNode 
          )
        /* This method returns the child MetaNode 
          which is associated with DataNode keyDataNode,
          or null if there is no such MetaNode.
          inParentDataNode is used for name lookup in case
          lazy-loading is needed.
          */
        {
          MetaNode scanMetaNode;
          Piterator < MetaNode > childPiterator= // Creating iterator. 
            getPiteratorOfMetaNode( inParentDataNode );
          while (true) {
            scanMetaNode= childPiterator.getE();  // Caching candidate. 
            if ( scanMetaNode == null ) // Exiting if no more candidates.
              break; 
            if  // Exiting if name Strings match.
            	( scanMetaNode.compareNamesWithSubstitutionB( keyDataNode) )
              break; 
            childPiterator.nextE(); // Going to next candidate.
            }
          return scanMetaNode;
          }

      /* Methods for getting iterators.  This needs organizing because
        there should be one base iterator which does 
        lazy loading of child MetaNodes on which all other code,
        including other special iterators, relies.
        There are 4 of them now!  There should be fewer, maybe only one.
        */

        public Piterator<MetaNode> getPiteratorOfMetaNode( 
            DataNode inParentDataNode 
            )
          /* This method returns a Piterator for 
            this MetaChildren's child MetaNodes.  
            It DOES, or WILL DO, lazy loading.
            It has parameter inParentDataNode for use in name lookup
            in case lazy loading is triggered.
            */
          { 
            return  // Piterator built from iterator.
              new Piterator<MetaNode>( 
                getLoadingListIteratorOfMetaNode( inParentDataNode )
                );
            }

        public ListIterator<IDNumber> getMaybeLoadingIteratorOfMetaNode( 
            DataNode inParentDataNode 
            )
          /* This method returns an iterator for the child MetaNodes. 
            Whether the iterator is able to do lazy loading depends on 
            whether forced loading is enabled when this method is called.
            It uses parameter inParentDataNode for name lookup
            in case a lazy load is triggered.
            */
          { 
            ListIterator<IDNumber> aListIiteratorOfIDNumber;
            if  // Selecting iterator based on forced loading status.
              ( theMetaFileManager.getForcedLoadingEnabledB() )
              { // Selecting lazy-loading iterator.
                @SuppressWarnings("unchecked")
                ListIterator<IDNumber> anotherListIiteratorOfIDNumber= 
                  (ListIterator<IDNumber>)
                  (ListIterator<?>)
                  getLoadingListIteratorOfMetaNode( inParentDataNode );
                aListIiteratorOfIDNumber= anotherListIiteratorOfIDNumber; 
                } // Get lazy loading ListIterator.
              else
              aListIiteratorOfIDNumber=  // Selecting non-loading iterator.
                getListIteratorOfIDNumber();
            return aListIiteratorOfIDNumber; // Returning the iterator.
            }

        public ListIterator<MetaNode> getLoadingListIteratorOfMetaNode( 
            DataNode inParentDataNode 
            )
          /* This method returns a ListIterator for the child MetaNodes. 
            It does lazy loading if needed.
            It uses parameter inParentDataNode for name lookup
            in case a lazy load is triggered.
            */
          { 
            ListIterator<IDNumber> aListIteratorOfIDNumber= 
              getListIteratorOfIDNumber();
            ListLiteratorOfIDNumber aListLiteratorOfIDNumber= 
              theMetaFileManager.makeListLiteratorOfIDNumber( 
                aListIteratorOfIDNumber,
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

      public void rwRecurseFlatV( 
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

          As usual, MetaNode parentDataNode is used for name lookup 
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
                  theMetaFileManager.rwFlatOrNestedMetaNode(   // Write MetaNode.
                    inMetaFile, (MetaNode)TheIDNumber, (DataNode)null 
                    );
                  else  // Is IDNumber.
                  IDNumber.rwIDNumber(    // Write IDNumber.
                    inMetaFile, TheIDNumber );
                else  // Reading.
                ChildListIterator.set( // Replace the child by the...
                  inMetaFile.readAndConvertIDNumber( // ...MetaNode equivalent...
                    TheIDNumber,  // ...of IDNumber using...
                    parentDataNode  // ...provided parent for lookup.
                    )
                  ); // read.
              } // Process this child.
            }

      public boolean purgeTryB( DataNode inParentDataNode )
        /* This method tries to purge child MetaNode-s which contain
          no useful information, meaning no attributes in them
          or any of their descendants.
          inParentDataNode is the parent DataNode used for 
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
              getLoadingListIteratorOfMetaNode( 
                inParentDataNode
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
