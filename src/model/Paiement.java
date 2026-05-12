package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single payment (Paiement) made by a student.
 * Immutable after creation to preserve payment records.
 */
public class Paiement {

    private static int compteur = 1; // Auto-increment payment ID counter

    private int id;
    private int etudiantId;
    private double montant;
    private LocalDateTime datePaiement;

    public Paiement(int etudiantId, double montant) {
        this.id = compteur++;
        this.etudiantId = etudiantId;
        this.montant = montant;
        this.datePaiement = LocalDateTime.now();
    }

    // Constructor with explicit ID (used when loading from persistent storage)
    public Paiement(int id, int etudiantId, double montant, LocalDateTime datePaiement) {
        this.id = id;
        this.etudiantId = etudiantId;
        this.montant = montant;
        this.datePaiement = datePaiement;
        if (id >= compteur) compteur = id + 1; // keep counter ahead
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public int getEtudiantId() { return etudiantId; }
    public double getMontant() { return montant; }
    public LocalDateTime getDatePaiement() { return datePaiement; }

    /**
     * Returns the payment date formatted as a readable string.
     */
    public String getDateFormatee() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return datePaiement.format(formatter);
    }

    @Override
    public String toString() {
        return String.format("Paiement{id=%d, etudiantId=%d, montant=%.2f, date=%s}",
                id, etudiantId, montant, getDateFormatee());
    }
}
