package com.mickare.xserver.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.mickare.xserver.AbstractXServerManager;
import com.mickare.xserver.Message;
import com.mickare.xserver.events.XServerMessageIncomingEvent;
import com.mickare.xserver.exceptions.NotInitializedException;

public class NetPacketHandler {

	public static void handle(final Connection con, DataInputStream input) throws IOException {
		final int packetID = input.readInt();
		final int length = input.readInt();
		final byte[] data = new byte[length];
		input.readFully(data);

		DataInputStream is = null;

		try {
			switch (packetID) {
			case 100: // Keep Alive

				break;
			case 200: // Disconnect
				AbstractXServerManager.getInstance().getLogger().info("Disconnecting from " + con.getHost() + ":" + con.getPort());
				con.disconnect();
				break;
			case 400: // Error
				AbstractXServerManager.getInstance().getLogger().info("Connection Error with " + con.getHost() + ":" + con.getPort());
				con.errorDisconnect();
				break;
			case 401: // LoginDenied
				AbstractXServerManager.getInstance().getLogger().info("Login denied from " + con.getHost() + ":" + con.getPort());
				con.errorDisconnect();
				break;
			case 500: // LoginRequest
				try {
					is = new DataInputStream(new ByteArrayInputStream(data));
					String name = is.readUTF();
					String password = is.readUTF();
					XServer s = AbstractXServerManager.getInstance().getServer(name);

					// Debugging...
					/*
					 * XServerManager.getInstance().getLogger().info
					 * ("Debugging!\n" + name + " - " + password + "\n" +
					 * "Serverfound:" + String.valueOf(s != null) + "\n" + ((s
					 * != null) ? s.getName() + " - " + s.getPassword() : ""));
					 */
					if (s != null && s.getPassword().equals(password)) {
						con.setReloginXserver(s);
						con.setStatus(Connection.stats.connected);
						con.sendAcceptedLoginRequest();
						AbstractXServerManager.getInstance().getLogger().info("Login Request from " + name + " accepted!");
					} else {
						con.send(new Packet(Packet.Types.LoginDenied, new byte[0]));
						AbstractXServerManager.getInstance().getLogger()
								.info("Login Request from " + name + " denied! (" + con.getHost() + ":" + con.getPort() + ")");
						con.errorDisconnect();
					}
				} finally {
					if (is != null) {
						is.close();
					}
				}
				break;
			case 501: // LoginAccepted
				try {
					is = new DataInputStream(new ByteArrayInputStream(data));
					String name = is.readUTF();
					String password = is.readUTF();
					XServer s = AbstractXServerManager.getInstance().getServer(name);

					// Debugging...
					/*
					 * XServerManager.getInstance().getLogger().info
					 * ("Debugging!\n" + name + " - " + password + "\n" +
					 * "Serverfound:" + String.valueOf(s != null) + "\n" + ((s
					 * != null) ? s.getName() + " - " + s.getPassword() : ""));
					 */
					if (s != null && s.getPassword().equals(password)) {
						con.setXserver(s);
						con.setStatus(Connection.stats.connected);
						AbstractXServerManager.getInstance().getLogger().info("Login Reply accepted from " + s.getName());
					} else {
						con.send(new Packet(Packet.Types.LoginDenied, new byte[0]));
						AbstractXServerManager.getInstance().getLogger()
								.info("Login Reply from " + name + " denied! (" + con.getHost() + ":" + con.getPort() + ")");
						con.errorDisconnect();
					}
				} finally {
					if (is != null) {
						is.close();
					}
				}
				break;
			case 600: // PingRequest
				con.send(new Packet(Packet.Types.PingAnswer, data));
				break;
			case 601: // PingAnswer
				is = new DataInputStream(new ByteArrayInputStream(data));
				AbstractPing.receive(is.readUTF(), con.getXserver());
				break;
			case 800: // Message
				//XServerManager.getInstance().getThreadPool().runTask(new Runnable() {
				//	public void run() {
						try {
							if (con.getXserver() != null && con.isConnected() && con.getStatus().equals(Connection.stats.connected)) {
								AbstractXServerManager.getInstance().getEventHandler()
										.callEvent(new XServerMessageIncomingEvent(con.getXserver(), Message.read(con.getXserver(), data)));
							}
						} catch (NotInitializedException e) {
							con.errorDisconnect();
						} catch (IOException e) {

						}
				//	}
				//});
				break;
			default:
				con.disconnect();
				break;
			}
		} catch (InterruptedException | IOException | NotInitializedException e) {
			try {
				AbstractXServerManager.getInstance().getLogger().severe(e.getMessage());
			} catch (NotInitializedException e1) {
			}
			con.errorDisconnect();
		}

	}
}
