package pe.edu.cibertec.smartfix.incident.service;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.cibertec.smartfix.incident.model.*;
import pe.edu.cibertec.smartfix.incident.repository.HistorialIncidenciaRepository;
import pe.edu.cibertec.smartfix.incident.repository.IncidenciaRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidenciaService {

    private final IncidenciaRepository repository;
    private final HistorialIncidenciaRepository historialRepository;

    public IncidenciaService(
            IncidenciaRepository repository,
            HistorialIncidenciaRepository historialRepository) {
        this.repository = repository;
        this.historialRepository = historialRepository;
    }

    public List<Incidencia> listar() {
        return repository.findAll();
    }

    public Incidencia buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incidencia no encontrada"
                ));}
    @Transactional
    public Incidencia registrar(Incidencia incidencia) {
        incidencia.setId(null);
        incidencia.setEstado(EstadoIncidencia.REPORTADA);
        incidencia.setFechaReporte(LocalDateTime.now());
        incidencia.setTecnicoId(null);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(
                guardada.getId(),
                null,
                EstadoIncidencia.REPORTADA,
                "Incidencia registrada"
        );
        return guardada;
    }

    public Incidencia actualizar(Long id, Incidencia datos) {
        Incidencia incidencia = buscarPorId(id);
        incidencia.setTitulo(datos.getTitulo());
        incidencia.setDescripcion(datos.getDescripcion());
        incidencia.setPrioridad(datos.getPrioridad());
        incidencia.setEquipoId(datos.getEquipoId());

        return repository.save(incidencia);
    }

    @Transactional
    public void eliminar(Long id) {
        Incidencia incidencia = buscarPorId(id);

        historialRepository.deleteAll(
                historialRepository.findByIncidenciaIdOrderByFechaCambioAsc(id)
        );
        repository.delete(incidencia);}

    @Transactional
    public Incidencia asignarTecnico(Long id, Long tecnicoId) {
        Incidencia incidencia = buscarPorId(id);

        if (tecnicoId == null || tecnicoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del tecnico debe ser valido");
        }

        if (incidencia.getEstado() != EstadoIncidencia.REPORTADA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden asignar incidencias reportadas");
        }

        EstadoIncidencia anterior = incidencia.getEstado();
        incidencia.setTecnicoId(tecnicoId);
        incidencia.setEstado(EstadoIncidencia.ASIGNADA);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(
                id,
                anterior,
                EstadoIncidencia.ASIGNADA,
                "Tecnico asignado: " + tecnicoId);
        return guardada;
    }

    @Transactional
    public Incidencia cambiarEstado(Long id, EstadoIncidencia nuevoEstado) {
        Incidencia incidencia = buscarPorId(id);
        EstadoIncidencia actual = incidencia.getEstado();

        if (nuevoEstado == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio");
        }

        boolean permitido = switch (actual) {
            case REPORTADA -> nuevoEstado == EstadoIncidencia.ASIGNADA;
            case ASIGNADA -> nuevoEstado == EstadoIncidencia.EN_PROCESO;
            case EN_PROCESO -> nuevoEstado == EstadoIncidencia.RESUELTA;
            case RESUELTA -> nuevoEstado == EstadoIncidencia.CERRADA;
            case CERRADA -> false;
        };

        if (!permitido) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cambio de estado no permitido");
        }

        if (nuevoEstado == EstadoIncidencia.ASIGNADA
                && incidencia.getTecnicoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Primero debe asignar un tecnico");
        }

        incidencia.setEstado(nuevoEstado);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(
                id,
                actual,
                nuevoEstado,
                "Cambio de estado de " + actual + " a " + nuevoEstado
        );

        return guardada;
    }

    public List<HistorialIncidencia> obtenerHistorial(Long id) {
        buscarPorId(id);
        return historialRepository
                .findByIncidenciaIdOrderByFechaCambioAsc(id);
    }

    private void guardarHistorial(
            Long incidenciaId,
            EstadoIncidencia anterior,
            EstadoIncidencia nuevo,
            String descripcion) {

        HistorialIncidencia historial = new HistorialIncidencia();
        historial.setIncidenciaId(incidenciaId);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setFechaCambio(LocalDateTime.now());
        historial.setDescripcion(descripcion);
        historialRepository.save(historial);
    }
}
