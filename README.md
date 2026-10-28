# EcoRecicla Backend — EcoAPI

Servicio RESTful desarrollado en **Kotlin** con **Spring Boot 3.3.5**, **Java 21**, **Spring Security**, autenticación mediante **JWT** (firmado con HMAC-SHA256), **Spring Data JPA** y base de datos relacional **H2 persistente en archivo local** para el sistema de gestión de rutas y pesajes de la empresa EcoRecicla.

---

## 1. Comando Único de Arranque

Para ejecutar el servicio desde la raíz del repositorio con el perfil local predeterminado y la carga automática de datos semilla:

```powershell
.\mvnw.cmd spring-boot:run
```

*En sistemas Linux/macOS:*
```bash
./mvnw spring-boot:run
```

El servidor quedará accesible en:
`http://localhost:8080/api/v1`

---

## 2. Requisitos del Sistema

- **JDK:** Java 21 LTS (`java -version` >= 21)
- **Maven Wrapper:** Incluido en el repositorio (`mvnw` / `mvnw.cmd`)
- **Base de Datos:** H2 Database embebida persistente en archivo local (`./data/ecorecicla.mv.db`), no requiere instalación externa ni Docker.

---

## 3. Estructura de Paquetes (Sección 4.7)

```
src/main/kotlin/pe/upeu/ecorecicla/
├── config/
│   ├── AppConfig.kt                # Configuración de beans generales y Clock (America/Lima)
│   ├── JwtAuthenticationFilter.kt  # Filtro OncePerRequestFilter para autenticación Bearer
│   ├── JwtService.kt               # Generación y validación de tokens JWT (60 min)
│   ├── ManejadorDeErrores.kt       # RestControllerAdvice con respuestas uniformes (400, 401, 403, 404, 409, 500)
│   └── SecurityConfig.kt           # Spring Security, PasswordEncoder BCrypt y matriz de privilegios
├── seguridad/
│   ├── AuthController.kt           # Endpoints /api/v1/auth/login y /api/v1/auth/me
│   ├── Privilegio.kt               # Enum de privilegios obligatorios y administrativos
│   ├── Rol.kt                      # Enum Rol con mapeo a privilegios
│   ├── Usuario.kt                  # Entidad JPA Usuario con password hash BCrypt
│   └── UsuarioRepository.kt        # Repositorio Spring Data JPA de usuarios
├── dominio/
│   ├── Excepciones.kt              # ReglaNegocioException, RecursoNoEncontradoException, ValidacionNegocioException
│   ├── PesajeEntity.kt             # Entidad JPA para pesajes con cálculo de neto y anulación lógica
│   ├── PuntoAcopioEntity.kt        # Entidad JPA para el padrón de puntos de acopio
│   ├── RutaEntity.kt               # Entidad JPA para rutas de trabajo
│   └── RutaPuntoEntity.kt          # Relación ruta-punto con secuencia, atención y omisión
├── repositorio/
│   ├── PesajeRepository.kt         # Consultas de pesajes, orden descendente e idempotencia
│   ├── PuntoAcopioRepository.kt    # Consultas paginadas y filtradas por zona
│   ├── RutaPuntoRepository.kt      # Consultas de puntos en ruta por secuencia
│   └── RutaRepository.kt           # Búsqueda de rutas por fecha y operario asignado
├── servicio/
│   ├── DataInitializer.kt          # Carga idempotente de datos semilla requeridos
│   ├── PesajeService.kt            # Reglas RN-01, RN-02, RN-03, RN-04, anulación lógica e idempotencia
│   ├── PuntoAcopioService.kt       # Gestión y consulta paginada de puntos
│   └── RutaService.kt              # Ruta del día, registro de omisión y cierre de ruta (RN-04)
├── web/
│   ├── AdminController.kt          # Endpoints administrativos (/api/v1/admin/...)
│   ├── PesajeController.kt         # CRUD completo (/api/v1/pesajes) con DELETE 204
│   ├── PuntoAcopioController.kt    # Endpoints /api/v1/puntos
│   ├── RutaController.kt           # Endpoints /api/v1/rutas
│   └── dto/
│       ├── AuthDto.kt              # DTOs de inicio de sesión y usuario actual
│       ├── ErrorRespuesta.kt       # Formato obligatorio común de error
│       ├── NegocioDto.kt           # DTOs de puntos, rutas, pesajes y solicitudes
│       └── PaginaRespuesta.kt      # Envoltorio obligatorio de paginación
└── Aplicacion.kt                   # Punto de entrada de Spring Boot
```

