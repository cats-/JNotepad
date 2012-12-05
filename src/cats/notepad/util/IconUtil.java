package cats.notepad.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class IconUtil {
	
	public static final Dimension DEFAULT = new Dimension(15, 15);
	
	public static final Icon CLOSE = get("close.png");
	public static final Icon CLOSE_ALL = get("close-all.png");
	public static final Icon FIND = get("find.png");
	public static final Icon FONT = get("font.png");
	public static final Icon OPEN = get("open.png");
	public static final Icon PRINT = get("print.png");
	public static final Icon SAVE = get("save.png");
	public static final Icon NEW = get("new.png");
	public static final Icon NEW_TAB = get("new-tab.png");
	public static final Icon EDIT = get("edit.png");
	public static final Icon FILE = get("file.png");
	public static final Icon SKIN = get("skin.png");
	public static final Icon OPEN_FILE = get("open-file.png");
	public static final Icon WEB = get("web.png");
	
	public static Icon create(final Color c, final int w, final int h){
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(c);
		g.fillRect(0, 0, w, h);
		g.dispose();
		return new ImageIcon(image);
	}
	
	public static Icon resize(final Icon icon, final int width, final int height){
		final Image image = ((ImageIcon)icon).getImage();
		final BufferedImage bim = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = bim.createGraphics();
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return new ImageIcon(bim);
	}
	
	public static Icon get(final String res){
		return new ImageIcon(IconUtil.class.getResource(res));
	}
	
	public static Icon create(final Color c){
		return create(c, DEFAULT);
	}
	
	public static Icon create(final Color c, final Dimension d){
		return create(c, d.width, d.height);
	}

}
