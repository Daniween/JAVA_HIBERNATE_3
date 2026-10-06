-- Script de création de la base "clinique_db" pour l'exemple ManyToOne (BTS SIO SLAM)
-- À exécuter AVANT de lancer l'application (dans un client MySQL : MySQL Workbench,
-- phpMyAdmin, DBeaver, ou en ligne de commande : mysql -u root -p < clinique_db.sql)

CREATE DATABASE IF NOT EXISTS clinique_db CHARACTER SET utf8mb4;
USE clinique_db;

-- Côté "un" de la relation
CREATE TABLE medecin (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(50) NOT NULL,
    prenom      VARCHAR(50) NOT NULL,
    specialite  VARCHAR(60) NOT NULL
);

-- Côté "plusieurs" de la relation : medecin_id est la clé étrangère
-- qui correspond à l'annotation @ManyToOne + @JoinColumn(name = "medecin_id")
CREATE TABLE patient (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(50) NOT NULL,
    prenom      VARCHAR(50) NOT NULL,
    telephone   VARCHAR(20),
    medecin_id  INT NOT NULL,
    CONSTRAINT fk_patient_medecin FOREIGN KEY (medecin_id) REFERENCES medecin(id)
);

-- Données d'exemple : 3 médecins, 6 patients répartis entre eux
INSERT INTO medecin (nom, prenom, specialite) VALUES
    ('Lefevre', 'Claire', 'Généraliste'),
    ('Morel', 'Julien', 'Cardiologue'),
    ('Girard', 'Sophie', 'Pédiatre');

INSERT INTO patient (nom, prenom, telephone, medecin_id) VALUES
    ('Martin', 'Lucas', '0601020304', 1),
    ('Bernard', 'Emma', '0602030405', 1),
    ('Dubois', 'Nathan', '0603040506', 2),
    ('Thomas', 'Chloe', '0604050607', 2),
    ('Robert', 'Lea', '0605060708', 3),
    ('Petit', 'Hugo', '0606070809', 3);

-- Un utilisateur applicatif dédié (à adapter/retirer selon votre configuration MySQL) :
-- CREATE USER 'user'@'localhost' IDENTIFIED BY 'userpassword';
-- GRANT ALL PRIVILEGES ON clinique_db.* TO 'user'@'localhost';
-- FLUSH PRIVILEGES;
