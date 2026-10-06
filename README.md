# Tutoriel : CRUD Médecins / Patients avec une relation ManyToOne (Hibernate + JavaFX + MySQL)

Objectif : partir du projet `2_JAVA_Hibernate_ManyToOne` (affichage de la
relation Patient → Medecin) et y ajouter un **CRUD complet** sur les deux
entités, comme dans le projet `1_JAVA_Hibernate` (Etudiant) :

- **Créer**, **modifier**, **supprimer** des médecins ;
- **Créer**, **modifier**, **supprimer** des patients, en choisissant leur
  médecin traitant dans une `ComboBox<Medecin>` ;
- tout se fait **dans la même vue** : le tableau en haut, le formulaire et les
  boutons en bas (pas de fenêtre popup). Le bouton "Gérer les médecins" /
  "Gérer les patients" remplace le contenu de la fenêtre, sans en ouvrir une
  nouvelle.

Ce qui est nouveau par rapport aux deux projets précédents :

| Projet 1 (Etudiant)           | Projet 2 (ManyToOne)                 | Projet 3 (ce projet)                                          |
| ----------------------------- | ------------------------------------ | ------------------------------------------------------------- |
| CRUD sur **une** table        | **Lecture** de deux tables liées     | **CRUD** sur deux tables liées                                |
| Formulaire de `TextField`     | Labels en lecture seule              | `TextField` + **`ComboBox<Medecin>`** pour la clé étrangère   |
| -                             | -                                    | Gestion de la **contrainte de clé étrangère** à la suppression |

Prérequis : identiques aux projets précédents (IntelliJ, JDK 17+, MySQL,
SceneBuilder).

---

## Étape 1 - La base de données

On réutilise **exactement** le script du projet 2 : `database/clinique_db.sql`
(tables `medecin` et `patient`, clé étrangère `patient.medecin_id`).

```
mysql -u root -p < database/clinique_db.sql
```

⚠️ Adapter `hibernate.cfg.xml` (URL de la base, `username`, `password`) à
votre configuration MySQL.

---

## Étape 2 - Créer le projet

Copier le projet 2 et le renommer (ou créer un nouveau projet Maven et y
recopier les fichiers). Dans le `pom.xml`, seul l'`artifactId` change :

```xml
<artifactId>ORMCrudManyToOne</artifactId>
```

Les entités `Medecin.java` et `Patient.java`, `HibernateUtil.java`,
`hibernate.cfg.xml` et `PatientApplication.java` sont **inchangés**.

---

## Étape 3 - Les DAO : ajouter / modifier / supprimer

On ajoute dans `MedecinDAO` et `PatientDAO` les trois méthodes d'écriture
du projet 1 (`EtudiantDAO`). Le principe est toujours le même :
ouvrir une `Session`, démarrer une `Transaction`, faire l'opération,
`commit()`, et `rollback()` en cas d'erreur.

```java
public int ajouter(Patient patient) {
    Transaction transaction = null;

    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
        transaction = session.beginTransaction();
        session.persist(patient);       // INSERT
        transaction.commit();
        return patient.getId();

    } catch (RuntimeException e) {
        if (transaction != null) {
            transaction.rollback();
        }
        throw new RuntimeException("Erreur lors de l'ajout du patient", e);
    }
}
```

- `ajouter` → `session.persist(...)` → `INSERT`
- `modifier` → `session.merge(...)` → `UPDATE`
- `supprimer` → `session.get(...)` puis `session.remove(...)` → `DELETE`

**Point clé ManyToOne** : pour enregistrer un patient, on ne manipule jamais
`medecin_id`. Il suffit que `patient.getMedecin()` contienne un `Medecin`
existant : Hibernate écrit lui-même son id dans la colonne `medecin_id`.

```java
patient.setMedecin(autreMedecin);   // changer de médecin traitant
patientDAO.modifier(patient);       // UPDATE patient SET ..., medecin_id = ? WHERE id = ?
```

