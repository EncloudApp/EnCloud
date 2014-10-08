package es.jesus.encloud.controls;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Created by jesus on 17/09/2014.
 */
public class WarningControl extends FlowPane {

    private Pane root;

    @FXML
    private Text textLabel;
    private String text;

    public WarningControl(Pane root, String text) {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/control/Warning.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        this.root = root;
        this.text = text;

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        AnchorPane.setLeftAnchor(this, 0d);
        AnchorPane.setRightAnchor(this, 0d);
    }

    public void show() {
        this.textLabel.setText(text);
        root.getChildren().add(this);
        // Fade show
        FadeTransition ft = new FadeTransition(Duration.millis(2000), this);
        ft.setFromValue(0.0);
        ft.setToValue(0.5);
        ft.setOnFinished(event -> {
            // Fade hide
            FadeTransition fade = new FadeTransition(Duration.millis(2000), this);
            fade.setFromValue(0.5);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> root.getChildren().remove(this));
            fade.play();
        });
        ft.play();
    }

}
