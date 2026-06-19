package com.medical.view;

import com.medical.dao.Database;
import com.medical.dao.PatientDAO;
import com.medical.dao.TraitementDAO;
import com.medical.model.Patient;
import com.medical.model.Traitement;
import com.medical.util.ExportUtil;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MainView {
    private static final String THEME_RESOURCE = "/com/medical/view/medical-theme.css";

    private final PatientDAO    patientDAO    = new PatientDAO();
    private final TraitementDAO traitementDAO = new TraitementDAO();

    private ObservableList<Patient>    patients    = FXCollections.observableArrayList();
    private ObservableList<Traitement> traitements = FXCollections.observableArrayList();

    private TableView<Patient>    tablePatients;
    private TableView<Traitement> tableTraitements;
    private ListView<Patient>     listViewPatients;
    private Accordion             patientDetailAccordion;
    private TitledPane            paneDetail;
    private TextField             tfRecherche;

    private Label lblTotalPatients, lblTraitementsActifs, lblSurveillance;

    // ── BUILD SCENE ───────────────────────────────────────────────────────────
    public Scene buildScene(Stage stage) {
        MenuBar menuBar = buildMenuBar(stage);
        menuBar.getStyleClass().add("app-menu");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("main-tabs");

        Tab tabPatients    = new Tab("👤 Patients",    buildPatientsTab());
        Tab tabTraitements = new Tab("💊 Traitements", buildTraitementsTab());
        Tab tabStats       = new Tab("📊 Stats",      buildStatsTab());
        Tab tabParametres  = new Tab("⚙ Param",      buildParametresTab(stage));

        tabPane.getTabs().addAll(tabPatients, tabTraitements, tabStats, tabParametres);

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nw) -> { if (nw == tabStats) rafraichirStats(); });

        VBox root = new VBox(0, menuBar, buildHeader(), tabPane, buildStatusBar());
        root.getStyleClass().add("app-root");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        chargerPatients();
        Scene scene = new Scene(root, 1000, 650);
        URL theme = getClass().getResource(THEME_RESOURCE);
        if (theme != null) {
            scene.getStylesheets().add(theme.toExternalForm());
        }

        setupKeyboardShortcuts(scene, stage);
        return scene;
    }

    private HBox buildHeader() {
        Label badge = new Label("+");
        badge.getStyleClass().add("brand-badge");

        Label title = new Label("Suivi de Traitements");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Patients, traitements et surveillance");
        subtitle.getStyleClass().add("app-subtitle");

        VBox text = new VBox(1, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox chips = new HBox(6,
                chip("Clinique", "header-chip"),
                chip("MVC", "header-chip"),
                chip("JavaFX", "header-chip"));
        chips.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(10, badge, text, spacer, chips);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");
        return header;
    }

    private HBox buildStatusBar() {
        Label statusLabel = new Label("✅ Prêt");
        statusLabel.getStyleClass().add("status-label");

        Label countLabel = new Label();
        countLabel.textProperty().bind(
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> patients.size() + " patients",
                        patients
                )
        );
        countLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(15, statusLabel, spacer, countLabel);
        statusBar.setPadding(new Insets(4, 16, 4, 16));
        statusBar.getStyleClass().add("status-bar");
        return statusBar;
    }

    private VBox buildSectionHeader(String titre, String sousTitre) {
        Label title = new Label(titre);
        title.getStyleClass().add("section-title");

        Label subtitle = new Label(sousTitre);
        subtitle.getStyleClass().add("section-subtitle");

        VBox box = new VBox(2, title, subtitle);
        box.getStyleClass().add("section-header");
        return box;
    }

    private Label chip(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    // ── MENU BAR ──────────────────────────────────────────────────────────────
    private MenuBar buildMenuBar(Stage stage) {
        MenuBar bar = new MenuBar();

        Menu mFichier = new Menu("Fichier");
        MenuItem miExportPatients    = new MenuItem("Exporter patients");
        MenuItem miExportTraitements = new MenuItem("Exporter traitements");
        MenuItem miQuitter           = new MenuItem("Quitter\tCtrl+Q");
        miExportPatients.setOnAction(e -> exporterPatients(stage));
        miExportTraitements.setOnAction(e -> exporterTraitements(stage));
        miQuitter.setOnAction(e -> { Database.fermer(); Platform.exit(); });
        mFichier.getItems().addAll(miExportPatients, miExportTraitements, new SeparatorMenuItem(), miQuitter);

        Menu mPatients = new Menu("Patients");
        MenuItem miAjouterPatient    = new MenuItem("Ajouter\tCtrl+N");
        MenuItem miModifierPatient   = new MenuItem("Modifier");
        MenuItem miSupprimerPatient  = new MenuItem("Supprimer\tDelete");
        miAjouterPatient.setOnAction(e -> ouvrirDialogPatient(null));
        miModifierPatient.setOnAction(e -> {
            Patient sel = tablePatients.getSelectionModel().getSelectedItem();
            if (sel != null) ouvrirDialogPatient(sel);
            else afficherWarning("Sélection", "Sélectionnez un patient.");
        });
        miSupprimerPatient.setOnAction(e -> supprimerPatient());
        mPatients.getItems().addAll(miAjouterPatient, miModifierPatient, miSupprimerPatient);

        Menu mAide = new Menu("Aide");
        MenuItem miAPropos = new MenuItem("À propos");
        miAPropos.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("À propos");
            a.setHeaderText("Application de Suivi Médical");
            a.setContentText("Version 2.0\nJavaFX + MySQL\n© 2026");
            a.showAndWait();
        });
        mAide.getItems().add(miAPropos);

        bar.getMenus().addAll(mFichier, mPatients, mAide);
        return bar;
    }

    // ── ONGLET PATIENTS ──────────────────────────────────────────────────────
    private Pane buildPatientsTab() {
        VBox sectionHeader = buildSectionHeader(
                "Dossier patients",
                "Informations, contacts et statuts de surveillance");

        tfRecherche = new TextField();
        tfRecherche.setPromptText("🔍 Rechercher…");
        tfRecherche.setPrefWidth(200);
        Tooltip.install(tfRecherche, new Tooltip("Nom, prénom, téléphone ou email"));

        Button btnAjouter   = new Button("➕ Ajouter");
        Button btnModifier  = new Button("✏ Modifier");
        Button btnSupprimer = new Button("🗑 Supprimer");
        Button btnRafraichir = new Button("🔄");

        for (Button btn : new Button[]{btnAjouter, btnModifier, btnSupprimer, btnRafraichir}) {
            btn.setStyle("-fx-padding: 4 10; -fx-font-size: 11px;");
        }

        styliserBoutons(btnAjouter, btnModifier, btnSupprimer, btnRafraichir);
        btnModifier.getStyleClass().remove("button-primary");
        btnModifier.getStyleClass().add("button-secondary");
        btnSupprimer.getStyleClass().remove("button-primary");
        btnSupprimer.getStyleClass().add("button-danger");
        btnRafraichir.getStyleClass().remove("button-primary");
        btnRafraichir.getStyleClass().add("button-secondary");

        Region toolbarSpacer = new Region();
        HBox toolbar = new HBox(6, tfRecherche, toolbarSpacer, btnAjouter, btnModifier, btnSupprimer, btnRafraichir);
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar-panel");

        tablePatients = new TableView<>();
        Label emptyPatients = new Label("Aucun patient.");
        emptyPatients.getStyleClass().add("empty-state");
        tablePatients.setPlaceholder(emptyPatients);
        tablePatients.getStyleClass().add("data-table");

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

        colId.setPrefWidth(40);
        colNom.setPrefWidth(120);
        colPrenom.setPrefWidth(120);
        colAge.setPrefWidth(50);
        colSexe.setPrefWidth(60);
        colTel.setPrefWidth(100);
        colSurv.setPrefWidth(80);

        tablePatients.getColumns().addAll(colId, colNom, colPrenom, colAge, colSexe, colTel, colSurv);

        FilteredList<Patient> filteredPatients = new FilteredList<>(patients, p -> true);
        tablePatients.setItems(filteredPatients);
        setupSearchField(filteredPatients);
        setupRowColoring();
        setupTableContextMenu();

        paneDetail = new TitledPane("Détail patient", new Label("Sélectionnez un patient."));
        paneDetail.setExpanded(false);
        patientDetailAccordion = new Accordion(paneDetail);
        patientDetailAccordion.getStyleClass().add("detail-accordion");

        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> {
            if (patient != null) {
                VBox detailContent = buildDetailPatient(patient);
                paneDetail.setContent(detailContent);
                paneDetail.setExpanded(true);
                animateNode(detailContent, "fadeIn");
            }
        });

        btnAjouter.setOnAction(e -> ouvrirDialogPatient(null));
        btnModifier.setOnAction(e -> {
            Patient sel = tablePatients.getSelectionModel().getSelectedItem();
            if (sel != null) ouvrirDialogPatient(sel);
            else afficherWarning("Sélection", "Sélectionnez un patient.");
        });
        btnSupprimer.setOnAction(e -> supprimerPatient());
        btnRafraichir.setOnAction(e -> {
            chargerPatients();
            ToastNotification.show((Stage) tablePatients.getScene().getWindow(),
                    "Patients rafraîchis", "success");
        });

        VBox layout = new VBox(8, sectionHeader, toolbar, tablePatients, patientDetailAccordion);
        VBox.setVgrow(tablePatients, Priority.ALWAYS);
        layout.setPadding(new Insets(4));
        layout.getStyleClass().add("content-pane");
        return layout;
    }

    // ── ONGLET TRAITEMENTS ──────────────────────────────────────
    private SplitPane buildTraitementsTab() {
        VBox sectionHeader = buildSectionHeader(
                "Plan de traitements",
                "Prescriptions, durées, prises et progression");

        // ── PATIENT LIST ──────────────────────────────────────────────────────
        listViewPatients = new ListView<>(patients);
        listViewPatients.setPrefWidth(180);
        listViewPatients.setPlaceholder(new Label("Aucun patient"));
        listViewPatients.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                if (empty || patient == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label avatar = new Label(initiales(patient));
                avatar.getStyleClass().add("patient-avatar");

                Label nom = new Label(patient.getNomComplet());
                nom.getStyleClass().add("patient-list-name");

                Label sub = new Label(patient.isSousSurveillance() ? "⚠" : "");
                sub.getStyleClass().add("patient-list-subtitle");

                VBox text = new VBox(1, nom);
                HBox row = new HBox(8, avatar, text, sub);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });

        TextField tfPatientSearch = new TextField();
        tfPatientSearch.setPromptText("🔍 Filtrer...");
        tfPatientSearch.setPrefWidth(160);
        tfPatientSearch.textProperty().addListener((obs, old, nw) -> {
            if (nw == null || nw.isEmpty()) {
                listViewPatients.setItems(patients);
            } else {
                FilteredList<Patient> filtered = new FilteredList<>(patients,
                        p -> p.getNomComplet().toLowerCase().contains(nw.toLowerCase()));
                listViewPatients.setItems(filtered);
            }
        });

        Label lblListePatients = new Label("Patients :");
        lblListePatients.getStyleClass().add("side-title");

        ComboBox<String> cbFiltreType = new ComboBox<>();
        cbFiltreType.getItems().addAll("Tous", "Antibiotique", "Antalgique", "Anti-inflammatoire",
                "Antihypertenseur", "Antidiabétique", "Autre");
        cbFiltreType.setValue("Tous");
        cbFiltreType.setMaxWidth(120);

        // ── BUTTONS ──────────────────────────────────────────────────────────
        Button btnAjouterT   = new Button("➕ Ajouter");
        Button btnModifierT  = new Button("✏ Modifier");
        Button btnSupprimerT = new Button("🗑 Supprimer");
        Button btnRafraichirT = new Button("🔄");

        // ── ACTION SUR LE BOUTON AJOUTER ─────────────────────────────────────
        btnAjouterT.setOnAction(e -> {
            Patient selected = listViewPatients.getSelectionModel().getSelectedItem();
            if (selected == null) {
                afficherWarning("Sélection requise", "Veuillez sélectionner un patient dans la liste de gauche.");
                return;
            }
            ouvrirDialogTraitement(selected, null);
        });

        btnModifierT.setOnAction(e -> {
            Traitement sel = tableTraitements.getSelectionModel().getSelectedItem();
            Patient pat = listViewPatients.getSelectionModel().getSelectedItem();
            if (sel == null) {
                afficherWarning("Sélection requise", "Veuillez sélectionner un traitement.");
                return;
            }
            if (pat == null) {
                afficherWarning("Sélection requise", "Veuillez sélectionner un patient.");
                return;
            }
            ouvrirDialogTraitement(pat, sel);
        });

        btnSupprimerT.setOnAction(e -> supprimerTraitement());
        btnRafraichirT.setOnAction(e -> {
            Patient sel = listViewPatients.getSelectionModel().getSelectedItem();
            if (sel != null) {
                chargerTraitements(sel.getId(), cbFiltreType.getValue());
                ToastNotification.show((Stage) tableTraitements.getScene().getWindow(),
                        "Traitements rafraîchis", "success");
            }
        });

        // ── STYLING ──────────────────────────────────────────────────────────
        for (Button btn : new Button[]{btnAjouterT, btnModifierT, btnSupprimerT, btnRafraichirT}) {
            btn.setStyle("-fx-padding: 4 10; -fx-font-size: 11px;");
        }
        styliserBoutons(btnAjouterT, btnModifierT, btnSupprimerT, btnRafraichirT);
        btnModifierT.getStyleClass().remove("button-primary");
        btnModifierT.getStyleClass().add("button-secondary");
        btnSupprimerT.getStyleClass().remove("button-primary");
        btnSupprimerT.getStyleClass().add("button-danger");
        btnRafraichirT.getStyleClass().remove("button-primary");
        btnRafraichirT.getStyleClass().add("button-secondary");

        // ── TOOLBAR ──────────────────────────────────────────────────────────
        Region toolbarTSpacer = new Region();
        HBox toolbarT = new HBox(6, new Label("Type:"), cbFiltreType,
                toolbarTSpacer, btnAjouterT, btnModifierT, btnSupprimerT, btnRafraichirT);
        HBox.setHgrow(toolbarTSpacer, Priority.ALWAYS);
        toolbarT.setPadding(new Insets(6, 10, 6, 10));
        toolbarT.setAlignment(Pos.CENTER_LEFT);
        toolbarT.getStyleClass().add("toolbar-panel");

        // ── TABLE DES TRAITEMENTS ──────────────────────────────────────────
        tableTraitements = new TableView<>();
        tableTraitements.setPlaceholder(new Label("Aucun traitement."));
        tableTraitements.getStyleClass().add("data-table");

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

        colProg.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar();
            { pb.setPrefWidth(80); }
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

        colNomT.setPrefWidth(130);
        colType.setPrefWidth(100);
        colPoso.setPrefWidth(110);
        colPrises.setPrefWidth(60);
        colDebut.setPrefWidth(80);
        colFin.setPrefWidth(80);
        colStatut.setPrefWidth(70);
        colProg.setPrefWidth(90);

        tableTraitements.getColumns().addAll(colNomT, colType, colPoso, colPrises, colDebut, colFin, colStatut, colProg);
        tableTraitements.setItems(traitements);

        // ── SÉLECTION PATIENT → charge ses traitements ──────────────────────
        listViewPatients.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> {
            if (patient != null) {
                chargerTraitements(patient.getId(), cbFiltreType.getValue());
                animateNode(tableTraitements, "fadeIn");
            }
        });

        cbFiltreType.setOnAction(e -> {
            Patient sel = listViewPatients.getSelectionModel().getSelectedItem();
            if (sel != null) chargerTraitements(sel.getId(), cbFiltreType.getValue());
        });

        // ── MENU CONTEXTUEL ─────────────────────────────────────────────────
        setupTreatmentContextMenu();

        // ── LAYOUT ──────────────────────────────────────────────────────────
        VBox leftPane = new VBox(8, lblListePatients, tfPatientSearch, listViewPatients);
        leftPane.setPadding(new Insets(6));
        leftPane.getStyleClass().add("side-panel");

        VBox rightPane = new VBox(8, sectionHeader, toolbarT, tableTraitements);
        VBox.setVgrow(tableTraitements, Priority.ALWAYS);
        rightPane.setPadding(new Insets(4));
        rightPane.getStyleClass().add("content-pane");

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.22);
        split.getStyleClass().add("medical-split");

        return split;
    }

    // ── ONGLET STATISTIQUES ─────────────────────────────────────────────────
    private Pane buildStatsTab() {
        lblTotalPatients    = new Label("—");
        lblTraitementsActifs = new Label("—");
        lblSurveillance      = new Label("—");

        lblTotalPatients.getStyleClass().addAll("stat-value", "stat-blue");
        lblTraitementsActifs.getStyleClass().addAll("stat-value", "stat-green");
        lblSurveillance.getStyleClass().addAll("stat-value", "stat-amber");

        VBox cardP = carte("Patients", lblTotalPatients, "Dossiers actifs");
        VBox cardT = carte("Traitements", lblTraitementsActifs, "En cours");
        VBox cardS = carte("Surveillance", lblSurveillance, "Attention renforcée");

        HBox cartes = new HBox(12, cardP, cardT, cardS);
        cartes.setPadding(new Insets(12));
        cartes.setAlignment(Pos.TOP_CENTER);

        Label summaryLabel = new Label();
        summaryLabel.getStyleClass().add("stats-summary");

        Runnable updateSummary = () -> {
            int total = patientDAO.compter();
            int actifs = traitementDAO.compterActifs();
            long surv = patients.stream().filter(Patient::isSousSurveillance).count();
            summaryLabel.setText(String.format("📊 %d patients | %d traitements actifs | %d sous surveillance",
                    total, actifs, surv));
        };
        updateSummary.run();

        Button btnRafraichir = new Button("🔄 Actualiser");
        btnRafraichir.setOnAction(e -> {
            rafraichirStats();
            updateSummary.run();
            ToastNotification.show((Stage) btnRafraichir.getScene().getWindow(),
                    "Statistiques actualisées", "success");
        });
        btnRafraichir.setStyle("-fx-padding: 4 14; -fx-font-size: 11px;");
        btnRafraichir.getStyleClass().addAll("button-medical", "button-primary");

        VBox layout = new VBox(12,
                buildSectionHeader("Tableau de bord", "Indicateurs rapides"),
                cartes,
                summaryLabel,
                btnRafraichir);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(12));
        layout.getStyleClass().add("stats-pane");
        rafraichirStats();
        return layout;
    }

    private VBox carte(String titre, Label valeur, String description) {
        Label lTitre = new Label(titre);
        lTitre.getStyleClass().add("stat-title");

        Label lDescription = new Label(description);
        lDescription.getStyleClass().add("stat-description");
        lDescription.setWrapText(true);

        VBox card = new VBox(4, lTitre, valeur, lDescription);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("stat-card");

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.12), 20, 0, 0, 8);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("");
        });

        return card;
    }

    private void rafraichirStats() {
        lblTotalPatients.setText(String.valueOf(patientDAO.compter()));
        lblTraitementsActifs.setText(String.valueOf(traitementDAO.compterActifs()));
        long surv = patients.stream().filter(Patient::isSousSurveillance).count();
        lblSurveillance.setText(String.valueOf(surv));
    }

    // ── ONGLET PARAMÈTRES ───────────────────────────────────────────────────
    private Pane buildParametresTab(Stage stage) {
        Label lblTheme = new Label("Couleur :");
        ColorPicker colorPicker = new ColorPicker(Color.web("#1565C0"));
        colorPicker.setOnAction(e -> {
            String hex = toHex(colorPicker.getValue());
            stage.getScene().getRoot().setStyle("-fx-accent: " + hex + ";");
            stage.getScene().getStylesheets().clear();
            URL theme = getClass().getResource(THEME_RESOURCE);
            if (theme != null) {
                stage.getScene().getStylesheets().add(theme.toExternalForm());
            }
            ToastNotification.show(stage, "Couleur: " + hex, "success");
        });

        Label lblInfo = new Label("MySQL (medical_db)");
        Label lblVersion = new Label("v2.0");
        lblVersion.getStyleClass().add("settings-version");

        Button btnTesterConnexion = new Button("Tester BD");
        btnTesterConnexion.setStyle("-fx-padding: 4 12; -fx-font-size: 11px;");
        btnTesterConnexion.getStyleClass().addAll("button-medical", "button-secondary");
        btnTesterConnexion.setOnAction(e -> {
            try {
                Database.getConnection();
                ToastNotification.show(stage, "Connexion OK", "success");
            } catch (Exception ex) {
                ToastNotification.show(stage, "Erreur: " + ex.getMessage(), "error");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(16));
        grid.getStyleClass().add("settings-pane");
        grid.add(lblTheme, 0, 0); grid.add(colorPicker, 1, 0);
        grid.add(lblInfo, 0, 1); grid.add(lblVersion, 1, 1);
        grid.add(btnTesterConnexion, 0, 2);
        return grid;
    }

    // ── DIALOGUE PATIENT ────────────────────────────────────────────────────
    private void ouvrirDialogPatient(Patient existant) {
        boolean isEdit = existant != null;
        Dialog<Patient> dialog = new Dialog<>();
        appliquerTheme(dialog);
        dialog.setTitle(isEdit ? "Modifier Patient" : "Ajouter Patient");
        dialog.setHeaderText(isEdit ? "Modification: " + existant.getNomComplet() : "Nouveau Patient");

        ButtonType btnOk  = new ButtonType(isEdit ? "Modifier" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnAnn);

        TextField   tfNom     = new TextField(isEdit ? existant.getNom() : "");
        TextField   tfPrenom  = new TextField(isEdit ? existant.getPrenom() : "");
        DatePicker  dpNais    = new DatePicker(isEdit ? existant.getDateNaissance() : null);
        TextField   tfTel     = new TextField(isEdit ? existant.getTelephone() : "");
        TextField   tfEmail   = new TextField(isEdit ? existant.getEmail() : "");
        TextArea    taObs     = new TextArea(isEdit ? existant.getObservations() : "");
        taObs.setPrefRowCount(2);
        CheckBox    cbSurv    = new CheckBox("Surveillance");
        cbSurv.setSelected(isEdit && existant.isSousSurveillance());

        ToggleGroup tgSexe  = new ToggleGroup();
        RadioButton rbH     = new RadioButton("Homme"); rbH.setToggleGroup(tgSexe);
        RadioButton rbF     = new RadioButton("Femme"); rbF.setToggleGroup(tgSexe);
        if (isEdit && "Femme".equals(existant.getSexe())) rbF.setSelected(true);
        else rbH.setSelected(true);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(14));
        g.getStyleClass().add("dialog-form");
        g.add(new Label("Nom *"), 0, 0); g.add(tfNom, 1, 0);
        g.add(new Label("Prénom *"), 0, 1); g.add(tfPrenom, 1, 1);
        g.add(new Label("Naissance"), 0, 2); g.add(dpNais, 1, 2);
        g.add(new Label("Sexe"), 0, 3); g.add(new HBox(8, rbH, rbF), 1, 3);
        g.add(new Label("Téléphone"), 0, 4); g.add(tfTel, 1, 4);
        g.add(new Label("Email"), 0, 5); g.add(tfEmail, 1, 5);
        g.add(new Label("Observations"), 0, 6); g.add(taObs, 1, 6);
        g.add(cbSurv, 1, 7);

        dialog.getDialogPane().setContent(g);
        Platform.runLater(tfNom::requestFocus);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                if (tfNom.getText().isBlank() || tfPrenom.getText().isBlank()) {
                    afficherErreur("Champs requis", "Nom et prénom obligatoires.");
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
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            ToastNotification.show(stage,
                    isEdit ? "Patient modifié" : "Patient ajouté",
                    "success");
        });
    }

    // ── DIALOGUE TRAITEMENT (Spinner corrigé : valeur tapée prise en compte) ──
    private void ouvrirDialogTraitement(Patient patient, Traitement existant) {
        boolean isEdit = existant != null;
        Dialog<Traitement> dialog = new Dialog<>();
        appliquerTheme(dialog);
        dialog.setTitle(isEdit ? "Modifier Traitement" : "Ajouter Traitement");
        dialog.setHeaderText("Patient : " + patient.getNomComplet());

        ButtonType btnOk  = new ButtonType(isEdit ? "Modifier" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnAnn);

        TextField  tfNomT  = new TextField(isEdit ? existant.getNom() : "");
        tfNomT.setPrefWidth(200);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Antibiotique", "Antalgique", "Anti-inflammatoire",
                "Antihypertenseur", "Antidiabétique", "Autre");
        cbType.setValue(isEdit ? existant.getType() : "Antibiotique");
        cbType.setPrefWidth(200);

        TextField  tfPoso  = new TextField(isEdit ? existant.getPosologie() : "");
        tfPoso.setPrefWidth(200);

        TextArea   taEffets = new TextArea(isEdit ? existant.getEffetsSecondaires() : "");
        taEffets.setPrefRowCount(2);
        taEffets.setPrefWidth(200);
        taEffets.setPrefHeight(50);

        DatePicker dpDebut = new DatePicker(isEdit ? existant.getDateDebut() : LocalDate.now());
        DatePicker dpFin   = new DatePicker(isEdit ? existant.getDateFin() : LocalDate.now().plusDays(7));

        // ── SPINNER PRISES/JOUR – Saisissable, valeur commitée en direct ───
        Spinner<Integer> spinnerPrises = new Spinner<>(1, 10, isEdit ? existant.getPrisesParJour() : 1);
        spinnerPrises.setEditable(true);
        spinnerPrises.setPrefWidth(80);
        spinnerPrises.setMaxWidth(100);
        Tooltip.install(spinnerPrises, new Tooltip("Nombre de prises par jour (1-10)"));

        // FIX: sans TextFormatter, la valeur tapée au clavier n'est validée
        // qu'au focus-out. Si l'utilisateur tape puis clique direct sur
        // "Ajouter", la valeur saisie est perdue. Le TextFormatter la
        // synchronise immédiatement avec la value du Spinner.
        TextFormatter<Integer> spinnerFormatter = new TextFormatter<>(
                new IntegerStringConverter(), spinnerPrises.getValue());
        spinnerPrises.getEditor().setTextFormatter(spinnerFormatter);
        spinnerFormatter.valueProperty().bindBidirectional(spinnerPrises.getValueFactory().valueProperty());

        Slider sliderDuree = new Slider(1, 90, isEdit ? existant.getDureeEstimee() : 7);
        sliderDuree.setShowTickLabels(true);
        sliderDuree.setShowTickMarks(true);
        sliderDuree.setMajorTickUnit(30);
        sliderDuree.setMaxWidth(200);
        Label lblDuree = new Label("Durée : " + (int) sliderDuree.getValue() + " jours");
        sliderDuree.valueProperty().addListener((obs, old, nw) ->
                lblDuree.setText("Durée : " + nw.intValue() + " jours"));

        CheckBox cbActif = new CheckBox("Traitement actif");
        cbActif.setSelected(!isEdit || existant.isActif());

        ProgressBar pbProg = new ProgressBar(isEdit ? existant.getProgression() : 0);
        pbProg.setPrefWidth(200);

        ColorPicker cpCouleur = new ColorPicker(
                isEdit && existant.getCouleur() != null ? Color.web(existant.getCouleur()) : Color.web("#2196F3"));
        cpCouleur.setPrefWidth(150);

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(18));
        g.getStyleClass().add("dialog-form");

        int row = 0;
        g.add(new Label("Nom *"), 0, row);
        g.add(tfNomT, 1, row++);

        g.add(new Label("Type"), 0, row);
        g.add(cbType, 1, row++);

        g.add(new Label("Posologie"), 0, row);
        g.add(tfPoso, 1, row++);

        g.add(new Label("Prises/jour"), 0, row);
        g.add(spinnerPrises, 1, row++);

        g.add(new Label("Début"), 0, row);
        g.add(dpDebut, 1, row++);

        g.add(new Label("Fin"), 0, row);
        g.add(dpFin, 1, row++);

        g.add(lblDuree, 0, row);
        g.add(sliderDuree, 1, row++);

        g.add(new Label("Effets secondaires"), 0, row);
        g.add(taEffets, 1, row++);

        g.add(cbActif, 1, row++);

        g.add(new Label("Couleur"), 0, row);
        g.add(cpCouleur, 1, row);

        ScrollPane scroll = new ScrollPane(g);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setMaxHeight(420);
        scroll.setPrefHeight(370);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().setMaxWidth(650);

        Platform.runLater(tfNomT::requestFocus);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                if (tfNomT.getText().isBlank()) {
                    afficherErreur("Champ requis", "Le nom est obligatoire.");
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
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            ToastNotification.show(stage,
                    isEdit ? "Traitement modifié" : "Traitement ajouté",
                    "success");
        });
    }

    // ── ACTIONS CRUD ────────────────────────────────────────────────────────
    private void supprimerPatient() {
        Patient sel = tablePatients.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherWarning("Sélection", "Sélectionnez un patient."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + sel.getNomComplet() + " ?");
        confirm.setContentText("Ses traitements seront aussi supprimés.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (patientDAO.supprimer(sel.getId())) {
                    chargerPatients();
                    traitements.clear();
                    Stage stage = (Stage) tablePatients.getScene().getWindow();
                    ToastNotification.show(stage, "Patient supprimé", "warning");
                } else {
                    afficherErreur("Erreur", "Impossible de supprimer.");
                }
            }
        });
    }

    private void supprimerTraitement() {
        Traitement sel = tableTraitements.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherWarning("Sélection", "Sélectionnez un traitement."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer « " + sel.getNom() + " » ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                traitementDAO.supprimer(sel.getId());
                Patient pat = listViewPatients.getSelectionModel().getSelectedItem();
                if (pat != null) chargerTraitements(pat.getId(), "Tous");
                Stage stage = (Stage) tableTraitements.getScene().getWindow();
                ToastNotification.show(stage, "Traitement supprimé", "warning");
            }
        });
    }

    // ── DONNÉES ──────────────────────────────────────────────────────────────
    private void chargerPatients() {
        patients.setAll(patientDAO.getTous());
        if (listViewPatients != null) {
            listViewPatients.setItems(patients);
        }
    }

    private void chargerTraitements(int patientId, String typeFiltre) {
        List<Traitement> liste = traitementDAO.getParPatient(patientId);
        if (typeFiltre != null && !typeFiltre.equals("Tous"))
            liste.removeIf(t -> !typeFiltre.equals(t.getType()));
        traitements.setAll(liste);
    }

    // ── EXPORT ───────────────────────────────────────────────────────────────
    private void exporterPatients(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter patients");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("patients.csv");
        File f = fc.showSaveDialog(stage);
        if (f != null) {
            try {
                ExportUtil.exporterPatientsCSV(patientDAO.getTous(), f);
                ToastNotification.show(stage, "Exporté avec succès", "success");
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
                ToastNotification.show(stage, "Exporté avec succès", "success");
            } catch (IOException e) {
                afficherErreur("Erreur export", e.getMessage());
            }
        }
    }

    // ── UTILITAIRES ──────────────────────────────────────────────────────────
    private void setupSearchField(FilteredList<Patient> filteredPatients) {
        tfRecherche.textProperty().addListener((obs, old, nw) -> {
            filteredPatients.setPredicate(p -> {
                if (nw == null || nw.isEmpty()) return true;
                String search = nw.toLowerCase().trim();
                return p.getNomComplet().toLowerCase().contains(search) ||
                        (p.getTelephone() != null && p.getTelephone().contains(search)) ||
                        (p.getEmail() != null && p.getEmail().toLowerCase().contains(search));
            });

            int count = filteredPatients.size();
            if (count == 0 && nw != null && !nw.isEmpty()) {
                tfRecherche.setStyle("-fx-border-color: #ef4444;");
            } else {
                tfRecherche.setStyle("");
            }
        });
    }

    private void setupRowColoring() {
        tablePatients.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                getStyleClass().removeAll("warning-row");
                if (patient != null && !empty && patient.isSousSurveillance()) {
                    getStyleClass().add("warning-row");
                }
            }
        });
    }

    private void setupTableContextMenu() {
        ContextMenu patientContextMenu = new ContextMenu();
        patientContextMenu.getStyleClass().add("context-menu");

        MenuItem editPatient = new MenuItem("✏ Modifier");
        editPatient.setOnAction(e -> {
            Patient selected = tablePatients.getSelectionModel().getSelectedItem();
            if (selected != null) ouvrirDialogPatient(selected);
        });

        MenuItem deletePatient = new MenuItem("🗑 Supprimer");
        deletePatient.setOnAction(e -> supprimerPatient());

        patientContextMenu.getItems().addAll(editPatient, deletePatient);
        tablePatients.setContextMenu(patientContextMenu);
    }

    private void setupTreatmentContextMenu() {
        ContextMenu treatmentContextMenu = new ContextMenu();
        treatmentContextMenu.getStyleClass().add("context-menu");

        MenuItem editTreatment = new MenuItem("✏ Modifier");
        editTreatment.setOnAction(e -> {
            Traitement selected = tableTraitements.getSelectionModel().getSelectedItem();
            Patient pat = listViewPatients.getSelectionModel().getSelectedItem();
            if (selected != null && pat != null) {
                ouvrirDialogTraitement(pat, selected);
            }
        });

        MenuItem deleteTreatment = new MenuItem("🗑 Supprimer");
        deleteTreatment.setOnAction(e -> supprimerTraitement());

        treatmentContextMenu.getItems().addAll(editTreatment, deleteTreatment);
        tableTraitements.setContextMenu(treatmentContextMenu);
    }

    private VBox buildDetailPatient(Patient p) {
        Label avatar = new Label(initiales(p));
        avatar.getStyleClass().add("patient-avatar-large");

        Label nom = new Label(p.getNomComplet());
        nom.getStyleClass().add("detail-name");

        Label statut = chip(p.isSousSurveillance() ? "⚠ Surveillance" : "Suivi standard",
                p.isSousSurveillance() ? "status-warning" : "status-ok");

        VBox identity = new VBox(4, nom, statut);
        HBox header = new HBox(12, avatar, identity);
        header.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(8);
        grid.getStyleClass().add("detail-grid");
        grid.add(detailItem("Âge", p.getAge() + " ans"), 0, 0);
        grid.add(detailItem("Sexe", valeurOuVide(p.getSexe())), 1, 0);
        grid.add(detailItem("Téléphone", valeurOuVide(p.getTelephone())), 0, 1);
        grid.add(detailItem("Email", valeurOuVide(p.getEmail())), 1, 1);
        grid.add(detailItem("Observations", valeurOuVide(p.getObservations())), 0, 2, 2, 1);

        int treatmentCount = traitementDAO.getParPatient(p.getId()).size();
        Label treatmentInfo = new Label("💊 " + treatmentCount + " traitement(s)");
        treatmentInfo.getStyleClass().add("detail-value");
        grid.add(treatmentInfo, 1, 3);

        VBox v = new VBox(10, header, grid);
        v.setPadding(new Insets(10));
        v.getStyleClass().add("patient-detail");
        return v;
    }

    private VBox detailItem(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("detail-label");

        Label v = new Label(value);
        v.getStyleClass().add("detail-value");
        v.setWrapText(true);

        VBox box = new VBox(1, l, v);
        box.getStyleClass().add("detail-item");
        return box;
    }

    private void animateNode(Node node, String animationType) {
        if (animationType.equals("fadeIn")) {
            node.setOpacity(0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
            slide.setFromY(15);
            slide.setToY(0);
            FadeTransition fade = new FadeTransition(Duration.millis(300), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            ParallelTransition pt = new ParallelTransition(slide, fade);
            pt.play();
        }
    }

    private void setupKeyboardShortcuts(Scene scene, Stage stage) {
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.N) {
                ouvrirDialogPatient(null);
                event.consume();
            }
            if (event.getCode() == KeyCode.DELETE) {
                if (tablePatients != null && tablePatients.isFocused()) {
                    supprimerPatient();
                    event.consume();
                }
                if (tableTraitements != null && tableTraitements.isFocused()) {
                    supprimerTraitement();
                    event.consume();
                }
            }
            if (event.isControlDown() && event.getCode() == KeyCode.Q) {
                Database.fermer();
                Platform.exit();
                event.consume();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                if (tfRecherche != null) {
                    tfRecherche.requestFocus();
                    tfRecherche.selectAll();
                    event.consume();
                }
            }
            if (event.getCode() == KeyCode.ESCAPE) {
                if (tablePatients != null) {
                    tablePatients.getSelectionModel().clearSelection();
                }
                if (tfRecherche != null) {
                    tfRecherche.clear();
                }
            }
        });
    }

    private String initiales(Patient patient) {
        String prenom = patient.getPrenom() != null && !patient.getPrenom().isBlank()
                ? patient.getPrenom().substring(0, 1) : "";
        String nom = patient.getNom() != null && !patient.getNom().isBlank()
                ? patient.getNom().substring(0, 1) : "";
        String initiales = (prenom + nom).toUpperCase();
        return initiales.isBlank() ? "P" : initiales;
    }

    private String valeurOuVide(String valeur) {
        return valeur == null || valeur.isBlank() ? "—" : valeur;
    }

    private void styliserBoutons(Button... btns) {
        for (Button b : btns) {
            b.getStyleClass().addAll("button-medical", "button-primary");
        }
    }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        appliquerTheme(a);
        a.setTitle(titre);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void afficherWarning(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        appliquerTheme(a);
        a.setTitle(titre);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        appliquerTheme(a);
        a.setTitle(titre);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void appliquerTheme(Dialog<?> dialog) {
        dialog.getDialogPane().getStyleClass().add("medical-dialog");
        URL theme = getClass().getResource(THEME_RESOURCE);
        if (theme != null) {
            dialog.getDialogPane().getStylesheets().add(theme.toExternalForm());
        }
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}