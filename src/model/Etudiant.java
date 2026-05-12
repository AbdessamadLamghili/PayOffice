package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student (Etudiant) in the PayOffice system.
 * Encapsulates all student data and provides calculated financial fields.
 */
public class Etudiant {

    private int id;
    private String nom;
    private String prenom;
    private String classe;
    private double fraisInscription;

    // List of all payments made by this student
    private List<Paiement> paiements;

    public Etudiant(int id, String nom, String prenom, String classe, double fraisInscription) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.classe = classe;
        this.fraisInscription = fraisInscription;
        this.paiements = new ArrayList<>();
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getClasse() { return classe; }
    public double getFraisInscription() { return fraisInscription; }
    public List<Paiement> getPaiements() { return paiements; }

    // ─── Setters ───────────────────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setClasse(String classe) { this.classe = classe; }
    public void setFraisInscription(double fraisInscription) { this.fraisInscription = fraisInscription; }

    // ─── Business Logic ────────────────────────────────────────────────────────

    /**
     * Calculates the total amount paid by this student across all payments.
     */
    public double getTotalPaye() {
        return paiements.stream()
                .mapToDouble(Paiement::getMontant)
                .sum();
    }

    /**
     * Calculates the remaining amount still owed by this student.
     */
    public double getMontantRestant() {
        return fraisInscription - getTotalPaye();
    }

    /**
     * Returns true if the student has fully paid their registration fees.
     */
    public boolean isFullyPaid() {
        return getMontantRestant() <= 0;
    }

    /**
     * Adds a payment to this student's payment list.
     */
    public void addPaiement(Paiement paiement) {
        this.paiements.add(paiement);
    }

    /**
     * Removes a payment from this student's payment list.
     */
    public void removePaiement(Paiement paiement) {
        this.paiements.remove(paiement);
    }

    /**
     * Returns the full name (Nom + Prénom) for display.
     */
    public String getNomComplet() {
        return nom + " " + prenom;
    }

    @Override
    public String toString() {
        return String.format("Etudiant{id=%d, nom='%s', prenom='%s', classe='%s', frais=%.2f}",
                id, nom, prenom, classe, fraisInscription);
    }
}
