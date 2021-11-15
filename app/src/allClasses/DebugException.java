package allClasses;

public class DebugException extends Exception 
  { 
  
    /* This exception is used during debugging to suspend a thread,
     * allowing the developer to examine the stack.
     * It is meant to be thrown when an anomaly is detected.
     * The Eclipse IDE would normally have a breakpoint set for this exception.
     * 
     * What a DebugException breakpoint does can be done
     * with one or more ordinary instruction breakpoints,
     * having a dedicated exception makes things easier.
     */
      
    }
