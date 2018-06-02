
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PatanP1ServerConnection extends Thread{
 private Socket senderSocket;
 private Socket receiverSocket;
 private String iPaddress;
 private DataInputStream in;
 private DataOutputStream out;
 private Map<String,String> userList;
 private  Map<String,List> receiverList;
	
 private String key = "0630";
 private int delta = 0x9e3779b9; // (2^32 golden ratio, key scheduling constant)
 private int DECRYPT_SUM_INIT = 0xC6EF3720;
 private int k[] = new int[4];
	// Mask for R -- LR -- 0 in left part, Right part 1
 private  long MASK32 = (1L << 32) - 1;
 
  
 public PatanP1ServerConnection(Socket senderSocket) {

		this.senderSocket = senderSocket;
		this.userList = new HashMap<String,String>();
		this.receiverList = new HashMap<String,List>();
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
 
 /*****************************************************************************
  * @Method:run
  * @purpose:Provides authentication service, receiver connection service,
  *          Intermediate service between sender and Receiver
  ****************************************************************************/

 public void run(){
	 try {
		if(autheticationCheck()){
			//If authentication is successful
			if(receiverNameCheck()){
				//If successfully connected to Receiver
				DataOutputStream receiverOut = new DataOutputStream(receiverSocket.getOutputStream());
				boolean t = true;
				String requestLCS= in.readUTF();
				do{
					//Provides intermediate services between sender and receiver
					forwardRequestToReceiver(requestLCS);
					requestLCS= in.readUTF();
					if(requestLCS.equalsIgnoreCase("close"))
						t= false;
				}while(t);
				receiverOut.writeUTF("close");	
			}
	     }
	} catch (IOException e) {
		// TODO Auto-generated catch block
		System.out.println("Thanks for using TCP service!!");
		System.out.println("Something went wrong!!!,Try after sometime!!");
		try {
			senderSocket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//e.printStackTrace();
	}
 }
 
 /*********************************************************
  * @Method: receiverNameCheck
  * @purpose: Checks receiver name against receiverList.txt
  *           and for valid receiver, connects to the 
  *           requested receiver and sends Success and fail
  *           acknowledgment to Sender 
  * @return true for successful receiver connection
  * @throws IOException
  ********************************************************/
 
 private boolean receiverNameCheck()throws IOException{
	 getReceiverListMap();
	 String receiverName = in.readUTF();
	 if(receiverList!=null){
		 if(receiverList.containsKey(receiverName)){
			
			 ArrayList receiverData = (ArrayList)receiverList.get(receiverName);
			 String ip ="";
			 int  portAddress =0;
			 if(receiverData != null && receiverData.size()==2){
				 ip = (String)receiverData.get(0);
				 portAddress = Integer.parseInt((String)receiverData.get(1));
			 }
			 //You can use the receiver data i.e ip and portAddress above 
			// iPaddress = "localhost";
			// int port = 10631;
			 receiverSocket = new Socket(ip,portAddress); 
			 if(receiverSocket.isConnected()){
			      out.writeUTF("Success"); 
			      return true;
			 }
			 
		 }else{
			 out.writeUTF("Fail");
			 if(receiverNameCheck());
				return true; 
		 }
		 
	 }else{
		 System.out.println("\n Unable to read receiver file..Please try again\n"); 
		 senderSocket.close();
	 }
	 
	 return false;
 }
 
 
 /********************************************************************
  * @Method: autheticationCheck
  * @purpose: Checks user name  and password against userList.txt
  *           and sends Success and fail acknowledgment to Sender 
  * @return true for successful authentication
  * @throws IOException
  *******************************************************************/
 
 private boolean autheticationCheck() throws IOException{
	 //Get UserList data
	 getUserListMap();
	 String username;
	 String passwordFromList;
		if(!(senderSocket.isClosed())){
			
			username = in.readUTF();
			System.out.println("received name "+username);
			
			username = decryt_Helper(username);
			System.out.println("after decryption name "+username);
			if(!(username.equalsIgnoreCase("close"))){
				//Received message is not a Close message 
				if(userList != null){
					//If Valid Username is received
					if(userList.containsKey(username)){
						passwordFromList = userList.get(username);
						//Acknowledgement to username
						out.writeUTF("Success");
						String dataPasswordFromUser = in.readUTF();
						dataPasswordFromUser = decryt_Helper(dataPasswordFromUser);
						System.out.println("after decryption password "+dataPasswordFromUser);
						
							if(dataPasswordFromUser.equals(passwordFromList)){
								//If Authentication successful, send Acknowledgment to password
								out.writeUTF("Success");
								return true;
							}else{
								//Invalid Password
								out.writeUTF("Fail");
								if(autheticationCheck());
								return true;
							}
						
					}else{
						//Invalid Username is received
						out.writeUTF("Fail");
						if(autheticationCheck());
						return true;
					}
				}else{
					//If user list is Null
					System.out.println("\n Unable to read UserList ... try again\n");
					senderSocket.close();
				}
			}
		
		}
	 
	 return false;
 }
 
 /********************************************************************
  * @Method: getUserListMap
  * @purpose: OPens userList.txt and get the content into HashMAp 
  * @throws IOException
  *******************************************************************/
 
 
 private void getUserListMap() throws IOException{
	 BufferedReader br = null;
	 try {
		br = new BufferedReader(new FileReader("userList.txt"));
		String text ="";
		while((text= br.readLine())!= null){
			String substring[] = text.split(" ");
			userList.put(substring[0], substring[1]);
		}
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally{
		br.close();
	}
 }


 /********************************************************************
  * @Method: getReceiverListMap
  * @purpose: OPens receiverList.txt and get the content into HashMAp 
  * @throws IOException
  *******************************************************************/
 
 private void getReceiverListMap() throws IOException {
	 BufferedReader br = null;
	 try {
		br = new BufferedReader(new FileReader("receiverList.txt"));
		String text ="";
		while((text= br.readLine())!= null){
			String substring[] = text.split(" ");
			List<String> list = new ArrayList<String>();
			list.add(substring[1]);
			list.add(substring[2]);		
			receiverList.put(substring[0], list);
		}
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}finally{
		br.close();
	}
 }
 
 /*************************************************************************
  * @Method:forwardRequestToReceiver
  * @Purpose:Forwards the request to Receiver and Writes the  response
  *  back to sender
  * @param requestLCS
  * @throws IOException
  ************************************************************************/

private void forwardRequestToReceiver(String requestLCS) throws IOException{
	//Forward request string from sender to the receiver
	
	DataInputStream recieverIn = new DataInputStream(receiverSocket.getInputStream());
	DataOutputStream receiverOut = new DataOutputStream(receiverSocket.getOutputStream());
	System.out.println("request string in relay server "+requestLCS);
	requestLCS = encrpt_Helper(requestLCS);
	receiverOut.writeUTF(requestLCS);
	//Forward result from Receiver to Sender
	String response = recieverIn.readUTF();
	//System.out.println("received reponse from receiver "+response);
	
	//response = decryt_Helper(response);
	//System.out.println("after decryption received reponse "+response);
	System.out.println("response string in relay server"+response);
	out.writeUTF(response);
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
			System.out.println("Encrpted Code "+output);

			
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
