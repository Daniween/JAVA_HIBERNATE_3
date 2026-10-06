package org.clinique.vue;

import org.clinique.dao.MedecinDAO;
import org.clinique.dao.PatientDAO;
import org.clinique.modele.Medecin;
import org.clinique.modele.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class MedecinFxmlControleur {

    @FXML
    private TableView<Medecin> tableMedecins;

    @FXML
    private TableColumn<Medecin, Integer> colMedecinId;
    @FXML
    private TableColumn<Medecin, String> colMedecinNom;
    @FXML
    private TableColumn<Medecin, String> colMedecinPrenom;
    @FXML
    private TableColumn<Medecin, String> colMedecinSpecialite;

    @FXML
    private TableView<Patient> tablePatients;

    @FXML
    private TableColumn<Patient, Integer> colPatientId;
    @FXML
    private TableColumn<Patient, String> colPatientNom;
    @FXML
    private TableColumn<Patient, String> colPatientPrenom;
    @FXML
    private TableColumn<Patient, String> colPatientTelephone;

    // --- Formulaire de saisie ---

    @FXML
    private TextField champNom;
    @FXML
    private TextField champPrenom;
    @FXML
    private TextField champSpecialite;

    @FXML
    private Label labelMessage;

    // --- Accès aux données : le contrôleur s'appuie sur les DAO, jamais sur du SQL direct ---

    private final MedecinDAO medecinDAO = new MedecinDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ObservableList<Medecin> medecins = FXCollections.observableArrayList();
    private final ObservableList<Patient> patientsDuMedecin = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colMedecinId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMedecinNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colMedecinPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colMedecinSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));

        colPatientId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatientNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPatientPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colPatientTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        tableMedecins.setItems(medecins);
        tablePatients.setItems(patientsDuMedecin);

        // Au clic sur un médecin : on le recopie dans le formulaire (pour le modifier)
        // et on parcourt la relation dans l'autre sens pour afficher ses patients.
        tableMedecins.getSelectionModel().selectedItemProperty().addListener(
                (observable, ancienneSelection, nouvelleSelection) -> {
                    if (nouvelleSelection != null) {
                        remplirFormulaire(nouvelleSelection);
                        afficherPatients(nouvelleSelection);
                    } else {
                        viderPatients();
                    }
                });

        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            medecins.setAll(medecinDAO.trouverTous());
            afficherMessage("");
        } catch (RuntimeException e) {
            afficherMessage("Erreur de connexion à la base : " + e.getMessage());
        }
    }

    @FXML
    private void onAjouter() {
        if (!formulaireValide()) {
            return;
        }

        try {
            Medecin nouveau = new Medecin(
                    champNom.getText().trim(),
                    champPrenom.getText().trim(),
                    champSpecialite.getText().trim());
            medecinDAO.ajouter(nouveau);
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Médecin ajouté.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        Medecin selectionne = tableMedecins.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherMessage("Sélectionnez d'abord un médecin dans le tableau");
            return;
        }
        if (!formulaireValide()) {
            return;
        }

        try {
            selectionne.setNom(champNom.getText().trim());
            selectionne.setPrenom(champPrenom.getText().trim());
            selectionne.setSpecialite(champSpecialite.getText().trim());

            medecinDAO.modifier(selectionne);
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Médecin modifié.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        Medecin selectionne = tableMedecins.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherMessage("Sélectionnez d'abord un médecin dans le tableau");
            return;
        }

        try {
            // Contrainte de clé étrangère : on ne peut pas supprimer un médecin
            // qui est encore le médecin traitant d'au moins un patient.
            int nbPatients = patientDAO.trouverParMedecin(selectionne).size();
            if (nbPatients > 0) {
                afficherMessage("Impossible : ce médecin suit encore " + nbPatients
                        + " patient(s). Changez d'abord leur médecin traitant ou supprimez-les.");
                return;
            }

            medecinDAO.supprimer(selectionne.getId());
            chargerDonnees();
            viderFormulaire();
            afficherMessage("Médecin supprimé.");
        } catch (RuntimeException e) {
            afficherMessage("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onEffacer() {
        tableMedecins.getSelectionModel().clearSelection();
        viderFormulaire();
        afficherMessage("");
    }

    @FXML
    private void onRafraichir() {
        chargerDonnees();
        viderFormulaire();
    }

    // Revient à la vue "Gestion des patients" dans la même fenêtre.
    @FXML
    private void onVoirPatients() {
        try {
            FXMLLoader chargeur = new FXMLLoader(getClass().getResource("patient-vue.fxml"));
            Parent racine = chargeur.load();

            Stage fenetre = (Stage) tableMedecins.getScene().getWindow();
            fenetre.setTitle("Gestion des patients - BTS SIO SLAM");
            fenetre.getScene().setRoot(racine);
        } catch (IOException e) {
            afficherMessage("Impossible d'ouvrir la vue des patients : " + e.getMessage());
        }
    }

    // --- Méthodes utilitaires privées : détails internes au contrôleur ---

    private boolean formulaireValide() {
        if (champNom.getText().isBlank() || champPrenom.getText().isBlank()
                || champSpecialite.getText().isBlank()) {
            afficherMessage("Le nom, le prénom et la spécialité sont obligatoires");
            return false;
        }
        return true;
    }

    private void remplirFormulaire(Medecin medecin) {
        champNom.setText(medecin.getNom());
        champPrenom.setText(medecin.getPrenom());
        champSpecialite.setText(medecin.getSpecialite());
    }

    private void viderFormulaire() {
        champNom.clear();
        champPrenom.clear();
        champSpecialite.clear();
        viderPatients();
    }

    private void afficherPatients(Medecin medecin) {
        try {
            patientsDuMedecin.setAll(patientDAO.trouverParMedecin(medecin));
            afficherMessage("");
        } catch (RuntimeException e) {
            afficherMessage("Erreur de connexion à la base : " + e.getMessage());
        }
    }

    private void viderPatients() {
        patientsDuMedecin.clear();
    }

    private void afficherMessage(String message) {
        labelMessage.setText(message);
    }
}