---

## 4. Contrato de Endpoints REST (/api/v1)

| Método | Ruta | Privilegio Exigido | Códigos de Respuesta | Descripción |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | `200`, `401` | Inicio de sesión, retorna JWT y privilegios |
| `GET` | `/api/v1/auth/me` | Autenticado | `200`, `401` | Consulta usuario y privilegios del token |
| `GET` | `/api/v1/rutas/hoy` | `PUNTO_LEER` | `200`, `401`, `404` | Ruta asignada del día con puntos en orden de visita |
| `GET` | `/api/v1/rutas/{id}` | `PUNTO_LEER` | `200`, `401`, `404` | Detalle de ruta por ID |
| `POST` | `/api/v1/rutas/{id}/cerrar` | `RUTA_CERRAR` | `200`, `401`, `403`, `409` | Cierra la ruta aplicando RN-04 |
| `POST` | `/api/v1/rutas/{id}/puntos/{pId}/omision` | `PESAJE_CREAR` / `PESAJE_EDITAR` | `200`, `401`, `404`, `409` | Registra motivo de omisión de un punto |
| `GET` | `/api/v1/puntos` | `PUNTO_LEER` | `200`, `401` | Listado paginado de puntos, filtro `?zona=` |
| `POST` | `/api/v1/puntos` | `PUNTO_GESTIONAR` | `201`, `400`, `401`, `403` | Crea punto en el padrón (Admin) |
| `GET` | `/api/v1/puntos/{id}/pesajes` | `PESAJE_LEER` | `200`, `401` | Listado paginado de pesajes desc por fecha |
| `POST` | `/api/v1/pesajes` | `PESAJE_CREAR` | `201`, `400`, `401`, `403`, `409` | Registra pesaje aplicando RN-01 y RN-03 |
| `PUT` | `/api/v1/pesajes/{id}` | `PESAJE_EDITAR` | `200`, `400`, `401`, `403`, `409` | Corrige pesaje aplicando RN-01, RN-02 y RN-03 |
| `DELETE` | `/api/v1/pesajes/{id}` | `PESAJE_ANULAR` | `204`, `401`, `403`, `409` | Anulación lógica con motivo (`?motivo=...`), sin borrado físico |
| `GET` | `/api/v1/admin/usuarios` | `USUARIO_GESTIONAR` | `200`, `401`, `403` | Lista usuarios y roles (Admin) |
| `PUT` | `/api/v1/admin/usuarios/{id}/rol` | `USUARIO_GESTIONAR` | `200`, `400`, `401`, `403` | Modifica rol de usuario (Admin) |
| `POST` | `/api/v1/admin/rutas/asignar` | `PUNTO_GESTIONAR` | `201`, `400`, `401`, `403` | Asigna ruta y secuencia de puntos (Admin) |

---

## 5. Formatos Comunes Obligatorios

### Envoltorio de Paginación (Sección 4.2)
```json
{
  "contenido": [ ... ],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 37,
  "totalPaginas": 2,
  "ultima": false
}
```

### Formato de Errores (Sección 4.2)
```json
{
  "timestamp": "2026-10-27T09:14:22",
  "status": 400,
  "error": "Bad Request",
  "message": "Datos invalidos en la solicitud",
  "path": "/api/v1/pesajes",
  "validationErrors": {
    "tara": "La tara debe ser menor que el peso bruto para que el peso neto sea mayor a cero"
  }
}
```

