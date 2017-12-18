package basketcasey.com.androidsecurenote.database;

import java.util.Comparator;
/*
 * Since the database is encrypted, you can't use SQL sorting
 * Need a custom comparator to use with the Collections.sort method
 */
public class CustomGroupComparator implements Comparator<Group>{
	public int compare(Group g1, Group g2) {
		return g1.getName().compareTo(g2.getName());
	}
}
