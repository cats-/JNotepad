package cats.notepad.run;

import cats.notepad.JNotepad;

public class JNotepadRunner {
	
	public static void main(String args[]){
		JNotepad window = new JNotepad();
		window.setSize(500, 500);
		window.setVisible(true);
		window.setLocationRelativeTo(null);
	}

}
