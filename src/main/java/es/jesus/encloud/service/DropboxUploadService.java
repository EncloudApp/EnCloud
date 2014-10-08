package es.jesus.encloud.service;

import com.dropbox.core.*;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Created by jesus on 20/09/2014.
 */
public class DropboxUploadService {

    public void authenticate() throws DbxException, URISyntaxException, IOException {
        // Get your app key and secret from the Dropbox developers website.
        final String APP_KEY = "75cdqk0p5hx15v9";
        final String APP_SECRET = "3vp5jpwev59uh14";

        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

        DbxRequestConfig config = new DbxRequestConfig(
                "JavaTutorial/1.0", Locale.getDefault().toString());
//        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
        LocalServerReceiver server =
                new LocalServerReceiver.Builder().setPort(8888).build();
        DbxWebAuth webAuth = new DbxWebAuth(config, appInfo,server.getRedirectUri(), new DbxSessionStore(){

            @Override
            public String get() {
                return "3js34l3";
            }

            @Override
            public void set(String value) {

            }

            @Override
            public void clear() {

            }
        });

        String authorizeUrl = webAuth.start();
        Desktop.getDesktop().browse(new URI(authorizeUrl));

        String code = server.waitForCode();
        System.out.println(code);
//        DbxAuthFinish authFinish = webAuth.finish("code"); //TODO: get and write code
//        String accessToken = authFinish.accessToken;
    }

    /*
      DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

        DbxRequestConfig config = new DbxRequestConfig(
            "JavaTutorial/1.0", Locale.getDefault().toString());
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
     */

    public void updload() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
