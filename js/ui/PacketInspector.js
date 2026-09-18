(function(exports) {
    class PacketInspector {
        constructor(container, simulator) {
            this.container = container;
            this.simulator = simulator;
            this.selectedPacket = null;
            this.filterProtocol = 'ALL';
            this.searchTerm = '';

            this.tableBody = container.querySelector('#packetTableBody');
            this.dissectionView = container.querySelector('#dissectionView');
            this.filterSelect = container.querySelector('#protoFilter');
            this.searchInput = container.querySelector('#packetSearch');

            this.setupEvents();
        }

        setupEvents() {
            if (this.filterSelect) {
                this.filterSelect.addEventListener('change', () => {
                    this.filterProtocol = this.filterSelect.value;
                    this.refresh();
                });
            }

            if (this.searchInput) {
                this.searchInput.addEventListener('input', () => {
                    this.searchTerm = this.searchInput.value.trim().toLowerCase();
                    this.refresh();
                });
            }

            const clearBtn = this.container.querySelector('#clearPacketsBtn');
            if (clearBtn) {
                clearBtn.addEventListener('click', () => {
                    this.simulator.packetHistory = [];
                    this.selectedPacket = null;
                    this.refresh();
                    this.dissectionView.textContent = 'Select a packet from the table above to view deep protocol dissection.';
                });
            }
        }

        addPacket(packet) {
            if (!this.matchesFilter(packet)) return;

            const tr = document.createElement('tr');
            tr.dataset.packetId = packet.id;
            tr.style.cursor = 'pointer';

            const timeStr = new Date(packet.creationTime).toTimeString().split(' ')[0] + '.' + String(packet.creationTime % 1000).padStart(3, '0');
            const protoCode = packet.protocol ? packet.protocol.code : 'DATA';
            const protoColor = packet.protocol ? packet.protocol.color : '#3b82f6';

            tr.innerHTML = `
                <td>${this.tableBody.children.length + 1}</td>
                <td>${timeStr}</td>
                <td>${packet.sourceIp}</td>
                <td>${packet.destIp}</td>
                <td style="color: ${protoColor}; font-weight: 700;">${protoCode}</td>
                <td>${packet.getPacketSize()} B</td>
                <td class="status-cell ${packet.status === 'DROPPED' ? 'text-red' : (packet.status === 'DELIVERED' ? 'text-green' : '')}">${packet.status}</td>
                <td>${this.escapeHtml(packet.payload)}</td>
            `;

            tr.addEventListener('click', () => {
                this.tableBody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
                tr.classList.add('selected');
                this.selectedPacket = packet;
                this.displayDissection(packet);
            });

            this.tableBody.appendChild(tr);
            this.tableBody.parentElement.scrollTop = this.tableBody.parentElement.scrollHeight;
        }

        updatePacketStatus(packet) {
            const tr = this.tableBody.querySelector(`tr[data-packet-id="${packet.id}"]`);
            if (tr) {
                const statusCell = tr.querySelector('.status-cell');
                if (statusCell) {
                    statusCell.textContent = packet.status;
                    statusCell.className = `status-cell ${packet.status === 'DROPPED' ? 'text-red' : (packet.status === 'DELIVERED' ? 'text-green' : '')}`;
                }
            }
            if (this.selectedPacket === packet) {
                this.displayDissection(packet);
            }
        }

        refresh() {
            this.tableBody.innerHTML = '';
            let count = 1;
            for (const packet of this.simulator.packetHistory) {
                if (this.matchesFilter(packet)) {
                    const tr = document.createElement('tr');
                    tr.dataset.packetId = packet.id;
                    if (this.selectedPacket === packet) tr.classList.add('selected');

                    const timeStr = new Date(packet.creationTime).toTimeString().split(' ')[0] + '.' + String(packet.creationTime % 1000).padStart(3, '0');
                    const protoCode = packet.protocol ? packet.protocol.code : 'DATA';
                    const protoColor = packet.protocol ? packet.protocol.color : '#3b82f6';

                    tr.innerHTML = `
                        <td>${count++}</td>
                        <td>${timeStr}</td>
                        <td>${packet.sourceIp}</td>
                        <td>${packet.destIp}</td>
                        <td style="color: ${protoColor}; font-weight: 700;">${protoCode}</td>
                        <td>${packet.getPacketSize()} B</td>
                        <td class="status-cell ${packet.status === 'DROPPED' ? 'text-red' : (packet.status === 'DELIVERED' ? 'text-green' : '')}">${packet.status}</td>
                        <td>${this.escapeHtml(packet.payload)} ${packet.dropReason ? `[DROP: ${packet.dropReason}]` : ''}</td>
                    `;

                    tr.addEventListener('click', () => {
                        this.tableBody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
                        tr.classList.add('selected');
                        this.selectedPacket = packet;
                        this.displayDissection(packet);
                    });

                    this.tableBody.appendChild(tr);
                }
            }
        }

        matchesFilter(p) {
            const code = p.protocol ? p.protocol.code : '';
            if (this.filterProtocol !== 'ALL') {
                if (this.filterProtocol === 'DROPPED' && p.status !== 'DROPPED') return false;
                if (this.filterProtocol === 'ICMP' && code !== 'ICMP') return false;
                if (this.filterProtocol === 'HTTP' && code !== 'HTTP') return false;
                if (this.filterProtocol === 'DNS' && code !== 'DNS') return false;
                if (this.filterProtocol === 'TCP' && code !== 'TCP-SYN' && code !== 'TCP-ACK') return false;
            }

            if (this.searchTerm) {
                const s = this.searchTerm;
                const match = p.sourceIp.toLowerCase().includes(s) ||
                              p.destIp.toLowerCase().includes(s) ||
                              (p.payload && p.payload.toLowerCase().includes(s));
                if (!match) return false;
            }
            return true;
        }

        displayDissection(p) {
            const protoCode = p.protocol ? p.protocol.code : 'DATA';
            const protoDesc = p.protocol ? p.protocol.description : '';

            let text = `========================================================================================\n`;
            text += `FRAME #${p.id}: ${p.getPacketSize()} bytes on wire, Protocol: ${protoCode}, Status: ${p.status}\n`;
            text += `========================================================================================\n\n`;

            text += `> LAYER 2 - ETHERNET II (Data Link Layer)\n`;
            text += `    Destination MAC: ${p.destMac || 'Broadcast / Gateway MAC'}\n`;
            text += `    Source MAC:      ${p.sourceMac || 'Host Interface MAC'}\n`;
            text += `    VLAN ID:         ${p.vlanId}\n`;
            text += `    Type:            IPv4 (0x0800)\n\n`;

            text += `> LAYER 3 - INTERNET PROTOCOL VERSION 4 (IPv4)\n`;
            text += `    Version:         4\n`;
            text += `    Header Length:   20 bytes\n`;
            text += `    Time to Live:    ${p.ttl}\n`;
            text += `    Protocol:        ${protoCode}\n`;
            text += `    Source IP:       ${p.sourceIp}\n`;
            text += `    Destination IP:  ${p.destIp}\n\n`;

            if (p.sourcePort || p.destPort) {
                text += `> LAYER 4 - TRANSPORT LAYER (TCP / UDP)\n`;
                text += `    Source Port:      ${p.sourcePort}\n`;
                text += `    Destination Port: ${p.destPort}\n`;
                text += `    Flags:            SYN=${p.synFlag}, ACK=${p.ackFlag}, FIN=${p.finFlag}\n\n`;
            }

            text += `> LAYER 7 - APPLICATION LAYER / PAYLOAD\n`;
            text += `    Protocol:        ${protoCode} (${protoDesc})\n`;
            if (p.httpMethod) text += `    HTTP Method:     ${p.httpMethod} ${p.httpUrl}\n`;
            if (p.dnsQuery) text += `    DNS Query:       ${p.dnsQuery}\n`;
            if (p.dnsResolvedIp) text += `    DNS Resolved IP: ${p.dnsResolvedIp}\n`;
            text += `    Payload Data:\n    "${p.payload.replace(/\n/g, '\n    ')}"\n\n`;

            text += `> ROUTE TRAJECTORY & HOPS\n`;
            text += `    Path Taken:      ${p.hopPath.join(' -> ')}\n`;
            if (p.dropReason) {
                text += `    [!] DROP REASON:   ${p.dropReason}\n`;
            }

            this.dissectionView.textContent = text;
        }

        escapeHtml(str) {
            return String(str || '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;');
        }
    }

    exports.PacketInspector = PacketInspector;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
