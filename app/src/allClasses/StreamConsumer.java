package allClasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.Reader;

class StreamConsumer extends Thread
  /* This class is a Thread which copies an InputStream to stdout.
   It was used on Processes created by ProcessBuilder to: 
   * Absorb stdout and stderr output from the created Process
     to prevent them from hanging.
   * Echo the output from the creating process to see whehther
     they were having problems.
   This class might no longer be used now that
   output is being logged to a file by methods in the Misc class.
   */
  {
    InputStream is;
    String NameString;
    
    // reads everything from is until empty. 
    StreamConsumer(InputStream is, String NameString) {
        this.is = is;
        this.NameString= NameString;
    }
  
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                System.out.println(NameString+":"+line);    
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
  }


