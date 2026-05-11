-- Schema initial pour la gestion universitaire (Contraintes assouplies pour migration)

-- !Ups

CREATE TABLE filieres (
    id_filiere VARCHAR(50) PRIMARY KEY,
    nom_filiere VARCHAR(255) NOT NULL,
    responsable VARCHAR(255)
);

CREATE TABLE formations (
    id_formation VARCHAR(50) PRIMARY KEY,
    nom_formation VARCHAR(255) NOT NULL,
    description TEXT,
    duree_annees INTEGER,
    responsable VARCHAR(255)
);

CREATE TABLE niveaux (
    id_niveau VARCHAR(50) PRIMARY KEY,
    filiere VARCHAR(50), -- REFERENCES filieres(id_filiere),
    niveau_etudes VARCHAR(50) NOT NULL,
    semestres TEXT 
);

CREATE TABLE semestres (
    id_semestre VARCHAR(50) PRIMARY KEY,
    nom_semestre VARCHAR(255) NOT NULL,
    niveau VARCHAR(50), -- REFERENCES niveaux(id_niveau),
    filiere VARCHAR(50), -- REFERENCES filieres(id_filiere),
    ues TEXT
);

CREATE TABLE ues (
    id_ue VARCHAR(50) PRIMARY KEY,
    nom_ue VARCHAR(255) NOT NULL,
    filiere VARCHAR(50),
    niveau VARCHAR(50),
    semestre VARCHAR(50),
    coefficient_total INTEGER,
    matieres TEXT
);

CREATE TABLE enseignants (
    id_enseignant VARCHAR(50) PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    grade VARCHAR(100),
    specialite VARCHAR(255),
    departement VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    telephone VARCHAR(50)
);

CREATE TABLE matieres (
    id_matiere VARCHAR(50) PRIMARY KEY,
    nom_matiere VARCHAR(255) NOT NULL,
    ue VARCHAR(50),
    coefficient INTEGER,
    volume_horaire INTEGER,
    id_enseignant VARCHAR(50)
);

CREATE TABLE etudiants (
    matricule VARCHAR(50) PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    sexe VARCHAR(10),
    date_naissance VARCHAR(50),
    email VARCHAR(255) UNIQUE NOT NULL,
    telephone VARCHAR(50),
    filiere VARCHAR(255),
    niveau VARCHAR(50),
    annee VARCHAR(20),
    statut VARCHAR(50) NOT NULL
);

CREATE TABLE inscriptions (
    id_inscription VARCHAR(50) PRIMARY KEY,
    matricule VARCHAR(50),
    filiere VARCHAR(255),
    niveau VARCHAR(50),
    annee VARCHAR(20),
    statut VARCHAR(50) NOT NULL
);

CREATE TABLE notes (
    id_note VARCHAR(50) PRIMARY KEY,
    matricule VARCHAR(50),
    id_matiere VARCHAR(50),
    controle_continu DOUBLE PRECISION,
    examen DOUBLE PRECISION
);

CREATE TABLE absences (
    id_absence VARCHAR(50) PRIMARY KEY,
    matricule VARCHAR(50),
    id_matiere VARCHAR(50),
    date_absence VARCHAR(50),
    heures INTEGER,
    justifiee BOOLEAN
);

CREATE TABLE paiements (
    id_paiement VARCHAR(50) PRIMARY KEY,
    matricule VARCHAR(50),
    montant_total DOUBLE PRECISION,
    montant_paye DOUBLE PRECISION,
    date_paiement VARCHAR(50),
    mode VARCHAR(50)
);

CREATE TABLE utilisateurs (
    id_utilisateur VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    id_profil VARCHAR(50) NOT NULL,
    actif BOOLEAN DEFAULT TRUE
);

CREATE TABLE salles (
    id_salle VARCHAR(50) PRIMARY KEY,
    nom_salle VARCHAR(255) NOT NULL,
    capacite INTEGER,
    type_salle VARCHAR(100)
);

CREATE TABLE seances_cours (
    id_seance VARCHAR(50) PRIMARY KEY,
    id_matiere VARCHAR(50),
    id_enseignant VARCHAR(50),
    id_salle VARCHAR(50),
    jour VARCHAR(20),
    heure_debut VARCHAR(20),
    heure_fin VARCHAR(20),
    filiere VARCHAR(255),
    niveau VARCHAR(50)
);

-- !Downs

DROP TABLE IF EXISTS seances_cours CASCADE;
DROP TABLE IF EXISTS salles CASCADE;
DROP TABLE IF EXISTS utilisateurs CASCADE;
DROP TABLE IF EXISTS paiements CASCADE;
DROP TABLE IF EXISTS absences CASCADE;
DROP TABLE IF EXISTS notes CASCADE;
DROP TABLE IF EXISTS inscriptions CASCADE;
DROP TABLE IF EXISTS etudiants CASCADE;
DROP TABLE IF EXISTS matieres CASCADE;
DROP TABLE IF EXISTS enseignants CASCADE;
DROP TABLE IF EXISTS ues CASCADE;
DROP TABLE IF EXISTS semestres CASCADE;
DROP TABLE IF EXISTS niveaux CASCADE;
DROP TABLE IF EXISTS formations CASCADE;
DROP TABLE IF EXISTS filieres CASCADE;
