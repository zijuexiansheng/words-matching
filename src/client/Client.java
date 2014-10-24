import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

class Client implements Runnable
{	
	public Main tMain;
	public int state;
	public int num;
	public int grade;
	public String res;
	public int[] I;
	public int[] J;
	
	public Client(Main tMain)
	{
		this.tMain = tMain;
		state = 2;
		I = new int[4];
		J = new int[4];
	}

	public void parse(String str)
	{
		String[] ss = str.split(" ");
		num = Integer.parseInt(ss[0]);
		switch(num)
		{
			case 0: case 1: case 2:
				for(int i=1; i<ss.length-1; i+=2)
				{
					I[i>>>1] = Integer.parseInt(ss[i]);
					J[i>>>1] = Integer.parseInt(ss[i+1]);
				}
			case 5:	case 6:
				grade = Integer.parseInt(ss[ss.length-1]);
				break;
			case 7:
				res = ss[1];
				break;
		}
	}

	//override the run() method
	public void run()
	{
		byte bytes[];
		String str;
		try
		{
			while(true)
			{
				System.out.println("waiting for message");
				bytes = new byte[30];	
				tMain.fromServer.read(bytes);
				str = new String(bytes);
				System.out.println("receive "+str.trim());
				parse(str.trim());

				switch(state)
				{
					case 2://waiting
						if(num == 5)
						{//start
							tMain.lbSelfGrade.setText(grade+"");
							tMain.lbOppoGrade.setText("0");
							if(grade<2 && grade>=0)	tMain.lbSelfGrade.setForeground(Color.black);
							//wait for the mapBytes
							bytes = new byte[tMain.N*tMain.N*4];
							tMain.fromServer.read(bytes);
							tMain.parseMap(bytes);


							tMain.lbWaiting.setVisible(false);

							System.out.println("After lbWaiting.setVisible(false)");
							tMain.panel2.setVisible(true);
							tMain.pnlMap.setVisible(true);
							tMain.btnReorder.setVisible(true);

							tMain.initBtnMap();

							System.out.println("Show Game Interface");
							state = 3;
						}
							
						break;
					case 3://in game
						if(num == 5)
						{//reorder
							tMain.lbSelfGrade.setText(grade+"");
							if(grade<2 && grade>=0)	tMain.lbSelfGrade.setForeground(Color.black);
							else if(grade<0 && grade>=-5)	tMain.lbSelfGrade.setForeground(Color.red);

							//wait for the mapBytes
							bytes = new byte[tMain.N * tMain.N*4];
							tMain.fromServer.read(bytes);
							tMain.lock.lock();
							tMain.parseMap(bytes);
							Access db = new Access();
							for(int i=0; i<tMain.N; ++i)
								for(int j=0; j<tMain.N; ++j)
								{
									if(tMain.map[i][j]!=0)
									{
										tMain.btnMap[i][j].setText(db.getItem(tMain.map[i][j]));
										tMain.btnMap[i][j].setVisible(true);
										tMain.btnMap[i][j].setBackground(null);
									}
									else	tMain.btnMap[i][j].setVisible(false);
								}
							db.closeAccess();
							tMain.lock.unlock();
						}
						else if(num == 6)
						{//update others grade
							tMain.lbOppoGrade.setText(grade+"");
						}
						else if(num>=0 && num<=2)
						{//del two buttons
							tMain.lbSelfGrade.setText(grade+"");
							if(grade<2 && grade>=0)	tMain.lbSelfGrade.setForeground(Color.black);
							tMain.lock.lock();
							tMain.map[I[0]][J[0]] = 0;
							tMain.map[I[num+1]][J[num+1]] = 0;
							tMain.btnMap[I[0]][J[0]].setVisible(false);
							tMain.btnMap[I[num+1]][J[num+1]].setVisible(false);
							if(tMain.lasti == I[0] && tMain.lastj == J[0] || tMain.lasti==I[num+1] || tMain.lastj==J[num+1])
							{
								tMain.lasti = -1;
								tMain.lastj = -1;
							}
							tMain.lock.unlock();
						}
						else if(num == 7)
						{//finish: win or lose
							if(res.equals("win"))
							{
								tMain.lbWin.setVisible(true);
								tMain.pnlMap.setVisible(false);
								tMain.panel2.setVisible(false);
								tMain.btnReorder.setVisible(false);

								Thread.sleep(2000);
								tMain.lbWin.setVisible(false);
							}
							else if(res.equals("lose"))
							{
								tMain.lbLose.setVisible(true);
								tMain.pnlMap.setVisible(false);
								tMain.panel2.setVisible(false);
								tMain.btnReorder.setVisible(false);

								Thread.sleep(2000);
								tMain.lbLose.setVisible(false);
							}
							tMain.lbWaiting.setVisible(true);
							state = 2;
						}
						break;
				}
			}
		}catch(Exception ex)
		{
			JOptionPane.showMessageDialog(tMain, "In Run();网络异常断开，关闭应用程序", "错误消息", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
			
		}
	}
}
