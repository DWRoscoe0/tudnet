package allClasses;

import java.util.HashMap;
import javax.swing.tree.DefaultMutableTreeNode;

public class ITreeNode

  extends DefaultMutableTreeNode
  
  /* This class was originally created for convenience because
    DefaultMutableTreeNode is such a long and clumsy 
    type name to use a lot.
    It was later expanded to store values for the DagInfo tree.
    */
    
  { // class ITreeNode.
  
    // Variables.
    
      private static final long serialVersionUID = 1L;
      // public boolean AutoExpandedB= false;  
    
      private HashMap< String, Object > TheHashMap;  // For storage of attribute.

    // Constructor.
    
      public ITreeNode(Object ObjectUserIn)
        /* constructor.  */
        {
          super( ObjectUserIn );  // call superclass constructor.  

          TheHashMap =  // For storage of attribute.
            new HashMap<String, Object>( 2 );  // Construct only a little one at first.
          }

    // Other methods.

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
