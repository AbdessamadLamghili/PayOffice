package service;

import model.Etudiant;

import java.util.List;

/**
 * ImportService — kept for backward compatibility and API clarity.
 *
 * In v1.1 all actual CSV I/O is handled by CSVService.
 * ImportService now delegates to it, so any existing code referencing
 * ImportService continues to compile and work unchanged.
 */
public class ImportService {

    private final CSVService csvService;

    /** Default constructor — creates its own CSVService instance. */
    public ImportService() {
        this.csvService = new CSVService();
    }

    /** Constructor for dependency injection (e.g. sharing one CSVService). */
    public ImportService(CSVService csvService) {
        this.csvService = csvService;
    }

    /**
     * Loads students from a CSV file at the given file path.
     * Delegates to CSVService.importerDepuisCSV().
     *
     * @param filePath absolute or relative path to the CSV file
     * @return list of Etudiant objects (no payments attached)
     */
    public List<Etudiant> chargerDepuisCSV(String filePath) {
        return csvService.importerDepuisCSV(filePath);
    }

    /**
     * Loads students from a CSV resource on the classpath.
     * Resolves the URL and delegates to chargerDepuisCSV().
     *
     * @param resourcePath e.g. "/data/etudiants.csv"
     * @return list of Etudiant objects
     */
    public List<Etudiant> chargerDepuisRessource(String resourcePath) {
        java.net.URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("[ImportService] Ressource introuvable : " + resourcePath);
            return new java.util.ArrayList<>();
        }
        return csvService.importerDepuisCSV(url.getPath());
    }
}
