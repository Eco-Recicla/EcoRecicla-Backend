package pe.upeu.ecorecicla.servicio

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.seguridad.Rol
import pe.upeu.ecorecicla.seguridad.Usuario
import pe.upeu.ecorecicla.seguridad.UsuarioRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class DataInitializer(
    private val usuarioRepository: UsuarioRepository,
    private val puntoAcopioRepository: PuntoAcopioRepository,
    private val rutaRepository: RutaRepository,
    private val rutaPuntoRepository: RutaPuntoRepository,
    private val pesajeRepository: PesajeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String?) {
        if (usuarioRepository.count() > 0) {
            return // Datos ya inicializados previamente de forma persistente
        }

        // 1. Usuarios obligatorios y de prueba
        val passHash = passwordEncoder.encode("Eco2026*")

        val operario1 = usuarioRepository.save(
            Usuario(
                username = "operario1",
                passwordHash = passHash,
                nombreCompleto = "Carlos Mendoza (Operario)",
                rol = Rol.OPERARIO,
                activo = true
            )
        )

        val supervisor1 = usuarioRepository.save(
            Usuario(
                username = "supervisor1",
                passwordHash = passHash,
                nombreCompleto = "Ana Ramos (Supervisora)",
                rol = Rol.SUPERVISOR,
                activo = true
            )
        )

        usuarioRepository.save(
            Usuario(
                username = "admin",
                passwordHash = passHash,
                nombreCompleto = "Administrador del Sistema",
                rol = Rol.ADMINISTRADOR,
                activo = true
            )
        )

        val operario2 = usuarioRepository.save(
            Usuario(
                username = "operario2",
                passwordHash = passHash,
                nombreCompleto = "Luis Fernandez (Operario 2)",
                rol = Rol.OPERARIO,
                activo = true
            )
        )

        // 2. 12 Puntos de acopio en 3 zonas (Norte, Sur, Centro)
        val puntosDef = listOf(
            Triple("EcoPlásticos Lima Norte", "Av. Gerardo Unger 3450, Independencia", "Norte" to "Plástico"),
            Triple("Metales Los Olivos", "Av. Alfredo Mendiola 6200, Los Olivos", "Norte" to "Metal"),
            Triple("Cartones San Martín", "Av. Perú 2100, SMP", "Norte" to "Cartón"),
            Triple("Vidriería Carabayllo", "Av. Túpac Amaru 4500, Carabayllo", "Norte" to "Vidrio"),
            Triple("Plásticos Chorrillos", "Av. Defensores del Morro 1200, Chorrillos", "Sur" to "Plástico"),
            Triple("Recicladora Surco", "Av. Santiago de Surco 340, Surco", "Sur" to "Papel"),
            Triple("Metales Villa El Salvador", "Av. Pachacútec 5600, VES", "Sur" to "Metal"),
            Triple("Cartones Miraflores", "Av. Benavides 1850, Miraflores", "Sur" to "Cartón"),
            Triple("Centro Histórico Vidrios", "Jr. Lampa 450, Cercado de Lima", "Centro" to "Vidrio"),
            Triple("Breña Papeles y Cartones", "Av. Arica 890, Breña", "Centro" to "Cartón"),
            Triple("La Victoria Metales", "Av. 28 de Julio 1900, La Victoria", "Centro" to "Metal"),
            Triple("Lince Plásticos Limpios", "Av. Petit Thouars 2100, Lince", "Centro" to "Plástico")
        )

        val puntosGuardados = puntosDef.map { (razon, dir, zonaMat) ->
            puntoAcopioRepository.save(
                PuntoAcopioEntity(
                    razonSocial = razon,
                    direccion = dir,
                    zona = zonaMat.first,
                    materialHabitual = zonaMat.second,
                    activo = true
                )
            )
        }

        val hoy = LocalDate.now(clock)
        val ayer = hoy.minusDays(1)

        // 3. Ruta de ayer (CERRADA) con 6 puntos y pesajes
        val rutaAyer = rutaRepository.save(
            RutaEntity(
                fecha = ayer,
                estado = EstadoRuta.CERRADA,
                usuarioAsignado = operario1,
                fechaCierre = LocalDateTime.now(clock).minusDays(1).withHour(18).withMinute(30),
                cerradaPor = supervisor1.username
            )
        )

        // Asignar puntos 7 al 12 a la ruta de ayer
        val puntosRutaAyer = puntosGuardados.subList(6, 12)
        puntosRutaAyer.forEachIndexed { index, punto ->
            rutaPuntoRepository.save(
                RutaPuntoEntity(
                    ruta = rutaAyer,
                    punto = punto,
                    secuencia = index + 1,
                    atendido = true,
                    fechaAtencion = LocalDateTime.now(clock).minusDays(1).withHour(10 + index)
                )
            )

            // Crear pesajes de ayer (10 pesajes en total)
            pesajeRepository.save(
                PesajeEntity(
                    punto = punto,
                    ruta = rutaAyer,
                    fechaHora = LocalDateTime.now(clock).minusDays(1).withHour(10 + index),
                    material = punto.materialHabitual,
                    pesoBruto = 120.0 + (index * 15.0),
                    tara = 12.0 + index,
                    pesoNeto = (120.0 + (index * 15.0)) - (12.0 + index),
                    operario = operario1.username,
                    estado = EstadoPesajeTipo.REGISTRADO
                )
            )
            if (index < 4) {
                pesajeRepository.save(
                    PesajeEntity(
                        punto = punto,
                        ruta = rutaAyer,
                        fechaHora = LocalDateTime.now(clock).minusDays(1).withHour(10 + index).withMinute(20),
                        material = punto.materialHabitual,
                        pesoBruto = 85.0 + (index * 10.0),
                        tara = 8.5 + index,
                        pesoNeto = (85.0 + (index * 10.0)) - (8.5 + index),
                        operario = operario1.username,
                        estado = EstadoPesajeTipo.REGISTRADO
                    )
                )
            }
        }

        // 4. Ruta de hoy (ABIERTA) con 6 puntos asignados a operario1
        val rutaHoy = rutaRepository.save(
            RutaEntity(
                fecha = hoy,
                estado = EstadoRuta.ABIERTA,
                usuarioAsignado = operario1
            )
        )

        val puntosRutaHoy = puntosGuardados.subList(0, 6)
        val rutaPuntosHoy = puntosRutaHoy.mapIndexed { index, punto ->
            val atendido = index < 3 // primeros 3 puntos atendidos, puntos 4, 5 y 6 pendientes de pesar
            rutaPuntoRepository.save(
                RutaPuntoEntity(
                    ruta = rutaHoy,
                    punto = punto,
                    secuencia = index + 1,
                    atendido = atendido,
                    fechaAtencion = if (atendido) LocalDateTime.now(clock).minusHours(3 - index.toLong()) else null
                )
            )
        }

        // Pesajes de hoy:
        // Pesaje 1: operario1 en punto 1 (REGISTRADO)
        pesajeRepository.save(
            PesajeEntity(
                punto = puntosRutaHoy[0],
                ruta = rutaHoy,
                fechaHora = LocalDateTime.now(clock).minusHours(3),
                material = "Plástico PET",
                pesoBruto = 160.0,
                tara = 15.0,
                pesoNeto = 145.0,
                operario = operario1.username,
                estado = EstadoPesajeTipo.REGISTRADO
            )
        )

        // Pesaje 2: operario2 en punto 2 (REGISTRADO) -> Demuestra RN-02: pesaje de otro operario
        pesajeRepository.save(
            PesajeEntity(
                punto = puntosRutaHoy[1],
                ruta = rutaHoy,
                fechaHora = LocalDateTime.now(clock).minusHours(2),
                material = "Metal Chatarra",
                pesoBruto = 210.0,
                tara = 20.0,
                pesoNeto = 190.0,
                operario = operario2.username,
                estado = EstadoPesajeTipo.REGISTRADO
            )
        )

        // Pesaje 3: operario1 en punto 3 ANULADO -> Demuestra anulación lógica con motivo
        pesajeRepository.save(
            PesajeEntity(
                punto = puntosRutaHoy[2],
                ruta = rutaHoy,
                fechaHora = LocalDateTime.now(clock).minusHours(2).withMinute(10),
                material = "Cartón Corrugado",
                pesoBruto = 95.0,
                tara = 10.0,
                pesoNeto = 85.0,
                operario = operario1.username,
                estado = EstadoPesajeTipo.ANULADO,
                motivoAnulacion = "Balanza descalibrada reportada por supervisor",
                anuladoPor = supervisor1.username,
                fechaAnulacion = LocalDateTime.now(clock).minusHours(1).withMinute(45)
            )
        )

        // Pesaje 4: operario1 en punto 3 REGISTRADO (corrección / pesaje válido posterior)
        pesajeRepository.save(
            PesajeEntity(
                punto = puntosRutaHoy[2],
                ruta = rutaHoy,
                fechaHora = LocalDateTime.now(clock).minusHours(1).withMinute(30),
                material = "Cartón Corrugado",
                pesoBruto = 98.0,
                tara = 12.0,
                pesoNeto = 86.0,
                operario = operario1.username,
                estado = EstadoPesajeTipo.REGISTRADO
            )
        )

        // Pesaje 5 adicional de operario1 para completar más de 15 pesajes
        pesajeRepository.save(
            PesajeEntity(
                punto = puntosRutaHoy[0],
                ruta = rutaHoy,
                fechaHora = LocalDateTime.now(clock).minusMinutes(40),
                material = "Plástico Film",
                pesoBruto = 75.0,
                tara = 7.0,
                pesoNeto = 68.0,
                operario = operario1.username,
                estado = EstadoPesajeTipo.REGISTRADO
            )
        )
    }
}
