package pe.edu.cibertec.smartfix.incident.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.cibertec.smartfix.incident.client.EquipoClient;
import pe.edu.cibertec.smartfix.incident.client.EquipoDto;
import pe.edu.cibertec.smartfix.incident.model.EstadoIncidencia;
import pe.edu.cibertec.smartfix.incident.model.HistorialIncidencia;
import pe.edu.cibertec.smartfix.incident.model.Incidencia;
import pe.edu.cibertec.smartfix.incident.repository.HistorialIncidenciaRepository;
import pe.edu.cibertec.smartfix.incident.repository.IncidenciaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidenciaService {

    private final IncidenciaRepository repository;
    private final HistorialIncidenciaRepository historialRepository;
    private final EquipoClient equipoClient;

    public IncidenciaService(
            IncidenciaRepository repository,
            HistorialIncidenciaRepository historialRepository,
            EquipoClient equipoClient) {
        this.repository = repository;
        this.historialRepository = historialRepository;
        this.equipoClient = equipoClient;
    }

    // Un cliente solo ve sus casos; administrador y técnico ven todos
    public List<Incidencia> listar(Actor actor) {
        if (actor.esUsuario()) {
            return repository.findBySolicitanteIdOrderByFechaReporteDesc(actor.id());
        }
        return repository.findAll();
    }

    public Incidencia buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incidencia no encontrada"));
    }

    public Incidencia buscarVisible(Long id, Actor actor) {
        Incidencia incidencia = buscarPorId(id);
        if (actor.esUsuario() && !actor.id().equals(incidencia.getSolicitanteId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada");
        }
        return incidencia;
    }

    // Consulta al servicio de equipos (Feign) antes de aceptar el caso
    private void validarEquipo(Long equipoId, Long clienteId) {
        EquipoDto equipo = equipoClient.obtenerPorId(equipoId);
        if (equipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo indicado no existe");
        }
        if ("BAJA".equalsIgnoreCase(equipo.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El equipo está dado de baja y no admite casos");
        }
        if (equipo.propietarioId() != null && !equipo.propietarioId().equals(clienteId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El equipo no pertenece al cliente indicado");
        }
    }

    @Transactional
    public Incidencia registrar(Incidencia datos, Actor actor) {
        Long cliente;
        String canal;
        if (actor.esUsuario()) {
            cliente = actor.id();
            canal = "ONLINE";
        } else {
            if (datos.getSolicitanteId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Indica el cliente dueño del equipo");
            }
            cliente = datos.getSolicitanteId();
            canal = "PRESENCIAL";
        }
        validarEquipo(datos.getEquipoId(), cliente);

        datos.setId(null);
        datos.setSolicitanteId(cliente);
        datos.setCanal(canal);
        datos.setEstado(EstadoIncidencia.RECIBIDO);
        datos.setFechaReporte(LocalDateTime.now());
        datos.setTecnicoId(null);
        datos.setMotivoDevolucion(null);
        datos.setReingresos(0);
        Incidencia guardada = repository.save(datos);
        guardarHistorial(
                guardada.getId(),
                null,
                EstadoIncidencia.RECIBIDO,
                canal.equals("ONLINE")
                        ? "Caso reportado en línea por el cliente"
                        : "Equipo recibido en mostrador");
        return guardada;
    }

    public Incidencia actualizar(Long id, Incidencia datos) {
        Incidencia incidencia = buscarPorId(id);
        if (!datos.getEquipoId().equals(incidencia.getEquipoId())) {
            validarEquipo(datos.getEquipoId(), incidencia.getSolicitanteId());
        }
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
                historialRepository.findByIncidenciaIdOrderByFechaCambioAsc(id));
        repository.delete(incidencia);
    }

    // Un técnico toma un caso que está esperando atención y ya no puede soltarlo
    @Transactional
    public Incidencia tomar(Long id, Actor actor) {
        Incidencia incidencia = buscarPorId(id);
        EstadoIncidencia anterior = incidencia.getEstado();
        if (anterior != EstadoIncidencia.RECIBIDO && anterior != EstadoIncidencia.DEVUELTO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este caso ya no está esperando técnico");
        }
        if (incidencia.getTecnicoId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este caso ya tiene un técnico asignado");
        }
        incidencia.setTecnicoId(actor.id());
        incidencia.setEstado(EstadoIncidencia.EN_ANALISIS);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(id, anterior, EstadoIncidencia.EN_ANALISIS,
                "Caso tomado por el técnico #" + actor.id());
        return guardada;
    }

    // Solo el administrador puede asignar o reasignar
    @Transactional
    public Incidencia asignarTecnico(Long id, Long tecnicoId) {
        Incidencia incidencia = buscarPorId(id);
        if (tecnicoId == null || tecnicoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del técnico debe ser válido");
        }
        EstadoIncidencia anterior = incidencia.getEstado();
        if (anterior == EstadoIncidencia.FINALIZADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El caso ya finalizó");
        }
        EstadoIncidencia nuevo = anterior;
        if (anterior == EstadoIncidencia.RECIBIDO || anterior == EstadoIncidencia.DEVUELTO) {
            nuevo = EstadoIncidencia.EN_ANALISIS;
        }
        incidencia.setTecnicoId(tecnicoId);
        incidencia.setEstado(nuevo);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(id, anterior, nuevo,
                "Técnico asignado por el administrador: #" + tecnicoId);
        return guardada;
    }

    // El técnico del caso (o el administrador) pasa al siguiente paso del proceso
    @Transactional
    public Incidencia avanzar(Long id, Actor actor) {
        Incidencia incidencia = buscarPorId(id);
        if (actor.esTecnico() && !actor.id().equals(incidencia.getTecnicoId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el técnico asignado puede avanzar este caso");
        }
        EstadoIncidencia actual = incidencia.getEstado();
        EstadoIncidencia siguiente = switch (actual) {
            case EN_ANALISIS -> EstadoIncidencia.EN_REPARACION;
            case EN_REPARACION -> EstadoIncidencia.FINALIZADO;
            default -> null;
        };
        if (siguiente == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "En este estado no se puede avanzar");
        }
        incidencia.setEstado(siguiente);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(id, actual, siguiente,
                "Cambio de estado de " + actual + " a " + siguiente);
        return guardada;
    }

    // Corrección manual del administrador
    @Transactional
    public Incidencia cambiarEstado(Long id, EstadoIncidencia nuevoEstado, Long tecnicoId) {
        if (nuevoEstado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        Incidencia incidencia = buscarPorId(id);
        EstadoIncidencia actual = incidencia.getEstado();
        if (tecnicoId != null && tecnicoId > 0) {
            incidencia.setTecnicoId(tecnicoId);
        }
        boolean exigeTecnico = nuevoEstado == EstadoIncidencia.EN_ANALISIS
                || nuevoEstado == EstadoIncidencia.EN_REPARACION
                || nuevoEstado == EstadoIncidencia.FINALIZADO;
        if (exigeTecnico && incidencia.getTecnicoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Primero debe asignar un técnico");
        }
        if (nuevoEstado == EstadoIncidencia.RECIBIDO || nuevoEstado == EstadoIncidencia.DEVUELTO) {
            incidencia.setTecnicoId(null);
        }
        incidencia.setEstado(nuevoEstado);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(id, actual, nuevoEstado,
                "Estado modificado por el administrador: " + actual + " a " + nuevoEstado);
        return guardada;
    }

    // El cliente devuelve el equipo porque la falla no quedó resuelta
    @Transactional
    public Incidencia devolver(Long id, String motivo, Actor actor) {
        Incidencia incidencia = buscarVisible(id, actor);
        if (incidencia.getEstado() != EstadoIncidencia.FINALIZADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se puede devolver un caso finalizado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Indica el motivo de la devolución");
        }
        String limpio = motivo.trim();
        int anteriores = incidencia.getReingresos() == null ? 0 : incidencia.getReingresos();
        incidencia.setEstado(EstadoIncidencia.DEVUELTO);
        incidencia.setTecnicoId(null);
        incidencia.setMotivoDevolucion(limpio);
        incidencia.setReingresos(anteriores + 1);
        Incidencia guardada = repository.save(incidencia);
        guardarHistorial(id, EstadoIncidencia.FINALIZADO, EstadoIncidencia.DEVUELTO,
                "Devuelto por el cliente: " + limpio);
        return guardada;
    }

    public List<HistorialIncidencia> obtenerHistorial(Long id, Actor actor) {
        buscarVisible(id, actor);
        return historialRepository.findByIncidenciaIdOrderByFechaCambioAsc(id);
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
