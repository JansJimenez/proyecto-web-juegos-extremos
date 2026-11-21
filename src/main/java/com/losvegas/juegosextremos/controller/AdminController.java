package com.losvegas.juegosextremos.controller;

import com.losvegas.juegosextremos.config.FirebaseConfig;
import com.losvegas.juegosextremos.dto.EstadoParqueDTO;
import com.losvegas.juegosextremos.model.Atraccion;
import com.losvegas.juegosextremos.model.ConfiguracionParque;
import com.losvegas.juegosextremos.model.Paquete;
import com.losvegas.juegosextremos.model.Reserva;
import com.losvegas.juegosextremos.service.AtraccionService;
import com.losvegas.juegosextremos.service.ConfiguracionService;
import com.losvegas.juegosextremos.service.PaqueteService;
import com.losvegas.juegosextremos.service.ReservaService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador del Panel de Administración (Backoffice /admin) para el propietario de Juegos Extremos Los Vegas.
 * Protegido mediante Spring Security (rol ADMIN).
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AtraccionService atraccionService;
    private final PaqueteService paqueteService;
    private final ReservaService reservaService;
    private final ConfiguracionService configuracionService;
    private final FirebaseConfig firebaseConfig;

    @GetMapping({"", "/"})
    public String mostrarDashboard(Model model) {
        ConfiguracionParque configParque = configuracionService.obtenerConfiguracionActual();
        List<Atraccion> atracciones = atraccionService.listarTodas();
        List<Paquete> paquetes = paqueteService.listarTodos();
        List<Reserva> reservas = reservaService.listarTodas();

        // Cálculo de KPIs operativos en Soles (S/)
        long totalReservas = reservas.size();
        long reservasPendientes = reservas.stream()
                .filter(r -> "PENDIENTE_WHATSAPP".equalsIgnoreCase(r.getEstado()))
                .count();
        long reservasConfirmadas = reservas.stream()
                .filter(r -> "CONFIRMADA".equalsIgnoreCase(r.getEstado()))
                .count();
        long reservasCompletadas = reservas.stream()
                .filter(r -> "COMPLETADA".equalsIgnoreCase(r.getEstado()))
                .count();
        long reservasCanceladas = reservas.stream()
                .filter(r -> "CANCELADA".equalsIgnoreCase(r.getEstado()))
                .count();

        int visitantesEsperados = reservas.stream()
                .filter(r -> !"CANCELADA".equalsIgnoreCase(r.getEstado()))
                .mapToInt(r -> r.getCantidadPersonas() != null ? r.getCantidadPersonas() : 0)
                .sum();

        double ingresosEstimadosSoles = reservas.stream()
                .filter(r -> !"CANCELADA".equalsIgnoreCase(r.getEstado()))
                .mapToDouble(r -> r.getMontoTotalSoles() != null ? r.getMontoTotalSoles() : 0.0)
                .sum();

        double ingresosConfirmadosSoles = reservas.stream()
                .filter(r -> "CONFIRMADA".equalsIgnoreCase(r.getEstado()) || "COMPLETADA".equalsIgnoreCase(r.getEstado()))
                .mapToDouble(r -> r.getMontoTotalSoles() != null ? r.getMontoTotalSoles() : 0.0)
                .sum();

        double ingresosPendientesSoles = Math.max(0.0, ingresosEstimadosSoles - ingresosConfirmadosSoles);

        model.addAttribute("configParque", configParque);
        model.addAttribute("atracciones", atracciones);
        model.addAttribute("paquetes", paquetes);
        model.addAttribute("reservas", reservas);
        model.addAttribute("firebaseConectado", firebaseConfig.isFirebaseConectado());

        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("reservasPendientes", reservasPendientes);
        model.addAttribute("reservasConfirmadas", reservasConfirmadas);
        model.addAttribute("reservasCompletadas", reservasCompletadas);
        model.addAttribute("reservasCanceladas", reservasCanceladas);
        model.addAttribute("visitantesEsperados", visitantesEsperados);
        model.addAttribute("ingresosEstimadosSoles", ingresosEstimadosSoles);
        model.addAttribute("ingresosConfirmadosSoles", ingresosConfirmadosSoles);
        model.addAttribute("ingresosPendientesSoles", ingresosPendientesSoles);

        return "admin/dashboard";
    }

    // =========================================================================
    // 1. CONTROL DE CLIMA Y BANNER EN VIVO (1 CLIC Y PERSONALIZADO)
    // =========================================================================

    @PostMapping("/clima/rapido")
    public String cambiarClimaRapido(
            @RequestParam("estadoOperativo") String estadoOperativo,
            RedirectAttributes redirectAttributes) {

        ConfiguracionParque actualizado = configuracionService.actualizarEstadoRapido(estadoOperativo);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "¡Estado de Huayobamba actualizado en vivo a: " + actualizado.getEstadoOperativo() + " (" + actualizado.getTemperaturaActual() + ")!");
        return "redirect:/admin";
    }

    @PostMapping("/clima/personalizado")
    public String actualizarBannerPersonalizado(
            @ModelAttribute EstadoParqueDTO dto,
            @RequestParam(value = "horarioAtencion", required = false) String horarioAtencion,
            RedirectAttributes redirectAttributes) {

        if (dto.getPromoFinDeSemanaActiva() == null) {
            dto.setPromoFinDeSemanaActiva(false);
        }
        ConfiguracionParque config = configuracionService.actualizarConfiguracionPersonalizada(dto);
        if (horarioAtencion != null && !horarioAtencion.isBlank()) {
            config.setHorarioAtencion(horarioAtencion.trim());
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "¡Mensaje del banner y promoción actualizados correctamente!");
        return "redirect:/admin";
    }

    // =========================================================================
    // 2. GESTIÓN DE RESERVAS RECIBIDAS
    // =========================================================================

    @PostMapping("/reservas/{id}/estado")
    public String cambiarEstadoReserva(
            @PathVariable("id") String id,
            @RequestParam("estado") String nuevoEstado,
            RedirectAttributes redirectAttributes) {

        reservaService.actualizarEstadoReserva(id, nuevoEstado);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "El estado de la reserva " + id + " fue actualizado a " + nuevoEstado + ".");
        return "redirect:/admin#seccion-reservas";
    }

    // =========================================================================
    // 3. CRUD DE ATRACCIONES (JUEGOS EXTREMOS)
    // =========================================================================

    @PostMapping("/atracciones/guardar")
    public String guardarAtraccion(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcionCorta") String descripcionCorta,
            @RequestParam(value = "descripcionDetallada", required = false) String descripcionDetallada,
            @RequestParam("precio") Double precio,
            @RequestParam("nivelAdrenalina") String nivelAdrenalina,
            @RequestParam(value = "alturaMetros", defaultValue = "50") Integer alturaMetros,
            @RequestParam(value = "pesoMinKg", defaultValue = "30") Integer pesoMinKg,
            @RequestParam(value = "pesoMaxKg", defaultValue = "100") Integer pesoMaxKg,
            @RequestParam(value = "edadMinima", defaultValue = "8") Integer edadMinima,
            @RequestParam(value = "duracionEstimadaMin", defaultValue = "15") Integer duracionEstimadaMin,
            @RequestParam(value = "imagenUrl", required = false) String imagenUrl,
            @RequestParam(value = "iconoBadge", defaultValue = "fa-bolt") String iconoBadge,
            @RequestParam(value = "equipoTexto", required = false) String equipoTexto,
            @RequestParam(value = "orden", defaultValue = "1") Integer orden,
            @RequestParam(value = "activo", defaultValue = "false") Boolean activo,
            RedirectAttributes redirectAttributes) {

        List<String> equipos = (equipoTexto != null && !equipoTexto.isBlank())
                ? Arrays.stream(equipoTexto.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList())
                : List.of("Arnés de seguridad", "Casco protector", "Guantes de protección", "Orientación del guía");

        String urlFinal = (imagenUrl != null && !imagenUrl.isBlank())
                ? imagenUrl.trim()
                : "/images/puente-extremo-portal.jpg";

        Atraccion atraccion = Atraccion.builder()
                .id(id != null && !id.isBlank() ? id.trim() : null)
                .nombre(nombre.trim())
                .descripcionCorta(descripcionCorta.trim())
                .descripcionDetallada(descripcionDetallada != null && !descripcionDetallada.isBlank() ? descripcionDetallada.trim() : descripcionCorta.trim())
                .precio(precio)
                .nivelAdrenalina(nivelAdrenalina)
                .alturaMetros(alturaMetros)
                .pesoMinKg(pesoMinKg)
                .pesoMaxKg(pesoMaxKg)
                .edadMinima(edadMinima)
                .duracionEstimadaMin(duracionEstimadaMin)
                .imagenUrl(urlFinal)
                .iconoBadge(iconoBadge != null && !iconoBadge.isBlank() ? iconoBadge.trim() : "fa-bolt")
                .incluyeEquipo(equipos)
                .orden(orden)
                .activo(activo)
                .build();

        Atraccion guardada = atraccionService.guardar(atraccion);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Atracción '" + guardada.getNombre() + "' (S/ " + String.format("%.2f", guardada.getPrecio()) + ") guardada con éxito.");
        return "redirect:/admin#seccion-juegos";
    }

    @PostMapping("/atracciones/{id}/toggle")
    public String alternarEstadoAtraccion(
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {

        atraccionService.obtenerPorId(id).ifPresent(a -> {
            boolean nuevoEstado = !Boolean.TRUE.equals(a.getActivo());
            atraccionService.cambiarEstadoActivo(id, nuevoEstado);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Juego '" + a.getNombre() + "' cambiado a: " + (nuevoEstado ? "ACTIVO EN CATÁLOGO" : "PAUSADO / MANTENIMIENTO"));
        });
        return "redirect:/admin#seccion-juegos";
    }

    @PostMapping("/atracciones/{id}/eliminar")
    public String eliminarAtraccion(
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {

        atraccionService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Atracción eliminada del catálogo.");
        return "redirect:/admin#seccion-juegos";
    }

    // =========================================================================
    // 4. CRUD DE PAQUETES Y COMBOS PROMOCIONALES
    // =========================================================================

    @PostMapping("/paquetes/guardar")
    public String guardarPaquete(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "tipoPublico", defaultValue = "INDIVIDUAL") String tipoPublico,
            @RequestParam("atraccionesTexto") String atraccionesTexto,
            @RequestParam("precioRegularTotal") Double precioRegularTotal,
            @RequestParam("precioOferta") Double precioOferta,
            @RequestParam(value = "minimoPersonas", defaultValue = "1") Integer minimoPersonas,
            @RequestParam(value = "etiquetaDestacada", defaultValue = "🔥 PROMOCIÓN") String etiquetaDestacada,
            @RequestParam(value = "orden", defaultValue = "1") Integer orden,
            @RequestParam(value = "activo", defaultValue = "false") Boolean activo,
            RedirectAttributes redirectAttributes) {

        List<String> nombresJuegos = Arrays.stream(atraccionesTexto.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        Paquete paquete = Paquete.builder()
                .id(id != null && !id.isBlank() ? id.trim() : null)
                .nombre(nombre.trim())
                .descripcion(descripcion.trim())
                .tipoPublico(tipoPublico)
                .atraccionesIncluidasIds(List.of())
                .atraccionesIncluidasNombres(nombresJuegos)
                .precioRegularTotal(precioRegularTotal)
                .precioOferta(precioOferta)
                .minimoPersonas(minimoPersonas)
                .etiquetaDestacada(etiquetaDestacada != null && !etiquetaDestacada.isBlank() ? etiquetaDestacada.trim() : "🔥 COMBO ESPECIAL")
                .orden(orden)
                .activo(activo)
                .build();

        Paquete guardado = paqueteService.guardar(paquete);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Combo '" + guardado.getNombre() + "' (Oferta S/ " + String.format("%.2f", guardado.getPrecioOferta()) + ") guardado con éxito.");
        return "redirect:/admin#seccion-combos";
    }

    @PostMapping("/paquetes/{id}/toggle")
    public String alternarEstadoPaquete(
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {

        paqueteService.obtenerPorId(id).ifPresent(p -> {
            boolean nuevoEstado = !Boolean.TRUE.equals(p.getActivo());
            paqueteService.cambiarEstadoActivo(id, nuevoEstado);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Combo '" + p.getNombre() + "' cambiado a: " + (nuevoEstado ? "ACTIVO" : "PAUSADO"));
        });
        return "redirect:/admin#seccion-combos";
    }

    @PostMapping("/paquetes/{id}/eliminar")
    public String eliminarPaquete(
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {

        paqueteService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Combo eliminado correctamente.");
        return "redirect:/admin#seccion-combos";
    }
}
