# Cahier des Charges Frontend
## Plateforme Intelligente de Gestion Universitaire

---

## 1. Présentation du projet

### 1.1 Contexte
Développement du frontend d'une plateforme web de gestion universitaire connectée à un backend Scala. L'interface doit permettre à trois types d'utilisateurs (Administrateur, Enseignant, Étudiant) d'accéder à leurs fonctionnalités respectives depuis un navigateur web.

### 1.2 Objectif
Fournir une interface web moderne, intuitive et sécurisée permettant la gestion académique, administrative et financière d'une université, avec des tableaux de bord analytiques adaptés à chaque profil.

### 1.3 Technologies imposées
- **Framework** : React (avec hooks)
- **Gestionnaire de paquets** : npm ou yarn
- **Routing** : React Router v6
- **Gestion d'état** : Context API ou Redux Toolkit
- **Appels API** : Axios ou Fetch
- **Graphiques** : Recharts ou Chart.js
- **UI Components** : Material UI, Ant Design ou Tailwind CSS (au choix)
- **Authentification** : JWT (JSON Web Token)

---

## 2. Utilisateurs et rôles

### 2.1 Profils utilisateurs

| Profil | Description | Niveau d'accès |
|---|---|---|
| **Administrateur** | Personnel de l'administration universitaire | Accès total à tous les modules |
| **Enseignant** | Corps enseignant de l'université | Accès à ses cours, notes, absences, emploi du temps |
| **Étudiant** | Étudiant inscrit | Accès en lecture à ses propres données |

### 2.2 Matrice des permissions

| Module | Admin | Enseignant | Étudiant |
|---|---|---|---|
| Gestion des étudiants | CRUD | Lecture | Lecture (soi-même) |
| Gestion des enseignants | CRUD | Lecture (soi-même) | — |
| Gestion des formations | CRUD | Lecture | Lecture |
| Gestion des inscriptions | CRUD | — | Lecture (soi-même) |
| Gestion des notes | CRUD | CRUD (ses matières) | Lecture (soi-même) |
| Gestion des absences | CRUD | CRUD (ses matières) | Lecture (soi-même) |
| Emplois du temps | CRUD | Lecture | Lecture |
| Gestion des paiements | CRUD | — | Lecture (soi-même) |
| Tableau de bord | Complet | Partiel | Personnel |
| Module Big Data | Accès total | — | — |

---

## 3. Authentification et sécurité

### 3.1 Page de connexion
- Formulaire avec champ **email** et **mot de passe**
- Sélection ou détection automatique du **rôle** (Admin / Enseignant / Étudiant)
- Gestion des erreurs de connexion (identifiants incorrects, compte suspendu)
- Option "Mot de passe oublié"
- Redirection automatique vers le tableau de bord du rôle concerné après connexion

### 3.2 Gestion des sessions
- Stockage du token JWT en mémoire (ou `httpOnly` cookie)
- Expiration automatique de session avec déconnexion
- Rafraîchissement de token (refresh token)
- Protection des routes selon le rôle (Route Guards)

### 3.3 Déconnexion
- Bouton de déconnexion accessible depuis toutes les pages
- Suppression du token et redirection vers la page de connexion

---

## 4. Structure de l'application

### 4.1 Layout général
```
App
├── AuthLayout (pages publiques)
│   └── LoginPage
│
└── MainLayout (pages privées)
    ├── Sidebar (navigation latérale)
    ├── Header (barre supérieure)
    ├── Breadcrumb
    └── PageContent
```

### 4.2 Navigation latérale (Sidebar)
La sidebar est **dynamique selon le rôle** connecté.

**Admin :**
- Tableau de bord
- Étudiants
- Enseignants
- Formations & Filières
- Inscriptions
- Notes
- Absences
- Emplois du temps
- Paiements
- Big Data & Rapports
- Paramètres

**Enseignant :**
- Mon tableau de bord
- Mes cours
- Saisie des notes
- Gestion des absences
- Mon emploi du temps
- Mon profil

**Étudiant :**
- Mon tableau de bord
- Mon profil
- Mes inscriptions
- Mes notes & relevé
- Mes absences
- Mon emploi du temps
- Mes paiements

---

## 5. Pages et fonctionnalités par module

### 5.1 Module Étudiants (Admin)

**Pages :**
- `/admin/etudiants` — Liste paginée des étudiants avec filtres (filière, niveau, statut)
- `/admin/etudiants/nouveau` — Formulaire de création
- `/admin/etudiants/:id` — Fiche détaillée
- `/admin/etudiants/:id/modifier` — Formulaire de modification

**Fonctionnalités UI :**
- Tableau avec tri sur toutes les colonnes
- Barre de recherche par matricule, nom ou prénom
- Filtres multi-critères : filière, niveau, statut (Actif / Suspendu / Diplômé)
- Badge coloré pour le statut de l'étudiant
- Export de la liste en CSV
- Pagination (20 éléments par page)
- Modal de confirmation pour les actions critiques (suppression, suspension)

