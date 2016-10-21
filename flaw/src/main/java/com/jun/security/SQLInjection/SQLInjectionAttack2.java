package com.jun.security.SQLInjection;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

/**
 * https://www.owasp.org/index.php/SQL_injection - Example 3
 * 
 * The following Java code dynamically constructs and executes a SQL query that searches for employee by matching a specified name and role.
 * The query restricts the employee displayed to those where name and role matches the user name and role of the currently-authenticated user.
 * 
 * column of table include: empid, name, salary, role
 * @author Jeff Yang
 *
 */
public class SQLInjectionAttack2 {
	
	String dbDriver;
	String dbHost;
	String dbPort;
	String dbInstance;
	String dbConnUsername;
	String dbConnPassword;
	

	public void getOracleConnection() {
		try {

			Properties pro = new Properties();
			InputStream inptStream = this.getClass().getClassLoader()
					.getResourceAsStream("com/jun/security/SQLInjection/db.properties");
			pro.load(inptStream);

			dbDriver = pro.getProperty("oracleDb_Driver");
			dbHost = pro.getProperty("oracleDb_Host");
			dbPort = pro.getProperty("oracleDb_Port");
			dbInstance = pro.getProperty("oracleDb_Instance");
			dbConnUsername = pro.getProperty("oracleDb_UserName");
			dbConnPassword = pro.getProperty("oracleDb_Password");

			inptStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("FAIL: Error when getting db properity.\n");
		}
	}

	/**
	 * Establishing a connection 建立连接
	 */
	public Connection establishConnection() {

		Connection conn = null;

		try {
			this.getOracleConnection();
			Class.forName(dbDriver);

			String url = "jdbc:oracle:thin:@" + dbHost + ":" + dbPort + ":" + dbInstance + "";
			conn = DriverManager.getConnection(url, dbConnUsername, dbConnPassword);

		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			System.out.println("FAIL: Class not found when load oracle driver.\n");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(
					"FAIL: Error when getting database connection.\n" + e.getErrorCode() + " : " + e.getMessage());
		}
		return conn;
	}

	/**
	 * select data 查询数据
	 */
	public void selectData() throws Exception {
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		SQLInjectionAttack1 myCreateStmt = new SQLInjectionAttack1();
		conn = myCreateStmt.establishConnection();

		boolean defaultCommit = conn.getAutoCommit();
		conn.setAutoCommit(false);

		try {
			
			/*这条查询语句应该是：SELECT * FROM employee where owner= 'Anneta' and role ='HR Manager';
			 只有userName是Anneta,并且role是HR Manager情况下，才有权限查询到所有员工的信息。
			
			然而，现在在roleName 输入"name'); SELECT * FROM items WHERE 'a'='a"，然后查询语句就变成
			SELECT * FROM items WHERE owner = 'Anneta'AND role = 'name' OR 'a'='a';
			*/
			String getAuthenticatedUserName = "Anneta";
			String getAuthenticatedRole = "name'; SELECT * FROM employee WHERE 'a'='a";
			String userName = getAuthenticatedUserName;
			String roleName = getAuthenticatedRole;
			String sql2 = "select * from employee where name="+"'"+userName+"'"+"AND role="+"'"+roleName+"'";

			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql2);
			while (rs.next()) {
				System.out.println(rs.getInt("EMPID") + rs.getString("NAME") + rs.getString("SALARY") + rs.getString("ROLE"));
			}

			conn.commit();
			System.out.println("查询成功");
		} catch (Exception ex) {
			conn.rollback();
			throw ex;
		}

		finally {
			conn.setAutoCommit(defaultCommit);
			myCreateStmt.colseConnection(conn, stmt, rs);
		}

	}


	/**
	 * close the connection 释放资源
	 */
	public void colseConnection(Connection conn, Statement stmt, ResultSet rs) {

		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			rs = null;
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws Exception { 
		
		SQLInjectionAttack1 mystmt = new SQLInjectionAttack1();
		mystmt.selectData();
		
	}

}
