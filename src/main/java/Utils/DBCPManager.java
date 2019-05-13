package Utils;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.DataSource;

public class DBCPManager {
	private DataSource ds=new DataSource();
	public DBCPManager() {
		initiate();
	}
	private void initiate() {
		String url = "jdbc:mysql://localhost:3306/users";
		ds.setUrl(url);
		String user = "root";
		ds.setUsername(user);
		String passWord = "010233";
		ds.setPassword(passWord);
		String driver = "com.mysql.cj.jdbc.Driver";
		ds.setDriverClassName(driver);
		int initialize = 6;
		ds.setInitialSize(initialize);
		int max = 6;
		ds.setMaxIdle(max);
		int maxWaitTime = 5000;
		ds.setMaxWait(maxWaitTime);
	    ds.setMaxActive(max);
	    ds.setTestWhileIdle(true);
		int minIdle = 6;
		ds.setMinIdle(minIdle);
		int testTime = 1000 * 60 * 60 * 4;
		ds.setMinEvictableIdleTimeMillis(testTime);
	    ds.setTimeBetweenEvictionRunsMillis(testTime /2);
	    try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Connection getConn() {
		Connection conn;
		try {
			conn = ds.getConnection();
			return conn;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("DCPManeger::getConn:ERROR:no conn");
			return null;
		}
	}
}
