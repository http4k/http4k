function initResizableColumns(panelSelector, headerSelector) {
    var panel = document.querySelector(panelSelector);
    var header = document.querySelector(headerSelector);
    if (!panel || !header) return;

    var cells = Array.from(header.children);
    var widths = cells.map(function (c) {
        return c.getBoundingClientRect().width;
    });

    function applyWidths() {
        var tpl = widths.map(function (w) {
            return w + 'px';
        }).join(' ');
        panel.style.setProperty('--grid-cols', tpl);
    }

    cells.forEach(function (cell, i) {
        if (i === cells.length - 1) return;
        var handle = document.createElement('div');
        handle.className = 'col-resize-handle';
        cell.style.position = 'relative';
        cell.appendChild(handle);

        handle.addEventListener('mousedown', function (e) {
            e.preventDefault();
            var startX = e.clientX;
            var startW = widths[i];
            var nextW = widths[i + 1];

            function onMove(e) {
                var dx = e.clientX - startX;
                widths[i] = Math.max(20, startW + dx);
                widths[i + 1] = Math.max(20, nextW - dx);
                applyWidths();
            }

            function onUp() {
                document.removeEventListener('mousemove', onMove);
                document.removeEventListener('mouseup', onUp);
            }

            document.addEventListener('mousemove', onMove);
            document.addEventListener('mouseup', onUp);
        });
    });
}

function initResizablePanel() {
    var handle = document.querySelector('.panel-resize-handle');
    if (!handle) return;

    var prev = handle.previousElementSibling;
    var top = prev.querySelector('.traffic-list') || prev.querySelector('.otel-trace-list') || prev.querySelector('.client-request-section') || prev;

    handle.addEventListener('mousedown', function (e) {
        e.preventDefault();
        var startY = e.clientY;
        var startH = top.getBoundingClientRect().height;

        function onMove(e) {
            var h = Math.max(80, startH + (e.clientY - startY));
            top.style.height = h + 'px';
        }

        function onUp() {
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
        }

        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
    });
}

var _chartInstances = {};

function _destroyChart(id) {
    if (_chartInstances[id]) {
        _chartInstances[id].destroy();
        delete _chartInstances[id];
    }
}

function _createChart(id, config) {
    var el = document.getElementById(id);
    if (!el) return;
    _destroyChart(id);
    _chartInstances[id] = new Chart(el, config);
}

function _createChartOnCanvas(canvas, config) {
    var key = canvas.getAttribute('data-chart-key');
    if (key && _chartInstances[key]) {
        _chartInstances[key].destroy();
        delete _chartInstances[key];
    }
    key = 'host-' + Math.random();
    canvas.setAttribute('data-chart-key', key);
    _chartInstances[key] = new Chart(canvas, config);
}

var _statusColors = {
    '2xx': 'rgba(25, 135, 84, 0.7)',
    '3xx': 'rgba(13, 202, 240, 0.7)',
    '4xx': 'rgba(255, 193, 7, 0.7)',
    '5xx': 'rgba(220, 53, 69, 0.7)'
};

var _statusBorders = {
    '2xx': '#198754',
    '3xx': '#0dcaf0',
    '4xx': '#ffc107',
    '5xx': '#dc3545'
};

function _stackedChartConfig(data) {
    var buckets = ['2xx', '3xx', '4xx', '5xx'];
    var datasets = buckets.map(function (bucket) {
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

    var dataEl = document.getElementById('overview-chart-data');
    if (!dataEl) return;
    if (dataEl.getAttribute('data-rendered') === 'true') return;
    dataEl.setAttribute('data-rendered', 'true');

    var trafficData = JSON.parse(dataEl.getAttribute('data-traffic'));
    var latencyData = JSON.parse(dataEl.getAttribute('data-latency'));

    var latencyColors = ['#198754', '#20c997', '#ffc107', '#fd7e14', '#dc3545'];

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

    var hostCards = document.querySelectorAll('[data-host-chart]');
    hostCards.forEach(function (card) {
        var hostData = JSON.parse(card.getAttribute('data-host-chart'));
        var canvas = card.querySelector('.hostChart');
        if (canvas && hostData.labels.length > 0) {
            _createChartOnCanvas(canvas, _stackedChartConfig(hostData));
        }
    });
}

function initMcpUrl() {
    var el = document.querySelector('.mcp-url');
    if (!el || el.getAttribute('data-resolved') === 'true') return;
    var path = el.textContent.trim();
    var fullUrl = window.location.origin + path;
    el.href = fullUrl;
    el.setAttribute('data-full-url', fullUrl);
    el.querySelector('code').textContent = path;
    el.setAttribute('data-resolved', 'true');

    var btn = document.querySelector('.mcp-copy-btn');
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

document.addEventListener('DOMContentLoaded', function () {
    initResizableColumns('.list-panel', '.list-header');
    initResizableColumns('.otel-trace-list', '.trace-list-header');
    initResizablePanel();
    initOverviewCharts();
    initMcpUrl();
});

var _chartInitTimer = null;
new MutationObserver(function () {
    initMcpUrl();
    var dataEl = document.getElementById('overview-chart-data');
    if (!dataEl || dataEl.getAttribute('data-rendered') === 'true') return;
    if (_chartInitTimer) clearTimeout(_chartInitTimer);
    _chartInitTimer = setTimeout(initOverviewCharts, 50);
}).observe(document.body, {childList: true, subtree: true});
