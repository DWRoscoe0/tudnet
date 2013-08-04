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

      /* Use this line to verify that code is being executed:
          System.out.print( "!" );  // Debug.
        */
      
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
  
      public static void NoOp( )
        /* This allows setting breakpoints in other code.  */
        { }

      private static int DbgCountI= 0;
      
      public static void xDbgOut( ) 
        { 
          DbgOut( "-" ); 
          }
      
      public static void DbgEventDone()
        /* This is a convenient place to catch 
          after a user input event is processed or 
          after any other signficant event occurs.
          This is useful because the Java event loop is not accessible.
          */
        { 
          //DbgOut( "-" ); 
          //MetaFile.writeDebugState( );  // ??? Display for debugging.
          //NoOp( );
          }
      
      public static void DbgConversionDone()
        /* This is called after a conversion from IDNumber to MetaNode.  */
        { 
          //DbgOut( "Conv." ); 
          //MetaFile.writeDebugState( );  // ??? Display for debugging.
          NoOp( );
          }
      
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

          NoOp( );
          }

    } // class Misc 
