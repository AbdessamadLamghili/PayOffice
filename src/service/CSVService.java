package service;

import model.Etudiant;
import model.Paiement;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CSVService — Single source of truth for all CSV persistence in PayOffice.
 *
 * Strategy: two CSV files side by side on disk:
 *   etudiants.csv  — base student data (id, nom, prenom, classe, fraisInscription)
 *   paiements.csv  — every payment ever made (paiementId, etudiantId, montant, date)
 *
 * On load  : both files are read; payments are attached to their parent Etudiant.
 * On save  : both files are fully rewritten from the in-memory list (write-through).
 *
 * The CSV path is resolved once from the classpath and cached so all calls
 * write to the same physical file on disk (not inside the JAR).
 */
public class CSVService {

    // ─── Date format shared between read and write ─────────────────────────────
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ─── CSV headers ───────────────────────────────────────────────────────────
    private static final String HEADER_ETUDIANTS  = "id,nom,prenom,classe,fraisInscription";
    private static final String HEADER_PAIEMENTS  = "paiementId,etudiantId,montant,date";

    // ─── Resolved file paths (set once on first load) ──────────────────────────
    private String csvEtudiantsPath  = null;
    private String csvPaiementsPath  = null;

    // ──────────────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads all students (with their payments) from the two CSV files.
     * Automatically resolves the file paths from the classpath on first call.
     *
     * @return mutable list of Etudiant objects with payments already attached
     */
    public List<Etudiant> chargerTout() {
        resoudreChemin();                              // locate files once

        List<Etudiant> etudiants = lireEtudiants();   // parse etudiants.csv
        List<Paiement> paiements = lirePaiements();   // parse paiements.csv

        // Attach each payment to the right student
        for (Paiement p : paiements) {
            etudiants.stream()
                     .filter(e -> e.getId() == p.getEtudiantId())
                     .findFirst()
                     .ifPresent(e -> e.addPaiement(p));
        }

        System.out.printf("[CSVService] Chargé : %d étudiant(s), %d paiement(s)%n",
                etudiants.size(), paiements.size());
        return etudiants;
    }

    /**
     * Persists the full in-memory state to both CSV files.
     * Called after every mutation (new payment, fee edit, deletion).
     *
     * @param etudiants the authoritative in-memory list
     */
    public void sauvegarderTout(List<Etudiant> etudiants) {
        if (csvEtudiantsPath == null || csvPaiementsPath == null) {
            System.err.println("[CSVService] Chemin non initialisé — sauvegarde annulée.");
            return;
        }
        ecrireEtudiants(etudiants);
        ecrirePaiements(etudiants);
        System.out.println("[CSVService] Sauvegarde effectuée.");
    }

