package allClasses;

public class InfogoraRoot
  //extends Object
  extends AbDataNode
  //implements DataNode
  { // class InfogoraRoot

    // Variables.
      FileRoots TheFileRoots= new FileRoots();  // the only child.
      Outline TheOutline= new Outline( 0 );  // base outline.
      Infinitree TheInfinitree= new Infinitree( null, 0 );

      
    // Constructors (none yet).  Default constructor is used.
        
    
    // A subset of delegated AbstractTreeModel methods.

      public DataNode getChild( int IndexI ) 
        /* This returns the child with index IndexI.  */
        {
          switch ( IndexI ) {
            case 0: return TheFileRoots;
            case 1: return TheOutline;
            case 2: return TheInfinitree;
            }
          return null;  // anything else returns null, to prevent compiler warning.r
          }

      public String toString( )
        /* Returns String representing name of this Object.  */
        {
          return "Infogora-Root";
          }

    } // class InfogoraRoot