Voir les fichiers complets : `src/main/java/org/clinique/dao/MedecinDAO.java`
et `src/main/java/org/clinique/dao/PatientDAO.java`.

---

## Étape 4 - La vue des patients : `patient-vue.fxml`

On garde la vue du projet 2 (tableau au centre, panneau "Médecin traitant"
en bas) et on y ajoute, comme dans le projet 1 :

- un **formulaire** (`GridPane`) : `champNom`, `champPrenom`,
  `champTelephone`, et une **`ComboBox` `comboMedecin`** ;
- une rangée de **boutons** : Ajouter, Modifier, Supprimer, Effacer le
  formulaire, Rafraîchir, Gérer les médecins ;
- une colonne `colMedecin` dans le tableau pour voir le médecin de chaque
  patient.

```xml
<Label text="Médecin :" GridPane.rowIndex="1" GridPane.columnIndex="2"/>
<ComboBox fx:id="comboMedecin" prefWidth="250.0" promptText="Choisir un médecin"
          GridPane.rowIndex="1" GridPane.columnIndex="3"/>
```

La `ComboBox` est la traduction "interface graphique" de la relation
ManyToOne : un patient a **un** médecin, choisi parmi **tous** les médecins.

---

## Étape 5 - Le contrôleur des patients : `PatientFxmlControleur.java`

Même structure que `EtudiantFxmlControleur` du projet 1. Les nouveautés :

### Remplir la ComboBox avec les médecins

```java
private final ObservableList<Medecin> medecins = FXCollections.observableArrayList();

// dans initialize()
comboMedecin.setItems(medecins);

// dans chargerDonnees()
medecins.setAll(medecinDAO.trouverTous());
```

La `ComboBox` affiche le `toString()` de chaque `Medecin` :
`Dr Claire Lefevre (Généraliste)`.

### Créer un patient avec le médecin choisi

```java
Patient nouveau = new Patient(
        champNom.getText().trim(),
        champPrenom.getText().trim(),
        champTelephone.getText().trim(),
        comboMedecin.getValue());       // l'objet Medecin sélectionné
patientDAO.ajouter(nouveau);
```

### Sélectionner le bon médecin quand on clique sur un patient

```java
comboMedecin.setValue(trouverMedecinDansListe(patient.getMedecin()));
```

Pourquoi une méthode `trouverMedecinDansListe` ? Le médecin du patient et
ceux de la `ComboBox` viennent de **deux requêtes différentes** : ce sont
des objets Java distincts, même s'ils ont le même id. On cherche donc dans
la liste le médecin qui a le même id, pour que la `ComboBox` le
sélectionne correctement.

### Validation du formulaire

Avant d'ajouter ou de modifier, `formulaireValide()` vérifie que le nom, le
prénom et le médecin sont renseignés (colonnes `NOT NULL` en base) et
affiche un message dans `labelMessage` sinon.

---

## Étape 6 - La vue et le contrôleur des médecins

`medecin-vue.fxml` garde le tableau des médecins et le tableau "Patients du
médecin sélectionné" du projet 2, avec en plus un formulaire (`champNom`,
`champPrenom`, `champSpecialite`) et les mêmes boutons.

`MedecinFxmlControleur.java` : CRUD identique à celui du projet 1, avec
**une règle en plus pour la suppression**.

### Supprimer un médecin : la contrainte de clé étrangère

En base, `patient.medecin_id` référence `medecin.id`. Si on supprime un
médecin qui suit encore des patients, MySQL refuse :

```
Cannot delete or update a parent row: a foreign key constraint fails
```

Le contrôleur vérifie donc **avant** de supprimer :

```java
int nbPatients = patientDAO.trouverParMedecin(selectionne).size();
if (nbPatients > 0) {
    afficherMessage("Impossible : ce médecin suit encore " + nbPatients
            + " patient(s). Changez d'abord leur médecin traitant ou supprimez-les.");
    return;
}
medecinDAO.supprimer(selectionne.getId());
```

