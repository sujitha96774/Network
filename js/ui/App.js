(function(exports) {
    const NetworkSimulator = (typeof require !== 'undefined') ? require('../simulator/NetworkSimulator').NetworkSimulator : exports.NetworkSimulator;
    const ScenarioManager = (typeof require !== 'undefined') ? require('../simulator/ScenarioManager').ScenarioManager : exports.ScenarioManager;
    const TrafficGenerator = (typeof require !== 'undefined') ? require('../simulator/TrafficGenerator').TrafficGenerator : exports.TrafficGenerator;
    const TopologyCanvas = (typeof require !== 'undefined') ? require('./TopologyCanvas').TopologyCanvas : exports.TopologyCanvas;
    const PacketInspector = (typeof require !== 'undefined') ? require('./PacketInspector').PacketInspector : exports.PacketInspector;
    const DeviceModal = (typeof require !== 'undefined') ? require('./DeviceModal').DeviceModal : exports.DeviceModal;
    const TrafficWizard = (typeof require !== 'undefined') ? require('./TrafficWizard').TrafficWizard : exports.TrafficWizard;
    const StatsChart = (typeof require !== 'undefined') ? require('./StatsChart').StatsChart : exports.StatsChart;
    const FirewallRule = (typeof require !== 'undefined') ? require('../protocols/FirewallEngine').FirewallRule : exports.FirewallRule;

    class App {
        constructor() {
            this.simulator = new NetworkSimulator();
            ScenarioManager.buildDefaultCampusTopology(this.simulator);
            this.trafficGenerator = new TrafficGenerator(this.simulator);

            this.initUI();
            this.setupListeners();
        }

        initUI() {
            // Topology Canvas
            const canvasElem = document.getElementById('networkCanvas');
            this.topology = new TopologyCanvas(canvasElem, this.simulator, this);

            // Packet Inspector
            const snifferElem = document.getElementById('snifferTab');
            this.inspector = new PacketInspector(snifferElem, this.simulator);

            // Device Modal & Traffic Wizard
            this.deviceModal = new DeviceModal(document.getElementById('deviceModal'), this.simulator);
            this.trafficWizard = new TrafficWizard(document.getElementById('trafficModal'), this.simulator);

            // Live Throughput Chart
            const chartElem = document.getElementById('throughputCanvas');
            this.statsChart = new StatsChart(chartElem, this.simulator);

            // UI Elements
            this.playPauseBtn = document.getElementById('playPauseBtn');
            this.stepBtn = document.getElementById('stepBtn');
            this.speedSlider = document.getElementById('speedSlider');
            this.speedVal = document.getElementById('speedVal');
            this.presetSelect = document.getElementById('presetSelect');
            this.runPresetBtn = document.getElementById('runPresetBtn');
            this.resetBtn = document.getElementById('resetBtn');
            this.trafficWizBtn = document.getElementById('trafficWizBtn');

            this.logConsole = document.getElementById('logConsole');
            this.contextMenu = document.getElementById('contextMenu');

            // Metric Counters
            this.mSent = document.getElementById('mSent');
            this.mDelivered = document.getElementById('mDelivered');
            this.mDropped = document.getElementById('mDropped');
            this.mRate = document.getElementById('mRate');
            this.mLatency = document.getElementById('mLatency');
            this.mBytes = document.getElementById('mBytes');

            this.setupControls();
            this.setupTabNavigation();
        }

        setupControls() {
            this.playPauseBtn.addEventListener('click', () => {
                if (this.simulator.isPaused) {
                    this.simulator.play();
                    this.playPauseBtn.textContent = '|| Pause';
                    this.playPauseBtn.classList.remove('btn-success');
                    this.playPauseBtn.classList.add('btn-primary');
                } else {
                    this.simulator.pause();
                    this.playPauseBtn.textContent = '> Resume';
                    this.playPauseBtn.classList.remove('btn-primary');
                    this.playPauseBtn.classList.add('btn-success');
                }
            });

            this.stepBtn.addEventListener('click', () => this.simulator.step());

            this.speedSlider.addEventListener('input', () => {
                const spd = this.speedSlider.value / 10.0;
                this.simulator.setSimulationSpeed(spd);
                this.speedVal.textContent = `${spd.toFixed(1)}x`;
            });

            this.trafficWizBtn.addEventListener('click', () => this.trafficWizard.open());

            this.runPresetBtn.addEventListener('click', () => this.runScenario(parseInt(this.presetSelect.value, 10)));

            this.resetBtn.addEventListener('click', () => {
                ScenarioManager.buildDefaultCampusTopology(this.simulator);
                this.inspector.refresh();
                this.logConsole.innerHTML = '<div style="color: #60a5fa;">[SYSTEM] Campus network reset to default topology.</div>';
            });

            // Close context menu on click elsewhere
            window.addEventListener('click', () => {
                this.contextMenu.style.display = 'none';
            });
        }

        setupTabNavigation() {
            const tabs = document.querySelectorAll('.tab-btn');
            tabs.forEach(btn => {
                btn.addEventListener('click', () => {
                    const parent = btn.closest('.tab-bar');
                    if (!parent) return;

                    parent.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');

                    const target = btn.dataset.tab;
                    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
                    const pane = document.getElementById(target);
                    if (pane) pane.classList.add('active');
                });
            });
        }

        setupListeners() {
            this.simulator.addListener({
                onPacketCreated: (packet) => this.inspector.addPacket(packet),
                onPacketDelivered: (packet) => this.inspector.updatePacketStatus(packet),
                onPacketDropped: (packet, reason) => this.inspector.updatePacketStatus(packet),
                onStatsUpdated: (metrics) => this.updateMetricsView(metrics),
                onLogMessage: (msg) => this.appendLog(msg)
            });
        }

        updateMetricsView(m) {
            this.mSent.textContent = m.totalPacketsSent;
            this.mDelivered.textContent = m.totalPacketsDelivered;
            this.mDropped.textContent = m.totalPacketsDropped;
            this.mRate.textContent = `${m.getDeliveryRate().toFixed(1)}%`;
            this.mLatency.textContent = `${m.getAverageLatency().toFixed(1)} ms`;
            this.mBytes.textContent = `${(m.totalBytesTransferred / 1024).toFixed(1)} KB`;

            this.statsChart.render();
        }

        appendLog(msg) {
            const time = new Date().toTimeString().split(' ')[0];
            const div = document.createElement('div');
            div.innerHTML = `<span style="color:#64748b;">[${time}]</span> ${this.inspector.escapeHtml(msg)}`;
            this.logConsole.appendChild(div);
            this.logConsole.scrollTop = this.logConsole.scrollHeight;
        }

        openDeviceModal(dev) {
            this.deviceModal.open(dev);
        }

        showDeviceContextMenu(x, y, dev) {
            this.contextMenu.innerHTML = `
                <div class="context-menu-item" id="ctxInspect">Inspect Device & Open CLI</div>
                <div class="context-menu-separator"></div>
                <div class="context-menu-item" id="ctxPing">Send Ping from this device...</div>
                <div class="context-menu-item" id="ctxHttp">Send HTTP Request from this device...</div>
                <div class="context-menu-separator"></div>
                <div class="context-menu-item" id="ctxPower">${dev.isOnline ? 'Set Offline (Power Off)' : 'Set Online (Power On)'}</div>
            `;

            this.contextMenu.style.left = `${x}px`;
            this.contextMenu.style.top = `${y}px`;
            this.contextMenu.style.display = 'block';

            document.getElementById('ctxInspect').addEventListener('click', () => this.openDeviceModal(dev));
            document.getElementById('ctxPing').addEventListener('click', () => this.trafficWizard.open(dev, 'PING'));
            document.getElementById('ctxHttp').addEventListener('click', () => this.trafficWizard.open(dev, 'HTTP'));
            document.getElementById('ctxPower').addEventListener('click', () => {
                dev.isOnline = !dev.isOnline;
                this.simulator.recalculateRouting();
            });
        }

        showLinkContextMenu(x, y, link) {
            this.contextMenu.innerHTML = `
                <div class="context-menu-item" id="ctxToggleLink">${link.isUp ? 'Fail Link (Sever Cable)' : 'Restore Link (Reconnect)'}</div>
            `;

            this.contextMenu.style.left = `${x}px`;
            this.contextMenu.style.top = `${y}px`;
            this.contextMenu.style.display = 'block';

            document.getElementById('ctxToggleLink').addEventListener('click', () => {
                this.simulator.toggleLinkState(link);
            });
        }

        runScenario(idx) {
            switch (idx) {
                case 0: // Intra-Department LAN Ping
                    this.simulator.log('--- SCENARIO 1: Intra-Department LAN Ping (CS-Student-01 -> CS-Student-02) ---');
                    this.trafficGenerator.sendPing('192.168.1.10', '192.168.1.11', 4);
                    break;
                case 1: // Inter-Building MAN Access
                    this.simulator.log('--- SCENARIO 2: Inter-Building MAN Web Access (CS Dept -> Campus Web Portal) ---');
                    this.trafficGenerator.sendDnsQuery('192.168.1.10', '10.0.1.10', 'portal.campus.edu');
                    setTimeout(() => {
                        this.trafficGenerator.sendHttpRequest('192.168.1.10', '10.0.1.50', '/');
                    }, 600);
                    break;
                case 2: // Remote WAN Branch
                    this.simulator.log('--- SCENARIO 3: Remote WAN Branch Communication (CS Lab -> Remote Campus PC) ---');
                    this.trafficGenerator.sendPing('192.168.1.10', '172.16.1.10', 4);
                    break;
                case 3: // Cloud Web Access with Border NAT
                    this.simulator.log('--- SCENARIO 4: Public Cloud Web Access via Campus Border NAT Gateway ---');
                    this.trafficGenerator.sendHttpRequest('192.168.2.10', '198.51.100.10', '/');
                    break;
                case 4: // MAN Fiber Cable Cut & Dynamic Failover
                    this.simulator.log('--- SCENARIO 5: Simulating MAN Primary Fiber Cut & SPF Reroute ---');
                    const primary = this.simulator.links.find(l => l.id === 'MAN_FIBER_PRIMARY');
                    if (primary) {
                        primary.isUp = false;
                        this.simulator.recalculateRouting();
                        this.simulator.log('[ALERT] Primary MAN Fiber CUT! Rerouting traffic via Secondary Ring Gateway Link...');
                        this.trafficGenerator.sendPing('192.168.1.10', '10.0.1.50', 4);
                    }
                    break;
                case 5: // DDoS Attack & Firewall Defense
                    this.simulator.log('--- SCENARIO 6: DDoS SYN Flood Attack & Firewall Protection Test ---');
                    this.simulator.firewallEngine.addRule(new FirewallRule(1, 'DENY', '198.51.100.0/24', '10.0.1.50', 'ANY', 0, 'DDoS Mitigation - Block Suspicious Cloud Net'));
                    this.simulator.log('[FIREWALL] Rule #1 Injected: DENY 198.51.100.0/24 -> 10.0.1.50');
                    this.trafficGenerator.sendDdosBurst('198.51.100.', '10.0.1.50', 15);
                    break;
            }
        }
    }

    exports.App = App;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
