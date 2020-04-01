package com.softwareag.iec104.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.softwareag.iec104.configuration.GatewayConfigurationProperties;

@Repository
public class SqliteRepository {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GatewayConfigurationProperties properties;

	private Connection connect() {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + properties.getBufferDbPath());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}

	public void createMeasurementTable() {
		String sql = "CREATE TABLE IF NOT EXISTS measurements(id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT NOT NULL, measurement TEXT NOT NULL, status TEXT DEFAULT 'PENDING');";

		Connection conn = this.connect();
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void createMqttTable() {
		String sql = "CREATE TABLE IF NOT EXISTS mqtt(id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT NOT NULL, topic TEXT NOT NULL, message TEXT NOT NULL, status TEXT DEFAULT 'PENDING');";

		Connection conn = this.connect();
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void createBufferConfigTable() {
		String sql = "CREATE TABLE IF NOT EXISTS bufferconfig(id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT NOT NULL, interval INTEGER NOT NULL, status TEXT DEFAULT 'PENDING');";

		Connection conn = this.connect();
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void insertMeasurement(String m) {
		String sql = "INSERT INTO measurements(measurement, timestamp) VALUES(?,?)";

		Connection conn = this.connect();
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, m);
			pstmt.setString(2, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(LocalDateTime.now()));
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void insertMqtt(String topic, String m) {
		String sql = "INSERT INTO mqtt(topic, message, timestamp) VALUES(?,?,?)";

		Connection conn = this.connect();
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, topic);
			pstmt.setString(2, m);
			pstmt.setString(3, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(LocalDateTime.now()));
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void setBufferInterval(int bufferInterval) {
		String put = "insert into bufferconfig(interval, timestamp) values(?,?)";

		Connection conn = this.connect();
		try {
			PreparedStatement pstmt = connect().prepareStatement(put);
			pstmt.setInt(1, bufferInterval);
			String timestamp = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(LocalDateTime.now());
			logger.info("Timestamp: {}", timestamp);
			pstmt.setString(2, timestamp);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Integer getBufferInterval() {
		Integer bufferInterval = null;

		String get = "select interval from bufferconfig";
		Connection conn = this.connect();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(get);
			
			if (rs.next()) {
				bufferInterval = rs.getInt("interval");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return bufferInterval;
	}
}
