document.addEventListener('DOMContentLoaded', () => {
    const app = document.getElementById('graficas-app');
    const errorBox = document.getElementById('graficas-error');
    if (!app) {
        return;
    }

    const mes = app.getAttribute('data-mes') || '';
    const meses = app.getAttribute('data-meses') || '6';

    const formatoMXN = new Intl.NumberFormat('es-MX', {
        style: 'currency',
        currency: 'MXN',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    const paletaCategorias = [
        '#9333ea',
        '#ec4899',
        '#c084fc',
        '#f472b6',
        '#6366f1',
        '#db2777',
        '#a855f7',
        '#fb7185',
        '#14b8a6',
        '#f59e0b',
        '#94a3b8'
    ];

    const mostrarError = (mensaje) => {
        if (!errorBox) {
            return;
        }
        errorBox.textContent = mensaje;
        errorBox.classList.remove('hidden');
    };

    if (typeof Chart === 'undefined') {
        mostrarError('No se pudo cargar Chart.js desde el CDN. Verifica tu conexión a internet.');
        return;
    }

    const params = new URLSearchParams();
    if (mes) {
        params.set('mes', mes);
    }
    params.set('meses', meses);

    fetch('/api/graficas?' + params.toString(), {
        headers: { 'Accept': 'application/json' }
    })
        .then((respuesta) => {
            if (!respuesta.ok) {
                throw new Error('No fue posible obtener los datos de las gráficas.');
            }
            return respuesta.json();
        })
        .then((datos) => {
            renderizarDona(datos);
            renderizarAcumulado(datos);
            renderizarBarrasMensuales(datos);
        })
        .catch(() => {
            mostrarError('Ocurrió un error al cargar los datos de las gráficas.');
        });

    function renderizarDona(datos) {
        const items = Array.isArray(datos.gastoPorCategoria) ? datos.gastoPorCategoria : [];
        const emptyEl = document.getElementById('empty-dona');
        const wrapEl = document.getElementById('wrap-dona');
        const canvas = document.getElementById('chart-dona-categorias');

        if (!canvas || !wrapEl || !emptyEl) {
            return;
        }

        if (items.length === 0) {
            wrapEl.classList.add('hidden');
            emptyEl.classList.remove('hidden');
            return;
        }

        new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: items.map((i) => i.categoria),
                datasets: [{
                    data: items.map((i) => Number(i.monto)),
                    backgroundColor: items.map((_, idx) => paletaCategorias[idx % paletaCategorias.length]),
                    borderColor: '#ffffff',
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '62%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            boxWidth: 12,
                            padding: 14
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => {
                                const item = items[ctx.dataIndex];
                                const montoTexto = formatoMXN.format(Number(item.monto));
                                return ' ' + item.categoria + ': ' + montoTexto + ' (' + item.porcentaje + ' %)';
                            }
                        }
                    }
                }
            }
        });
    }

    function renderizarAcumulado(datos) {
        const serie = Array.isArray(datos.gastoAcumuladoDiario) ? datos.gastoAcumuladoDiario : [];
        const canvas = document.getElementById('chart-gasto-acumulado');
        const hint = document.getElementById('hint-presupuesto');
        if (!canvas) {
            return;
        }

        const presupuestoTotal = Number(datos.presupuestoTotalMes || 0);
        const tienePresupuesto = presupuestoTotal > 0;

        if (hint && !tienePresupuesto) {
            hint.textContent = 'Define presupuestos en la pestaña Presupuestos para ver la línea tope';
        }

        const datasets = [{
            label: 'Gasto acumulado',
            data: serie.map((d) => (d.acumulado === null ? null : Number(d.acumulado))),
            borderColor: '#db2777',
            backgroundColor: 'rgba(236, 72, 153, 0.14)',
            fill: true,
            tension: 0.28,
            pointRadius: 2.5,
            pointHoverRadius: 5,
            spanGaps: false
        }];

        if (tienePresupuesto) {
            datasets.push({
                label: 'Presupuesto total (' + formatoMXN.format(presupuestoTotal) + ')',
                data: serie.map(() => presupuestoTotal),
                borderColor: '#7e22ce',
                borderDash: [6, 6],
                borderWidth: 2,
                pointRadius: 0,
                fill: false
            });
        }

        new Chart(canvas, {
            type: 'line',
            data: {
                labels: serie.map((d) => String(d.dia)),
                datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                scales: {
                    x: {
                        title: {
                            display: true,
                            text: 'Día del mes'
                        }
                    },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (val) => formatoMXN.format(Number(val))
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'bottom'
                    },
                    tooltip: {
                        callbacks: {
                            title: (items) => {
                                if (!items.length) {
                                    return '';
                                }
                                const punto = serie[items[0].dataIndex];
                                return 'Día ' + punto.dia + ' (' + punto.fecha + ')';
                            },
                            label: (ctx) => {
                                if (ctx.parsed.y === null) {
                                    return '';
                                }
                                return ' ' + ctx.dataset.label + ': ' + formatoMXN.format(ctx.parsed.y);
                            }
                        }
                    }
                }
            }
        });
    }

    function renderizarBarrasMensuales(datos) {
        const serie = Array.isArray(datos.ingresosVsGastosMensual) ? datos.ingresosVsGastosMensual : [];
        const canvas = document.getElementById('chart-ingresos-gastos');
        if (!canvas) {
            return;
        }

        new Chart(canvas, {
            type: 'bar',
            data: {
                labels: serie.map((m) => m.etiqueta),
                datasets: [
                    {
                        label: 'Ingresos',
                        data: serie.map((m) => Number(m.ingresos)),
                        backgroundColor: 'rgba(16, 185, 129, 0.85)',
                        borderRadius: 8
                    },
                    {
                        label: 'Gastos',
                        data: serie.map((m) => Number(m.gastos)),
                        backgroundColor: 'rgba(236, 72, 153, 0.85)',
                        borderRadius: 8
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (val) => formatoMXN.format(Number(val))
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'bottom'
                    },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => ' ' + ctx.dataset.label + ': ' + formatoMXN.format(ctx.parsed.y)
                        }
                    }
                }
            }
        });
    }
});
