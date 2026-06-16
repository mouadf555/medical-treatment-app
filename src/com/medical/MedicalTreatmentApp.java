package com.medical;

import com.medical.dao.Database;
import com.medical.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MedicalTreatmentApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialise la base de données
        Database.initialiserBase();

        // Construit et affiche l'interface
        MainView mainView = new MainView();
        Scene scene = mainView.buildScene(primaryStage);

        primaryStage.setTitle("Suivi de Traitements Médicaux");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        Database.fermer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}