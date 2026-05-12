# PayOffice — Gestion des Frais d'Inscription

Application de bureau JavaFX pour la gestion des paiements de frais d'inscription universitaires.

---

## Structure du Projet

```
PayOffice/
├── pom.xml                        ← Configuration Maven
├── README.md
├── diagrams/
│   └── diagrams.puml              ← Diagrammes UML (PlantUML)
└── src/
    ├── model/
    │   ├── Etudiant.java          ← Entité étudiant
    │   ├── Paiement.java          ← Entité paiement
    │   └── Recu.java              ← Entité reçu
    ├── service/
    │   ├── ImportService.java     ← Import CSV
    │   └── PaiementService.java   ← Logique métier paiements
    ├── ui/
    │   ├── Main.java              ← Point d'entrée JavaFX
    │   ├── MainController.java    ← Contrôleur FXML
    │   └── MainView.fxml          ← Interface graphique
    └── data/
        └── etudiants.csv          ← Données d'exemple
```

---

## Prérequis

| Outil     | Version minimale |
|-----------|-----------------|
| Java JDK  | 17+             |
| JavaFX SDK| 21+             |
| Maven     | 3.8+            |

---

## Instructions de Lancement

### Option 1 — Avec Maven (recommandé)

```bash
cd PayOffice
mvn clean javafx:run
```

### Option 2 — IntelliJ IDEA

1. Ouvrir le projet : **File > Open** → sélectionner le dossier `PayOffice`
2. Laisser Maven importer les dépendances automatiquement
3. Aller dans **Run > Edit Configurations**
4. Ajouter une configuration **Maven** avec la commande : `javafx:run`
5. Cliquer **Run**

### Option 3 — VS Code

1. Installer les extensions : **Extension Pack for Java** + **Maven for Java**
2. Ouvrir le dossier du projet
3. Dans le panneau Maven → PayOffice → Plugins → javafx → cliquer `javafx:run`

### Option 4 — Sans Maven (JavaFX manuel)

1. Télécharger JavaFX SDK depuis https://openjfx.io/
2. Compiler :
```bash
javac --module-path /chemin/vers/javafx/lib \
      --add-modules javafx.controls,javafx.fxml \
      -d out \
      src/model/*.java src/service/*.java src/ui/*.java
```
3. Copier `src/data/` et `src/ui/MainView.fxml` dans `out/`
4. Lancer :
```bash
java --module-path /chemin/vers/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out ui.Main
```

---

## Fonctionnalités

### 1. Importation des Étudiants
- Au démarrage, le fichier `src/data/etudiants.csv` est chargé automatiquement
- Le bouton **📂 Importer CSV** permet de charger n'importe quel fichier CSV
- Format attendu : `id,nom,prenom,classe,fraisInscription`

### 2. Tableau des Étudiants
- Affiche tous les étudiants avec leurs frais, montants payés, et soldes restants
- Codage couleur :
  - 🟢 **Vert** : inscription entièrement réglée
  - 🟡 **Jaune** : paiement partiel effectué
  - ⚪ **Blanc** : aucun paiement

### 3. Enregistrement d'un Paiement
1. Cliquer sur un étudiant dans le tableau
2. Saisir le montant dans le champ **Montant (MAD)**
3. Cliquer **✔ Valider le Paiement**
4. Le reçu s'affiche automatiquement

### 4. Historique des Paiements
- S'affiche automatiquement lors de la sélection d'un étudiant
- Montre tous les paiements avec ID, montant et date/heure

### 5. Génération et Export du Reçu
- Le reçu est généré automatiquement après chaque paiement
- Le bouton **💾 Exporter en TXT** sauvegarde le reçu dans un fichier `.txt`

---

## Format du Fichier CSV

```csv
id,nom,prenom,classe,fraisInscription
1,Ayoub,Karim,GI1,5000
2,Ali,Sara,GI2,4500
3,Benali,Youssef,GI1,5000
```

**Règles :**
- La première ligne est l'en-tête (ignorée)
- Les champs sont séparés par des virgules
- `fraisInscription` doit être un nombre décimal

---

## Diagrammes UML

Les diagrammes se trouvent dans `diagrams/diagrams.puml`.

Pour les visualiser :
- Utiliser le plugin **PlantUML** dans IntelliJ IDEA ou VS Code
- Ou coller le contenu sur https://www.plantuml.com/plantuml/uml/

Diagrammes inclus :
1. **Diagramme de Classes** — Architecture OOP du projet
2. **Diagramme des Cas d'Utilisation** — Fonctionnalités vues par l'utilisateur
3. **Diagramme de Séquence** — Flux d'enregistrement d'un paiement

---

## Architecture OOP

| Principe         | Application dans le projet                                    |
|------------------|---------------------------------------------------------------|
| **Encapsulation**| Tous les attributs des classes `model` sont `private`        |
| **Séparation**   | Packages `model`, `service`, `ui` avec responsabilités claires|
| **SRP**          | `ImportService` n'importe que, `PaiementService` ne paie que |
| **Cohésion**     | Chaque classe a un rôle unique et bien défini                 |

---

## Auteur

Projet développé dans le cadre du module Java — Université
Application : PayOffice v1.0
