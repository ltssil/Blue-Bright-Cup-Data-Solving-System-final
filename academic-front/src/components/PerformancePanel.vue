<template>
  <div class="performance-panel">
    <strong>Test CSV</strong>
    <span>logs: {{ count }}</span>
    <button @click="refreshCount">Refresh</button>
    <button @click="download">Download CSV</button>
    <button @click="clearAll">Clear</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { clearMetrics, downloadMetricsCsv, getMetricsCount } from '@/utils/performanceLogger'

const count = ref(0)

function refreshCount() {
  count.value = getMetricsCount()
}

function download() {
  downloadMetricsCsv()
  refreshCount()
}

function clearAll() {
  if (!window.confirm('Clear browser performance logs?')) return
  clearMetrics()
  refreshCount()
}

onMounted(refreshCount)
</script>

<style scoped>
.performance-panel {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 9999;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid #999;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
  font-size: 13px;
}

.performance-panel button {
  padding: 4px 8px;
}
</style>
