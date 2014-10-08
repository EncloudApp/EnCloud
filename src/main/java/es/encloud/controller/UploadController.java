package es.encloud.controller;

import com.dropbox.core.DbxException;
import com.google.api.client.auth.oauth2.Credential;
import es.encloud.controls.CompletedControl;
import es.encloud.controls.PasswordControl;
import es.encloud.controls.WarningControl;
import es.encloud.model.EncFile;
import es.encloud.service.DropboxUploadService;
import es.encloud.service.EncryptionService;
import es.encloud.service.GoogleUploadService;
import es.encloud.service.ZipService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedParameters")
public class UploadController implements Initializable {

    @FXML
    private TableView<EncFile> fileTable;

    @FXML
    AnchorPane root;

    private ObservableList<EncFile> fileList;

    @FXML
    public void handleAddFileAction(ActionEvent event) throws Exception {

        Stage stage = (Stage) root.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        List<File> filesChose = fileChooser.showOpenMultipleDialog(stage);


        if (filesChose != null) {
            filesChose.stream().map(
                    (fileChose)
                            -> new EncFile(fileChose)).forEach((encFile) -> {
                fileList.add(encFile);
            });
        }
        event.consume();
    }

    @FXML
    public void handelSelectAllAction(ActionEvent event) throws Exception {

        CheckBox combo = (CheckBox) event.getSource();
        fileList.stream().forEach(file -> {
            file.setSelected(combo.isSelected());
        });
        event.consume();
    }


    @FXML
    public void handleRemoveAction(ActionEvent event) throws Exception {
        fileList.removeIf(f -> f.getSelected());
        event.consume();
    }


    @FXML
    public void handleEncryptAction(ActionEvent event) throws Exception {
        // Discover if this is an encryption or decryption
        Button button = (Button) event.getSource();
        EncryptionService.Operation operation = button.getId().equals("enc") ? EncryptionService.Operation.ENCRYPT : EncryptionService.Operation.DECRYPT;
        if (operation == EncryptionService.Operation.DECRYPT) {
            // only enc files can be decoded. Unselect the rest
            fileList.stream().filter(f -> f.getSelected() && !f.getName().endsWith(".enc")).forEach(f -> f.setSelected(false));
        }
        // Validation
        if (!validateFile()) {
            return;
        }
        // Number of processed items.
        AtomicLong processed = new AtomicLong();
        long total = fileList.stream().filter(f -> f.getSelected()).count();

        ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        PasswordControl passwordPanel = new PasswordControl(root, password -> {
            for (EncFile file : fileList) {
                if (file.getSelected()) {

                    EncryptionService enc = new EncryptionService(
                            file,
                            password,
                            operation);

                    file.progressProperty().bind(enc.progressProperty());
                    // Increment the processed counter
                    enc.setOnSucceeded(e -> {
                        try {
                            fileList.add(enc.get());
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        } catch (ExecutionException e1) {
                            e1.printStackTrace();
                        }
                        if (processed.incrementAndGet() == total) {
                            // All the items have been processed
                            CompletedControl cc = new CompletedControl(root);
                            cc.show();
                        }
                    });
                    executor.execute(enc);
                    //executor.submit(enc);
                } else {
                    if (file.progressProperty().isBound()) {
                        file.progressProperty().unbind();
                    }
                    file.setProgress(0);
                }
            }
        });
        passwordPanel.show();
        event.consume();
    }

