package de.mickare.xserver.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.mickare.xserver.XServerPlugin;

public class TableInstall {

	private static final String sql_createServers = "CREATE TABLE IF NOT EXISTS `{table_servers}` ("
			+ " `ID` int(10) unsigned NOT NULL AUTO_INCREMENT," + " `NAME` varchar(64) COLLATE utf8_bin NOT NULL,"
			+ " `ADRESS` varchar(128) COLLATE utf8_bin NOT NULL," + " `PW` varchar(20) COLLATE utf8_bin NOT NULL," + " PRIMARY KEY (`ID`),"
			+ " UNIQUE KEY `ID` (`ID`)," + " UNIQUE KEY `ADRESS` (`ADRESS`)," + " KEY `ID_2` (`ID`)"
			+ " ) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;";

	private static final String sql_createGroups = "CREATE TABLE IF NOT EXISTS `{table_groups}` ("
			+ " `groupID` int(10) unsigned NOT NULL AUTO_INCREMENT, `name` varchar(32) NOT NULL, PRIMARY KEY (`groupID`),"
			+ "UNIQUE KEY `name` (`name`) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	private static final String sql_createServersGroups = "CREATE TABLE IF NOT EXISTS `{table_relations}` ("
			+ " `serverID` int(10) unsigned NOT NULL," + " `groupID` int(10) unsigned NOT NULL," + " PRIMARY KEY (`serverID`,`groupID`),"
			+ " KEY `groupID` (`groupID`)" + " ) ENGINE=InnoDB DEFAULT CHARSET=latin1;";

	private static final String sql_alterServersGroups = "ALTER TABLE `{table_relations}`"
			+ " ADD CONSTRAINT `xservers_xgroups_ibfk_1` FOREIGN KEY (`groupID`) REFERENCES `{table_groups}` (`groupID`) ON DELETE CASCADE ON UPDATE CASCADE,"
			+ " ADD CONSTRAINT `xservers_xgroups_ibfk_2` FOREIGN KEY (`serverID`) REFERENCES `{table_servers}` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE;";

	private final XServerPlugin plugin;
	private final MySQL connection;

	private final String sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups;

	public TableInstall(XServerPlugin plugin, MySQL connection, String sql_table_xservers, String sql_table_xgroups,
			String sql_table_xserversxgroups) {
		this.plugin = plugin;
		this.connection = connection;
		this.sql_table_xservers = sql_table_xservers;
		this.sql_table_xgroups = sql_table_xgroups;
		this.sql_table_xserversxgroups = sql_table_xserversxgroups;
	}

	private void info( String msg ) {
		this.plugin.getLogger().info( msg );
	}

	private void log( Throwable e ) {
		this.plugin.getLogger().info( e.getMessage() + "\n" + MyStringUtils.stackTraceToString( e ) );
	}

	public void install() {

		connection.reconnect();

		boolean existing_servers = false, existing_groups = false, existing_relation = false;

		try (Statement stmt = connection.getConnection().createStatement()) {
			ResultSet rs = stmt.executeQuery( "SHOW TABLES" );
			while (rs.next()) {
				String name = rs.getString( 1 );
				if (name.equals( this.sql_table_xservers )) {
					info( "Table \"" + this.sql_table_xservers + "\" found" );
					existing_servers = true;
				} else if (name.equals( this.sql_table_xgroups )) {
					info( "Table \"" + this.sql_table_xgroups + "\" found" );
					existing_groups = true;
				} else if (name.equals( this.sql_table_xserversxgroups )) {
					info( "Table \"" + this.sql_table_xserversxgroups + "\" found" );
					existing_relation = true;
				}
			}
		} catch (SQLException e) {
			log( e );
		}

		if (!existing_servers) {
			info( "Table " + sql_table_xservers + " is missing and automatically creating it." );
			try (Statement stmt = connection.getConnection().createStatement()) {
				stmt.executeUpdate( sql_createServers.replace( "{table_servers}", sql_table_xservers ) );
			} catch (SQLException e) {
				log( e );
			}
		}

		if (!existing_groups) {
			info( "Table " + sql_table_xgroups + " is missing and automatically creating it." );
			try (Statement stmt = connection.getConnection().createStatement()) {
				stmt.executeUpdate( sql_createGroups.replace( "{table_groups}", sql_table_xgroups ) );
			} catch (SQLException e) {
				log( e );
			}
		}

		if (!existing_relation) {
			info( "Table " + sql_table_xserversxgroups + " is missing and automatically creating it." );
			try (Statement stmt = connection.getConnection().createStatement()) {
				stmt.executeUpdate( sql_createServersGroups.replace( "{table_relations}", sql_table_xserversxgroups ) );
			} catch (SQLException e) {
				log( e );
			}

			info( "Table " + sql_table_xserversxgroups + " was missing and adding relations." );
			try (Statement stmt = connection.getConnection().createStatement()) {
				stmt.executeUpdate( sql_alterServersGroups.replace( "{table_relations}", sql_table_xserversxgroups )
						.replace( "{table_servers}", sql_table_xservers ).replace( "{table_groups}", sql_table_xgroups ) );
			} catch (SQLException e) {
				log( e );
			}
		}

	}
}
