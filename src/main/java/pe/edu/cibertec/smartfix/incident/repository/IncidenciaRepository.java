package pe.edu.cibertec.smartfix.incident.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.smartfix.incident.model.Incidencia;
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
}
