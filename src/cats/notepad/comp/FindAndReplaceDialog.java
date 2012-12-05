package cats.notepad.comp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cats.notepad.JNotepad;
import cats.notepad.event.SearchListener;
import cats.notepad.util.IconUtil;

public class FindAndReplaceDialog extends JDialog implements ActionListener{
	
	private JMenuBar menuBar;
	
	private JNotepad parent;
	
	private JMenuItem colorItem;
	
	private SearchListener listener;
	
	private JLabel findLabel;
	private JTextField findBox;
	private JButton findButton;
	private JPanel findPanel;
	
	private JPanel replacePanel;
	private JButton replaceButton;
	private JLabel replaceLabel;
	private JTextField replaceBox;
	
	private JPanel buttonPanel;
	private JPanel replaceFindPanel;
	
	private JPanel northPanel;
	
	private JLabel occurrenceLabel;
	private int occur;
		
	public FindAndReplaceDialog(final JNotepad parent){
		super(parent, "Find & Replace - ", false);
		this.parent = parent;
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						if(listener != null) listener.onExit();
					}
					public void windowClosed(WindowEvent e){
						if(listener != null) listener.onExit();
					}
				}
		);
		
		menuBar = new JMenuBar();
		
		colorItem = new JMenuItem("Highlight Color");
		colorItem.setIcon(IconUtil.create(parent.getCurrentHighlightColor()));
		colorItem.addActionListener(this);
		
		menuBar.add(colorItem);
		
		setJMenuBar(menuBar);
		
		occur = 0;
		
		findLabel = new JLabel("Search For:");
		findLabel.setHorizontalAlignment(JLabel.CENTER);
		
		findBox = new JTextField(15);
		findBox.setHorizontalAlignment(JLabel.CENTER);
		
		findPanel = new JPanel(new GridLayout(1, 2));
		findPanel.add(findLabel);
		findPanel.add(findBox);
		
		findButton = new JButton("Search");
		findButton.addActionListener(this);
		
		replaceLabel = new JLabel("Replace With:");
		replaceLabel.setHorizontalAlignment(JLabel.CENTER);
		
		replaceBox = new JTextField(15);
		replaceBox.setHorizontalAlignment(JLabel.CENTER);
		
		replacePanel = new JPanel(new GridLayout(1, 2));
		replacePanel.add(replaceLabel);
		replacePanel.add(replaceBox);
		
		replaceButton = new JButton("Replace");
		replaceButton.addActionListener(this);
		
		replaceFindPanel = new JPanel(new BorderLayout());
		replaceFindPanel.add(findPanel, BorderLayout.NORTH);
		replaceFindPanel.add(replacePanel, BorderLayout.SOUTH);
		
		buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(findButton);
		buttonPanel.add(replaceButton);
		
		northPanel = new JPanel(new BorderLayout());
		northPanel.add(replaceFindPanel, BorderLayout.NORTH);
		northPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		occurrenceLabel = new JLabel("Occurrences: 0");
		occurrenceLabel.setHorizontalAlignment(JLabel.CENTER);
		
		add(northPanel, BorderLayout.NORTH);
		add(occurrenceLabel, BorderLayout.SOUTH);
	}
	
	public void setTitle(final String name){
		super.setTitle("Find & Replace - " + name);
	}
	
	public void actionPerformed(ActionEvent e){
		final Object source = e.getSource();
		if(source.equals(findButton)){
			if(listener != null && findBox.getText().length() > 0) listener.onSearch(findBox.getText());
		}else if(source.equals(replaceButton)){
			if(occur < 1) return;
			if(listener != null) listener.onReplace(findBox.getText(), replaceBox.getText());
		}else if(source.equals(colorItem)){
			final Color c = JColorChooser.showDialog(parent, "Select Highlighter Color", parent.getCurrentHighlightColor());
			if(c != null){
				parent.setHighlightColor(c);
				colorItem.setIcon(IconUtil.create(c));
			}
		}
	}
	
	public String getSearch(){
		return findBox.getText();
	}
	
	public void setOccurrences(final int n){
		occurrenceLabel.setText("Occurrences: " + (occur = n));
		occurrenceLabel.repaint();
	}
	
	public void reset(){
		findBox.setText("");
		replaceBox.setText("");
		setOccurrences(0);
	}
	
	public void setSearchListener(final SearchListener listener){
		this.listener = listener;
	}
	
	public SearchListener getSearchListener(){
		return listener;
	}
	
}