    @FXML
    public void handleZipAction(ActionEvent event) {
        // Validation
        if (!validateFile()) {
            return;
        }

        Stage stage = (Stage) root.getScene().getWindow();
        // Select the file where the zip file will be saved
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("zip files (*.zip)", "*.zip");
        chooser.setInitialFileName(UUID.randomUUID().toString() + ".zip");
        chooser.getExtensionFilters().add(extFilter);
        File target = chooser.showSaveDialog(stage);

        if (target != null) {

            List<EncFile> selectedFiles = new ArrayList<>();
            ZipService zs = new ZipService(selectedFiles, target, ZipService.Operation.ZIP);

            fileList.stream().filter(file -> file.getSelected()).forEach(file -> {
                selectedFiles.add(file);
                file.progressProperty().bind(zs.progressProperty());
            });
            fileList.stream().filter(file -> !file.getSelected()).forEach(file -> {
                        file.progressProperty().unbind();
                        file.setProgress(0);
                    }
            );
            zs.setOnSucceeded(e -> {
                CompletedControl cc = new CompletedControl(root);
                cc.show();
            });
            Thread t = new Thread(zs);
            t.setDaemon(true);
            t.start();
        }
        event.consume();
    }

    @FXML
    public void handleUnzipAction(ActionEvent event) {

        // only zip files can be unzipped. Unselect the rest
        fileList.stream().filter(f -> f.getSelected() && !f.getName().endsWith(".zip")).forEach(f -> f.setSelected(false));
        // Validation
        if (!validateFile()) {
            return;
        }
        Stage stage = (Stage) root.getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(stage);

        if (directory != null) {
            List<EncFile> selectedFiles = new ArrayList<>();
            ZipService zs = new ZipService(selectedFiles, directory, ZipService.Operation.UNZIP);

            fileList.stream().filter(file -> file.getSelected()).forEach(file -> {
                selectedFiles.add(file);
                file.progressProperty().bind(zs.progressProperty());
            });
            zs.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    CompletedControl cc = new CompletedControl(root);
                    cc.show();
                    t.consume();
                }
            });
            Thread t = new Thread(zs);
            t.setDaemon(true);
            t.start();
        }
        event.consume();
    }

    @FXML
    public void handleGoogleAction(ActionEvent event) throws Exception {

        // Validation
        if (!validateFile()) {
            return;
        }

        // Get google credentials
        Credential credential = GoogleUploadService.authorize();

        ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        fileList.stream().filter(f -> f.getSelected()).forEach(f -> {
            GoogleUploadService gus = new GoogleUploadService(f, credential);
            f.progressProperty().bind(gus.progressProperty());
            executor.execute(gus);
        });
        event.consume();
    }

    @FXML
    public void handleDropBoxAction(ActionEvent event) throws IOException, DbxException, URISyntaxException {
        DropboxUploadService db = new DropboxUploadService();
        db.authenticate();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Select Column
        TableColumn<EncFile, Boolean> selectColumn = (TableColumn<EncFile, Boolean>) fileTable.getColumns().get(0);
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        // Name Column
        TableColumn<EncFile, ?> nameColumn = fileTable.getColumns().get(1);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        // Type Column
        TableColumn<EncFile, ?> typeColumn = fileTable.getColumns().get(2);
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        // Size Column
        TableColumn<EncFile, ?> lengthColumn = fileTable.getColumns().get(3);
        lengthColumn.setCellValueFactory(new PropertyValueFactory<>("length"));
        // Progress Column
        TableColumn<EncFile, Double> progressColumn = (TableColumn<EncFile, Double>) fileTable.getColumns().get(4);
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressColumn.setCellFactory(ProgressBarTableCell.<EncFile>forTableColumn());

        // Bind the file collection to the table
        fileList = FXCollections.observableArrayList();
        fileTable.setItems(fileList);

    }

    @FXML
    public void handleTableDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            fileList.addAll(db.getFiles().stream().filter(file -> !file.isDirectory()).map(EncFile::new).collect(Collectors.toList()));
            success = true;
        }

        /*
        * Let the source know whether the string was successfully
        * transferred and used
        */
        event.setDropCompleted(success);

        event.consume();
    }

    @FXML
    public void handleTableDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
            event.acceptTransferModes(TransferMode.ANY);
        }

        event.consume();
    }

    private boolean validateFile() {
        if (fileList.stream().filter(f -> f.getSelected()).count() == 0) {
            WarningControl wc = new WarningControl(root, "At least one file has to be selected");
            wc.show();
            return false;
        }
        return true;
    }
}
