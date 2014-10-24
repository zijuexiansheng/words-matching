import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import javax.swing.*;
import java.awt.*;

class Client implements Runnable
{
	Main server;
	int[][] map;
	int id;
	int group;
	InetAddress inetAddress;
	String ip;
	int n;
	int[] I;
	int[] J;
	DataInputStream inputFromClient;

	public Client(int id, Main server)
	{
		this.id = id;
		this.server = server;
		try
		{
			inputFromClient = new DataInputStream(server.clients[id].socket.getInputStream());
			server.clients[id].outputToClient = new DataOutputStream(server.clients[id].socket.getOutputStream());
		}catch(IOException ex)
		{}
		I = new int[4];
		J = new int[4];
		if(id <= server.totalThread/2)
		{
			group = server.clients[id].group = 0;
			map = server.map1;
		}
		else
		{
			group = server.clients[id].group = 1;
			map = server.map2;
		}

		inetAddress = server.clients[id].socket.getInetAddress();
		ip = inetAddress.getHostAddress();
		server.jta.append(ip + "[" + id + ": "+group+"] connected\n");
		server.jta.setCaretPosition(server.jta.getText().length());
		
		try
		{
			server.clients[id].outputToClient.writeBoolean(true);//notice the client that he is accepted
		}catch(IOException ex)
		{}
	}

	public void parse(String str)
	{
		String[] ss = str.split(" ");
		n = Integer.parseInt(ss[0]);
		for(int i=1; i<ss.length; i+=2)
		{
			I[i>>>1] = Integer.parseInt(ss[i]);
			J[i>>>1] = Integer.parseInt(ss[i+1]);
		}
	}
	
	//send <5 grade> and <mapBytes> to all players
	public void sendStart()
	{
		server.jta.append("["+id + "]sendStart()\n");
		server.jta.setCaretPosition(server.jta.getText().length());

		server.grades[0] = server.grades[1] = 0;
		server.counters[0] = server.counters[1] = server.N*server.N/2;
		String str = "5 "+server.grades[group];
		server.generateMap();
		server.toBytes(server.map1);
		server.jta.setCaretPosition(server.jta.getText().length());

		for(int i=server.clients[server.head].next; i!=server.tail; i = server.clients[i].next)
		{
			try
			{
				server.clients[i].outputToClient.write(str.getBytes());
				server.clients[i].outputToClient.write(server.mapBytes);
			}catch(IOException ex)
			{}
		}
	}

	//reorder
	//send <5 grade> and <mapBytes> to player i and his partner
	//send <6 grade> to their opponents
	public void sendReorder()
	{
		server.jta.append("["+id+"]sendReorder()\n");
		server.jta.setCaretPosition(server.jta.getText().length());

		server.reorder(map);
		server.toBytes(map);
		server.grades[group] -= 5;

		for(int i=server.clients[server.head].next; i!=server.tail; i=server.clients[i].next)
		{
			//<5 grade> and <mapBytes>
			try
			{
			if(server.clients[i].group == group)
			{//the partner or the player i itself
				String str = "5 "+server.grades[group];
				server.clients[i].outputToClient.write(str.getBytes());
				server.clients[i].outputToClient.write(server.mapBytes);
			}
			else//<6 grade>
			{//the opponents
				String str = "6 "+server.grades[group];
				server.clients[i].outputToClient.write(str.getBytes());
			}
			}catch(IOException ex)
			{}
		}
	}
	
	//check if there's a pic between the two pos(not included) in the row
	//if yes, return true
	//otherwise, return false
	public boolean rowHasPic(int row, int start, int over)
	{
		if(start > over)
		{
			int t=start;	start = over;	over = t;
		}
		for(++start; start<over; ++start)
			if(map[row][start] != 0)
				return true;//there's a pic
		return false;
	}

	//check if there's a picc between the two pos(not included) in the col
	//if yes, return true
	//otherwise, return false
	public boolean colHasPic(int col, int start, int over)
	{
		if(start > over)
		{
			int t=start;	start=over;	over=t;
		}
		for(++start; start<over; ++start)
			if(map[start][col] != 0)
				return true;//there's a pic
		return false;
	}
	
	//finish
	//win: <7 win>
	//lose: <7 lose>
	public void sendFinish()
	{
		server.grades[group] += 10;
		String self, oppo;
		if(server.grades[group] > server.grades[group^1])
		{//win
			self = "7 win";
			oppo = "7 lose";
		}
		else
		{//lose
			self = "7 lose";
			oppo = "7 win";
		}

		for(int i=server.clients[server.head].next; i!=server.tail; i=server.clients[i].next)
		{
			try
			{
				if(server.clients[i].group == group)//the partner or the player i itself
					server.clients[i].outputToClient.write(self.getBytes());
				else
					server.clients[i].outputToClient.write(oppo.getBytes());
			}catch(IOException ex)
			{}
		}
	}

	//send player i and his partner: <n points grade>
	//send oppo: <6 grade>
	public void sendPairOK()
	{
		String self, oppo;
		oppo = "6 "+server.grades[group];
		self = n+" "+I[0]+" "+J[0];
		for(int i=1; i<=n; ++i)
			self+=" "+I[i]+" "+J[i];
		self+=" "+I[n+1]+" "+J[n+1]+" "+server.grades[group];
		for(int i=server.clients[server.head].next; i!=server.tail; i=server.clients[i].next)
		{
			try
			{
				if(server.clients[i].group == group)
					server.clients[i].outputToClient.write(self.getBytes());
				else
					server.clients[i].outputToClient.write(oppo.getBytes());
			}catch(IOException ex)
			{}
		}
	}

