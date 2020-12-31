package allClasses.ifile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;
import allClasses.TitledTextViewer;
import allClasses.javafx.TitledTextNode;
import allClasses.javafx.TreeStuff;
import javafx.scene.Node;

import static allClasses.SystemSettings.NL;

public class IFile 

  extends INamedList

  /* This class represents regular files.
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
          It does this by searching for a child with a matching name.
          This works regardless of how many the children
          have been converted to IFiles.
          This avoids calculating a lot of IFiles unnecessarily.
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
          DataTreeModel inDataTreeModel 
          ) 
        /* Returns a Node Component capable of displaying this IFile.
          */
        {
          Node resultNode= // Using TitledListViewer.
            new TitledTextNode(
                subjectDataNode,
                //// inDataTreeModel, 
                getFileString(),
                //// new TreeStuff(
                TreeStuff.makeWithAutoCompleteTreeStuff(
                    subjectDataNode,
                    null
                    )
                );
  
          return resultNode;  // Returning result from above.
          }

          
    // other methods.

    } // class IFile
