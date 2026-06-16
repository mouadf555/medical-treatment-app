package com.medical.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Traitement {
    private int id;
    private int patientId;
    private String nom;
    private String type;         // Antibiotique, Antalgique, etc.
    private String posologie;
    private int prisesParJour;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int dureeEstimee;    // en jours (Slider)
    private boolean actif;
    private String effetsSecondaires;
    private String couleur;      // ColorPicker (optionnel)

    public Traitement() {}

    public Traitement(int id, int patientId, String nom, String type, String posologie,
                      int prisesParJour, LocalDate dateDebut, LocalDate dateFin,
                      int dureeEstimee, boolean actif, String effetsSecondaires) {
        this.id = id;
        this.patientId = patientId;
        this.nom = nom;
        this.type = type;
        this.posologie = posologie;
        this.prisesParJour = prisesParJour;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.dureeEstimee = dureeEstimee;
        this.actif = actif;
        this.effetsSecondaires = effetsSecondaires;
        this.couleur = "#2196F3";
    }

    // Calcule la progression du traitement (0.0 à 1.0)
    public double getProgression() {
        if (dateDebut == null || dateFin == null) return 0.0;
        long total = ChronoUnit.DAYS.between(dateDebut, dateFin);
        long ecoule = ChronoUnit.DAYS.between(dateDebut, LocalDate.now());
        if (total <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, (double) ecoule / total));
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPosologie() { return posologie; }
    public void setPosologie(String posologie) { this.posologie = posologie; }

    public int getPrisesParJour() { return prisesParJour; }
    public void setPrisesParJour(int prisesParJour) { this.prisesParJour = prisesParJour; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getDureeEstimee() { return dureeEstimee; }
    public void setDureeEstimee(int dureeEstimee) { this.dureeEstimee = dureeEstimee; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public String getEffetsSecondaires() { return effetsSecondaires; }
    public void setEffetsSecondaires(String effetsSecondaires) { this.effetsSecondaires = effetsSecondaires; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    public String getStatut() { return actif ? "Actif" : "Terminé"; }

    @Override
    public String toString() { return nom + " (" + type + ")"; }
}