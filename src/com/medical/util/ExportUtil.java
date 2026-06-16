package com.medical.util;

import com.medical.model.Patient;
import com.medical.model.Traitement;

import java.io.*;
import java.util.List;

public class ExportUtil {

    // ── Export patients CSV ────────────────────────────────────────────────────
    public static void exporterPatientsCSV(List<Patient> patients, File fichier) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fichier))) {
            pw.println("ID,Nom,Prénom,Date Naissance,Sexe,Téléphone,Email,Sous Surveillance,Observations");
            for (Patient p : patients) {
                pw.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        p.getId(),
                        csvEchapper(p.getNom()),
                        csvEchapper(p.getPrenom()),
                        p.getDateNaissance() != null ? p.getDateNaissance().toString() : "",
                        csvEchapper(p.getSexe()),
                        csvEchapper(p.getTelephone()),
                        csvEchapper(p.getEmail()),
                        p.isSousSurveillance() ? "Oui" : "Non",
                        csvEchapper(p.getObservations())
                );
            }
        }
    }

    // ── Export traitements CSV ─────────────────────────────────────────────────
    public static void exporterTraitementsCSV(List<Traitement> traitements, File fichier) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fichier))) {
            pw.println("ID,Patient ID,Nom,Type,Posologie,Prises/Jour,Date Début,Date Fin,Durée,Actif,Effets Secondaires");
            for (Traitement t : traitements) {
                pw.printf("%d,%d,%s,%s,%s,%d,%s,%s,%d,%s,%s%n",
                        t.getId(),
                        t.getPatientId(),
                        csvEchapper(t.getNom()),
                        csvEchapper(t.getType()),
                        csvEchapper(t.getPosologie()),
                        t.getPrisesParJour(),
                        t.getDateDebut() != null ? t.getDateDebut().toString() : "",
                        t.getDateFin() != null ? t.getDateFin().toString() : "",
                        t.getDureeEstimee(),
                        t.isActif() ? "Oui" : "Non",
                        csvEchapper(t.getEffetsSecondaires())
                );
            }
        }
    }

    private static String csvEchapper(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }
}