---

### 5.2 Module Enseignants (Admin)

**Pages :**
- `/admin/enseignants` — Liste des enseignants
- `/admin/enseignants/nouveau` — Formulaire de création
- `/admin/enseignants/:id` — Fiche avec ses cours assignés
- `/admin/enseignants/:id/modifier` — Formulaire de modification

**Fonctionnalités UI :**
- Filtrage par département et grade
- Affichage de la charge horaire par enseignant
- Association visuelle enseignant ↔ matières

---

### 5.3 Module Formations & Filières (Admin)

**Pages :**
- `/admin/formations` — Vue arborescente : Filière > Niveau > Semestre > UE > Matière
- `/admin/formations/filieres` — Gestion des filières
- `/admin/formations/matieres` — Gestion des matières avec coefficients

**Fonctionnalités UI :**
- Composant arborescent (Tree View) pour la hiérarchie des formations
- Affichage des coefficients et volumes horaires
- Assignation d'un enseignant à une matière

---

### 5.4 Module Inscriptions (Admin)

**Pages :**
- `/admin/inscriptions` — Liste des inscriptions avec statut
- `/admin/inscriptions/nouvelle` — Formulaire d'inscription

**Fonctionnalités UI :**
- Statut visuel : Validée (vert), En attente (orange), Annulée (rouge)
- Vérification en temps réel des doublons d'inscription
- Filtres par année académique, filière, statut

---

### 5.5 Module Notes

**Pages Admin/Enseignant :**
- `/notes/saisie` — Grille de saisie des notes par matière et étudiant
- `/notes/validation` — Validation des notes saisies

**Page Étudiant :**
- `/etudiant/notes` — Relevé de notes personnel

**Fonctionnalités UI :**
- Tableau de saisie type spreadsheet (modifiable inline)
- Calcul automatique et affichage de la moyenne en temps réel
  - Formule : `Moyenne = 40% CC + 60% Examen`
- Indicateur visuel de la décision : Admis (vert) / Ajourné (rouge) / Redoublement (orange)
- Alerte pour les notes manquantes ou hors plage [0–20]
- Relevé de notes téléchargeable en PDF

---

### 5.6 Module Absences

**Pages Admin/Enseignant :**
- `/absences/saisie` — Enregistrement des absences par séance
- `/absences/liste` — Liste complète avec filtres

**Page Étudiant :**
- `/etudiant/absences` — Historique de ses absences

**Fonctionnalités UI :**
- Badge Justifiée / Non justifiée
- Compteur d'heures cumulées par étudiant
- Alerte visuelle (rouge) dès dépassement du seuil de 10 heures
- Graphique en barres : absences par matière

---

### 5.7 Module Emplois du temps

**Pages :**
- `/emplois-du-temps` — Vue calendrier hebdomadaire

**Fonctionnalités UI :**
- Affichage en calendrier (vue semaine)
- Filtres : par filière/niveau, par enseignant, par salle
- Détection et affichage visuel des conflits d'horaires
- Affichage responsive des séances (couleur par matière)

---

### 5.8 Module Paiements (Admin)

**Pages :**
- `/admin/paiements` — Liste des paiements
- `/admin/paiements/:matricule` — Détail du compte étudiant

**Page Étudiant :**
- `/etudiant/paiements` — Suivi personnel des paiements

**Fonctionnalités UI :**
- Barre de progression du paiement (montant payé / total dû)
- Badge : Soldé (vert) / Partiel (orange) / Impayé (rouge)
- Indicateur global du taux de recouvrement
- Tableau des étudiants débiteurs avec montant restant dû
- Synthèse financière par filière (graphique)

---

### 5.9 Tableau de bord

#### Admin — `/admin/dashboard`
Indicateurs affichés sous forme de **cartes KPI**, **graphiques** et **tableaux** :

| Indicateur | Visualisation |
|---|---|
| Nombre total d'étudiants | Carte KPI |
| Étudiants par filière | Graphique en secteurs (Pie chart) |
| Étudiants par niveau | Graphique en barres |
| Moyenne générale par filière | Graphique en barres horizontales |
| Top 5 meilleurs étudiants | Tableau classement |
| Étudiants à risque | Liste avec alertes |
| Taux d'absentéisme global | Jauge (gauge chart) |
| Taux d'absentéisme par matière | Graphique en barres |
| Suivi financier (encaissé / attendu / restant) | Graphique en barres groupées |
| Taux de réussite global et par filière | Graphique en secteurs |
| Matière la plus difficile | Carte avec badge |
| Volume horaire par enseignant | Graphique en barres |

#### Enseignant — `/enseignant/dashboard`
- Nombre de cours cette semaine
- Prochain cours (heure, salle, filière)
- Nombre d'absences enregistrées ce mois
- Moyenne de ses matières
- Alertes : notes manquantes à saisir

