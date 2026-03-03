function makeDraggable(handle, onMove) {
    handle.addEventListener('mousedown', function (e) {
        e.preventDefault();
        var state = {startX: e.clientX, startY: e.clientY};

        function move(e) {
            onMove(e, state);
        }

        function up() {
            document.removeEventListener('mousemove', move);
            document.removeEventListener('mouseup', up);
        }

        document.addEventListener('mousemove', move);
        document.addEventListener('mouseup', up);
    });
}

function initResizableColumns(panelSelector, headerSelector) {
    const panel = document.querySelector(panelSelector);
    const header = document.querySelector(headerSelector);
    if (!panel || !header) return;

    const cells = Array.from(header.children);
    const widths = cells.map(function (c) {
        return c.getBoundingClientRect().width;
    });

    function applyWidths() {
        const tpl = widths.map(function (w) {
            return w + 'px';
        }).join(' ');
        panel.style.setProperty('--grid-cols', tpl);
    }

    cells.forEach(function (cell, i) {
        if (i === cells.length - 1) return;
        const handle = document.createElement('div');
        handle.className = 'col-resize-handle';
        cell.style.position = 'relative';
        cell.appendChild(handle);

        const startW_i = i;
        const nextW_i = i + 1;
        makeDraggable(handle, function (e, state) {
            const dx = e.clientX - state.startX;
            if (!state.startW) {
                state.startW = widths[startW_i];
                state.nextW = widths[nextW_i];
            }
            widths[startW_i] = Math.max(20, state.startW + dx);
            widths[nextW_i] = Math.max(20, state.nextW - dx);
            applyWidths();
        });
    });
}

function initResizablePanel() {
    document.querySelectorAll('.panel-resize-handle').forEach(function (handle) {
        if (handle.dataset.resizeInit) return;
        handle.dataset.resizeInit = 'true';

        const prev = handle.previousElementSibling;
        const top = prev.querySelector('.traffic-list') || prev.querySelector('.otel-trace-list') || prev.querySelector('.client-request-section') || prev.querySelector('.mcp-client-detail-form') || prev;

        makeDraggable(handle, function (e, state) {
            if (!state.startH) {
                state.startH = top.getBoundingClientRect().height;
            }
            const h = Math.max(80, state.startH + (e.clientY - state.startY));
            top.style.height = h + 'px';
        });
    });
}

const _chartInstances = {};

function _destroyChart(id) {
    if (_chartInstances[id]) {
        _chartInstances[id].destroy();
        delete _chartInstances[id];
    }
}

function _createChart(id, config) {
    const el = document.getElementById(id);
    if (!el) return;
    _destroyChart(id);
    _chartInstances[id] = new Chart(el, config);
}

function _createChartOnCanvas(canvas, config) {
    let key = canvas.getAttribute('data-chart-key');
    if (key && _chartInstances[key]) {
        _chartInstances[key].destroy();
        delete _chartInstances[key];
    }
    key = 'host-' + Math.random();
    canvas.setAttribute('data-chart-key', key);
    _chartInstances[key] = new Chart(canvas, config);
}

const _statusColors = {
    '2xx': 'rgba(25, 135, 84, 0.7)',
    '3xx': 'rgba(13, 202, 240, 0.7)',
    '4xx': 'rgba(255, 193, 7, 0.7)',
    '5xx': 'rgba(220, 53, 69, 0.7)'
};

const _statusBorders = {
    '2xx': '#198754',
    '3xx': '#0dcaf0',
    '4xx': '#ffc107',
    '5xx': '#dc3545'
};

function _stackedChartConfig(data) {
    const buckets = ['2xx', '3xx', '4xx', '5xx'];
    const datasets = buckets.map(function (bucket) {
        return {
            label: bucket,
            data: data.datasets[bucket] || [],
            backgroundColor: _statusColors[bucket],
            borderColor: _statusBorders[bucket],
            borderWidth: 1,
            fill: true,
            tension: 0.3,
            pointRadius: 1
        };
    });

    return {
        type: 'line',
        data: {
            labels: data.labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {position: 'bottom', labels: {usePointStyle: true, boxWidth: 8}},
                tooltip: {mode: 'index', intersect: false}
            },
            scales: {
                x: {title: {display: true, text: 'Time ago'}},
                y: {
                    stacked: true,
                    beginAtZero: true,
                    ticks: {stepSize: 1},
                    title: {display: true, text: 'Requests'}
                }
            }
        }
    };
}

