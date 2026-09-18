(function(exports) {
    const VirtualCLI = (typeof require !== 'undefined') ? require('../cli/VirtualCLI').VirtualCLI : exports.VirtualCLI;

    class DeviceModal {
        constructor(modalElement, simulator) {
            this.modal = modalElement;
            this.simulator = simulator;
            this.cli = new VirtualCLI(simulator);
            this.currentDevice = null;
            this.commandHistory = [];
            this.historyIndex = -1;

            this.titleElem = modalElement.querySelector('#modalDeviceTitle');
            this.bodyElem = modalElement.querySelector('#modalDeviceBody');
            this.closeBtn = modalElement.querySelector('#closeModalBtn');

            this.setupEvents();
        }

        setupEvents() {
            if (this.closeBtn) {
                this.closeBtn.addEventListener('click', () => this.close());
            }

            this.modal.addEventListener('click', (e) => {
                if (e.target === this.modal) this.close();
            });
        }

        open(device) {
            this.currentDevice = device;
            this.titleElem.textContent = `${device.name} [${device.getDeviceType()}] - Area: ${device.areaType ? device.areaType.id : 'N/A'}`;
            this.renderTabs();
            this.modal.classList.add('active');
        }

        close() {
            this.modal.classList.remove('active');
            this.currentDevice = null;
        }

        renderTabs() {
            const dev = this.currentDevice;
            let tabsHtml = `
                <div class="tabs" style="margin-bottom: 12px;">
                    <button class="tab-btn active" data-tab="interfaces">Interfaces & IP</button>
                    ${dev.getDeviceType() === 'L3 Router' ? '<button class="tab-btn" data-tab="routes">Routing Table</button>' : ''}
                    ${dev.getDeviceType() === 'L2 Switch' ? '<button class="tab-btn" data-tab="mactable">MAC Table</button>' : ''}
                    <button class="tab-btn" data-tab="arp">ARP Cache</button>
                    <button class="tab-btn" data-tab="terminal" style="color: #4ade80;">Terminal CLI</button>
                </div>
                <div class="device-tab-panes">
                    <div class="dev-pane active" id="pane-interfaces">${this.renderInterfacesHtml(dev)}</div>
                    ${dev.getDeviceType() === 'L3 Router' ? `<div class="dev-pane" id="pane-routes" style="display:none;">${this.renderRoutesHtml(dev)}</div>` : ''}
                    ${dev.getDeviceType() === 'L2 Switch' ? `<div class="dev-pane" id="pane-mactable" style="display:none;">${this.renderMacTableHtml(dev)}</div>` : ''}
                    <div class="dev-pane" id="pane-arp" style="display:none;">${this.renderArpHtml(dev)}</div>
                    <div class="dev-pane" id="pane-terminal" style="display:none;">${this.renderTerminalHtml(dev)}</div>
                </div>
            `;

            this.bodyElem.innerHTML = tabsHtml;

            // Wire Tab buttons
            const tabBtns = this.bodyElem.querySelectorAll('.tab-btn');
            tabBtns.forEach(btn => {
                btn.addEventListener('click', () => {
                    tabBtns.forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');

                    const tab = btn.dataset.tab;
                    this.bodyElem.querySelectorAll('.dev-pane').forEach(p => p.style.display = 'none');
                    const activePane = this.bodyElem.querySelector(`#pane-${tab}`);
                    if (activePane) {
                        activePane.style.display = 'block';
                        if (tab === 'terminal') {
                            const input = activePane.querySelector('.terminal-input');
                            if (input) input.focus();
                        }
                    }
                });
            });

            this.setupTerminalInteractivity();
        }

        renderInterfacesHtml(dev) {
            let html = `
                <table class="sniffer-table" style="margin-bottom: 12px;">
                    <thead>
                        <tr>
                            <th>Interface</th>
                            <th>IP Address</th>
                            <th>Subnet Mask</th>
                            <th>MAC Address</th>
                            <th>Status</th>
                            <th>Connected Peer</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            for (const iface of dev.interfaces) {
                let peer = 'None';
                if (iface.connectedLink) {
                    const other = iface.connectedLink.getOtherDevice(dev);
                    if (other) {
                        peer = `${other.name} (${iface.connectedLink.areaType ? iface.connectedLink.areaType.id : 'LINK'})`;
                    }
                }
                html += `
                    <tr>
                        <td><b>${iface.name}</b></td>
                        <td>${iface.ipAddress || 'Unassigned'}</td>
                        <td>${iface.subnetMask || 'N/A'}</td>
                        <td>${iface.macAddress}</td>
                        <td style="color: ${iface.isUp ? '#34d399' : '#ef4444'}; font-weight: 700;">${iface.isUp ? 'UP' : 'DOWN'}</td>
                        <td>${peer}</td>
                    </tr>
                `;
            }

            html += `</tbody></table>`;
            html += `
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 0.82rem; background: #0f172a; padding: 10px; border-radius: 6px;">
                    <div><b>Primary IP:</b> ${dev.getPrimaryIp()}</div>
                    <div><b>Power State:</b> <span style="color: ${dev.isOnline ? '#34d399' : '#ef4444'}">${dev.isOnline ? 'ONLINE' : 'OFFLINE'}</span></div>
                    <div><b>Default Gateway:</b> ${dev.defaultGateway || 'N/A'}</div>
                    <div><b>DNS Server:</b> ${dev.dnsServerIp || 'N/A'}</div>
                    <div style="grid-column: span 2;"><b>Description:</b> ${dev.description || 'N/A'}</div>
                </div>
            `;
            return html;
        }

        renderRoutesHtml(dev) {
            let html = `
                <table class="sniffer-table">
                    <thead>
                        <tr>
                            <th>Destination Network</th>
                            <th>Subnet Mask</th>
                            <th>Gateway / Next Hop</th>
                            <th>Interface</th>
                            <th>Metric</th>
                            <th>Protocol</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            for (const r of dev.routingTable) {
                html += `
                    <tr>
                        <td><b>${r.destinationNetwork}</b></td>
                        <td>${r.subnetMask}</td>
                        <td>${r.nextHopIp}</td>
                        <td>${r.outgoingInterface}</td>
                        <td>${r.metric}</td>
                        <td><span class="badge" style="background: rgba(168,85,247,0.2); color: #c084fc;">${r.protocol}</span></td>
                    </tr>
                `;
            }
            html += `</tbody></table>`;
            return html;
        }

        renderMacTableHtml(dev) {
            let html = `
                <table class="sniffer-table">
                    <thead>
                        <tr>
                            <th>VLAN</th>
                            <th>MAC Address</th>
                            <th>Port</th>
                            <th>Type</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            for (const [mac, entry] of dev.macTable.entries()) {
                html += `
                    <tr>
                        <td>${entry.vlanId}</td>
                        <td><b>${entry.mac}</b></td>
                        <td>${entry.portName}</td>
                        <td>DYNAMIC</td>
                    </tr>
                `;
            }
            if (dev.macTable.size === 0) {
                html += `<tr><td colspan="4" style="text-align: center; color: #94a3b8;">(Table is empty - send traffic to learn MACs)</td></tr>`;
            }
            html += `</tbody></table>`;
            return html;
        }

        renderArpHtml(dev) {
            let html = `
                <table class="sniffer-table">
                    <thead>
                        <tr>
                            <th>IP Address</th>
                            <th>MAC Address</th>
                            <th>Interface</th>
                            <th>Type</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            for (const [ip, entry] of dev.arpTable.entries()) {
                html += `
                    <tr>
                        <td><b>${ip}</b></td>
                        <td>${entry.mac}</td>
                        <td>${entry.interfaceName}</td>
                        <td>dynamic</td>
                    </tr>
                `;
            }
            if (dev.arpTable.size === 0) {
                html += `<tr><td colspan="4" style="text-align: center; color: #94a3b8;">(No ARP entries cached yet)</td></tr>`;
            }
            html += `</tbody></table>`;
            return html;
        }

        renderTerminalHtml(dev) {
            return `
                <div class="terminal-window">
                    <div class="terminal-logs" id="terminalOutput">=======================================================================
 Welcome to ${dev.name} Interactive CLI Terminal
 Type 'help' to view diagnostic, routing, and testing commands.
=======================================================================

</div>
                    <div class="terminal-input-bar">
                        <span class="terminal-prompt">${dev.name}# </span>
                        <input type="text" class="terminal-input" id="terminalInput" autocomplete="off" spellcheck="false" placeholder="Type command here (e.g. ping 10.0.1.50, traceroute, show ip route)...">
                    </div>
                </div>
            `;
        }

        setupTerminalInteractivity() {
            const input = this.bodyElem.querySelector('#terminalInput');
            const output = this.bodyElem.querySelector('#terminalOutput');
            if (!input || !output) return;

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    const cmd = input.value.trim();
                    if (!cmd) return;

                    this.commandHistory.push(cmd);
                    this.historyIndex = this.commandHistory.length;
                    input.value = '';

                    output.textContent += `${this.currentDevice.name}# ${cmd}\n`;

                    const res = this.cli.executeCommand(this.currentDevice, cmd);
                    if (res === '__CLEAR__') {
                        output.textContent = '';
                    } else if (res) {
                        output.textContent += res + '\n\n';
                    }

                    output.scrollTop = output.scrollHeight;
                } else if (e.key === 'ArrowUp') {
                    if (this.commandHistory.length > 0 && this.historyIndex > 0) {
                        this.historyIndex--;
                        input.value = this.commandHistory[this.historyIndex];
                    }
                } else if (e.key === 'ArrowDown') {
                    if (this.historyIndex < this.commandHistory.length - 1) {
                        this.historyIndex++;
                        input.value = this.commandHistory[this.historyIndex];
                    } else {
                        this.historyIndex = this.commandHistory.length;
                        input.value = '';
                    }
                }
            });
        }
    }

    exports.DeviceModal = DeviceModal;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
