import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;


public class Main extends JFrame
{
	final int N = 8;//rows and cols

	public MapButton[][] btnMap;//the map shown on the screen
	public int[][] map;//the map with numbers, 0: no obstacle, >0: English; <0: Chinese

	public int lasti, lastj;//last (i, j)
	public int cnt;
	public int cornerNum;//turning point number
	public int cornerI, cornerJ, cornerII, cornerJJ;//turning points
	public int total;//total number of data in database
	public int grades;
	public JLabel lbSelfGrade, lbSelfGradeTitle;//my grade
	public JLabel lbOppoGrade, lbOppoGradeTitle;//opponent's grade
	public Font btnFont;
	public JLabel lbWin, lbLose, lbWaiting;
	public JPanel pnlMap, panel2;
	public JButton btnReorder;
	public Socket socket;
	public DataOutputStream toServer;
	public DataInputStream fromServer;
	public boolean online;
	public static Lock lock;
	
	public void init()
	{
		online = true;
		String ip = "";
		try
		{
			lock = new ReentrantLock();//create a lock

			//read ip address from file
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("ip")));
			ip = br.readLine().trim();

			//connect to server
			socket = new Socket(ip, 8000);
			toServer = new DataOutputStream(socket.getOutputStream());
			fromServer = new DataInputStream(socket.getInputStream());

