/*
 Copyright (c) 1997 by Columbia University. All rights reserved.
 */

import java.lang.*;
import java.io.*;

/** 
 * TracertWrapper is a wrapper around the common traceroute program
 * that exists on many operating systems. In Windows, the program is
 * called tracert. Surprisingly the Unix program name is longer. It is
 * traceroute.
 * @version 1.0 31 Aug 1998
 * @author Terrence Truta
 */
public class TracertWrapper extends java.lang.Object
{
  
    String executable = "tracert";
    String address = "cunix.columbia.edu";

		ChartData chartdata;

		StartGUI gui;
    int progress = 1;
		boolean debug = false;
		public static final int MAX_HOPS = 30;
    
		/**
		 * Sets the address to perform the traceroute to.
		 */
		public void setAddress(String addr) {
			address = addr;
		}

		/**
		 * Sets the gui object. It is used to update the progress bar.
		 */
		public void setGUI(StartGUI gui) {
			this.gui = gui;
		}

		/**
		 * Sets the amount that the progress bar will be updated after each
		 * line of traceroute output.
		 */
		public void setProgressIncr(int progress) {
			this.progress = progress;
		}

		/**
		 *Returns the data that was collected running traceroute.
		 *@see ChartData
		 */
		public ChartData getChartData() {
			return chartdata;
		}

		
		/** Test driver for this class.
		 */
    public static void main (String args[]) {
    try {
        TracertWrapper tw = new TracertWrapper();
        if (args.length > 0) 
            tw.setAddress(args[0]);
	      
				tw.execute();
    
      } catch (Exception e) {
        System.out.println(e);
        System.exit(1);
      }
    }
    
		
    TracertWrapper() {
    }
    
