(function(exports) {
    class StatsChart {
        constructor(canvas, simulator) {
            this.canvas = canvas;
            this.ctx = canvas.getContext('2d');
            this.simulator = simulator;

            this.initSize();
            window.addEventListener('resize', () => this.initSize());
        }

        initSize() {
            const rect = this.canvas.getBoundingClientRect();
            this.canvas.width = rect.width;
            this.canvas.height = rect.height;
        }

        render() {
            const ctx = this.ctx;
            const w = this.canvas.width;
            const h = this.canvas.height;
            const history = this.simulator.metrics.throughputHistory;

            ctx.clearRect(0, 0, w, h);

            let maxVal = 10;
            for (const val of history) {
                if (val > maxVal) maxVal = val;
            }

            // Grid
            ctx.strokeStyle = 'rgba(51, 65, 85, 0.4)';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(0, h / 2); ctx.lineTo(w, h / 2);
            ctx.stroke();

            // Line
            ctx.strokeStyle = '#10b981';
            ctx.lineWidth = 2;
            ctx.beginPath();

            const n = history.length;
            for (let i = 0; i < n; i++) {
                const x = (i * w) / (n - 1);
                const norm = history[i] / maxVal;
                const y = h - 4 - (norm * (h - 8));
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.stroke();

            // Text
            ctx.fillStyle = '#94a3b8';
            ctx.font = '9px Segoe UI';
            ctx.fillText(`Throughput: ${history[history.length - 1] || 0} Kbps [Peak: ${maxVal}]`, 6, 12);
        }
    }

    exports.StatsChart = StatsChart;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
