package net.sf.openrocket.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;

import net.sf.openrocket.gui.Resettable;

public class GUIUtil {

	private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final String CLOSE_ACTION_KEY =  "escape:WINDOW_CLOSING"; 
	
    private static final List<Image> images = new ArrayList<Image>();
    static {
    	loadImage("pix/icon/icon-256.png");
    	loadImage("pix/icon/icon-064.png");
    	loadImage("pix/icon/icon-048.png");
    	loadImage("pix/icon/icon-032.png");
    	loadImage("pix/icon/icon-016.png");
    }
    private static void loadImage(String file) {
    	InputStream is;
 
    	is = ClassLoader.getSystemResourceAsStream(file);
    	if (is == null)
    		return;
    	
    	try {
    		Image image = ImageIO.read(is);
    		images.add(image);
    	} catch (IOException ignore) {
    		ignore.printStackTrace();
    	}
    }
    

    
    /**
     * Set suitable options for a single-use disposable dialog.  This includes
     * setting ESC to close the dialog and adding the appropriate window icons.
     * If defaultButton is provided, it is set to the default button action.
     * <p>
     * The default button must be already attached to the dialog.
     * 
     * @param dialog		the dialog.
     * @param defaultButton	the default button of the dialog, or <code>null</code>.
     */
    public static void setDisposableDialogOptions(JDialog dialog, JButton defaultButton) {
    	installEscapeCloseOperation(dialog);
    	setWindowIcons(dialog);
    	addModelNullingListener(dialog);
    	if (defaultButton != null) {
    		setDefaultButton(defaultButton);
    	}
    }
    
	
	
