
import java.net.*;
import java.io.*;
public class PatanP1Server {
	
	public static void main(String[] args) {
	  int serverPort = 10630;
	  //int serverPort = arg[0];
	  try {
		ServerSocket senderListenSocket = new ServerSocket(serverPort);
		while(true){
			//Listens to the Sender
			Socket senderSocket = senderListenSocket.accept();
			//Upon receiving connection creates PatanP1ServerConnection
			PatanP1ServerConnection connection = new PatanP1ServerConnection(senderSocket);
		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	}

}
