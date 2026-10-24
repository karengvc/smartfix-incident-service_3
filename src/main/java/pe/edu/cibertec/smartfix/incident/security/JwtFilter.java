package pe.edu.cibertec.smartfix.incident.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String TECNICO = "TECNICO";
    private static final String USUARIO = "USUARIO";

    private final String secret;

    public JwtFilter(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            responder(response, 401, "Debes iniciar sesión");
            return;
        }
        JwtUtil.Claims claims = JwtUtil.validar(secret, cabecera.substring(7));
        if (claims == null) {
            responder(response, 401, "Tu sesión expiró o no es válida");
            return;
        }
        Set<String> permitidos = rolesPermitidos(request.getMethod(), request.getRequestURI());
        if (!permitidos.isEmpty() && !permitidos.contains(claims.rol())) {
            responder(response, 403, "Tu rol (" + claims.rol() + ") no tiene permiso para esta acción");
            return;
        }
        request.setAttribute("usuarioId", claims.id());
        request.setAttribute("rol", claims.rol());
        chain.doFilter(request, response);
    }

    private Set<String> rolesPermitidos(String metodo, String ruta) {
        if (metodo.equals("GET")) {
            return Set.of();
        }
        if (metodo.equals("POST")) {
            return Set.of(ADMIN, USUARIO);
        }
        if (ruta.endsWith("/tomar")) {
            return Set.of(TECNICO);
        }
        if (ruta.endsWith("/avanzar")) {
            return Set.of(ADMIN, TECNICO);
        }
        if (ruta.endsWith("/devolucion")) {
            return Set.of(ADMIN, USUARIO);
        }
        return Set.of(ADMIN);
    }

    private void responder(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(("{\"mensaje\":\"" + mensaje + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
