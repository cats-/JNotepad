package cats.notepad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import cats.notepad.comp.FindAndReplaceDialog;
import cats.notepad.comp.JFontChooser;
import cats.notepad.event.SearchListener;
import cats.notepad.util.IconUtil;
import cats.notepad.util.UserAgentUtil;

public class JNotepad extends JFrame{

	private class CloseableTab extends JPanel{

		private JLabel label;

		private JLabel xLabel;

		private MouseAdapter mouseAdapter;
		
		private CloseableTab(final int index, final TextArea comp){
			this(index, comp, null);
		}

		private CloseableTab(final int index, final TextArea comp, final String tooltip){
			super(new BorderLayout());
			setOpaque(false);
			if(tooltip != null) setToolTipText(tooltip);

			mouseAdapter = new MouseAdapter(){
				public void mouseEntered(MouseEvent e){
					setXVisibility(true);
				}
				public void mouseExited(MouseEvent e){
					if(!JNotepad.this.tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()).equals(comp))
						setXVisibility(false);
					
				}
				public void mousePressed(MouseEvent e){
					JNotepad.this.tabbedPane.setSelectedComponent(comp);
					if(xLabel.isVisible() && e.getComponent().equals(xLabel)){
						int value = SAVE_SUCCESS;
						if(comp.shouldSave()){ 
							if((value = saveCurrent()) == SAVE_SUCCESS)
								JOptionPane.showMessageDialog(null, "Successfully saved", "Success", JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane.showMessageDialog(null, "Did not save " + tabbedPane.getTitleAt(tabbedPane.indexOfComponent(comp)), "Failed", JOptionPane.ERROR_MESSAGE);
						}
						if(value != SAVE_CANCEL){
							tabbedPane.remove(comp);
							if(tabbedPane.getTabCount() == 0) addNew();
						}
						final CloseableTab tab = (CloseableTab)tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
						tab.setXVisibility(true);
						for(int i = 0; i < tabbedPane.getTabCount(); i++){
							final CloseableTab t = (CloseableTab)tabbedPane.getTabComponentAt(i);
							if(tab != t) t.setXVisibility(false);
						}
					}
				}
			};

			addMouseListener(mouseAdapter);

			label = new JLabel(JNotepad.this.tabbedPane.getTitleAt(index));
			label.setIcon(JNotepad.this.tabbedPane.getIconAt(index));
			label.setHorizontalAlignment(JLabel.CENTER);

			xLabel = new JLabel(IconUtil.resize(IconUtil.CLOSE, 12, 12));
			xLabel.setBorder(new EmptyBorder(2, 5, 0, 0));
			xLabel.setVisible(false);
			xLabel.addMouseListener(mouseAdapter);

			add(label, BorderLayout.WEST);
			add(xLabel, BorderLayout.EAST);
			
			JNotepad.this.tabbedPane.setTabComponentAt(index, this);
			if(tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()).equals(comp))
				setXVisibility(true);
			setPreferredSize(getPreferredSize());
		}
		
