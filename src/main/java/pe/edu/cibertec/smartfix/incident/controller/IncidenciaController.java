package pe.edu.cibertec.smartfix.incident.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.smartfix.incident.model.EstadoIncidencia;
import pe.edu.cibertec.smartfix.incident.model.HistorialIncidencia;
import pe.edu.cibertec.smartfix.incident.model.Incidencia;
import pe.edu.cibertec.smartfix.incident.service.Actor;
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

    private Actor actor(HttpServletRequest request) {
        return new Actor((Long) request.getAttribute("usuarioId"), (String) request.getAttribute("rol"));
    }

    @GetMapping
    public List<Incidencia> listar(HttpServletRequest request) {
        return service.listar(actor(request));
    }

    @GetMapping("/{id}")
    public Incidencia buscar(@PathVariable Long id, HttpServletRequest request) {
        return service.buscarVisible(id, actor(request));
    }

    @PostMapping
    public ResponseEntity<Incidencia> registrar(
            @Valid @RequestBody Incidencia incidencia, HttpServletRequest request) {
        Incidencia creada = service.registrar(incidencia, actor(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
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

    @PatchMapping("/{id}/tomar")
    public Incidencia tomar(@PathVariable Long id, HttpServletRequest request) {
        return service.tomar(id, actor(request));
    }

    @PatchMapping("/{id}/avanzar")
    public Incidencia avanzar(@PathVariable Long id, HttpServletRequest request) {
        return service.avanzar(id, actor(request));
    }

    @PatchMapping("/{id}/estado")
    public Incidencia cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado(), request.tecnicoId());
    }

    @PatchMapping("/{id}/asignacion")
    public Incidencia asignarTecnico(
            @PathVariable Long id,
            @Valid @RequestBody AsignacionRequest request) {
        return service.asignarTecnico(id, request.tecnicoId());
    }

    @PatchMapping("/{id}/devolucion")
    public Incidencia devolver(
            @PathVariable Long id,
            @Valid @RequestBody DevolucionRequest cuerpo,
            HttpServletRequest request) {
        return service.devolver(id, cuerpo.motivo(), actor(request));
    }

    @GetMapping("/{id}/historial")
    public List<HistorialIncidencia> obtenerHistorial(
            @PathVariable Long id, HttpServletRequest request) {
        return service.obtenerHistorial(id, actor(request));
    }

    public record CambioEstadoRequest(
            @NotNull EstadoIncidencia estado,
            Long tecnicoId
    ) {}

    public record AsignacionRequest(
            @NotNull @Positive Long tecnicoId
    ) {}

    public record DevolucionRequest(
            @NotBlank @Size(max = 250) String motivo
    ) {}
}
