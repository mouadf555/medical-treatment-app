package com.medical.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Patient {
    private int id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String sexe;
    private String telephone;
    private String email;
    private boolean sousSurveillance;
    private String observations;
    private List<Traitement> traitements;

    public Patient() {
        this.traitements = new ArrayList<>();
    }

    public Patient(int id, String nom, String prenom, LocalDate dateNaissance,
                   String sexe, String telephone, String email,
                   boolean sousSurveillance, String observations) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.sexe = sexe;
        this.telephone = telephone;
        this.email = email;
        this.sousSurveillance = sousSurveillance;  // corrigé
        this.observations = observations;
        this.traitements = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isSousSurveillance() { return sousSurveillance; }
    public void setSousSurveillance(boolean sousSurveillance) { this.sousSurveillance = sousSurveillance; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<Traitement> getTraitements() { return traitements; }
    public void setTraitements(List<Traitement> traitements) { this.traitements = traitements; }

    public String getNomComplet() { return prenom + " " + nom; }

    public int getAge() {
        if (dateNaissance == null) return 0;
        return LocalDate.now().getYear() - dateNaissance.getYear();
    }

    @Override
    public String toString() {
        return getNomComplet();
    }
}