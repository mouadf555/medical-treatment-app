package com.medical.view;

import com.medical.dao.Database;
import com.medical.dao.PatientDAO;
import com.medical.dao.TraitementDAO;
import com.medical.model.Patient;
import com.medical.model.Traitement;
import com.medical.util.ExportUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MainView {

    // ── DAO ───────────────────────────────────────────────────────────────────
    private final PatientDAO    patientDAO    = new PatientDAO();
    private final TraitementDAO traitementDAO = new TraitementDAO();

    // ── Data ──────────────────────────────────────────────────────────────────
    private ObservableList<Patient>    patients    = FXCollections.observableArrayList();
    private ObservableList<Traitement> traitements = FXCollections.observableArrayList();

    // ── Widgets partagés ──────────────────────────────────────────────────────
    private TableView<Patient>    tablePatients;
    private TableView<Traitement> tableTraitements;
    private ListView<Patient>     listViewPatients;

    // Labels stats
    private Label lblTotalPatients, lblTraitementsActifs, lblSurveillance;

    // ── BUILD SCENE ───────────────────────────────────────────────────────────
    public Scene buildScene(Stage stage) {
        // Menu bar
        MenuBar menuBar = buildMenuBar(stage);

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabPatients    = new Tab("👤 Patients",    buildPatientsTab());
        Tab tabTraitements = new Tab("💊 Traitements", buildTraitementsTab());
        Tab tabStats       = new Tab("📊 Statistiques",buildStatsTab());
        Tab tabParametres  = new Tab("⚙ Paramètres",  buildParametresTab(stage));

        tabPane.getTabs().addAll(tabPatients, tabTraitements, tabStats, tabParametres);

        // Rafraîchir stats à chaque changement d'onglet
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nw) -> { if (nw == tabStats) rafraichirStats(); });

        VBox root = new VBox(menuBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        chargerPatients();
        return new Scene(root, 1100, 720);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MENU BAR
    // ═════════════════════════════════════════════════════════════════════════
    private MenuBar buildMenuBar(Stage stage) {
        MenuBar bar = new MenuBar();

        // Fichier
        Menu mFichier = new Menu("Fichier");
        MenuItem miExportPatients    = new MenuItem("Exporter patients (CSV)");
        MenuItem miExportTraitements = new MenuItem("Exporter traitements (CSV)");
        MenuItem miQuitter           = new MenuItem("Quitter");
        miExportPatients.setOnAction(e -> exporterPatients(stage));
        miExportTraitements.setOnAction(e -> exporterTraitements(stage));
        miQuitter.setOnAction(e -> { Database.fermer(); Platform.exit(); });
        mFichier.getItems().addAll(miExportPatients, miExportTraitements, new SeparatorMenuItem(), miQuitter);

        // Patients
        Menu mPatients = new Menu("Patients");
        MenuItem miAjouterPatient    = new MenuItem("Ajouter patient");
        MenuItem miSupprimerPatient  = new MenuItem("Supprimer patient sélectionné");
        miAjouterPatient.setOnAction(e -> ouvrirDialogPatient(null));
        miSupprimerPatient.setOnAction(e -> supprimerPatient());
        mPatients.getItems().addAll(miAjouterPatient, miSupprimerPatient);

        // Aide
        Menu mAide = new Menu("Aide");
        MenuItem miAPropos = new MenuItem("À propos");
        miAPropos.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("À propos");
            a.setHeaderText("Application de Suivi Médical");
            a.setContentText("Version 1.0\nDéveloppée avec JavaFX + MySQL\n© 2025");
            a.showAndWait();
        });
        mAide.getItems().add(miAPropos);

        bar.getMenus().addAll(mFichier, mPatients, mAide);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ONGLET PATIENTS
    // ═════════════════════════════════════════════════════════════════════════
    private Pane buildPatientsTab() {
        // ── Barre de recherche ─────────────────────────────────────────────
        TextField tfRecherche = new TextField();
        tfRecherche.setPromptText("🔍  Rechercher un patient…");
        tfRecherche.setPrefWidth(300);
        Tooltip.install(tfRecherche, new Tooltip("Tapez le nom ou prénom du patient"));

        Button btnAjouter   = new Button("➕ Ajouter");
        Button btnModifier  = new Button("✏ Modifier");
        Button btnSupprimer = new Button("🗑 Supprimer");
        styliserBoutons(btnAjouter, btnModifier, btnSupprimer);

        HBox toolbar = new HBox(10, tfRecherche, new Region(), btnAjouter, btnModifier, btnSupprimer);
        HBox.setHgrow(new Region(), Priority.ALWAYS);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        // fix region
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);

        // ── TableView ─────────────────────────────────────────────────────
        tablePatients = new TableView<>();
        tablePatients.setPlaceholder(new Label("Aucun patient enregistré."));

        TableColumn<Patient, Integer> colId     = new TableColumn<>("ID");
        TableColumn<Patient, String>  colNom    = new TableColumn<>("Nom");
        TableColumn<Patient, String>  colPrenom = new TableColumn<>("Prénom");
        TableColumn<Patient, String>  colAge    = new TableColumn<>("Âge");
        TableColumn<Patient, String>  colSexe   = new TableColumn<>("Sexe");
        TableColumn<Patient, String>  colTel    = new TableColumn<>("Téléphone");
        TableColumn<Patient, String>  colSurv   = new TableColumn<>("Surveillance");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colAge.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAge() + " ans"));
        colSexe.setCellValueFactory(new PropertyValueFactory<>("sexe"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colSurv.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isSousSurveillance() ? "⚠ Oui" : "Non"));

        colId.setPrefWidth(50); colNom.setPrefWidth(150); colPrenom.setPrefWidth(150);
        colAge.setPrefWidth(70); colSexe.setPrefWidth(80); colTel.setPrefWidth(130); colSurv.setPrefWidth(100);

        tablePatients.getColumns().addAll(colId, colNom, colPrenom, colAge, colSexe, colTel, colSurv);

        FilteredList<Patient> filteredPatients = new FilteredList<>(patients, p -> true);
        tablePatients.setItems(filteredPatients);

        tfRecherche.textProperty().addListener((obs, old, nw) ->
                filteredPatients.setPredicate(p ->
                        nw == null || nw.isEmpty() ||
                                p.getNomComplet().toLowerCase().contains(nw.toLowerCase())));

        // ── Accordion détail patient ───────────────────────────────────────
        TitledPane paneDetail = new TitledPane("Détail du patient sélectionné", new Label("Sélectionnez un patient."));
        paneDetail.setExpanded(false);
        Accordion accordion = new Accordion(paneDetail);

        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> {
            if (patient != null) {
                paneDetail.setContent(buildDetailPatient(patient));
                paneDetail.setExpanded(true);
            }
        });

        // ── Actions boutons ────────────────────────────────────────────────
        btnAjouter.setOnAction(e -> ouvrirDialogPatient(null));
        btnModifier.setOnAction(e -> {
            Patient sel = tablePatients.getSelectionModel().getSelectedItem();
            if (sel != null) ouvrirDialogPatient(sel);
            else afficherWarning("Sélection requise", "Veuillez sélectionner un patient à modifier.");
        });
        btnSupprimer.setOnAction(e -> supprimerPatient());

        VBox layout = new VBox(10, toolbar, tablePatients, accordion);
        VBox.setVgrow(tablePatients, Priority.ALWAYS);
        layout.setPadding(new Insets(5));
        return layout;
    }

    private VBox buildDetailPatient(Patient p) {
        Label lNom   = new Label("Nom complet : " + p.getNomComplet());
        Label lAge   = new Label("Âge : " + p.getAge() + " ans");
        Label lEmail = new Label("Email : " + (p.getEmail() != null ? p.getEmail() : "—"));
        Label lObs   = new Label("Observations : " + (p.getObservations() != null ? p.getObservations() : "—"));
        VBox v = new VBox(5, lNom, lAge, lEmail, lObs);
        v.setPadding(new Insets(8));
        return v;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ONGLET TRAITEMENTS
    // ═════════════════════════════════════════════════════════════════════════
    private SplitPane buildTraitementsTab() {
        // Filtre par patient + type
        listViewPatients = new ListView<>(patients);
        listViewPatients.setPrefWidth(200);
        listViewPatients.setPlaceholder(new Label("Aucun patient"));
        Label lblListePatients = new Label("Patients :");

        ComboBox<String> cbFiltreType = new ComboBox<>();
        cbFiltreType.getItems().addAll("Tous", "Antibiotique", "Antalgique", "Anti-inflammatoire",
                "Antihypertenseur", "Antidiabétique", "Autre");
        cbFiltreType.setValue("Tous");
        Tooltip.install(cbFiltreType, new Tooltip("Filtrer par type de traitement"));

        Button btnAjouterT   = new Button("➕ Ajouter");
        Button btnModifierT  = new Button("✏ Modifier");
        Button btnSupprimerT = new Button("🗑 Supprimer");
        styliserBoutons(btnAjouterT, btnModifierT, btnSupprimerT);

        HBox toolbarT = new HBox(10, new Label("Filtre type :"), cbFiltreType,
                new Region(), btnAjouterT, btnModifierT, btnSupprimerT);
        HBox.setHgrow(toolbarT.getChildren().get(2), Priority.ALWAYS);
        toolbarT.setPadding(new Insets(10));
        toolbarT.setAlignment(Pos.CENTER_LEFT);

        // TableView traitements
        tableTraitements = new TableView<>();
        tableTraitements.setPlaceholder(new Label("Aucun traitement."));

        TableColumn<Traitement, String>  colNomT    = new TableColumn<>("Traitement");
        TableColumn<Traitement, String>  colType    = new TableColumn<>("Type");
        TableColumn<Traitement, String>  colPoso    = new TableColumn<>("Posologie");
        TableColumn<Traitement, Integer> colPrises  = new TableColumn<>("Prises/j");
        TableColumn<Traitement, String>  colDebut   = new TableColumn<>("Début");
        TableColumn<Traitement, String>  colFin     = new TableColumn<>("Fin");
        TableColumn<Traitement, String>  colStatut  = new TableColumn<>("Statut");
        TableColumn<Traitement, String>  colProg    = new TableColumn<>("Progression");

        colNomT.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPoso.setCellValueFactory(new PropertyValueFactory<>("posologie"));
        colPrises.setCellValueFactory(new PropertyValueFactory<>("prisesParJour"));
        colDebut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateDebut() != null ? c.getValue().getDateDebut().toString() : ""));
        colFin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateFin() != null ? c.getValue().getDateFin().toString() : ""));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // ProgressBar dans la colonne Progression
        colProg.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar();
            { pb.setPrefWidth(100); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Traitement t = (Traitement) getTableRow().getItem();
                    pb.setProgress(t.getProgression());
                    setGraphic(pb);
                }
            }
        });

        colNomT.setPrefWidth(160); colType.setPrefWidth(120); colPoso.setPrefWidth(130);
        colPrises.setPrefWidth(70); colDebut.setPrefWidth(90); colFin.setPrefWidth(90);
        colStatut.setPrefWidth(80); colProg.setPrefWidth(110);

        tableTraitements.getColumns().addAll(colNomT, colType, colPoso, colPrises, colDebut, colFin, colStatut, colProg);
        tableTraitements.setItems(traitements);

        // Sélection patient => charge ses traitements
        listViewPatients.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> {
            if (patient != null) chargerTraitements(patient.getId(), cbFiltreType.getValue());
        });
        cbFiltreType.setOnAction(e -> {
            Patient sel = listViewPatients.getSelectionModel().getSelectedItem();
            if (sel != null) chargerTraitements(sel.getId(), cbFiltreType.getValue());
        });

        // Actions
        btnAjouterT.setOnAction(e -> {
            Patient sel = listViewPatients.getSelectionModel().getSelectedItem();
            if (sel == null) { afficherWarning("Sélection requise", "Sélectionnez d'abord un patient."); return; }
            ouvrirDialogTraitement(sel, null);
        });
        btnModifierT.setOnAction(e -> {
            Traitement sel = tableTraitements.getSelectionModel().getSelectedItem();
            Patient pat = listViewPatients.getSelectionModel().getSelectedItem();
            if (sel == null) { afficherWarning("Sélection requise", "Sélectionnez un traitement."); return; }
            ouvrirDialogTraitement(pat, sel);
        });
        btnSupprimerT.setOnAction(e -> supprimerTraitement());

        // Layout
        VBox leftPane = new VBox(5, lblListePatients, listViewPatients);
        leftPane.setPadding(new Insets(10));

        VBox rightPane = new VBox(5, toolbarT, tableTraitements);
        VBox.setVgrow(tableTraitements, Priority.ALWAYS);
        rightPane.setPadding(new Insets(5));

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.22);
        return split;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ONGLET STATISTIQUES
    // ═════════════════════════════════════════════════════════════════════════
    private Pane buildStatsTab() {
        lblTotalPatients    = new Label("—");
        lblTraitementsActifs = new Label("—");
        lblSurveillance      = new Label("—");

        lblTotalPatients.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#1565C0;");
        lblTraitementsActifs.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#2E7D32;");
        lblSurveillance.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#E65100;");

        VBox cardP = carte("Total Patients", lblTotalPatients, "#E3F2FD");
        VBox cardT = carte("Traitements actifs", lblTraitementsActifs, "#E8F5E9");
        VBox cardS = carte("Sous surveillance", lblSurveillance, "#FFF3E0");

        HBox cartes = new HBox(20, cardP, cardT, cardS);
        cartes.setPadding(new Insets(20));
        cartes.setAlignment(Pos.TOP_CENTER);

        Button btnRafraichir = new Button("🔄 Actualiser");
        btnRafraichir.setOnAction(e -> rafraichirStats());
        btnRafraichir.setStyle("-fx-background-color:#1565C0; -fx-text-fill:white; -fx-font-size:13px;");

        VBox layout = new VBox(20, new Label(" "), cartes, btnRafraichir);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));
        rafraichirStats();
        return layout;
    }

    private VBox carte(String titre, Label valeur, String bg) {
        Label lTitre = new Label(titre);
        lTitre.setStyle("-fx-font-size:14px; -fx-text-fill:#555;");
        VBox card = new VBox(8, lTitre, valeur);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12; -fx-border-radius:12;" +
                "-fx-border-color:#ccc; -fx-min-width:200; -fx-min-height:120;");
        return card;
    }

    private void rafraichirStats() {
        lblTotalPatients.setText(String.valueOf(patientDAO.compter()));
        lblTraitementsActifs.setText(String.valueOf(traitementDAO.compterActifs()));
        long surv = patients.stream().filter(Patient::isSousSurveillance).count();
        lblSurveillance.setText(String.valueOf(surv));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ONGLET PARAMÈTRES
    // ═════════════════════════════════════════════════════════════════════════
    private Pane buildParametresTab(Stage stage) {
        Label lblTheme = new Label("Couleur d'accentuation :");
        ColorPicker colorPicker = new ColorPicker(Color.web("#1565C0"));
        colorPicker.setOnAction(e -> {
            // Applique dynamiquement la couleur choisie aux boutons (démonstration)
            String hex = toHex(colorPicker.getValue());
            stage.getScene().getRoot().setStyle("-fx-accent: " + hex + ";");
        });
        Tooltip.install(colorPicker, new Tooltip("Personnaliser la couleur d'accentuation"));

        Label lblInfo = new Label("Base de données : MySQL (medical_db)");
        Button btnTesterConnexion = new Button("Tester la connexion BD");
        btnTesterConnexion.setOnAction(e -> {
            try {
                Database.getConnection();
                afficherInfo("Connexion OK", "La connexion à MySQL est fonctionnelle.");
            } catch (Exception ex) {
                afficherErreur("Erreur connexion", ex.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(25));
        grid.add(lblTheme, 0, 0); grid.add(colorPicker, 1, 0);
        grid.add(lblInfo, 0, 1);
        grid.add(btnTesterConnexion, 0, 2);
        return grid;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DIALOGUES PATIENT
    // ═════════════════════════════════════════════════════════════════════════
    private void ouvrirDialogPatient(Patient existant) {
        boolean isEdit = existant != null;
        Dialog<Patient> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Patient" : "Ajouter Patient");
        dialog.setHeaderText(isEdit ? "Modification : " + existant.getNomComplet() : "Nouveau Patient");

        ButtonType btnOk  = new ButtonType(isEdit ? "Modifier" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnAnn);

        // Champs
        TextField   tfNom     = new TextField(isEdit ? existant.getNom() : "");
        TextField   tfPrenom  = new TextField(isEdit ? existant.getPrenom() : "");
        DatePicker  dpNais    = new DatePicker(isEdit ? existant.getDateNaissance() : null);
        TextField   tfTel     = new TextField(isEdit ? existant.getTelephone() : "");
        TextField   tfEmail   = new TextField(isEdit ? existant.getEmail() : "");
        TextArea    taObs     = new TextArea(isEdit ? existant.getObservations() : "");
        taObs.setPrefRowCount(3);
        CheckBox    cbSurv    = new CheckBox("Sous surveillance particulière");
        cbSurv.setSelected(isEdit && existant.isSousSurveillance());

        // RadioButtons Sexe
        ToggleGroup tgSexe  = new ToggleGroup();
        RadioButton rbH     = new RadioButton("Homme"); rbH.setToggleGroup(tgSexe);
        RadioButton rbF     = new RadioButton("Femme"); rbF.setToggleGroup(tgSexe);
        if (isEdit && "Femme".equals(existant.getSexe())) rbF.setSelected(true);
        else rbH.setSelected(true);

        // Tooltips
        Tooltip.install(tfNom,    new Tooltip("Nom de famille du patient"));
        Tooltip.install(tfPrenom, new Tooltip("Prénom du patient"));
        Tooltip.install(dpNais,   new Tooltip("Date de naissance (format : jj/mm/aaaa)"));
        Tooltip.install(tfEmail,  new Tooltip("Adresse email pour contact"));

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(20));
        g.add(new Label("Nom *"),           0, 0); g.add(tfNom,  1, 0);
        g.add(new Label("Prénom *"),        0, 1); g.add(tfPrenom, 1, 1);
        g.add(new Label("Date naissance"),  0, 2); g.add(dpNais, 1, 2);
        g.add(new Label("Sexe"),            0, 3); g.add(new HBox(10, rbH, rbF), 1, 3);
        g.add(new Label("Téléphone"),       0, 4); g.add(tfTel, 1, 4);
        g.add(new Label("Email"),           0, 5); g.add(tfEmail, 1, 5);
        g.add(new Label("Observations"),    0, 6); g.add(taObs, 1, 6);
        g.add(cbSurv, 1, 7);

        dialog.getDialogPane().setContent(g);
        Platform.runLater(tfNom::requestFocus);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                if (tfNom.getText().isBlank() || tfPrenom.getText().isBlank()) {
                    afficherErreur("Champs requis", "Le nom et le prénom sont obligatoires.");
                    return null;
                }
                Patient p = isEdit ? existant : new Patient();
                p.setNom(tfNom.getText().trim());
                p.setPrenom(tfPrenom.getText().trim());
                p.setDateNaissance(dpNais.getValue());
                p.setSexe(rbF.isSelected() ? "Femme" : "Homme");
                p.setTelephone(tfTel.getText().trim());
                p.setEmail(tfEmail.getText().trim());
                p.setObservations(taObs.getText().trim());
                p.setSousSurveillance(cbSurv.isSelected());
                return p;
            }
            return null;
        });

        Optional<Patient> result = dialog.showAndWait();
        result.ifPresent(p -> {
            if (isEdit) { patientDAO.modifier(p); }
            else        { patientDAO.ajouter(p); }
            chargerPatients();
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DIALOGUES TRAITEMENT
    // ═════════════════════════════════════════════════════════════════════════
    private void ouvrirDialogTraitement(Patient patient, Traitement existant) {
        boolean isEdit = existant != null;
        Dialog<Traitement> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Traitement" : "Ajouter Traitement");
        dialog.setHeaderText("Patient : " + patient.getNomComplet());

        ButtonType btnOk  = new ButtonType(isEdit ? "Modifier" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnAnn);

        TextField  tfNomT  = new TextField(isEdit ? existant.getNom() : "");
        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Antibiotique", "Antalgique", "Anti-inflammatoire",
                "Antihypertenseur", "Antidiabétique", "Autre");
        cbType.setValue(isEdit ? existant.getType() : "Antibiotique");

        TextField  tfPoso  = new TextField(isEdit ? existant.getPosologie() : "");
        TextArea   taEffets = new TextArea(isEdit ? existant.getEffetsSecondaires() : "");
        taEffets.setPrefRowCount(2);

        DatePicker dpDebut = new DatePicker(isEdit ? existant.getDateDebut() : LocalDate.now());
        DatePicker dpFin   = new DatePicker(isEdit ? existant.getDateFin() : LocalDate.now().plusDays(7));

        // Spinner prises/jour
        Spinner<Integer> spinnerPrises = new Spinner<>(1, 10, isEdit ? existant.getPrisesParJour() : 1);
        spinnerPrises.setEditable(true);
        Tooltip.install(spinnerPrises, new Tooltip("Nombre de prises par jour (1-10)"));

        // Slider durée
        Slider sliderDuree = new Slider(1, 90, isEdit ? existant.getDureeEstimee() : 7);
        sliderDuree.setShowTickLabels(true);
        sliderDuree.setShowTickMarks(true);
        sliderDuree.setMajorTickUnit(15);
        Label lblDuree = new Label("Durée : " + (int) sliderDuree.getValue() + " jours");
        sliderDuree.valueProperty().addListener((obs, old, nw) ->
                lblDuree.setText("Durée : " + nw.intValue() + " jours"));

        CheckBox cbActif = new CheckBox("Traitement actif");
        cbActif.setSelected(!isEdit || existant.isActif());

        // ProgressBar lecture seule
        ProgressBar pbProg = new ProgressBar(isEdit ? existant.getProgression() : 0);
        pbProg.setPrefWidth(200);
        Label lblProg = new Label("Progression actuelle");

        // ColorPicker
        ColorPicker cpCouleur = new ColorPicker(
                isEdit && existant.getCouleur() != null ? Color.web(existant.getCouleur()) : Color.web("#2196F3"));
        Tooltip.install(cpCouleur, new Tooltip("Couleur d'étiquette du traitement"));

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(20));
        g.add(new Label("Nom traitement *"), 0, 0); g.add(tfNomT,  1, 0);
        g.add(new Label("Type *"),           0, 1); g.add(cbType,  1, 1);
        g.add(new Label("Posologie"),        0, 2); g.add(tfPoso,  1, 2);
        g.add(new Label("Prises/jour"),      0, 3); g.add(spinnerPrises, 1, 3);
        g.add(new Label("Date début"),       0, 4); g.add(dpDebut, 1, 4);
        g.add(new Label("Date fin"),         0, 5); g.add(dpFin,   1, 5);
        g.add(lblDuree,                      0, 6); g.add(sliderDuree, 1, 6);
        g.add(new Label("Effets secondaires"), 0, 7); g.add(taEffets, 1, 7);
        g.add(cbActif,                       1, 8);
        g.add(lblProg,                       0, 9); g.add(pbProg,  1, 9);
        g.add(new Label("Couleur"),          0, 10); g.add(cpCouleur, 1, 10);

        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                if (tfNomT.getText().isBlank()) {
                    afficherErreur("Champ requis", "Le nom du traitement est obligatoire.");
                    return null;
                }
                Traitement t = isEdit ? existant : new Traitement();
                t.setPatientId(patient.getId());
                t.setNom(tfNomT.getText().trim());
                t.setType(cbType.getValue());
                t.setPosologie(tfPoso.getText().trim());
                t.setPrisesParJour(spinnerPrises.getValue());
                t.setDateDebut(dpDebut.getValue());
                t.setDateFin(dpFin.getValue());
                t.setDureeEstimee((int) sliderDuree.getValue());
                t.setEffetsSecondaires(taEffets.getText().trim());
                t.setActif(cbActif.isSelected());
                t.setCouleur(toHex(cpCouleur.getValue()));
                return t;
            }
            return null;
        });

        Optional<Traitement> result = dialog.showAndWait();
        result.ifPresent(t -> {
            if (isEdit) traitementDAO.modifier(t);
            else        traitementDAO.ajouter(t);
            chargerTraitements(patient.getId(), "Tous");
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ACTIONS CRUD
    // ═════════════════════════════════════════════════════════════════════════
    private void supprimerPatient() {
        Patient sel = tablePatients.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherWarning("Sélection requise", "Veuillez sélectionner un patient."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer " + sel.getNomComplet() + " ?");
        confirm.setContentText("Cette action supprimera aussi tous ses traitements.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (patientDAO.supprimer(sel.getId())) {
                    chargerPatients();
                    traitements.clear();
                } else {
                    afficherErreur("Erreur", "Impossible de supprimer le patient.");
                }
            }
        });
    }

    private void supprimerTraitement() {
        Traitement sel = tableTraitements.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherWarning("Sélection requise", "Veuillez sélectionner un traitement."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le traitement « " + sel.getNom() + " » ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                traitementDAO.supprimer(sel.getId());
                Patient pat = listViewPatients.getSelectionModel().getSelectedItem();
                if (pat != null) chargerTraitements(pat.getId(), "Tous");
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DONNÉES
    // ═════════════════════════════════════════════════════════════════════════
    private void chargerPatients() {
        patients.setAll(patientDAO.getTous());
        if (listViewPatients != null) listViewPatients.setItems(patients);
    }

    private void chargerTraitements(int patientId, String typeFiltre) {
        List<Traitement> liste = traitementDAO.getParPatient(patientId);
        if (typeFiltre != null && !typeFiltre.equals("Tous"))
            liste.removeIf(t -> !typeFiltre.equals(t.getType()));
        traitements.setAll(liste);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  EXPORT
    // ═════════════════════════════════════════════════════════════════════════
    private void exporterPatients(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter patients");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("patients.csv");
        File f = fc.showSaveDialog(stage);
        if (f != null) {
            try {
                ExportUtil.exporterPatientsCSV(patientDAO.getTous(), f);
                afficherInfo("Export réussi", "Fichier exporté : " + f.getAbsolutePath());
            } catch (IOException e) {
                afficherErreur("Erreur export", e.getMessage());
            }
        }
    }

    private void exporterTraitements(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter traitements");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("traitements.csv");
        File f = fc.showSaveDialog(stage);
        if (f != null) {
            try {
                ExportUtil.exporterTraitementsCSV(traitementDAO.getTous(), f);
                afficherInfo("Export réussi", "Fichier exporté : " + f.getAbsolutePath());
            } catch (IOException e) {
                afficherErreur("Erreur export", e.getMessage());
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═════════════════════════════════════════════════════════════════════════
    private void styliserBoutons(Button... btns) {
        for (Button b : btns)
            b.setStyle("-fx-background-color:#1565C0; -fx-text-fill:white; -fx-background-radius:6;");
    }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(titre); a.setContentText(msg); a.showAndWait();
    }
    private void afficherWarning(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(titre); a.setContentText(msg); a.showAndWait();
    }
    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(titre); a.setContentText(msg); a.showAndWait();
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}