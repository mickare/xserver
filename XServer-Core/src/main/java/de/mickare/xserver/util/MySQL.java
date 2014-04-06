package de.mickare.xserver.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQL {

	private final Logger logger;

	private final String user, pass, db, host, name;
	private final int port;
	private Connection connection;

	public MySQL(Logger logger, String user, String pass, String db, String host, int port, String name) {
		this.logger = logger;
		this.user = user;
		this.pass = pass;
		this.db = db;
		this.host = host;
		this.port = port;
		this.name = name;
	}

	public void reconnect() {
		try {
			if (connection != null) {
				if (connection.isClosed()) {
					connect();
				}
			} else {
				connect();
			}
		} catch (SQLException ex) {
			logger.log( Level.SEVERE, ex.getMessage() );
		}
	}

	public void connect() {
		try {
			Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
			connection = DriverManager.getConnection( "jdbc:mysql://" + host + ":" + port + "/" + db + "?autoReconnect=true", user, pass );
		} catch (Exception ex) {
			logger.log( Level.SEVERE, ex.getMessage() );
			// Bukkit.getLogger().severe(java.util.Arrays.toString(ex.getStackTrace()));
		}
	}

	public void disconnect() {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception ex) {
				logger.log( Level.SEVERE, ex.getMessage() );
				// Bukkit.getLogger().severe(java.util.Arrays.toString(ex.getStackTrace()));
			}
		}
	}

	public void updateSilent( String qry ) {
		try {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate( qry );
			stmt.close();
		} catch (Exception ex) {
		}
	}

	public void update( String qry ) {
		try {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate( qry );
			stmt.close();
		} catch (Exception ex) {
			logger.log( Level.SEVERE, ex.getMessage() );
			// Bukkit.getLogger().severe(qry);
			// Bukkit.getLogger().severe(java.util.Arrays.toString(ex.getStackTrace()));
		}
	}

	public void update( String stmtqry, String... values ) {
		try {
			PreparedStatement stmt = connection.prepareStatement( stmtqry );
			for (int i = 0; i < values.length; i++) {
				stmt.setString( i + 1, values[i] );
			}
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception ex) {
			logger.log( Level.SEVERE, ex.getMessage() );
			// Bukkit.getLogger().severe(stmtqry);
			// Bukkit.getLogger().severe(java.util.Arrays.toString(ex.getStackTrace()));
		}
	}

	public ResultSet query( String qry ) {
		ResultSet rs = null;
		try {
			PreparedStatement pstmt = connection.prepareStatement( qry );
			rs = pstmt.executeQuery( qry );
		} catch (Exception ex) {
			logger.log( Level.SEVERE, ex.getMessage() );
			// Bukkit.getLogger().severe(qry);
			// Bukkit.getLogger().severe(java.util.Arrays.toString(ex.getStackTrace()));
		}
		return rs;
	}

	public ResultSet query( String qry, String... values ) {
		ResultSet rs = null;
		try {
			PreparedStatement pstmt = connection.prepareStatement( qry );
			for (int i = 0; i < values.length; i++) {
				pstmt.setString( i + 1, values[i] );
			}
			rs = pstmt.executeQuery( qry );
		} catch (Exception ex) {
			logger.log( Level.SEVERE, ex.getMessage() );
			// Bukkit.getLogger().severe(qry);
			// Bukkit.getLogger().severe(java.util.Arrays.toString(ex.getStackTrace()));
		}
		return rs;
	}

	public Connection getConnection() {
		return connection;
	}

	public String getName() {
		return name;
	}

}