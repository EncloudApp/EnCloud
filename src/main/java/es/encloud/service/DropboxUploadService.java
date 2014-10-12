package es.encloud.service;

import com.dropbox.core.*;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import es.encloud.model.EncFile;
import javafx.concurrent.Task;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Created by jesus on 20/09/2014.
 */
public class DropboxUploadService extends Task<Void> {

    private EncFile file;
    private String credential;
    public DropboxUploadService(EncFile file, String credential){
        this.file = file;
        this.credential = credential;

    }

    @Override
    protected Void call() throws Exception {
        try {
            upload();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String authenticate() throws DbxException, URISyntaxException, IOException, DbxWebAuth.NotApprovedException, DbxWebAuth.BadRequestException, DbxWebAuth.BadStateException, DbxWebAuth.CsrfException, DbxWebAuth.ProviderException {
        // Get your app key and secret from the Dropbox developers website.
        final String APP_KEY = "75cdqk0p5hx15v9";
        final String APP_SECRET = "3vp5jpwev59uh14";
        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

        DbxRequestConfig config = new DbxRequestConfig(
                "JavaTutorial/1.0", Locale.getDefault().toString());
//        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
        // Lets reuse google's server receiver to get the dropbox auth code.
        LocalServerReceiver server =  new LocalServerReceiver.Builder().setPort(58888).build();
        DbxSessionStore store = new DbxSessionStore() {

            String key;

            @Override
            public String get() {
                return key;
            }

            @Override
            public void set(String value) {
                key = value;
            }

            @Override
            public void clear() {
                key = "";
            }
        };
        DbxWebAuth webAuth = new DbxWebAuth(config, appInfo, server.getRedirectUri(), store);

        String authorizeUrl = webAuth.start();
        // Open the browser with the authorize URL
        Desktop.getDesktop().browse(new URI(authorizeUrl));
        String code = server.waitForCode();
        server.stop();
        System.out.println(code);
        Map<String, String[]> codes = new HashMap<>();
        codes.put("code",new String[]{code});
        codes.put("state",new String[]{store.get()});
        DbxAuthFinish authFinish = webAuth.finish(codes);
        return authFinish.accessToken;
    }

    /*
      DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

        DbxRequestConfig config = new DbxRequestConfig(
            "JavaTutorial/1.0", Locale.getDefault().toString());
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
     */

    private void upload() throws IOException, DbxException {
        DbxRequestConfig config = new DbxRequestConfig(
                "JavaTutorial/1.0", Locale.getDefault().toString());
        DbxClient client = new DbxClient(config, credential);

        File inputFile = new File(file.getFullPath());
        FileInputStream inputStream = new FileInputStream(inputFile);
        try {
            DbxEntry.File uploadedFile = client.uploadFile("/"+file.getName(),
                    DbxWriteMode.add(), inputFile.length(), inputStream);
            System.out.println("Uploaded: " + uploadedFile.toString());
        } finally {
            inputStream.close();
        }
    }


}
