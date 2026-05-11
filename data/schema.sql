-- ─────────────────────────────────────────────
-- Schéma PostgreSQL pour Gestion Universitaire
-- ─────────────────────────────────────────────

-- Créer la base de données (à exécuter en psql)
-- CREATE DATABASE universite;

-- Connexion à la base
-- \c universite;

-- ─── Table des utilisateurs ─────────────────
CREATE TABLE IF NOT EXISTS utilisateurs (
    id_utilisateur VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'ENSEIGNANT', 'ETUDIANT')),
    id_profil VARCHAR(50) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour accélérer les recherches
CREATE INDEX IF NOT EXISTS idx_utilisateurs_email ON utilisateurs(email);
CREATE INDEX IF NOT EXISTS idx_utilisateurs_role ON utilisateurs(role);
CREATE INDEX IF NOT EXISTS idx_utilisateurs_id_profil ON utilisateurs(id_profil);

-- ─── Données initiales ───────────────────────
-- Mot de passe: "password" (hashé avec bcrypt)
INSERT INTO utilisateurs (id_utilisateur, email, password_hash, role, id_profil, actif) VALUES
('ADM_USR_ADMIN', 'admin@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ADMIN', 'ADMIN', true),
('ENS_USR_ENS001', 'prof.dupont@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ENSEIGNANT', 'ENS001', true),
('ENS_USR_ENS002', 'prof.martin@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ENSEIGNANT', 'ENS002', true),
('ETU_USR_ETU001', 'etudiant001@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ETUDIANT', 'ETU001', true),
('ETU_USR_ETU002', 'etudiant002@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ETUDIANT', 'ETU002', true),
('ETU_USR_ETU003', 'etudiant003@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ETUDIANT', 'ETU003', true),
('ETU_USR_ETU004', 'etudiant004@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ETUDIANT', 'ETU004', true),
('ETU_USR_ETU005', 'etudiant005@univ.fr', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G', 'ETUDIANT', 'ETU005', true)
ON CONFLICT (id_utilisateur) DO NOTHING;
