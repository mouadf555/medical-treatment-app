package com.medical.dao;

import com.medical.model.Patient;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    // ── CREATE ─────────────────────────────────────────────────────────────────
    public boolean ajouter(Patient p) {
        String sql = """
            INSERT INTO patients
            (nom, prenom, date_naissance, sexe, telephone, email, sous_surveillance, observations)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setDate(3, p.getDateNaissance() != null ? Date.valueOf(p.getDateNaissance()) : null);
            ps.setString(4, p.getSexe());
            ps.setString(5, p.getTelephone());
            ps.setString(6, p.getEmail());
            ps.setBoolean(7, p.isSousSurveillance());
            ps.setString(8, p.getObservations());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) p.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajout patient : " + e.getMessage());
        }
        return false;
    }

    // ── READ ALL ───────────────────────────────────────────────────────────────
    public List<Patient> getTous() {
        List<Patient> liste = new ArrayList<>();
        String sql = "SELECT * FROM patients ORDER BY nom, prenom";
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapper(rs));
        } catch (SQLException e) {
            System.err.println("Erreur lecture patients : " + e.getMessage());
        }
        return liste;
    }

    // ── READ BY ID ─────────────────────────────────────────────────────────────
    public Patient getParId(int id) {
        String sql = "SELECT * FROM patients WHERE id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapper(rs);
        } catch (SQLException e) {
            System.err.println("Erreur getParId patient : " + e.getMessage());
        }
        return null;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────
    public List<Patient> rechercher(String motCle) {
        List<Patient> liste = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE CONCAT(nom,' ',prenom) LIKE ? ORDER BY nom";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + motCle + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapper(rs));
        } catch (SQLException e) {
            System.err.println("Erreur recherche patients : " + e.getMessage());
        }
        return liste;
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────
    public boolean modifier(Patient p) {
        String sql = """
            UPDATE patients SET nom=?, prenom=?, date_naissance=?, sexe=?,
            telephone=?, email=?, sous_surveillance=?, observations=?
            WHERE id=?""";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setDate(3, p.getDateNaissance() != null ? Date.valueOf(p.getDateNaissance()) : null);
            ps.setString(4, p.getSexe());
            ps.setString(5, p.getTelephone());
            ps.setString(6, p.getEmail());
            ps.setBoolean(7, p.isSousSurveillance());
            ps.setString(8, p.getObservations());
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification patient : " + e.getMessage());
        }
        return false;
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────
    public boolean supprimer(int id) {
        String sql = "DELETE FROM patients WHERE id=?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression patient : " + e.getMessage());
        }
        return false;
    }

    // ── COUNT ──────────────────────────────────────────────────────────────────
    public int compter() {
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM patients")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    // ── MAPPER ─────────────────────────────────────────────────────────────────
    private Patient mapper(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        Date d = rs.getDate("date_naissance");
        if (d != null) p.setDateNaissance(d.toLocalDate());
        p.setSexe(rs.getString("sexe"));
        p.setTelephone(rs.getString("telephone"));
        p.setEmail(rs.getString("email"));
        p.setSousSurveillance(rs.getBoolean("sous_surveillance"));
        p.setObservations(rs.getString("observations"));
        return p;
    }
}