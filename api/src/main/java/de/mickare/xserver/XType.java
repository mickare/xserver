package de.mickare.xserver;

public enum XType
{
	Other(0, "Other"), Bukkit(1, "Bukkit"), BungeeCord(2, "BungeeCord");

	private final int number;
	private final String name;

	private XType(int number, String name)
	{
		this.number = number;
		this.name = name;
	}

	public static XType getByNumber(int number)
	{
		switch (number)
		{
		case 1:
			return Bukkit;
		case 2:
			return BungeeCord;
		default:
			return Other;
		}
	}

	public int getNumber()
	{
		return number;
	}

	public String getName()
	{
		return name;
	}

}