	public void acceptPair()
	{
		server.grades[group] += 2;
		--server.counters[group];
		if(group == 0)
		{
			server.map1[I[0]][J[0]] = 0;
			server.map1[I[n+1]][J[n+1]] = 0;
		}
		else
		{
			server.map2[I[0]][J[0]] = 0;
			server.map2[I[n+1]][J[n+1]] = 0;
		}
		if(server.counters[group] == 0)//finish
			sendFinish();
		else//not finish yet
			sendPairOK();
	}

	public void sendNoTurningPoint()
	{
		server.jta.append("["+id+"] sendNoTurningPoint()\n");
		server.jta.setCaretPosition(server.jta.getText().length());

		if(map[I[0]][J[0]]==0 || map[I[1]][J[1]]==0)	return;
		if(I[0] == I[1] && !rowHasPic(I[0], J[0], J[1]))//accept the pair
			acceptPair();
		else if(J[0] == J[1] && !colHasPic(J[0], I[0], I[1]))//accept the pair
			acceptPair();
	}

	public void sendOneTurningPoint()
	{
		server.jta.append("["+id+"] sendOneTurningPoint()\n");
		server.jta.setCaretPosition(server.jta.getText().length());

		if(map[I[0]][J[0]] == 0 || map[I[2]][J[2]]==0)	return;
		if(map[I[1]][J[1]] != 0)	return;
		if(I[0]==I[1])
		{
			if(!rowHasPic(I[0], J[0], J[1]) && !colHasPic(J[1], I[1], I[2]))//accept the pair
				acceptPair();
		}
		else//(J[0] == J[1])
		{
			if(!colHasPic(J[0], I[0], I[1]) && !rowHasPic(I[1], J[1], J[2]))//accept the pair
				acceptPair();
		}
	}

	public void sendTwoTurningPoints()
	{
		server.jta.append("["+id+"] sendTwoTurningPoint()\n");
		server.jta.setCaretPosition(server.jta.getText().length());
		if(map[I[0]][J[0]] == 0 || map[I[3]][J[3]]==0)	return;

		if(I[0] == I[1])
		{
			if(!rowHasPic(I[0], J[0], J[1]) && !rowHasPic(I[3], J[3], J[2]))
			{
				if(J[1]==-1 || J[1]==server.N)//accept the pair
					acceptPair();
				else if(map[I[1]][J[1]]==0 && map[I[2]][J[2]]==0 && !colHasPic(J[1], I[1], I[2]))
					acceptPair();
			}
		}
		else//J[0] == J[1]
		{
			if(!colHasPic(J[0], I[0], I[1]) && !colHasPic(J[3], I[3], I[2]))
			{
				if(I[1]==-1 || I[1]==server.N)//accept the pair
					acceptPair();
				else if(map[I[1]][J[1]]==0 && map[I[2]][J[2]]==0 && !rowHasPic(I[1], J[1], J[2]))
					acceptPair();
			}
		}
	}

	//thread running for the client
	public void run()
	{
		try
		{
			byte[] bytes;
			String str;

			while(true)
			{
				server.lock.lock();
				switch(server.state)
				{
					case 0://less more than 4 players and the game doesn't start
						if(server.cnt == 0)//there are four players now
						{
							server.state = 2;
							Thread.sleep(2000);
							sendStart();
						}
						break;
					case 1://less than 4 players but the game starts
						if(n == 5)//reorder
							sendReorder();
						else if( n == 0)
							sendNoTurningPoint();
						else if( n == 1)
							sendOneTurningPoint();
						else if( n == 2)
							sendTwoTurningPoints();

						if(server.counters[group] == 0)
						{//finish
							server.state = 0;
						}
						break;
					case 2://4 players and the game starts
						if( n == 5)//reorder
							sendReorder();
						else if(n == 0)
							sendNoTurningPoint();
						else if(n == 1)
							sendOneTurningPoint();
						else if(n == 2)
							sendTwoTurningPoints();

						if(server.counters[group] == 0)
						{//finish
							System.out.println("finish...");
							Thread.sleep(5000);//sleep 5 sec
							sendStart();//regenerate data
						}
						break;
				}
				server.lock.unlock();

				bytes = new byte[20];
				inputFromClient.read(bytes);
				str = new String(bytes);
				parse(str.trim());

				
				/*
				bytes = new byte[20];
				inputFromClient.read(bytes);
				str = new String(bytes);
				server.jta.append("from "+ip+"["+id+"]:["+str+"]\n");
				
				ss = str.trim().split(" ");
				for(int ii=0; ii<ss.length; ++ii)
					server.jta.append("["+ss[ii]+"]\n");
				server.jta.setCaretPosition(server.jta.getText().length());
				*/
			}
		}catch(Exception ex)
		{
			//System.err.println(ex);
		}finally
		{
			server.jta.append(ip+"["+id+"] disconnected\n");
			server.jta.setCaretPosition(server.jta.getText().length());
			try
			{
				server.clients[id].socket.close();
			}catch(Exception ex)
			{}
			
			server.lock.lock();
			//delete the node
			server.clients[server.clients[id].pre].next = server.clients[id].next;
			server.clients[server.clients[id].next].pre = server.clients[id].pre;

			server.freeNum[server.cnt++] = id;

			if(server.cnt == 4 || server.state == 0)
				server.state = 0;
			else
				server.state = 1;
			server.lock.unlock();
		}
	}
}

