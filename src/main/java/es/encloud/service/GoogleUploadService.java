package es.encloud.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.drive.Drive;
import es.encloud.controller.UploadController;
import es.encloud.model.EncFile;
import javafx.concurrent.Task;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Created by jesus on 17/09/2014.
 */
public class GoogleUploadService extends Task<Void> {

    private static HttpTransport httpTransport;
    //    private static FileDataStoreFactory dataStoreFactory;
    private static MemoryDataStoreFactory memoryDataStoreFactory = MemoryDataStoreFactory.getDefaultInstance();
    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".store/encCloud");
    private static final String APPLICATION_NAME = "EncDrive";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    static {
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
//            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private EncFile file;
    private Credential credential;

    public GoogleUploadService(EncFile file, Credential credential) {
        this.file = file;
        this.credential = credential;
    }

    @Override
    protected Void call() throws Exception {
        try {
            upload(file, credential);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Upload files to the cloud.
     *
     * @return
     * @throws Exception
     */
    public static Credential authorize() throws Exception {
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(UploadController.class
                        .getResourceAsStream("/client_secrets.json")));
        if (clientSecrets.getDetails()
                .getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=plus "
                            + "into plus-cmdline-sample/src/main/resources/client_secrets.json");
            System.exit(1);
        }
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton("https://www.googleapis.com/auth/drive")).setDataStoreFactory(
                memoryDataStoreFactory).build();

        // authorize
        return new AuthorizationCodeInstalledApp(flow,
                new LocalServerReceiver()).authorize("user");
    }

    public void upload(EncFile file, HttpRequestInitializer credential) throws FileNotFoundException, IOException {
        class CustomProgressListener implements MediaHttpUploaderProgressListener {
            @Override
            public void progressChanged(MediaHttpUploader uploader) throws IOException {
                switch (uploader.getUploadState()) {
                    case INITIATION_STARTED:
                        System.out.println("Initiation has started!");
                        break;
                    case INITIATION_COMPLETE:
                        System.out.println("Initiation is complete!");
                        break;
                    case MEDIA_IN_PROGRESS:
                        updateProgress(uploader.getProgress(), 1);
                        break;
                    case MEDIA_COMPLETE:
                        updateProgress(1, 1);
                }
            }
        }

        File choseFile = new File(file.getFullPath());

        InputStreamContent mediaContent
                = new InputStreamContent(null,//"image/jpeg",
                new BufferedInputStream(new FileInputStream(choseFile)));
        mediaContent.setLength(choseFile.length());

        com.google.api.services.drive.model.File metadata = new com.google.api.services.drive.model.File();
        metadata.setTitle(choseFile.getName());

        Drive drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
        Drive.Files.Insert request = drive.files().insert(metadata, mediaContent);
        request.getMediaHttpUploader().setProgressListener(new CustomProgressListener());
        request.execute();

    }


}
