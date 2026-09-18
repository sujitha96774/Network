(function(exports) {
    const TrafficGenerator = (typeof require !== 'undefined') ? require('../simulator/TrafficGenerator').TrafficGenerator : exports.TrafficGenerator;
    const ProtocolType = (typeof require !== 'undefined') ? require('../models/ProtocolType').ProtocolType : exports.ProtocolType;

    class TrafficWizard {
        constructor(modalElement, simulator) {
            this.modal = modalElement;
            this.simulator = simulator;
            this.trafficGenerator = new TrafficGenerator(simulator);

            this.srcSelect = modalElement.querySelector('#wizSource');
            this.dstSelect = modalElement.querySelector('#wizDest');
            this.typeSelect = modalElement.querySelector('#wizType');
            this.payloadInput = modalElement.querySelector('#wizPayload');
            this.countInput = modalElement.querySelector('#wizCount');
            this.submitBtn = modalElement.querySelector('#wizSendBtn');
            this.cancelBtn = modalElement.querySelector('#wizCancelBtn');

            this.setupEvents();
        }

        setupEvents() {
            if (this.cancelBtn) {
                this.cancelBtn.addEventListener('click', () => this.close());
            }

            this.modal.addEventListener('click', (e) => {
                if (e.target === this.modal) this.close();
            });

            if (this.submitBtn) {
                this.submitBtn.addEventListener('click', () => this.execute());
            }
        }

        open(initialSrc = null, initialType = 'PING') {
            this.populateSelects(initialSrc);
            if (initialType === 'HTTP') this.typeSelect.value = '1';
            else this.typeSelect.value = '0';
            this.modal.classList.add('active');
        }

        close() {
            this.modal.classList.remove('active');
        }

        populateSelects(selectedSrc = null) {
            this.srcSelect.innerHTML = '';
            this.dstSelect.innerHTML = '';

            for (const d of this.simulator.devices) {
                const opt1 = document.createElement('option');
                opt1.value = d.name;
                opt1.textContent = `${d.name} (${d.getPrimaryIp()}) - ${d.getDeviceType()}`;
                if (selectedSrc && selectedSrc === d) opt1.selected = true;
                this.srcSelect.appendChild(opt1);

                const opt2 = document.createElement('option');
                opt2.value = d.name;
                opt2.textContent = `${d.name} (${d.getPrimaryIp()}) - ${d.getDeviceType()}`;
                this.dstSelect.appendChild(opt2);
            }

            if (this.dstSelect.options.length > 1 && !selectedSrc) {
                this.dstSelect.selectedIndex = 1;
            }
        }

        execute() {
            const srcDev = this.simulator.findDeviceByName(this.srcSelect.value);
            const dstDev = this.simulator.findDeviceByName(this.dstSelect.value);
            const type = parseInt(this.typeSelect.value, 10);
            const payload = this.payloadInput.value.trim();
            const count = parseInt(this.countInput.value, 10) || 4;

            if (!srcDev || !dstDev) return;

            const srcIp = srcDev.getPrimaryIp();
            const dstIp = dstDev.getPrimaryIp();

            switch (type) {
                case 0: // ICMP Ping
                    this.trafficGenerator.sendPing(srcIp, dstIp, count);
                    break;
                case 1: // HTTP Web Request
                    this.trafficGenerator.sendHttpRequest(srcIp, dstIp, payload || '/');
                    break;
                case 2: // DNS Resolution Query
                    this.trafficGenerator.sendDnsQuery(srcIp, dstIp, payload || 'portal.campus.edu');
                    break;
                case 3: // Continuous TCP Stream
                    this.trafficGenerator.startContinuousTraffic(srcIp, dstIp, ProtocolType.TCP_SYN, 5, Math.min(count, 30));
                    break;
                case 4: // DDoS Flood
                    this.trafficGenerator.sendDdosBurst('198.51.100.', dstIp, count * 5);
                    break;
            }

            this.close();
        }
    }

    exports.TrafficWizard = TrafficWizard;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
