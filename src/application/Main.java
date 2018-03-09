package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import presenter.DownloadModeController;
import services.DownloadService;
import services.ExportZipService;
import tools.ActionTool;
import tools.InfoTool;
import tools.NotificationType;
import tools.ResourceLeng;

/**
 * @version 1.0
 * @author goxr3plus
 * @see https://github.com/goxr3plus/JavaFXApplicationAutoUpdater
 *
 * @version 1.1.0
 * @author Diego Alvarez Limpieza de codigo, documentacion y localizacion
 */
public class Main extends Application {

    //================Variables================
    /**
     * This is the folder where the update will take place [ obviously the
     * parent folder of the application]
     */
    private File updateFolder = new File(InfoTool.getBasePathForClass(Main.class));

    /**
     * Download update as a ZIP Folder , this is the prefix name of the ZIP
     * folder
     */
    private static String foldersNamePrefix;

    /**
     * Update to download
     */
    private static int update;

    /**
     * The name of the application you want to update
     */
    private String applicationName;

    /**
     * Give String in the location
     */
    private static ResourceBundle rb;

    private static final String DOWNLOAD_SEED
            = "https://github.com/diegoAlvArg/Updater/releases/download/V0.0%o/HelloWorld.0%o.zip";
    //================Listeners================
    //Create a change listener
    ChangeListener<? super Number> listener = (observable, oldValue, newValue) -> {
        if (newValue.intValue() == 1) {
            exportUpdate();
        }
    };
    //Create a change listener
    ChangeListener<? super Number> listener2 = (observable, oldValue, newValue) -> {
        if (newValue.intValue() == 1) {
            packageUpdate();
        }
    };

    //================Services================
    DownloadService downloadService;
    ExportZipService exportZipService;

    //=============================================
    private Stage window;
    private static DownloadModeController downloadMode = new DownloadModeController();

