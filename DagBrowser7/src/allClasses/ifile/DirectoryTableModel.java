package allClasses.ifile;

import java.io.*;
import java.sql.Date;
import java.text.SimpleDateFormat;

import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;

public class DirectoryTableModel

  extends AbstractTableModel 
  
  /* This class is the data model for directory tables.
    It contains no selection information within the directory it models.
    
    ?? It will need the ability to Listen to the TreeModel for changes,
    respond to only the relevant changes,
    and fire Event-s to its own Listener-s.
    Wrap Listener-s in weak-references to prevent memory leaks.
    */

  { // class DirectoryTableModel

    // variables.
      private static final long serialVersionUID = 1L;

      protected IFile directoryIFile;  // The directory modeled as a table.
      protected TreeModel theTreeModel;  // TreeModel which provides context.
      
      protected Object directoryIconObject;  ///enh ?? unused.  reactivate?
      protected Object fileIconObject;  ///enh ?? unused.  reactivate?

      private SimpleDateFormat aSimpleDateFormat=  // For time-stamps.
        new SimpleDateFormat(); 
      
    // constructor methods.
    
      public DirectoryTableModel( IFile inIFile, TreeModel inTreeModel ) 
        /* The constructs an instance for displaying directory inIFile
          using inTreeModel to convert child directory entries
          as rows in the table.
          */
        {
          { // Load icons.
            directoryIconObject = 
              UIManager.get( "DirectoryPane.directoryIcon" );
            fileIconObject = UIManager.get( "DirectoryPane.FileIconObject" );
            } // Load icons.

          theTreeModel= inTreeModel;  // Save TreeModel.
          setDirectory( inIFile );  // Save directory to be displayed.
          }

    // input methods (there was only one.)

      public void setDirectory( IFile inIFile ) 
        /* Sets the directory whose contents will be displayed as a table.  */
        {
          directoryIFile = inIFile;  // save directory to be modeled.
          fireTableDataChanged();  // inform listeners about possible changes.
          }

    // output methods.

      public int getRowCount() 
        /* Returns the number of child directory entries as
          the number of rows in the table. 
          */
        {
          return theTreeModel.getChildCount( directoryIFile );
          }

      public int getColumnCount() 
        {
          return 4; // There are presently 4 columns in the table.
          }

      public Object getValueAt(int rowI, int columnI)
        /* Returns the table entry at row rowI and column ComumnI.
          If the value is undefined for some reason,
          for example, an unreadable directory,
          then it returns null.
          */
        { 
          IFile rowIFile= // Get the IFile associated with row rowI.
            ((IFile)theTreeModel.getChild( directoryIFile, rowI ));
          File rowFile= rowIFile.getFile(); // Get the File also.
          Object resultObject;  // Place for result.
          switch   // Return the appropriate value.
            ( columnI )  // based on the desired column whose number is columnI.
            {
              case 0:  // file type.
                  resultObject= rowFile.isDirectory()  // Is a directory.
                    ? "d"   // true: d for directories.
                    : "f";  // false: f for non-directory files.
                  break;
              case 1:  // file name.
                  resultObject= rowFile.getName();  // Name of file.
                  break;
              case 2:  // file length.
                  if ( rowFile.isDirectory() )  // Is a directories.
                      resultObject= "--";  // Use dashes.
                  else  // actual length for files.
                      resultObject= new Long( rowFile.length() );
                  break;
              case 3:  // modified date and time.
                  Date ModifiedDate= new Date(rowFile.lastModified());
                  String ModifiedString= 
                    aSimpleDateFormat.format( ModifiedDate );
                  resultObject= ModifiedString;
                  break;
              default:  // anything else, indicate undefined value.
                  resultObject= "UNDEFINED-CELL-VALUE";
              }
          return resultObject;  // Return calculated result.
          }

      public String getColumnName( int columnI ) 
        /* Returns a string representing the name of the column 
          whose number is columnI.
          */
        {
          switch ( columnI ) {
            case 0:
                return "Type";
            case 1:
                return "Name";
            case 2:
                return "Bytes";
            case 3:
                return "Modified";
            default:
                return "UNDEFINED-COLUMN-NAME";
            }
          }

      public Class<? extends Object> getColumnClass( int columnI ) ///enh reactivate?
        /* Returns the class of the objects in 
          the column whose number is ComumnI.
          I think this is to handle an earlier version when
          column 0 was a graphical icon.
          */
        {
          if ( columnI == 0 ) 
            {
              return getValueAt( 0, columnI).getClass();
              }
            else 
            {
              return super.getColumnClass( columnI );
              }
          }

  } // class DirectoryTableModel
