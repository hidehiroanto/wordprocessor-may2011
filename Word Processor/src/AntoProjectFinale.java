/***********************************************************************************
 * @author Hidehiro Anto
 * 
 * 
 *  Program #22: Word Processor
 *  Description:
 * 		This word processor allows the user to type into a text area, change the font,
 * 		undo, redo, open a new document, open, save, and print.
 * 				
 ***********************************************************************************/
import com.apple.eawt.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
public class AntoProjectFinale implements AboutHandler, QuitHandler
{
	private static WordProcessor root;

	// Final variables
	private static final String SCREEN_MENU_BAR_KEY = "apple.laf.useScreenMenuBar";
	private static final Image DOCK_ICON_IMAGE = new ImageIcon("bacon.png").getImage();
	private static final String ABOUT_MESSAGE = "Version 1.0 for Mac. © 2011 Hidehiro Anto.";

	public static void main(String[] args)
	{
		System.setProperty(SCREEN_MENU_BAR_KEY, String.valueOf(true));
		Application app = Application.getApplication();
		AntoProjectFinale eventHandler = new AntoProjectFinale();
		app.setAboutHandler(eventHandler);
		app.setQuitHandler(eventHandler);
		app.setDockIconImage(DOCK_ICON_IMAGE);
		app.setDefaultMenuBar(MenuHandler.getMenuBar(null));
		root = new WordProcessor();
	}

	/**
	 * An about window pops up.
	 */
	public void handleAbout(AppEvent.AboutEvent e)
	{
		JOptionPane.showMessageDialog(null, ABOUT_MESSAGE);
	}

	/**
	 * Quits the application.
	 */
	public void handleQuitRequestWith(AppEvent.QuitEvent e, QuitResponse response)
	{
		if (root.quit())
			response.performQuit();
		else
			response.cancelQuit();
	}

	/**
	 * Acts appropriately according to the command
	 * @param command	the command sent by the menu
	 */
	public static void menuActionPerformed(String command)
	{
		if (command.equals(MenuHandler.NEW))
			root.newDocument();
		else if (command.equals(MenuHandler.OPEN))
			root.open();
		else if (command.equals(MenuHandler.HELP))
			WordProcessor.help();
	}
}

/**
 * This is the word processor that does most of the work of this program.
 */
class WordProcessor extends JFrame implements ActionListener, WindowListener, UndoableEditListener
{
	// Here to facilitate closing mechanism
	private WordProcessor[] children;

	// Components of the word processor
	private JTextArea textArea;
	private FileDialog fileDialog;
	private JComboBox fontName, fontSize;
	private JToggleButton italic, bold;
	private UndoManager undo;
	private JMenuBar menuBar; 

	// Final variables
	private static final long serialVersionUID = 1L;
	private static final Font DEFAULT_FONT = new Font("Dialog", Font.PLAIN, 12);
	private static final int ROWS = 100, COLUMNS = 100;
	private static final int UNDO_MANAGER_LIMIT = -1;
	private static final Icon BOLD_ICON = new javax.swing.ImageIcon("bold.png"), ITALIC_ICON = new javax.swing.ImageIcon("italic.png");
	private static final Integer[] FONT_SIZES = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
	private static final String FONT_SIZE_CHANGE_ERROR_MESSAGE = "Must be a positive integer <= Integer.MAX_VALUE.";
	private static final String UNTITLED = "Untitled";

	/**
	 * Initializes the word processor.
	 */
	public WordProcessor()
	{
		super(UNTITLED);
		add(getToolBar(), BorderLayout.PAGE_START);
		textArea = new JTextArea(ROWS, COLUMNS);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setFont(DEFAULT_FONT);
		textArea.getDocument().addUndoableEditListener(this);
		add(new JScrollPane(textArea));
		menuBar = MenuHandler.getMenuBar(this);
		setJMenuBar(menuBar);
		pack();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		fileDialog = new FileDialog(this);
		fileDialog.setDirectory("/Users/" + System.getProperty("user.name"));
		undo = new UndoManager();
		undo.setLimit(UNDO_MANAGER_LIMIT);
		updateUndoRedoState();
		setVisible(true);
	}

