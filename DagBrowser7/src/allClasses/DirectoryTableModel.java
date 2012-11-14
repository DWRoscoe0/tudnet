package allClasses;

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
    
    ??? It will need the ability to Listen to the TreeModel for changes,
    respond to only the relevant changes,
    and fire Event-s to its own Listener-s.
    Wrap Listener-s in weak-references to prevent memory leaks.
    */

  { // class DirectoryTableModel

    // variables.
      private static final long serialVersionUID = 1L;

      protected IFile DirectoryIFile;  // The directory modeled as a table.
      protected TreeModel TheTreeModel;  // TreeModel which provides context.
      
      protected Object DirectoryIconObject;  // ?? unused.
      protected Object FileIconObject;  // ?? unused.

      private SimpleDateFormat ASimpleDateFormat=  // For displaying time-stamps.
        new SimpleDateFormat(); 
      
    // constructor methods.
    
      /* public DirectoryTableModel( int ForceErrorI ) 
        {
          this( null );
          }
        */
    
      public DirectoryTableModel
        ( IFile InputDirectoryIFile, 
          TreeModel InTreeModel
          // , int ForceErrorI  
          ) 
        {
          DirectoryIconObject = UIManager.get( "DirectoryPane.directoryIcon" );
          FileIconObject = UIManager.get( "DirectoryPane.FileIconObject" );
          TheTreeModel= InTreeModel;
          setDirectory( InputDirectoryIFile );  // this will notify Listeners.
          }


    // input methods (there was only one.)

      public void setDirectory( IFile InputDirectoryIFile ) 
        /* Sets the directory whose contents will be displayed as a table.  */
        {
          DirectoryIFile = InputDirectoryIFile;  // save directory to be modeled.
          fireTableDataChanged();  // inform listeners about possible changes.
          }

    // output methods.

      public int getRowCount() 
        {
          return TheTreeModel.getChildCount( DirectoryIFile );
          }

      public int getColumnCount() 
        {
          //return SortedNamesStrings != null ? 3 :0;
          //return 3; // There are always 3 columns.
          return 4; // There are 4 columns with date column.
          }

      public Object getValueAt(int RowI, int ColumnI)
        /* returns the value at row RowI and column ComumnI.
          if the value is undefined for some reason,
          for example, an unreadable directory,
          then it returns null.
          */
        { 
/*        if ( DirectoryIFile == null || SortedNamesStrings == null ) 
            return null;

          String ChildNamesString= SortedNamesStrings[RowI]; // extract child name.
          if ( ChildNamesString == null ) // if it is null then return null.
            return null;

          File RowFile = new File(  // calculate File name of the entry at selected row from...
            DirectoryIFile, // ...the present directory name and...
            SortedNamesStrings[RowI] ); // ...the name String of the file entry.
*/          
          IFile RowIFile= // get the IFile associated with row RowI.
            ((IFile)TheTreeModel.getChild( DirectoryIFile, RowI ));
          File RowFile= RowIFile.GetFile(); // and get the File also.
          //DagNode RowDagNode= (DagNode)RowIFile;  // ???
          switch   // return the apprropriate value.
            ( ColumnI )  // based on the desired column whose number is ColumnI.
            {
              case 0:  // file type.
                  return RowFile.isDirectory()  // return based on directory status.
                    ? "d"   // d for directories.
                    : "f";  // f for non-directory files.
              case 1:  // file name.
                  return RowFile.getName();
                  //return RowIFile.GetNameString();
                  //return RowDagNode.GetNameString();
                  //return "DirectoryTableModel:test";
              case 2:  // file lenfth.
                  if ( RowFile.isDirectory() )  // dashes for directories.
                      return "--";
                  else  // actual length for files.
                      return new Long( RowFile.length() );
              case 3:  // modified date and time.
                  Date ModifiedDate= new Date(RowFile.lastModified());
                  //String ModifiedString= ModifiedDate.toString();
                  String ModifiedString= ASimpleDateFormat.format( ModifiedDate );
                  return ModifiedString;
              default:  // anything else, return empty string.
                  return "";
              }
          }

      public String getColumnName( int ColumnI ) 
        /* returns a string representing the name of the column 
          whose number is ComumnI.
          */
        {
          switch ( ColumnI ) {
            case 0:
                return "Type";
            case 1:
                return "Name";
            case 2:
                return "Bytes";
            case 3:
                return "Modified";
            default:
                return "unknown";
            }
          }

      public Class<? extends Object> getColumnClass( int ColumnI ) 
        /* returns the class of the objects in 
          the column whose number is ComumnI.
          I think this is to handle an earlier version when
          column 0 was a graphical icon.
          */
        {
          if ( ColumnI == 0 ) 
            {
              return getValueAt( 0, ColumnI).getClass();
              }
            else 
            {
              return super.getColumnClass( ColumnI );
              }
          }

  } // class DirectoryTableModel
