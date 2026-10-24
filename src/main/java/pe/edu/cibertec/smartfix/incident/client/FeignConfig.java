package pe.edu.cibertec.smartfix.incident.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor tokenInterceptor() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String token = attrs.getRequest().getHeader("Authorization");
                if (token != null) {
                    template.header("Authorization", token);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (metodo, respuesta) -> {
            if (respuesta.status() == 404) {
                return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El equipo indicado no existe o no pertenece al cliente");
            }
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de equipos no respondió correctamente");
        };
    }
}