    //---------------------------------------------------------------------
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            rb = ResourceBundle.getBundle("languages.SystemMessages", Locale.getDefault());
        } catch (Exception e) {
            rb = ResourceBundle.getBundle("languages.SystemMessages", Locale.ENGLISH);
        }

        //Parse Arguments -> I want one parameter -> for example [45] which is the update i want
        List<String> applicationParameters = super.getParameters().getRaw();
        if (applicationParameters.isEmpty()) {
            update = 6;
//            update = Integer.valueOf(applicationParameters.get(0));
        } else {
            System.out.println(rb.getString(ResourceLeng.APP_NO_ARGS));
            System.exit(0);
        }

        //We need this in order to restart the update when it fails
        System.out.println(rb.getString(ResourceLeng.APP_UPDATER_INIT));

        // --------Window---------
        window = primaryStage;
        window.setResizable(false);
        window.centerOnScreen();
        window.getIcons().add(InfoTool.getImageFromResourcesFolder("icon.png"));
        window.centerOnScreen();
        window.setOnCloseRequest(exit -> {

            //Check
            if (exportZipService != null && exportZipService.isRunning()) {
                ActionTool.showNotification(rb.getString(ResourceLeng.MESSAGE_TXT),
                        rb.getString(ResourceLeng.APP_WARNIGN_EXIT), Duration.seconds(5), NotificationType.WARNING);
                exit.consume();
                return;
            }

            //Question
            if (!ActionTool.doQuestion(String.format(rb.getString(ResourceLeng.APP_CLOSE_ASK), applicationName), window)) {
                exit.consume();
            } else {

                //Delete the ZIP Folder
                deleteZipFolder();

                //Exit the application
                System.exit(0);
            }

        });

        // Scene
        Scene scene = new Scene(downloadMode);
        scene.getStylesheets().add(getClass().getResource(InfoTool.STYLES + InfoTool.APPLICATIONCSS).toExternalForm());
        window.setScene(scene);

        //Show
        window.show();

        //Start
        prepareForUpdate("HelloWorld");
    }

    //-------------------------------------------------------------------------------------------------------------------------------
    private String makeUpdaterFolder() {
        String carpeta = "nVersion";
        File theDir = new File(updateFolder.getAbsolutePath() + File.separator
                + carpeta);

        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("creating directory: " + theDir.getName());
            boolean result = false;

            try {
                theDir.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
            }
            if (result) {
                System.out.println("DIR created");
            }
        }
        return theDir.toString();
    }

    private static void deleteFolder(String _path) {
        String carpeta = "nVersion";
        File theDir = new File(_path + File.separator
                + carpeta);
// >>>>>       System.out.println("Deleting: " + _path + File.separator
//                + carpeta);
        if (theDir.exists()) {
            theDir.delete();
        }
    }

    /**
     * Prepare for the Update
     *
     * @param applicationName nombre de la aplicacion
     */
    public void prepareForUpdate(String applicationName) {
        this.applicationName = applicationName;
        window.setTitle(applicationName + rb.getString(ResourceLeng.UPDATER_2WORLD));

        //Check the Permissions
        if (checkPermissions()) {
            String directory = makeUpdaterFolder();
            //FoldersNamePrefix	
            foldersNamePrefix = directory + File.separator + applicationName + " Update Package " + update;
// >>>>>>>           System.out.println("foldersNamePrefix: " + foldersNamePrefix);
            downloadMode.getProgressLabel().setText(rb.getString(ResourceLeng.CHECK_PERMISSIONS));
// >>>>>>>           System.out.println("URL: " + String.format(DOWNLOAD_SEED, update, update));
            downloadUpdate(String.format(DOWNLOAD_SEED, update, update));;
        } else {

            //Update
            downloadMode.getProgressBar().setProgress(-1);
            downloadMode.getProgressLabel().setText(rb.getString(ResourceLeng.CLOSE_NICELY));

            //Show Message
            ActionTool.showNotification(rb.getString(ResourceLeng.ERROR_NO_PERMISSIONS),
                    String.format(rb.getString(ResourceLeng.ERROR_NO_PERMISSINOS_TXT), updateFolder.getAbsolutePath(), applicationName),
                    Duration.minutes(1), NotificationType.ERROR);
        }
    }

    /**
     * In order to update this application must have READ,WRITE AND CREATE
     * permissions on the current folder
     */
    public boolean checkPermissions() {

        //Check for permission to Create
        try {
            File sample = new File(updateFolder.getAbsolutePath() + File.separator + "empty123123124122354345436.txt");
            /*
			 * Create and delete a dummy file in order to check file
			 * permissions. Maybe there is a safer way for this check.
             */
            sample.createNewFile();
            sample.delete();
        } catch (IOException e) {
            //Error message shown to user. Operation is aborted
            return false;
        }

        //Also check for Read and Write Permissions
        return updateFolder.canRead() && updateFolder.canWrite();
    }

    /**
     * Try to download the Update
     *
     * @param downloadURL URL where the resource is locate
     */
    private void downloadUpdate(String downloadURL) {

//  >>>>>      System.out.println("downloadURL: " + downloadURL);
        if (InfoTool.isReachableByPing("www.google.com")) {

            //Download it
            try {
                //Delete the ZIP Folder
                deleteZipFolder();

                //Create the downloadService
                downloadService = new DownloadService();

                //Add Bindings
                downloadMode.getProgressBar().progressProperty().bind(downloadService.progressProperty());
                downloadMode.getProgressLabel().textProperty().bind(downloadService.messageProperty());
                downloadMode.getProgressLabel().textProperty().addListener((observable, oldValue, newValue) -> {
                    //Give try again option to the user
                    if (newValue.toLowerCase().contains("failed")) {
                        downloadMode.getFailedStackPane().setVisible(true);
                    }
                });
                downloadMode.getProgressBar().progressProperty().addListener(listener);
                window.setTitle(String.format(rb.getString(ResourceLeng.APP_TITLE), this.applicationName, this.update));

                //Start
                downloadService.startDownload(new URL(downloadURL), Paths.get(foldersNamePrefix + ".zip"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        } else {
            //Update
            downloadMode.getProgressBar().setProgress(-1);
            downloadMode.getProgressLabel().setText(rb.getString(ResourceLeng.ERROR_NO_INTERNET));

            //Delete the ZIP Folder
            deleteZipFolder();

            //Give try again option to the user
            downloadMode.getFailedStackPane().setVisible(true);
        }
    }

    /**
     * Exports the Update ZIP Folder
     */
    private void exportUpdate() {

        //Create the ExportZipService
        exportZipService = new ExportZipService();

        //Remove Listeners
        downloadMode.getProgressBar().progressProperty().removeListener(listener);

        //Add Bindings		
        downloadMode.getProgressBar().progressProperty().bind(exportZipService.progressProperty());
        downloadMode.getProgressLabel().textProperty().bind(exportZipService.messageProperty());
        downloadMode.getProgressBar().progressProperty().addListener(listener2);

        //Start it
        exportZipService.exportZip(foldersNamePrefix + ".zip", updateFolder.getAbsolutePath());
    }

    /**
     * After the exporting has been done i must delete the old update files and
     * add the new ones
     */
    private void packageUpdate() {

        //Remove Listeners
        downloadMode.getProgressBar().progressProperty().removeListener(listener2);

        //Bindings
        downloadMode.getProgressBar().progressProperty().unbind();
        downloadMode.getProgressLabel().textProperty().unbind();

        //Packaging
        downloadMode.getProgressBar().setProgress(-1);
        downloadMode.getProgressLabel().setText(String.format(rb.getString(ResourceLeng.LAUNCHING_APP), applicationName));

        //Delete the ZIP Folder
        deleteZipFolder();

        //Start XR3Player
        restartApplication(applicationName);

    }

    //---------------------------------------------------------------------------------------
    /**
     * Calling this method to start the main Application which is XR3Player
     *
     * @param appName name of application to restart
     */
    public static void restartApplication(String appName) {

        // Restart XR3Player
        new Thread(() -> {
            String path = InfoTool.getBasePathForClass(Main.class);
            String[] applicationPath = {new File(path + appName + ".jar").getAbsolutePath()};
//  >>>>           String[] applicationPath = {new File(path + "HelloWorld" + ".jar").getAbsolutePath()};
//  >>>>          String[] applicationPath = {new File(path + "nVersion" + File.separator + "HelloWorld" + ".jar").getAbsolutePath()};
            //Show message that application is restarting
            Platform.runLater(() -> ActionTool.showNotification(String.format(rb.getString(ResourceLeng.RESTART_TITLE), appName),
                    String.format(rb.getString(ResourceLeng.RESTART_TXT), applicationPath[0]), Duration.seconds(25),
                    NotificationType.INFORMATION));

            try {

                //Delete the ZIP Folder
//                deleteZipFolder();
                //------------Wait until Application is created
//  >>>>              System.out.println("Flag_02>>>> " + applicationPath[0]);
                File applicationFile = new File(applicationPath[0]);
                String waitLine = String.format(rb.getString(ResourceLeng.WAITING_TXT), appName);
                while (!applicationFile.exists()) {
                    Thread.sleep(50);
                    System.out.println(waitLine);
                }

//  >>>>              System.out.println(appName + " Path is : " + applicationPath[0]);
                //Create a process builder
                ProcessBuilder builder = new ProcessBuilder("java", "-jar", applicationPath[0], !"XR3PlayerUpdater".equals(appName) ? "" : String.valueOf(update));
//				                        System.out.println("CMD: " + builder);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // Wait n seconds
                PauseTransition pause = new PauseTransition(Duration.seconds(20));
                pause.setOnFinished(f -> Platform.runLater(() -> ActionTool.showNotification(
                        String.format(rb.getString(ResourceLeng.RESTART_TITLE_FAILED), appName),
                        String.format(rb.getString(ResourceLeng.RESTART_TXT_FAILED), applicationPath[0]),
                        Duration.seconds(10), NotificationType.ERROR)));
                pause.play();

                // Continuously Read Output to check if the main application started
                String line;
//  >>>>              System.out.println("CHECKING is alive");
                String mark_OK = rb.getString(ResourceLeng.APP_INIT);
                String mark_ERROR = rb.getString(ResourceLeng.ERROR);
                while (process.isAlive()) {
                    while ((line = bufferedReader.readLine()) != null) {
//  >>>>                      System.out.println("LINE: " + line);
                        if (line.isEmpty()) {
                            break;
                        } else if (line.contains(mark_OK)) {
                            //This line is being printed when XR3Player Starts 
                            //So the AutoUpdater knows that it must exit
//  >>>>                          deleteFolder(path);
                            System.exit(0);
                        } else if(line.contains(mark_ERROR)){
                            //Some kind of problem
                            throw new InterruptedException();
                        }
                    }
                }

            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, null, ex);

                // Show failed message
                Platform.runLater(() -> Platform.runLater(() -> ActionTool.showNotification(
                        String.format(rb.getString(ResourceLeng.RESTART_TITLE_FAILED), appName),
                        String.format(rb.getString(ResourceLeng.RESTART_TXT_FAILED), applicationPath[0]),
                        Duration.seconds(10), NotificationType.ERROR)));

            }
        }, "Start Application Thread").start();
    }

    /**
     * Delete the ZIP folder from the update
     *
     * @return True if deleted , false if not
     */
    public static boolean deleteZipFolder() {
//  >>>>>      System.out.println("Flag_01 DELETING>>>> " + foldersNamePrefix + ".zip");
        return new File(foldersNamePrefix + ".zip").delete();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
