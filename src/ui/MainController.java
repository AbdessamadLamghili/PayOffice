package ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import model.Etudiant;
import model.Paiement;
import model.Recu;
import service.CSVService;
import service.PaiementService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * MainController binds the FXML view to the application logic.
 *
 * Changes in v1.1:
 *  - Uses CSVService as the single persistence layer (loaded on start, saved on every change)
 *  - FilteredList wraps the master student list to power the live search field
 *  - PaiementService now receives CSVService and the live student list at initialisation
 */
public class MainController implements Initializable {

    // ─── Services ──────────────────────────────────────────────────────────────
    private final CSVService      csvService      = new CSVService();
    private final PaiementService paiementService = new PaiementService(csvService);

    // ─── Master + filtered student lists ──────────────────────────────────────
    // listeEtudiants : authoritative in-memory list (never filtered itself)
    // listeFiltree   : what the TableView actually shows (filtered by search)
    private ObservableList<Etudiant> listeEtudiants = FXCollections.observableArrayList();
    private FilteredList<Etudiant>   listeFiltree;

    private ObservableList<Paiement> listePaiements = FXCollections.observableArrayList();

    // ─── Student Table FXML Bindings ───────────────────────────────────────────
    @FXML private TableView<Etudiant>            tableEtudiants;
    @FXML private TableColumn<Etudiant, Integer> colId;
    @FXML private TableColumn<Etudiant, String>  colNom;
    @FXML private TableColumn<Etudiant, String>  colPrenom;
    @FXML private TableColumn<Etudiant, String>  colClasse;
    @FXML private TableColumn<Etudiant, Double>  colFrais;
    @FXML private TableColumn<Etudiant, Double>  colPaye;
    @FXML private TableColumn<Etudiant, Double>  colRestant;

    // ─── Search field (new in v1.1) ────────────────────────────────────────────
    @FXML private TextField fieldRecherche;

    // ─── Payment History Table FXML Bindings ───────────────────────────────────
    @FXML private TableView<Paiement>            tablePaiements;
    @FXML private TableColumn<Paiement, Integer> colPaiementId;
    @FXML private TableColumn<Paiement, Double>  colMontant;
    @FXML private TableColumn<Paiement, String>  colDate;

    // ─── Payment Form ─────────────────────────────────────────────────────────
    @FXML private TextField fieldMontant;
    @FXML private Button    btnPayer;
    @FXML private Button    btnImporter;
    @FXML private Button    btnExporterRecu;
    @FXML private Button    btnAnnulerPaiement;

    // ─── Info Labels ──────────────────────────────────────────────────────────
    @FXML private Label labelNomEtudiant;
    @FXML private Label labelFraisTotal;
    @FXML private Label labelTotalPaye;
    @FXML private Label labelSoldeRestant;
    @FXML private Label labelStatut;

    // ─── Receipt Display ──────────────────────────────────────────────────────
    @FXML private TextArea areaRecu;
    @FXML private Label    labelMessage;

    // ─── State ────────────────────────────────────────────────────────────────
    private Etudiant etudiantSelectionne = null;
    private Recu     dernierRecu         = null;

    // ─── Initialization ───────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurerColonnesEtudiants();
        configurerColonnesPaiements();
        configurerRecherche();
        configurerSelectionEtudiant();
        configurerSelectionPaiement();

        // Load all students + their saved payments from CSV files
        chargerDepuisCSV();

