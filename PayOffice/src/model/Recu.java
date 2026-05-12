package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a payment receipt (Recu).
 * Generated automatically after each validated payment.
 * Inherits the date/id from the associated Paiement.
 */
public class Recu {

    private static int compteurRecu = 1000; // Receipts start from 1000

    private int numero;
    private String nomEtudiant;
    private String classeEtudiant;
    private double montantPaye;
    private double montantRestant;
    private double totalFrais;
    private LocalDateTime dateEmission;

    public Recu(Etudiant etudiant, Paiement paiement) {
        this.numero = compteurRecu++;
        this.nomEtudiant = etudiant.getNomComplet();
        this.classeEtudiant = etudiant.getClasse();
        this.montantPaye = paiement.getMontant();
        this.montantRestant = etudiant.getMontantRestant();
        this.totalFrais = etudiant.getFraisInscription();
        this.dateEmission = paiement.getDatePaiement();
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public int getNumero() { return numero; }
    public String getNomEtudiant() { return nomEtudiant; }
    public String getClasseEtudiant() { return classeEtudiant; }
    public double getMontantPaye() { return montantPaye; }
    public double getMontantRestant() { return montantRestant; }
    public double getTotalFrais() { return totalFrais; }
    public LocalDateTime getDateEmission() { return dateEmission; }

    /**
     * Returns the receipt date as a formatted string.
     */
    public String getDateFormatee() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateEmission.format(formatter);
    }

    /**
     * Generates the full text content of the receipt for display or export.
     */
    public String genererTexte() {
        String separator = "═".repeat(46);
        String thin = "─".repeat(46);
        return separator + "\n" +
               "           UNIVERSITÉ - PAYOFFICE          \n" +
               "         REÇU DE PAIEMENT D'INSCRIPTION     \n" +
               separator + "\n" +
               String.format("  N° Reçu      : %d%n", numero) +
               String.format("  Date         : %s%n", getDateFormatee()) +
               thin + "\n" +
               String.format("  Étudiant     : %s%n", nomEtudiant) +
               String.format("  Classe       : %s%n", classeEtudiant) +
               thin + "\n" +
               String.format("  Frais totaux : %.2f MAD%n", totalFrais) +
               String.format("  Montant payé : %.2f MAD%n", montantPaye) +
               String.format("  Reste à payer: %.2f MAD%n", montantRestant) +
               thin + "\n" +
               (montantRestant <= 0
                   ? "  ✔ INSCRIPTION ENTIÈREMENT RÉGLÉE\n"
                   : "  ⚠ SOLDE RESTANT DÛ\n") +
               separator + "\n" +
               "      Signature du caissier : ____________  \n" +
               separator;
    }

    @Override
    public String toString() {
        return String.format("Recu{numero=%d, etudiant='%s', montant=%.2f}", numero, nomEtudiant, montantPaye);
    }
}
