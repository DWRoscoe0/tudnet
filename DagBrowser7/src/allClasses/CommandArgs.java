package allClasses; // original package was com.jenkov.cliargs;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.TreeSet;


public class CommandArgs {

  /* This class is based on the one by the same name 
    created by Jakob Jenkov, published at
    https://github.com/jjenkov/cli-args and described at
    http://tutorials.jenkov.com/java-howto/java-command-line-argument-parser.html

    Arguments consist of
    * switches, which are Strings that begin with "-" 
      optionally followed by a switch value which can be either
      * nothing
      * boolean
      * long
      * double
      * String array
    * targets, which are strings remaining after all switches are processed.

    Processing of arguments normally goes proceeds as follows:
    * Construct an instance of this object or 
      call parse(argStrings) on an existing instance.
    * Process all the switches and their values by 
      * testing for their presence using 
        one more calls to switchPresent(), and-or
      * getting the values of switches using one more calls to:
        * switchValue(), which returns a String
        * switchLongValue()
        * switchDoubleValue()
        * switchValues(), which returns a String array
        * switchPojo(), which returns a pojo whose fields match the switches
    * Call targets() to return all remaining arguments, 
      arguments that are targets because they
      were not processed earlier as switches or switch values. 


    ///enh Don't take switch indexes until the switch is actually processed.
      This will allow targets() to return any arguments, switch or otherwise,
      which was not processed.  Note this as a significant change.

    ///enh Presently only the presence of switches is significant.
      Their order is not.  It might be worthwhile to provides 
      a way to process arguments sequentially,
      a way in which the switches are significant.

   */

  private String[] args = null;

  private HashMap<String, Integer> switchIndexes= 
      new HashMap<String, Integer>(); // Found switches and their positions.
  private TreeSet<Integer> takenIndexes= 
      new TreeSet<Integer>(); /* Positions of switches that have been gotten.
        Originally switch indexes were at parse() time.
        Now they are added only when particular switches are asked for.  */

  
  public CommandArgs(String[] args){ // Constructor.
      parse(args);
  }

  public void parse(String[] arguments)
    /* This method parses the arguments list, defining the values of
      switchIndexes and takenIndexes appropriately.
      After this is done, the interrogation methods which follow
      may be called.  
      */
    {
      this.args = arguments;
      //locate switches.
      switchIndexes.clear();
      takenIndexes.clear();
      for(int i=0; i < args.length; i++) {
          if(args[i].startsWith("-") ){
            /// appLogger.debug( "CommandArgs.parse(..) switch "+args[i] );
            switchIndexes.put(args[i], i);
            // takenIndexes.add(i);  Stopped doing this.
          } else {
            /// appLogger.debug( "CommandArgs.parse(..) target "+args[i] );
          }
      }
  }

  
  // Interrogation methods follow.
  
  public String[] args() 
    // Returns argument String array associated with this object.
    { return args; }

  public String arg(int indexI)
    // Returns the argument String whose index is indexI.
    { return args[indexI]; }

  public boolean switchPresent(String switchName)
    /* Returns true if the switch switchName was parsed, false otherwise.
      Also records the switch argument as taken.
      */
    { 
      boolean isPresentB= switchIndexes.containsKey(switchName);
      if (isPresentB) 
        takenIndexes.add(switchIndexes.get(switchName));
      return isPresentB;
      }

  public String switchValue(String switchName) 
    /* Returns the String value of switchName.
      If that switch did not appear in the parse arguments
      then null is returned.
      */
    { return switchValue(switchName, null); }

  public String switchValue(String switchName, String defaultValue) 
    /* Returns the String value associated with switchName, which is 
      the String argument that followed the switch in the argument list.
      If that switch did not appear in the parsed arguments or it had no value
      then defaultValue is returned.
      */
    {
      if(!switchPresent(switchName)) // Take switch if present. 
        return defaultValue; // Return default value if not.

      int switchIndex = switchIndexes.get(switchName);
      if(switchIndex + 1 < args.length){
          takenIndexes.add(switchIndex +1);
          return args[switchIndex +1];
      }
      return defaultValue;
  }

  public Long switchLongValue(String switchName)
    /* Returns the Long value associated with switchName,
      otherwise null. */
    { return switchLongValue(switchName, null); }

  public Long switchLongValue(String switchName, Long defaultValue) 
    /* Returns the Long value associated with switchName, 
      otherwise defaultValue.  */
    { 
      String switchValue = switchValue(switchName, null); 

      if(switchValue == null) return defaultValue;
      return Long.parseLong(switchValue);
      }

