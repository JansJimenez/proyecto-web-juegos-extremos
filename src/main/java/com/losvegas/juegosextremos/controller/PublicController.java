package com.losvegas.juegosextremos.controller;

import com.losvegas.juegosextremos.config.FirebaseConfig;
import com.losvegas.juegosextremos.model.Atraccion;
import com.losvegas.juegosextremos.model.ConfiguracionParque;
import com.losvegas.juegosextremos.model.Paquete;
import com.losvegas.juegosextremos.service.AtraccionService;
import com.losvegas.juegosextremos.service.ConfiguracionService;
import com.losvegas.juegosextremos.service.PaqueteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controlador principal para las vistas públicas (SSR con Thymeleaf).
 * Alimenta home.html con el estado climático en vivo de Huayobamba, el catálogo de atracciones y combos.
 */
@Controller
@RequiredArgsConstructor
public class PublicController {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private final AtraccionService atraccionService;
    private final PaqueteService paqueteService;
    private final ConfiguracionService configuracionService;
    private final FirebaseConfig firebaseConfig;

    @Value("${app.parque.whatsapp:51953220851}")
    private String whatsappOficial;

    @Value("${app.parque.telefonoDisplay:953 220 851}")
    private String telefonoDisplay;

    @GetMapping({"/", "/home"})
    public String mostrarHome(Model model) {
        ConfiguracionParque configParque = configuracionService.obtenerConfiguracionActual();
        List<Atraccion> atracciones = atraccionService.listarActivas();
        List<Paquete> paquetes = paqueteService.listarActivos();

        model.addAttribute("configParque", configParque);
        model.addAttribute("atracciones", atracciones);
        model.addAttribute("paquetes", paquetes);
        model.addAttribute("fechaMinima", LocalDate.now(ZONA_PERU).toString());
        model.addAttribute("whatsappOficial", whatsappOficial);
        model.addAttribute("telefonoDisplay", telefonoDisplay);
        model.addAttribute("firebaseConectado", firebaseConfig.isFirebaseConectado());

        return "home";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    /**
     * Soporte adicional para cierre de sesión vía GET /logout por si el usuario recarga la URL /logout en el navegador.
     */
    @GetMapping("/logout")
    public String cerrarSesionGet(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    /**
     * Endpoint de verificación para Google Search Console (google9a360452cd0d0137.html).
     */
    @GetMapping(value = "/google9a360452cd0d0137.html", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> googleSiteVerification() {
        return ResponseEntity.ok("google-site-verification: google9a360452cd0d0137.html");
    }

    /**
     * Endpoint ligero de Keep-Alive para Render / cron-job.org.
     * No realiza lecturas a Firebase Firestore para no consumir cuota gratuita.
     */
    @GetMapping({"/health", "/ping"})
    @ResponseBody
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Juegos Extremos Los Vegas - Huayobamba, San Marcos"
        ));
    }
}