    /**
		 * Builds the command line string to execute traceroute depending
		 * on the operating system. After execution it creates a BufferedReader
		 * object and calls the appropriate method to parse the output depending
		 * on the OS.
		 */
		public void execute() {
      BufferedReader in = null;
      chartdata = new ChartData();
      try {
				if (System.getProperty("os.name").compareTo("Windows 95") == 0)
						executable = "tracert";
				else 
						executable = "traceroute";
				
				String	cmd = executable + " " + address;

        
				debugPrint(cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        
        in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				if (System.getProperty("os.name").indexOf("Windows") != -1)
						parseWin95Output(in);
				else {
						debugPrint("OS is: " + System.getProperty("os.name"));
						parseUnixOutput(in);
				}
					

      }
      catch (Exception e) {
            System.out.println("Error running tracert");
            System.out.println(e);
            System.exit(1);
      }
    }

	  /** wrapper around the BufferedReader readLine method. It is used
		  * to store each actual unparsed line of the traceroute output
			* so we can display it to the user later.
			*/
	  private String read(BufferedReader in) throws IOException {
		  
			String s = in.readLine();
			chartdata.trace_out.addElement(s);
			return s;
		}

		/**
		 * Parses the output of the tracert program in Windows 95. It will
		 * probably work with Windows NT but it hasn't been tested in that
		 * environment.
		 */
		public void parseWin95Output(BufferedReader in) {
			try {
			String s;
			//skip over initial lines of output
				s = read(in);
				while (s.indexOf("1  ") == -1) { 
					s = read(in);
				} 
				
				//parse Traceroute output
				char[] cha;;
				char[] temp = new char[s.length() * 2]; //estimate a safe length for temp
				int i,j,num;
				debugPrint("s = " + s);
				int hop = 0;
				do {
            //skip over any blank lines in input
						while((s.trim()).compareTo("") == 0)
								s = read(in);
						
						if ((s.trim()).compareTo("Trace complete.") == 0)
							break;
						
						debugPrint("after blank skip, s = " + s);
						
						i = -1;
            num = 3;    //number of round trip time values per hop (non timed out)
            cha = s.toCharArray();
            while(cha[++i] == ' ')	//Ingnore beginning white space before hop #
                ;
            ++i; //in case there are double digits for hop number
                
            for (int l = 0; l < 3; l++) {    //loop 3 time to get 3 time values

              //reinitialize temp to blanks
							for (int a = 0; a < temp.length; a++)
								temp[a] = ' ';	
							
							
							while(cha[++i] == ' ')				  //Ingnore wh space
                  ;
               j = 0;
               do {														
                if (cha[i] == '*') {  //if timeout decr num
                  temp[j] = '0';
                  num--;
                }	
                else 
                  temp[j] = cha[i];   //store ms time value in char array
                j++; i++;
               } while(cha[i] != ' ');
               try {
                    String t = new String(temp);
                    int a = Integer.parseInt(t.trim());
                chartdata.avg[hop] = chartdata.avg[hop] + a;
               } catch (NumberFormatException ne) {
                System.out.println("NumberFormatException in TracertWrapper");
                    System.out.println(ne);
               }
               i += 3;  //incr i to skip over "ms" label.
            }
            if (num != 0) {
              chartdata.avg[hop] = chartdata.avg[hop] / num;   //store the average for at most 3 different
                    			 //round trip time values for this host.
            }
            else {
                chartdata.avg[hop] = 0;
            }
            chartdata.packetLoss[hop] = 3 - num;
						try {
							int c = s.indexOf(" [");
              chartdata.hostName[hop] = (s.substring(i,c)).trim(); //store the name and address for this
              int d = s.indexOf(']');
							chartdata.IPNum[hop] = (s.substring(c+2,d)).trim();	 //host
							debugPrint("IPNum[hop] = " +	chartdata.IPNum[hop]);
							
						} catch (StringIndexOutOfBoundsException stre) {
							//this catches the error that occurs if the request from tracert
							//times out then no host informatio is given
							//In this case just store the tracert info message
							chartdata.hostName[hop] = (s.substring(i)).trim();
							chartdata.IPNum[hop] = "";
						} 
						hop++;
						if (gui != null) gui.incrementProgressBar(progress);	



				} 
				while(((s = read(in)).trim()).compareTo("Trace complete.") != 0); 

				//hop--; //this is how many total hops there were
				chartdata.hops = hop; //store it in chartdata object
				debugPrint("chartdata.hops = " + chartdata.hops +
								   "chartdata.getLength() = " + chartdata.getLength());
        for (i = 0; i <=hop; i++) 
				    debugPrint((i + 1) + "\t" + chartdata.avg[i] + "\t" + chartdata.hostName[i] + 
				                "\tPacket Loss: " + chartdata.packetLoss[i]);
				debugPrint("Finished parsing output");
			} catch (IOException e) {
				System.out.println("IOException in parseOutput()");
				System.exit(1);
			}
		}

	/**
		* Parse the output of the traceroute program in Unix. Also works in Linux.
		* This is more complex than the output of the win95 version. Miraculously
		* it works too! 
		*/
	public void parseUnixOutput(BufferedReader in) {
		try {
			String s;
			//skip over initial lines of output
			s = read(in);
			while (s.indexOf("1  ") == -1) { 
				s = read(in);
			} 
				
			//parse Traceroute output
			char[] cha;
			char[] temp;
			int i,j,num;
			debugPrint("s = " + s);
			int hop = 0;

			//loop until end of traceroute output
			do {
					//skip over any blank lines in input
					while((s.trim()).compareTo("") == 0)
						s = read(in);
												
					if ((s.trim()).compareTo("Trace complete.") == 0)
		   			break;
												
					debugPrint("after blank skip, s = " + s);
												
					i = -1;
					num = 3;    //number of round trip time values per hop
											//(it is decremented for a lost packet)
					if (s.length() >= 149) {
						debugPrint("s.length = "+ s.length());
						cha = (s.substring(0, 149)).toCharArray();
					}
					else	
						cha = s.toCharArray();
                    
					while(cha[++i] == ' ')	//Ingnore beginning white space before hop #
								 ;
					++i; //in case there are double digits for hop number
                        
					//in the unix traceroute one or more '*'s can come before the the host name
					//signifying lost packets before the traceroute program knows what host it is
					//we deal with it here
					int acount = 0; 
					while(cha[++i] == ' ')      //Ingnore whitespace
								;

					int b;
					for (b = 0; b < 3;) {
						debugPrint("b = " + b + ", cha[i] = " + cha[i] + ", i = " + i);
						if ( cha[i] != '*') break;
						if(b == 2) {b++; break;} //avoid ArrayOutOfBoundsException
						while(cha[++i] == ' ')
							;
						b++;
					}
					num = num - b;
					debugPrint("skipped " + b + " '*' before hosts");
		   
					//worst case - three '*', host not reachable. To handle this
					//case make the hostName the error message and set average to 0
					if (b == 3) {
					 chartdata.hostName[hop] = "Unreachable host.";
					 chartdata.avg[hop] = 0;
								 s = read(in);
					 debugPrint("mid of loop - unreachable host. s = " + s);
					 continue;
					}

          int c, d = 0;
					try {
						c = s.indexOf(" (");
						//store the name and address for this
						chartdata.hostName[hop] = (s.substring(i,c)).trim(); 
						d = s.indexOf(") ");
						chartdata.IPNum[hop] = (s.substring(c+2,d)).trim();	 //host
						debugPrint("IPNum[hop] = " +	chartdata.IPNum[hop]);									  
					} catch (Exception stre) {
						//this catches the error that occurs if the request from tracert
						//times out then no host informatio is given
						//In this case just store the tracert info message
						debugPrint("Caught exception here");
						chartdata.hostName[hop] = (s.substring(i)).trim();
						chartdata.IPNum[hop] = "";
					} 


				
					temp = new char[7]; //7 -  max length of num (in Linux could be 123.456)
					i = d+1; //update the "pointer" to the position after the host Name and IPaddress
					int l = 0;
					debugPrint("Starting loop, b = " + b);
					try {	
						for (l = 0; l < (3 - b); l++) { //loop 3 time to get 3 time values
																						//b is the amount of timeouts
							debugPrint("Beginning of for loop, loop control l = " + l);
							
							//reinitialize temp to blanks
							for (int a = 0; a < temp.length; a++)
								temp[a] = ' ';	
			
								while(cha[++i] == ' ')	//Ingnore wh space
								  ;
								j = 0;
								do {														
									if (cha[i] == '*') {  //if timeout decr num
										temp[j] = '0';
										num--;
									}	
									else 
										temp[j] = cha[i];   //store ms time value in char array
 
									j++; i++;
									debugPrint ("break test: i >= cha.length" +i+" "+cha.length);
									if (i >= cha.length) break;
                } while(cha[i] != ' ');

							  String t = "";
							  int a = 0;
								try {
									t = new String(temp);
									debugPrint("temp is: " + temp);	
									debugPrint("(Float.valueOf... is : " +
														(Float.valueOf(t.trim())).intValue());


									if (System.getProperty("os.name").compareTo("Linux") == 0)
										a = (Float.valueOf(t.trim())).intValue();
									else
										a = Integer.parseInt(t.trim());
									debugPrint("chartdata.avg.length = " + chartdata.avg.length);
									chartdata.avg[hop] = chartdata.avg[hop] + a;
								} catch (Exception ne) {
									System.out.println("NumberFormatException in TracertWrapper");
									System.out.println(ne);
									System.out.println("NumberFormatException in TracertWrapper");
									System.out.println(ne);
								}

								debugPrint("i >=cha.length : " + i + " " + cha.length);
								if (i >= cha.length) break; 	
										
								while(cha[++i] == ' ')      //Ingnore beginning white space
 														;
								//skip of ms label
								if (cha[i] == 'm' && cha[i+1] == 's')
									 i += 2;
								if (i >= cha.length) 
									 break;
								debugPrint("After skipping ms label cha.length = " +
								cha.length + ", i =" + i);
			
								debugPrint("cha[i-2,i-1] ='"+ cha[i-2] + cha[i-1] + "'");  
  
						} //end of for loop to get time values
					} catch (ArrayIndexOutOfBoundsException ae) {
						System.out.println("Error in ParseOutput()");
						System.out.println("Trying to continue...");
						System.out.println(ae);
					}
		  

			debugPrint("after for loop, l = " + l + ", num = " + num + 
						", i = " + i + ", cha.length = " + cha.length);
      if (num != 0) {
        //store the average for at       
				//round trip time values for this host. l was the 
				//loop control variable in the above loop
				chartdata.avg[hop] = chartdata.avg[hop] / num; 
          
      }
      else {
         chartdata.avg[hop] = 0;
      }
      chartdata.packetLoss[hop] = 3 - num;
			hop++;	
			if (gui != null) gui.incrementProgressBar(progress);

			s = read(in);
			debugPrint("end of loop, s = " + s);
		} 
		while(s != null); //end of main do-while loop
				
		//hop--; //this is how many total hops there were
		debugPrint("after do-while loop, hop = " + hop);
		chartdata.hops = hop; //store it in chartdata object

    for (i = 0; i <=hop; i++) 
			 debugPrint((i + 1) + "\t" + chartdata.avg[i] + "\t" + chartdata.hostName[i] + 
				                "\tPacket Loss: " + chartdata.packetLoss[i]);
		debugPrint("Finished parsing output");
		} catch (Exception e) {
				System.out.println("Exception in parseOutput()");
				System.out.println(e);
				System.exit(1);
		}
  }

    
    private void debugPrint(String s) {
       if (debug) System.out.println(s);
    }
       
}



