package es.encloud.controls;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by jesus on 17/09/2014.
 */
public class PasswordControl extends FlowPane {


    private Pane root;

    private Consumer<String> passwordConsumer;

    @FXML
    private PasswordField password1;

    @FXML
    private PasswordField password2;


    public PasswordControl(AnchorPane root, Consumer<String> passwordConsumer) {
        this.root = root;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/control/Password.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        this.passwordConsumer = passwordConsumer;
        AnchorPane.setLeftAnchor(this, 0d);
        AnchorPane.setRightAnchor(this, 0d);
        AnchorPane.setTopAnchor(this, 0d);
        AnchorPane.setBottomAnchor(this, 0d);

    }


    public void show() {
        root.getChildren().add(this);
        TranslateTransition tt = new TranslateTransition(Duration.millis(1000), this);
        tt.setFromY(-200);
        tt.setToY(0);
        tt.play();
    }


    public void hide() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(1000), this);
        tt.setFromY(0);
        tt.setToY(-200);
        tt.play();
        tt.setOnFinished(event -> root.getChildren().remove(this));
    }

    @FXML
    public void handleCancelAction(ActionEvent event) {
        hide();
    }

    @FXML
    public void handleProceedAction(ActionEvent event) {
        if (!password1.getText().equals(password2.getText())) {
            password1.clear();
            password2.clear();
            return;
        }
        passwordConsumer.accept(password1.getText());
        hide();
    }


}
