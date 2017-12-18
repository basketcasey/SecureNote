package basketcasey.com.androidsecurenote.database;

public class Group {
	// Model that will hold note data to be retrieved/stored/displayed
	private long id;
	private String name;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	// Used for adapter to display in dropdown
	public String toString() {
		return name;
	}
}
