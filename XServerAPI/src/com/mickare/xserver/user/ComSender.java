package com.mickare.xserver.user;

public interface ComSender
{

	public String getName();

	public boolean hasPermission(String perm);

	public void sendMessage(String message);

}
