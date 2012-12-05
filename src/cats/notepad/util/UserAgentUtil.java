package cats.notepad.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

public final class UserAgentUtil {
	
	public static final LinkedList<String> AGENTS = new LinkedList<String>();
	
	static{
		final Scanner reader = new Scanner(UserAgentUtil.class.getResourceAsStream("agents.txt"));
		while(reader.hasNextLine()){
			final String line = reader.nextLine().trim();
			if(line.startsWith("Mozilla/")) AGENTS.add(line);
		}
		reader.close();
	}
	
	public static String random(){
		Collections.shuffle(AGENTS);
		return AGENTS.getFirst();
	}

}
