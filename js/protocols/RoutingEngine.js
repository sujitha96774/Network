(function(exports) {
    const NetworkInterface = (typeof require !== 'undefined') ? require('../models/NetworkInterface').NetworkInterface : exports.NetworkInterface;
    const RouteEntry = (typeof require !== 'undefined') ? require('../models/RouterDevice').RouteEntry : exports.RouteEntry;
    const RouterDevice = (typeof require !== 'undefined') ? require('../models/RouterDevice').RouterDevice : exports.RouterDevice;

    class RoutingEngine {
        static computeAllRoutes(devices, links) {
            for (const dev of devices) {
                if (dev instanceof RouterDevice || dev.getDeviceType() === 'L3 Router') {
                    RoutingEngine.computeRoutesForRouter(dev, devices, links);
                }
            }
        }

        static computeRoutesForRouter(router, allDevices, allLinks) {
            router.clearRoutes();

            // 1. Directly Connected Networks
            for (const iface of router.interfaces) {
                if (iface.isConfigured() && iface.isUp) {
                    const network = NetworkInterface.getNetworkAddress(iface.ipAddress, iface.subnetMask);
                    router.addRoute(new RouteEntry(network, iface.subnetMask, 'Direct', iface.name, 0, 'DIRECT'));
                }
            }

            // 2. Dijkstra SPF Algorithm across the topology graph
            const distances = new Map();
            const firstInterface = new Map();
            const unvisited = new Set();

            for (const d of allDevices) {
                distances.set(d, Infinity);
                unvisited.add(d);
            }

            distances.set(router, 0);

            while (unvisited.size > 0) {
                // Find node in unvisited with smallest distance
                let current = null;
                let minDist = Infinity;
                for (const node of unvisited) {
                    const d = distances.get(node);
                    if (d < minDist) {
                        minDist = d;
                        current = node;
                    }
                }

                if (!current || minDist === Infinity) break;
                unvisited.delete(current);

                // Explore neighbors
                for (const uIface of current.interfaces) {
                    if (!uIface.isUp) continue;
                    const link = uIface.connectedLink;
                    if (!link || !link.isUp) continue;

                    const v = link.getOtherDevice(current);
                    if (!v || !v.isOnline || !unvisited.has(v)) continue;

                    const linkCost = link.getRoutingCost();
                    const newDist = distances.get(current) + linkCost;

                    if (newDist < distances.get(v)) {
                        distances.set(v, newDist);

                        if (current === router) {
                            firstInterface.set(v, uIface);
                        } else {
                            firstInterface.set(v, firstInterface.get(current));
                        }
                    }
                }
            }

            // 3. Populate routes for all reachable subnets
            for (const target of allDevices) {
                if (target === router || distances.get(target) === Infinity) continue;

                const outIface = firstInterface.get(target);
                if (!outIface) continue;

                const metricCost = Math.round(distances.get(target));
                let nextHopIp = 'Direct';
                const link = outIface.connectedLink;
                if (link) {
                    const otherIface = link.getOtherInterface(outIface);
                    if (otherIface && otherIface.ipAddress) {
                        nextHopIp = otherIface.ipAddress;
                    }
                }

                for (const targetIface of target.interfaces) {
                    if (targetIface.isConfigured()) {
                        const targetNet = NetworkInterface.getNetworkAddress(targetIface.ipAddress, targetIface.subnetMask);
                        const exists = router.routingTable.some(r =>
                            r.destinationNetwork === targetNet && r.subnetMask === targetIface.subnetMask
                        );

                        if (!exists) {
                            router.addRoute(new RouteEntry(
                                targetNet,
                                targetIface.subnetMask,
                                nextHopIp,
                                outIface.name,
                                metricCost,
                                'OSPF'
                            ));
                        }
                    }
                }
            }
        }
    }

    exports.RoutingEngine = RoutingEngine;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
