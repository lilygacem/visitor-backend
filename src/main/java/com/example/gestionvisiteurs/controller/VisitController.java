
package com.example.gestionvisiteurs.controller;

import com.example.gestionvisiteurs.dto.VisitDto;
import com.example.gestionvisiteurs.model.ServiceEntity;
import com.example.gestionvisiteurs.model.Statut;
import com.example.gestionvisiteurs.model.Visit;
import com.example.gestionvisiteurs.repository.ServiceRepository;
import com.example.gestionvisiteurs.repository.VisitRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") 
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitRepository visitRepository;
    private final ServiceRepository serviceRepository;

    public VisitController(VisitRepository visitRepository, ServiceRepository serviceRepository) {
        this.visitRepository = visitRepository;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    public List<VisitDto> getAllVisits() {
        return visitRepository.findAll().stream().map(visit -> {
            VisitDto dto = new VisitDto();
            dto.setId(visit.getId_visit());
            dto.setNom(visit.getNom());
            dto.setPrenom(visit.getPrenom());
            dto.setNumeroId(visit.getNumeroId());
            dto.setHeureArrivee(visit.getVisitDate() != null ? visit.getVisitDate().toString() : null);
            dto.setHeureSortie(visit.getExitDate() != null ? visit.getExitDate().toString() : null);
            dto.setServiceVisite(visit.getService().getNomService());
            dto.setStatut(visit.getStatus() != null ? visit.getStatus().toString() : "-");
            dto.setServiceVisite(visit.getService().getNomService());
            dto.setServiceId(visit.getService().getId()); // Ajouter cette ligne
            dto.setServiceVisite(visit.getService().getNomService());
            dto.setSatisfaction(visit.getSatisfaction());
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<VisitDto> searchVisitsFlexible(@RequestParam String query) {
        String[] parts = query.split(" ");
        String part1 = parts.length > 0 ? parts[0] : "";
        String part2 = parts.length > 1 ? parts[1] : "";

        return visitRepository.searchByNomAndPrenomFlexible(part1, part2)
                .stream()
                .map(visit -> {
                    VisitDto dto = new VisitDto();
                    dto.setId(visit.getId_visit());
                    dto.setNom(visit.getNom());
                    dto.setPrenom(visit.getPrenom());
                    dto.setNumeroId(visit.getNumeroId());
                    dto.setHeureArrivee(visit.getVisitDate() != null ? visit.getVisitDate().toString() : null);
                    dto.setHeureSortie(visit.getExitDate() != null ? visit.getExitDate().toString() : null);
                    dto.setServiceVisite(visit.getService().getNomService());
                    dto.setStatut(visit.getStatus().name()); // Au lieu de toString()
                    dto.setServiceVisite(visit.getService().getNomService());
                    dto.setServiceId(visit.getService().getId()); // Ajouter cette ligne
                    dto.setSatisfaction(visit.getSatisfaction());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @PostMapping
    public Visit addVisit(@RequestBody VisitDto visitDto) {
        ServiceEntity service = serviceRepository.findById(visitDto.getServiceId()).orElseThrow();

        Visit visit = new Visit();
        visit.setNom(visitDto.getNom());
        visit.setPrenom(visitDto.getPrenom());
        visit.setNumeroId(visitDto.getNumeroId());
        visit.setVisitDate(LocalDateTime.parse(visitDto.getHeureArrivee()));
        visit.setExitDate(visitDto.getHeureSortie() != null ? LocalDateTime.parse(visitDto.getHeureSortie()) : null);
        visit.setService(service);
        visit.setStatus(Statut.EN_ATTENTE);
        visit.setSatisfaction(visitDto.getSatisfaction());

        return visitRepository.save(visit);
    }

    @PutMapping("/{id}")
    public Visit updateVisitById(@PathVariable Long id, @RequestBody Visit updatedVisit) {
        Visit existingVisit = visitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visit not found with id: " + id));
        existingVisit.setNom(updatedVisit.getNom());
        existingVisit.setPrenom(updatedVisit.getPrenom());
        existingVisit.setNumeroId(updatedVisit.getNumeroId());
        existingVisit.setVisitDate(updatedVisit.getVisitDate());
        existingVisit.setExitDate(updatedVisit.getExitDate());
        existingVisit.setStatus(updatedVisit.getStatus()); // Ajout de cette ligne
        if (updatedVisit.getService() != null) {
            existingVisit.setService(updatedVisit.getService());
        }
        existingVisit.setSatisfaction(updatedVisit.getSatisfaction());
        return visitRepository.save(existingVisit);
    }

    @PutMapping("/{id}/statut")
    public Visit updateVisitStatut(@PathVariable Long id, @RequestParam String statut) {
        Visit visit = visitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visit not found with id: " + id));
    
        Statut newStatus;
        try {
            newStatus = Statut.valueOf(statut.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide. Valeurs possibles : PRESENT, EN_ATTENTE, CLOTURE");
        }
    
        visit.setStatus(newStatus);
    
        // Mise à jour du statut du service
        ServiceEntity service = visit.getService();
        if (newStatus == Statut.PRESENT) {
            service.setStatut("occupé");
        } else {
            service.setStatut("libre");
        }
        serviceRepository.save(service);
    
        return visitRepository.save(visit);
    }
    @DeleteMapping("/{id}")
    public void deleteVisit(@PathVariable Long id) {
        visitRepository.deleteById(id);
    }

    @GetMapping("/count/today")
public Map<String, Long> getTodayVisitsCount() {
    LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
    
    long total = visitRepository.countByVisitDateBetween(startOfDay, endOfDay);
    long presents = visitRepository.countByStatusAndVisitDateBetween(Statut.PRESENT, startOfDay, endOfDay);
    long pending = visitRepository.countByStatusAndVisitDateBetween(Statut.EN_ATTENTE, startOfDay, endOfDay);
    
    return Map.of(
        "total", total,
        "presents", presents,
        "pending", pending
    );
}

@GetMapping("/recent")
public List<VisitDto> getRecentVisits() {
    return visitRepository.findTop5ByOrderByVisitDateDesc()
        .stream()
        .map(visit -> {
            VisitDto dto = new VisitDto();
            dto.setId(visit.getId_visit());
            dto.setNom(visit.getNom());
            dto.setPrenom(visit.getPrenom());
            dto.setStatut(visit.getStatus().toString());
            return dto;
        })
        .collect(Collectors.toList());
}
}
