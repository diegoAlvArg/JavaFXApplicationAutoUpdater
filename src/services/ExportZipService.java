/*
 * 
 */
package services;

import application.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Duration;
import tools.ActionTool;
import tools.LoggGen;
import tools.NotificationType;
import tools.ResourceLeng;

/**
 * This class is used to import an XR3Player database (as .zip folder)
 * 
 * @author SuperGoliath
 *  
 *
 */
public class ExportZipService extends Service<Boolean> {

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The given ZIP file
     */
    private String zipFile;

    /**
     * The output folder
     */
    private String destinationFolder;

    /**
     * The exception.
     */
    private String exception;

    /**
     * Constructor.
     */
    public ExportZipService() {

        setOnSucceeded(s -> {

            //Check the value
            if (!getValue()) {
                ActionTool.showNotification(Main.getResourceBundle().getString(ResourceLeng.EXPORT_ZIP_TITLE), exception, Duration.seconds(2), NotificationType.ERROR);
//                ActionTool.showNotification("Exporting ZIP File", exception, Duration.seconds(2), NotificationType.ERROR);
                done();
            } else {
                ActionTool.showNotification(Main.getResourceBundle().getString(ResourceLeng.EXPORT_ZIP_TITLE),
                        Main.getResourceBundle().getString(ResourceLeng.EXPORT_ZIP_MSJ), Duration.seconds(2), NotificationType.INFORMATION);
//                ActionTool.showNotification("Exporting ZIP File", "Successfully exported the ZIP File", Duration.seconds(2), NotificationType.INFORMATION);
            }

        });

        setOnFailed(failed -> {
            done();
            ActionTool.showNotification(Main.getResourceBundle().getString(ResourceLeng.EXPORT_ZIP_TITLE), exception, Duration.seconds(2), NotificationType.ERROR);
//            ActionTool.showNotification("Exporting ZIP File", exception, Duration.seconds(2), NotificationType.ERROR);
        });

        setOnCancelled(c -> {
            done();
            ActionTool.showNotification(Main.getResourceBundle().getString(ResourceLeng.EXPORT_ZIP_TITLE), exception, Duration.seconds(2), NotificationType.ERROR);
//            ActionTool.showNotification("Exporting ZIP File", exception, Duration.seconds(2), NotificationType.ERROR);
        });
    }

    /**
     * Done.
     */
    private static void done() {

        //		Main.updateScreen.setVisible(false);
        //		Main.updateScreen.getProgressBar().progressProperty().unbind();
    }

    /**
     * This Services initialises and external Thread to Export a ZIP folder to a
     * Destination Folder
     *
     * @param zipFolder The absolute path of the ZIP folder
     * @param destinationFolder The absolutePath of the destination folder
     */
    public void exportZip(String zipFolder, String destinationFolder) {

        //-----
        this.zipFile = zipFolder;
        this.destinationFolder = destinationFolder;

        reset();
        restart();
    }

    /*
	 * (non-Javadoc)
	 * @see javafx.concurrent.Service#createTask()
     */
    @Override
    protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                ResourceBundle rb = Main.getResourceBundle();
//                System.out.println("FLAG_07 " + zipFile);
//                System.out.println("FLAG_08 " + destinationFolder);

                //---------------------Move on Importing the Database-----------------------------------------------
                // get the zip file content
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {

                    // create output directory is not exists
                    File folder = new File(destinationFolder);
                    if (!folder.exists()) {
                        folder.mkdir();
                    }

                    // get the zipped file list entry
                    ZipEntry ze = zis.getNextEntry();

                    // Count entries
                    ZipFile zip = new ZipFile(zipFile);
                    double counter = 0, total = zip.size();

                    //Start
                    for (byte[] buffer = new byte[1024]; ze != null;) {

                        String fileName = ze.getName();
                        File newFile = new File(destinationFolder + File.separator + fileName);

                        // Refresh the dataLabel text
                        updateMessage(rb.getString(ResourceLeng.EXPORT_LABEL) + newFile.getName() + " ]");
////                        updateMessage("Exporting: [ " + newFile.getName() + " ]");

                        // create all non exists folders else you will hit FileNotFoundException for compressed folder
                        new File(newFile.getParent()).mkdirs();

                        //Create File OutputStream
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {

                            // Copy byte by byte
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }

                        } catch (IOException ex) {
//                            System.err.println("FLAG_03");
                            exception = ex.getMessage();
                            StringWriter errors = new StringWriter();
                            ex.printStackTrace(new PrintWriter(errors));
                            LogRecord logRegistro = new LogRecord(Level.WARNING, rb.getString(ResourceLeng.TRACE_ERROR_DOWNLOAD)
                                    + errors.toString());
                            logRegistro.setSourceClassName(this.getClass().getName());
                            LoggGen.log(logRegistro);
//                            logger.log(Level.WARNING, "", ex);
                        }

                        //Get next entry
                        ze = zis.getNextEntry();

                        //Update the progress
                        updateProgress(++counter / total, 1);
                    }

                    zis.closeEntry();
                    zis.close();
                    zip.close();

                } catch (IOException ex) {
//                    System.err.println("FLAG_09");
                    exception = ex.getMessage();
                    StringWriter errors = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors));
                    LogRecord logRegistro = new LogRecord(Level.WARNING, rb.getString(ResourceLeng.TRACE_ERROR_ZIP)
                            + errors.toString());
                    logRegistro.setSourceClassName(this.getClass().getName());
                    LoggGen.log(logRegistro);
//                    logger.log(Level.WARNING, "", ex);
                    return false;
                }

                return true;
            }

        };
    }
}
