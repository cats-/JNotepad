package cats.notepad.comp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cats.notepad.JNotepad;

public class JFontChooser extends JDialog implements ItemListener{
	
	public static final String[] FONT_NAMES = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	public static final String[] SIZE_NAMES;
	public static final Integer[] SIZES;
	public static final String[] STYLE_NAMES = {
		"Plain", "Italic", "Bold", "Bold-Italic"
	};
	public static final int[] STYLES = {
		Font.PLAIN, Font.ITALIC, Font.BOLD, (Font.BOLD + Font.ITALIC)
	};
	
	static{
		final LinkedList<String> sTemp = new LinkedList<String>();
		final LinkedList<Integer> nTemp = new LinkedList<Integer>();
		for(int i = 10; i <= 72; i++){
			sTemp.add(Integer.toString(i));
			nTemp.add(i);
		}
		SIZES = nTemp.toArray(new Integer[0]);
		SIZE_NAMES = sTemp.toArray(new String[0]);
	}
	
	private JComboBox<String> fontBox;
	private JComboBox<String> sizeBox;
	private JComboBox<String> styleBox;
	private Font preview;
	private JPanel northPanel;
	
	private JLabel previewLabel;
	
	private JButton okButton;
	
	public JFontChooser(final JNotepad parent, final String title, final Font current){
		super(parent, title, true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		preview = current;
		
		fontBox = new JComboBox<String>(FONT_NAMES);
		fontBox.setBorder(BorderFactory.createTitledBorder("Font Family Names"));
		fontBox.setSelectedItem(preview.getFamily());
		fontBox.addItemListener(this);
		
		styleBox = new JComboBox<String>(STYLE_NAMES);
		styleBox.setBorder(BorderFactory.createTitledBorder("Font Styles"));
		styleBox.setSelectedIndex(getStyleIndex(preview.getStyle()));
		styleBox.addItemListener(this);
		
		sizeBox = new JComboBox<String>(SIZE_NAMES);
		sizeBox.setBorder(BorderFactory.createTitledBorder("Font Sizes"));
		sizeBox.setSelectedIndex(getSizeIndex(preview.getSize()));
		sizeBox.addItemListener(this);
		
		northPanel = new JPanel(new GridLayout(1, 3));
		northPanel.add(fontBox);
		northPanel.add(styleBox);
		northPanel.add(sizeBox);
		
		previewLabel = new JLabel("Preview");
		previewLabel.setHorizontalAlignment(JLabel.CENTER);
		previewLabel.setFont(preview);
		
		okButton = new JButton("Select");
		okButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						dispose();
					}
				}
		);
		
		add(northPanel, BorderLayout.NORTH);
		add(previewLabel, BorderLayout.CENTER);
		add(okButton, BorderLayout.SOUTH);
	}
	
	private int getStyleIndex(final int n){
		for(int i = 0; i < STYLES.length; i++)
			if(STYLES[i] == n) return i;
		return -1;
	}
	
	private int getSizeIndex(final int n){
		for(int i = 0; i < SIZES.length; i++)
			if(SIZES[i] == n) return i;
		return -1;
	}
	
	public Font getSelectedFont(){
		return preview;
	}
	
	public void itemStateChanged(ItemEvent e){
		final Object source = e.getSource();
		if(!(source instanceof JComboBox)) return;
		if(source.equals(fontBox)){
			preview = new Font(FONT_NAMES[fontBox.getSelectedIndex()], preview.getStyle(), preview.getSize());
			updatePreviewLabel();
		}else if(source.equals(styleBox)){
			preview = new Font(preview.getFamily(), STYLES[styleBox.getSelectedIndex()], preview.getSize());
			updatePreviewLabel();
		}else if(source.equals(sizeBox)){
			preview = new Font(preview.getFamily(), preview.getStyle(), SIZES[sizeBox.getSelectedIndex()]);
			updatePreviewLabel();
		}
	}
	
	private void updatePreviewLabel(){
		previewLabel.setFont(preview);
		previewLabel.repaint();
	}
	
	public static Font showDialog(final JNotepad parent, final String title, final Font current){
		final JFontChooser chooser = new JFontChooser(parent, title, current);
		final Dimension d = chooser.getPreferredSize();
		chooser.setSize(d.width, d.height+150);
		chooser.setLocationRelativeTo(null);
		chooser.setVisible(true);
		while(chooser.isVisible()){}
		return chooser.getSelectedFont();
	}

}
