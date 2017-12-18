package basketcasey.com.androidsecurenote.database;

import java.util.Comparator;

/*
 * Since the database is encrypted, you can't use SQL sorting
 * Need a custom comparator to use with the Collections.sort method
 */

public class CustomNoteComparator implements Comparator<Note>{

	public int compare(Note n1, Note n2) {
		return n1.getTitle().compareTo(n2.getTitle());
	}
}
