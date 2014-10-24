import java.sql.*;
import java.io.*;

class Access
{
	String url;
	File directory;
	Connection conn;
	Statement stmt;
	public Access()
	{
		directory = new File("data.mdb");
		url = "jdbc:odbc:driver={Microsoft Access Driver (*.mdb, *.accdb)}; DBQ="+directory.getAbsolutePath();

		try
		{
			Connection con = DriverManager.getConnection(url);
			conn = con;
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		}catch(Exception e)
		{
			System.out.println("Database Init Error");
		}
	}

	public String getItem(int num)
	{
		String ret="";
		ResultSet rs;
		try
		{
			stmt = conn.createStatement();
			if(num < 0)
				rs = stmt.executeQuery("select English from data where ID="+(-num));
			else
				rs = stmt.executeQuery("select Chinese from data where ID="+num);
			rs.next();
			ret = rs.getString(1);
			rs.close();
			stmt.close();
		}
		catch(Exception e)
		{
			System.out.println("Query Error");
		}
		finally
		{
			return ret;
		}
	}

	public void closeAccess()
	{
		try
		{
			conn.close();
		}catch(Exception e)
		{
			System.out.println("Database Close Error");
		}
	}

	public int getTotal()
	{
		int ret = 0;
		ResultSet rs;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select count(ID) from data");
			rs.next();
			ret = rs.getInt(1);
			rs.close();
			stmt.close();
		}
		catch(Exception e)
		{
			System.out.println("Get Total Error");
		}
		finally
		{
			return ret;
		}
	}

	public static void main(String[] args)
	{
		Access db = new Access();
		System.out.println(db.getTotal());
	}
}
