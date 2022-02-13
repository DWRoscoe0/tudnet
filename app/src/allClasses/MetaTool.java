package allClasses;

// import java.util.List;
// import java.util.Stack;

import javax.swing.tree.TreePath;
 
abstract class MetaTool
      
  { // class MetaTool.

    /* This class was created to make operations on 
      DataNodes and their associated MetaNodes easier.

      It maintains two paths:
      * A TreePath which represents a path to a DataNode 
        in the DataNode DAG.
      * A MetaPath which represents a path to 
        the associated MetaNode in the MetaNode DAG.
  
      The MetaNode DAG stores data about the DataNode DAG.
      The paths are maintained in sync so that 
      the two paths always reference equivalent locations 
      in the different but related DAGs.  When one path changes, 
      the other is changed to an equivalent value.
  
      ///opt  Possible enhancements:
  
        Optimize MetaNode space:
          Eliminate nodes which:
            * have no children,
            * have no attributes, and
            * are not the most recently referenced or 
              are not the top/default node.
              
        Instead of starting the constructor Sync() operation
        at the roots of the 2 DAGs, 
        start it from the target of the previous sync, 
        because operations will often be
        concentrated in areas far from the roots.
      */
  
    // Injected dependency variables.

      MetaRoot theMetaRoot;
  
    // Other instance variables.
    
      private TreePath theTreePath;  /* The path of DataNodes from 
        the DataNode DAG root parent to 
        the DataNode at the location of interest.  */
      private MetaPath theMetaPath;  /* The path of MetaNodes from 
        the MetaNode DAG root parent to 
        the MetaNode that holds the meta-data for
        the DataNode at the location of interest.  */

     // Constructors.
      public MetaTool( MetaRoot theMetaRoot, TreePath inTreePath )
        /* This constructor builds a MetaTool which is set-up to access 
          the MetaNode for the DAG location associated with inTreePath.
          It does this by initializing both path instance variables
          to point to the roots of their respective DAGs,
          and then syncing them to inTreePath.
          */
        {
          this.theMetaRoot= theMetaRoot;

          theTreePath=  // Initializing DataNode TreePath.
            theMetaRoot.getTheDataRoot().getParentOfRootTreePath();
          theMetaPath=  // Initializing MetaNode TreePath.
            theMetaRoot.getParentOfRootMetaPath( );
          
          syncV( inTreePath ); // Adjusting the instance variables so...
            // ...the locations they represent match...
            // ...the MetaNode associated with inTreePath.

          }

    // Instance methods.
      
      protected void syncV( TreePath inTreePath )
        /* This recursive method adjusts the 2 tree path variables 
          so that the locations they represent match inTreePath.
          It does this by comparing inTreePath with instance variable
          theTreePath and if necessary adjusting 
          theTreePath and theMetaPath to match inTreePath.
          It tries to do this incrementally and recursively, 
          so if inTreePath and theTreePath are very similar,
          then syncing will be very fast.
          */
        { 
        	int inPathLengthI= inTreePath.getPathCount();
        	int thePathLengthI= theTreePath.getPathCount();
        	int lengthDifferenceI= inPathLengthI - thePathLengthI;

          if ( lengthDifferenceI > 0 )
          	syncToLongerLengthNewPathV( inTreePath, lengthDifferenceI );
          else if ( lengthDifferenceI < 0 ) 
          	syncToShorterLengthNewPathV( inTreePath, -lengthDifferenceI );
          else // if ( lengthDifferenceI == 0 )
            syncToSameLengthNewPathV( inTreePath );
          }

      private void syncToLongerLengthNewPathV(
		  		TreePath inTreePath, int lengthDifferenceI 
		  		)
	      /* This method handles the Sync case when
		      the new path length is longer than or equal to the old path length.
		      lengthDifferenceI is how much longer inTreePath is than
		      theTreePath in this MetaTool.
		      */
	      {
	    	  if ( lengthDifferenceI == 0 ) // Syncing based on length difference.
	    	  	syncToSameLengthNewPathV( inTreePath ); // Syncing same length.
	    	  	else
	    	  	{ // Syncing to longer new path.
	    	  		syncToLongerLengthNewPathV( // Syncing paths one child shorter.
	    	  				inTreePath.getParentPath(), lengthDifferenceI - 1 
	  		          );
  		        appendMetaChildFromTreePathV( inTreePath ); // Appending child.
		          theTreePath= inTreePath;  // Extending TreePath to match.
  	    	  	}
	        }
	
      private void syncToShorterLengthNewPathV( 
      		TreePath inTreePath, int lengthDifferenceI 
      		)
	      /* This method handles the Sync case when
	        the new path length is shorter than or equal to the old path length.
	        lengthDifferenceI is how much shorter inTreePath is than
	        theTreePath in this MetaTool.
	        */
	      {
      	  if ( lengthDifferenceI == 0 ) // Syncing based on length difference.
      	  	syncToSameLengthNewPathV( inTreePath ); // Syncing same length.
      	  	else
      	  	{ // Syncing shorter new path.
			        theTreePath=  // Shorten old path by removing last DataNode.
			          theTreePath.getParentPath();
			        theMetaPath=  // Shorten MetaPath by removing last MetaNode.
			          theMetaPath.getParentMetaPath( );
			        syncToShorterLengthNewPathV( inTreePath, lengthDifferenceI - 1 );
      	  		}
	        }

      private void syncToSameLengthNewPathV( TreePath inTreePath )
        /* This method handles the Sync case when
          the new path is the same length as the old path,
          but they are (not necessarily) known to be unequal.
          */
        {
	        if ( equalsB( inTreePath, theTreePath ) ) // Returning if equal.
	        	; // Nothing to do.  Sync achieved.
        	else // Syncing by recursing on parent and appending children.
	        	{
		          theTreePath= // Remove old TreePath child.
		          		theTreePath.getParentPath();
		          theMetaPath=  // Remove MetaPath child.
		          		theMetaPath.getParentMetaPath( ); 
		          TreePath parentOfInTreePath= // Remove new TreePath child. 
		          		inTreePath.getParentPath();
		
            	syncToSameLengthNewPathV( parentOfInTreePath ); // Sync parents.
		
		          appendMetaChildFromTreePathV( inTreePath ); // Append child.
		          theTreePath= inTreePath;  // Extending TreePath to match.
	        		}
          }

      private void appendMetaChildFromTreePathV( TreePath inTreePath )
        /* This is a helper method for 
          some of the above Sync... methods.  
          It adds an element to theMetaPath,
          extending the MetaNode DAG if needed,
          to match the DataNode DAG path inTreePath.
          */
        {
          DataNode theDataNode= // Getting DataNode from end of TreePath.
            (DataNode)inTreePath.getLastPathComponent( );
          MetaNode childMetaNode= // Putting it in MetaNode as child.
            theMetaPath.
            getLastMetaNode().
            addV( theDataNode );
          theMetaPath=  // Add resulting child MetaNode to path by...
            new MetaPath(  // ...constructing new MataPath from...
              theMetaPath,  // ...old MetaPath...
              childMetaNode  // ...and childMetaNode as new element.
              );
          }

      private boolean equalsB( TreePath aTreePath, TreePath otherTreePath )
        /* Special version of equals(..) customized for this app.  
          It doesn't use getPathCount().
          It works best for TreePaths which have 
          frequent common prefixes but rare common suffices. 
          */
	      {
      	  boolean resultB= false; // Assuming paths not equal.
	        while (true) { // Looping, testing, and shortening paths.
	        	if // Exiting with true if path references are equal.
	        	  ( aTreePath == otherTreePath )
         			{ resultB= true; break; }
	        	{ // Exiting with false if only one reference is null.
		        	if ( aTreePath == null ) break;
		        	if ( otherTreePath == null ) break;
	        		}
	        	if // Exiting with false if children are unequal.
	        	  ( ! aTreePath.getLastPathComponent().equals( 
	        	  		otherTreePath.getLastPathComponent() 
	        	  		) )
	        		break;
	        	aTreePath= aTreePath.getParentPath(); // Removing child.
	        	otherTreePath= otherTreePath.getParentPath(); // Removing child.
	        	}
      	  return resultB;
	        }
  
    // Instance getter methods.

      protected MetaPath getMetaPath()
        /* Returns the MetaPath associated with this tool.  */
        { return theMetaPath; }

      protected MetaNode getMetaNode()
        /* Returns the MetaNode associated with this tool.  */
        { return theMetaPath.getLastMetaNode(); }

    } // class MetaTool.
