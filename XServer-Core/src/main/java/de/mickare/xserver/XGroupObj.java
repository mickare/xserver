package de.mickare.xserver;

public class XGroupObj implements XGroup {

	private final int id;
	private final String name;

	protected XGroupObj(int id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals( Object o ) {
		return (!(o instanceof XGroup)) ? false : ((XGroup) o).getID() == this.id;
	}

}
