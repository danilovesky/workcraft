package org.workcraft.gui;

import org.workcraft.Framework;
import org.workcraft.commands.Command;
import org.workcraft.commands.MenuOrdering.Position;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.OperationCancelledException;
import org.workcraft.gui.actions.Action;
import org.workcraft.gui.actions.ActionCheckBoxMenuItem;
import org.workcraft.gui.actions.ActionMenuItem;
import org.workcraft.gui.tabs.DockableWindow;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Format;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.plugins.PluginManager;
import org.workcraft.utils.CommandUtils;
import org.workcraft.workspace.WorkspaceEntry;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;

@SuppressWarnings("serial")
public class MainMenu extends JMenuBar {
    private static final String MENU_SECTION_PROMOTED_PREFIX = "!";

    private final MainWindow mainWindow;
    private final JMenu mnExport = new JMenu("Export");
    private final JMenu mnRecent = new JMenu("Open recent");
    private final JMenu mnToolbars = new JMenu("Toolbars");
    private final JMenu mnWindows = new JMenu("Windows");
    private final HashMap<JToolBar, ActionCheckBoxMenuItem> toolbarItems = new HashMap<>();
    private final HashMap<DockableWindow, ActionCheckBoxMenuItem> windowItems = new HashMap<>();
    private final LinkedList<JMenu> mnCommandsList = new LinkedList<>();
    private final JMenu mnHelp = new JMenu("Help");

    MainMenu(final MainWindow mainWindow) {
        super();
        this.mainWindow = mainWindow;
        addFileMenu();
        addEditMenu();
        addViewMenu();
        addHelpMenu();
    }

    private void addFileMenu() {
        JMenu mnFile = new JMenu("File");
        mnFile.setMnemonic(KeyEvent.VK_F);
        add(mnFile);

        ActionMenuItem miNewModel = new ActionMenuItem(MainWindowActions.CREATE_WORK_ACTION);
        miNewModel.setMnemonic(KeyEvent.VK_N);
        mnFile.add(miNewModel);

        ActionMenuItem miOpenModel = new ActionMenuItem(MainWindowActions.OPEN_WORK_ACTION);
        miOpenModel.setMnemonic(KeyEvent.VK_O);
        mnFile.add(miOpenModel);
        mnFile.add(mnRecent);

        ActionMenuItem miMergeModel = new ActionMenuItem(MainWindowActions.MERGE_WORK_ACTION);
        miMergeModel.setMnemonic(KeyEvent.VK_M);
        mnFile.add(miMergeModel);

        mnFile.addSeparator();

        ActionMenuItem miSaveWork = new ActionMenuItem(MainWindowActions.SAVE_WORK_ACTION);
        miSaveWork.setMnemonic(KeyEvent.VK_S);
        mnFile.add(miSaveWork);

        ActionMenuItem miSaveWorkAs = new ActionMenuItem(MainWindowActions.SAVE_WORK_AS_ACTION);
        mnFile.add(miSaveWorkAs);

        ActionMenuItem miCloseActive = new ActionMenuItem(MainWindowActions.CLOSE_ACTIVE_EDITOR_ACTION);
        mnFile.add(miCloseActive);

        ActionMenuItem miCloseAll = new ActionMenuItem(MainWindowActions.CLOSE_ALL_EDITORS_ACTION);
        mnFile.add(miCloseAll);

        mnFile.addSeparator();

        ActionMenuItem miImport = new ActionMenuItem(MainWindowActions.IMPORT_ACTION);
        mnFile.add(miImport);
        mnFile.add(mnExport);

        mnFile.addSeparator();

        // FIXME: Workspace functionality is not working yet.
/*
        ActionMenuItem miNewWorkspace = new ActionMenuItem(WorkspaceWindowActions.NEW_WORKSPACE_AS_ACTION);
        mnFile.add(miNewWorkspace);

        ActionMenuItem miOpenWorkspace = new ActionMenuItem(WorkspaceWindowActions.OPEN_WORKSPACE_ACTION);
        mnFile.add(miOpenWorkspace);

        ActionMenuItem miAddFiles = new ActionMenuItem(WorkspaceWindowActions.ADD_FILES_TO_WORKSPACE_ACTION);
        mnFile.add(miAddFiles);

        ActionMenuItem miSaveWorkspace = new ActionMenuItem(WorkspaceWindowActions.SAVE_WORKSPACE_ACTION);
        mnFile.add(miSaveWorkspace);

        ActionMenuItem miSaveWorkspaceAs = new ActionMenuItem(WorkspaceWindowActions.SAVE_WORKSPACE_AS_ACTION);
        mnFile.add(miSaveWorkspaceAs);

        mnFile.addSeparator();
*/
        ActionMenuItem miShutdownGUI = new ActionMenuItem(MainWindowActions.SHUTDOWN_GUI_ACTION);
        mnFile.add(miShutdownGUI);
        mnFile.addSeparator();

        ActionMenuItem miExit = new ActionMenuItem(MainWindowActions.EXIT_ACTION);
        mnFile.add(miExit);
    }

