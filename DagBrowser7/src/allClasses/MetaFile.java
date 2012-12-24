package allClasses;

public class MetaFile 
  /* This class manages the file(s) that contains the external 
    representation of the MetaNode-s rooted at MetaRoot.  
    It reads from the file(s) after the application starts,
    and it writes to the file(s) before the application terminates.
	*/
  { // class MetaFile.
  
    public static void start()
	  /* Starts reading [and writing] MetaNode-s from external file(s).
	    This should be called after application startup.  */
	  { }
  
    public static void finish()
	  /* Finishes writing all new or modified MetaNode-s  
	    to external file(s).  
	    This should be called before application termination.  */
	  { }

    } // class MetaFile.
