package service;

import model.Etudiant;
import model.Paiement;
import model.Recu;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PaiementService handles all payment-related business logic.
 *
 * Responsibilities:
 *  - Validate and register payments
 *  - Persist changes via CSVService after every mutation
 *  - Generate receipts (Recu)
 *  - Export receipts to TXT files
 *  - Query payment history for a student
 */
public class PaiementService {

    // CSVService is injected so PaiementService can persist after each change.
    private final CSVService csvService;
    // The authoritative student list — needed by CSVService to rewrite both files.
    private List<Etudiant> tousLesEtudiants;

    public PaiementService(CSVService csvService) {
        this.csvService = csvService;
    }

    /**
     * Must be called once by the controller after the student list is loaded,
     * so PaiementService can pass it to CSVService when saving.
     */
    public void setListeEtudiants(List<Etudiant> etudiants) {
        this.tousLesEtudiants = etudiants;
    }

    /**
     * Registers a payment for a student after validation.
     * Automatically persists the full state to CSV.
     *
     * @param etudiant the student making the payment
     * @param montant  the amount being paid
     * @return the generated Recu
     * @throws IllegalArgumentException if payment is invalid
     */
    public Recu enregistrerPaiement(Etudiant etudiant, double montant) {
        // ─── Validation ────────────────────────────────────────────────────────
        if (etudiant == null) {
            throw new IllegalArgumentException("Aucun étudiant sélectionné.");
        }
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0.");
        }
        if (etudiant.isFullyPaid()) {
            throw new IllegalArgumentException("Cet étudiant a déjà réglé l'intégralité de ses frais.");
        }
        if (montant > etudiant.getMontantRestant()) {
            throw new IllegalArgumentException(String.format(
                "Le montant saisi (%.2f) dépasse le solde restant (%.2f).",
                montant, etudiant.getMontantRestant()
            ));
        }

        // ─── Create payment and attach to student ──────────────────────────────
        Paiement paiement = new Paiement(etudiant.getId(), montant);
        etudiant.addPaiement(paiement);

        // ─── Persist immediately so data survives a restart ────────────────────
        sauvegarder();

        // ─── Generate receipt ──────────────────────────────────────────────────
        Recu recu = new Recu(etudiant, paiement);

        System.out.println("[PaiementService] Paiement enregistré : " + paiement);
        return recu;
    }

    /**
     * Returns all payments made by a student.
     *
     * @param etudiant the student whose payments to retrieve
     * @return list of Paiement objects
     */
    public List<Paiement> getPaiementsEtudiant(Etudiant etudiant) {
        return etudiant.getPaiements();
    }

    /**
     * Reverses a payment for a student.
     * Automatically persists the full state to CSV.
     *
     * @param etudiant the student whose payment to reverse
     * @param paiement the payment to reverse
     * @throws IllegalArgumentException if payment is invalid
     */
    public void annulerPaiement(Etudiant etudiant, Paiement paiement) {
        // ─── Validation ────────────────────────────────────────────────────────
        if (etudiant == null) {
            throw new IllegalArgumentException("Aucun étudiant sélectionné.");
        }
        if (paiement == null) {
            throw new IllegalArgumentException("Aucun paiement sélectionné.");
        }
        if (!etudiant.getPaiements().contains(paiement)) {
            throw new IllegalArgumentException("Ce paiement n'appartient pas à cet étudiant.");
        }

        // ─── Remove payment from student ───────────────────────────────────────
        etudiant.removePaiement(paiement);

        // ─── Persist immediately so data survives a restart ────────────────────
        sauvegarder();

        System.out.println("[PaiementService] Paiement annulé : " + paiement);
    }

    /**
     * Exports a receipt to a TXT file in the current directory.
     *
     * @param recu the receipt to export
     * @return the file path where the receipt was saved
     */
    public String exporterRecu(Recu recu) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("recu_%d_%s.txt", recu.getNumero(), dateStr);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(recu.genererTexte());
            System.out.println("[PaiementService] Reçu exporté : " + fileName);
        } catch (IOException e) {
            System.err.println("[PaiementService] Erreur lors de l'export du reçu : " + e.getMessage());
            return null;
        }

        return fileName;
    }

    /**
     * Calculates the total amount paid by a student.
     */
    public double calculerTotalPaye(Etudiant etudiant) {
        return etudiant.getTotalPaye();
    }

    /**
     * Calculates the remaining balance owed by a student.
     */
    public double calculerSoldeRestant(Etudiant etudiant) {
        return etudiant.getMontantRestant();
    }

    // ─── Internal persistence helper ──────────────────────────────────────────

    /**
     * Delegates full persistence to CSVService.
     * Safe to call even if the list has not been set (logs a warning instead of crashing).
     */
    private void sauvegarder() {
        if (tousLesEtudiants == null) {
            System.err.println("[PaiementService] Liste étudiants non définie — sauvegarde ignorée.");
            return;
        }
        csvService.sauvegarderTout(tousLesEtudiants);
    }
}