function initOverviewCharts() {
    if (typeof Chart === 'undefined') return;

    const dataEl = document.getElementById('overview-chart-data');
    if (!dataEl) return;
    if (dataEl.getAttribute('data-rendered') === 'true') return;
    dataEl.setAttribute('data-rendered', 'true');

    const trafficData = JSON.parse(dataEl.getAttribute('data-traffic'));
    const latencyData = JSON.parse(dataEl.getAttribute('data-latency'));

    const latencyColors = ['#198754', '#20c997', '#ffc107', '#fd7e14', '#dc3545'];

    if (trafficData.labels.length > 0) {
        _createChart('trafficChart', _stackedChartConfig(trafficData));
    }

    if (latencyData.data.some(function (v) {
        return v > 0;
    })) {
        _createChart('latencyChart', {
            type: 'bar',
            data: {
                labels: latencyData.labels,
                datasets: [{label: 'Requests', data: latencyData.data, backgroundColor: latencyColors}]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {legend: {display: false}},
                scales: {y: {beginAtZero: true, ticks: {stepSize: 1}}}
            }
        });
    }

    const hostCards = document.querySelectorAll('[data-host-chart]');
    hostCards.forEach(function (card) {
        const hostData = JSON.parse(card.getAttribute('data-host-chart'));
        const canvas = card.querySelector('.hostChart');
        if (canvas && hostData.labels.length > 0) {
            _createChartOnCanvas(canvas, _stackedChartConfig(hostData));
        }
    });
}

function initMcpUrl() {
    const el = document.querySelector('.mcp-url');
    if (!el || el.getAttribute('data-resolved') === 'true') return;
    const path = el.textContent.trim();
    const fullUrl = window.location.origin + path;
    el.href = fullUrl;
    el.setAttribute('data-full-url', fullUrl);
    el.querySelector('code').textContent = path;
    el.setAttribute('data-resolved', 'true');

    const btn = document.querySelector('.mcp-copy-btn');
    if (btn && !btn.getAttribute('data-bound')) {
        btn.setAttribute('data-bound', 'true');
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            navigator.clipboard.writeText(fullUrl).then(function () {
                const icon = btn.querySelector('i');
                icon.className = 'bi bi-check';
                setTimeout(function () {
                    icon.className = 'bi bi-clipboard';
                }, 1500);
            });
        });
    }
}

const _mcpAppsBasePath = '/_wiretap/mcp/apps';
let _mcpCurrentServerId = null;

async function loadMcpApp(serverId, uri) {
    _mcpCurrentServerId = serverId;
    const res = await fetch(_mcpAppsBasePath + '/api/resources?serverId=' + serverId + '&uri=' + encodeURIComponent(uri));
    const csp = res.headers.get('Content-Security-Policy');
    let html = await res.text();

    if (csp) {
        html = '<meta http-equiv="Content-Security-Policy" content="' + csp + '; style-src * \'unsafe-inline\'; script-src * \'unsafe-inline\'">' + html;
    }

    document.getElementById('app').srcdoc = html;
}

addEventListener('message', async function (e) {
    if (!e.data || !e.data.jsonrpc || e.data.id === undefined) return;

    let result;
    switch (e.data.method) {
        case 'ui/initialize':
            result = {
                protocolVersion: '2026-01-26',
                hostInfo: {name: 'http4k-mcp-apps-host', version: '0.0.0'},
                hostCapabilities: {serverTools: {}, openLinks: {}, updateModelContext: {text: {}}},
                hostContext: {theme: 'light', platform: 'web', displayMode: 'inline', availableDisplayModes: ['inline']}
            };
            break;

        case 'tools/call':
            result = await fetch(_mcpAppsBasePath + '/api/tools/call', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    serverId: _mcpCurrentServerId,
                    name: e.data.params.name,
                    arguments: e.data.params.arguments
                })
            }).then(function (r) {
                return r.json();
            });
            break;

        case 'ui/openLink':
            window.open(e.data.params.url, '_blank');
            result = {};
            break;

        case 'ui/message':
        case 'ui/updateModelContext':
        case 'ui/requestDisplayMode':
            result = {};
            break;

        default:
            e.source.postMessage({
                jsonrpc: '2.0', id: e.data.id,
                error: {code: -32601, message: 'Method not found'}
            }, '*');
            return;
    }

    e.source.postMessage({jsonrpc: '2.0', id: e.data.id, result: result}, '*');
});

function initSwaggerUI() {
    if (typeof SwaggerUIBundle === 'undefined') return;
    var el = document.getElementById('swagger-ui');
    if (!el || el.getAttribute('data-rendered') === 'true') return;
    el.setAttribute('data-rendered', 'true');
    SwaggerUIBundle({
        url: '/openapi',
        dom_id: '#swagger-ui'
    });
}

document.addEventListener('DOMContentLoaded', function () {
    initResizableColumns('.list-panel', '.list-header');
    initResizableColumns('.otel-trace-list', '.trace-list-header');
    initResizablePanel();
    initOverviewCharts();
    initMcpUrl();
    initSwaggerUI();
});

let _chartInitTimer = null;
new MutationObserver(function () {
    initResizablePanel();
    initMcpUrl();
    initSwaggerUI();
    const dataEl = document.getElementById('overview-chart-data');
    if (!dataEl || dataEl.getAttribute('data-rendered') === 'true') return;
    if (_chartInitTimer) clearTimeout(_chartInitTimer);
    _chartInitTimer = setTimeout(initOverviewCharts, 50);
}).observe(document.body, {childList: true, subtree: true});
