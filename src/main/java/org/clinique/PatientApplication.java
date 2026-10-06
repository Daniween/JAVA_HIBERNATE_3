package org.clinique;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PatientApplication extends Application {

    @Override
    public void start(Stage fenetrePrincipale) throws IOException {
        FXMLLoader chargeur = new FXMLLoader(getClass().getResource("vue/patient-vue.fxml"));
        Parent racine = chargeur.load();

        fenetrePrincipale.setTitle("Gestion des patients - BTS SIO SLAM");
        fenetrePrincipale.setScene(new Scene(racine));
        fenetrePrincipale.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