Pour supprimer un médecin, il faut donc d'abord, dans la vue des patients,
réaffecter ses patients à un autre médecin (bouton Modifier) ou les
supprimer.

---

## Étape 7 - Arborescence finale

```
3_JAVA_Hibernate_CRUD_ManyToOne/
├── database/
│   └── clinique_db.sql                 (identique au projet 2)
├── pom.xml
└── src/main/
    ├── java/org/clinique/
    │   ├── PatientApplication.java
    │   ├── modele/
    │   │   ├── Medecin.java             (identique au projet 2)
    │   │   └── Patient.java             (identique au projet 2, porte @ManyToOne)
    │   ├── dao/
    │   │   ├── HibernateUtil.java
    │   │   ├── MedecinDAO.java          (+ ajouter / modifier / supprimer)
    │   │   └── PatientDAO.java          (+ ajouter / modifier / supprimer)
    │   └── vue/
    │       ├── MedecinFxmlControleur.java   (CRUD médecins)
    │       └── PatientFxmlControleur.java   (CRUD patients + ComboBox)
    └── resources/
        ├── hibernate.cfg.xml
        └── org/clinique/vue/
            ├── medecin-vue.fxml
            └── patient-vue.fxml
```

---

## Étape 8 - Lancer et tester

1. Exécuter `database/clinique_db.sql` si ce n'est pas déjà fait.
2. Lancer `PatientApplication` depuis IntelliJ, ou `mvn javafx:run`.

Scénario de test :

1. **Ajouter un patient** : remplir nom, prénom, téléphone, choisir un
   médecin, cliquer sur **Ajouter** → il apparaît dans le tableau avec son
   médecin.
2. **Modifier un patient** : cliquer sur une ligne (le formulaire se
   remplit), changer de médecin dans la `ComboBox`, cliquer sur
   **Modifier** → la colonne "Médecin traitant" est mise à jour.
3. **Supprimer un patient** : sélectionner une ligne, **Supprimer**.
4. Cliquer sur **Gérer les médecins** (la vue change dans la même fenêtre).
5. **Ajouter un médecin**, puis revenir aux patients : il est disponible
   dans la `ComboBox`.
6. **Supprimer un médecin qui a des patients** → message d'erreur. Le
   supprimer après avoir réaffecté ses patients → ça fonctionne.

---

## Dépannage des erreurs fréquentes

| Symptôme                                                                    | Cause probable                                                                       | Solution                                                                                        |
| ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `Cannot delete or update a parent row: a foreign key constraint fails`      | Suppression d'un médecin qui est encore le médecin traitant d'un patient              | Réaffecter ou supprimer ses patients d'abord (le contrôleur le vérifie normalement avant)       |
| `Column 'medecin_id' cannot be null`                                         | Patient enregistré sans médecin choisi                                               | Choisir un médecin dans la `ComboBox` (vérifié par `formulaireValide()`)                         |
| La `ComboBox` reste vide quand on clique sur un patient                      | Le médecin du patient n'a pas été retrouvé dans la liste (liste pas rechargée)        | Cliquer sur **Rafraîchir** : `chargerDonnees()` recharge aussi la liste des médecins            |
| Un nouveau médecin n'apparaît pas dans la `ComboBox`                        | La liste des médecins a été chargée avant son ajout                                   | **Rafraîchir**, ou revenir sur la vue des patients (elle est rechargée à chaque affichage)       |
| `SchemaManagementException: Schema-validation: missing table [medecin]`     | Le script SQL n'a pas été exécuté                                                     | Exécuter `database/clinique_db.sql`                                                              |
| `HibernateException: Unable to open JDBC Connection` / `Access denied`       | Mauvais identifiants dans `hibernate.cfg.xml`, ou MySQL non démarré                    | Vérifier l'URL, le `username`/`password`, et que le service MySQL tourne                         |
| `Location is not set` au chargement du FXML                                  | Le `.fxml` n'est pas dans `src/main/resources/org/clinique/vue/`                       | Vérifier l'emplacement des fichiers FXML                                                          |
