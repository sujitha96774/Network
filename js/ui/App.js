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

            this.currentView = 'dashboard';
            this.initUI();
            this.setupListeners();
            this.updateAllViews();
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

            const monChartElem = document.getElementById('monitoringThroughputCanvas');
            if (monChartElem) {
                this.monStatsChart = new StatsChart(monChartElem, this.simulator);
            }

            // Controls
            this.playPauseBtn = document.getElementById('playPauseBtn');
            this.stepBtn = document.getElementById('stepBtn');
            this.speedSlider = document.getElementById('speedSlider');
            this.speedVal = document.getElementById('speedVal');
            this.presetSelect = document.getElementById('presetSelect');
            this.runPresetBtn = document.getElementById('runPresetBtn');
            this.resetBtn = document.getElementById('resetBtn');
            this.trafficWizBtn = document.getElementById('trafficWizBtn');

            this.logConsole = document.getElementById('logConsole');
            this.dashLogConsole = document.getElementById('dashLogConsole');
            this.contextMenu = document.getElementById('contextMenu');

            // Metric Elements
            this.mSent = document.getElementById('mSent');
            this.mDelivered = document.getElementById('mDelivered');
            this.mDropped = document.getElementById('mDropped');
            this.mRate = document.getElementById('mRate');
            this.mLatency = document.getElementById('mLatency');
            this.mBytes = document.getElementById('mBytes');

            this.dashTotalNodes = document.getElementById('dashTotalNodes');
            this.dashActiveLinks = document.getElementById('dashActiveLinks');
            this.dashSent = document.getElementById('dashSent');
            this.dashRate = document.getElementById('dashRate');
            this.dashLatency = document.getElementById('dashLatency');
            this.dashDropped = document.getElementById('dashDropped');

            this.pageTitle = document.getElementById('pageTitle');

            this.setupNavigation();
            this.setupControls();
            this.setupTabNavigation();
            this.setupChaosButtons();
        }

        setupNavigation() {
            const navItems = document.querySelectorAll('.nav-item');
            navItems.forEach(item => {
                item.addEventListener('click', () => {
                    const view = item.dataset.view;
                    if (view) this.switchView(view);
                });
            });

            const btnGoToTopo = document.getElementById('btnGoToTopology');
            if (btnGoToTopo) {
                btnGoToTopo.addEventListener('click', () => this.switchView('topology'));
            }

            const btnDevTraffic = document.getElementById('btnDeviceTrafficWiz');
            if (btnDevTraffic) {
                btnDevTraffic.addEventListener('click', () => this.trafficWizard.open());
            }

            const btnSnifferWiz = document.getElementById('btnLaunchWizardFromSniffer');
            if (btnSnifferWiz) {
                btnSnifferWiz.addEventListener('click', () => this.trafficWizard.open());
            }

            const routerSelect = document.getElementById('routerSelect');
            if (routerSelect) {
                routerSelect.addEventListener('change', () => this.renderRoutingTableForSelectedRouter());
            }
        }

        switchView(viewName) {
            this.currentView = viewName;

            // Update nav item active states
            document.querySelectorAll('.nav-item').forEach(el => {
                if (el.dataset.view === viewName) {
                    el.classList.add('active');
                } else {
                    el.classList.remove('active');
                }
            });

            // Update view pane visibility
            document.querySelectorAll('.view-pane').forEach(pane => {
                pane.classList.remove('active');
            });

            const targetPane = document.getElementById(`view-${viewName}`);
            if (targetPane) targetPane.classList.add('active');

            // Set Header Title
            const titleMap = {
                'dashboard': 'Dashboard',
                'topology': 'Network Topology',
                'devices': 'Devices',
                'packet-simulator': 'Packet Simulator',
                'routing': 'Routing Protocols',
                'monitoring': 'Monitoring',
                'scenarios': 'Scenarios & Chaos',
                'settings': 'Settings'
            };
            if (this.pageTitle) {
                this.pageTitle.textContent = titleMap[viewName] || 'Dashboard';
            }

            // Canvas resize handling when switching to topology view
            if (viewName === 'topology') {
                setTimeout(() => {
                    if (this.topology) this.topology.resizeCanvas();
                }, 50);
            }

            this.updateAllViews();
        }

        setupControls() {
            if (this.playPauseBtn) {
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
            }

            if (this.stepBtn) {
                this.stepBtn.addEventListener('click', () => this.simulator.step());
            }

            if (this.speedSlider) {
                this.speedSlider.addEventListener('input', () => {
                    const spd = this.speedSlider.value / 10.0;
                    this.simulator.setSimulationSpeed(spd);
                    if (this.speedVal) this.speedVal.textContent = `${spd.toFixed(1)}x`;
                });
            }

            const settingsSpeed = document.getElementById('settingsSpeed');
            if (settingsSpeed) {
                settingsSpeed.addEventListener('input', () => {
                    const spd = settingsSpeed.value / 10.0;
                    this.simulator.setSimulationSpeed(spd);
                    if (this.speedSlider) this.speedSlider.value = settingsSpeed.value;
                    if (this.speedVal) this.speedVal.textContent = `${spd.toFixed(1)}x`;
                });
            }

            if (this.trafficWizBtn) {
                this.trafficWizBtn.addEventListener('click', () => this.trafficWizard.open());
            }

            if (this.runPresetBtn) {
                this.runPresetBtn.addEventListener('click', () => this.runScenario(parseInt(this.presetSelect.value, 10)));
            }

            const resetHandler = () => {
                ScenarioManager.buildDefaultCampusTopology(this.simulator);
                this.inspector.refresh();
                this.appendLog('[SYSTEM] Campus network reset to default topology.');
                this.updateAllViews();
            };

            if (this.resetBtn) this.resetBtn.addEventListener('click', resetHandler);
            const btnSettingsReset = document.getElementById('btnSettingsReset');
            if (btnSettingsReset) btnSettingsReset.addEventListener('click', resetHandler);

            window.addEventListener('click', () => {
                if (this.contextMenu) this.contextMenu.style.display = 'none';
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

        setupChaosButtons() {
            // Chaos scenario triggers (From screenshot & dashboard)
            const fiberCutHandler = () => {
                this.simulator.log('--- CHAOS FAULT: MAN Backbone Primary Fiber Cut (Failover Test) ---');
                const primary = this.simulator.links.find(l => l.id === 'MAN_FIBER_PRIMARY');
                if (primary) {
                    primary.isUp = false;
                    this.simulator.recalculateRouting();
                    this.simulator.log('[ALERT] Primary MAN Fiber Severed! OSPF rerouting traffic via Ring Switch Gateway...');
                    this.trafficGenerator.sendPing('192.168.1.10', '10.0.1.50', 4);
                }
                this.updateAllViews();
            };

            const ddosHandler = () => {
                this.simulator.log('--- CHAOS FAULT: DDoS Volumetric Flood Attack against Web Server ---');
                this.simulator.firewallEngine.addRule(new FirewallRule(1, 'DENY', '198.51.100.0/24', '10.0.1.50', 'ANY', 0, 'DDoS Mitigation - Block Attacker Subnet'));
                this.simulator.log('[FIREWALL] Rule #1 Injected: DENY 198.51.100.0/24 -> 10.0.1.50');
                this.trafficGenerator.sendDdosBurst('198.51.100.', '10.0.1.50', 15);
                this.updateAllViews();
            };

            const congestionHandler = () => {
                this.simulator.log('--- CHAOS FAULT: North Campus Core Router Congestion (10 Mbps Throttling) ---');
                const link = this.simulator.links.find(l => l.id === 'LINK_CS_RTR');
                if (link) {
                    link.bandwidthMbps = 10;
                    link.latencyMs = 45;
                    this.simulator.log('[WARNING] Link LINK_CS_RTR throttled to 10 Mbps with 45ms congestion delay.');
                    this.trafficGenerator.sendPing('192.168.1.10', '192.168.2.10', 4);
                }
                this.updateAllViews();
            };

            const wanStormHandler = () => {
                this.simulator.log('--- CHAOS FAULT: WAN Uplink Carrier Degradation & Packet Storm ---');
                const wanLink = this.simulator.links.find(l => l.id === 'WAN_CAMPUS_ISP');
                if (wanLink) {
                    wanLink.latencyMs = 95;
                    wanLink.packetDropRate = 0.35;
                    this.simulator.log('[WARNING] WAN Link degraded: 95ms latency, 35% drop rate.');
                    this.trafficGenerator.sendHttpRequest('192.168.1.10', '198.51.100.10', '/');
                }
                this.updateAllViews();
            };

            const restoreHandler = () => {
                this.simulator.log('--- RECOVERY: Restoring All Nominal Network Link States ---');
                this.simulator.links.forEach(l => {
                    l.isUp = true;
                    if (l.id.includes('MAN')) { l.bandwidthMbps = 10000; l.latencyMs = 2; l.packetDropRate = 0; }
                    else if (l.id.includes('WAN')) { l.bandwidthMbps = 1000; l.latencyMs = 35; l.packetDropRate = 0; }
                    else { l.bandwidthMbps = 1000; l.latencyMs = 1; l.packetDropRate = 0; }
                });
                this.simulator.firewallEngine.clearRules();
                this.simulator.firewallEngine.addRule(new FirewallRule(10, 'PERMIT', 'any', 'any', 'DNS', 53, 'Allow DNS'));
                this.simulator.firewallEngine.addRule(new FirewallRule(20, 'PERMIT', 'any', 'any', 'HTTP', 80, 'Allow Web'));
                this.simulator.firewallEngine.addRule(new FirewallRule(30, 'PERMIT', 'any', 'any', 'ICMP', 0, 'Allow Ping'));
                this.simulator.recalculateRouting();
                this.simulator.log('[RECOVERY] All 16 links restored to 100% operational capacity.');
                this.updateAllViews();
            };

            // Bind Buttons
            const btnCut = document.getElementById('btnChaosFiberCut');
            if (btnCut) btnCut.addEventListener('click', fiberCutHandler);
            const dashCut = document.getElementById('dashTriggerFiberCut');
            if (dashCut) dashCut.addEventListener('click', fiberCutHandler);

            const btnDdos = document.getElementById('btnChaosDdos');
            if (btnDdos) btnDdos.addEventListener('click', ddosHandler);
            const dashDdos = document.getElementById('dashTriggerDdos');
            if (dashDdos) dashDdos.addEventListener('click', ddosHandler);

            const btnCong = document.getElementById('btnChaosCongestion');
            if (btnCong) btnCong.addEventListener('click', congestionHandler);

            const btnWan = document.getElementById('btnChaosWanStorm');
            if (btnWan) btnWan.addEventListener('click', wanStormHandler);

            const btnRest = document.getElementById('btnChaosRestore');
            if (btnRest) btnRest.addEventListener('click', restoreHandler);

            // Dashboard Quick Actions
            const dashPing = document.getElementById('dashTriggerPing');
            if (dashPing) dashPing.addEventListener('click', () => this.runScenario(0));

            const dashHttp = document.getElementById('dashTriggerHttp');
            if (dashHttp) dashHttp.addEventListener('click', () => this.runScenario(1));

            const dashWan = document.getElementById('dashTriggerWan');
            if (dashWan) dashWan.addEventListener('click', () => this.runScenario(2));
        }

        setupListeners() {
            this.simulator.addListener({
                onPacketCreated: (packet) => {
                    this.inspector.addPacket(packet);
                    this.syncPacketSimulatorTable(packet);
                },
                onPacketDelivered: (packet) => {
                    this.inspector.updatePacketStatus(packet);
                    this.syncPacketSimulatorTable(packet);
                },
                onPacketDropped: (packet, reason) => {
                    this.inspector.updatePacketStatus(packet);
                    this.syncPacketSimulatorTable(packet);
                },
                onStatsUpdated: (metrics) => this.updateMetricsView(metrics),
                onLogMessage: (msg) => this.appendLog(msg)
            });
        }

        syncPacketSimulatorTable(packet) {
            const simBody = document.getElementById('simPacketTableBody');
            if (!simBody) return;

            // Simple refresh or mirror from main packet table
            const sourceBody = document.getElementById('packetTableBody');
            if (sourceBody) {
                simBody.innerHTML = sourceBody.innerHTML;
            }
        }

        updateMetricsView(m) {
            if (this.mSent) this.mSent.textContent = m.totalPacketsSent;
            if (this.mDelivered) this.mDelivered.textContent = m.totalPacketsDelivered;
            if (this.mDropped) this.mDropped.textContent = m.totalPacketsDropped;
            if (this.mRate) this.mRate.textContent = `${m.getDeliveryRate().toFixed(1)}%`;
            if (this.mLatency) this.mLatency.textContent = `${m.getAverageLatency().toFixed(1)} ms`;
            if (this.mBytes) this.mBytes.textContent = `${(m.totalBytesTransferred / 1024).toFixed(1)} KB`;

            if (this.dashSent) this.dashSent.textContent = m.totalPacketsSent;
            if (this.dashRate) this.dashRate.textContent = `${m.getDeliveryRate().toFixed(1)}%`;
            if (this.dashLatency) this.dashLatency.textContent = `${m.getAverageLatency().toFixed(1)} ms`;
            if (this.dashDropped) this.dashDropped.textContent = m.totalPacketsDropped;

            const monLatency = document.getElementById('monAvgLatency');
            if (monLatency) monLatency.textContent = `${m.getAverageLatency().toFixed(1)} ms`;

            const monIcmp = document.getElementById('monIcmp');
            const monHttp = document.getElementById('monHttp');
            const monDns = document.getElementById('monDns');
            const monDrop = document.getElementById('monDropped');

            if (monIcmp) monIcmp.textContent = m.protocolCounts['ICMP'] || 0;
            if (monHttp) monHttp.textContent = m.protocolCounts['HTTP'] || 0;
            if (monDns) monDns.textContent = m.protocolCounts['DNS'] || 0;
            if (monDrop) monDrop.textContent = m.totalPacketsDropped;

            if (this.statsChart) this.statsChart.render();
            if (this.monStatsChart) this.monStatsChart.render();
        }

        updateAllViews() {
            // Update node & link counts
            if (this.dashTotalNodes) this.dashTotalNodes.textContent = `${this.simulator.devices.length} Nodes`;
            if (this.dashActiveLinks) {
                const activeCount = this.simulator.links.filter(l => l.isUp).length;
                this.dashActiveLinks.textContent = `${activeCount} / ${this.simulator.links.length} Links`;
            }

            this.renderDevicesTable();
            this.renderRoutingTables();
        }

        renderDevicesTable() {
            const tbody = document.getElementById('devicesTableBody');
            if (!tbody) return;

            tbody.innerHTML = '';
            this.simulator.devices.forEach(dev => {
                const tr = document.createElement('tr');
                const primaryIf = dev.interfaces[0];
                const ipStr = primaryIf ? (primaryIf.ipAddress || 'N/A (Switch)') : 'N/A';
                const macStr = primaryIf ? primaryIf.macAddress : 'N/A';

                let typeBadge = 'Host';
                if (dev.type === 'ROUTER') typeBadge = 'Router';
                else if (dev.type === 'SWITCH') typeBadge = 'Switch';
                else if (dev.type === 'SERVER') typeBadge = 'Server';

                tr.innerHTML = `
                    <td style="font-family: var(--font-mono); font-weight: 700; color: var(--accent-blue);">${dev.id}</td>
                    <td style="font-weight: 600;">${dev.name}</td>
                    <td><span class="badge-tag badge-performance">${typeBadge}</span></td>
                    <td><span class="badge-tag badge-security">${dev.area}</span></td>
                    <td style="font-family: var(--font-mono);">${ipStr}</td>
                    <td style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-muted);">${macStr}</td>
                    <td>
                        <span class="status-pill ${dev.isOnline ? 'status-online' : 'status-offline'}">
                            ${dev.isOnline ? '● Online' : '○ Offline'}
                        </span>
                    </td>
                    <td>
                        <div style="display: flex; gap: 6px;">
                            <button class="btn" style="font-size: 0.72rem; padding: 2px 6px;" onclick="window.app.openDeviceModalById('${dev.id}')">Inspect / CLI</button>
                            <button class="btn btn-primary" style="font-size: 0.72rem; padding: 2px 6px;" onclick="window.app.trafficWizard.open(window.app.simulator.getDevice('${dev.id}'), 'PING')">Ping</button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }

        renderRoutingTables() {
            const routerSelect = document.getElementById('routerSelect');
            if (!routerSelect) return;

            const routers = this.simulator.devices.filter(d => d.type === 'ROUTER');

            // Populate select if empty
            if (routerSelect.options.length !== routers.length) {
                routerSelect.innerHTML = '';
                routers.forEach(r => {
                    const opt = document.createElement('option');
                    opt.value = r.id;
                    opt.textContent = `${r.name} (${r.id})`;
                    routerSelect.appendChild(opt);
                });
            }

            this.renderRoutingTableForSelectedRouter();
            this.renderNatTable();
            this.renderArpTable();
        }

        renderRoutingTableForSelectedRouter() {
            const routerSelect = document.getElementById('routerSelect');
            const tbody = document.getElementById('routingTableBody');
            if (!routerSelect || !tbody) return;

            const selectedId = routerSelect.value;
            const router = this.simulator.getDevice(selectedId);

            tbody.innerHTML = '';
            if (!router || !router.routingTable) {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No routing entries found.</td></tr>';
                return;
            }

            router.routingTable.forEach(entry => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td style="font-family: var(--font-mono); font-weight: 700;">${entry.destSubnet}</td>
                    <td style="font-family: var(--font-mono);">${entry.netmask}</td>
                    <td style="font-family: var(--font-mono); color: var(--accent-blue);">${entry.nextHop || '0.0.0.0 (Direct)'}</td>
                    <td style="font-family: var(--font-mono); color: var(--accent-green);">${entry.interfaceName}</td>
                    <td><span class="badge-tag badge-performance">${entry.protocol || 'OSPF / Static'}</span></td>
                    <td style="font-family: var(--font-mono);">${entry.metric || 1}</td>
                `;
                tbody.appendChild(tr);
            });
        }

        renderNatTable() {
            const tbody = document.getElementById('natTableBody');
            if (!tbody) return;

            tbody.innerHTML = '';
            const rtrGw = this.simulator.getDevice('RTR_GATEWAY');
            if (!rtrGw || !rtrGw.natEngine || rtrGw.natEngine.translations.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color: var(--text-muted); padding: 12px;">No active NAT sessions. Send HTTP/Ping from LAN host to Cloud server (198.51.100.10) to initiate translation.</td></tr>';
                return;
            }

            rtrGw.natEngine.translations.forEach(t => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td style="font-family: var(--font-mono); color: var(--accent-orange);">${t.insideLocalIp}:${t.insideLocalPort}</td>
                    <td style="font-family: var(--font-mono); color: var(--accent-green);">${t.insideGlobalIp}:${t.insideGlobalPort}</td>
                    <td style="font-family: var(--font-mono); color: var(--accent-blue);">${t.outsideIp}:${t.outsidePort}</td>
                    <td><span class="badge-tag badge-security">${t.protocol}</span></td>
                `;
                tbody.appendChild(tr);
            });
        }

        renderArpTable() {
            const tbody = document.getElementById('arpTableBody');
            if (!tbody) return;

            tbody.innerHTML = '';
            // Aggregate ARP entries from all hosts & routers
            let totalArp = 0;
            this.simulator.devices.forEach(dev => {
                if (dev.arpCache) {
                    for (const [ip, mac] of Object.entries(dev.arpCache)) {
                        totalArp++;
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td style="font-family: var(--font-mono); color: var(--accent-blue);">${ip}</td>
                            <td style="font-family: var(--font-mono);">${mac}</td>
                            <td style="font-family: var(--font-mono); color: var(--text-muted);">${dev.id} (eth0)</td>
                            <td><span class="badge-tag badge-performance">Dynamic</span></td>
                        `;
                        tbody.appendChild(tr);
                    }
                }
            });

            if (totalArp === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color: var(--text-muted); padding: 12px;">ARP cache currently empty. Transmit packets to populate layer 2 bindings.</td></tr>';
            }
        }

        openDeviceModalById(devId) {
            const dev = this.simulator.getDevice(devId);
            if (dev) this.openDeviceModal(dev);
        }

        appendLog(msg) {
            const time = new Date().toTimeString().split(' ')[0];
            const div = document.createElement('div');
            div.innerHTML = `<span style="color:#64748b;">[${time}]</span> ${this.inspector ? this.inspector.escapeHtml(msg) : msg}`;

            if (this.logConsole) {
                this.logConsole.appendChild(div);
                this.logConsole.scrollTop = this.logConsole.scrollHeight;
            }

            if (this.dashLogConsole) {
                const dashDiv = div.cloneNode(true);
                this.dashLogConsole.appendChild(dashDiv);
                this.dashLogConsole.scrollTop = this.dashLogConsole.scrollHeight;
            }
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
                this.updateAllViews();
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
                this.updateAllViews();
            });
        }

        runScenario(idx) {
            switch (idx) {
                case 0:
                    this.simulator.log('--- SCENARIO 1: Intra-Department LAN Ping (CS-Student-01 -> CS-Student-02) ---');
                    this.trafficGenerator.sendPing('192.168.1.10', '192.168.1.11', 4);
                    break;
                case 1:
                    this.simulator.log('--- SCENARIO 2: Inter-Building MAN Web Access (CS Dept -> Campus Web Portal) ---');
                    this.trafficGenerator.sendDnsQuery('192.168.1.10', '10.0.1.10', 'portal.campus.edu');
                    setTimeout(() => {
                        this.trafficGenerator.sendHttpRequest('192.168.1.10', '10.0.1.50', '/');
                    }, 600);
                    break;
                case 2:
                    this.simulator.log('--- SCENARIO 3: Remote WAN Branch Communication (CS Lab -> Remote Campus PC) ---');
                    this.trafficGenerator.sendPing('192.168.1.10', '172.16.1.10', 4);
                    break;
                case 3:
                    this.simulator.log('--- SCENARIO 4: Public Cloud Web Access via Campus Border NAT Gateway ---');
                    this.trafficGenerator.sendHttpRequest('192.168.2.10', '198.51.100.10', '/');
                    break;
                case 4:
                    this.simulator.log('--- SCENARIO 5: Simulating MAN Primary Fiber Cut & SPF Reroute ---');
                    const primary = this.simulator.links.find(l => l.id === 'MAN_FIBER_PRIMARY');
                    if (primary) {
                        primary.isUp = false;
                        this.simulator.recalculateRouting();
                        this.simulator.log('[ALERT] Primary MAN Fiber CUT! Rerouting traffic via Secondary Ring Gateway Link...');
                        this.trafficGenerator.sendPing('192.168.1.10', '10.0.1.50', 4);
                    }
                    break;
                case 5:
                    this.simulator.log('--- SCENARIO 6: DDoS SYN Flood Attack & Firewall Protection Test ---');
                    this.simulator.firewallEngine.addRule(new FirewallRule(1, 'DENY', '198.51.100.0/24', '10.0.1.50', 'ANY', 0, 'DDoS Mitigation - Block Suspicious Cloud Net'));
                    this.simulator.log('[FIREWALL] Rule #1 Injected: DENY 198.51.100.0/24 -> 10.0.1.50');
                    this.trafficGenerator.sendDdosBurst('198.51.100.', '10.0.1.50', 15);
                    break;
            }
            this.updateAllViews();
        }
    }

    exports.App = App;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
