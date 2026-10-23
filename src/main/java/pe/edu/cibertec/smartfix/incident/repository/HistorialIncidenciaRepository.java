package pe.edu.cibertec.smartfix.incident.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.smartfix.incident.model.HistorialIncidencia;
import java.util.List;

public interface HistorialIncidenciaRepository
        extends JpaRepository<HistorialIncidencia, Long> {

    List<HistorialIncidencia> findByIncidenciaIdOrderByFechaCambioAsc(
            Long incidenciaId
    );
}
