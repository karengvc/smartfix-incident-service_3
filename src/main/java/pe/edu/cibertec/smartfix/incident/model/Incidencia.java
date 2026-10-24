package pe.edu.cibertec.smartfix.incident.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencias")
@Data
public class Incidencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "El titulo es obligatorio")
    @Column(nullable = false)
    private String titulo;
    @NotBlank(message = "La descripcion es obligatoria")
    @Column(nullable = false, length = 1000)
    private String descripcion;
    @NotNull(message = "La prioridad es obligatoria")
    @Enumerated(EnumType.STRING)

    @Column(nullable = false)

    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)

    private EstadoIncidencia estado = EstadoIncidencia.RECIBIDO;

    private LocalDateTime fechaReporte = LocalDateTime.now();
    @NotNull(message = "El equipo es obligatorio")
    @Column(nullable = false)
    private Long equipoId;

    private Long tecnicoId;

    private Long solicitanteId;

    @Column(length = 20)
    private String canal;

    @Column(length = 300)
    private String motivoDevolucion;

    private Integer reingresos = 0;

    @PrePersist
    public void antesDeGuardar() {
        if (estado == null) {
            estado = EstadoIncidencia.RECIBIDO;
        }

        if (fechaReporte == null) {
            fechaReporte = LocalDateTime.now();
        }
    }
}