			if(!fromServer.readBoolean())
			{
				online = false;
				return;
			}

		}catch(Exception ex)
		{//read ip address failed
			online = false;
		}
	}

	public Main()
	{
		lasti = lastj = -1;
		map = new int[N][N];
		btnMap = new MapButton[N][N];
		for(int i=0; i<N; ++i)
			for(int j=0; j<N; ++j)
			{
				btnMap[i][j] = new MapButton(this, i, j, "");
				btnMap[i][j].setFont(btnFont);
				btnMap[i][j].addActionListener(btnMap[i][j]);
			}
		btnFont = new Font("楷书", Font.PLAIN,12);

		//the map
		JPanel panel = new JPanel();
		pnlMap = new JPanel();
		pnlMap.setLayout(new GridLayout(N, N, 1, 1));
		for(int i=0; i<N; ++i)
			for(int j=0; j<N; ++j)
				pnlMap.add(btnMap[i][j]);

		pnlMap.setBounds(20, 40, 640, 440);
		panel.setLayout(null);
		panel.add(pnlMap);

		//Grades
		grades=0;
		lbSelfGradeTitle = new JLabel("己方分数：");
		lbOppoGradeTitle = new JLabel("对方分数：");
		lbSelfGrade = new JLabel("0");
		lbOppoGrade = new JLabel("0");
		//change the grades color and size
		lbSelfGradeTitle.setFont(new Font("隶书", Font.PLAIN, 20));
		lbSelfGrade.setFont(new Font("宋体", Font.PLAIN, 20));
		lbOppoGradeTitle.setFont(new Font("隶书", Font.PLAIN, 20));
		lbOppoGrade.setFont(new Font("宋体", Font.PLAIN, 20));
		lbOppoGradeTitle.setForeground(Color.red);
		lbOppoGrade.setForeground(Color.red);
		//reorder
		btnReorder = new JButton("重排");
		btnReorder.addActionListener(new Reorder());
		//right panel

		panel2 = new JPanel();
		panel2.setLayout(new GridLayout(4, 1, 0, 0));
		panel2.setBounds(685, 80, 100, 200);
		panel2.add(lbSelfGradeTitle);
		panel2.add(lbSelfGrade);
		panel2.add(lbOppoGradeTitle);
		panel2.add(lbOppoGrade);
		panel.add(panel2);

		btnReorder.setBounds(680, 448, 100, 30);
		panel.add(btnReorder);

		//three labels
		//JLabel for win
		
		lbWin = new JLabel("<html><font color=red><center>又赢了，都不好意思了O(∩_∩)O~</center></font></html>");
		lbWin.setFont(new Font("隶书", Font.PLAIN, 65));
		lbWin.setVisible(false);
		lbWin.setBounds(60, 50, 680, 300);

		panel.add(lbWin);

		//JLabel for lose
		lbLose = new JLabel("<html><font color=gray><center>orz~</center></font></html>");
		lbLose.setFont(new Font("隶书", Font.PLAIN, 200));
		lbLose.setVisible(false);
		lbLose.setBounds(180, 90, 400, 200);

		panel.add(lbLose);
		//JLabel for waiting

		lbWaiting = new JLabel("<html><font color=orange><center>稍 等 哦<br>亲~~~</center></font></html>");
		lbWaiting.setFont(new Font("隶书", Font.PLAIN, 100));
		lbWaiting.setBounds(180, 80, 600, 250);

		panel.add(lbWaiting);

		//set main interface invisible
		panel2.setVisible(false);
		pnlMap.setVisible(false);
		btnReorder.setVisible(false);

		add(panel);
	}

	public void generateData(int[][] data)
	{
		//generate data
		int num=0;
		int ii, jj;
		Random rnd = new Random();
		boolean[][] m = new boolean[N][N];

		cnt = N*N/2;
		for(int i=0; i<cnt; ++i)
		{
			if(i%3==0)	num = rnd.nextInt(total)+1;
			//positive
			while(true)
			{
				ii = rnd.nextInt(N);
				jj = rnd.nextInt(N);
				if(!m[ii][jj])
				{
					m[ii][jj] = true;
					data[ii][jj] = num;
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
					data[ii][jj] = -num;
					break;
				}
			}
		}
	}

	public void parseMap(byte[] bytes)
	{
		int idx=0;
		for(int i=0; i<N; ++i)
		{
			for(int j=0; j<N; ++j)
			{
				map[i][j]=(bytes[idx]<<24) | (bytes[idx+1]<<16) | (bytes[idx+2]<<8) | bytes[idx+3];
				idx+=4;
			}
		}

		//test
		System.out.println("=========receive map==============");
		for(int i=0;i<N; ++i)
		{
			for(int j=0; j<N; ++j)
				System.out.print(map[i][j]+" ");
			System.out.println();
		}
		System.out.println("============print map end===============");
	}

	public void initBtnMap()
	{
		Access db = new Access();
		total = db.getTotal();
		if(!online)	generateData(map);//for stand-alone game

		for(int i=0; i<map.length; ++i)
			for(int j=0; j<map[i].length; ++j)
			{
				btnMap[i][j].setText(db.getItem(map[i][j]));
				btnMap[i][j].setVisible(true);
				btnMap[i][j].setBackground(null);
			}

		db.closeAccess();
	}

	class Reorder implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if(online)//online game
			{
				try
				{
					toServer.write("5".getBytes());
				}catch(IOException ex)
				{
					JOptionPane.showMessageDialog(null, "网络异常断开，关闭应用程序", "错误消息", JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				}
			}
			else//stand-alone game
			{
				grades-=5;
				lbSelfGrade.setText(grades+"");
				if(grades<0 && grades>=-5)	lbSelfGrade.setForeground(Color.red);
				Random rnd = new Random();
				for(int t=0; t<15; ++t)
				{
					int i = rnd.nextInt(N);
					int j = rnd.nextInt(N);
					int ii = rnd.nextInt(N);
					int jj = rnd.nextInt(N);
					if(i==ii && j==jj)	continue;

					int tmp = map[i][j];
					map[i][j] = map[ii][jj];
					map[ii][jj] = tmp;
				}
				
				Access db = new Access();
				for(int i=0; i<map.length; ++i)
					for(int j=0; j<map[i].length; ++j)
					{
						if(map[i][j]!=0)
						{
							btnMap[i][j].setText(db.getItem(map[i][j]));
							btnMap[i][j].setVisible(true);
							btnMap[i][j].setBackground(null);
						}
						else	btnMap[i][j].setVisible(false);
					}
				db.closeAccess();
			}
		}
	}

	public static void main(String[]  args)
	{
		Main frame = new Main();
		frame.setTitle("单词连连看");
		frame.setBounds(0, 0, 800, 560);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		frame.init();
		if(frame.online)
		{
			frame.setTitle("单词连连看(联机)");
			//create a thread for receiving data
			Thread thread = new Thread(new Client(frame));
			thread.start();
		}
		else
		{
			frame.setTitle("单词连连看(单机)");
			frame.btnReorder.setVisible(true);
			frame.pnlMap.setVisible(true);
			frame.panel2.setVisible(true);
			frame.lbOppoGradeTitle.setVisible(false);
			frame.lbOppoGrade.setVisible(false);
			frame.lbSelfGradeTitle.setText("得分：");
			frame.initBtnMap();
		}
	}
}
