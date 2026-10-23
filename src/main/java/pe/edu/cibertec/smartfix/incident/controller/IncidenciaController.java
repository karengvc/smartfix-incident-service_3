package pe.edu.cibertec.smartfix.incident.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.smartfix.incident.model.EstadoIncidencia;
import pe.edu.cibertec.smartfix.incident.model.HistorialIncidencia;
import pe.edu.cibertec.smartfix.incident.model.Incidencia;
import pe.edu.cibertec.smartfix.incident.service.IncidenciaService;
import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
@CrossOrigin(origins = "http://localhost:4200")
public class IncidenciaController {

    private final IncidenciaService service;
    public IncidenciaController(IncidenciaService service) {
        this.service = service;
    }
    @GetMapping
    public List<Incidencia> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Incidencia buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Incidencia> registrar(
            @Valid @RequestBody Incidencia incidencia) {
        Incidencia creada = service.registrar(incidencia);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(creada);
    }

    @PutMapping("/{id}")
    public Incidencia actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Incidencia incidencia) {
        return service.actualizar(id, incidencia);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public Incidencia cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request) {

        return service.cambiarEstado(id, request.estado());
    }

    @PatchMapping("/{id}/asignacion")
    public Incidencia asignarTecnico(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionRequest request) {

        return service.asignarTecnico(id, request.tecnicoId());
    }

    @GetMapping("/{id}/historial")
    public List<HistorialIncidencia> obtenerHistorial(
            @PathVariable Long id) {

        return service.obtenerHistorial(id);
    }

    public record CambioEstadoRequest(
            @NotNull EstadoIncidencia estado
    ) {}

    public record AsignacionRequest(
            @NotNull @Positive Long tecnicoId
    ) {}
}
