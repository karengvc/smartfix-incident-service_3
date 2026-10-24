package pe.edu.cibertec.smartfix.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.smartfix.incident.model.Incidencia;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findBySolicitanteIdOrderByFechaReporteDesc(Long solicitanteId);
}
