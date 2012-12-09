package allClasses;

import java.util.HashMap;
import java.util.LinkedHashMap;

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

    } // class ITreeNode.
