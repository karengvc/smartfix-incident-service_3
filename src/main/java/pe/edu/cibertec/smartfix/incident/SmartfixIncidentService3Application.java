package pe.edu.cibertec.smartfix.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class SmartfixIncidentService3Application {

    public static void main(String[] args) {
        SpringApplication.run(SmartfixIncidentService3Application.class, args);
    }

}
