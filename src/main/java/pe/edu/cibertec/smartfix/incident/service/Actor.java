package pe.edu.cibertec.smartfix.incident.service;

public record Actor(Long id, String rol) {

    public boolean esUsuario() {
        return "USUARIO".equals(rol);
    }

    public boolean esTecnico() {
        return "TECNICO".equals(rol);
    }

    public boolean esAdmin() {
        return "ADMINISTRADOR".equals(rol);
    }
}
