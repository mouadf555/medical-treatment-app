package com.medical.dao;

import com.medical.model.Traitement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TraitementDAO {

    // ── CREATE ─────────────────────────────────────────────────────────────────
    public boolean ajouter(Traitement t) {
        String sql = """
            INSERT INTO traitements
            (patient_id, nom, type, posologie, prises_par_jour, date_debut, date_fin,
             duree_estimee, actif, effets_secondaires, couleur)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, t);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) t.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajout traitement : " + e.getMessage());
        }
        return false;
    }

    // ── READ BY PATIENT ────────────────────────────────────────────────────────
    public List<Traitement> getParPatient(int patientId) {
        List<Traitement> liste = new ArrayList<>();
        String sql = "SELECT * FROM traitements WHERE patient_id=? ORDER BY date_debut DESC";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapper(rs));
        } catch (SQLException e) {
            System.err.println("Erreur lecture traitements : " + e.getMessage());
        }
        return liste;
    }

    // ── READ ALL ───────────────────────────────────────────────────────────────
    public List<Traitement> getTous() {
        List<Traitement> liste = new ArrayList<>();
        String sql = "SELECT * FROM traitements ORDER BY date_debut DESC";
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapper(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getTous traitements : " + e.getMessage());
        }
        return liste;
    }

    // ── FILTER BY TYPE ─────────────────────────────────────────────────────────
    public List<Traitement> getParType(String type) {
        List<Traitement> liste = new ArrayList<>();
        String sql = "SELECT * FROM traitements WHERE type=? ORDER BY nom";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapper(rs));
        } catch (SQLException e) {
            System.err.println("Erreur filtre type : " + e.getMessage());
        }
        return liste;
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────
    public boolean modifier(Traitement t) {
        String sql = """
            UPDATE traitements SET patient_id=?, nom=?, type=?, posologie=?,
            prises_par_jour=?, date_debut=?, date_fin=?, duree_estimee=?,
            actif=?, effets_secondaires=?, couleur=?
            WHERE id=?""";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            remplir(ps, t);
            ps.setInt(12, t.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification traitement : " + e.getMessage());
        }
        return false;
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────
    public boolean supprimer(int id) {
        String sql = "DELETE FROM traitements WHERE id=?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression traitement : " + e.getMessage());
        }
        return false;
    }

    // ── STATS ─────────────────────────────────────────────────────────────────
    public int compterActifs() {
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM traitements WHERE actif=1")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    public List<Object[]> repartitionParType() {
        List<Object[]> stats = new ArrayList<>();
        String sql = "SELECT type, COUNT(*) AS nb FROM traitements GROUP BY type";
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                stats.add(new Object[]{rs.getString("type"), rs.getInt("nb")});
        } catch (SQLException e) { /* ignore */ }
        return stats;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void remplir(PreparedStatement ps, Traitement t) throws SQLException {
        ps.setInt(1, t.getPatientId());
        ps.setString(2, t.getNom());
        ps.setString(3, t.getType());
        ps.setString(4, t.getPosologie());
        ps.setInt(5, t.getPrisesParJour());
        ps.setDate(6, t.getDateDebut() != null ? Date.valueOf(t.getDateDebut()) : null);
        ps.setDate(7, t.getDateFin() != null ? Date.valueOf(t.getDateFin()) : null);
        ps.setInt(8, t.getDureeEstimee());
        ps.setBoolean(9, t.isActif());
        ps.setString(10, t.getEffetsSecondaires());
        ps.setString(11, t.getCouleur() != null ? t.getCouleur() : "#2196F3");
    }

    private Traitement mapper(ResultSet rs) throws SQLException {
        Traitement t = new Traitement();
        t.setId(rs.getInt("id"));
        t.setPatientId(rs.getInt("patient_id"));
        t.setNom(rs.getString("nom"));
        t.setType(rs.getString("type"));
        t.setPosologie(rs.getString("posologie"));
        t.setPrisesParJour(rs.getInt("prises_par_jour"));
        Date dd = rs.getDate("date_debut");
        if (dd != null) t.setDateDebut(dd.toLocalDate());
        Date df = rs.getDate("date_fin");
        if (df != null) t.setDateFin(df.toLocalDate());
        t.setDureeEstimee(rs.getInt("duree_estimee"));
        t.setActif(rs.getBoolean("actif"));
        t.setEffetsSecondaires(rs.getString("effets_secondaires"));
        t.setCouleur(rs.getString("couleur"));
        return t;
    }
}