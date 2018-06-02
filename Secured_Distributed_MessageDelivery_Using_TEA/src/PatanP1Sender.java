import java.net.*;
import java.io.*;

public class PatanP1Sender {
	private static String key = "0630";
	private static int delta = 0x9e3779b9; // (2^32 golden ratio, key scheduling constant)
	private static int DECRYPT_SUM_INIT = 0xC6EF3720;
	private static int k[] = new int[4];
	// Mask for R -- LR -- 0 in left part, Right part 1
	private static long MASK32 = (1L << 32) - 1;

	public static void main(String[] args) throws IOException {
        //String ip = args[0];
		String ip = "127.0.0.1";
		Socket serverSocket = null;
		DataOutputStream out;
		DataInputStream in;
		try {
     
			// Connecting to the Relay Server with port 10630
			//serverSocket = new Socket("localhost", 10630);
			serverSocket = new Socket(ip, 10630);
			out = new DataOutputStream(serverSocket.getOutputStream());
			in = new DataInputStream(serverSocket.getInputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));

			// Connection to Relay Server is Success -- User authentication
			// begins
			if (serverSocket.isConnected()) {
				System.out.println("Welcome to TCP Service");
				System.out.println("\nPlease enter your username\n");

				// Authenticate the user
				if (authentication(serverSocket)) {
					// Connect to the valid receiver
					if (receiverConnection(serverSocket)) {
						boolean t = true;

						do {

							// Now user can use longest common substring service
							// as many times he/she wishes to
							serviceLCS(serverSocket);
							System.out
							.println("\n Do you want to continue Yes/No");
							String input = br.readLine();
							if (input.equalsIgnoreCase("no")) {
								out.writeUTF("close");
								t = false;
							}
						} while (t);

						// If user wants to terminate, shutdown the service
						shutdown(serverSocket);
					}
				}

			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("\nSomething went wrong!! try after some time\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("\nSomething went wrong!! try after some time\n");
		} finally {
			serverSocket.close();
		}

	}

	/******************************************************************************
	 * @Purpose: Accepts User name and password, then forwards the credential to
	 * the RelayServer for authentication. Based on response from Relay Server,
	 * Displays error message to the user.Terminates the connection if user
	 * enters close message.
	 * 
	 * @MethodName: authentication
	 * @parameter serverSocket
	 * @return true upon valid credentials or terminates the connection
	 * @throws IOException
	 *****************************************************************************/
	private static boolean authentication(Socket serverSocket)
			throws IOException {

		DataOutputStream out = new DataOutputStream(
				serverSocket.getOutputStream());
		DataInputStream in = new DataInputStream(serverSocket.getInputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String username = br.readLine();
		// Entered message is not a close message
		if (!(username.equalsIgnoreCase("Close"))) {

			// Send Username to Relay Server
			//System.out.println("username "+username);
			username = encrpt_Helper(username);
			//System.out.println("Encrpted username "+username);
			out.writeUTF(username);
			String ack1 = in.readUTF();
			if (ack1.equalsIgnoreCase("Success")) {
				// Valid Username acknowledgment
				// Accept password from user
				System.out.println("\nPlease enter the password\n");
				String password = br.readLine();
				password = encrpt_Helper(password);
				out.writeUTF(password);
				String ack2 = in.readUTF();
				if (ack2.equalsIgnoreCase("Success")) {
					// Valid password acknowledgment -- Authentication is
					// Successful
					System.out.println("\n Authentication is successfull \n");
					return true;

				} else {
					// Invalid password acknowledgment -- Ask users to reenter
					System.out
					.println("\nAuthentication failure!! Invalid Password!\n");
					System.out.println("\nTry again!\n");
					System.out.println("\nPlease enter your username\n");
					// Repeat entire authentication process
					if (authentication(serverSocket))
						;
					return true;
				}

			} else {
				// Invalid username --- Ask users for valid username
				// Repeat entire authentication process
				System.out.println("\nPlease enter valid username\n");
				if (authentication(serverSocket))
					;
				return true;
			}

		} else {
			// If user enters close message, terminate the connection
			br.close();
			in.close();
			out.close();
			shutdown(serverSocket);
		}
		return false;
	}

	/**********************************************************************
	 * @purpose: Closes the socket to the RelayServer
	 * @MethodName: shutdown
	 * @param serverSocket
	 *********************************************************************/

	private static void shutdown(Socket serverSocket) {
		System.out.println("Thanks for using TCP service!!");
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/***************************************************************
	 * @purpose: Accepts receiver name and forwards the receiver name
	 *           to the Relay Server. Based on response from
	 *           Relay Server, displays error message.Terminates the
	 *           connection if user enters close message
	 * @MethodName: receiverConnection
	 * @param serverSocket
	 * @return true for successful receiver connection or else terminates
	 * @throws IOException
	 **************************************************************/
	
	private static boolean receiverConnection(Socket serverSocket)
			throws IOException {
		DataOutputStream out = new DataOutputStream(
				serverSocket.getOutputStream());
		DataInputStream in = new DataInputStream(serverSocket.getInputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out
		.println("\nPlease enter the receiver name you wish to connect with\n");
		String receiverName = br.readLine();
        //Checks if the user has entered close message
		if (!(receiverName.equalsIgnoreCase("Close"))) {
			//Sends receiver name to RelayServer
			out.writeUTF(receiverName);
			String ack3 = in.readUTF();
			
			if (ack3.equalsIgnoreCase("Success")) {
				//Successfully connected to the receiver
				System.out.println("\n Succesfully Connected with Receiver \n");
				return true;
			} else {
				//For invalid receiver name, again accept receiver name 
				System.out.println("\n!!!Please provide valid reciever name!!!\n");
				//repeat enter receiver check method 
				if (receiverConnection(serverSocket))
				return true;
			}

		} else {
			br.close();
			in.close();
			out.close();
			//When user enters close message, terminate the relay server connection
			shutdown(serverSocket);
		}
		return false;
	}

	
	/*****************************************************************************
	 * @purpose Creates the request message to user entered inputs in the format
	 *          n/string1/string2/string3/....../stringn
	 * @MethodName:serviceRequestLCS
	 * @param serverSocket
	 * @return request string in predefined format
	 * @throws IOException
	 ***************************************************************************/

	private static String serviceRequestLCS(Socket serverSocket)
			throws IOException {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String request = "";
		System.out.println("\n ---- Largest Common Substring (LCS) Service----- \n");
		System.out.println("\n Please enter the count of Strings for LCS \n");
		request = br.readLine();
		// Check whether count is a valid number or not
		if (!checkNumeric(request)) {
			//If not numeric, request again and check
			System.out
			.println("\n !!! Please enter valid number for count of Strings !!! \n");
			request = br.readLine();
			if (!checkNumeric(request)) {
				//If user has entered invalid number again, terminate the connection 
				System.out.println("\n Try the service again \n");
				shutdown(serverSocket);
			}

		}
        // Valid count number has been entered
		int count = Integer.parseInt(request);
		//Construct request string
		for (int i = 0; i < count; i++) {
			System.out.println("\n Please enter " + (i + 1) + "th String\n");
			request = request + "9" + br.readLine();
		}

		return request;

	}

	/*****************************************************************
	 * @purpose: Check whether String is a valid number
	 * @MethodName:serviceLCS
	 * @param str
	 * @return true if valid numeric
	 ****************************************************************/
	private static boolean checkNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	/*****************************************************************
	 * @purpose: Creates request string to LCS service, forwards request
	 *           string to Relay Server and Displays the response
	 * @MethodName:serviceLCS
	 * @param serverSocket
	 * @throws IOException
	 *****************************************************************/

	private static void serviceLCS(Socket serverSocket) throws IOException {
		DataOutputStream out = new DataOutputStream(
				serverSocket.getOutputStream());
		DataInputStream in = new DataInputStream(serverSocket.getInputStream());
		// Building request object based on user input
		String request = serviceRequestLCS(serverSocket);
		out.writeUTF(request);
		// Receive the response form the relay Server
		String response = in.readUTF();
		System.out.println("received response from server "+response);
		
		response = decryt_Helper(response);
		System.out.println("after decryption received reponse "+response);
		// System.out.println(response);
		//String res[] = response.split("0");
		System.out.println("\n The longest Common Substing is --- " + response);
	}
	
	
 /************************************************************************
  * @Method:getBlocksofPlainText
  * @Purpose:Converts string of data into blocks of size 4 
  *  by appending / between blocks and for last insufficient block, pads 
  *  with required number of 1
  * @return String with appended / between each bloack of 4 strings
  * @param input string
  ************************************************************************/
	public static String getBlocksofPlainText(String input) {
		//Max input message size is 10X64 = 640bits/16=40 characters 
		String blocks_Data = new String();
		int count = 0, idx = 0;
		try {
			int length = input.length();
			int num_blocks = length/4;
			
				while ( num_blocks>0 && count < num_blocks) {
					//Block of 64 bits each --- 16 *4
					/*data[count++] = Long.parseLong(input.substring(idx, idx + 4),36);
					idx += 4;*/
					blocks_Data += input.substring(idx, idx + 4)+",";
					idx += 4;
					count++;
				}
			
			int left_Characters = length%4;
				
			String leftOverCharecters = input.substring(idx, idx+left_Characters);
			int padding_Count = 4 - leftOverCharecters.length();
				
				while(padding_Count>0){
					input = input+ "1";
					padding_Count--;
				}
			blocks_Data += input.substring(idx, idx + 4)+",";
		
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		return blocks_Data;
	}
	
	
  /***************************************************************************
  * @Method:getKey
  * @Purpose:Converts string key into int array of size 4 (4*32) = 128 bit key
  * @param input string key
  ***************************************************************************/
	// Key Size is 128 bits -- 128/16 = 8, in int 128/32 = 4 
		public static void getKey(String key) {
			int count = 0, idx = 0;
			try {
				while (count <= 3) {
					k[count++] = Integer.parseInt(String.valueOf(key.charAt(idx)));
					idx += 1;
				}	
			} catch (NumberFormatException ex) {
				ex.printStackTrace();
			}
		}	

  /************************************************************************
  * @Method:encrypt
  * @Purpose: Uses TEA algorithm to encrpt given 128 bit of data into  
  * 128 bit ciphertext using 128 shared key with 32 rounds.
  * @return 128 bit ciphertext stored in long array of size 2 (64*2)
  * @param 128 bit data input as a long array of size 2 (64*2)
  ************************************************************************/		
	  public  static long[] encrypt(long in[]) {
			   long[] cipher = new long[2];
		    	//R
		        int v01 = (int) in[0];
		        //L
		        int v00 = (int) (in[0] >>> 32);
		        //R
		        int v11 = (int) in[1];
		        //L
		        int v10 = (int) (in[1] >>> 32);
		        
		        int sum = 0;
		        for (int i=0; i<32; i++) {
		            sum += delta;
		            v00 += ((v01<<4) + k[0]) ^ (v01 + sum) ^ ((v01>>>5) + k[1]);
		            v10 += ((v11<<4) + k[0]) ^ (v11 + sum) ^ ((v11>>>5) + k[1]);
		            v01 += ((v00<<4) + k[2]) ^ (v00 + sum) ^ ((v00>>>5) + k[3]);
		            v11 += ((v10<<4) + k[2]) ^ (v10 + sum) ^ ((v10>>>5) + k[3]);
		        }
		       // In long, left part v0 and right part v1 ---v0v1
		        cipher[0]=  (v00 & MASK32) << 32 | (v01 & MASK32);
		        cipher[1]=  (v10 & MASK32) << 32 | (v11 & MASK32);
		       return cipher;
		 }

  /************************************************************************
  * @Method:decrypt
  * @Purpose: Uses TEA algorithm to decrypt given 128 bit of ciphertext to  
  * 128 bit data using 128 shared key with 32 rounds.
  * @return 128 bit decrypted data stored in long array of size 2 (64*2)
  * @param 128 bit ciphertext input as a long array of size 2 (64*2)
  ************************************************************************/
	  public static long[] decrypt(long in[]) {
		   long[] plainText = new long[2];
	    	//R
	        int v01 = (int) in[0];
	        //L
	        int v00 = (int) (in[0] >>> 32);
	    	//R
	        int v11 = (int) in[1];
	        //L
	        int v10 = (int) (in[1] >>> 32);
	        
	        int sum = DECRYPT_SUM_INIT;
	        for (int i=0; i<32; i++) {
	            v01 -= ((v00<<4) + k[2]) ^ (v00 + sum) ^ ((v00>>>5) + k[3]);
	            v11 -= ((v10<<4) + k[2]) ^ (v10 + sum) ^ ((v10>>>5) + k[3]);
	            v00 -= ((v01<<4) + k[0]) ^ (v01 + sum) ^ ((v01>>>5) + k[1]);
	            v10 -= ((v11<<4) + k[0]) ^ (v11 + sum) ^ ((v11>>>5) + k[1]);
	            sum -= delta;
	        }
	        //long --- v0v1
	        plainText[0]= (v00 & MASK32) << 32 | (v01 & MASK32);
	        plainText[1]= (v10 & MASK32) << 32 | (v11 & MASK32);
	        return plainText;
	    }
	
  /*********************************************************************************
  * @Method:encrpt_Helper
  * @Purpose: For an input string, provides the encrpted string using TEA algorithm
  * for transmittion.
  * @return cipher text for transmittion
  * @param plaintext of any size to encrpt
  *********************************************************************************/
       private static String encrpt_Helper(String inputData){
    	   String output= new String();
   		try{
   			//Gets ket string into int[4];
   			getKey(key);
   			String data = getBlocksofPlainText(inputData);
   			
   			//block of size 4 characters -- 16*4 = 64 bit
   			String[] data_block = data.split(",");
   			
   			int size = data_block.length;
   			long input[] = new long[size];
   			
   			//String to long
   			for(int i=0; i<size; i++){
   				input[i] = Long.parseLong(data_block[i],36);
   			}
   			
   			// encrypt 128 bits --- 2 long
   			int encrpts_count = input.length/2, y=0;
   			int left_for_encrpt = input.length%2;
   			
   			long cipher[];
   			
   			if(left_for_encrpt==1){
   				cipher = new long[size+1];
   			}else{
   				cipher = new long[size];
   			}
   			
   			
   			long in[] = new long[2];
   			long out[] = new long[2];
   			
   			
   			while(encrpts_count>0){
   				 in[0] = input[y];
   				 in[1] = input[y+1];
   				
   				 out = encrypt(in);
   				 
   				 cipher[y] = out[0];
   				 cipher[y+1] = out[1];
   				 
   				 y=y+2;
   				 encrpts_count--;
   			}
   			
   			if(left_for_encrpt==1){
   				 in[0] = input[y];
   				 String padding = "1111";
   				 in[1] = Long.parseLong(padding, 36);
   				 out = encrypt(in);
   				 cipher[y] = out[0];
   				 cipher[y+1] = out[1];
   				 
   			}
   			//encryted long into String
   			
   			
   			
   			for(long block :cipher ){
   				output += block+",";
   			}
   			//blocks of 8;
   			//System.out.println("Encrpted Code "+output);
 
   			
   		}catch(Exception e){
   			e.getMessage();
   			e.printStackTrace();
   		}
			return output;
   	}
       
   /*********************************************************************************
  * @Method:decryt_Helper
  * @Purpose:For a cipher text, provides the decrpyted string using TEA algorithm
  * after receiving data.
  * @return decrpyted text for received cipher text.
  * @param cipher text received.
  *********************************************************************************/       
       
       private static String decryt_Helper(String inputData){
  			
  		String output_D= new String();  		
    	   try{
           //Decryption starts
    	    getKey(key);
   			String[] data_block_forDecryt = inputData.split(",");
   			int size_D = data_block_forDecryt.length, z=0;
   			
   			long [] cipherText_D = new long[size_D];
   			
   			for(String data_D:data_block_forDecryt){
   				cipherText_D[z] = Long.parseLong(data_D);
   				z++;
   			}
   			
   			//Decrypt 
   			
   			// encrypt 128 bits --- 2 long
   			long decrpted[] = new long[size_D];
   			
   			int count = cipherText_D.length/2, a=0;
   			
   			long in_D[] = new long[2];
   			long out_D[] = new long[2];
   			
   			while(count>0){
   				 in_D[0] = cipherText_D[a];
   				 in_D[1] = cipherText_D[a+1];
   				
   				 out_D = decrypt(in_D);
   				 
   				 decrpted[a] = out_D[0];
   				 decrpted[a+1] = out_D[1];
   				 
   				 a=a+2;
   				 count--;
   			}
   			
   			//encryted long into String

   			
   			for(long block :decrpted ){
   				output_D += Long.toString(block, 36);
   			}
   			//blocks of 8;
   			System.out.println("Decrypted  Code "+output_D.replaceAll("1", ""));
   			
   		}catch(Exception e){
   			e.getMessage();
   			e.printStackTrace();
   		}
    		return output_D.replaceAll("1", "");
   		}
}
