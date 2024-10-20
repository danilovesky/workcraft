package org.workcraft.utils;

import org.workcraft.Framework;
import org.workcraft.exceptions.OperationCancelledException;
import org.workcraft.gui.MainWindow;
import org.workcraft.interop.Format;
import org.workcraft.interop.FormatFileFilter;
import org.workcraft.workspace.FileFilters;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class DialogUtils {

    private static final int TRUNCATE_LENGTH = 100;
    private static final String MESSAGE_TITLE = "Message";
    private static final String INFO_TITLE = "Information";
    private static final String ERROR_TITLE = "Error";
    private static final String WARNING_TITLE = "Warning";
    private static final String INPUT_TITLE = "Input";

    private static final String CONFIG_FILE_CHOOSER_WIDTH = "filechooser.width";
    private static final String CONFIG_FILE_CHOOSER_HEIGHT = "filechooser.height";

    private static void logMessage(String message, int messageType) {
        if ((message != null) && !message.isEmpty()) {
            switch (messageType) {
            case JOptionPane.INFORMATION_MESSAGE:
                LogUtils.logInfo(message);
                break;
            case JOptionPane.WARNING_MESSAGE:
                LogUtils.logWarning(message);
                break;
            case JOptionPane.ERROR_MESSAGE:
                LogUtils.logError(message);
                break;
            default:
                LogUtils.logMessage(message);
                break;
            }
        }
    }

    public static void showMessage(String message, String title, int messageType, boolean log) {
        if (log) {
            logMessage(message, messageType);
        }
        Framework framework = Framework.getInstance();
        if (framework.isInGuiMode()) {
            MainWindow mainWindow = framework.getMainWindow();
            String text = TextUtils.truncateLines(message, TRUNCATE_LENGTH);
            JOptionPane.showMessageDialog(mainWindow, text, title, messageType);
        }
    }

    public static void showMessage(String message) {
        showMessage(message, MESSAGE_TITLE);
    }

    public static void showMessage(String message, String title) {
        showMessage(message, title, JOptionPane.PLAIN_MESSAGE, true);
    }

    public static void showInfo(String message) {
        showInfo(message, INFO_TITLE);
    }

    public static void showInfo(String message, String title) {
        showMessage(message, title, JOptionPane.INFORMATION_MESSAGE, true);
    }

    public static void showWarning(String message) {
        showWarning(message, WARNING_TITLE);
    }

    public static void showWarning(String message, String title) {
        showMessage(message, title, JOptionPane.WARNING_MESSAGE, true);
    }

    public static void showError(String message) {
        showError(message, ERROR_TITLE);
    }

    public static void showError(String message, String title) {
        showMessage(message, title, JOptionPane.ERROR_MESSAGE, true);
    }

    public static boolean showConfirm(String message, String question, String title, boolean defaultChoice) {
        return showConfirm(message, question, title, defaultChoice, JOptionPane.QUESTION_MESSAGE, true);
    }

    public static boolean showConfirmInfo(String message, String question) {
        return showConfirmInfo(message, question, INFO_TITLE, true);
    }

    public static boolean showConfirmInfo(String message, String question, String title, boolean defaultChoice) {
        return showConfirm(message, question, title, defaultChoice, JOptionPane.INFORMATION_MESSAGE, true);
    }

    public static boolean showConfirmWarning(String message, String question) {
        return showConfirmWarning(message, question, WARNING_TITLE, true);
    }

    public static boolean showConfirmWarning(String message, String question, String title, boolean defaultChoice) {
        return showConfirm(message, question, title, defaultChoice, JOptionPane.WARNING_MESSAGE, true);
    }

    public static boolean showConfirmError(String message, String question) {
        return showConfirmError(message, question, ERROR_TITLE,  true);
    }

    public static boolean showConfirmError(String message, String question, String title, boolean defaultChoice) {
        return showConfirm(message, question, title, defaultChoice, JOptionPane.ERROR_MESSAGE, true);
    }

    public static boolean showConfirm(String message, String question, String title, boolean defaultChoice,
            int messageType, boolean log) {

        boolean result = defaultChoice;
        if (log) {
            logMessage(message, messageType);
        }
        Framework framework = Framework.getInstance();
        if (framework.isInGuiMode()) {
            MainWindow mainWindow = framework.getMainWindow();
            String yesText = UIManager.getString("OptionPane.yesButtonText");
            String noText = UIManager.getString("OptionPane.noButtonText");
            String[] options = {yesText, noText};
            int answer = JOptionPane.showOptionDialog(mainWindow, message + question, title,
                    JOptionPane.YES_NO_OPTION, messageType, null, options, defaultChoice ? yesText : noText);

            result = answer == JOptionPane.YES_OPTION;
        }
        return result;
    }

    public static String showInput(String message, String initial) {
        return showInput(message, INPUT_TITLE, initial);
    }

    public static String showInput(String message, String title, String initial) {
        Framework framework = Framework.getInstance();
        if (framework.isInGuiMode()) {
            MainWindow mainWindow = framework.getMainWindow();
            return (String) JOptionPane.showInputDialog(mainWindow, message, title,
                    JOptionPane.QUESTION_MESSAGE, null, null, initial);
        }
        return initial;
    }

    public static int showYesNoCancel(String message, String title, int defaultChoice) {
        String yesText = UIManager.getString("OptionPane.yesButtonText");
        String noText = UIManager.getString("OptionPane.noButtonText");
        String cancelText = UIManager.getString("OptionPane.cancelButtonText");
        return showChoice(message, title, yesText, noText, cancelText, defaultChoice);
    }

    private static int showChoice(String message, String title, String yesText, String noText, String cancelText,
            int defaultChoice) {

        int result = JOptionPane.CANCEL_OPTION;
        Framework framework = Framework.getInstance();
        if (framework.isInGuiMode()) {
            MainWindow mainWindow = framework.getMainWindow();
            String[] options = {yesText, noText, cancelText};
            result = JOptionPane.showOptionDialog(mainWindow, message, title, JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[defaultChoice]);
        }
        return result;
    }

    public static JFileChooser createFileOpener(String title, boolean allowWorkFiles, Format format) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setDialogTitle(title);
        boolean allowAllFileFilter = true;
        if (allowWorkFiles) {
            fc.setFileFilter(FileFilters.DOCUMENT_FILES);
            allowAllFileFilter = false;
        }
        if (format != null) {
            fc.addChoosableFileFilter(new FormatFileFilter(format));
            allowAllFileFilter = false;
        }
        fc.setCurrentDirectory(Framework.getInstance().getLastDirectory());
        fc.setAcceptAllFileFilterUsed(allowAllFileFilter);
        return fc;
    }

    public static JFileChooser createFileSaver(String title, File file, Format format) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle(title);
        // Set file name
        fc.setSelectedFile(file);
        // Set working directory
        if (file.exists()) {
            fc.setCurrentDirectory(file.getParentFile());
        } else {
            fc.setCurrentDirectory(Framework.getInstance().getLastDirectory());
        }
        // Set file filters
        fc.setAcceptAllFileFilterUsed(false);
        if (format == null) {
            fc.setFileFilter(FileFilters.DOCUMENT_FILES);
        } else {
            fc.setFileFilter(new FormatFileFilter(format));
        }
        return fc;
    }

    public static File chooseValidSaveFileOrCancel(JFileChooser fc, Format format) throws OperationCancelledException {
        while (DialogUtils.showFileSaver(fc)) {
            String path = fc.getSelectedFile().getPath();
            if (format == null) {
                if (!FileFilters.isWorkPath(path)) {
                    path += FileFilters.DOCUMENT_EXTENSION;
                }
            } else {
                String extension = format.getExtension();
                if (!path.endsWith(extension)) {
                    path += extension;
                }
            }

            File file = new File(path);
            if (!file.exists()) {
                return file;
            }

            String message = "The file '" + file.getName() + "' already exists";
            String question = ".\nOverwrite it?";
            if (DialogUtils.showConfirmWarning(message, question, "Save work", false)) {
                return file;
            }
        }
        throw new OperationCancelledException();
    }

    public static boolean showFileOpener(JFileChooser fc) {
        Framework framework = Framework.getInstance();
        if (framework.isInGuiMode()) {
            MainWindow mainWindow = framework.getMainWindow();
            loadFileChooserSize(fc);
            int returnValue = fc.showOpenDialog(mainWindow);
            saveFileChooserSize(fc);
            return returnValue == JFileChooser.APPROVE_OPTION;
        }
        return false;
    }

    public static boolean showFileSaver(JFileChooser fc) {
        Framework framework = Framework.getInstance();
        if (framework.isInGuiMode()) {
            loadFileChooserSize(fc);
            MainWindow mainWindow = framework.getMainWindow();
            int returnValue = fc.showSaveDialog(mainWindow);
            saveFileChooserSize(fc);
            return returnValue == JFileChooser.APPROVE_OPTION;
        }
        return false;
    }

    private static void loadFileChooserSize(JFileChooser fc) {
        Framework framework = Framework.getInstance();

        String widthStr = framework.getConfigVar(CONFIG_FILE_CHOOSER_WIDTH, false);
        int width = ParseUtils.parseInt(widthStr, -1);

        String heightStr = framework.getConfigVar(CONFIG_FILE_CHOOSER_HEIGHT, false);
        int height = ParseUtils.parseInt(heightStr, -1);

        if ((width > 0) && (height > 0)) {
            Dimension size = new Dimension(width, height);
            fc.setPreferredSize(size);
        }
    }

    private static void saveFileChooserSize(JFileChooser fc) {
        Framework framework = Framework.getInstance();
        framework.setConfigVar(CONFIG_FILE_CHOOSER_WIDTH, Integer.toString(fc.getWidth()), false);
        framework.setConfigVar(CONFIG_FILE_CHOOSER_HEIGHT, Integer.toString(fc.getHeight()), false);
    }

}
