package com.mickare.xserver.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.mickare.xserver.Message;
import com.mickare.xserver.XServerManager;
import com.mickare.xserver.events.XServerMessageIncomingEvent;
import com.mickare.xserver.exceptions.NotInitializedException;

public class NetPacketHandler {

	public static void handle(Connection con, DataInputStream input) throws IOException {
		int packetID = input.readInt();
		int length = input.readInt();
		byte[] data = new byte[length];
		input.readFully(data);
		
		DataInputStream is = null;
		try {
		try {
			switch (packetID) {
			case 100: // Keep Alive

				break;
			case 200: // Disconnect
				XServerManager.getInstance().getLogger()
						.info("Disconnecting from " + con.getHost() + ":" + con.getPort());
				con.disconnect();
				break;
			case 400: // Error
				XServerManager.getInstance().getLogger()
						.info("Connection Error with " + con.getHost() + ":" + con.getPort());
				con.errorDisconnect();
				break;
			case 401: // LoginDenied
				XServerManager.getInstance().getLogger()
						.info("Login denied from " + con.getHost() + ":" + con.getPort());
				con.errorDisconnect();
				break;
			case 500: // LoginRequest
				
				try {
					is = new DataInputStream(new ByteArrayInputStream(data));
					String name = is.readUTF();
					String password = is.readUTF();
					XServer s = XServerManager.getInstance().getServer(name);
					
					// Debugging...
					/*
					XServerManager.getInstance().getLogger().info("Debugging!\n" + 
					name + " - " + password + "\n" +
					"Serverfound:" + String.valueOf(s != null) + "\n" +
					((s != null) ? s.getName() + " - " + s.getPassword() : ""));
					*/
					if(s != null && s.getPassword().equals(password)) {
						con.setXserver(s);
						con.setStatus(Connection.stats.connected);
						con.sendAcceptedLoginRequest();
						XServerManager.getInstance().getLogger()
						.info("Login Request from " + name + " accepted!");
					} else {
						con.send(new Packet(Packet.Types.LoginDenied, new byte[0]));
						XServerManager.getInstance().getLogger()
						.info("Login Request from " + name + " denied! (" + con.getHost() + ":" + con.getPort() + ")");
						con.errorDisconnect();
					}
				} finally {
					if(is != null) {
						is.close();
					}
				}
				break;
			case 501: // LoginAccepted
				
				try {
					is = new DataInputStream(new ByteArrayInputStream(data));
					String name = is.readUTF();
					String password = is.readUTF();
					XServer s = XServerManager.getInstance().getServer(name);
					
					// Debugging...
					/*
					XServerManager.getInstance().getLogger().info("Debugging!\n" + 
					name + " - " + password + "\n" +
					"Serverfound:" + String.valueOf(s != null) + "\n" +
					((s != null) ? s.getName() + " - " + s.getPassword() : ""));
					*/					
					if(s != null && s.getPassword().equals(password)) {
						con.setXserver(s);
						con.setStatus(Connection.stats.connected);
						XServerManager.getInstance().getLogger()
						.info("Login Reply accepted from " + s.getName());
					} else {
						con.send(new Packet(Packet.Types.LoginDenied, new byte[0]));
						XServerManager.getInstance().getLogger()
						.info("Login Reply from " + name + " denied! (" + con.getHost() + ":" + con.getPort() + ")");
						con.errorDisconnect();
					}
				} finally {
					if(is != null) {
						is.close();
					}
				}
				break;
			case 600: // PingRequest
				con.send(new Packet(Packet.Types.PingAnswer, data));
				break;
			case 601: // PingAnswer
				try {
					is = new DataInputStream(new ByteArrayInputStream(data));
					con.returningPing(is.readUTF());
				} finally {
					if(is != null) {
						is.close();
					}
				}
				break;
			case 800: // Message
				XServerManager.getInstance().getLogger().info("A");
				if(con.getXserver() != null && con.isConnected() && con.getStatus().equals(Connection.stats.connected)) {
					XServerManager.getInstance().getLogger().info("B");
					XServerManager.getInstance().getEventHandler().callEvent(new XServerMessageIncomingEvent(con.getXserver(), Message.read(con.getXserver(), data)));
				}
				break;
			default:
				con.disconnect();
				break;
			}
		} catch (InterruptedException e) {
			XServerManager.getInstance().getLogger().severe(e.getMessage());
			con.errorDisconnect();
		}
		}catch (NotInitializedException e) {
			con.errorDisconnect();
		}
	}

}