---

## 6. Seguridad y Matriz de Privilegios

| Privilegio | Descripción | OPERARIO | SUPERVISOR | ADMINISTRADOR |
|---|---|:---:|:---:|:---:|
| `PUNTO_LEER` | Consultar ruta y puntos | Sí | Sí | Sí |
| `PESAJE_LEER` | Consultar pesajes de un punto | Sí | Sí | Sí |
| `PESAJE_CREAR` | Registrar pesaje nuevo | Sí | Sí | **No** |
| `PESAJE_EDITAR` | Corregir pesaje existente | Solo propios mismo día | Todos | **No** |
| `PESAJE_ANULAR` | Anular pesaje lógicamente | **No** | Sí | **No** |
| `RUTA_CERRAR` | Cerrar ruta del día | **No** | Sí | **No** |
| `PUNTO_GESTIONAR` | Crear puntos y asignar rutas | **No** | **No** | Sí |
| `USUARIO_GESTIONAR`| Administrar usuarios y roles | **No** | **No** | Sí |

- **JWT:** Vigencia de 60 minutos, firmado con HMAC-SHA256. Clave persistente en `./data/jwt.key`.
- **Contraseñas:** Cifradas con BCrypt.

---

## 7. Reglas de Negocio Implementadas

- **RN-01:** No se puede registrar ni corregir pesajes en una ruta cerrada. Responde `409 Conflict`.
- **RN-02:** Un operario solo corrige pesajes propios y del mismo día (zona horaria `America/Lima`). Si es ajeno o fuera de plazo, responde `403 Forbidden`. El supervisor puede corregir pesajes de otros operarios siempre que la ruta no esté cerrada.
- **RN-03:** El servidor calcula `pesoNeto = pesoBruto - tara`. Debe ser > 0. Si no, responde `400 Bad Request` con `validationErrors: {"tara": "..."}`. El cliente nunca envía `pesoNeto`.
- **RN-04:** La ruta no puede cerrarse si algún punto planificado carece de pesaje válido o motivo de omisión. Responde `409 Conflict` indicando cuántos puntos faltan.

---

## 8. Usuarios de Prueba y Datos Semilla

| Usuario | Contraseña | Rol | Finalidad en la Evaluación |
|---|---|---|---|
| `operario1` | `Eco2026*` | `OPERARIO` | Registro y edición de pesajes propios en la ruta abierta de hoy |
| `supervisor1` | `Eco2026*` | `SUPERVISOR` | Corrección de pesajes ajenos, anulación lógica y cierre de ruta |
| `admin` | `Eco2026*` | `ADMINISTRADOR` | Gestión de padrón de puntos y usuarios (sin permisos de pesaje) |
| `operario2` | `Eco2026*` | `OPERARIO` | Usuario adicional para evidenciar rechazo 403 de RN-02 (pesaje ajeno) |

- **Puntos de acopio:** 12 puntos distribuidos en zonas Norte, Sur y Centro.
- **Ruta de ayer:** Cerrada con 6 puntos y 10 pesajes completados (para evidenciar RN-01).
- **Ruta de hoy:** Abierta con 6 puntos asignados a `operario1`, 5 pesajes (incluyendo 1 anulado con motivo y 1 de `operario2`), y 3 puntos sin pesar (para evidenciar RN-04).

---

## 9. Pruebas Automatizadas

Para ejecutar la suite de 16 pruebas de integración:

```powershell
.\mvnw.cmd test
```

Reporte generado en: `target/surefire-reports/pe.upeu.ecorecicla.EcoApiIntegrationTests.txt`
Colección de Postman: [`EcoAPI_Coleccion_Examen.json`](file:///c:/Users/XD/Documents/EcoRecicla/EcoReciclaBackend/EcoAPI_Coleccion_Examen.json)
