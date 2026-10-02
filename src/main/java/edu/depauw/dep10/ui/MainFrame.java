package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextArea;

import com.formdev.flatlaf.util.SystemInfo;

import edu.depauw.dep10.simulator.Controller;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    static final String APP_TITLE = "DeP10 IDE";

    SourcePanel source;
    OutputPanel listing;
    OutputPanel object;
    TerminalPanel terminal;
    InputPanel batch;
    OutputPanel resourceView;
    OutputPanel tracePanel;
    StatePanel statePanel;
    
    private Controller control;

    private JComboBox<SourceType> sourceType;
    private JTabbedPane tabs;

    public MainFrame() {
        setSize(800, 600);
        setTitle(APP_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        source = new SourcePanel(this, "source");

        tabs = new JTabbedPane(JTabbedPane.TOP);

        listing = new OutputPanel("listing");
        tabs.add(listing.getTitle(), listing);

        object = new OutputPanel("object");
        tabs.add(object.getTitle(), object);

        terminal = new TerminalPanel("term");
        tabs.add(terminal.getTitle(), terminal);
        
        batch = new InputPanel("batch in");
        tabs.add(batch.getTitle(), batch);

        resourceView = new OutputPanel("resource");
        tabs.add(resourceView.getTitle(), resourceView);
        
        tracePanel = new OutputPanel("trace");
        tabs.add(tracePanel.getTitle(), tracePanel);
        
        statePanel = new StatePanel();
//        tabs.add(statePanel.getTitle(), statePanel);
        
        var hsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, statePanel);

        var vsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, source, hsplit);
        this.add(vsplit, BorderLayout.CENTER);

        var tools = new JToolBar(JToolBar.HORIZONTAL);

        SourceType[] sourceTypes = new SourceType[] {
                SourceType.Pep10UserFull,
                SourceType.Pep10UserBare,
                SourceType.Pep10System,
                SourceType.DeCLan
        };
        sourceType = new JComboBox<>(sourceTypes);
        sourceType.setMaximumSize(sourceType.getPreferredSize());
        tools.add(sourceType);
        tools.add(new JToolBar.Separator());

        var menuBar = createMenuBar(tools);
        setJMenuBar(menuBar);

        this.add(tools, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        vsplit.setDividerLocation(0.5);
        hsplit.setDividerLocation(0.5);
    }

    public JMenuBar createMenuBar(JToolBar tools) {
        var menuBar = new JMenuBar();

        // File Menu
        var fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(source.getNewAction()));
        fileMenu.add(new JMenuItem(source.getOpenDialogAction()));
        fileMenu.add(new JMenuItem(source.getSaveAction()));
        fileMenu.add(new JMenuItem(source.getSaveAsDialogAction()));
        menuBar.add(fileMenu);

        // Edit Menu
        var editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.DELETE_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)));
        editMenu.addSeparator();

        editMenu.add(createMenuItem(getIncreaseFontAction()));
        editMenu.add(createMenuItem(getDecreaseFontAction()));
        editMenu.add(createMenuItem(getResetFontAction()));

        menuBar.add(editMenu);

        // Search Menu
        var searchMenu = new JMenu("Search");
        searchMenu.add(new JMenuItem(source.getShowFindDialogAction())); // TODO createMenuItem?
        searchMenu.add(new JMenuItem(source.getShowReplaceDialogAction()));
        searchMenu.add(new JMenuItem(source.getGoToLineAction()));
        searchMenu.addSeparator();
        searchMenu.add(new JMenuItem(source.getShowFindBarAction()));
        searchMenu.add(new JMenuItem(source.getShowReplaceBarAction()));
        searchMenu.add(new JMenuItem(source.getHideSearchBarAction()));
        menuBar.add(searchMenu);

        // Simulator Menu
        var simulatorMenu = new JMenu("Simulator");

        var build = source.getBuildAction(listing, object);
        simulatorMenu.add(createMenuItem(build));
        tools.add(new JButton(build));

        var run = source.getRunAction();
        run.setEnabled(false); // not enabled until assembly successful
        simulatorMenu.add(createMenuItem(run));
        tools.add(new JButton(run));

        var debug = source.getDebugAction();
        debug.setEnabled(false);
        simulatorMenu.add(createMenuItem(debug));
        tools.add(new JButton(debug));
        
        var step = source.getStepAction();
        step.setEnabled(false);
        simulatorMenu.add(createMenuItem(step));
        tools.add(new JButton(step));
        
        var back = source.getBackAction();
        back.setEnabled(false);
        simulatorMenu.add(createMenuItem(back));
        tools.add(new JButton(back));

        menuBar.add(simulatorMenu);

        // Help Menu
        var helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem(viewHelp("Pep/10 Reference", "pep10ref.html")));
        helpMenu.add(new JMenuItem(viewHelp("DeCLan Grammar", "declan.html")));
        helpMenu.add(new JMenuItem(viewHelp("DeP10 Macro Reference", "dep10macroref.html")));
        helpMenu.addSeparator();
        helpMenu.add(new JMenuItem(viewResource("View Pep/10 Full OS Listing", "pep10os.pepl")));
        helpMenu.add(new JMenuItem(viewResource("View Pep/10 Bare Metal OS Listing", "pep10baremetal.pepl")));
        helpMenu.add(new JMenuItem(viewResource("View Standard Macros", "stdmacro.pep")));
        menuBar.add(helpMenu);

        if (!SystemInfo.isMacOS) {
            fileMenu.addSeparator();
            fileMenu.add(new JMenuItem(new AbstractAction("Exit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MainFrame.this.dispose(); // TODO check whether it is OK to close
                }
            }));

            editMenu.addSeparator();
            editMenu.add(new JMenuItem("Preferences..."));

            helpMenu.addSeparator();
            helpMenu.add(new JMenuItem(viewHelp("About...", "aboutdep10.html")));
        }

        return menuBar;
    }

    private Action getDecreaseFontAction() {
        var keyDecrease = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, getToolkit().getMenuShortcutKeyMaskEx());
        var decreaseSource = new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction();

        var result = new AbstractAction("Decrease Font Size") {
            @Override
            public void actionPerformed(ActionEvent e) {
                decreaseSource.actionPerformed(e);
                for (var tab : tabs.getComponents()) {
                    ((TabPanel) tab).setPanelFont(source.getPanelFont());
                }
                statePanel.setPanelFont(source.getPanelFont());
                SwingUtilities.updateComponentTreeUI(MainFrame.this);
            }
        };

        result.putValue(Action.ACCELERATOR_KEY, keyDecrease);
        return result;
    }

    private Action getIncreaseFontAction() {
        var keyIncrease = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, getToolkit().getMenuShortcutKeyMaskEx());
        var increaseSource = new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction();

        var result = new AbstractAction("Increase Font Size") {
            @Override
            public void actionPerformed(ActionEvent e) {
                increaseSource.actionPerformed(e);
                for (var tab : tabs.getComponents()) {
                    ((TabPanel) tab).setPanelFont(source.getPanelFont());
                }
                SwingUtilities.updateComponentTreeUI(MainFrame.this);
            }
        };

        result.putValue(Action.ACCELERATOR_KEY, keyIncrease);
        return result;
    }

    private Action getResetFontAction() {
        var keyReset = KeyStroke.getKeyStroke(KeyEvent.VK_0, getToolkit().getMenuShortcutKeyMaskEx());

        var result = new AbstractAction("Reset Font Size") {
            @Override
            public void actionPerformed(ActionEvent e) {
                source.setPanelFont(SourcePanel.DEFAULT_FONT);
                for (var tab : tabs.getComponents()) {
                    ((TabPanel) tab).setPanelFont(source.getPanelFont());
                }
                SwingUtilities.updateComponentTreeUI(MainFrame.this);
            }
        };

        result.putValue(Action.ACCELERATOR_KEY, keyReset);
        return result;
    }

    private Action viewHelp(String description, String resource) {
        return new AbstractAction(description) {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelpDialog(resource);
            }
        };
    }

    private Action viewResource(String description, String resource) {
        return new AbstractAction(description) {
            @Override
            public void actionPerformed(ActionEvent e) {
                resourceView.setContent(getResourceAsString(resource));
                tabs.setSelectedComponent(resourceView);
            }
        };
    }

    // Source:
    // https://github.com/bobbylight/RSyntaxTextArea/wiki/Example:-Adding-an-Edit-menu-for-the-editor
    private static JMenuItem createMenuItem(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setToolTipText(null); // Swing annoyingly adds tool tip text to the menu item
        return item;
    }

    public void selectListingTab() {
        tabs.setSelectedComponent(listing);
    }

    public void selectTerminalTab() {
        tabs.setSelectedComponent(terminal);
    }

    public void selectStateTab() {
        // Do nothing, since the State panel is not in the tabs
//        tabs.setSelectedComponent(statePanel);
    }

    public void updateTitle(String fileName) {
        setTitle(fileName + " - " + APP_TITLE);
    }

    public boolean canQuit() {
        return source.canQuit();
    }

    public SourceType getSourceType() {
        return sourceType.getItemAt(sourceType.getSelectedIndex());
    }
    
    public void setSourceType(SourceType type) {
        sourceType.setSelectedItem(type);
    }

    public String getResourceAsString(String resource) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            if (is != null) {
                try (
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader reader = new BufferedReader(isr)) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (IOException e1) {
            // TODO print a message?
        }
        return null;
    }

    public void showHelpDialog(String resource) {
        JEditorPane helpPane = new JEditorPane("text/html", getResourceAsString(resource));
        helpPane.setEditable(false);
        helpPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        helpPane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(helpPane);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Help Documentation",
                JOptionPane.PLAIN_MESSAGE);
    }

    public synchronized void setController(Controller control) {
        this.control = control;
    }
    
    public Controller getController() {
        return control;
    }
}
