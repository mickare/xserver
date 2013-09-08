package com.mickare.xserver.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import com.mickare.xserver.Message;
import com.mickare.xserver.XServerManager;
import com.mickare.xserver.XType;
import com.mickare.xserver.events.XServerConnectionDenied;
import com.mickare.xserver.events.XServerLoggedInEvent;
import com.mickare.xserver.events.XServerMessageIncomingEvent;
import com.mickare.xserver.exceptions.NotInitializedException;

public class NetPacketHandler extends Thread
{

	private final static int CAPACITY = 512;

	private final Connection con;

	private final ArrayBlockingQueue<Packet> pendingReceivingPackets = new ArrayBlockingQueue<Packet>(CAPACITY, true);

	public NetPacketHandler(Connection con)
	{
		this.con = con;
	}

	@Override
	public void run()
	{
		try
		{
			while (!isInterrupted() && con.isConnected())
			{
				doHandle(pendingReceivingPackets.take());
			}
		} catch (IOException | InterruptedException e)
		{
			con.errorDisconnect();
		}
		this.interrupt();
	}

	public void handle(Packet p)
	{
		pendingReceivingPackets.offer(p);
	}

	private void doHandle(Packet p) throws IOException
	{
		DataInputStream is = null;

		try
		{
			XServerManager.getInstance().getLogger().info("Packet: " + p.getPacketID() + " - L" + p.getData().length);
		} catch (NotInitializedException e2)
		{

		}

		try
		{

			if (p.getPacketID() == PacketType.KeepAlive.packetID) // Keep Alive
			{

			} else if (p.getPacketID() == PacketType.Disconnect.packetID) // Disconnect
			{
				XServerManager.getInstance().getLogger().info("Disconnecting from " + con.getHost() + ":" + con.getPort());
				con.disconnect();

			} else if (p.getPacketID() == PacketType.Error.packetID) // Error
			{
				XServerManager.getInstance().getLogger().info("Connection Error with " + con.getHost() + ":" + con.getPort());
				con.errorDisconnect();

			} else if (p.getPacketID() == PacketType.LoginDenied.packetID) // LoginDenied
			{
				XServerManager.getInstance().getLogger().info("Login denied from " + con.getHost() + ":" + con.getPort());
				con.errorDisconnect();

			} else if (p.getPacketID() == PacketType.LoginRequest.packetID) // LoginRequest
			{
				is = new DataInputStream(new ByteArrayInputStream(p.getData()));
				String name = is.readUTF();
				String password = is.readUTF();
				XType xtype = XType.getByNumber(is.readInt());
				XServer s = XServerManager.getInstance().getServer(name);

				// Debugging...
				/*
				 * XServerManager.getInstance().getLogger().info ("Debugging!\n"
				 * + name + " - " + password + "\n" + "Serverfound:" +
				 * String.valueOf(s != null) + "\n" + ((s != null) ? s.getName()
				 * + " - " + s.getPassword() : ""));
				 */
				if (s != null && s.getPassword().equals(password))
				{
					s.setType(xtype);
					sendAcceptedLoginRequest();
					con.setReloginXserver(s);
					con.setStatus(Connection.stats.connected);

					s.getManager().getLogger().info("Login Request from " + name + " accepted!");
					s.flushCache();
					s.getManager().getEventHandler().callEvent(new XServerLoggedInEvent(con.getXserver()));
				} else
				{
					con.send(new Packet(PacketType.LoginDenied, new byte[0]));
					XServerManager.getInstance().getLogger()
							.info("Login Request from " + name + " denied! (" + con.getHost() + ":" + con.getPort() + ")");
					con.errorDisconnect();
					XServerManager.getInstance().getEventHandler()
							.callEvent(new XServerConnectionDenied(name, password, con.getHost(), con.getPort()));
				}

			} else if (p.getPacketID() == PacketType.LoginAccepted.packetID) // LoginAccepted
			{
				is = new DataInputStream(new ByteArrayInputStream(p.getData()));
				String name = is.readUTF();
				String password = is.readUTF();
				XType xtype = XType.getByNumber(is.readInt());
				XServer s = XServerManager.getInstance().getServer(name);

				// Debugging...

				/*
				 * XServerManager .getInstance() .getLogger()
				 * .info("Debugging!\n" + name + " - " + password + "\n" +
				 * "Serverfound:" + String.valueOf(s != null) + "\n" + ((s !=
				 * null) ? s.getName() + " - " + s.getPassword() : ""));
				 */

				if (s != null && s.getPassword().equals(password))
				{
					s.setType(xtype);
					con.setXserver(s);
					con.setStatus(Connection.stats.connected);
					s.getManager().getLogger().info("Login Reply accepted from " + s.getName());
					s.flushCache();
					s.getManager().getEventHandler().callEvent(new XServerLoggedInEvent(s));
				} else
				{
					con.send(new Packet(PacketType.LoginDenied, new byte[0]));
					XServerManager.getInstance().getLogger()
							.info("Login Reply from " + name + " denied! (" + con.getHost() + ":" + con.getPort() + ")");
					con.errorDisconnect();
					XServerManager.getInstance().getEventHandler()
							.callEvent(new XServerConnectionDenied(name, password, con.getHost(), con.getPort()));
				}

			} else if (p.getPacketID() == PacketType.PingRequest.packetID) // PingRequest
			{
				con.send(new Packet(PacketType.PingAnswer, p.getData()));

			} else if (p.getPacketID() == PacketType.PingAnswer.packetID) // PingAnswer
			{
				is = new DataInputStream(new ByteArrayInputStream(p.getData()));
				Ping.receive(is.readUTF(), con.getXserver());

			} else if (p.getPacketID() == PacketType.Message.packetID) // Message
			{
				// XServerManager.getInstance().getThreadPool().runTask(new
				// Runnable() {
				// public void run() {

				try
				{
					if (con.getXserver() != null && con.isConnected() && con.isLoggedIn())
					{
						XServerManager.getInstance().getEventHandler()
								.callEvent(new XServerMessageIncomingEvent(con.getXserver(), Message.read(con.getXserver(), p.getData())));
					}
				} catch (NotInitializedException e)
				{
					con.errorDisconnect();
				} catch (IOException e)
				{

				}
				// }
				// });

			} else
			{
				con.errorDisconnect();

			}
		} catch (InterruptedException | IOException | NotInitializedException e)
		{
			try
			{
				XServerManager.getInstance().getLogger().severe(e.getMessage());
			} catch (NotInitializedException e1)
			{
			}
			con.errorDisconnect();
		} finally
		{
			if (is != null)
			{
				is.close();
			}
		}

	}

	protected void sendFirstLoginRequest() throws IOException, InterruptedException, NotInitializedException
	{
		sendLoginRequest(PacketType.LoginRequest);
	}

	protected void sendAcceptedLoginRequest() throws IOException, InterruptedException, NotInitializedException
	{
		sendLoginRequest(PacketType.LoginAccepted);
	}

	private void sendLoginRequest(PacketType type) throws IOException, InterruptedException, NotInitializedException
	{
		ByteArrayOutputStream b = null;
		DataOutputStream out = null;
		try
		{
			b = new ByteArrayOutputStream();
			out = new DataOutputStream(b);
			out.writeUTF(XServerManager.getInstance().getHomeServer().getName());
			out.writeUTF(XServerManager.getInstance().getHomeServer().getPassword());
			out.writeInt(XServerManager.HOMETYPE.getNumber());
			con.send(new Packet(type, b.toByteArray()));
		} finally
		{
			if (out != null)
			{
				out.close();
			}
		}
	}

}
