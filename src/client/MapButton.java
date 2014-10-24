import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

class MapButton extends JButton implements ActionListener
{
	public int i;
	public int j;
	Main tMain;
	public MapButton(Main tMain, String text)
	{
		super(text);
		this.tMain = tMain;
		i = 0;
		j = 0;
	}

	public MapButton(Main tMain, int i, int j, String text)
	{
		super(text);
		this.tMain = tMain;
		this.i = i;
		this.j = j;
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
			if(tMain.map[row][start] != 0)
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
			if(tMain.map[start][col] != 0)
				return true;//there's a pic
		return false;
	}

	public boolean checkMatched()
	{
		if(tMain.map[tMain.lasti][tMain.lastj] != -tMain.map[i][j])
			return false;

		//System.out.println("In checkMatched()...");

		//no turning point & on the same row
		tMain.cornerNum = 0;
		if(tMain.lasti == i && !rowHasPic(i, tMain.lastj, j))
			return true;
		//no turning point & on the same col
		else if(tMain.lastj == j && !colHasPic(j, tMain.lasti, i))
			return true;
		else
		{
			//one turning point
			tMain.cornerNum = 1;
			if(tMain.lasti!=i && tMain.lastj!=j)
			{
				if(tMain.map[tMain.lasti][j]==0 && !colHasPic(j, tMain.lasti, i) && !rowHasPic(tMain.lasti, j, tMain.lastj))//the turning point (lasti, j) is on the same line with (lasti, lastj)
				{
					tMain.cornerI = tMain.lasti;	tMain.cornerJ = j;
					return true;
				}
				else if(tMain.map[i][tMain.lastj]==0 && !rowHasPic(i, j, tMain.lastj) && !colHasPic(tMain.lastj, i, tMain.lasti))//the turning point (i, lastj) is on the same line with (i, j)
				{
					tMain.cornerI = i;	tMain.cornerJ = tMain.lastj;
					return true;
				}
			}
			
			//two turning points
			tMain.cornerNum = 2;
			//find leftward
			tMain.cornerI = tMain.lasti;	tMain.cornerII = i;
			for(tMain.cornerJ=tMain.lastj-1; tMain.cornerJ>=0; --tMain.cornerJ)
			{
				tMain.cornerJJ = tMain.cornerJ;
				if(tMain.map[tMain.cornerI][tMain.cornerJ] != 0)	break;
				if(tMain.map[tMain.cornerII][tMain.cornerJJ]!=0)	continue;
				if(!rowHasPic(i, j, tMain.cornerJJ) && !colHasPic(tMain.cornerJ, tMain.cornerI, tMain.cornerII))
					return true;
			}
			if(tMain.cornerJ==-1 && !rowHasPic(i, j, -1))
			{
				tMain.cornerJJ = -1;
				return true;
			}

			//find rightward
			for(tMain.cornerJ=tMain.lastj+1; tMain.cornerJ<tMain.N; ++tMain.cornerJ)
			{
				tMain.cornerJJ = tMain.cornerJ;
				if(tMain.map[tMain.cornerI][tMain.cornerJ] != 0)	break;
				if(tMain.map[tMain.cornerII][tMain.cornerJJ]!=0)	continue;
				if(!rowHasPic(i, j, tMain.cornerJJ) && !colHasPic(tMain.cornerJ, tMain.cornerI, tMain.cornerII))
					return true;
			}
			if(tMain.cornerJ==tMain.N && !rowHasPic(i, j, tMain.N))
			{
				tMain.cornerJJ = tMain.N;
				return true;
			}

			//find upward
			tMain.cornerJ = tMain.lastj;	tMain.cornerJJ = j;
			for(tMain.cornerI=tMain.lasti-1; tMain.cornerI>=0; --tMain.cornerI)
			{
				tMain.cornerII = tMain.cornerI;
				if(tMain.map[tMain.cornerI][tMain.cornerJ] != 0)	break;
				if(tMain.map[tMain.cornerII][tMain.cornerJJ]!=0)	continue;
				if(!rowHasPic(tMain.cornerI, tMain.cornerJ, tMain.cornerJJ) && !colHasPic(j, i, tMain.cornerII))
					return true;
			}
			if(tMain.cornerI == -1 && !colHasPic(j, i, -1))
			{
				tMain.cornerII = -1;
				return true;
			}

			//find downward
			for(tMain.cornerI = tMain.lasti + 1; tMain.cornerI<tMain.N; ++ tMain.cornerI)
			{
				tMain.cornerII = tMain.cornerI;
				if(tMain.map[tMain.cornerI][tMain.cornerJ] != 0)	break;
				if(tMain.map[tMain.cornerII][tMain.cornerJJ]!=0)	continue;
				if(!rowHasPic(tMain.cornerI, tMain.cornerJ, tMain.cornerJJ) && !colHasPic(j, i, tMain.cornerII))
					return true;
			}
			if(tMain.cornerI == tMain.N && !colHasPic(j, i, tMain.N))
			{
				tMain.cornerII = tMain.N;
				return true;
			}
		}
		return false;
	}

	public void actionPerformed(ActionEvent e)
	{
		tMain.lock.lock();
		if(tMain.lasti != i || tMain.lastj != j)
		{
			if(tMain.lasti == -1  && tMain.lastj == -1)//the first time to choose the button
			{
				tMain.lasti = i;
				tMain.lastj = j;
				setBackground(Color.yellow);
			}
			else if(tMain.map[i][j]!=0 && checkMatched())//the two button matches
			{
				if(tMain.online)//online game
				{
					String str = tMain.cornerNum + " "+tMain.lasti+" "+tMain.lastj;
					if(tMain.cornerNum>=1)	str+=" "+tMain.cornerI+" "+tMain.cornerJ;
					if(tMain.cornerNum==2)	str+=" "+tMain.cornerII+" "+tMain.cornerJJ;
					str+=" "+i+" "+j;
					//send to the server for check
					try
					{
						tMain.toServer.write(str.getBytes());
					}catch(IOException ex)
					{
						JOptionPane.showMessageDialog(tMain, "网络异常断开，关闭应用程序", "错误消息", JOptionPane.ERROR_MESSAGE);
						System.exit(0);
					}
				}
				else//stand-alone
				{
					/*
					System.out.print("["+tMain.lasti+", "+tMain.lastj+"]  ");
					if(tMain.cornerNum >0)	System.out.print("["+tMain.cornerI+", "+tMain.cornerJ+"]  ");
					if(tMain.cornerNum ==2)	System.out.print("["+tMain.cornerII+", "+tMain.cornerJJ+"]  ");
					System.out.println("["+i+", "+j+"]");*/

					tMain.grades += 2;
					tMain.lbSelfGrade.setText(tMain.grades + "");
					if(tMain.grades<2 && tMain.grades>=0)	tMain.lbSelfGrade.setForeground(Color.black);

					tMain.map[tMain.lasti][tMain.lastj] = 0;
					tMain.map[i][j] = 0;

					tMain.btnMap[tMain.lasti][tMain.lastj].setVisible(false);
					setVisible(false);

					tMain.lasti = -1;
					tMain.lastj = -1;
					--tMain.cnt;

					if(tMain.cnt == 0)
					{
						tMain.initBtnMap();//regenerate data
					}
				}
			}
			else
			{
				tMain.btnMap[tMain.lasti][tMain.lastj].setBackground(null);
				tMain.lasti = i;
				tMain.lastj = j;
				setBackground(Color.yellow);
			}
		}
		tMain.lock.unlock();
	}
}