	/**
	 * Initializes the font toolbar.
	 * @return	the toolbar
	 */
	private JToolBar getToolBar()
	{
		JToolBar toolBar = new JToolBar();
		fontName = new JComboBox(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
		fontSize = new JComboBox(FONT_SIZES);
		bold = new JToggleButton(BOLD_ICON);
		italic = new JToggleButton(ITALIC_ICON);
		fontName.setSelectedItem(DEFAULT_FONT.getName());
		fontSize.setSelectedItem(DEFAULT_FONT.getSize());
		fontName.setEditable(true);
		fontSize.setEditable(true);
		fontName.addActionListener(this);
		fontSize.addActionListener(this);
		italic.addActionListener(this);
		bold.addActionListener(this);
		toolBar.add(fontName);
		toolBar.add(fontSize);
		toolBar.add(bold);
		toolBar.add(italic);
		return toolBar;
	}

	/**
	 * Changes the font.
	 */
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			int size = Integer.parseInt(fontSize.getSelectedItem().toString());
			if (size <= 0)
				throw new NumberFormatException();
			String name = fontName.getSelectedItem().toString();
			int style = Font.PLAIN + (bold.isSelected() ? Font.BOLD : 0) + (italic.isSelected() ? Font.ITALIC : 0);
			textArea.setFont(new Font(name, style, size));
		}
		catch (NumberFormatException cce)
		{
			JOptionPane.showMessageDialog(this, FONT_SIZE_CHANGE_ERROR_MESSAGE);
			fontSize.setSelectedItem(textArea.getFont().getSize());
		}
	}

	/**
	 * Acts appropriately according to the command.
	 * @param command	the command sent by the menu
	 */
	public void menuActionPerformed(String command)
	{
		if (command.equals(MenuHandler.NEW))
			newDocument();
		else if (command.equals(MenuHandler.OPEN))
			open();
		else if (command.equals(MenuHandler.SAVE))
			save();
		else if (command.equals(MenuHandler.CLOSE))
			close();
		else if (command.equals(MenuHandler.PRINT))
			print();
		else if (command.equals(MenuHandler.UNDO))
			undo();
		else if (command.equals(MenuHandler.REDO))
			redo();
		else if (command.equals(MenuHandler.BOLD))
			bold.doClick();
		else if (command.equals(MenuHandler.ITALIC))
			italic.doClick();
		else if (command.equals(MenuHandler.DECREASE))
			fontSize.setSelectedItem(Integer.parseInt(fontSize.getSelectedItem().toString()) - 1);
		else if (command.equals(MenuHandler.INCREASE))
			fontSize.setSelectedItem(Integer.parseInt(fontSize.getSelectedItem().toString()) + 1);
		else if (command.equals(MenuHandler.MINIMIZE))
			setExtendedState(ICONIFIED);
		else if (command.equals(MenuHandler.ZOOM))
			zoom();
		else if (command.equals(MenuHandler.HELP))
			help();
	}

	/**
	 * Quits the program by closing all the windows.
	 * @return	If all windows are closed and disposed, then return true. Otherwise, return false.
	 */
	public boolean quit()
	{
		if (children != null)
			for (int child = 0; child < children.length; child++)
				if (!children[child].quit())
					return false;
		return !isVisible() || close();
	}

	/**
	 * Opens a new document and adds it to the array.
	 */
	public void newDocument()
	{
		if (children == null)
			children = new WordProcessor[] {new WordProcessor()};
		else
		{
			WordProcessor[] newChildren = new WordProcessor[children.length + 1];
			System.arraycopy(children, 0, newChildren, 0, children.length);
			newChildren[children.length] = new WordProcessor();
			children = newChildren;
		}
	}

	/**
	 * Opens a document from a file.
	 */
	public void open()
	{
		fileDialog.setMode(FileDialog.LOAD);
		fileDialog.setVisible(true);
		String file = fileDialog.getFile();
		if (file != null)
		{
			try
			{
				Scanner in = new Scanner(new BufferedReader(new FileReader(fileDialog.getDirectory() + file))).useDelimiter("\0");
				String text = in.next();
				newDocument();
				children[children.length - 1].textArea.setText(text);
				children[children.length - 1].setTitle(file);
				in.close();
			}
			catch (FileNotFoundException e)
			{
				JOptionPane.showMessageDialog(null, "Unable to open file.");
			}
		}
	}

	/**
	 * Saves the document to a file.
	 * @return	whether or not the file is successfully saved.
	 */
	private boolean save()
	{
		fileDialog.setMode(FileDialog.SAVE);
		fileDialog.setVisible(true);
		String file = fileDialog.getFile();
		if (file != null)
		{
			try
			{
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileDialog.getDirectory() + file)));
				out.write(textArea.getText());
				setTitle(file);
				out.close();
				return true;
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null, "Unable to save file.");
			}
		}
		return false;
	}

	/**
	 * Closes the window.
	 * @return	If closed, return true. If not, return false.
	 */
	private boolean close()
	{
		int pick = JOptionPane.showConfirmDialog(this, "Do you want to save \"" + getTitle() + "\"?");
		setVisible((pick == JOptionPane.YES_OPTION && !save()) || pick != JOptionPane.NO_OPTION);
		return !isVisible();
	}

	/**
	 * Prints the document.
	 */
	private void print()
	{
		try
		{
			textArea.print();
		}
		catch (PrinterException e)
		{
			JOptionPane.showMessageDialog(null, "Could not print.");
		}
	}

	private void undo()
	{
		undo.undo();
		updateUndoRedoState();
	}

	private void redo()
	{
		undo.redo();
		updateUndoRedoState();
	}

	/**
	 * Zooms the window.
	 */
	private void zoom()
	{
		if (getExtendedState() == NORMAL)
			setExtendedState(MAXIMIZED_BOTH);
		else
			setExtendedState(NORMAL);
	}

	/**
	 * Pops up a help dialog.
	 */
	public static void help()
	{
		boolean correct = "12345678".equals(JOptionPane.showInputDialog(null, "What is 846 * 14593?"));
		JOptionPane.showMessageDialog(null, correct ? "You don't need help." : "I can't help you.");
	}

	public void undoableEditHappened(UndoableEditEvent e)
	{
		undo.addEdit(e.getEdit());
		updateUndoRedoState();
	}

	private void updateUndoRedoState()
	{
		int menuCount = menuBar.getMenuCount();
		for (int numMenu = 0; numMenu < menuCount; numMenu++)
		{
			JMenu menu = menuBar.getMenu(numMenu);
			int itemCount = menu.getItemCount();
			for (int numItem = 0; numItem < itemCount; numItem++)
			{
				JMenuItem item = menu.getItem(numItem);
				if (item != null)
				{
					String name = item.getText();
					if (name.equals(MenuHandler.UNDO))
						item.setEnabled(undo.canUndo());
					else if (name.equals(MenuHandler.REDO))
						item.setEnabled(undo.canRedo());
				}
			}
		}
	}

	/**
	 * This is what happens when I try to press the red button on the window.
	 */
	public void windowClosing(WindowEvent e)
	{
		close();
	}

	/**
	 * These are extra methods that I do not use.
	 */
	public void windowActivated(WindowEvent e)
	{
	}
	public void windowClosed(WindowEvent e)
	{
	}
	public void windowDeactivated(WindowEvent e)
	{
	}
	public void windowDeiconified(WindowEvent e)
	{
	}
	public void windowIconified(WindowEvent e)
	{
	}
	public void windowOpened(WindowEvent e)
	{
	}
}

