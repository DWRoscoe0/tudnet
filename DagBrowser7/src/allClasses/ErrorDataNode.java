package allClasses;

public class ErrorDataNode 
  extends AbDataNode

  /* This class is a DataNode that serves as a place-holder
    to avoid null pointer references and to indicate an error.  */

  { // class ErrorDataNode
  
    private static ErrorDataNode TheErrorDataNode= // The single instance.
      new ErrorDataNode();
      
      // ??? convert to Singleton and test in MetaRoot constructor.

    private ErrorDataNode( ) 
      { 
        System.out.print( "  ErrorDataNode( ) " );
        }  // To make debugging easier.

    public String GetNameString( )
      /* Returns String representing name of this Object.  */
      {
        return "ErrorDataNode";
        }
        
    public static ErrorDataNode getSingletonErrorDataNode( )
      { 
        System.out.print( "  getSingletonErrorDataNode( ) " );
        return TheErrorDataNode; 
        }

    } // class ErrorDataNode
