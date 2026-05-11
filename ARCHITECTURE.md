# Architecture — Plateforme Intelligente de Gestion Universitaire

## Table des matières
1. [Vue d'ensemble](#1-vue-densemble)
2. [Structure des projets](#2-structure-des-projets)
3. [Projet API — Play Framework](#3-projet-api--play-framework)
4. [Projet Big Data — Apache Spark](#4-projet-big-data--apache-spark)
5. [Données partagées](#5-données-partagées)
6. [Architecture des packages Scala](#6-architecture-des-packages-scala)
7. [Modèle de données](#7-modèle-de-données)
8. [API REST — Endpoints](#8-api-rest--endpoints)
9. [Flux de données](#9-flux-de-données)
10. [Stack technique](#10-stack-technique)
11. [Configuration build.sbt](#11-configuration-buildsbt)

---

## 1. Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────┐
│                CLIENT (navigateur / Postman / front)            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP / JSON
┌──────────────────────────▼──────────────────────────────────────┐
│              PROJET API — Play Framework (Scala)                 │
│                                                                  │
│   Controllers → Services → Repositories → Models                │
│                                                                  │
│   Modules :                                                      │
│   ├── Étudiants        ├── Notes          ├── Paiements         │
│   ├── Enseignants      ├── Absences       ├── Tableau de bord   │
│   ├── Formations       ├── Inscriptions                         │
│   └── Emploi du temps                                           │
└──────────────────────────┬──────────────────────────────────────┘
                           │ lecture / écriture
┌──────────────────────────▼──────────────────────────────────────┐
│                   DONNÉES PARTAGÉES (data/)                      │
│                                                                  │
│   etudiants.csv   notes.csv      absences.csv                   │
│   enseignants.csv paiements.csv  inscriptions.csv               │
│   matieres.csv    salles.csv     emplois_du_temps.csv           │
│                                                                  │
│   output/exports/*.parquet  (résultats Spark)                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │ lecture / écriture
┌──────────────────────────▼──────────────────────────────────────┐
│             PROJET BIG DATA — Apache Spark (Scala)               │
│                                                                  │
│   Chargement CSV → Nettoyage → Analyse → Export CSV/Parquet     │
│                                                                  │
│   Analyses :                                                     │
│   ├── Moyennes par étudiant     ├── Synthèse financière         │
│   ├── Taux de réussite          ├── Performances par promo      │
│   ├── Absences par matière      └── Tendances multi-années      │
│   └── Valeurs manquantes                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Structure des projets

```
gestion-universitaire/
│
├── api/                              ← Projet Play Framework (REST API)
│   ├── build.sbt
│   ├── conf/
│   │   ├── application.conf
│   │   └── routes
│   ├── project/
│   │   └── build.properties
│   └── src/main/scala/universite/
│       ├── controllers/
│       ├── services/
│       ├── repositories/
│       ├── models/
│       ├── dashboard/
│       └── Module.scala
│
├── bigdata/                          ← Projet Apache Spark (batch)
│   ├── build.sbt
│   ├── project/
│   │   └── build.properties
│   └── src/main/scala/universite/
│       └── bigdata/
│           └── SparkAnalyse.scala
│
└── data/                             ← Fichiers CSV partagés
    ├── etudiants.csv
    ├── enseignants.csv
    ├── filieres.csv
    ├── matieres.csv
    ├── inscriptions.csv
    ├── notes.csv
    ├── absences.csv
    ├── paiements.csv
    ├── salles.csv
    ├── emplois_du_temps.csv
    └── output/
        ├── rapports/
        ├── statistiques/
        │   └── indicateurs.csv
        └── exports/
            ├── etudiants_par_filiere/
            ├── moyennes_etudiants/
            ├── taux_reussite/
            └── performances_promo/
```

---

## 3. Projet API — Play Framework

### Architecture interne (couches)

```
HTTP Request
     │
     ▼
┌──────────────────────────────────────┐
│           CONTROLLERS                │  ← reçoit la requête HTTP,
│  EtudiantController                  │    retourne du JSON
│  NoteController                      │
│  AbsenceController                   │
│  PaiementController                  │
│  EnseignantController                │
│  EmploiDuTempsController             │
│  DashboardController                 │
└──────────────────┬───────────────────┘
                   │ appelle
┌──────────────────▼───────────────────┐
│             SERVICES                 │  ← logique métier,
│  EtudiantService                     │    calculs, validations
│  NoteService                         │
│  AbsenceService                      │
│  PaiementService                     │
│  EnseignantService                   │
│  EmploiDuTempsService                │
└──────────────────┬───────────────────┘
                   │ appelle
┌──────────────────▼───────────────────┐
│           REPOSITORIES               │  ← accès aux données CSV
│  EtudiantRepository                  │
│  NoteRepository                      │
│  AbsenceRepository                   │
│  PaiementRepository                  │
│  EnseignantRepository                │
│  MatiereRepository                   │
└──────────────────┬───────────────────┘
                   │ lit / écrit
┌──────────────────▼───────────────────┐
│            MODELS (case class)       │  ← objets métier Scala
│  Etudiant, Enseignant, Note          │
│  Absence, Paiement, Matiere          │
│  Inscription, Salle, SeanceCours     │
│  Filiere, Niveau, UniteEnseignement  │
└──────────────────────────────────────┘
```

### Traits Scala utilisés

| Trait         | Utilisé par                          |
|---------------|--------------------------------------|
| `Identifiable`  | Etudiant, Enseignant, Matiere       |
| `Affichable`    | Etudiant, Note, SeanceCours         |
| `Validable`     | Note, Paiement, Inscription         |
| `Calculable`    | NoteService, PaiementService        |
| `Recherchable`  | EtudiantRepository, NoteRepository  |

---

## 4. Projet Big Data — Apache Spark

### Pipeline de traitement

```
Fichiers CSV (data/)
       │
       ▼
┌─────────────────────┐
│  Chargement         │  spark.read.csv(...)
│  DataFrames         │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  Nettoyage          │  na.drop(), filter(), dropDuplicates()
│  Qualité données    │  rapport valeurs manquantes
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  Analyses           │  groupBy(), agg(), join(), withColumn()
│  - Moyennes         │
│  - Taux réussite    │
│  - Absences         │
│  - Finance          │
│  - Promotions       │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  Export             │  CSV (coalesce(1)) ou Parquet
│  data/output/       │
└─────────────────────┘
```

### Analyses produites

| Analyse                        | Méthode Spark                  | Format sortie |
|-------------------------------|-------------------------------|---------------|
| Étudiants par filière          | `groupBy` + `count`           | CSV           |
| Moyennes par étudiant          | `avg` + `withColumn`          | CSV           |
| Taux de réussite par filière   | `join` + `sum` + `round`      | CSV           |
| Absences par étudiant          | `groupBy` + `sum`             | —             |
| Synthèse financière            | `agg` global                  | —             |
| Tendance absences par matière  | `join` + `groupBy`            | —             |
| Performances par promotion     | `join` + `groupBy` niveau     | Parquet       |

---

## 5. Données partagées

### Fichiers CSV — schémas

#### `etudiants.csv`
```
matricule, nom, prenom, sexe, date_naissance, email,
telephone, filiere, niveau, annee, statut
```

#### `notes.csv`
```
id_note, matricule, matiere, controle_continu, examen
```

#### `absences.csv`
```
id_absence, matricule, matiere, date_absence, heures, justifiee
```

#### `paiements.csv`
```
id_paiement, matricule, montant_total, montant_paye,
date_paiement, mode
```

#### `matieres.csv`
```
id_matiere, nom_matiere, ue, coefficient, volume_horaire, enseignant
```

#### `emplois_du_temps.csv`
```
id_seance, matiere, enseignant, salle, jour,
heure_debut, heure_fin, filiere, niveau
```

---

## 6. Architecture des packages Scala

### Projet API

```
universite/
├── models/
│   ├── Etudiant.scala
│   ├── Enseignant.scala
│   ├── Note.scala
│   ├── Absence.scala
│   ├── Paiement.scala
│   ├── Matiere.scala
│   ├── Inscription.scala
│   ├── Salle.scala
│   ├── SeanceCours.scala
│   ├── Filiere.scala
│   ├── Niveau.scala
│   └── UniteEnseignement.scala
│
├── traits/
│   ├── Identifiable.scala
│   ├── Affichable.scala
│   ├── Validable.scala
│   ├── Calculable.scala
│   └── Recherchable.scala
│
├── repositories/
│   ├── EtudiantRepository.scala
│   ├── NoteRepository.scala
│   ├── AbsenceRepository.scala
│   ├── PaiementRepository.scala
│   ├── EnseignantRepository.scala
│   └── MatiereRepository.scala
│
├── services/
│   ├── EtudiantService.scala
│   ├── NoteService.scala
│   ├── AbsenceService.scala
│   ├── PaiementService.scala
│   ├── EnseignantService.scala
│   └── EmploiDuTempsService.scala
│
├── controllers/
│   ├── EtudiantController.scala
│   ├── NoteController.scala
│   ├── AbsenceController.scala
│   ├── PaiementController.scala
│   ├── EnseignantController.scala
│   ├── EmploiDuTempsController.scala
│   └── DashboardController.scala
│
└── dashboard/
    └── TableauBord.scala
```

### Projet Big Data

```
universite/
└── bigdata/
    └── SparkAnalyse.scala
```

---

## 7. Modèle de données

### Diagramme de relations

```
Filiere ──────< Etudiant >────── Inscription
                   │
                   ├──────────< Note >────── Matiere <─── Enseignant
                   │
                   ├──────────< Absence >─── Matiere
                   │
                   └──────────< Paiement

Enseignant ────< SeanceCours >── Matiere
Salle ─────────< SeanceCours
```

### Formules de calcul

```
Moyenne matière     = (CC × 0.4) + (Examen × 0.6)
Moyenne générale    = moyenne de toutes les moyennes matières
Décision            = "Admis" si moyenne ≥ 10, sinon "Ajourné"
Reste à payer       = montant_total - montant_paye
Taux recouvrement   = (montant_paye / montant_total) × 100
Taux réussite       = (nb admis / nb total) × 100
Taux absentéisme    = (total heures absence / total heures cours) × 100
```

---

## 8. API REST — Endpoints

### Étudiants — `GET /api/etudiants`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/etudiants`                      | Liste tous les étudiants           |
| GET     | `/api/etudiants/:matricule`           | Détail d'un étudiant               |
| GET     | `/api/etudiants/filiere/:filiere`     | Filtrer par filière                |
| GET     | `/api/etudiants/niveau/:niveau`       | Filtrer par niveau                 |
| GET     | `/api/etudiants/actifs`               | Étudiants actifs                   |
| GET     | `/api/etudiants/suspendus`            | Étudiants suspendus                |

### Notes — `GET /api/notes`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/notes/:matricule`               | Relevé de notes                    |
| GET     | `/api/notes/:matricule/moyenne`       | Moyenne générale                   |
| GET     | `/api/notes/classement`               | Classement de tous les étudiants   |
| GET     | `/api/notes/top5`                     | Top 5 étudiants                    |
| GET     | `/api/notes/ajournes`                 | Étudiants ajournés                 |
| GET     | `/api/notes/matieres/difficiles`      | Matières les plus difficiles       |

### Absences — `GET /api/absences`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/absences/:matricule`            | Absences d'un étudiant             |
| GET     | `/api/absences/non-justifiees`        | Absences non justifiées            |
| GET     | `/api/absences/risque`                | Étudiants > 10h d'absence          |
| GET     | `/api/absences/taux-global`           | Taux d'absentéisme global          |
| GET     | `/api/absences/par-matiere`           | Rapport par matière                |

### Paiements — `GET /api/paiements`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/paiements/:matricule`           | Paiement d'un étudiant             |
| GET     | `/api/paiements/dettes`               | Étudiants en retard                |
| GET     | `/api/paiements/synthese`             | Synthèse financière globale        |
| GET     | `/api/paiements/synthese/filiere`     | Synthèse par filière               |

### Enseignants — `GET /api/enseignants`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/enseignants`                    | Liste tous les enseignants         |
| GET     | `/api/enseignants/:id/cours`          | Cours d'un enseignant              |
| GET     | `/api/enseignants/volumes`            | Volumes horaires                   |

### Emploi du temps — `GET /api/emplois`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/emplois/filiere/:filiere`       | Emploi par filière                 |
| GET     | `/api/emplois/enseignant/:id`         | Emploi d'un enseignant             |
| GET     | `/api/emplois/salle/:id`              | Séances dans une salle             |
| GET     | `/api/emplois/conflits`               | Détecter les conflits              |

### Tableau de bord — `GET /api/dashboard`

| Méthode | Endpoint                              | Description                        |
|---------|---------------------------------------|------------------------------------|
| GET     | `/api/dashboard`                      | Tous les indicateurs               |
| GET     | `/api/dashboard/export`               | Export CSV des indicateurs         |

---

## 9. Flux de données

### Requête REST typique

```
Client
  │
  │  GET /api/notes/ETU001/moyenne
  ▼
EtudiantController
  │  noteService.moyenneGenerale("ETU001")
  ▼
NoteService
  │  noteRepo.parEtudiant("ETU001")
  ▼
NoteRepository
  │  lit notes.csv, filtre par matricule
  ▼
List[Note]  →  calcul moyenne  →  Double
  │
  ▼
Json.obj("matricule" -> "ETU001", "moyenne" -> 14.6)
  │
  ▼
Client  ←  HTTP 200  {"matricule":"ETU001","moyenne":14.6}
```

### Pipeline Big Data (batch)

```
sbt run (bigdata/)
  │
  ▼
SparkAnalyse.lancerAnalyses()
  │
  ├── charger CSV  →  DataFrame
  ├── nettoyer     →  DataFrame propre
  ├── analyser     →  DataFrame résultat
  └── sauvegarder  →  data/output/exports/*.csv / *.parquet
```

---

## 10. Stack technique

| Composant       | Technologie              | Version   |
|-----------------|--------------------------|-----------|
| Langage         | Scala                    | 2.13.12   |
| Build tool      | sbt                      | 1.9.7     |
| Web framework   | Play Framework           | 2.9.x     |
| Big Data        | Apache Spark             | 3.5.0     |
| Format données  | CSV / Parquet            | —         |
| Runtime         | Java JDK                 | 17        |
| IDE recommandé  | IntelliJ IDEA            | —         |
| Versionnement   | Git                      | —         |

---

## 11. Configuration build.sbt

### `api/build.sbt`

```scala
name := "gestion-universitaire-api"
version := "0.1"
scalaVersion := "2.13.12"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
)
```

### `bigdata/build.sbt`

```scala
name := "gestion-universitaire-bigdata"
version := "0.1"
scalaVersion := "2.13.12"

mainClass := Some("universite.bigdata.SparkAnalyse")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql"  % "3.5.0"
)

// Spark uses log4j — avoid conflicts
libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always
```

### `project/build.properties` (les deux projets)

```
sbt.version=1.9.7
```

---

## Démarrage

```bash
# 1. Lancer l'API Play
cd api
sbt run
# → API disponible sur http://localhost:9000

# 2. Lancer l'analyse Spark (dans un autre terminal)
cd bigdata
sbt run
# → Résultats exportés dans data/output/exports/
```
