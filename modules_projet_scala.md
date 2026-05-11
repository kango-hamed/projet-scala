# Plateforme Intelligente de Gestion Universitaire — Description des Modules

---

## Module 1 : Gestion des étudiants

### Description
L'application doit permettre de :
- Créer un étudiant
- Modifier ses informations
- Rechercher un étudiant par matricule
- Afficher la liste des étudiants
- Filtrer les étudiants par niveau, filière ou statut
- Gérer les étudiants actifs, suspendus ou diplômés

### Données gérées
Chaque étudiant doit avoir : matricule, nom, prénom, sexe, date de naissance, email, téléphone, filière, niveau, année académique, statut.

### Résultats attendus
- Affichage de tous les étudiants
- Recherche d'un étudiant par matricule
- Filtrage des étudiants par filière
- Filtrage des étudiants par niveau
- Comptage des étudiants actifs
- Détection des étudiants suspendus

---

## Module 2 : Gestion des enseignants

### Description
L'application doit permettre de :
- Enregistrer un enseignant
- Affecter un enseignant à un cours
- Rechercher un enseignant
- Afficher les enseignants par département
- Afficher les cours assurés par un enseignant

### Données gérées
Chaque enseignant doit avoir : identifiant, nom, prénom, grade, spécialité, département, email, téléphone.

### Résultats attendus
- Liste des enseignants par département
- Affichage des cours assurés par un enseignant
- Enseignants ayant le plus grand volume horaire (indicateur tableau de bord)

---

## Module 3 : Gestion des formations

### Description
L'application doit gérer :
- Les filières
- Les niveaux
- Les semestres
- Les unités d'enseignement (UE)
- Les matières
- Les coefficients
- Les volumes horaires

### Structure hiérarchique
```
Formation
  └── Niveau
        └── Semestre
              └── Unité d'Enseignement (UE)
                    └── Matière
```

### Résultats attendus
- Navigation dans la hiérarchie des formations
- Affichage des matières avec leurs coefficients et volumes horaires
- Identification de la matière la plus difficile (indicateur décisionnel)

---

## Module 4 : Gestion des inscriptions

### Description
L'application doit permettre :
- D'inscrire un étudiant à une année académique
- D'associer l'étudiant à une filière et un niveau
- De vérifier qu'un étudiant n'est pas inscrit deux fois la même année
- De gérer le statut de l'inscription : **Validée**, **En attente**, **Annulée**

### Résultats attendus
- Enregistrement d'une inscription avec contrôle des doublons
- Liste des inscriptions par statut
- Nombre d'étudiants inscrits par filière et par niveau

---

## Module 5 : Gestion des notes

### Description
L'application doit permettre :
- D'enregistrer les notes de contrôle continu
- D'enregistrer les notes d'examen
- De calculer la moyenne d'une matière
- De calculer la moyenne d'une UE
- De calculer la moyenne générale
- D'attribuer une décision : **Admis**, **Ajourné**, **Redoublement**
- De détecter les notes manquantes
- De détecter les notes invalides

### Formule de calcul
```
Moyenne matière = 40% × Contrôle Continu + 60% × Examen
```

### Résultats attendus
- Calcul de la moyenne par matière, par UE et générale
- Classement des étudiants
- Détection des étudiants ajournés
- Détection des notes invalides ou manquantes
- Relevé de notes par étudiant
- Top 5 des meilleurs étudiants (indicateur tableau de bord)
- Taux de réussite par filière et global

---

## Module 6 : Gestion des absences

### Description
L'application doit permettre :
- D'enregistrer une absence
- De préciser si elle est justifiée ou non
- De calculer le nombre total d'absences par étudiant
- De signaler les étudiants dépassant un seuil (> 10 heures)
- De produire un rapport des absences par matière

### Résultats attendus
- Total d'heures d'absence par étudiant
- Liste des absences non justifiées
- Détection des étudiants ayant plus de 10 heures d'absence
- Calcul du taux d'absentéisme par filière
- Taux d'absentéisme global et par matière (indicateurs tableau de bord)

---

## Module 7 : Gestion des emplois du temps

### Description
L'application doit permettre :
- De créer des séances de cours
- D'affecter une salle
- D'affecter un enseignant
- D'affecter une matière
- De vérifier les conflits d'horaires
- De consulter l'emploi du temps par classe, enseignant ou salle

### Résultats attendus
- Affichage de l'emploi du temps par filière/niveau
- Affichage de l'emploi du temps par enseignant
- Affichage de l'emploi du temps par salle
- Détection des conflits d'horaires

---

## Module 8 : Gestion des paiements

### Description
L'application doit permettre :
- D'enregistrer les frais d'inscription
- D'enregistrer les paiements
- De calculer le reste à payer
- De détecter les étudiants en retard de paiement
- De générer une synthèse financière par filière

### Résultats attendus
- Calcul du reste à payer par étudiant
- Liste des étudiants ayant une dette
- Montant total encaissé
- Montant total attendu
- Montant restant à recouvrer
- Taux de recouvrement global (indicateur tableau de bord)

---

## Module 9 : Tableau de bord académique

### Description
Le système doit produire un ensemble d'indicateurs décisionnels permettant le pilotage de l'université.

### Résultats attendus (indicateurs produits)

| Indicateur | Description |
|---|---|
| Nombre total d'étudiants | Comptage global |
| Nombre d'étudiants par filière | Répartition par filière |
| Nombre d'étudiants par niveau | Répartition par niveau |
| Moyenne générale par filière | Performance académique |
| Top 5 des meilleurs étudiants | Classement |
| Liste des étudiants à risque | Académique et financier |
| Taux d'absentéisme global | Suivi des présences |
| Taux d'absentéisme par matière | Détail par cours |
| Montant total attendu | Suivi financier |
| Montant total encaissé | Suivi financier |
| Montant restant à recouvrer | Suivi financier |
| Taux de réussite par filière | Performance académique |
| Taux de réussite global | Performance académique |
| Matière la plus difficile | Analyse pédagogique |
| Filière avec le meilleur taux de réussite | Analyse comparative |
| Enseignants avec le plus grand volume horaire | Charge d'enseignement |

---

## Module 10 : Module Big Data

### Description
Les données historiques doivent être stockées dans une architecture Big Data. Le système devra analyser des volumes importants de données sur plusieurs années.

### Périmètre d'analyse
- Plusieurs années académiques
- Plusieurs milliers d'étudiants
- Performances par promotion
- Tendances des absences
- Paiements par période
- Résultats par matière

### Résultats attendus (avec Spark Scala)
- Chargement des fichiers CSV
- Création de DataFrames
- Nettoyage des données
- Détection des valeurs manquantes
- Calcul des indicateurs décisionnels
- Sauvegarde des résultats en CSV ou Parquet

### Technologies utilisées
- Apache Spark (Scala)
- HDFS ou fichiers CSV volumineux
- Hive ou PostgreSQL
- Format de sortie : CSV / Parquet

---

*Document généré à partir du cahier des charges — Projet Scala Gestion Universitaire*
