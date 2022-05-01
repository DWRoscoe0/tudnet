package allClasses.ifile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.DataTreeModel;
import allClasses.Persistent;
import allClasses.TitledTextViewer;
import allClasses.javafx.EpiTreeItem;
import allClasses.javafx.Selections;
import allClasses.javafx.TitledTextNode;
import allClasses.javafx.TreeStuff;
import javafx.scene.Node;

import static allClasses.SystemSettings.NL;

public class IFile 

  extends INamedList

  /* This class extends INamedList to represent a regular file.
   * 
   * ///org
   * It never actually uses the list capabilities of INamedList.
   * This is wasteful of resources.  Fix?
   * Some of the following code could probably be deleted.
   * Some ancestor class rearrangement might be necessary.
   */

  { // class IFile
  
    // Variables.
    
    // Constructors.

      public IFile(File theFile) 
        { 
          super(theFile);
          }

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf()
        {
          return true;  // Because regular files are leaves.
          }
    
      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of childObject in directory ParentObject.
          This method must exist and override the version in class DataNode
          as part of this classes lazy evaluation feature.

          Instead of searching for a child object that is equal to childObject,
          this method searches for a child with a matching name.
          It does this by searching for a child with a matching name.
          This works regardless of whether children 
          have been converted to IFiles.
          */
        {
          DataNode childDataNode= (DataNode)childObject;
          String childString= childDataNode.toString(); // Get name of target.

          int resultI = -1;  // Initialize result for not found.
          for // Search for child with matching name. 
            ( int i = 0; i < childMultiLinkOfDataNodes.getCountI(); ++i) 
            {
              if 
                ( childString.equals( 
                    childMultiLinkOfDataNodes.getE(i).getNameString()))
                {
                  resultI = i;  // Set result to index of found child.
                  break;
                  }
              }

          return resultI;
          }

    // interface DataNode methods.

      public String getFileString() // Was previously named getContentString().
        /* This method produces a String containing the contents of a file.
         * It is used by TitledTextViewer to display a file.
          */
        {
          StringBuilder theStringBuilder= new StringBuilder();
          { // Read in file to JTextArea.
            String lineString;  // temporary storage for file lines.
            try {
              FileInputStream theFileInputStream= 
                new FileInputStream(getFile());
              BufferedReader theBufferedReader= 
                new BufferedReader(new InputStreamReader(theFileInputStream));
              
              while ((lineString = theBufferedReader.readLine()) != null) {
                  theStringBuilder.append(lineString + NL);
                  }
              }
            catch (Exception ReaderException){
              // System.out.println("error reading file! " + ReaderException);
              theStringBuilder.append(
                  NL + "Error reading file!" + NL + NL + ReaderException + NL
                  );
              }
            } // Read in file to JTextArea.
          return theStringBuilder.toString();
          }
      
      public JComponent getDataJComponent(
          TreePath theTreePath,
          DataTreeModel inDataTreeModel 
          )
        /* Returns a JComponent capable of displaying the 
         * IFile at the end of theTreePath. 
         */
        {
          JComponent resultJComponent= // Return a text viewer to view the file.
                new TitledTextViewer(theTreePath, inDataTreeModel, getFileString());
          return resultJComponent;  // return the final result.
          }

      public Node getJavaFXNode( 
          TreePath theTreePath, 
          DataNode subjectDataNode, 
          DataTreeModel inDataTreeModel,
          Persistent thePersistent,
          DataRoot theDataRoot,
          EpiTreeItem theRootEpiTreeItem,
          Selections theSelections
          ) 
        /* Returns a Node Component capable of displaying this IFile.
          */
        {
          Node resultNode= // Using TitledListViewer.
            new TitledTextNode(
                subjectDataNode,
                getFileString(),
                TreeStuff.makeTreeStuff(
                    subjectDataNode,
                    null,
                    thePersistent,
                    theDataRoot,
                    theRootEpiTreeItem,
                    theSelections
                    )
                );
  
          return resultNode;  // Returning result from above.
          }


    public String getContentString()
      /* Returns the content of the DataNode as a String.  
        This is potentially a long String, such as the content of a file, 
        and it might consist of multiple lines.
        This method will be overridden.
        */
      {
        return getFileString();
        }

    // other methods.

    } // class IFile
