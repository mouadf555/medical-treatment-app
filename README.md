# Application de Suivi de Traitements Médicaux

Application desktop développée avec **JavaFX** et **MySQL** dans le cadre du module
Développement Java IHM — ENSAO GI3 2025/2026.

---

## Prérequis

Avant de commencer, assure-toi d'avoir installé :

- [JDK](https://adoptium.net)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [JavaFX SDK](https://openjfx.io/) — télécharger le SDK compatible avec ta version JDK, extraire le zip
- [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) — choisir "Platform Independent", extraire le zip et garder uniquement le fichier `.jar`
- [WAMP Server](https://www.wampserver.com/) — pour MySQL en local

---

## Installation

### 1. Cloner le projet

```bash
git clone https://github.com/YoussefEl-ghomry1/medical-treatment-app.git
```

Ouvrir le dossier dans **IntelliJ IDEA**.

---

### 2. Ajouter les bibliothèques

`File > Project Structure > Libraries > + > Java`

Ajouter **séparément** :
- Le dossier `lib/` du SDK JavaFX (ex: `C:\javafx-sdk-21\lib`)
- Le fichier `mysql-connector-j-X.X.X.jar`

Cliquer `OK`.

---

### 3. Configurer les VM options

`Run > Edit Configurations > + > Application`

- **Main class** : `com.medical.MedicalTreatmentApp`
- Cliquer `Modify options > Add VM options` et coller :

```
--module-path "C:\javafx-sdk-XX\lib" --add-modules javafx.controls,javafx.fxml
```

> Adapter le chemin selon l'emplacement de ton SDK JavaFX sur ta machine.

Cliquer `OK`.

---

### 4. Créer la base de données

1. Lancer **WAMP Server** et attendre que l'icône soit verte
2. Ouvrir **phpMyAdmin** → `http://localhost/phpmyadmin`
3. Se connecter avec `root` / mot de passe vide
4. Créer une nouvelle base de données nommée `medical_db` avec l'interclassement `utf8mb4_unicode_ci`

> Les tables sont créées automatiquement au premier lancement.

---

### 5. Vérifier la connexion MySQL

Dans `src/com/medical/dao/Database.java` :

```java
private static final String URL      = "jdbc:mysql://localhost:3306/medical_db?useSSL=false&serverTimezone=UTC";
private static final String USER     = "root";
private static final String PASSWORD = "";
```

Adapter `USER` et `PASSWORD` si nécessaire.

---

### 6. Lancer l'application

Cliquer sur le bouton **Run** vert dans IntelliJ.

---

## Structure du projet

```
src/com/medical/
├── model/        → Patient.java, Traitement.java
├── dao/          → Database.java, PatientDAO.java, TraitementDAO.java
├── view/         → MainView.java
├── util/         → ExportUtil.java
└── MedicalTreatmentApp.java
```

---
## Note sur l'architecture

Les opérations CRUD sont réparties dans `PatientDAO.java` et `TraitementDAO.java`
plutôt que dans un seul `Database.java`, en suivant le patron de conception **DAO
(Data Access Object)**. Cela permet de séparer clairement la logique d'accès à la
base de données de l'interface graphique, rendant le code plus lisible et maintenable.
---

## Binôme

- EL-GHOMRY Youssef — ENSAO GI3
- DAOUDI Mouad — ENSAO GI3