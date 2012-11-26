package allClasses;

import java.util.HashMap;
import javax.swing.tree.DefaultMutableTreeNode;

public class ITreeNode

  extends DefaultMutableTreeNode
  
  /* This class represents DagNode metadata, information about DagNodes.
    Some of that data is now stored in a variable called TheHashMap,
    but some of it is still stored in the superclass DefaultMutableTreeNode.
    
    Originally the class DefaultMutableTreeNode stored all the metadata.
    This subclass was created for the convenience of a shorter name.
    New metadata was added to it later.
    */
    
  { // class ITreeNode.
  
    // Variables.
    
      private static final long serialVersionUID = 1L;

      // public boolean AutoExpandedB= false;  // [moved to TheHashMap]
      private HashMap< String, Object > TheHashMap;  // For storage of various attribute.

    // Constructor.
    
      public ITreeNode(Object ObjectUserIn)
        /* constructor.  */
        {
          super( ObjectUserIn );  // call superclass constructor.  

          TheHashMap =  // For storage of attribute.
            new HashMap<String, Object>( 2 );  // Construct only a little one at first.
          }

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

    } // class ITreeNode.
