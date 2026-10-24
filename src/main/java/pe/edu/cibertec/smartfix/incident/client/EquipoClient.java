package pe.edu.cibertec.smartfix.incident.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "equipment-service",
        url = "${equipment.service.url:http://equipment-service:8082}",
        configuration = FeignConfig.class)
public interface EquipoClient {

    @GetMapping("/api/equipos/{id}")
    EquipoDto obtenerPorId(@PathVariable("id") Long id);
}
