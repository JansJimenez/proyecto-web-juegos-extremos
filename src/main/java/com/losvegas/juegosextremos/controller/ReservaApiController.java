package com.losvegas.juegosextremos.controller;

import com.losvegas.juegosextremos.dto.ReservaRequestDTO;
import com.losvegas.juegosextremos.dto.ReservaResponseDTO;
import com.losvegas.juegosextremos.model.ConfiguracionParque;
import com.losvegas.juegosextremos.service.ConfiguracionService;
import com.losvegas.juegosextremos.service.ReservaService;
import jakarta.validation.Valid;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el Simulador Inteligente de Reservas y consulta en vivo del estado del parque.
 * Nota: La ruta /api/** tiene CSRF ignorado en SecurityConfig para permitir llamadas fetch asíncronas.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservaApiController {

    private final ReservaService reservaService;
    private final ConfiguracionService configuracionService;

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponseDTO> crearPreReserva(
            @Valid @RequestBody ReservaRequestDTO request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errores = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .distinct()
                    .collect(Collectors.joining(" | "));

            log.warn("Solicitud de reserva inválida: {}", errores);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ReservaResponseDTO.builder()
                            .exito(false)
                            .mensajeConfirmacion(errores)
                            .build());
        }

        try {
            ReservaResponseDTO response = reservaService.procesarPreReserva(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error de validación de negocio en reserva: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ReservaResponseDTO.builder()
                            .exito(false)
                            .mensajeConfirmacion(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error inesperado al procesar la pre-reserva", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ReservaResponseDTO.builder()
                            .exito(false)
                            .mensajeConfirmacion("Ocurrió un inconveniente temporal al procesar tu reserva. Por favor intenta nuevamente o escríbenos directo al WhatsApp 953 220 851.")
                            .build());
        }
    }

    @GetMapping("/estado-parque")
    public ResponseEntity<ConfiguracionParque> obtenerEstadoParqueEnVivo() {
        return ResponseEntity.ok(configuracionService.obtenerConfiguracionActual());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReservaResponseDTO> manejarJsonMalformado(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ReservaResponseDTO.builder()
                        .exito(false)
                        .mensajeConfirmacion("El formato de los datos enviados no es válido. Verifica los campos del simulador.")
                        .build());
    }
}
