package cats.notepad.event;

public interface SearchListener {
	
	public void onSearch(final String search);
	public void onReplace(final String search, final String replace);
	public void onExit();

}
