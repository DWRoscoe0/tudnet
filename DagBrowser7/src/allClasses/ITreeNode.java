package allClasses;

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
      public boolean AutoExpandedB= false;  

    // Constructor.
    
      public ITreeNode(Object ObjectUserIn)
        /* constructor.  */
        {
          super( ObjectUserIn );  // call superclass constructor.  
          }
          
    } // class ITreeNode.
