(function(exports) {
    class TopologyCanvas {
        constructor(canvas, simulator, app) {
            this.canvas = canvas;
            this.ctx = canvas.getContext('2d');
            this.simulator = simulator;
            this.app = app;

            this.selectedDevice = null;
            this.draggedDevice = null;
            this.dragOffset = { x: 0, y: 0 };
            this.hoveredLink = null;
            this.hoveredDevice = null;

            this.initCanvasSize();
            window.addEventListener('resize', () => this.initCanvasSize());
            this.setupEvents();
            this.startRenderLoop();
        }

        initCanvasSize() {
            const rect = this.canvas.parentElement.getBoundingClientRect();
            this.canvas.width = rect.width;
            this.canvas.height = rect.height;
        }

        setupEvents() {
            this.canvas.addEventListener('mousedown', (e) => {
                const rect = this.canvas.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;

                if (e.button === 0) { // Left Click
                    const dev = this.findDeviceAt(x, y);
                    if (dev) {
                        this.selectedDevice = dev;
                        this.draggedDevice = dev;
                        this.dragOffset.x = x - dev.x;
                        this.dragOffset.y = y - dev.y;
                    } else {
                        this.selectedDevice = null;
                    }
                }
            });

            this.canvas.addEventListener('mousemove', (e) => {
                const rect = this.canvas.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;

                if (this.draggedDevice) {
                    this.draggedDevice.x = Math.max(40, Math.min(this.canvas.width - 40, x - this.dragOffset.x));
                    this.draggedDevice.y = Math.max(40, Math.min(this.canvas.height - 40, y - this.dragOffset.y));
                } else {
                    this.hoveredDevice = this.findDeviceAt(x, y);
                    this.hoveredLink = this.findLinkAt(x, y);
                    this.canvas.style.cursor = this.hoveredDevice ? 'pointer' : (this.hoveredLink ? 'crosshair' : 'default');
                }
            });

            window.addEventListener('mouseup', () => {
                this.draggedDevice = null;
            });

            this.canvas.addEventListener('dblclick', (e) => {
                const rect = this.canvas.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const dev = this.findDeviceAt(x, y);
                if (dev) {
                    this.app.openDeviceModal(dev);
                }
            });

            this.canvas.addEventListener('contextmenu', (e) => {
                e.preventDefault();
                const rect = this.canvas.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;

                const dev = this.findDeviceAt(x, y);
                const link = this.findLinkAt(x, y);

                if (dev) {
                    this.app.showDeviceContextMenu(e.clientX, e.clientY, dev);
                } else if (link) {
                    this.app.showLinkContextMenu(e.clientX, e.clientY, link);
                }
            });
        }

        findDeviceAt(x, y) {
            for (const dev of this.simulator.devices) {
                const dx = x - dev.x;
                const dy = y - dev.y;
                if (dx * dx + dy * dy <= 26 * 26) {
                    return dev;
                }
            }
            return null;
        }

        findLinkAt(x, y) {
            for (const link of this.simulator.links) {
                const devA = link.interfaceA ? link.interfaceA.ownerDevice : null;
                const devB = link.interfaceB ? link.interfaceB.ownerDevice : null;
                if (devA && devB) {
                    const dist = this.ptSegDist(devA.x, devA.y, devB.x, devB.y, x, y);
                    if (dist <= 8.0) {
                        return link;
                    }
                }
            }
            return null;
        }

        ptSegDist(x1, y1, x2, y2, px, py) {
            const dx = x2 - x1;
            const dy = y2 - y1;
            const lenSq = dx * dx + dy * dy;
            if (lenSq === 0) return Math.hypot(px - x1, py - y1);
            let t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
            t = Math.max(0, Math.min(1, t));
            return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
        }

        startRenderLoop() {
            const render = () => {
                this.draw();
                requestAnimationFrame(render);
            };
            requestAnimationFrame(render);
        }

        draw() {
            const ctx = this.ctx;
            ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

            // 1. Draw Area Zones
            this.drawAreaZones(ctx);

            // 2. Draw Links
            this.drawLinks(ctx);

            // 3. Draw Traveling Packets
            this.drawPackets(ctx);

            // 4. Draw Device Nodes
            this.drawDevices(ctx);

            // 5. Draw Hover Tooltip
            if (this.hoveredDevice) {
                this.drawDeviceTooltip(ctx, this.hoveredDevice);
            } else if (this.hoveredLink) {
                this.drawLinkTooltip(ctx, this.hoveredLink);
            }
        }

        drawAreaZones(ctx) {
            // LAN 1 & 2: Academic Building A
            this.drawZoneBox(ctx, 20, 20, 340, 500, 'CAMPUS BUILDING A (Academic & Admin LANs)', '#3b82f6');

            // LAN 3 & 4: Research Building B & Data Center
            this.drawZoneBox(ctx, 390, 20, 420, 500, 'CAMPUS BUILDING B (Research & Central DC LANs)', '#3b82f6');

            // MAN Backbone Ring
            this.drawZoneBox(ctx, 240, 220, 320, 240, 'CAMPUS METROPOLITAN AREA NETWORK (MAN FIBER RING)', '#10b981');

            // WAN Zone
            this.drawZoneBox(ctx, 20, 530, 930, 150, 'WIDE AREA NETWORK (WAN - ISP CORE, REMOTE BRANCH & CLOUD)', '#f59e0b');
        }

        drawZoneBox(ctx, x, y, w, h, title, color) {
            ctx.save();
            ctx.fillStyle = color === '#3b82f6' ? 'rgba(59, 130, 246, 0.07)' :
                (color === '#10b981' ? 'rgba(16, 185, 129, 0.07)' : 'rgba(245, 158, 11, 0.07)');
            ctx.strokeStyle = color;
            ctx.lineWidth = 1.5;
            ctx.setLineDash([6, 4]);

            ctx.beginPath();
            ctx.roundRect(x, y, w, h, 14);
            ctx.fill();
            ctx.stroke();

            // Badge
            ctx.setLineDash([]);
            ctx.font = '600 11px Segoe UI';
            const tw = ctx.measureText(title).width + 16;
            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.roundRect(x + 12, y - 9, tw, 18, 6);
            ctx.fill();

            ctx.fillStyle = '#ffffff';
            ctx.fillText(title, x + 20, y + 4);
            ctx.restore();
        }

        drawLinks(ctx) {
            for (const link of this.simulator.links) {
                const devA = link.interfaceA ? link.interfaceA.ownerDevice : null;
                const devB = link.interfaceB ? link.interfaceB.ownerDevice : null;
                if (!devA || !devB) continue;

                const isHovered = (link === this.hoveredLink);

                ctx.save();
                if (!link.isUp) {
                    ctx.strokeStyle = '#ef4444';
                    ctx.lineWidth = 2.5;
                    ctx.setLineDash([8, 6]);
                    ctx.beginPath();
                    ctx.moveTo(devA.x, devA.y);
                    ctx.lineTo(devB.x, devB.y);
                    ctx.stroke();

                    const midX = (devA.x + devB.x) / 2;
                    const midY = (devA.y + devB.y) / 2;
                    ctx.fillStyle = '#ef4444';
                    ctx.font = '700 11px Consolas';
                    ctx.fillText('✖ CUT', midX - 18, midY - 6);
                } else {
                    let strokeColor = '#3b82f6';
                    let strokeW = 2.0;
                    if (link.areaType && link.areaType.id === 'MAN') {
                        strokeColor = '#10b981';
                        strokeW = 3.5;
                    } else if (link.areaType && link.areaType.id === 'WAN') {
                        strokeColor = '#f59e0b';
                        strokeW = 2.5;
                    }

                    ctx.strokeStyle = isHovered ? '#ffffff' : strokeColor;
                    ctx.lineWidth = strokeW;
                    ctx.beginPath();
                    ctx.moveTo(devA.x, devA.y);
                    ctx.lineTo(devB.x, devB.y);
                    ctx.stroke();

                    // Latency Badge
                    const midX = (devA.x + devB.x) / 2;
                    const midY = (devA.y + devB.y) / 2;
                    const tag = `${link.latencyMs}ms`;
                    ctx.font = '10px Segoe UI';
                    const tw = ctx.measureText(tag).width + 6;

                    ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
                    ctx.beginPath();
                    ctx.roundRect(midX - tw / 2, midY - 7, tw, 14, 4);
                    ctx.fill();

                    ctx.fillStyle = '#94a3b8';
                    ctx.fillText(tag, midX - tw / 2 + 3, midY + 4);
                }
                ctx.restore();
            }
        }

        drawPackets(ctx) {
            for (const packet of this.simulator.activeInFlightPackets) {
                const link = packet.currentLink;
                if (!link) continue;

                const fromDev = packet.currentDevice;
                const toDev = link.getOtherDevice(fromDev);
                if (!fromDev || !toDev) continue;

                const prog = packet.progressOnLink;
                const curX = fromDev.x + (toDev.x - fromDev.x) * prog;
                const curY = fromDev.y + (toDev.y - fromDev.y) * prog;

                const pColor = (packet.protocol && packet.protocol.color) ? packet.protocol.color : '#3b82f6';

                ctx.save();
                // Glow
                ctx.fillStyle = pColor;
                ctx.globalAlpha = 0.3;
                ctx.beginPath();
                ctx.arc(curX, curY, 12, 0, Math.PI * 2);
                ctx.fill();

                // Core
                ctx.globalAlpha = 1.0;
                ctx.fillStyle = pColor;
                ctx.beginPath();
                ctx.arc(curX, curY, 6, 0, Math.PI * 2);
                ctx.fill();
                ctx.strokeStyle = '#ffffff';
                ctx.lineWidth = 1.5;
                ctx.stroke();

                // Protocol Tag
                const tag = packet.protocol ? packet.protocol.code : 'DATA';
                ctx.font = 'bold 9px Segoe UI';
                const tw = ctx.measureText(tag).width + 8;

                ctx.fillStyle = 'rgba(15, 23, 42, 0.9)';
                ctx.beginPath();
                ctx.roundRect(curX - tw / 2, curY - 22, tw, 14, 4);
                ctx.fill();

                ctx.fillStyle = '#ffffff';
                ctx.fillText(tag, curX - tw / 2 + 4, curY - 12);
                ctx.restore();
            }
        }

        drawDevices(ctx) {
            for (const dev of this.simulator.devices) {
                const isSelected = (dev === this.selectedDevice);
                const isHovered = (dev === this.hoveredDevice);
                const radius = 22;

                ctx.save();
                let devColor = '#3b82f6';
                let iconSymbol = 'PC';
                const type = dev.getDeviceType();

                if (type === 'L3 Router') {
                    devColor = '#f59e0b';
                    iconSymbol = 'RTR';
                } else if (type === 'L2 Switch') {
                    devColor = '#3b82f6';
                    iconSymbol = 'SW';
                } else if (type === 'Server') {
                    devColor = '#a855f7';
                    iconSymbol = 'SRV';
                } else {
                    devColor = '#10b981';
                    iconSymbol = 'PC';
                }

                if (!dev.isOnline) devColor = '#6b7280';

                // Selected Halo
                if (isSelected || isHovered) {
                    ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
                    ctx.beginPath();
                    ctx.arc(dev.x, dev.y, radius + 6, 0, Math.PI * 2);
                    ctx.fill();
                }

                // Body
                ctx.fillStyle = dev.isOnline ? '#1e293b' : '#374151';
                ctx.beginPath();
                ctx.arc(dev.x, dev.y, radius, 0, Math.PI * 2);
                ctx.fill();

                ctx.strokeStyle = devColor;
                ctx.lineWidth = isSelected ? 3 : 2;
                ctx.stroke();

                // Device Type Icon / Text
                ctx.fillStyle = '#ffffff';
                ctx.font = 'bold 11px Segoe UI';
                const sw = ctx.measureText(iconSymbol).width;
                ctx.fillText(iconSymbol, dev.x - sw / 2, dev.y + 4);

                // Name Label
                ctx.font = 'bold 11px Segoe UI';
                ctx.fillStyle = '#f8fafc';
                const nw = ctx.measureText(dev.name).width;
                ctx.fillText(dev.name, dev.x - nw / 2, dev.y + radius + 14);

                // IP Label
                ctx.font = '10px Consolas';
                ctx.fillStyle = '#94a3b8';
                const ipStr = dev.getPrimaryIp();
                const iw = ctx.measureText(ipStr).width;
                ctx.fillText(ipStr, dev.x - iw / 2, dev.y + radius + 26);

                ctx.restore();
            }
        }

        drawDeviceTooltip(ctx, dev) {
            const x = dev.x + 30;
            const y = dev.y - 20;
            const lines = [
                `Device: ${dev.name} (${dev.getDeviceType()})`,
                `IP: ${dev.getPrimaryIp()}`,
                `MAC: ${dev.getPrimaryMac()}`,
                `Area: ${dev.areaType ? dev.areaType.id : 'N/A'}`,
                dev.description ? `Note: ${dev.description}` : ''
            ].filter(Boolean);

            ctx.save();
            ctx.font = '11px Segoe UI';
            let maxW = 0;
            for (const l of lines) {
                maxW = Math.max(maxW, ctx.measureText(l).width);
            }

            ctx.fillStyle = 'rgba(15, 23, 42, 0.95)';
            ctx.strokeStyle = '#334155';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.roundRect(x, y, maxW + 16, lines.length * 16 + 10, 6);
            ctx.fill();
            ctx.stroke();

            ctx.fillStyle = '#f8fafc';
            lines.forEach((line, idx) => {
                ctx.fillText(line, x + 8, y + 16 + idx * 16);
            });
            ctx.restore();
        }

        drawLinkTooltip(ctx, link) {
            const devA = link.interfaceA ? link.interfaceA.ownerDevice : null;
            const devB = link.interfaceB ? link.interfaceB.ownerDevice : null;
            if (!devA || !devB) return;

            const midX = (devA.x + devB.x) / 2;
            const midY = (devA.y + devB.y) / 2;

            const lines = [
                `Link ID: ${link.id} [${link.areaType ? link.areaType.id : 'LINK'}]`,
                `Endpoints: ${devA.name} <---> ${devB.name}`,
                `Bandwidth: ${link.bandwidthMbps} Mbps | Latency: ${link.latencyMs} ms`,
                `Status: ${link.isUp ? 'UP (ACTIVE)' : 'DOWN (SEVERED)'}`,
                `Right-click to Toggle UP/DOWN`
            ];

            ctx.save();
            ctx.font = '11px Segoe UI';
            let maxW = 0;
            for (const l of lines) {
                maxW = Math.max(maxW, ctx.measureText(l).width);
            }

            ctx.fillStyle = 'rgba(15, 23, 42, 0.95)';
            ctx.strokeStyle = '#334155';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.roundRect(midX + 10, midY - 20, maxW + 16, lines.length * 16 + 10, 6);
            ctx.fill();
            ctx.stroke();

            ctx.fillStyle = '#f8fafc';
            lines.forEach((line, idx) => {
                ctx.fillText(line, midX + 18, midY - 4 + idx * 16);
            });
            ctx.restore();
        }
    }

    exports.TopologyCanvas = TopologyCanvas;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
