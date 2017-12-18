package basketcasey.com.androidsecurenote.database;

public class Configuration {
	private long id;
	private String salt;
	private String testvalue;
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getId() {
		return this.id;
	}
	
	public void setTestValue(String test) {
		this.testvalue = test;
	}
	
	public String getTestValue() {
		return this.testvalue;
	}
	
	public void setSalt(String salt){
		this.salt = salt;
	}
	
	public String getSalt() {
		return this.salt;
	}
}
