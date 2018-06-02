import java.io.*;
import java.net.*;


public class PatanP1ReceiverConnection  extends Thread{
	 private  Socket senderSocket;
	 private DataInputStream in;
	 private  DataOutputStream out;
	 private String key = "0630";
	 private  int delta = 0x9e3779b9; // (2^32 golden ratio, key scheduling constant)
	 private int DECRYPT_SUM_INIT = 0xC6EF3720;
	 private int k[] = new int[4];
		// Mask for R -- LR -- 0 in left part, Right part 1
	 private long MASK32 = (1L << 32) - 1;
	 
	public PatanP1ReceiverConnection(Socket senderSocket) {
		
		this.senderSocket = senderSocket;
		try {
			this.in = new DataInputStream(senderSocket.getInputStream());
			this.out = new DataOutputStream(senderSocket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(senderSocket.isConnected()){
			this.start();
		}
	}
	
	/**************************************************************************
	 * @method: run
	 * @purpose: Receives request message from Relay server and provides responses
	 *************************************************************************/
	public void run(){
	   try {
		String request = in.readUTF();
		System.out.println("received  request "+request);
		
		request = decryt_Helper(request);
		System.out.println("after decryption request "+request);
		
		System.out.println("request string received in receiver "+request);
		boolean t = true;
		
		do{
			String req[]= request.split("9");
			//generate and send response to the Relay server
			response(req);
		    request = in.readUTF();

		    //Checks for close message
			if(request.equalsIgnoreCase("close"))
				t= false;
		}while(t);
		//Terminates when close message is received
		shutdown();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	 
	/***********************************************************************
	 * @method findLCS
	 * @purpose finds the longest common substring for given array of strings
	 * @param sub
	 * @return best match of longest common substring
	 ***********************************************************************/
	private String findLCS(String sub[] ){
		int n = sub.length;
		String temp = sub[1];
        String result = "";

		int len = temp.length();


		 for(int i=0; i<len; i++){
			for(int j=i+1; j<= len; j++){
				// generating all the possible substrings with first string
				String part = temp.substring(i, j);
				int k=0;
				for(k=2;k<n;k++){
					//Check if the generated subpart is common to all words
					if(!sub[k].contains(part)){
						break;
					}
				}
				
				//If current substring is present in all strings and its length is greater than the current result
				
				if(k==n && result.length()< part.length())
					result= part;
		 }
		}
		return result;
	}
	
	/**********************************************************************
	 * @methodName: shutdown
	 * @purpose: Closes the socket connection  to the RelayServer
	 *********************************************************************/
	
	private void shutdown(){
		System.out.println("Receiver has shutdown based on sender close message!!");
		try {
			senderSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	/****
	 * @methodName  response
	 * @purpose writes response to the relay Server based on given input
	 * @param req
	 * @throws IOException
	 */
	
	private void response(String[] req) throws IOException{
		//Find the Longest Common Substring for given substrings
		String resultLCS= "";
		//System.out.println(req);
		resultLCS= findLCS(req);
		
		
		String res = resultLCS;
		System.out.println("response from  Receiver"+res);
		//Send the response to the relay server
		res = encrpt_Helper(res);
		System.out.println("encrpted response from  Receiver"+res);
		out.writeUTF(res);
	}

		
 /************************************************************************
  * @Method:getBlocksofPlainText
  * @Purpose:Converts string of data into blocks of size 4 
  *  by appending / between blocks and for last insufficient block, pads 
  *  with required number of 1
  * @return String with appended / between each bloack of 4 strings
  * @param input string
  ************************************************************************/
	public  String getBlocksofPlainText(String input) {
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
		public  void getKey(String key) {
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
	  public   long[] encrypt(long in[]) {
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
	  public  long[] decrypt(long in[]) {
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
       private  String encrpt_Helper(String inputData){
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
       
       private  String decryt_Helper(String inputData){
  			
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