#### Étudiant — `/etudiant/dashboard`
- Moyenne générale et rang dans la promotion
- Nombre d'heures d'absence cumulées
- Prochains cours (emploi du temps)
- Solde des paiements (reste à payer)
- Dernières notes enregistrées

---

### 5.10 Module Big Data & Rapports (Admin uniquement)

**Page :** `/admin/bigdata`

**Fonctionnalités UI :**
- Interface de lancement d'analyses Spark
- Visualisation des tendances sur plusieurs années académiques
- Graphiques d'évolution des performances par promotion
- Export des résultats en CSV ou Parquet
- Affichage des données nettoyées et des valeurs manquantes détectées

---

## 6. Composants transversaux

### 6.1 Composants réutilisables à développer

| Composant | Description |
|---|---|
| `DataTable` | Tableau générique avec tri, pagination, filtre, export CSV |
| `KPICard` | Carte d'indicateur avec icône, valeur, variation |
| `StatusBadge` | Badge coloré selon le statut (Actif, Suspendu, Validée...) |
| `SearchBar` | Barre de recherche avec debounce |
| `FilterPanel` | Panneau de filtres multi-critères |
| `ConfirmModal` | Modal de confirmation pour actions critiques |
| `NotificationToast` | Notification temporaire (succès, erreur, avertissement) |
| `LoadingSpinner` | Indicateur de chargement |
| `EmptyState` | Affichage quand aucune donnée disponible |
| `ProgressBar` | Barre de progression (paiements, taux) |
| `WeekCalendar` | Calendrier hebdomadaire pour l'emploi du temps |
| `GradeInput` | Champ de saisie de note avec validation instantanée |

### 6.2 Gestion des états globaux (Context / Redux)

| State | Contenu |
|---|---|
| `AuthContext` | Utilisateur connecté, rôle, token JWT |
| `NotificationContext` | File de notifications globales |
| `ThemeContext` | Thème clair / sombre |

---

## 7. Gestion des erreurs et cas limites

- **404** : Page non trouvée avec redirection vers le tableau de bord
- **403** : Accès refusé si le rôle ne permet pas l'accès à la route
- **500** : Erreur serveur avec message explicite
- **Réseau** : Gestion des timeouts et erreurs de connexion API
- **Formulaires** : Validation côté client avant envoi (champs requis, formats, plages de valeurs)
- **Session expirée** : Redirection automatique vers la page de connexion

---

## 8. Exigences non fonctionnelles

### 8.1 Performance
- Chargement initial de l'application < 3 secondes
- Pagination côté serveur pour les listes volumineuses
- Lazy loading des composants par route (code splitting)
- Mise en cache des données stables (filières, matières)

### 8.2 Accessibilité
- Respect des normes WCAG 2.1 niveau AA
- Navigation clavier complète
- Contrastes suffisants pour la lisibilité
- Attributs `aria-label` sur les éléments interactifs

### 8.3 Responsive
- L'application est conçue pour **desktop en priorité** (>= 1024px)
- Adaptation minimale pour tablette (>= 768px)
- Mobile non requis dans cette version

### 8.4 Internationalisation
- Langue principale : **Français**
- Dates au format `DD/MM/YYYY`
- Montants au format `1 000 000 FCFA`

---

## 9. Organisation des fichiers React

```
src/
├── assets/                  # Images, icônes, polices
├── components/
│   ├── common/              # Composants réutilisables (DataTable, KPICard, ...)
│   ├── layout/              # Sidebar, Header, Footer, MainLayout
│   └── charts/              # Graphiques Recharts/Chart.js
├── context/                 # AuthContext, NotificationContext
├── hooks/                   # Hooks personnalisés (useAuth, useFetch, ...)
├── pages/
│   ├── auth/                # LoginPage
│   ├── admin/               # Dashboard, Étudiants, Enseignants, ...
│   ├── enseignant/          # Dashboard enseignant, notes, absences
│   └── etudiant/            # Dashboard étudiant, notes, paiements
├── routes/                  # Définition des routes protégées par rôle
├── services/                # Appels API (etudiantService, noteService, ...)
├── store/                   # Redux store ou Context providers
├── utils/                   # Fonctions utilitaires (formatDate, calcMoyenne, ...)
└── App.jsx
```

---

## 10. Livrables frontend attendus

- [ ] Code source React complet et organisé
- [ ] Fichier `README.md` avec instructions d'installation et de lancement
- [ ] Variables d'environnement documentées (`.env.example`)
- [ ] Maquettes ou captures d'écran des pages principales
- [ ] Documentation des composants réutilisables
- [ ] Jeu de données mock pour tester sans backend (JSON ou MSW)

---

*Cahier des charges Frontend — Plateforme de Gestion Universitaire — Version 1.0*