		private void setXVisibility(final boolean visible){
			xLabel.setVisible(visible);
			revalidate();
			repaint();
		}

	}

	private class TextArea extends JPanel implements ActionListener{
		
		private class TextScrollBar extends JScrollBar{
			
			private LinkedList<Rectangle> rects;
			private Color color;
			private JScrollPane scroll;
			
			public TextScrollBar(final JScrollPane scroll){
				super(JScrollBar.VERTICAL);

				this.scroll = scroll;
				
				color = currentHighlightColor;
								
				rects = new LinkedList<Rectangle>();
				
			}
			
			private void addRect(final Rectangle r){
				r.x = 0;
				r.width = scroll.getWidth();
				rects.add(r);
				repaint();
			}
			
			private void clearRects(){
				rects.clear();
				repaint();
			}

			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(color);
				for(final Rectangle r : rects)
					g.fillRect(r.x, r.y, r.width, r.height);
			}
			
		}

		private JTextArea area;
		private JScrollPane scroll;
		private TextScrollBar vertScroll;

		private LinkedList<String> previous;
		private int current;

		private Color textColor;
		private Color backgroundColor;
		private Font textFont;

		private KeyAdapter keyAdapter;
		private MouseAdapter mouseAdapter;

		private JPopupMenu popup;
		private JMenuItem textColorItem;
		private JMenuItem backgroundColorItem;
		private JMenuItem fontItem;
		private JMenuItem findItem;
		private JMenuItem printItem;
		private JMenuItem saveItem;
		private JMenuItem closeItem;

		private TextArea(){
			this("");
		}

		private TextArea(final String text){
			super(new BorderLayout());

			mouseAdapter = new MouseAdapter(){
				public void mousePressed(MouseEvent e){
					if(e.getButton() == MouseEvent.BUTTON3)
						popup.show(e.getComponent(), e.getX(), e.getY());
				}
			};

			keyAdapter = new KeyAdapter(){
				public void keyPressed(KeyEvent e){
					final int keycode = e.getKeyCode();
					if(e.isControlDown()){
						switch(keycode){
						case KeyEvent.VK_N:
							menuBar.newItem.doClick();
							break;
						case KeyEvent.VK_X:
							menuBar.closeItem.doClick();
							break;
						case KeyEvent.VK_S:
							menuBar.saveItem.doClick();
							break;
						case KeyEvent.VK_Z:
							if(current > 0){
								area.setText(previous.get(--current));
								area.repaint();
							}
							break;
						case KeyEvent.VK_Y:
							if(current < previous.size()-1){
								area.setText(previous.get(++current));
								area.repaint();
							}
							break;
						case KeyEvent.VK_F:
							menuBar.findItem.doClick();
							break;
						case KeyEvent.VK_P:
							menuBar.printItem.doClick();
							break;
						case KeyEvent.VK_D:
							menuBar.closeAllItem.doClick();
							break;
						case KeyEvent.VK_O:
							menuBar.fileItem.doClick();
							break;
						case KeyEvent.VK_W:
							menuBar.urlItem.doClick();
							break;
						}
					}
					if(Character.isLetterOrDigit(e.getKeyChar())){
						previous.add(area.getText());
						current = previous.size()-1;
					}
				}
			};
			
			setFocusable(true);
			addKeyListener(keyAdapter);

			previous = new LinkedList<String>();
			current = 0;

			area = new JTextArea(text);
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			area.addKeyListener(keyAdapter);
			area.addMouseListener(mouseAdapter);

			textColor = area.getForeground();
			backgroundColor = area.getBackground();
			textFont = area.getFont();

			popup = new JPopupMenu();

			textColorItem = new JMenuItem("Text Color");
			textColorItem.setIcon(IconUtil.create(textColor));
			textColorItem.addActionListener(this);

			backgroundColorItem = new JMenuItem("Background Color");
			backgroundColorItem.setIcon(IconUtil.create(backgroundColor));
			backgroundColorItem.addActionListener(this);

			fontItem = new JMenuItem("Font");
			fontItem.setIcon(IconUtil.FONT);
			fontItem.addActionListener(this);

			findItem = new JMenuItem("Find & Replace");
			findItem.setIcon(IconUtil.FIND);
			findItem.addActionListener(this);

			printItem = new JMenuItem("Print");
			printItem.setIcon(IconUtil.PRINT);
			printItem.addActionListener(this);

			saveItem = new JMenuItem("Save");
			saveItem.setIcon(IconUtil.SAVE);
			saveItem.addActionListener(this);

			closeItem = new JMenuItem("Close");
			closeItem.setIcon(IconUtil.CLOSE);
			closeItem.addActionListener(this);

			popup.add(saveItem);
			popup.add(closeItem);
			popup.addSeparator();
			popup.add(printItem);
			popup.addSeparator();
			popup.add(findItem);
			popup.addSeparator();
			popup.add(textColorItem);
			popup.add(backgroundColorItem);
			popup.addSeparator();
			popup.add(fontItem);

			scroll = new JScrollPane(area);
			
			vertScroll = new TextScrollBar(scroll);
						
			scroll.setVerticalScrollBar(vertScroll);

			add(scroll, BorderLayout.CENTER);
		}

		public void actionPerformed(ActionEvent e){
			final Object source = e.getSource();
			if(!(source instanceof JMenuItem)) return;
			if(source.equals(textColorItem)){
				final Color c = JColorChooser.showDialog(JNotepad.this, "Select Text Color", textColor);
				if(c != null){
					area.setForeground(textColor = c);
					textColorItem.setIcon(IconUtil.create(textColor));
					final TextArea area = ((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()));
					menuBar.backgroundColorItem.setIcon(IconUtil.create(area.backgroundColor));
					menuBar.textColorItem.setIcon(IconUtil.create(area.textColor));
				}
			}else if(source.equals(backgroundColorItem)){
				final Color c = JColorChooser.showDialog(JNotepad.this, "Select Background Color", backgroundColor);
				if(c != null){
					area.setBackground(backgroundColor = c);
					backgroundColorItem.setIcon(IconUtil.create(backgroundColor));
					final TextArea area = ((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()));
					menuBar.backgroundColorItem.setIcon(IconUtil.create(area.backgroundColor));
					menuBar.textColorItem.setIcon(IconUtil.create(area.textColor));
				}
			}else if(source.equals(fontItem)){
				final Font f = JFontChooser.showDialog(JNotepad.this, "Select Font", textFont);
				if(f != null)
					area.setFont(textFont = f);
			}else if(source.equals(findItem)){
				JNotepad.this.menuBar.findItem.doClick();
			}else if(source.equals(printItem)){
				JNotepad.this.menuBar.printItem.doClick();
			}else if(source.equals(saveItem)){
				JNotepad.this.menuBar.saveItem.doClick();
			}else if(source.equals(closeItem)){
				JNotepad.this.menuBar.closeItem.doClick();
			}
		}

		private boolean shouldSave(){
			return area.getText().trim().length() > 0;
		}
	}

	private class MenuBar extends JMenuBar implements ActionListener, SearchListener{

		private JMenu fileMenu;
		
		private JMenu openMenu;
		private JMenuItem fileItem;
		private JMenuItem urlItem;
		
		private JMenuItem newItem;
		private JMenuItem saveItem;
		private JMenuItem closeItem;
		private JMenuItem closeAllItem;
		private JMenuItem printItem;

		private JMenu editMenu;
		private JMenuItem findItem;
		private JMenuItem textColorItem;
		private JMenuItem backgroundColorItem;
		private JMenuItem fontItem;

		private MenuBar(){
			super();

			fileMenu = new JMenu("File");
			fileMenu.setIcon(IconUtil.FILE);

			openMenu = new JMenu("Open From...");
			openMenu.setIcon(IconUtil.OPEN);
			
			fileItem = new JMenuItem("File (Ctrl + O)");
			fileItem.setIcon(IconUtil.OPEN_FILE);
			fileItem.addActionListener(this);
			
			urlItem = new JMenuItem("URL (Ctrl + W)");
			urlItem.setIcon(IconUtil.WEB);
			urlItem.addActionListener(this);
			
			openMenu.add(fileItem);
			openMenu.add(urlItem);

			newItem = new JMenuItem("New (Ctrl + N)");
			newItem.setIcon(IconUtil.NEW);
			newItem.addActionListener(this);

			saveItem = new JMenuItem("Save (Ctrl + S)");
			saveItem.setIcon(IconUtil.SAVE);
			saveItem.addActionListener(this);

			closeItem = new JMenuItem("Close (Ctrl + X)");
			closeItem.setIcon(IconUtil.CLOSE);
			closeItem.addActionListener(this);

			closeAllItem = new JMenuItem("Close All (Ctrl + D)");
			closeAllItem.setIcon(IconUtil.CLOSE_ALL);
			closeAllItem.addActionListener(this);

			printItem = new JMenuItem("Print (Ctrl + P)");
			printItem.setIcon(IconUtil.PRINT);
			printItem.addActionListener(this);

			fileMenu.add(newItem);
			fileMenu.add(openMenu);
			fileMenu.add(saveItem);
			fileMenu.addSeparator();
			fileMenu.add(printItem);
			fileMenu.addSeparator();
			fileMenu.add(closeItem);
			fileMenu.add(closeAllItem);

			editMenu = new JMenu("Edit");
			editMenu.setIcon(IconUtil.EDIT);

			findItem = new JMenuItem("Find & Replace (Ctrl + F)");
			findItem.setIcon(IconUtil.FIND);
			findItem.addActionListener(this);

			textColorItem = new JMenuItem("Text Color");
			textColorItem.setIcon(IconUtil.create(Color.BLACK));
			textColorItem.addActionListener(this);

			backgroundColorItem = new JMenuItem("Background Color");
			backgroundColorItem.setIcon(IconUtil.create(Color.WHITE));
			backgroundColorItem.addActionListener(this);

			fontItem = new JMenuItem("Font");
			fontItem.setIcon(IconUtil.FONT);
			fontItem.addActionListener(this);

			editMenu.add(findItem);
			editMenu.addSeparator();
			editMenu.add(textColorItem);
			editMenu.add(backgroundColorItem);
			editMenu.add(fontItem);

			add(fileMenu);
			add(editMenu);
			add(createSkinMenu());
		}

		public void actionPerformed(ActionEvent e){
			final Object source = e.getSource();
			if(!(source instanceof JMenuItem)) return;
			if(source.equals(newItem)){
				addNew();
			}else if(source.equals(closeItem)){
				final TextArea area = (TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
				int value = SAVE_SUCCESS;
				if(area.shouldSave()){ 
					if((value = saveCurrent()) == SAVE_SUCCESS)
						JOptionPane.showMessageDialog(null, "Successfully saved", "Success", JOptionPane.INFORMATION_MESSAGE);
					else
						JOptionPane.showMessageDialog(null, "Did not save " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()), "Failed", JOptionPane.ERROR_MESSAGE);
				}
				if(value != SAVE_CANCEL){
					tabbedPane.remove(area);
					if(tabbedPane.getTabCount() == 0) addNew();
				}
			}else if(source.equals(closeAllItem)){
				for(int i = tabbedPane.getTabCount()-1; i > -1; i--){
					tabbedPane.setSelectedIndex(i);
					closeItem.doClick();
				}
				if(tabbedPane.getTabCount() == 0) addNew();
			}else if(source.equals(saveItem)){
				if(saveCurrent() == SAVE_SUCCESS)
					JOptionPane.showMessageDialog(null, "Successfully saved", "Success", JOptionPane.INFORMATION_MESSAGE);
				else
					JOptionPane.showMessageDialog(null, "Did not save " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()), "Failed", JOptionPane.ERROR_MESSAGE);
			}else if(source.equals(fileItem)){
				final int v = fileOpener.showOpenDialog(null);
				if(v != JFileChooser.APPROVE_OPTION) return;
				new Thread(
					new Runnable(){
						public void run(){
							final File[] files = fileOpener.getSelectedFiles();
							for(final File f : files){
								final String text = loadFromFile(f);
								if(text != null){
									String fileName = f.getName();
									fileName = fileName.substring(0, fileName.indexOf(".txt"));
									final TextArea area = new TextArea(text);
									tabbedPane.addTab(fileName, IconUtil.NEW_TAB, area);
									tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
									new CloseableTab(tabbedPane.getTabCount()-1, area);
									((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).area.requestFocus();
								}else{
									JOptionPane.showMessageDialog(null, "Error loading file contents", "Error", JOptionPane.ERROR_MESSAGE);
								}
							}
						}
					}
				).start();
			}else if(source.equals(urlItem)){
				final String url = JOptionPane.showInputDialog(null, "Enter URL");
				if(url != null && !url.trim().equals("")){
					new Thread(
						new Runnable(){
							public void run(){
								final String content = loadFromWeb(url);
								if(content != null){
									final String host = getURLHost(url);
									final TextArea area = new TextArea(content);
									tabbedPane.addTab(host, IconUtil.NEW_TAB, area);
									tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
									new CloseableTab(tabbedPane.getTabCount()-1, area, url);
									((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).area.requestFocus();
								}else{
									JOptionPane.showMessageDialog(null, "Error loading contents from URL", "Error", JOptionPane.ERROR_MESSAGE);
								}
							}
						}
					).start();
				}
			}else if(source.equals(findItem)){
				findDialog.setTitle(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
				findDialog.pack();
				findDialog.setLocationRelativeTo(null);
				findDialog.setVisible(true);
			}else if(source.equals(textColorItem)){
				((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).textColorItem.doClick();
			}else if(source.equals(backgroundColorItem)){
				((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).backgroundColorItem.doClick();
			}else if(source.equals(fontItem)){
				((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).fontItem.doClick();
			}else if(source.equals(printItem)){
				final TextArea area = (TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
				try{
					if(area.area.print())
						JOptionPane.showMessageDialog(null, "Successfully printed " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()), "Success", JOptionPane.INFORMATION_MESSAGE);
				}catch(PrinterException ex){
					JOptionPane.showMessageDialog(null, "Error Printing " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		private Integer[] indexesOf(final String text, final String search){
			final LinkedList<Integer> list = new LinkedList<Integer>();
			for(int i = text.indexOf(search); i >= 0; i = text.indexOf(search, i+1))
				list.add(i);
			return list.toArray(new Integer[0]);
		}
		
		private Integer[] indexesOfIgnoreCase(final String text, final String search){
			return indexesOf(text.toLowerCase(Locale.CANADA), search.toLowerCase(Locale.CANADA));
		}

		public void onSearch(final String search){
			((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).area.getHighlighter().removeAllHighlights();
			final TextArea area = ((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()));
			final Highlighter highlighter = area.area.getHighlighter();
			final DefaultHighlighter.DefaultHighlightPainter paint = new DefaultHighlighter.DefaultHighlightPainter(currentHighlightColor);
			final Integer[] indexes = indexesOfIgnoreCase(area.area.getText(), search);
			findDialog.setOccurrences(indexes.length);
			for(Integer i : indexes){
				try{
					area.vertScroll.addRect(area.area.modelToView(i));
					highlighter.addHighlight(i, i+search.length(), paint);
				}catch(BadLocationException e){
					e.printStackTrace();
				}
			}
		}

		public void onReplace(final String search, final String replace){
			final TextArea area = ((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()));
			area.area.getHighlighter().removeAllHighlights();
			area.vertScroll.clearRects();
			area.area.setText(replaceAllIgnoreCase(area.area.getText(), search, replace));
			area.area.repaint();
			findDialog.reset();
		}
		
		private String replaceAllIgnoreCase(final String text, final String search, final String replacement){
			if(search.equals(replacement)) return text;
            final StringBuffer buffer = new StringBuffer(text);
            final String lowerSearch = search.toLowerCase(Locale.CANADA);
            int i = 0;
            int prev = 0;
            while((i = buffer.toString().toLowerCase(Locale.CANADA).indexOf(lowerSearch, prev)) > -1){
                buffer.replace(i, i+search.length(), replacement);
                prev = i+replacement.length();
            }
            return buffer.toString();
        }

		public void onExit(){
			final TextArea area = ((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()));
			area.area.getHighlighter().removeAllHighlights();
			findDialog.reset();
			area.vertScroll.clearRects();
		}
	}

	private MenuBar menuBar;

	private FindAndReplaceDialog findDialog;

	private int untitledCount;

	private JFileChooser fileSaver;
	private FileNameExtensionFilter filter;

	private JFileChooser fileOpener;

	private JTabbedPane tabbedPane;

	private Color currentHighlightColor;

	public static final int SAVE_SUCCESS = 1;
	public static final int SAVE_NO = 2;
	public static final int SAVE_CANCEL = 3;
	public static final int SAVE_FAIL = 4;

	public JNotepad(final String title){
		super(title);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setFocusable(true);
		addKeyListener(
				new KeyAdapter(){
					public void keyPressed(KeyEvent e){
						final int keycode = e.getKeyCode();
						if(e.isControlDown()){
							switch(keycode){
							case KeyEvent.VK_N:
								menuBar.newItem.doClick();
								break;
							case KeyEvent.VK_X:
								menuBar.closeItem.doClick();
								break;
							case KeyEvent.VK_S:
								menuBar.saveItem.doClick();
								break;
							case KeyEvent.VK_F:
								menuBar.findItem.doClick();
								break;
							case KeyEvent.VK_P:
								menuBar.printItem.doClick();
								break;
							case KeyEvent.VK_O:
								menuBar.fileItem.doClick();
								break;
							case KeyEvent.VK_W:
								menuBar.urlItem.doClick();
								break;
							case KeyEvent.VK_D:
								menuBar.closeAllItem.doClick();
								break;
							}
						}
					}
				}	
			);
		addWindowListener(
				new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						for(int i = tabbedPane.getTabCount()-1; i > -1; i--){
							tabbedPane.setSelectedIndex(i);
							final TextArea area = (TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
							int value = SAVE_SUCCESS;
							if(area.shouldSave()){ 
								if((value = saveCurrent()) == SAVE_SUCCESS)
									JOptionPane.showMessageDialog(null, "Successfully saved", "Success", JOptionPane.INFORMATION_MESSAGE);
								else
									JOptionPane.showMessageDialog(null, "Did not save " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()), "Failed", JOptionPane.ERROR_MESSAGE);
							}
							if(value != SAVE_CANCEL){
								tabbedPane.remove(area);
							}else{
								return;
							}
						}
						System.exit(0);
					}
				}
		);

		currentHighlightColor = Color.ORANGE;

		filter = new FileNameExtensionFilter("Text Files", "txt", ".txt");

		fileSaver = new JFileChooser();
		fileSaver.setMultiSelectionEnabled(false);
		fileSaver.setFileFilter(filter);
		fileSaver.setDialogTitle("Save File");

		fileOpener = new JFileChooser();
		fileOpener.setMultiSelectionEnabled(true);
		fileOpener.setFileFilter(filter);
		fileOpener.setDialogTitle("Open File");

		menuBar = new MenuBar();
		setJMenuBar(menuBar);

		findDialog = new FindAndReplaceDialog(this);
		findDialog.setSearchListener(menuBar);

		untitledCount = 1;

		tabbedPane = new JTabbedPane();
		tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		tabbedPane.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						if(tabbedPane.getSelectedIndex() > -1){
							final TextArea area = ((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex()));
							final CloseableTab tab = (CloseableTab)tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
							area.area.requestFocus();
							menuBar.backgroundColorItem.setIcon(IconUtil.create(area.backgroundColor));
							menuBar.textColorItem.setIcon(IconUtil.create(area.textColor));
							for(int i = 0; i < tabbedPane.getTabCount(); i++){
								final CloseableTab t = (CloseableTab)tabbedPane.getTabComponentAt(i);
								if(tab != t) t.setXVisibility(false);
							}
						}
					}
				}
				);

		addNew();

		add(tabbedPane, BorderLayout.CENTER);
	}
	
	public JNotepad(){
		this("JNotepad");
	}

	private JMenu createSkinMenu(){
		final JMenu menu = new JMenu("Skin");
		menu.setIcon(IconUtil.SKIN);
		final ButtonGroup group = new ButtonGroup();
		for(final LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()){
			final JRadioButtonMenuItem item = createLAFMenuItem(laf);
			group.add(item);
			menu.add(item);
		}
		return menu;
	}

	private void setLAF(final String laf){
		try{
			UIManager.setLookAndFeel(laf);
			SwingUtilities.updateComponentTreeUI(this);
			SwingUtilities.updateComponentTreeUI(findDialog);
			SwingUtilities.updateComponentTreeUI(menuBar);
			SwingUtilities.updateComponentTreeUI(fileOpener);
			SwingUtilities.updateComponentTreeUI(fileSaver);
			for(int i = 0; i < tabbedPane.getTabCount(); i++){
				final TextArea area = (TextArea)tabbedPane.getComponentAt(i);
				SwingUtilities.updateComponentTreeUI(area.popup);
			}
		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Error setting skin", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private JRadioButtonMenuItem createLAFMenuItem(final LookAndFeelInfo laf){
		final JRadioButtonMenuItem item = new JRadioButtonMenuItem(laf.getName(), laf.getName().equals(UIManager.getLookAndFeel().getName()));
		item.addItemListener(
				new ItemListener(){
					public void itemStateChanged(ItemEvent e){
						if(item.isSelected()) setLAF(laf.getClassName());
					}
				}
				);
		return item;
	}

	public void setHighlightColor(final Color c){
		currentHighlightColor = c;
		for(int i = 0; i < tabbedPane.getTabCount(); i++){
			final TextArea area = (TextArea)tabbedPane.getComponentAt(i);
			area.vertScroll.color = c;
			area.repaint();
			area.vertScroll.repaint();
		}
		if(!findDialog.getSearch().equals("")) findDialog.getSearchListener().onSearch(findDialog.getSearch());
	}

	public Color getCurrentHighlightColor(){
		return currentHighlightColor;
	}

	private void addNew(){
		final TextArea area = new TextArea();
		tabbedPane.addTab("Untitled"+untitledCount++, IconUtil.NEW_TAB, area);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
		new CloseableTab(tabbedPane.getTabCount()-1, area);
		((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).area.requestFocus();
	}

	private String loadFromFile(final File f){
		String text = "";
		try{
			final Scanner reader = new Scanner(f);
			while(reader.hasNextLine()) text += reader.nextLine()+"\n";
			reader.close();
		}catch(IOException e){
			text = null;
		}
		return text;
	}
	
	private String loadFromWeb(final String ref){
		String text = "";
		URL url = null;
		URLConnection connection = null;
		Scanner reader = null;
		try{
			url = new URL(ref.startsWith("http://") ? ref : "http://"+ref);
			connection = url.openConnection();
			connection.setConnectTimeout(60000);
			connection.setConnectTimeout(60000);
			connection.setUseCaches(false);
			connection.addRequestProperty("User-Agent", UserAgentUtil.random());
			connection.addRequestProperty("User-Content", "application/x-www-form-urlencoded");
			reader = new Scanner(connection.getInputStream(), "UTF-8");
			while(reader.hasNextLine()) text += reader.nextLine()+"\n";
			reader.close();
		}catch(IOException e){
			text = null;
		}
		return text;
	}
	
	private String getURLHost(final String ref){
		try{
			return new URL(ref).getHost();
		}catch(IOException e){
			return null;
		}
	}

	private int saveCurrent(){
		final int value = JOptionPane.showConfirmDialog(null, "Do you want to save this file?", "Save?", JOptionPane.INFORMATION_MESSAGE);
		if(value != JOptionPane.OK_OPTION){
			if(value == JOptionPane.CANCEL_OPTION)
				return SAVE_CANCEL;
			else if(value == JOptionPane.NO_OPTION)
				return SAVE_NO;
			else
				return SAVE_CANCEL;
		}
		fileSaver.setSelectedFile(new File(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())+".txt"));
		final int v = fileSaver.showSaveDialog(null);
		if(v != JFileChooser.APPROVE_OPTION) return SAVE_CANCEL;
		String path = fileSaver.getSelectedFile().getPath();
		if(new File(path).exists()){
			final int t = JOptionPane.showConfirmDialog(null, "Do you want to overwrite this file?", "Overwrite?", JOptionPane.INFORMATION_MESSAGE);
			if(t != JOptionPane.OK_OPTION){
				if(t == JOptionPane.CANCEL_OPTION)
					return SAVE_CANCEL;
				else if(t == JOptionPane.NO_OPTION)
					return SAVE_NO;
				else
					return SAVE_CANCEL;
			}
		}
		if(!path.endsWith(".txt")) path += ".txt";
		final File destination = new File(path);
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(destination));
			writer.write(((TextArea)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).area.getText());
		}catch(IOException e){
			return SAVE_FAIL;
		}finally{
			try{
				writer.flush();
				writer.close();
			}catch(IOException e){
				return SAVE_FAIL;
			}
		}
		path = path.substring(path.lastIndexOf(File.separator)+1);
		tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), path.substring(0, path.indexOf(".txt")));
		return SAVE_SUCCESS; 
	}

}
