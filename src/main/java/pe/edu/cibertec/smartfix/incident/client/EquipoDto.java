package pe.edu.cibertec.smartfix.incident.client;

public record EquipoDto(
        Long id,
        String nombre,
        String tipo,
        String marca,
        String modelo,
        String numeroSerie,
        String estado,
        String descripcion,
        Long propietarioId
) {}
