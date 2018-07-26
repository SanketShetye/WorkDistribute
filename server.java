import java.io.*;
import java.net.*;

class ConnectedClients{
}

class ConnectionHandler implements Runnable{
	Socket clientSocket;
	String clientID;
	Callback clbk;

	ConnectionHandler(Socket clientSocket, String clientID, Callback clbk){
		this.clientSocket = clientSocket;
		this.clbk = clbk;
		this.clientID = clientID+"";
	}

	public void run(){
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(),true);
			
			pw.println(clientID);
		
			while(true){
				String message = br.readLine();
				if(!(message.equals(".q"))){
					//System.out.println("Client : "+message);
					//System.out.println("Message Received By ClientID="+clientID);
					
					if(message.substring(0,2).equals(".r")){
						//System.out.println("Result for Request is Returned.!");
						clbk.retRes(message.substring(2),clientID);
					}
					else{
						clbk.callbk(message,clientID);
					}
					//This is message from client.
					//int totalConnections = Server.connection.length;
				
				}
				else{
					System.out.println("Shutting Down Connection..!!");
					break;
				}
				//pw.println("Server Echo: "+message);
				//pw.flush();
			}
		}catch(IOException e){
			System.out.println("Error 1 in client "+e);
		}
	}

	public void sendWorkRequest(String req){
		try{
			PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(),true);
			pw.println(req);
			pw.flush();
		}catch(Exception e){
			System.out.println("Error 2 in client "+e);
		}		
	}

	public void requestComplete(String result){
		try{

			//System.out.println("Request Complete Method of "+clientID);

			PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(),true);
			pw.println("Server Completed Your Request Your RESULT is :"+result);
			pw.flush();
			System.out.println("Requested Operation by Client "+clientID+ " is Complete.");
		
		}catch(Exception e){
			System.out.println("Error 3 in client "+e);
		}
	}

	public void parseClientRequest(String req){
		String[] request = req.split(" ");
	}
}

class Server{
	public static String requestID = null;
	public static int totalClientConnected = -1;		
	public static ConnectionHandler[] connection = new ConnectionHandler[50];//At max 50 clients.

	public static int splitCount=-1;
	public static String[] res= new String[50];
	
	public static long startTime; 

	public static void main(String args[]){

		try{
			ServerSocket serverSocket = new ServerSocket(9090);
			System.out.println("Server Started");
			while(true){
				Socket clientSocket = serverSocket.accept();
				
				totalClientConnected++;
				connection[totalClientConnected] = new ConnectionHandler(clientSocket,totalClientConnected+"", new Callback(){
					
						public void callbk(String clientRequest, String clientID){
						//System.out.println("This is the Callback Method -- r="+r);
						String[] request = breakRequest(clientRequest);

						if(requestID != null){
							connection[Integer.parseInt(clientID)].requestComplete("Server is Busy.!");
							return;
						}
						
						System.out.println("REQUEST LENGTh IS = "+request.length);
						if(request.length != 4){
							connection[Integer.parseInt(clientID)].requestComplete("Unkown Command for Server");
							return;
						}

						requestID = request[0];
						System.out.println("GOT THE REQUEST FROM = "+clientRequest);
						splitCount = totalClientConnected+1;
						
						/*System.out.println("REQ 1 = "+request[1]);
						System.out.println("REQ 2 = "+request[2]);
						System.out.println("REQ 3 = "+request[3]);*/

						int jobs[] = splittingJobs(Integer.parseInt(request[2]),Integer.parseInt(request[3]),totalClientConnected+1);

						//int jobs[] = splittingJobs(1,5000,2);
						startTime = System.currentTimeMillis();
						for(int i=0;i<=totalClientConnected;i++)
							connection[i].sendWorkRequest(request[1]+" "+jobs[i]+" "+(jobs[i+1]-1));

						//connection[0].sendWorkRequest(request[1]+" 1 5000");
						//connection[1].sendWorkRequest(request[1]+" 5000 10000");
						
					}
			
					public void retRes(String result, String clientID){
						splitCount--;
						
						System.out.println("Client "+clientID+" has finished execution in "+System.currentTimeMillis()-startTime+" ms");	
						System.out.println("Clients Still Working = "+splitCount);
						
						res[Integer.parseInt(clientID)] = result;				
						if(splitCount == 0){
							//Task is completed. Notify the client Which requested the task.
							//Gather the result
							String resultForClient = "";
							for(int i = 0;i<=totalClientConnected;i++){
								resultForClient = resultForClient+res[i];				
							}
							//send it to the client
							System.out.println("Returning Result for ID="+Integer.parseInt(requestID));
							//System.out.println("Result = \n"+resultForClient);
							connection[Integer.parseInt(requestID)].requestComplete(resultForClient);
							requestID = null;

						}
					}
				});

				//System.out.println("GOT THE REQUEST === === R= "+request);

				new Thread(connection[totalClientConnected]).start();
			}
		}catch(Exception e){
			System.out.println("Error in Server "+e.getMessage());
			System.exit(-1);
		}
	}

	public static int[] splittingJobs(int jobStart,int jobEnd,int noOfClients)
	{
		int job[]=new int[noOfClients+1];
		//int jobTotalLength=(jobEnd-jobStart)%2==0?(jobEnd-jobStart):(jobEnd-jobStart+1);
		double lengthOfEachJob=1.0*(jobEnd-jobStart)/noOfClients;
		int i=0;
		
		double startOfEachJob=jobStart;
		
		job[i++]=jobStart;
		
		while((i<noOfClients+1))
		{
			double temporary=startOfEachJob+=lengthOfEachJob;
			//System.out.println("Start Of Each Job: START:"+startOfEachJob);
			/*if(startOfEachJob-(int)startOfEachJob>0.5)
				startOfEachJob=(int)startOfEachJob+1;
			else*/
				startOfEachJob=(int)startOfEachJob;
			//System.out.println("Start Of Each Job: END:"+startOfEachJob);
			job[i++]=(int)startOfEachJob;
			startOfEachJob=temporary;
		}
		
		if(job[i-1]!=jobEnd)
		{
			job[i-1]=jobEnd;
		}
		return job;
	}


	public static String[] breakRequest(String req){
		String[] request = req.split(" ");
		return request;
	}

}


interface Callback{
	public void callbk(String clientRequest,String clientID);
	public void retRes(String result,String clientID);
}

