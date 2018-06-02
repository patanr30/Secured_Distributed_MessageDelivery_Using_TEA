import java.net.*;
import java.io.*;

public class PatanP1Receiver {

String iPAddress = "localhost";

public static void main(String[] args) {
	int port = 10631;
	  //int serverPort = arg[0];
	  try {
		ServerSocket RelayServerListener = new ServerSocket(port);
		while(true){
			Socket RelaySocket = RelayServerListener.accept();
			PatanP1ReceiverConnection connection = new PatanP1ReceiverConnection(RelaySocket);
		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	}


}
