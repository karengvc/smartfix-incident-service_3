package pe.edu.cibertec.smartfix.incident.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.cibertec.smartfix.incident.client.EquipoClient;
import pe.edu.cibertec.smartfix.incident.client.EquipoDto;
import pe.edu.cibertec.smartfix.incident.model.EstadoIncidencia;
import pe.edu.cibertec.smartfix.incident.model.HistorialIncidencia;
import pe.edu.cibertec.smartfix.incident.model.Incidencia;
import pe.edu.cibertec.smartfix.incident.model.Prioridad;
import pe.edu.cibertec.smartfix.incident.repository.HistorialIncidenciaRepository;
import pe.edu.cibertec.smartfix.incident.repository.IncidenciaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository repository;

    @Mock
    private HistorialIncidenciaRepository historialRepository;

    @Mock
    private EquipoClient equipoClient;

    @InjectMocks
    private IncidenciaService service;

    private Incidencia incidencia;
    private final Actor tecnico = new Actor(2L, "TECNICO");
    private final Actor cliente = new Actor(4L, "USUARIO");

    @BeforeEach
    void prepararDatos() {
        incidencia = new Incidencia();
        incidencia.setId(1L);
        incidencia.setTitulo("Computadora no enciende");
        incidencia.setDescripcion("Equipo sin energia");
        incidencia.setPrioridad(Prioridad.ALTA);
        incidencia.setEquipoId(1L);
        incidencia.setSolicitanteId(4L);
        incidencia.setEstado(EstadoIncidencia.RECIBIDO);
    }

    @Test
    void registrarCasoEnLinea() {
        when(equipoClient.obtenerPorId(1L)).thenReturn(
                new EquipoDto(1L, "Laptop", "Laptop", "Dell", "Latitude", "SN-1", "ACTIVO", "", 4L));
        when(repository.save(any(Incidencia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Incidencia resultado = service.registrar(incidencia, cliente);

        assertEquals(EstadoIncidencia.RECIBIDO, resultado.getEstado());
        assertEquals("ONLINE", resultado.getCanal());
        assertEquals(4L, resultado.getSolicitanteId());
        verify(historialRepository).save(any(HistorialIncidencia.class));
    }

    @Test
    void noRegistraEquipoDeOtroCliente() {
        when(equipoClient.obtenerPorId(1L)).thenReturn(
                new EquipoDto(1L, "Laptop", "Laptop", "Dell", "Latitude", "SN-1", "ACTIVO", "", 9L));

        assertThrows(ResponseStatusException.class,
                () -> service.registrar(incidencia, cliente));
        verify(repository, never()).save(any(Incidencia.class));
    }

    @Test
    void tecnicoTomaElCaso() {
        when(repository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(repository.save(any(Incidencia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Incidencia resultado = service.tomar(1L, tecnico);

        assertEquals(EstadoIncidencia.EN_ANALISIS, resultado.getEstado());
        assertEquals(2L, resultado.getTecnicoId());
    }

    @Test
    void tecnicoNoAvanzaCasoAjeno() {
        incidencia.setEstado(EstadoIncidencia.EN_ANALISIS);
        incidencia.setTecnicoId(3L);
        when(repository.findById(1L)).thenReturn(Optional.of(incidencia));

        assertThrows(ResponseStatusException.class,
                () -> service.avanzar(1L, tecnico));
    }

    @Test
    void soloSeDevuelveUnCasoFinalizado() {
        when(repository.findById(1L)).thenReturn(Optional.of(incidencia));

        assertThrows(ResponseStatusException.class,
                () -> service.devolver(1L, "Sigue fallando", cliente));
    }
}
