package allClasses;

//import java.util.logging.Logger;

public class Globals 
  /* This class is used for things the app will need often.
    It will be the argument for "import static" to reduce
    the need for fully qualified names, as in:
      import static allClasses.Globals.*;  // appLogger;
    */
  {
		//public static Logger appLogger= Logger.getAnonymousLogger();
    public static Misc appLogger= Misc.getMisc();  // Emulate Logger subset.
    }
