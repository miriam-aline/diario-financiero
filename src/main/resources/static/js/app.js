document.addEventListener('DOMContentLoaded', () => {
    const STORAGE_SIDEBAR_KEY = 'df_sidebar_collapsed';
    const appShell = document.querySelector('.app-shell');
    const sidebar = document.getElementById('app-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    const openBtn = document.getElementById('sidebar-open');
    const closeBtn = document.getElementById('sidebar-close');
    const collapseBtn = document.getElementById('sidebar-collapse-btn');

    // 1. Sidebar colapsable a tira de iconos en escritorio (recordando preferencia visual)
    if (appShell) {
        try {
            if (window.localStorage.getItem(STORAGE_SIDEBAR_KEY) === '1') {
                appShell.classList.add('sidebar-collapsed');
            }
        } catch {
            // Ignora si el navegador bloquea storage local
        }

        if (collapseBtn) {
            collapseBtn.addEventListener('click', () => {
                const colapsado = appShell.classList.toggle('sidebar-collapsed');
                collapseBtn.setAttribute('aria-expanded', colapsado ? 'false' : 'true');
                try {
                    window.localStorage.setItem(STORAGE_SIDEBAR_KEY, colapsado ? '1' : '0');
                } catch {
                    // Ignora
                }
            });
        }
    }

    // 2. Drawer móvil
    const abrirMenu = () => {
        if (sidebar) {
            sidebar.classList.add('open');
        }
        if (backdrop) {
            backdrop.classList.add('visible');
        }
    };

    const cerrarMenu = () => {
        if (sidebar) {
            sidebar.classList.remove('open');
        }
        if (backdrop) {
            backdrop.classList.remove('visible');
        }
    };

    if (openBtn) {
        openBtn.addEventListener('click', abrirMenu);
    }
    if (closeBtn) {
        closeBtn.addEventListener('click', cerrarMenu);
    }
    if (backdrop) {
        backdrop.addEventListener('click', cerrarMenu);
    }
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            cerrarMenu();
        }
    });

    // 3. Selector visual rápido de Tipo (Gasto / Ingreso)
    const tipoInput = document.getElementById('tipo');
    const tipoButtons = document.querySelectorAll('.js-tipo-btn');
    if (tipoInput && tipoButtons.length > 0) {
        const sincronizarTipo = () => {
            tipoButtons.forEach((btn) => {
                const valor = btn.getAttribute('data-tipo');
                const activo = valor === tipoInput.value;
                btn.classList.toggle('active', activo);
                btn.setAttribute('aria-pressed', activo ? 'true' : 'false');
            });
        };

        tipoButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                const valor = btn.getAttribute('data-tipo');
                if (valor) {
                    tipoInput.value = valor;
                    sincronizarTipo();
                }
            });
        });

        sincronizarTipo();
    }

    // 4. Chips de categorías recientes
    const categoriaSelect = document.getElementById('categoriaId');
    const chips = document.querySelectorAll('.chip[data-categoria-id]');
    if (categoriaSelect && chips.length > 0) {
        const sincronizarChips = () => {
            chips.forEach((chip) => {
                const activa = chip.getAttribute('data-categoria-id') === categoriaSelect.value;
                chip.classList.toggle('active', activa);
            });
        };

        chips.forEach((chip) => {
            chip.addEventListener('click', () => {
                const id = chip.getAttribute('data-categoria-id');
                if (id) {
                    categoriaSelect.value = id;
                    sincronizarChips();
                }
            });
        });

        categoriaSelect.addEventListener('change', sincronizarChips);
        sincronizarChips();
    }

    // 5. Selector visual interactivo de estado de ánimo (Diario)
    const animoInput = document.getElementById('diario-animo');
    const moodButtons = document.querySelectorAll('.js-mood-btn');
    if (animoInput && moodButtons.length > 0) {
        const sincronizarAnimo = () => {
            moodButtons.forEach((btn) => {
                const val = btn.getAttribute('data-mood');
                const activo = val === String(animoInput.value || '');
                btn.classList.toggle('active', activo);
                btn.setAttribute('aria-pressed', activo ? 'true' : 'false');
            });
        };

        moodButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                const val = btn.getAttribute('data-mood') || '';
                animoInput.value = animoInput.value === val ? '' : val;
                sincronizarAnimo();
            });
        });

        sincronizarAnimo();
    }

    // 6. Pestañas internas (ej. Recurrentes | Meses Sin Intereses)
    const tabButtons = document.querySelectorAll('.js-tab-btn');
    const tabPanels = document.querySelectorAll('.js-tab-panel');
    if (tabButtons.length > 0 && tabPanels.length > 0) {
        const activarTab = (tabId) => {
            tabButtons.forEach((btn) => {
                const activo = btn.getAttribute('data-tab-target') === tabId;
                btn.classList.toggle('active', activo);
                btn.setAttribute('aria-selected', activo ? 'true' : 'false');
            });
            tabPanels.forEach((panel) => {
                const visible = panel.getAttribute('data-tab-panel') === tabId;
                panel.classList.toggle('hidden', !visible);
            });
        };

        tabButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                const target = btn.getAttribute('data-tab-target');
                if (target) {
                    activarTab(target);
                }
            });
        });
    }

    // 7. Zona drag & drop de CSV
    const dropzone = document.getElementById('csv-dropzone');
    const fileInput = document.getElementById('archivo-csv');
    const fileLabel = document.getElementById('csv-file-name');
    if (dropzone && fileInput && fileLabel) {
        const actualizarNombre = () => {
            if (fileInput.files && fileInput.files.length > 0) {
                fileLabel.textContent = 'Archivo listo: ' + fileInput.files[0].name;
                dropzone.classList.add('has-file');
            } else {
                fileLabel.textContent = 'Arrastra aquí tu archivo .csv o haz clic para seleccionarlo';
                dropzone.classList.remove('has-file');
            }
        };

        fileInput.addEventListener('change', actualizarNombre);

        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });

        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('dragover');
        });

        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                fileInput.files = e.dataTransfer.files;
                actualizarNombre();
            }
        });
    }

    // 8. Confirmación accesible de borrado mediante <dialog> (sin window.confirm)
    const dialog = document.getElementById('confirm-delete-dialog');
    const dialogMessage = document.getElementById('dialog-message');
    const cancelBtn = document.getElementById('dialog-cancel');
    const confirmBtn = document.getElementById('dialog-confirm');
    let formularioPendiente = null;

    document.querySelectorAll('.js-delete-trigger').forEach((boton) => {
        boton.addEventListener('click', () => {
            const form = boton.closest('form');
            if (!form || !dialog || !dialogMessage) {
                return;
            }
            const descripcion = form.getAttribute('data-descripcion') || 'este registro';
            formularioPendiente = form;
            dialogMessage.textContent = '¿Seguro que deseas eliminar "' + descripcion + '"?';
            dialog.showModal();
        });
    });

    if (cancelBtn && dialog) {
        cancelBtn.addEventListener('click', () => {
            formularioPendiente = null;
            dialog.close();
        });
    }

    if (confirmBtn && dialog) {
        confirmBtn.addEventListener('click', () => {
            if (formularioPendiente) {
                const form = formularioPendiente;
                formularioPendiente = null;
                dialog.close();
                form.submit();
            }
        });
    }
});
