import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

final class MyFileChooser extends JFileChooser {
	private final MyFilterWrapper filter;

	public MyFileChooser(MyFilterWrapper filter) {
		this.filter = filter;
		// 扩展名过滤
		setFileFilter(filter);

		// 文件选择属性设置
		setMultiSelectionEnabled(true);
		setAcceptAllFileFilterUsed(false);
		setFileSelectionMode(FILES_AND_DIRECTORIES);
	}

	public String [] getAbsolutePathsRecursively() {
		ArrayList<String> paths = new ArrayList<String>();
		File [] files = getSelectedFiles();
		traverse(files, paths);
		return paths.toArray(new String [] {});
	}

	private void traverse(File [] files, ArrayList<String> paths) {
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isDirectory()) {
				traverse(f.listFiles(this.filter), paths);
			} else if (f.isFile() && this.filter.accept(f)) {
				paths.add(f.getAbsolutePath());
			}
		}
	}
}

final class MyFilterWrapper extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
	private final FileNameExtensionFilter filter;

	public MyFilterWrapper(String description, String... extensions) {
		this.filter = new FileNameExtensionFilter(description, extensions);
	}

	public boolean accept(File f) {
		return this.filter.accept(f);
	}

	public String getDescription() {
		return this.filter.getDescription();
	}
}

class ZoomablePicture extends JComponent {
	private Image image;
	private int width, height;
	private float zoomFactor;

	public void load(String filename) {
		unload();
		image = Toolkit.getDefaultToolkit().getImage(filename);
		MediaTracker mt = new MediaTracker(this);
		mt.addImage(image, 0);
		try {
			mt.waitForAll();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		width = image.getWidth(null);
		height = image.getHeight(null);
		zoomFactor = 1.0f;
		setPreferredSize(new Dimension(width, height));
		revalidate();
		repaint();
	}

	public void unload() {
		if (image != null) {
			image = null;
			setPreferredSize(new Dimension(1, 1));
			revalidate();
			repaint();
		}
	}

	public void setZoomFactor(float factor) {
		zoomFactor = factor;
		setPreferredSize(new Dimension((int) (width * zoomFactor), (int) (height * zoomFactor)));
		revalidate();
		repaint();
	}

	public float getZoomFactor() {
		return zoomFactor;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			int ws = getWidth();
			int hs = getHeight();
			int wp = getPreferredSize().width;
			int hp = getPreferredSize().height;
			g.drawImage(image, (ws - wp) / 2, (hs - hp) / 2, wp, hp, null);
		}
	}
}

class ScrollablePicture extends ZoomablePicture {
	private Point oldCursorPos;

	public ScrollablePicture() {
		addMouseMotionListener(new MouseMotionAdapter () {
			public void mouseDragged(MouseEvent e) {
				dragTo(e.getLocationOnScreen());
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				startDragging(e.getLocationOnScreen());
			}
			public void mouseReleased(MouseEvent e) {
				stopDragging();
			}
		});
	}

	public void load(String filename) {
		super.load(filename);
		scrollRectToVisible(new Rectangle()); // 滚动到左上角位置
	}

	private void startDragging(Point cursorPos) {
		oldCursorPos = cursorPos;
		setCursor(new Cursor(Cursor.MOVE_CURSOR));
	}

	private void stopDragging() {
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	private void dragTo(Point newCursorPos) {
		int dx = newCursorPos.x - oldCursorPos.x;
		int dy = newCursorPos.y - oldCursorPos.y;
		Rectangle visibleRect = getVisibleRect();
		visibleRect.translate(-dx, -dy);
		scrollRectToVisible(visibleRect);
		oldCursorPos = newCursorPos;
	}
}

class ToolBarStatusFrame extends JFrame {
	private final JToolBar toolbar = new JToolBar();
	private final JLabel status = new JLabel();

	public ToolBarStatusFrame() {
		Container cp = getContentPane();
		cp.add(toolbar, BorderLayout.NORTH);
		cp.add(status, BorderLayout.SOUTH);
	}

	public void setToolBarComponentsEnabled(boolean... enabled) {
		for (int i = 0; i < enabled.length; i++) {
			toolbar.getComponent(i).setEnabled(enabled[i]);
		}
	}

	public void addToolBarComponents(JComponent... comp) {
		for (int i = 0; i < comp.length; i++) {
			toolbar.add(comp[i]);
		}
	}

	public void setStatus(String statusText) {
		status.setText(statusText);
	}
}

final class MyPicViewer extends ToolBarStatusFrame {
	private String [] pictureList = {};
	private int pictureIndex = -1;
    private JButton btn_winTopButton;
	private ZoomablePicture view = new ScrollablePicture();

	public static void main(String [] args) {
		new MyPicViewer();
	}

	public MyPicViewer() {
		setTitle("图片查看器");
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		createToolBarButtons();
		setToolBarComponentsEnabled(true, false, false, false, false, true);

		getContentPane().add(new JScrollPane(view));
		showCurrentPicture();

		setVisible(true);
	}

	private class ToolbarButton extends JButton {
		public ToolbarButton(String text, String icon, ActionListener l) {
			super(text, new ImageIcon(MyPicViewer.this. getClass().getClassLoader().getResource(icon)));
			addActionListener(l);
			setPreferredSize(new Dimension(100, 21));
		}
	}

	private void createToolBarButtons() {
		 btn_winTopButton = new ToolbarButton("窗口置顶", "icons/icon_top.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setAlwaysOnTop(!isAlwaysOnTop());
				if(isAlwaysOnTop()){
					btn_winTopButton.setText("取消置顶");
					
				}
				else{
					btn_winTopButton .setText("窗口置顶");
				}
			}
		});
		
		addToolBarComponents(new ToolbarButton("打开/查找", "icons/document-open.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				choosePictures();
				showCurrentPicture();
			}
		}), new ToolbarButton("放大", "icons/list-add.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setZoomFactor(view.getZoomFactor() * 1.1f);
			}
		}), new ToolbarButton("缩小", "icons/list-remove.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setZoomFactor(view.getZoomFactor() * 0.9f);
			}
		}), new ToolbarButton("", "icons/go-previous.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pictureIndex--;
				showCurrentPicture();
			}
		}), new ToolbarButton("", "icons/go-next.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pictureIndex++;
				showCurrentPicture();
			}
		}),btn_winTopButton,
		 
		new ToolbarButton("退出", "icons/system-log-out.png", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		}));
	}

	private void choosePictures() {
		MyFileChooser fc = new MyFileChooser(new MyFilterWrapper("图片文件", "jpg", "png", "gif"));
		int ret = fc.showOpenDialog(this);
		if (ret == JFileChooser.APPROVE_OPTION) {
			this.pictureList = fc.getAbsolutePathsRecursively();
			this.pictureIndex = (this.pictureList.length > 0) ? 0 : -1;
		}
	}

	private void showCurrentPicture() {
		int i = this.pictureIndex;
		if (i >= 0) {
			String filename = this.pictureList[i];
			this.view.load(filename);
			setStatus(String.format("[%d/%d] %s", i + 1, this.pictureList.length, filename));
		} else {
			this.view.unload();
			setStatus("没有加载图片");
		}
		setToolBarComponentsEnabled(true, i >= 0, i >= 0, i > 0, i + 1 < this.pictureList.length, true);
	}
}