  public Double switchDoubleValue(String switchName)
    /* Returns the Double value associated with switchName,
      otherwise null. */
    {
      return switchDoubleValue(switchName, null);
      }

  public Double switchDoubleValue(String switchName, Double defaultValue) 
    /* Returns the Double value associated with switchName, 
      otherwise defaultValue. */
    {
      String switchValue = switchValue(switchName, null);

      if(switchValue == null) return defaultValue;
      return Double.parseDouble(switchValue);
      }


  public String[] switchValues(String switchName)
    /* Returns a String array containing all the String argument values 
      which follow the switch whose name is switchName.
      If there are none then it returns an empty String array.
      */
    {
      if // Return an empty array if the switch did not appear at all.
        (!switchIndexes.containsKey(switchName)) return new String[0];

      int switchIndex= switchIndexes.get(switchName); // Get index of switch.

      int nextArgIndex = switchIndex + 1; // Get index of first switch value.
      while // Take all the switch's arguments.
        (nextArgIndex < args.length && !args[nextArgIndex].startsWith("-"))
        {
          takenIndexes.add(nextArgIndex); // Take the argument.
          nextArgIndex++; // Advance argument index.
          }

      String[] values= // Allocate a sufficiently large argument result array.
          new String[nextArgIndex - switchIndex - 1];
      for(int j=0; j < values.length; j++){ // Copy arguments to result array.
          values[j] = args[switchIndex + j + 1];
      }
      return values;
  }

  public <T> T switchPojo(Class<T> pojoClass)
    /* This method returns an object of type T.
      First it creates a new object of type T with its default values.
      If there are any switches which match fields in the T object,
      then it replaces the field's default value by the switch's value. 
      When doing the matching, T field names have 
      '_' replaced with '-' and have a '-' prepended,
      because Java field names can not contain "-".
      Switches can be gotten using one or more calls to this method 
      instead of or in addition to methods which get individual switches.
      */
    {
      try {
        T pojo= pojoClass.newInstance(); // Create T pojo with default values.

        Field[] fields= pojoClass.getFields(); // Get pojo fields.
        for(Field field : fields) { // Iterate over all pojo fields.
          Class<?> fieldType= field.getType(); /// I added <?> after Class.
          String fieldName= // Calculate switch associated with field. 
              "-" + field.getName().replace('_', '-');

          // Decode individual field into appropriate switch and copy value.
          if(fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)){
              field.set(pojo, switchPresent(fieldName) );
          } else if(fieldType.equals(String.class)){
              if(switchValue(fieldName) != null){
                  field.set(pojo, switchValue(fieldName ) );
              }
          } else if(fieldType.equals(Long.class) || fieldType.equals(long.class) ){
              if(switchLongValue(fieldName) != null){
                  field.set(pojo, switchLongValue(fieldName) );
              }
          } else if(fieldType.equals(Integer.class) || fieldType.equals(int.class) ){
              if(switchLongValue(fieldName) != null){
                  field.set(pojo, switchLongValue(fieldName).intValue() );
              }
          } else if(fieldType.equals(Short.class) || fieldType.equals(short.class) ){
              if(switchLongValue(fieldName) != null){
                  field.set(pojo, switchLongValue(fieldName).shortValue() );
              }
          } else if(fieldType.equals(Byte.class) || fieldType.equals(byte.class) ){
              if(switchLongValue(fieldName) != null){
                  field.set(pojo, switchLongValue(fieldName).byteValue() );
              }
          } else if(fieldType.equals(Double.class) || fieldType.equals(double.class)) {
              if(switchDoubleValue(fieldName) != null){
                  field.set(pojo, switchDoubleValue(fieldName) );
              }
          } else if(fieldType.equals(Float.class) || fieldType.equals(float.class)) {
              if(switchDoubleValue(fieldName) != null){
                  field.set(pojo, switchDoubleValue(fieldName).floatValue() );
              }
          } else if(fieldType.equals(String[].class)){
              String[] values = switchValues(fieldName);
              if(values.length != 0){
                  field.set(pojo, values);
              }
          }
        }

        return pojo;
    } catch (Exception e) {
        throw new RuntimeException("Error creating switch POJO", e);
    }
  }

  public String[] targets()
    /* This method returns an array of Strings containing
      all the arguments that have not been taken, as either
      * a switch which begins with a "-", or
      * a switch value, one or more of which follows a switch.
     */
    {
      String[] targetStrings= new String[args.length - takenIndexes.size()];
      int targetIndex = 0;
      for(int i = 0; i < args.length ; i++) {
          if( !takenIndexes.contains(i) ) {
              targetStrings[targetIndex++] = args[i];
          }
      }

      return targetStrings;
    }

  }
