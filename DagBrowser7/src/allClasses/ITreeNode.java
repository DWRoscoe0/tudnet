package allClasses;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
// import java.util.Map.Entry;

// import javax.swing.tree.DefaultMutableTreeNode;

public class ITreeNode

  // extends DefaultMutableTreeNode
  
  /* This class represents DagNode metadata, information about DagNodes.
    Some of that data is now stored in a variable called TheHashMap,
    but some of it is still stored in the superclass DefaultMutableTreeNode.
    
    Originally the class DefaultMutableTreeNode stored all the metadata.
    This subclass was created for the convenience of a shorter name.
    New metadata was added to it later.
    */
    
  { // class ITreeNode.
  
    // Variables.
    
      // private static final long serialVersionUID = 1L;

      private Object UserObject;  // User object associated with this node.
      // public boolean AutoExpandedB= false;  // [moved to TheHashMap]
      private HashMap< String, Object > TheHashMap;  // For storage of various attribute.
      public LinkedHashMap< Object, ITreeNode  > ChildrenLinkedHashMap;  /* LRU children,
        with the Key being a user Object, 
        and the Value being the child ITreeNode that contains it. */

    // Constructor.
    
      public ITreeNode(Object ObjectUserIn)
        /* constructor.  */
        {
          //super( ObjectUserIn );  // call superclass constructor.  ???
          // super( null );  // call superclass constructor.  ???
          UserObject= ObjectUserIn;  // Save user object associated with this node.

          TheHashMap =  // For storage of attribute.
            new HashMap<String, Object>( 2 );  // Construct only a little Map at first.
          ChildrenLinkedHashMap =  // For storage of LRU child references,
            new LinkedHashMap< Object, ITreeNode  >( 
              2, // Initial size (small).
              0.75f,  // Load factor
              true  // access-order.
              );  
          }

          Object ChildUserObject= getUserObject();

    // Methods which reference TheHashMap.

      public boolean containsKey( String KeyString ) 
        /* This is a pass-through to TheHashMap. */
        { // get(..)
          return TheHashMap.containsKey( KeyString );
          } // get(..)

      public Object get( String KeyString ) 
        /* This is a pass-through to TheHashMap. */
        { // get(..)
          return TheHashMap.get( KeyString );
          } // get(..)

      public Object remove( String KeyString ) 
        /* This is a pass-through to TheHashMap. */
        { // remove(..)
          return TheHashMap.remove( KeyString );
          } // remove(..)

      public Object put( String KeyString, Object ValueObject ) 
        /* This is a pass-through to TheHashMap. */
        { // put(..)
          return TheHashMap.put( KeyString, ValueObject );
          } // put(..)

    // Other methods.

      public Object getUserObject()
        /* This returns the UserObject associated with this node.  
          At the moment it simply calls the superclass method.
          */
        { // getUserObject()
          // return super.getUserObject();  // old location.
          return UserObject;  // Return the user object associated with this node.
          } // getUserObject()

      
    // The following are low-level LRU child routines.

			ITreeNode PutLastUserChildITreeNode( Object DesiredObject )
        /* This method puts the object DesiriedObject in a child ITreeNode
          at the LRU position within this parent ITreeNode.
          It creates and uses a new ITreeNode if one with the DesiredObject
          does not already exist.
          In either case, it returns the/a ITreeNode with the DesiredObject.
          */
        { // ITreeNode PutLastUserChildITreeNode( DesiredObject )
          ITreeNode NewChildITreeNode= // Create new ITreeNode with desired Object.
            new ITreeNode( DesiredObject );  // It might or mignt not be used ahead.
        
          ITreeNode MapChildITreeNode=  // Try to get the ITreeNode and move-to-front...
            ChildrenLinkedHashMap.get(  // ...in the child LinkedHashMap...
              DesiredObject );  // ... from the entry containing the DesiredObject.
          if ( MapChildITreeNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildITreeNode= // Create new ITreeNode with desired Object.
                //new ITreeNode( DesiredObject );
                NewChildITreeNode;  
              ChildrenLinkedHashMap.put(   // Add new entry which maps...
                DesiredObject,  // ...DesiredObject to...
                MapChildITreeNode  // ... the ITreeNode containing it created earlier.
                );
              } // Create new HashMap entry.
        
          // return ChildITreeNode;  // Return new/old child as result.
          return MapChildITreeNode;  // Return new/old child from map as result.
          } // ITreeNode PutLastUserChildITreeNode( DesiredObject )

			public ITreeNode GetLastChildITreeNode(  )
        /* This method gets the last child referenced in this ITreeNode,
          or null if there is none.  InITreeNode must not be null.
          */
        { // GetLastChildITreeNode( )
          ITreeNode LastChildITreeNode= null;  // Assume there is no last child.
          
          Iterator < Map.Entry < Object, ITreeNode > > MapIterator=  // Get an iterator...
            ChildrenLinkedHashMap.entrySet().iterator();  // ...for HashMap entries.
          while // Use Iterator to get the HashMap's last Entry's Value.
            ( MapIterator.hasNext() ) // If there is a next Entry...
            LastChildITreeNode= // ...get a reference to...
              (ITreeNode)  // ...the ITreeNode which is...
              MapIterator.next().  // ...that next Entry's...
              getValue();  // ...Value.
        
          return LastChildITreeNode; // return last child ITreeNode result, if any.
          } // GetLastChildITreeNode( )

			DagNode GetRecentChildDagNode(  )
        /* This method returns the user object DagNode of 
          the most recently visited child of this ITreeNode,
          or it returns null if there is no such object.
          */
        { // GetRecentChildDagNode( )
          DagNode RecentChildDagNode= null;// assume default value of null.
          switch (0) { // override with child if there is one.
            default:  // always start here.  switch allows break-outs.
            //if (this == null)  // no ITreeNode was provided.
            //  break ;  // so keep the null result.
            ITreeNode LastChildITreeNode= GetLastChildITreeNode( );
            if (LastChildITreeNode == null)  // there is no last child.
              break ;  // so keep the default null result.
            RecentChildDagNode=  // Result recent child DagNode is...
              (DagNode)  // ...a DagNode caste of...
              LastChildITreeNode.   // ...the last child's...
              getUserObject();  // user object.
            } // override with child if there is one.
          return RecentChildDagNode; // return resulting DagNode, or null if none.
          } // GetRecentChildDagNode( )

    } // class ITreeNode.
