package basketcasey.com.androidsecurenote.database;

public class Note {
	// Model that will hold note data to be retrieved/stored/displayed
	private long id;
	private long group_id;
	private int priority;
	private String title;
	private String description;
	
	public long getId() {
		return this.id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getGroupId() {
		return this.group_id;
	}
	
	public void setGroupId(long groupId) {
		this.group_id = groupId;
	}
	
	public int getPriority() {
		return this.priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	// Used to diplay in the listview via the adapter
	public String toString() {
		return this.title;
	}
}
