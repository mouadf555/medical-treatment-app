package com.medical.dao;

import java.sql.*;

public class Database {
    private static final String URL      = "jdbc:mysql://localhost:3306/medical_db?useSSL=false&serverTimezone=UTC";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection connection = null;

    // ── Connexion singleton ────────────────────────────────────────────────────
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✔ Connexion MySQL établie.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL introuvable : " + e.getMessage());
            }
        }
        return connection;
    }

    // ── Initialisation du schéma ───────────────────────────────────────────────
    public static void initialiserBase() {
        String createPatients = """
            CREATE TABLE IF NOT EXISTS patients (
                id               INT AUTO_INCREMENT PRIMARY KEY,
                nom              VARCHAR(100) NOT NULL,
                prenom           VARCHAR(100) NOT NULL,
                date_naissance   DATE,
                sexe             VARCHAR(10),
                telephone        VARCHAR(20),
                email            VARCHAR(150),
                sous_surveillance TINYINT(1) DEFAULT 0,
                observations     TEXT
            )""";

        String createTraitements = """
            CREATE TABLE IF NOT EXISTS traitements (
                id                INT AUTO_INCREMENT PRIMARY KEY,
                patient_id        INT NOT NULL,
                nom               VARCHAR(150) NOT NULL,
                type              VARCHAR(80),
                posologie         VARCHAR(200),
                prises_par_jour   INT DEFAULT 1,
                date_debut        DATE,
                date_fin          DATE,
                duree_estimee     INT DEFAULT 7,
                actif             TINYINT(1) DEFAULT 1,
                effets_secondaires TEXT,
                couleur           VARCHAR(20) DEFAULT '#2196F3',
                FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
            )""";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPatients);
            stmt.execute(createTraitements);
            System.out.println("✔ Tables vérifiées / créées.");
        } catch (SQLException e) {
            System.err.println("Erreur init BD : " + e.getMessage());
        }
    }

    public static void fermer() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion MySQL fermée.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur fermeture : " + e.getMessage());
        }
    }
}