	/**
	 * Add the correct action to close a JDialog when the ESC key is pressed.
	 * The dialog is closed by sending is a WINDOW_CLOSING event.
	 * 
	 * @param dialog	the dialog for which to install the action.
	 */
	public static void installEscapeCloseOperation(final JDialog dialog) { 
	    Action dispatchClosing = new AbstractAction() { 
	        public void actionPerformed(ActionEvent event) { 
	            dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING)); 
	        } 
	    }; 
	    JRootPane root = dialog.getRootPane(); 
	    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESCAPE, CLOSE_ACTION_KEY); 
	    root.getActionMap().put(CLOSE_ACTION_KEY, dispatchClosing); 
	}
	
	
	/**
	 * Set the given button as the default button of the frame/dialog it is in.  The button
	 * must be first attached to the window component hierarchy.
	 * 
	 * @param button	the button to set as the default button.
	 */
	public static void setDefaultButton(JButton button) {
		Window w = SwingUtilities.windowForComponent(button);
		if (w == null) {
			throw new IllegalArgumentException("Attach button to a window first.");
		}
		if (!(w instanceof RootPaneContainer)) {
			throw new IllegalArgumentException("Button not attached to RootPaneContainer, w="+w);
		}
		((RootPaneContainer)w).getRootPane().setDefaultButton(button);
	}

	
	
	/**
	 * Change the behavior of a component so that TAB and Shift-TAB cycles the focus of
	 * the components.  This is necessary for e.g. <code>JTextArea</code>.
	 * 
	 * @param c		the component to modify
	 */
    public static void setTabToFocusing(Component c) {
        Set<KeyStroke> strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")));
        c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes);
        strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")));
        c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes);
    }

    
    
    /**
     * Set the OpenRocket icons to the window icons.
     * 
     * @param window	the window to set.
     */
    public static void setWindowIcons(Window window) {
    	window.setIconImages(images);
    }
    
    /**
     * Add a listener to the provided window that will call {@link #setNullModels(Component)}
     * on the window once it is disposed.  This method may only be used on single-use
     * windows and dialogs, that will never be shown again once closed!
     * 
     * @param window	the window to add the listener to.
     */
    public static void addModelNullingListener(final Window window) {
    	window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				setNullModels(window);
			}
    	});
    }


	/**
	 * Traverses recursively the component tree, and sets all applicable component 
	 * models to null, so as to remove the listener connections.  After calling this
	 * method the component hierarchy should no longed be used.
	 * <p>
	 * All components that use custom models should be added to this method, as
	 * there exists no standard way of removing the model from a component.
	 * 
	 * @param c		the component (<code>null</code> is ok)
	 */
	public static void setNullModels(Component c) {
		if (c==null)
			return;
		
		// Remove various listeners
		for (ComponentListener l: c.getComponentListeners()) {
			c.removeComponentListener(l);
		}
		for (FocusListener l: c.getFocusListeners()) {
			c.removeFocusListener(l);
		}
		for (MouseListener l: c.getMouseListeners()) {
			c.removeMouseListener(l);
		}
		for (PropertyChangeListener l: c.getPropertyChangeListeners()) {
			c.removePropertyChangeListener(l);
		}
		for (PropertyChangeListener l: c.getPropertyChangeListeners("model")) {
			c.removePropertyChangeListener("model", l);
		}
		for (PropertyChangeListener l: c.getPropertyChangeListeners("action")) {
			c.removePropertyChangeListener("action", l);
		}
		
		// Remove models for known components
		//  Why the FSCK must this be so hard?!?!?
	
		if (c instanceof JSpinner) {
			
			JSpinner spinner = (JSpinner)c;
			for (ChangeListener l: spinner.getChangeListeners()) {
				spinner.removeChangeListener(l);
			}
			spinner.setModel(new SpinnerNumberModel());
			
		} else if (c instanceof JSlider) {
			
			JSlider slider = (JSlider)c;
			for (ChangeListener l: slider.getChangeListeners()) {
				slider.removeChangeListener(l);
			}
			slider.setModel(new DefaultBoundedRangeModel());
			
		} else if (c instanceof JComboBox) {
			
			JComboBox combo = (JComboBox)c;
			for (ActionListener l: combo.getActionListeners()) {
				combo.removeActionListener(l);
			}
			combo.setModel(new DefaultComboBoxModel());
			
		} else if (c instanceof AbstractButton) {
			
			AbstractButton button = (AbstractButton)c;
			for (ActionListener l: button.getActionListeners()) {
				button.removeActionListener(l);
			}
			button.setAction(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) { }
			});
			
		} else if (c instanceof JTable) {
			
			JTable table = (JTable)c;
			table.setModel(new DefaultTableModel());
			table.setColumnModel(new DefaultTableColumnModel());
			table.setSelectionModel(new DefaultListSelectionModel());
			
		} else if (c instanceof JTree) {
			
			JTree tree = (JTree)c;
			tree.setModel(new DefaultTreeModel(new TreeNode() {
				@SuppressWarnings("unchecked")
				@Override
				public Enumeration children() {
					return new Vector().elements();
				}
				@Override
				public boolean getAllowsChildren() {
					return false;
				}
				@Override
				public TreeNode getChildAt(int childIndex) {
					return null;
				}
				@Override
				public int getChildCount() {
					return 0;
				}
				@Override
				public int getIndex(TreeNode node) {
					return 0;
				}
				@Override
				public TreeNode getParent() {
					return null;
				}
				@Override
				public boolean isLeaf() {
					return true;
				}
			}));
			tree.setSelectionModel(new DefaultTreeSelectionModel());
			
		} else if (c instanceof Resettable) {
			
			((Resettable)c).resetModel();
			
		}
	
		// Recurse the component
		if (c instanceof Container) {
			Component[] cs = ((Container)c).getComponents();
			for (Component sub: cs)
				setNullModels(sub);
		}
	
	}
	
	

    
    /**
     * A mouse listener that toggles the state of a boolean value in a table model
     * when clicked on another column of the table.
     * <p>
     * NOTE:  If the table model does not extend AbstractTableModel, the model must
     * fire a change event (which in normal table usage is not necessary).
     * 
     * @author Sampo Niskanen <sampo.niskanen@iki.fi>
     */
    public static class BooleanTableClickListener extends MouseAdapter {
    	
    	private final JTable table;
    	private final int clickColumn;
    	private final int booleanColumn;
    	
    	
    	public BooleanTableClickListener(JTable table) {
    		this(table, 1, 0);
    	}

    	
    	public BooleanTableClickListener(JTable table, int clickColumn, int booleanColumn) {
    		this.table = table;
    		this.clickColumn = clickColumn;
    		this.booleanColumn = booleanColumn;
    	}
    	
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1)
				return;
			
			Point p = e.getPoint();
			int col = table.columnAtPoint(p);
			if (col < 0)
				return;
			col = table.convertColumnIndexToModel(col);
			if (col != clickColumn) 
				return;
			
			int row = table.rowAtPoint(p);
			if (row < 0)
				return;
			row = table.convertRowIndexToModel(row);
			if (row < 0)
				return;
			
			TableModel model = table.getModel();
			Object value = model.getValueAt(row, booleanColumn);
			
			if (!(value instanceof Boolean)) {
				throw new IllegalStateException("Table value at row="+row+" col="+
						booleanColumn + " is not a Boolean, value=" +value);
			}
			
			Boolean b = (Boolean)value;
			b = !b;
			model.setValueAt(b, row, booleanColumn);
			if (model instanceof AbstractTableModel) {
				((AbstractTableModel)model).fireTableCellUpdated(row, booleanColumn);
			}
		}

    }
    
}