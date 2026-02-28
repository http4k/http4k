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

function initOverviewCharts() {
    if (typeof Chart === 'undefined') return;

    var dataEl = document.getElementById('overview-chart-data');
    if (!dataEl) return;
    if (dataEl.getAttribute('data-rendered') === 'true') return;
    dataEl.setAttribute('data-rendered', 'true');

    var trafficData = JSON.parse(dataEl.getAttribute('data-traffic'));
    var statusData = JSON.parse(dataEl.getAttribute('data-status'));
    var methodData = JSON.parse(dataEl.getAttribute('data-method'));
    var latencyData = JSON.parse(dataEl.getAttribute('data-latency'));

    var statusColors = ['#198754', '#0dcaf0', '#ffc107', '#dc3545'];
    var methodColors = ['#0d6efd', '#6610f2', '#198754', '#dc3545', '#fd7e14', '#20c997', '#6c757d'];
    var latencyColors = ['#198754', '#20c997', '#ffc107', '#fd7e14', '#dc3545'];

    if (trafficData.labels.length > 0) {
        _createChart('trafficChart', {
            type: 'line',
            data: {
                labels: trafficData.labels,
                datasets: [{
                    label: 'Requests',
                    data: trafficData.data,
                    borderColor: '#0d6efd',
                    backgroundColor: 'rgba(13, 110, 253, 0.1)',
                    fill: true,
                    tension: 0.3,
                    pointRadius: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {legend: {display: false}},
                scales: {
                    x: {title: {display: true, text: 'Time ago'}},
                    y: {beginAtZero: true, ticks: {stepSize: 1}, title: {display: true, text: 'Requests'}}
                }
            }
        });
    }

    if (statusData.data.some(function (v) {
        return v > 0;
    })) {
        _createChart('statusChart', {
            type: 'doughnut',
            data: {
                labels: statusData.labels,
                datasets: [{data: statusData.data, backgroundColor: statusColors}]
            },
            options: {responsive: true, maintainAspectRatio: false, plugins: {legend: {position: 'bottom'}}}
        });
    }

    if (methodData.data.some(function (v) {
        return v > 0;
    })) {
        _createChart('methodChart', {
            type: 'doughnut',
            data: {
                labels: methodData.labels,
                datasets: [{data: methodData.data, backgroundColor: methodColors.slice(0, methodData.labels.length)}]
            },
            options: {responsive: true, maintainAspectRatio: false, plugins: {legend: {position: 'bottom'}}}
        });
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
}

document.addEventListener('DOMContentLoaded', function () {
    initResizableColumns('.list-panel', '.list-header');
    initResizableColumns('.otel-trace-list', '.trace-list-header');
    initResizablePanel();
    initOverviewCharts();
});

var _chartInitTimer = null;
new MutationObserver(function () {
    var dataEl = document.getElementById('overview-chart-data');
    if (!dataEl || dataEl.getAttribute('data-rendered') === 'true') return;
    if (_chartInitTimer) clearTimeout(_chartInitTimer);
    _chartInitTimer = setTimeout(initOverviewCharts, 50);
}).observe(document.body, {childList: true, subtree: true});