    private void addExportSeparator(String text) {
        mnExport.add(new JLabel(text));
        mnExport.addSeparator();
    }

    private void addExporter(Exporter exporter) {
        Format format = exporter.getFormat();
        String text = format.getDescription() + " (*" + format.getExtension() + ")";
        Action action = new Action(text, () -> {
            try {
                Framework.getInstance().getMainWindow().export(exporter);
            } catch (OperationCancelledException e) {
            }
        });
        ActionMenuItem miExport = new ActionMenuItem(action);
        mnExport.add(miExport);
        mnExport.setEnabled(true);
    }

    private void addEditMenu() {
        JMenu mnEdit = new JMenu("Edit");
        mnEdit.setMnemonic(KeyEvent.VK_E);
        add(mnEdit);

        ActionMenuItem miUndo = new ActionMenuItem(MainWindowActions.EDIT_UNDO_ACTION);
        miUndo.setMnemonic(KeyEvent.VK_U);
        mnEdit.add(miUndo);

        ActionMenuItem miRedo = new ActionMenuItem(MainWindowActions.EDIT_REDO_ACTION);
        miRedo.setMnemonic(KeyEvent.VK_R);
        mnEdit.add(miRedo);

        mnEdit.addSeparator();

        ActionMenuItem miCut = new ActionMenuItem(MainWindowActions.EDIT_CUT_ACTION);
        miCut.setMnemonic(KeyEvent.VK_T);
        mnEdit.add(miCut);

        ActionMenuItem miCopy = new ActionMenuItem(MainWindowActions.EDIT_COPY_ACTION);
        miCopy.setMnemonic(KeyEvent.VK_C);
        mnEdit.add(miCopy);

        ActionMenuItem miPaste = new ActionMenuItem(MainWindowActions.EDIT_PASTE_ACTION);
        miPaste.setMnemonic(KeyEvent.VK_P);
        mnEdit.add(miPaste);

        ActionMenuItem miDelete = new ActionMenuItem(MainWindowActions.EDIT_DELETE_ACTION);
        miDelete.setMnemonic(KeyEvent.VK_D);
        // Add Backspace as an alternative shortcut for delete action (in addition to the Delete key).
        InputMap deleteInputMap = miDelete.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke backspace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
        deleteInputMap.put(backspace, MainWindowActions.EDIT_DELETE_ACTION);
        miDelete.getActionMap().put(MainWindowActions.EDIT_DELETE_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindowActions.EDIT_DELETE_ACTION.run();
            }
        });
        mnEdit.add(miDelete);

        mnEdit.addSeparator();

        ActionMenuItem miSelectAll = new ActionMenuItem(MainWindowActions.EDIT_SELECT_ALL_ACTION);
        miSelectAll.setMnemonic(KeyEvent.VK_A);
        mnEdit.add(miSelectAll);

        ActionMenuItem miSelectInverse = new ActionMenuItem(MainWindowActions.EDIT_SELECT_INVERSE_ACTION);
        miSelectInverse.setMnemonic(KeyEvent.VK_V);
        mnEdit.add(miSelectInverse);

        ActionMenuItem miSelectNone = new ActionMenuItem(MainWindowActions.EDIT_SELECT_NONE_ACTION);
        miSelectNone.setMnemonic(KeyEvent.VK_E);
        mnEdit.add(miSelectNone);

        mnEdit.addSeparator();

        ActionMenuItem miProperties = new ActionMenuItem(MainWindowActions.EDIT_SETTINGS_ACTION);
        mnEdit.add(miProperties);
    }

    private void addViewMenu() {
        JMenu mnView = new JMenu("View");
        mnView.setMnemonic(KeyEvent.VK_V);
        add(mnView);

        ActionMenuItem miZoomIn = new ActionMenuItem(MainWindowActions.VIEW_ZOOM_IN);
        mnView.add(miZoomIn);

        ActionMenuItem miZoomOut = new ActionMenuItem(MainWindowActions.VIEW_ZOOM_OUT);
        mnView.add(miZoomOut);

        ActionMenuItem miZoomDefault = new ActionMenuItem(MainWindowActions.VIEW_ZOOM_DEFAULT);
        mnView.add(miZoomDefault);

        ActionMenuItem miZoomFit = new ActionMenuItem(MainWindowActions.VIEW_ZOOM_FIT);
        mnView.add(miZoomFit);

        mnView.addSeparator();

        ActionMenuItem miPanCenter = new ActionMenuItem(MainWindowActions.VIEW_PAN_CENTER);
        mnView.add(miPanCenter);

        ActionMenuItem miPanLeft = new ActionMenuItem(MainWindowActions.VIEW_PAN_LEFT);
        mnView.add(miPanLeft);

        ActionMenuItem miPanUp = new ActionMenuItem(MainWindowActions.VIEW_PAN_UP);
        mnView.add(miPanUp);

        ActionMenuItem miPanRight = new ActionMenuItem(MainWindowActions.VIEW_PAN_RIGHT);
        mnView.add(miPanRight);

        ActionMenuItem miPanDown = new ActionMenuItem(MainWindowActions.VIEW_PAN_DOWN);
        mnView.add(miPanDown);

        mnView.addSeparator();

        mnView.add(mnToolbars);
        mnView.add(mnWindows);

        ActionMenuItem miResetLayout = new ActionMenuItem(MainWindowActions.RESET_GUI_ACTION);
        mnView.add(miResetLayout);
    }

    private void addHelpMenu() {
        mnHelp.setText("Help");
        mnHelp.setMnemonic(KeyEvent.VK_H);
        add(mnHelp);

        ActionMenuItem miOverview = new ActionMenuItem(MainWindowActions.HELP_OVERVIEW_ACTION);
        mnHelp.add(miOverview);

        ActionMenuItem miContents = new ActionMenuItem(MainWindowActions.HELP_CONTENTS_ACTION);
        mnHelp.add(miContents);

        ActionMenuItem miTutorials = new ActionMenuItem(MainWindowActions.HELP_TUTORIALS_ACTION);
        mnHelp.add(miTutorials);

        mnHelp.addSeparator();

        ActionMenuItem miBugreport = new ActionMenuItem(MainWindowActions.HELP_BUGREPORT_ACTION);
        mnHelp.add(miBugreport);

        ActionMenuItem miQuestion = new ActionMenuItem(MainWindowActions.HELP_EMAIL_ACTION);
        mnHelp.add(miQuestion);

        mnHelp.addSeparator();

        ActionMenuItem miAbout = new ActionMenuItem(MainWindowActions.HELP_ABOUT_ACTION);
        mnHelp.add(miAbout);
    }

    private void setExportMenu(final WorkspaceEntry we) {
        mnExport.removeAll();
        mnExport.setEnabled(false);

        VisualModel model = we.getModelEntry().getVisualModel();
        final Framework framework = Framework.getInstance();
        PluginManager pluginManager = framework.getPluginManager();
        Collection<PluginInfo<? extends Exporter>> plugins = pluginManager.getExporterPlugins();

        boolean hasVisualModelExporter = false;
        for (PluginInfo<? extends Exporter> info : plugins) {
            Exporter exporter = info.getSingleton();
            if (exporter.isCompatible(model)) {
                if (!hasVisualModelExporter) {
                    addExportSeparator("Visual model");
                }
                addExporter(exporter);
                hasVisualModelExporter = true;
            }
        }

        boolean hasMathModelExporter = false;
        for (PluginInfo<? extends Exporter> info : plugins) {
            Exporter exporter = info.getSingleton();
            if (exporter.isCompatible(model.getMathModel())) {
                if (!hasMathModelExporter) {
                    addExportSeparator("Math model");
                }
                addExporter(exporter);
                hasMathModelExporter = true;
            }
        }
        revalidate();
    }

    public void setExportMenuState(boolean enable) {
        mnExport.setEnabled(enable);
    }

    public final void registerToolbar(JToolBar toolbar) {
        Action action = new Action(toolbar.getName(), () -> toolbar.setVisible(!toolbar.isVisible()));
        ActionCheckBoxMenuItem miToolbarItem = new ActionCheckBoxMenuItem(action);
        miToolbarItem.setSelected(toolbar.isVisible());
        toolbarItems.put(toolbar, miToolbarItem);
        mnToolbars.add(miToolbarItem);
    }

    public final void registerUtilityWindow(DockableWindow dockableWindow) {
        MainWindow mainWindow = Framework.getInstance().getMainWindow();
        Action action = new Action(dockableWindow.getTitle(), () -> mainWindow.toggleDockableWindow(dockableWindow));
        ActionCheckBoxMenuItem miWindowItem = new ActionCheckBoxMenuItem(action);
        miWindowItem.setSelected(!dockableWindow.isClosed());
        windowItems.put(dockableWindow, miWindowItem);
        mnWindows.add(miWindowItem);
    }

    public final void updateRecentMenu() {
        ArrayList<String> entries = Framework.getInstance().getRecentFilePaths();
        mnRecent.removeAll();
        mnRecent.setEnabled(false);
        int index = 0;
        for (final String entry : entries) {
            if (entry != null) {
                JMenuItem miFile = new JMenuItem();
                if (index > 9) {
                    miFile.setText(entry);
                } else {
                    miFile.setText(index + ". " + entry);
                    miFile.setMnemonic(index + '0');
                    index++;
                }
                miFile.addActionListener(event -> mainWindow.openWork(new File(entry)));
                mnRecent.add(miFile);
                mnRecent.setEnabled(true);
            }
        }
        mnRecent.addSeparator();
        JMenuItem miClear = new JMenuItem("Clear the list");
        miClear.addActionListener(event -> {
            Framework framework = Framework.getInstance();
            framework.clearRecentFilePaths();
            updateRecentMenu();
        });
        mnRecent.add(miClear);
    }

    public final void setToolbarVisibility(JToolBar toolbar, boolean selected) {
        ActionCheckBoxMenuItem mi = toolbarItems.get(toolbar);
        if (mi != null) {
            mi.setSelected(selected);
        }
    }

    public final void setWindowVisibility(DockableWindow window, boolean selected) {
        ActionCheckBoxMenuItem mi = windowItems.get(window);
        if (mi != null) {
            mi.setSelected(selected);
        }
    }

    private void createCommandsMenu(final WorkspaceEntry we) {
        removeCommandsMenu();
        if (we == null) {
            return;
        }

        List<Command> applicableVisibleCommands = CommandUtils.getApplicableVisibleCommands(we);
        List<String> sections = CommandUtils.getSections(applicableVisibleCommands);

        JMenu mnCommands = new JMenu("Tools");
        mnCommands.setMnemonic(KeyEvent.VK_T);
        mnCommandsList.clear();
        for (String section : sections) {
            JMenu mnSection = mnCommands;
            if (!section.isEmpty()) {
                mnSection = new JMenu(section);
                if (isPromotedSection(section)) {
                    String menuName = getMenuNameFromSection(section);
                    mnSection.setText(menuName);
                    mnCommandsList.add(mnSection);
                } else {
                    mnCommands.add(mnSection);
                    mnCommandsList.addFirst(mnCommands);
                }
            }
            List<Command> sectionCommands = CommandUtils.getSectionCommands(section, applicableVisibleCommands);
            addCommandMenuSection(mnSection, sectionCommands);
        }
        addCommandsMenu();
    }

    private void addCommandMenuSection(JMenu mnSection, List<Command> sectionCommands) {
        List<List<Command>> sectionCommandsPartitions = new LinkedList<>();
        sectionCommandsPartitions.add(CommandUtils.getUnpositionedCommands(sectionCommands));
        for (Position position: Position.values()) {
            sectionCommandsPartitions.add(CommandUtils.getPositionedCommands(sectionCommands, position));
        }
        boolean needSeparator = false;
        for (List<Command> sectionCommandsPartition : sectionCommandsPartitions) {
            boolean isFirstItem = true;
            for (Command command : sectionCommandsPartition) {
                if (needSeparator && isFirstItem) {
                    mnSection.addSeparator();
                }
                needSeparator = true;
                isFirstItem = false;
                Action action = new Action(command.getDisplayName().trim(), () -> CommandUtils.run(mainWindow, command));
                ActionMenuItem miCommand = new ActionMenuItem(action);
                mnSection.add(miCommand);
            }
        }
    }

    public static boolean isPromotedSection(String section) {
        return (section != null) && section.startsWith(MENU_SECTION_PROMOTED_PREFIX);
    }

    public static String getMenuNameFromSection(String section) {
        String result = "";
        if (section != null) {
            if (section.startsWith(MENU_SECTION_PROMOTED_PREFIX)) {
                result = section.substring(MENU_SECTION_PROMOTED_PREFIX.length());
            } else {
                result = section;
            }
        }
        return result.trim();
    }

    private void addCommandsMenu() {
        for (JMenu mnCommands : mnCommandsList) {
            add(mnCommands);
        }
        remove(mnHelp);
        add(mnHelp);
        revalidate();
    }

    public void removeCommandsMenu() {
        for (JMenu mnCommands : mnCommandsList) {
            remove(mnCommands);
        }
        revalidate();
    }

    public void updateCommandsMenuState(boolean enable) {
        for (JMenu mnCommands : mnCommandsList) {
            mnCommands.setEnabled(enable);
        }
    }

    public void setMenuForWorkspaceEntry(final WorkspaceEntry we) {
        if (we != null) {
            createCommandsMenu(we);
            setExportMenu(we);
            we.updateActionState();
        }
    }

}
