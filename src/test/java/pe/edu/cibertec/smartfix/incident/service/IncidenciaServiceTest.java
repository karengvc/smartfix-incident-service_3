package pe.edu.cibertec.smartfix.incident.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.cibertec.smartfix.incident.model.*;
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

    @InjectMocks
    private IncidenciaService service;

    private Incidencia incidencia;

    @BeforeEach
    void prepararDatos() {
        incidencia = new Incidencia();
        incidencia.setId(1L);
        incidencia.setTitulo("Computadora no enciende");
        incidencia.setDescripcion("Equipo sin energia");
        incidencia.setPrioridad(Prioridad.ALTA);
        incidencia.setEquipoId(1L);
        incidencia.setEstado(EstadoIncidencia.REPORTADA);
    }

    @Test
    void registrarIncidencia() {
        when(repository.save(any(Incidencia.class)))
                .thenAnswer(invocacion -> {
                    Incidencia datos = invocacion.getArgument(0);
                    datos.setId(1L);
                    return datos;
                });

        Incidencia resultado = service.registrar(incidencia);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(EstadoIncidencia.REPORTADA, resultado.getEstado());

        verify(repository).save(any(Incidencia.class));
        verify(historialRepository).save(any(HistorialIncidencia.class));
    }

    @Test
    void buscarIncidenciaPorId() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(incidencia));

        Incidencia resultado = service.buscarPorId(1L);

        assertEquals("Computadora no enciende", resultado.getTitulo());
        assertEquals(Prioridad.ALTA, resultado.getPrioridad());
    }

    @Test
    void asignarTecnico() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(incidencia));

        when(repository.save(any(Incidencia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Incidencia resultado = service.asignarTecnico(1L, 2L);

        assertEquals(2L, resultado.getTecnicoId());
        assertEquals(EstadoIncidencia.ASIGNADA, resultado.getEstado());

        verify(historialRepository).save(any(HistorialIncidencia.class));
    }

    @Test
    void rechazarCambioDeEstadoInvalido() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(incidencia));

        assertThrows(
                ResponseStatusException.class,
                () -> service.cambiarEstado(
                        1L,
                        EstadoIncidencia.CERRADA
                )
        );

        verify(repository, never()).save(any(Incidencia.class));
        verify(historialRepository, never())
                .save(any(HistorialIncidencia.class));
    }
}