        btnExporterRecu.setDisable(true);
        btnAnnulerPaiement.setDisable(true);
    }

    // ─── Table configuration ───────────────────────────────────────────────────

    private void configurerColonnesEtudiants() {
        colId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colNom.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNom()));
        colPrenom.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPrenom()));
        colClasse.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getClasse()));
        colFrais.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getFraisInscription()).asObject());
        colPaye.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getTotalPaye()).asObject());
        colRestant.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getMontantRestant()).asObject());

        // Color rows: green = fully paid, yellow = partial, white = unpaid
        tableEtudiants.setRowFactory(tv -> new TableRow<Etudiant>() {
            @Override
            protected void updateItem(Etudiant item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isFullyPaid()) {
                    setStyle("-fx-background-color: #d4edda;");
                } else if (item.getTotalPaye() > 0) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else {
                    setStyle("");
                }
            }
        });

        // The table shows the FILTERED list — master list is never shown directly
        listeFiltree = new FilteredList<>(listeEtudiants, e -> true);
        tableEtudiants.setItems(listeFiltree);
    }

    private void configurerColonnesPaiements() {
        colPaiementId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colMontant.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getMontant()).asObject());
        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDateFormatee()));
        tablePaiements.setItems(listePaiements);
    }

    // ─── Search configuration ─────────────────────────────────────────────────

    /**
     * Wires the search TextField to the FilteredList predicate.
     *
     * Filter rules (case-insensitive, live as-you-type):
     *   - Partial match on nom
     *   - Partial match on prenom
     *   - Partial match on "nom prenom" combined
     *   - Prefix match on student id (numeric)
     *
     * Empty field → predicate reset → all students shown.
     */
    private void configurerRecherche() {
        fieldRecherche.textProperty().addListener((obs, ancien, terme) -> {
            String t = (terme == null) ? "" : terme.trim().toLowerCase();

            if (t.isEmpty()) {
                listeFiltree.setPredicate(e -> true);
            } else {
                listeFiltree.setPredicate(e -> {
                    if (String.valueOf(e.getId()).startsWith(t))         return true;
                    if (e.getNom().toLowerCase().contains(t))            return true;
                    if (e.getPrenom().toLowerCase().contains(t))         return true;
                    if (e.getNomComplet().toLowerCase().contains(t))     return true;
                    return false;
                });
            }
        });
    }

    /** Clears the search field and resets the filter. */
    @FXML
    private void handleEffacerRecherche() {
        fieldRecherche.clear();
    }

    // ─── Student selection listener ────────────────────────────────────────────

    private void configurerSelectionEtudiant() {
        tableEtudiants.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, nouveau) -> {
                    etudiantSelectionne = nouveau;
                    if (nouveau != null) {
                        mettreAJourInfosEtudiant(nouveau);
                        mettreAJourHistoriquePaiements(nouveau);
                    }
                });
    }

    // ─── Payment selection listener ────────────────────────────────────────────

    private void configurerSelectionPaiement() {
        tablePaiements.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, nouveau) -> {
                    btnAnnulerPaiement.setDisable(nouveau == null);
                });
    }

    // ─── FXML Action Handlers ─────────────────────────────────────────────────

    /**
     * Opens FileChooser, imports a CSV, replaces the in-memory list,
     * and immediately persists both CSV files so the import survives restart.
     */
    @FXML
    private void handleImporterCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un fichier CSV d'étudiants");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File fichier = chooser.showOpenDialog(tableEtudiants.getScene().getWindow());

        if (fichier != null) {
            List<Etudiant> importes = csvService.importerDepuisCSV(fichier.getAbsolutePath());
            listeEtudiants.setAll(importes);
            paiementService.setListeEtudiants(listeEtudiants);
            csvService.sauvegarderTout(listeEtudiants);  // persist imported students
            fieldRecherche.clear();
            afficherMessage("✔ " + importes.size() + " étudiant(s) importé(s) depuis "
                    + fichier.getName(), "success");
            reinitialiserZonePaiement();
        }
    }

    /**
     * Validates and registers a payment.
     * CSVService is called inside PaiementService.enregistrerPaiement(),
     * so data is persisted before this method returns.
     */
    @FXML
    private void handlePayer() {
        if (etudiantSelectionne == null) {
            afficherMessage("⚠ Veuillez sélectionner un étudiant.", "warning");
            return;
        }
        String texte = fieldMontant.getText().trim();
        if (texte.isEmpty()) {
            afficherMessage("⚠ Veuillez saisir un montant.", "warning");
            return;
        }

        try {
            double montant = Double.parseDouble(texte.replace(",", "."));
            Recu recu = paiementService.enregistrerPaiement(etudiantSelectionne, montant);

            dernierRecu = recu;
            mettreAJourInfosEtudiant(etudiantSelectionne);
            mettreAJourHistoriquePaiements(etudiantSelectionne);
            afficherRecu(recu);
            tableEtudiants.refresh();
            fieldMontant.clear();
            btnExporterRecu.setDisable(false);
            afficherMessage("✔ Paiement de " + montant + " MAD enregistré et sauvegardé.", "success");

        } catch (NumberFormatException e) {
            afficherMessage("⚠ Montant invalide. Veuillez saisir un nombre.", "warning");
        } catch (IllegalArgumentException e) {
            afficherMessage("⚠ " + e.getMessage(), "warning");
        }
    }

    /** Exports the last receipt to a TXT file. */
    @FXML
    private void handleExporterRecu() {
        if (dernierRecu == null) {
            afficherMessage("⚠ Aucun reçu à exporter.", "warning");
            return;
        }
        String fichier = paiementService.exporterRecu(dernierRecu);
        if (fichier != null) {
            afficherMessage("✔ Reçu exporté : " + fichier, "success");
        } else {
            afficherMessage("✘ Erreur lors de l'export du reçu.", "error");
        }
    }

    /** Reverses a selected payment after confirmation. */
    @FXML
    private void handleAnnulerPaiement() {
        if (etudiantSelectionne == null) {
            afficherMessage("⚠ Veuillez sélectionner un étudiant.", "warning");
            return;
        }

        Paiement paiementSelectionne = tablePaiements.getSelectionModel().getSelectedItem();
        if (paiementSelectionne == null) {
            afficherMessage("⚠ Veuillez sélectionner un paiement à annuler.", "warning");
            return;
        }

        // Confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer l'annulation");
        confirmation.setHeaderText("Annuler le paiement");
        confirmation.setContentText(String.format(
                "Voulez-vous vraiment annuler ce paiement ?\n\n" +
                "Montant : %.2f MAD\n" +
                "Date : %s",
                paiementSelectionne.getMontant(),
                paiementSelectionne.getDateFormatee()
        ));

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    paiementService.annulerPaiement(etudiantSelectionne, paiementSelectionne);
                    
                    // Update UI
                    mettreAJourInfosEtudiant(etudiantSelectionne);
                    mettreAJourHistoriquePaiements(etudiantSelectionne);
                    tableEtudiants.refresh();
                    areaRecu.clear();
                    btnExporterRecu.setDisable(true);
                    dernierRecu = null;
                    
                    afficherMessage("✔ Paiement de " + paiementSelectionne.getMontant() + 
                            " MAD annulé avec succès.", "success");
                    
                } catch (IllegalArgumentException e) {
                    afficherMessage("⚠ " + e.getMessage(), "warning");
                }
            }
        });
    }

    // ─── Private UI helpers ────────────────────────────────────────────────────

    /**
     * Loads students + payments via CSVService on startup,
     * then hands the live ObservableList to PaiementService for future saves.
     */
    private void chargerDepuisCSV() {
        List<Etudiant> etudiants = csvService.chargerTout();
        listeEtudiants.setAll(etudiants);
        // Give PaiementService the live list so it can persist the full state
        paiementService.setListeEtudiants(listeEtudiants);

        if (etudiants.isEmpty()) {
            afficherMessage("ℹ Aucun étudiant trouvé. Importez un fichier CSV.", "info");
        } else {
            afficherMessage("✔ " + etudiants.size()
                    + " étudiant(s) chargé(s) avec leur historique de paiements.", "success");
        }
    }

    private void mettreAJourInfosEtudiant(Etudiant etudiant) {
        labelNomEtudiant.setText(etudiant.getNomComplet() + " — " + etudiant.getClasse());
        labelFraisTotal.setText(String.format("%.2f MAD", etudiant.getFraisInscription()));
        labelTotalPaye.setText(String.format("%.2f MAD", etudiant.getTotalPaye()));
        labelSoldeRestant.setText(String.format("%.2f MAD", etudiant.getMontantRestant()));

        if (etudiant.isFullyPaid()) {
            labelStatut.setText("✔ SOLDÉ");
            labelStatut.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            fieldMontant.setDisable(true);
            btnPayer.setDisable(true);
        } else {
            labelStatut.setText("⚠ EN COURS");
            labelStatut.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            fieldMontant.setDisable(false);
            btnPayer.setDisable(false);
        }
    }

    private void mettreAJourHistoriquePaiements(Etudiant etudiant) {
        listePaiements.setAll(etudiant.getPaiements());
    }

    private void afficherRecu(Recu recu) {
        areaRecu.setText(recu.genererTexte());
    }

    private void reinitialiserZonePaiement() {
        etudiantSelectionne = null;
        dernierRecu         = null;
        labelNomEtudiant.setText("—");
        labelFraisTotal.setText("—");
        labelTotalPaye.setText("—");
        labelSoldeRestant.setText("—");
        labelStatut.setText("—");
        labelStatut.setStyle("");
        listePaiements.clear();
        areaRecu.clear();
        fieldMontant.clear();
        fieldMontant.setDisable(true);
        btnPayer.setDisable(true);
        btnExporterRecu.setDisable(true);
    }

    private void afficherMessage(String message, String type) {
        labelMessage.setText(message);
        switch (type) {
            case "success": labelMessage.setStyle("-fx-text-fill: #27ae60;"); break;
            case "warning": labelMessage.setStyle("-fx-text-fill: #e67e22;"); break;
            case "error":   labelMessage.setStyle("-fx-text-fill: #c0392b;"); break;
            default:        labelMessage.setStyle("-fx-text-fill: #2c3e50;");
        }
    }
}