/**
 * This class can make menu bars and perform actions when a menu item is pressed.
 */
class MenuHandler extends AbstractAction
{
	private static final long serialVersionUID = 1L;
	private static final int MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	public static final String NEW = "New";
	public static final String OPEN = "Open";
	public static final String SAVE = "Save";
	public static final String CLOSE = "Close";
	public static final String PRINT = "Print";
	public static final String UNDO = "Undo";
	public static final String REDO = "Redo";
	public static final String BOLD = "Toggle Bold";
	public static final String ITALIC = "Toggle Italic";
	public static final String DECREASE = "Decrease Font Size";
	public static final String INCREASE = "Increase Font Size";
	public static final String MINIMIZE = "Minimize";
	public static final String ZOOM = "Zoom";
	public static final String HELP = "Help";
	private static final String SPLITTER = "/";
	private static final String SEPARATOR = "separator";
	private static final String DEFAULT_MENU_ITEMS = NEW + OPEN + HELP;
	private static final String[] MENUS =
		{
		"File/NNew/OOpen/SSave/separator/WClose/PPrint",
		"Edit/ZUndo/YRedo/separator/BToggle Bold/IToggle Italic/[Decrease Font Size/]Increase Font Size",
		"Window/MMinimize/\0Zoom",
		"Help/\0Help"
		};
	private WordProcessor source;

	/**
	 * Creates a menu bar with the requested menus and menu items.
	 * @param actionSource		the word processor called when menu item is pressed
	 * @return					the menu bar
	 */
	public static JMenuBar getMenuBar(WordProcessor actionSource)
	{
		JMenuBar menuBar = new JMenuBar();
		for (int numMenu = 0; numMenu < MENUS.length; numMenu++)
		{
			String[] items = MENUS[numMenu].split(SPLITTER);
			JMenu menu = new JMenu(items[0]);
			for (int numItem = 1; numItem < items.length; numItem++)
			{
				String text = items[numItem];
				if (text.equals(SEPARATOR))
					menu.addSeparator();
				else
				{
					MenuHandler handler = new MenuHandler();
					String name = text.substring(1);
					handler.putValue(NAME, name);
					handler.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(text.charAt(0), MASK));
					handler.source = actionSource;
					handler.setEnabled(actionSource != null || DEFAULT_MENU_ITEMS.indexOf(name) >= 0);
					menu.add(handler);
				}
			}
			menuBar.add(menu);
		}
		return menuBar;
	}

	/**
	 * Called when menu item is pressed.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (source == null)
			AntoProjectFinale.menuActionPerformed((String) getValue(NAME));
		else
			source.menuActionPerformed((String) getValue(NAME));
	}
}