    /**
     * Loads students from an arbitrary CSV file chosen by the user (Import button).
     * Payments are NOT loaded here — the file contains only base student data.
     * After import, call sauvegarderTout() to persist the new list.
     *
     * @param filePath absolute path to the CSV file
     * @return list of Etudiant without payments
     */
    public List<Etudiant> importerDepuisCSV(String filePath) {
        List<Etudiant> etudiants = new ArrayList<>();
        try (BufferedReader reader = nouveauReader(filePath)) {
            String ligne;
            boolean first = true;
            while ((ligne = reader.readLine()) != null) {
                if (first) { first = false; continue; }  // skip header
                if (ligne.isBlank()) continue;
                Etudiant e = parseEtudiant(ligne);
                if (e != null) etudiants.add(e);
            }
        } catch (IOException ex) {
            System.err.println("[CSVService] Erreur import : " + ex.getMessage());
        }
        System.out.printf("[CSVService] Import : %d étudiant(s) depuis %s%n",
                etudiants.size(), filePath);
        return etudiants;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  PATH RESOLUTION
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Finds etudiants.csv on disk (not inside the JAR) once and derives the
     * sibling paiements.csv path next to it.
     */
    private void resoudreChemin() {
        if (csvEtudiantsPath != null) return;          // already done

        // Try to locate via classpath
        URL url = getClass().getResource("/data/etudiants.csv");
        if (url != null) {
            try {
                File f = new File(url.toURI());
                csvEtudiantsPath = f.getAbsolutePath();
                csvPaiementsPath = new File(f.getParent(), "paiements.csv").getAbsolutePath();
                System.out.println("[CSVService] Fichiers CSV : " + f.getParent());
                return;
            } catch (URISyntaxException | IllegalArgumentException ex) {
                // Fallback: use getPath() which works for file:// URLs
                csvEtudiantsPath = url.getPath();
                csvPaiementsPath = csvEtudiantsPath.replace("etudiants.csv", "paiements.csv");
                return;
            }
        }

        // Last-resort fallback: current working directory
        csvEtudiantsPath = "src/data/etudiants.csv";
        csvPaiementsPath = "src/data/paiements.csv";
        System.err.println("[CSVService] Avertissement : chemin par défaut utilisé — " + csvEtudiantsPath);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  READ
    // ──────────────────────────────────────────────────────────────────────────

    /** Reads etudiants.csv → list of Etudiant (no payments attached yet). */
    private List<Etudiant> lireEtudiants() {
        List<Etudiant> list = new ArrayList<>();
        File f = new File(csvEtudiantsPath);
        if (!f.exists()) return list;

        try (BufferedReader reader = nouveauReader(csvEtudiantsPath)) {
            String ligne;
            boolean first = true;
            while ((ligne = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                if (ligne.isBlank()) continue;
                Etudiant e = parseEtudiant(ligne);
                if (e != null) list.add(e);
            }
        } catch (IOException ex) {
            System.err.println("[CSVService] Erreur lecture etudiants.csv : " + ex.getMessage());
        }
        return list;
    }

    /** Reads paiements.csv → flat list of Paiement. */
    private List<Paiement> lirePaiements() {
        List<Paiement> list = new ArrayList<>();
        File f = new File(csvPaiementsPath);
        if (!f.exists()) return list;   // first run — no payments yet

        try (BufferedReader reader = nouveauReader(csvPaiementsPath)) {
            String ligne;
            boolean first = true;
            while ((ligne = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                if (ligne.isBlank()) continue;
                Paiement p = parsePaiement(ligne);
                if (p != null) list.add(p);
            }
        } catch (IOException ex) {
            System.err.println("[CSVService] Erreur lecture paiements.csv : " + ex.getMessage());
        }
        return list;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  WRITE
    // ──────────────────────────────────────────────────────────────────────────

    /** Rewrites etudiants.csv completely from the in-memory list. */
    private void ecrireEtudiants(List<Etudiant> etudiants) {
        try (BufferedWriter w = nouveauWriter(csvEtudiantsPath)) {
            w.write(HEADER_ETUDIANTS);
            w.newLine();
            for (Etudiant e : etudiants) {
                w.write(String.format("%d,%s,%s,%s,%.2f",
                        e.getId(),
                        echapper(e.getNom()),
                        echapper(e.getPrenom()),
                        echapper(e.getClasse()),
                        e.getFraisInscription()));
                w.newLine();
            }
        } catch (IOException ex) {
            System.err.println("[CSVService] Erreur écriture etudiants.csv : " + ex.getMessage());
        }
    }

    /** Rewrites paiements.csv completely — all payments from all students. */
    private void ecrirePaiements(List<Etudiant> etudiants) {
        try (BufferedWriter w = nouveauWriter(csvPaiementsPath)) {
            w.write(HEADER_PAIEMENTS);
            w.newLine();
            for (Etudiant e : etudiants) {
                for (Paiement p : e.getPaiements()) {
                    w.write(String.format("%d,%d,%.2f,%s",
                            p.getId(),
                            p.getEtudiantId(),
                            p.getMontant(),
                            p.getDatePaiement().format(DATE_FMT)));
                    w.newLine();
                }
            }
        } catch (IOException ex) {
            System.err.println("[CSVService] Erreur écriture paiements.csv : " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  PARSERS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Parses one CSV line into an Etudiant.
     * Expected format: id,nom,prenom,classe,fraisInscription
     */
    private Etudiant parseEtudiant(String ligne) {
        try {
            String[] p = ligne.split(",", -1);
            if (p.length < 5) return null;
            return new Etudiant(
                    Integer.parseInt(p[0].trim()),
                    p[1].trim(),
                    p[2].trim(),
                    p[3].trim(),
                    Double.parseDouble(p[4].trim().replace(",", "."))
            );
        } catch (NumberFormatException ex) {
            System.err.println("[CSVService] Ligne étudiant ignorée : " + ligne);
            return null;
        }
    }

    /**
     * Parses one CSV line into a Paiement.
     * Expected format: paiementId,etudiantId,montant,date (ISO-8601)
     */
    private Paiement parsePaiement(String ligne) {
        try {
            String[] p = ligne.split(",", -1);
            if (p.length < 4) return null;
            return new Paiement(
                    Integer.parseInt(p[0].trim()),
                    Integer.parseInt(p[1].trim()),
                    Double.parseDouble(p[2].trim().replace(",", ".")),
                    LocalDateTime.parse(p[3].trim(), DATE_FMT)
            );
        } catch (Exception ex) {
            System.err.println("[CSVService] Ligne paiement ignorée : " + ligne);
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Escapes a field value: if it contains a comma or quote, wrap in quotes.
     * Prevents CSV corruption for names like "El Alaoui, Mohamed".
     */
    private String echapper(String valeur) {
        if (valeur == null) return "";
        if (valeur.contains(",") || valeur.contains("\"")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }

    private BufferedReader nouveauReader(String path) throws IOException {
        return new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
    }

    private BufferedWriter nouveauWriter(String path) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path, false), StandardCharsets.UTF_8));
    }
}
