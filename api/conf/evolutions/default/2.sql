-- Ajout des champs id_semestre et nb_semaines à seances_cours pour les quotas

-- !Ups
TRUNCATE TABLE seances_cours;
ALTER TABLE seances_cours ADD COLUMN id_semestre VARCHAR(50) NOT NULL DEFAULT 'S1';
ALTER TABLE seances_cours ADD COLUMN nb_semaines INTEGER NOT NULL DEFAULT 1;

-- !Downs
ALTER TABLE seances_cours DROP COLUMN id_semestre;
ALTER TABLE seances_cours DROP COLUMN nb_semaines;
