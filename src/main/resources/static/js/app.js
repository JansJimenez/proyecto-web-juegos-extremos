/**
 * JUEGOS EXTREMOS "LOS VEGAS" — HUAYOBAMBA, SAN MARCOS (CAJAMARCA)
 * Interacciones Editoriales: Planificador de Visita, Lightbox de Galería Real y Animaciones Scroll
 */

document.addEventListener('DOMContentLoaded', () => {
    const carrito = new Map();

    // Referencias DOM
    const cartItemsContainer = document.getElementById('cartItemsContainer');
    const simuladorTotalSoles = document.getElementById('simuladorTotalSoles');
    const simuladorAhorroBox = document.getElementById('simuladorAhorroBox');
    const simuladorAhorroMonto = document.getElementById('simuladorAhorroMonto');
    const navCartCount = document.getElementById('navCartCount');
    const mobileStickyCart = document.getElementById('mobileStickyCart');
    const stickyItemCount = document.getElementById('stickyItemCount');
    const stickyTotalPrice = document.getElementById('stickyTotalPrice');
    const btnClearCart = document.getElementById('btnClearCart');

    const reservaForm = document.getElementById('reservaForm');
    const simulatorAlert = document.getElementById('simulatorAlert');
    const btnSubmitReserva = document.getElementById('btnSubmitReserva');
    const inputCantidadPersonas = document.getElementById('cantidadPersonas');

    // Modal de confirmación
    const confirmacionModal = document.getElementById('confirmacionModal');
    const modalCodigoReserva = document.getElementById('modalCodigoReserva');
    const modalMontoTotal = document.getElementById('modalMontoTotal');
    const modalWhatsappLink = document.getElementById('modalWhatsappLink');
    const btnCloseModal = document.getElementById('btnCloseModal');

    // Lightbox de Galería Real
    const photoLightbox = document.getElementById('photoLightbox');
    const lightboxImage = document.getElementById('lightboxImage');
    const lightboxCaption = document.getElementById('lightboxCaption');
    const btnCloseLightbox = document.getElementById('btnCloseLightbox');

    // =========================================================================
    // 1. MENÚ DESPLEGABLE DE TRES RAYITAS (LAPTOP Y CELULAR)
    // =========================================================================
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const mobileDrawer = document.getElementById('mobileDrawer');
    const menuToggleIcon = document.getElementById('menuToggleIcon');

    function cerrarMenuDesplegable() {
        if (!mobileDrawer || !mobileMenuToggle) return;
        mobileDrawer.classList.remove('open');
        mobileMenuToggle.classList.remove('active');
        mobileMenuToggle.setAttribute('aria-expanded', 'false');
        if (menuToggleIcon) {
            menuToggleIcon.className = 'fa-solid fa-bars';
        }
    }

    if (mobileMenuToggle && mobileDrawer) {
        mobileMenuToggle.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = mobileDrawer.classList.toggle('open');
            mobileMenuToggle.classList.toggle('active', isOpen);
            mobileMenuToggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
            if (menuToggleIcon) {
                menuToggleIcon.className = isOpen ? 'fa-solid fa-xmark' : 'fa-solid fa-bars';
            }
        });

        mobileDrawer.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                cerrarMenuDesplegable();
            });
        });

        document.addEventListener('click', (e) => {
            if (mobileDrawer.classList.contains('open') &&
                !mobileDrawer.contains(e.target) &&
                !mobileMenuToggle.contains(e.target)) {
                cerrarMenuDesplegable();
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                cerrarMenuDesplegable();
            }
        });
    }

    // =========================================================================
    // 2. TABS DEL SELECTOR RÁPIDO EN EL PLANIFICADOR
    // =========================================================================
    const tabJuegosBtn = document.getElementById('tabJuegosBtn');
    const tabCombosBtn = document.getElementById('tabCombosBtn');
    const quickListJuegos = document.getElementById('quickListJuegos');
    const quickListCombos = document.getElementById('quickListCombos');

    if (tabJuegosBtn && tabCombosBtn && quickListJuegos && quickListCombos) {
        tabJuegosBtn.addEventListener('click', () => {
            tabJuegosBtn.classList.add('active');
            tabCombosBtn.classList.remove('active');
            quickListJuegos.style.display = 'grid';
            quickListCombos.style.display = 'none';
        });

        tabCombosBtn.addEventListener('click', () => {
            tabCombosBtn.classList.add('active');
            tabJuegosBtn.classList.remove('active');
            quickListCombos.style.display = 'grid';
            quickListJuegos.style.display = 'none';
        });
    }

    // =========================================================================
    // 3. AGREGAR ÍTEMS AL PLANIFICADOR
    // =========================================================================
    document.querySelectorAll('.js-add-to-cart').forEach(button => {
        button.addEventListener('click', () => {
            const id = button.getAttribute('data-id');
            const nombre = button.getAttribute('data-nombre');
            const precio = parseFloat(button.getAttribute('data-precio') || '0');
            const tipo = button.getAttribute('data-tipo') || 'ATRACCION';
            const ahorro = parseFloat(button.getAttribute('data-ahorro') || '0');
            const minPersonas = parseInt(button.getAttribute('data-min-personas') || '1', 10);

            agregarItemAlCarrito({
                itemId: id,
                nombre: nombre,
                precioUnitario: precio,
                tipo: tipo,
                ahorroUnitario: ahorro,
                minPersonas: minPersonas
            });

            if (inputCantidadPersonas && minPersonas > parseInt(inputCantidadPersonas.value || '1', 10)) {
                inputCantidadPersonas.value = minPersonas;
            }

            const originalHtml = button.innerHTML;
            button.classList.add('added-pulse');
            button.innerHTML = '<i class="fa-solid fa-check"></i> Agregado';
            setTimeout(() => {
                button.classList.remove('added-pulse');
                button.innerHTML = originalHtml;
            }, 1100);
        });
    });

    if (btnClearCart) {
        btnClearCart.addEventListener('click', () => {
            carrito.clear();
            renderizarCarrito();
        });
    }

    function agregarItemAlCarrito(item) {
        if (!item.itemId) return;

        if (carrito.has(item.itemId)) {
            const existente = carrito.get(item.itemId);
            existente.cantidad += 1;
            carrito.set(item.itemId, existente);
        } else {
            carrito.set(item.itemId, {
                itemId: item.itemId,
                nombre: item.nombre,
                precioUnitario: item.precioUnitario,
                tipo: item.tipo,
                ahorroUnitario: item.ahorroUnitario || 0,
                cantidad: 1
            });
        }

        ocultarAlertaError();
        renderizarCarrito();
    }

    function modificarCantidadItem(itemId, delta) {
        if (!carrito.has(itemId)) return;
        const item = carrito.get(itemId);
        item.cantidad += delta;

        if (item.cantidad <= 0) {
            carrito.delete(itemId);
        } else {
            carrito.set(itemId, item);
        }
        renderizarCarrito();
    }

    function renderizarCarrito() {
        if (!cartItemsContainer) return;

        let totalUnidades = 0;
        let montoTotal = 0;
        let ahorroTotal = 0;

        if (carrito.size === 0) {
            cartItemsContainer.innerHTML = `
                <div class="empty-cart-box">
                    <i class="fa-regular fa-compass fa-2x" style="color: var(--brand-gold); margin-bottom: 0.5rem;"></i>
                    <p>Aún no has agregado pases. Toca <strong>"+ Agregar"</strong> arriba para calcular el monto de tu visita.</p>
                </div>
            `;
            if (btnClearCart) btnClearCart.style.display = 'none';
        } else {
            if (btnClearCart) btnClearCart.style.display = 'inline-flex';
            let html = '';

            carrito.forEach(item => {
                const subtotal = item.precioUnitario * item.cantidad;
                totalUnidades += item.cantidad;
                montoTotal += subtotal;
                ahorroTotal += (item.ahorroUnitario || 0) * item.cantidad;

                const etiqueta = item.tipo === 'PAQUETE'
                    ? '<span style="color: var(--brand-red); font-weight:700; font-size:0.75rem;">PASE</span>'
                    : '<span style="color: var(--brand-green); font-weight:700; font-size:0.75rem;">EXPERIENCIA</span>';

                const textoPrecio = item.precioUnitario <= 0
                    ? '<strong style="color: var(--brand-green);">GRATIS (Incluido)</strong>'
                    : `S/ ${item.precioUnitario.toFixed(2)} c/u &bull; Subtotal: <strong style="color: var(--ink-dark);">S/ ${subtotal.toFixed(2)}</strong>`;

                html += `
                    <div class="cart-line-item">
                        <div>
                            <span class="cart-line-title">${etiqueta} &bull; ${escaparHtml(item.nombre)}</span>
                            <span class="cart-line-meta">${textoPrecio}</span>
                        </div>
                        <div class="qty-controls">
                            <button type="button" class="qty-btn js-qty-btn" data-id="${item.itemId}" data-delta="-1" aria-label="Disminuir">-</button>
                            <span class="qty-number">${item.cantidad}</span>
                            <button type="button" class="qty-btn js-qty-btn" data-id="${item.itemId}" data-delta="1" aria-label="Aumentar">+</button>
                        </div>
                    </div>
                `;
            });

            cartItemsContainer.innerHTML = html;

            cartItemsContainer.querySelectorAll('.js-qty-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const id = btn.getAttribute('data-id');
                    const delta = parseInt(btn.getAttribute('data-delta'), 10);
                    modificarCantidadItem(id, delta);
                });
            });
        }

        if (simuladorTotalSoles) {
            simuladorTotalSoles.textContent = `S/ ${montoTotal.toFixed(2)}`;
        }
        if (simuladorAhorroBox && simuladorAhorroMonto) {
            if (ahorroTotal > 0) {
                simuladorAhorroBox.style.display = 'block';
                simuladorAhorroMonto.textContent = `S/ ${ahorroTotal.toFixed(2)}`;
            } else {
                simuladorAhorroBox.style.display = 'none';
            }
        }
        if (navCartCount) {
            navCartCount.textContent = totalUnidades.toString();
        }
        if (stickyItemCount) {
            stickyItemCount.textContent = `${totalUnidades} ${totalUnidades === 1 ? 'experiencia elegida' : 'experiencias elegidas'}`;
        }
        if (stickyTotalPrice) {
            stickyTotalPrice.textContent = `S/ ${montoTotal.toFixed(2)}`;
        }
        if (mobileStickyCart) {
            if (totalUnidades > 0) {
                mobileStickyCart.classList.add('visible');
            } else {
                mobileStickyCart.classList.remove('visible');
            }
        }
    }

    // =========================================================================
    // 4. ENVÍO ASÍNCRONO DE PRE-RESERVA (POST /api/reservas) Y WHATSAPP
    // =========================================================================
    if (reservaForm) {
        const inputFecha = document.getElementById('fechaVisita');
        if (inputFecha && !inputFecha.value) {
            const minDate = inputFecha.getAttribute('min');
            if (minDate) {
                inputFecha.value = minDate;
            }
        }

        reservaForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            ocultarAlertaError();

            if (carrito.size === 0) {
                mostrarAlertaError('Primero agrega al menos una experiencia (Puente Extremo, Tirolesa o Combo) en el Paso 1.');
                return;
            }

            const nombreCliente = document.getElementById('nombreCliente').value.trim();
            const telefonoCliente = document.getElementById('telefonoCliente').value.trim();
            const origenVisitante = document.getElementById('origenVisitante').value.trim();
            const fechaVisita = document.getElementById('fechaVisita').value.trim();
            const turnoPreferido = document.getElementById('turnoPreferido').value;
            const cantidadPersonas = parseInt(document.getElementById('cantidadPersonas').value || '1', 10);
            const comentariosAdicionales = document.getElementById('comentariosAdicionales').value.trim();

            if (!nombreCliente || !telefonoCliente || !fechaVisita) {
                mostrarAlertaError('Por favor completa tu nombre, número de celular y fecha de visita.');
                return;
            }

            const itemsPayload = [];
            carrito.forEach(item => {
                itemsPayload.push({
                    itemId: item.itemId,
                    tipo: item.tipo,
                    cantidad: item.cantidad
                });
            });

            const payload = {
                nombreCliente,
                telefonoCliente,
                origenVisitante,
                fechaVisita,
                turnoPreferido,
                cantidadPersonas,
                items: itemsPayload,
                comentariosAdicionales
            };

            const originalBtnHtml = btnSubmitReserva.innerHTML;
            btnSubmitReserva.disabled = true;
            btnSubmitReserva.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Preparando tu reserva...';

            try {
                const response = await fetch('/api/reservas', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (!response.ok || !data.exito) {
                    mostrarAlertaError(data.mensajeConfirmacion || 'Verifica los datos ingresados.');
                    btnSubmitReserva.disabled = false;
                    btnSubmitReserva.innerHTML = originalBtnHtml;
                    return;
                }

                if (modalCodigoReserva) modalCodigoReserva.textContent = data.codigoReserva;
                if (modalMontoTotal) modalMontoTotal.textContent = `S/ ${Number(data.montoTotalSoles).toFixed(2)}`;
                if (modalWhatsappLink) modalWhatsappLink.href = data.whatsappUrl;
                if (confirmacionModal) confirmacionModal.classList.add('active');

                carrito.clear();
                renderizarCarrito();
                reservaForm.reset();
                if (inputFecha && inputFecha.getAttribute('min')) {
                    inputFecha.value = inputFecha.getAttribute('min');
                }

                setTimeout(() => {
                    window.open(data.whatsappUrl, '_blank', 'noopener,noreferrer');
                }, 850);

            } catch (error) {
                mostrarAlertaError('No se pudo conectar con el servidor. Escríbenos directo al WhatsApp 953 220 851.');
            } finally {
                btnSubmitReserva.disabled = false;
                btnSubmitReserva.innerHTML = originalBtnHtml;
            }
        });
    }

    if (btnCloseModal && confirmacionModal) {
        btnCloseModal.addEventListener('click', () => {
            confirmacionModal.classList.remove('active');
        });
    }

    // =========================================================================
    // 5. VISOR LIGHTBOX DE GALERÍA FOTOGRÁFICA REAL
    // =========================================================================
    document.querySelectorAll('.js-lightbox-trigger').forEach(card => {
        card.addEventListener('click', () => {
            const fullSrc = card.getAttribute('data-full');
            const caption = card.getAttribute('data-caption') || '';
            if (photoLightbox && lightboxImage && fullSrc) {
                lightboxImage.src = fullSrc;
                if (lightboxCaption) lightboxCaption.textContent = caption;
                photoLightbox.classList.add('active');
            }
        });
    });

    if (btnCloseLightbox && photoLightbox) {
        btnCloseLightbox.addEventListener('click', () => {
            photoLightbox.classList.remove('active');
        });
        photoLightbox.addEventListener('click', (e) => {
            if (e.target === photoLightbox) {
                photoLightbox.classList.remove('active');
            }
        });
    }

    // =========================================================================
    // 6. ACORDEÓN DE PREGUNTAS FRECUENTES (FAQ)
    // =========================================================================
    document.querySelectorAll('.faq-question').forEach(questionBtn => {
        questionBtn.addEventListener('click', () => {
            const parentItem = questionBtn.closest('.faq-item');
            if (parentItem) {
                parentItem.classList.toggle('open');
            }
        });
    });

    // =========================================================================
    // 7. ANIMACIONES SCROLL-REVEAL CON INTERSECTION OBSERVER
    // =========================================================================
    const revealElements = document.querySelectorAll('.reveal-on-scroll');
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries, obs) => {
            entries.forEach((entry, index) => {
                if (entry.isIntersecting) {
                    setTimeout(() => {
                        entry.target.classList.add('is-visible');
                    }, (index % 3) * 85);
                    obs.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '0px 0px -30px 0px'
        });

        revealElements.forEach(el => observer.observe(el));
    } else {
        revealElements.forEach(el => el.classList.add('is-visible'));
    }

    function mostrarAlertaError(mensaje) {
        if (!simulatorAlert) return;
        simulatorAlert.textContent = mensaje;
        simulatorAlert.className = 'simulator-alert error';
        simulatorAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    function ocultarAlertaError() {
        if (!simulatorAlert) return;
        simulatorAlert.className = 'simulator-alert';
        simulatorAlert.textContent = '';
    }

    function escaparHtml(texto) {
        const div = document.createElement('div');
        div.textContent = texto || '';
        return div.innerHTML;
    }
});
