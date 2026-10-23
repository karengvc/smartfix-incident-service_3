# SmartFix - Microservicio de Incidencias

Microservicio desarrollado para el sistema SmartFix, encargado de gestionar las incidencias relacionadas con los equipos tecnológicos.

## Tecnologias utilizadas

- Java 25
- Spring Boot 4.1.1
- Spring Data JPA
- MySQL
- Maven
- Docker
- JUnit 5 y Mockito
- Postman

## Funcionalidades

- Registrar nuevas incidencias.
- Consultar todas las incidencias.
- Buscar incidencias por ID.
- Actualizar los datos de una incidencia.
- Eliminar incidencias.
- Asignar tecnicos responsables.
- Cambiar el estado de las incidencias.
- Consultar el historial de atencion.
- Validar cambios de estado no permitidos.

## Estados de una incidencia

REPORTADA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA

## Endpoints

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | /api/incidencias | Listar incidencias |
| GET | /api/incidencias/{id} | Buscar incidencia |
| POST | /api/incidencias | Registrar incidencia |
| PUT | /api/incidencias/{id} | Actualizar incidencia |
| DELETE | /api/incidencias/{id} | Eliminar incidencia |
| PATCH | /api/incidencias/{id}/asignacion | Asignar tecnico |
| PATCH | /api/incidencias/{id}/estado | Cambiar estado |
| GET | /api/incidencias/{id}/historial | Consultar historial |

## Base de datos

Motor: MySQL

Base de datos: smartfix_incidentes_db

Tablas:
- incidencias
- historial_incidencias

## Configuracion

El microservicio utiliza variables de entorno:

- SERVER_PORT: puerto de ejecucion, por defecto 8083.
- DB_URL: direccion de conexion a MySQL.
- DB_USERNAME: usuario de MySQL.
- DB_PASSWORD: contrasena de MySQL.

La base de datos debe existir antes de ejecutar el servicio.

## Ejecucion local

Configurar las variables de entorno y ejecutar:

./mvnw spring-boot:run

En Windows se puede utilizar:

mvnw.cmd spring-boot:run

## Ejecucion con Docker

Construir la imagen:

docker build -t smartfix-incident-service-3 .

Ejecutar el contenedor con las variables de entorno configuradas:

docker run --name smartfix-incidentes -p 8083:8083 -e DB_URL="jdbc:mysql://host.docker.internal:3306/smartfix_incidentes_db" -e DB_USERNAME=root -e DB_PASSWORD="<tu_contrasena>" smartfix-incident-service-3

## Pruebas automatizadas

Ejecutar:

mvnw.cmd test

Se implementaron pruebas para:
- Registro de incidencias.
- Busqueda por ID.
- Asignacion de tecnicos.
- Validacion de cambios de estado.

Tambien se verifica el inicio del contexto de Spring Boot.

Resultado de las pruebas realizadas: 5 pruebas aprobadas.

## Integracion

El microservicio utiliza el puerto 8083 y expone una API REST para su consumo desde el frontend Angular de SmartFix.

## Proyecto academico

Sistema SmartFix - Gestion de Incidencias y Mantenimiento de Equipos Tecnologicos.

Modulo desarrollado: Gestion de Incidencias (Integrante 3).
