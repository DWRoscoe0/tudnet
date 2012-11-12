package allClasses;

import java.awt.Component;

//import java.util.ArrayList;
//import java.util.Collections;

//import javax.swing.tree.TreePath;

public class Misc
  { // class Misc 
    // flags.
      public static final boolean ReminderB= false;  // true;

    // debugging methods.
    
      public static String ComponentInfoString( Component InComponent )
        {
          if ( InComponent == null )
            return " null ";
            
          String ResultString= "";
          ResultString+= " " + InComponent.hashCode(); 
          ResultString+= " " + InComponent.getClass().getName();
          if ( InComponent.getName() != null )
            ResultString+= " name:"+InComponent.getName(); 
          return ResultString;
          }
  
    // translator methods.

/*    public static TreePath xIFileToTreePath(IFile InIFile)
        /* Translates the IFile InNodeIFile to a TreePath.  ??? */
/*      { // IFileToTreePath(IFile InNodeIFile)
          TreePath ResultTreePath= null;  // initialize result to null,
          if (InIFile != null)  // translate IFile is present.
            { // translate to TreePath.
              IFile ScanningIFile=  // initialize scanning variable...
                InIFile;  // starting at input IFile.
              ArrayList<IFile> PathArrayList =   // create empty accumulator list.
                new ArrayList<IFile>();
              while // add all IFile-s in the path to the root to PathArrayList.
                (ScanningIFile != null)  // IFile-s remain.
                { // Add next IFile to PathArrayList.
                  PathArrayList.add(ScanningIFile);  // add node.
                  String ScanningString = ScanningIFile.getParent();  // Get parent string.
                  if ( ScanningString == null )
                    ScanningIFile= null;  // No more parents.
                    else
                    ScanningIFile = new IFile( ScanningString );  // Convert to IFile.
                  } // Add next IFile to PathArrayList.
              Collections.reverse(PathArrayList);  // reverse the List order so root is first.
              ResultTreePath= // Convert array of nodes to TreePath
                new TreePath(PathArrayList.toArray());
              } // translate to TreePath.
          return ResultTreePath;
          } // IFileToTreePath(IFile InNodeIFile)
*/

/*    public static IFile xTreePathToIFile(TreePath InTreePath)  // ???
        /* Translates TreePath InTreePath to a IFile.  ??? */
/*      { // TreePathToIFile(TreePath InTreePath)
          IFile ResultIFile = null;  // initialize result to null.
          if (InTreePath != null)  // translate TreePath if present.
            { // translate.
              ResultIFile = // calculate new selection by...
                (IFile)  // ...converting to IFile the result of...
                InTreePath.  // ...the input TreePath and...
                getLastPathComponent();  //...eliminating all but the last IFile element.
              } // translate.
          return ResultIFile;
          } // TreePathToIFile(TreePath InTreePath)
*/

      public static void NoOp( )
        /* This allows setting breakpoints in other code.  */
        { }

      private static int DbgCountI= 0;
      public static void DbgOut( String InString )
        /* This outputs to the console a new line containing a counter, 
          which is incremented, followed by InString.
          */
        { 
          System.out.println( );
          System.out.print( 
            "DbgOut() " + 
            DbgCountI++ + 
            ": " +
            InString 
            );

          }

    } // class Misc 
