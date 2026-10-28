package pe.upeu.ecorecicla

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import pe.upeu.ecorecicla.dominio.EstadoPesajeTipo
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.web.dto.*

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = ["classpath:application-test.properties"])
class EcoApiIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var rutaRepository: RutaRepository

    @Autowired
    private lateinit var puntoAcopioRepository: PuntoAcopioRepository

    @Autowired
    private lateinit var pesajeRepository: PesajeRepository

    private fun obtenerToken(usuario: String, contrasena: String = "Eco2026*"): String {
        val loginReq = LoginRequest(usuario, contrasena)
        val mvcResult = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andReturn()

        val resp = objectMapper.readValue(mvcResult.response.contentAsString, LoginResponse::class.java)
        return resp.token
    }

    @Test
    fun `testLoginCorrecto devuelve token y privilegios`() {
        val loginReq = LoginRequest("operario1", "Eco2026*")
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.usuario").value("operario1"))
            .andExpect(jsonPath("$.rol").value("OPERARIO"))
            .andExpect(jsonPath("$.token").isString)
            .andExpect(jsonPath("$.privilegios").isArray)
    }

    @Test
    fun `testLoginIncorrecto devuelve 401 con formato ErrorRespuesta`() {
        val loginReq = LoginRequest("operario1", "PasswordErroneo")
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Credenciales incorrectas"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
    }

    @Test
    fun `testTokenAusenteProduce401`() {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Token ausente, alterado o vencido"))
    }

    @Test
    fun `testTokenAlteradoProduce401`() {
        val tokenValido = obtenerToken("operario1")
        val tokenAlterado = tokenValido + "X"
        mockMvc.perform(
            get("/api/v1/auth/me")
                .header("Authorization", "Bearer $tokenAlterado")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun `testMatrizPrivilegiosAdminSinPermisosPesaje devuelve 403`() {
        val tokenAdmin = obtenerToken("admin")
        val punto = puntoAcopioRepository.findAll().first()

        val req = PesajeRequest(
            puntoId = punto.id,
            material = "Plástico",
            pesoBruto = 50.0,
            tara = 5.0
        )

        mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenAdmin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Acceso denegado: no posee el privilegio requerido"))
    }

    @Test
    fun `testMatrizPrivilegiosOperarioSinPermisoAnular devuelve 403`() {
        val tokenOperario = obtenerToken("operario1")
        val pesaje = pesajeRepository.findAll().first()

        mockMvc.perform(
            delete("/api/v1/pesajes/${pesaje.id}")
                .header("Authorization", "Bearer $tokenOperario")
                .param("motivo", "Intento anular")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `testMatrizPrivilegiosOperarioSinPermisoCerrarRuta devuelve 403`() {
        val tokenOperario = obtenerToken("operario1")
        val ruta = rutaRepository.findAll().first()

        mockMvc.perform(
            post("/api/v1/rutas/${ruta.id}/cerrar")
                .header("Authorization", "Bearer $tokenOperario")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `testRN01RechazaPesajeEnRutaCerrada devuelve 409`() {
        val tokenSupervisor = obtenerToken("supervisor1")
        val rutaCerrada = rutaRepository.findAll().first { it.estado.name == "CERRADA" }
        val punto = puntoAcopioRepository.findAll().first()

        val req = PesajeRequest(
            puntoId = punto.id,
            rutaId = rutaCerrada.id,
            material = "Plástico",
            pesoBruto = 80.0,
            tara = 8.0
        )

        mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenSupervisor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("No se puede registrar ni corregir ningún pesaje en una ruta ya cerrada"))
    }

    @Test
    fun `testRN02OperarioNoCorrigePesajeAjeno devuelve 403`() {
        val tokenOperario1 = obtenerToken("operario1")
        // Buscar el pesaje registrado por operario2
        val pesajeOperario2 = pesajeRepository.findAll().first { it.operario == "operario2" }

        val updateReq = PesajeUpdateRequest(
            material = "Metal Corregido",
            pesoBruto = 220.0,
            tara = 20.0
        )

        mockMvc.perform(
            put("/api/v1/pesajes/${pesajeOperario2.id}")
                .header("Authorization", "Bearer $tokenOperario1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Acceso denegado: no posee el privilegio requerido"))
    }

    @Test
    fun `testRN02SupervisorSiPuedeCorregirPesajeAjeno devuelve 200`() {
        val tokenSupervisor = obtenerToken("supervisor1")
        val pesajeOperario2 = pesajeRepository.findAll().first {
            it.operario == "operario2" && it.ruta?.estado?.name == "ABIERTA"
        }

        val updateReq = PesajeUpdateRequest(
            material = "Metal Actualizado por Supervisor",
            pesoBruto = 250.0,
            tara = 25.0
        )

        mockMvc.perform(
            put("/api/v1/pesajes/${pesajeOperario2.id}")
                .header("Authorization", "Bearer $tokenSupervisor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pesoNeto").value(225.0))
            .andExpect(jsonPath("$.material").value("Metal Actualizado por Supervisor"))
    }

    @Test
    fun `testRN03CalculoPesoNetoYRechazoNetoInvalido devuelve 400`() {
        val tokenOperario = obtenerToken("operario1")
        val punto = puntoAcopioRepository.findAll().first()

        // Bruto 10.0 <= Tara 15.0
        val reqInvalido = PesajeRequest(
            puntoId = punto.id,
            material = "Cartón",
            pesoBruto = 10.0,
            tara = 15.0
        )

        mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenOperario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqInvalido))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.tara").exists())
    }

    @Test
    fun `testRN03ServidorCalculaPesoNetoExitosamente devuelve 201`() {
        val tokenOperario = obtenerToken("operario1")
        val punto = puntoAcopioRepository.findAll().first()

        val reqValido = PesajeRequest(
            puntoId = punto.id,
            material = "Vidrio",
            pesoBruto = 100.0,
            tara = 12.5,
            clientOperationId = "op-test-100"
        )

        mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenOperario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqValido))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.pesoNeto").value(87.5))
            .andExpect(jsonPath("$.operario").value("operario1"))
            .andExpect(jsonPath("$.estado.tipo").value("REGISTRADO"))
    }

    @Test
    fun `testRN04NoCerrarRutaConPuntosFaltantes devuelve 409`() {
        val tokenSupervisor = obtenerToken("supervisor1")
        val rutaAbierta = rutaRepository.findAll().first { it.estado.name == "ABIERTA" }

        mockMvc.perform(
            post("/api/v1/rutas/${rutaAbierta.id}/cerrar")
                .header("Authorization", "Bearer $tokenSupervisor")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("La ruta no puede cerrarse mientras falten")))
    }

    @Test
    fun `testDELETEAnulacionLogica204`() {
        val tokenSupervisor = obtenerToken("supervisor1")
        val pesaje = pesajeRepository.findAll().first { it.estado == EstadoPesajeTipo.REGISTRADO && it.ruta?.estado?.name == "ABIERTA" }

        mockMvc.perform(
            delete("/api/v1/pesajes/${pesaje.id}")
                .header("Authorization", "Bearer $tokenSupervisor")
                .param("motivo", "Tara erronea verificada en planta")
        )
            .andExpect(status().isNoContent)

        // Verificar que NO se eliminó físicamente de la base
        val entidadEnDb = pesajeRepository.findById(pesaje.id!!).get()
        assertEquals(EstadoPesajeTipo.ANULADO, entidadEnDb.estado)
        assertEquals("Tara erronea verificada en planta", entidadEnDb.motivoAnulacion)
        assertEquals("supervisor1", entidadEnDb.anuladoPor)
    }

    @Test
    fun `testIdempotenciaPesajeNoDuplicaFilas`() {
        val tokenOperario = obtenerToken("operario1")
        val punto = puntoAcopioRepository.findAll().first()
        val opId = "idempotencia-key-12345"

        val req = PesajeRequest(
            puntoId = punto.id,
            material = "Plástico Polietileno",
            pesoBruto = 150.0,
            tara = 15.0,
            clientOperationId = opId
        )

        val mvc1 = mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenOperario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isCreated).andReturn()

        val resp1 = objectMapper.readValue(mvc1.response.contentAsString, PesajeDto::class.java)

        // Segunda llamada idéntica
        val mvc2 = mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenOperario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        ).andReturn()

        val resp2 = objectMapper.readValue(mvc2.response.contentAsString, PesajeDto::class.java)

        assertEquals(resp1.id, resp2.id)
        assertEquals(resp1.pesoNeto, resp2.pesoNeto)
    }

    @Test
    fun `testPaginacionEnvoltorioDevuelveEstructuraExacta`() {
        val tokenOperario = obtenerToken("operario1")

        mockMvc.perform(
            get("/api/v1/puntos?pagina=0&tamanio=5")
                .header("Authorization", "Bearer $tokenOperario")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contenido").isArray)
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamanio").value(5))
            .andExpect(jsonPath("$.totalElementos").isNumber)
            .andExpect(jsonPath("$.totalPaginas").isNumber)
            .andExpect(jsonPath("$.ultima").isBoolean)
    }

    @Test
    fun `17 SC-B Rol AUDITOR permite todas las lecturas y deniega todas las escrituras con 403`() {
        val loginReq = LoginRequest("auditor1", "Eco2026*")
        val loginRes = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isString)
            .andExpect(jsonPath("$.rol").value("AUDITOR"))
            .andExpect(jsonPath("$.privilegios").isArray)
            .andReturn()

        val tokenAuditor = objectMapper.readTree(loginRes.response.contentAsString).get("token").asText()

        // 1. Lecturas permitidas (200 OK)
        mockMvc.perform(get("/api/v1/rutas/hoy").header("Authorization", "Bearer $tokenAuditor"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/puntos").header("Authorization", "Bearer $tokenAuditor"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/puntos/1/pesajes").header("Authorization", "Bearer $tokenAuditor"))
            .andExpect(status().isOk)

        // 2. Escrituras denegadas (403 Forbidden)
        val crearPesajeReq = PesajeRequest(
            puntoId = 1L,
            material = "Metal",
            pesoBruto = 100.0,
            tara = 10.0
        )
        mockMvc.perform(
            post("/api/v1/pesajes")
                .header("Authorization", "Bearer $tokenAuditor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crearPesajeReq))
        )
            .andExpect(status().isForbidden)

        mockMvc.perform(
            delete("/api/v1/pesajes/1?motivo=IntentoAuditor")
                .header("Authorization", "Bearer $tokenAuditor")
        )
            .andExpect(status().isForbidden)

        mockMvc.perform(
            post("/api/v1/rutas/1/cerrar")
                .header("Authorization", "Bearer $tokenAuditor")
        )
            .andExpect(status().isForbidden)
    }
}
