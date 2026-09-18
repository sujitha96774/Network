const { NetworkSimulator } = require('./js/simulator/NetworkSimulator');
const { ScenarioManager } = require('./js/simulator/ScenarioManager');
const { Packet } = require('./js/models/Packet');

console.log('=========================================================');
console.log(' Running Multi-Area Campus Network Simulator JS Test Suite ');
console.log('=========================================================');

let passed = 0;
const total = 6;

async function runTests() {
    if (testTopologyInitialization()) passed++;
    if (testRoutingTableCalculation()) passed++;
    if (await testIntraLanPacketDelivery()) passed++;
    if (await testInterBuildingManDelivery()) passed++;
    if (testDnsAndNatWanFlow()) passed++;
    if (testDynamicLinkFailover()) passed++;

    console.log('=========================================================');
    console.log(` Test Suite Finished: ${passed} / ${total} Tests PASSED!`);
    console.log('=========================================================');

    if (passed !== total) {
        process.exit(1);
    } else {
        process.exit(0);
    }
}

function testTopologyInitialization() {
    process.stdout.write('[TEST 1] Testing Topology & Device Initialization... ');
    const sim = new NetworkSimulator();
    ScenarioManager.buildDefaultCampusTopology(sim);

    if (sim.devices.length >= 10 && sim.links.length >= 10) {
        console.log(`PASSED (${sim.devices.length} devices, ${sim.links.length} links)`);
        sim.shutdown();
        return true;
    }
    console.log('FAILED');
    sim.shutdown();
    return false;
}

function testRoutingTableCalculation() {
    process.stdout.write('[TEST 2] Testing Dijkstra SPF Routing Table Computation... ');
    const sim = new NetworkSimulator();
    ScenarioManager.buildDefaultCampusTopology(sim);

    const rtrA = sim.findDeviceByName('RTR-Building-A');
    if (!rtrA || rtrA.routingTable.length === 0) {
        console.log('FAILED: RTR-Building-A routing table is empty');
        sim.shutdown();
        return false;
    }

    const route = rtrA.findBestRoute('10.0.1.50');
    if (route) {
        console.log(`PASSED (Route to 10.0.1.50 via ${route.outgoingInterface} metric=${route.metric})`);
        sim.shutdown();
        return true;
    }
    console.log('FAILED: No route to 10.0.1.50');
    sim.shutdown();
    return false;
}

function testIntraLanPacketDelivery() {
    return new Promise((resolve) => {
        process.stdout.write('[TEST 3] Testing Intra-Department LAN Packet Delivery... ');
        const sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        let delivered = false;
        sim.addListener({
            onPacketDelivered: (packet) => {
                if (packet.destIp === '192.168.1.11') {
                    delivered = true;
                }
            }
        });

        const ping = Packet.createPing('192.168.1.10', '192.168.1.11', 1);
        sim.sendPacket(ping);

        setTimeout(() => {
            sim.shutdown();
            if (delivered) {
                console.log('PASSED');
                resolve(true);
            } else {
                console.log('FAILED');
                resolve(false);
            }
        }, 1200);
    });
}

function testInterBuildingManDelivery() {
    return new Promise((resolve) => {
        process.stdout.write('[TEST 4] Testing Inter-Building MAN Packet Delivery (Bldg A -> Bldg B DC)... ');
        const sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        let delivered = false;
        sim.addListener({
            onPacketDelivered: (packet) => {
                if (packet.destIp === '10.0.1.50') {
                    delivered = true;
                }
            }
        });

        const httpReq = Packet.createHttpRequest('192.168.1.10', '10.0.1.50', '/');
        sim.sendPacket(httpReq);

        setTimeout(() => {
            sim.shutdown();
            if (delivered) {
                console.log('PASSED');
                resolve(true);
            } else {
                console.log('FAILED');
                resolve(false);
            }
        }, 2000);
    });
}

function testDnsAndNatWanFlow() {
    process.stdout.write('[TEST 5] Testing DNS Resolution & WAN Border NAT Translation... ');
    const sim = new NetworkSimulator();
    ScenarioManager.buildDefaultCampusTopology(sim);

    const ip = sim.dnsService.resolve('portal.campus.edu');
    if (ip !== '10.0.1.50') {
        console.log('FAILED: DNS resolution failed');
        sim.shutdown();
        return false;
    }

    const wanPacket = Packet.createHttpRequest('192.168.2.10', '198.51.100.10', '/');
    sim.natEngine.applyOutboundNat(wanPacket, '203.0.113.2');

    if (wanPacket.sourceIp === '203.0.113.2' && wanPacket.sourcePort >= 40000) {
        console.log(`PASSED (Source IP translated to Public WAN IP ${wanPacket.sourceIp}:${wanPacket.sourcePort})`);
        sim.shutdown();
        return true;
    }
    console.log('FAILED: NAT did not rewrite source IP');
    sim.shutdown();
    return false;
}

function testDynamicLinkFailover() {
    process.stdout.write('[TEST 6] Testing Dynamic Link Failover & Rerouting... ');
    const sim = new NetworkSimulator();
    ScenarioManager.buildDefaultCampusTopology(sim);

    const primary = sim.links.find(l => l.id === 'MAN_FIBER_PRIMARY');
    if (primary) {
        primary.isUp = false; // Disconnect primary fiber
        sim.recalculateRouting();

        const rtrA = sim.findDeviceByName('RTR-Building-A');
        const rerouted = rtrA.findBestRoute('10.0.1.50');

        if (rerouted && rerouted.outgoingInterface === 'fiber0/2') {
            console.log('PASSED (Rerouted through backup fiber0/2 via Campus Gateway ring)');
            sim.shutdown();
            return true;
        }
    }
    console.log('FAILED');
    sim.shutdown();
    return false;
}

runTests();
