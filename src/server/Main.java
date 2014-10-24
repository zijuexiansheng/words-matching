import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import javax.swing.*;
import java.awt.*;

class OneClient
{
	public Socket socket;
	public int pre;//pre linked node
	public int next;//next lined node
	public int group;//which group the player belongs to
	public DataOutputStream outputToClient;

	public OneClient()
	{
		pre = -1;
		next = -1;
		group = 0;
	}
	
	public OneClient(Socket socket)
	{
		pre = -1;
		next = -1;
		group = 0;
		this.socket = socket;
	}
}

public class Main extends JFrame
{
	final static int totalThread = 4;
	final static int N=8;
	final static int tail = totalThread+1;
	final static int head = 0;
	public static Lock lock = new ReentrantLock();//create a lock
	public int cnt;
	public int readyNum;
	public OneClient[] clients;//the linked list for clients
	public int[] freeNum;//free index, a stack
	public ExecutorService executor;
	public JTextArea jta = new JTextArea();
	public int[][] map1;//map of group1
	public int[][] map2;//map of group2
	public int[] grades;//grades of group1 and group2
	public int[] counters;//number of existing elements
	public byte[] mapBytes;//send to the clients
	public int state = 0;

	public Main()
	{
		setLayout(new BorderLayout());
		add(new JScrollPane(jta), BorderLayout.CENTER);
		jta.setEditable(false);

		setTitle("Server");
		setSize(500, 550);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	public void reorder(int[][] map)
	{
		Random rnd = new Random();
		for(int t=0; t<15; ++t)
		{
			int i = rnd.nextInt(N);
			int j = rnd.nextInt(N);
			int ii = rnd.nextInt(N);
			int jj = rnd.nextInt(N);
			if(i == ii && j == jj)	continue;

			int tmp = map[i][j];
			map[i][j] = map[ii][jj];
			map[ii][jj] = tmp;
		}
	}

	public void toBytes(int[][] map)
	{
		int idx = 0;
		for(int i=0; i<N; ++i)
		{
			for(int j=0; j<N; ++j)
			{
				mapBytes[idx++] = (byte)(map[i][j]>>>24);
				mapBytes[idx++] = (byte)(map[i][j]>>>16);
				mapBytes[idx++] = (byte)(map[i][j]>>>8);
				mapBytes[idx++] = (byte)(map[i][j]>>>0);
			}
		}
	}
	
	//generate data
	public void generateMap()
	{
		Access db = new Access();
		int total = db.getTotal();
		db.closeAccess();

		int num = 0;
		int ii, jj;
		Random rnd = new Random();
		boolean[][] m = new boolean[N][N];
		TreeSet<Integer> ts = new TreeSet<Integer>();
		int cc = N*N/2;
		for(int i=0; i<cc; ++i)
		{
			if(i%3 == 0)
			{
				num = rnd.nextInt(total)+1;
				while(ts.contains(num))
					num = rnd.nextInt(total)+1;
				ts.add(num);
			}
			//positive
			while(true)
			{
				ii = rnd.nextInt(N);
				jj = rnd.nextInt(N);
				if(!m[ii][jj])
				{
					m[ii][jj] = true;
					map1[ii][jj] = num;
					map2[ii][jj] = num;
					break;
				}
			}
			//negative
			while(true)
			{
				ii = rnd.nextInt(N);
				jj = rnd.nextInt(N);
				if(!m[ii][jj])
				{
					m[ii][jj] = true;
					map1[ii][jj] = -num;
					map2[ii][jj] = -num;
					break;
				}
			}
		}
	}

	public static void main(String[] args)
	{
		Main server = new Main();
		//init data
		server.executor = Executors.newFixedThreadPool(totalThread);
		server.cnt = totalThread;
		server.readyNum = 0;
		server.clients = new OneClient[totalThread+2];
		server.clients[head] = new OneClient();
		server.clients[tail] = new OneClient();
		server.clients[head].next = tail;//list head
		server.clients[tail].pre = head;//list tail
		server.freeNum = new int[totalThread];
		for(int i=0; i<totalThread; ++i)
			server.freeNum[i]=totalThread - i;
		server.map1 = new int[N][N];
		server.map2 = new int[N][N];
		server.mapBytes = new byte[N*N*4];
		server.grades = new int[2];
		server.counters = new int[2];
		server.state = 0;

		ServerSocket serverSocket;
		try
		{
			serverSocket = new ServerSocket(8000);
		}catch(IOException ex)
		{
			//System.out.println("create server error");
			server.jta.append("create server error\n");
			return ;
		}
		server.jta.append("Server starts at "+new Date()+"\n");

		while(true)
		{
			//Listen for a connection
			try
			{
				Socket socket = serverSocket.accept();

				if(server.cnt == 0 || server.state!=0)
				{//refuse
					socket.close();
				}
				else
				{
					lock.lock();//acquire a lock
					int index = server.freeNum[--server.cnt];//get the free index

					server.clients[index] = new OneClient(socket);
					//insert the new one to the tail of the list
					server.clients[index].pre = server.clients[tail].pre;
					server.clients[index].next = tail;
					server.clients[server.clients[index].pre].next = index;
					server.clients[server.clients[index].next].pre = index;

					server.executor.execute(new Client(index, server));
					lock.unlock();
				}
			}catch(Exception e)
			{}
		}
	}
}
