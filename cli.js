const readline = require('readline');
const { NetworkSimulator } = require('./js/simulator/NetworkSimulator');
const { ScenarioManager } = require('./js/simulator/ScenarioManager');
const { VirtualCLI } = require('./js/cli/VirtualCLI');

const simulator = new NetworkSimulator();
ScenarioManager.buildDefaultCampusTopology(simulator);
const cli = new VirtualCLI(simulator);

console.log('=========================================================================');
console.log(' Multi-Area Campus Network Simulator (LAN, MAN, WAN) - Node.js CLI Mode');
console.log('=========================================================================');
console.log('Active Devices in Campus Network:');
for (const d of simulator.devices) {
    const type = d.getDeviceType().padEnd(10);
    const ip = d.getPrimaryIp().padEnd(16);
    const area = d.areaType ? d.areaType.id : 'N/A';
    console.log(`  * ${d.name.padEnd(20)} [${type}] IP: ${ip} Area: ${area}`);
}
console.log('-------------------------------------------------------------------------');

let currentDevice = simulator.devices[1]; // CS-Student-01
console.log(`Current active device: ${currentDevice.name} (${currentDevice.getPrimaryIp()})`);
console.log(`Type 'help' for commands, 'switch <device_name>' to change device, 'exit' to quit.\n`);

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: `${currentDevice.name}# `
});

rl.prompt();

rl.on('line', (line) => {
    const input = line.trim();
    if (input.toLowerCase() === 'exit' || input.toLowerCase() === 'quit') {
        simulator.shutdown();
        rl.close();
        return;
    }

    if (input.toLowerCase().startsWith('switch ')) {
        const devName = input.substring(7).trim();
        const found = simulator.findDeviceByName(devName);
        if (found) {
            currentDevice = found;
            console.log(`Switched to device: ${currentDevice.name} [${currentDevice.getPrimaryIp()}]`);
            rl.setPrompt(`${currentDevice.name}# `);
        } else {
            console.log(`Device '${devName}' not found.`);
        }
        rl.prompt();
        return;
    }

    const output = cli.executeCommand(currentDevice, input);
    if (output === '__CLEAR__') {
        console.clear();
    } else if (output) {
        console.log(output);
    }
    rl.prompt();
}).on('close', () => {
    console.log('\nSimulator exited.');
    process.exit(0);
});
