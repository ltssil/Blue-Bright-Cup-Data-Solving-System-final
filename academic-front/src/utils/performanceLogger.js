const STORAGE_KEY = 'performanceMetrics'

function nowText() {
  const d = new Date()
  const pad = (n, size = 2) => String(n).padStart(size, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`
}

function readMetrics() {
  try {
    return JSON.parse(window.localStorage.getItem(STORAGE_KEY) || '[]')
  } catch (e) {
    return []
  }
}

function writeMetrics(metrics) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(metrics))
}

function summarizeData(data) {
  if (!data) return ''
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    const parts = []
    for (const [key, value] of data.entries()) {
      if (value instanceof File) {
        parts.push(`${key}=file:${value.name}:${value.size}`)
      } else {
        parts.push(`${key}=${value}`)
      }
    }
    return parts.join(';')
  }
  if (typeof data === 'object') {
    try {
      return JSON.stringify(data)
    } catch (e) {
      return '[object]'
    }
  }
  return String(data)
}

export function logMetric(metric) {
  const metrics = readMetrics()
  metrics.push({
    time: nowText(),
    type: metric.type || 'manual',
    name: metric.name || '',
    method: metric.method || '',
    url: metric.url || '',
    status: metric.status || '',
    durationMs: Math.round(metric.durationMs || 0),
    rows: metric.rows ?? '',
    bytes: metric.bytes ?? '',
    detail: metric.detail || ''
  })
  writeMetrics(metrics)
}

export function installAxiosPerformanceLogger(axios) {
  axios.interceptors.request.use((config) => {
    config.metadata = {
      startedAt: performance.now(),
      detail: summarizeData(config.data)
    }
    return config
  })

  axios.interceptors.response.use((response) => {
    const startedAt = response.config.metadata?.startedAt || performance.now()
    logMetric({
      type: 'api',
      name: response.config.url,
      method: (response.config.method || 'get').toUpperCase(),
      url: response.config.url,
      status: response.status,
      durationMs: performance.now() - startedAt,
      rows: response.data?.count ?? (Array.isArray(response.data?.rows) ? response.data.rows.length : ''),
      bytes: response.data instanceof Blob ? response.data.size : '',
      detail: response.config.metadata?.detail || ''
    })
    return response
  }, (error) => {
    const config = error.config || {}
    const startedAt = config.metadata?.startedAt || performance.now()
    logMetric({
      type: 'api-error',
      name: config.url,
      method: (config.method || 'get').toUpperCase(),
      url: config.url,
      status: error.response?.status || 'NETWORK_ERROR',
      durationMs: performance.now() - startedAt,
      detail: error.message || ''
    })
    return Promise.reject(error)
  })
}

export function downloadMetricsCsv() {
  const rows = readMetrics()
  const headers = ['time', 'type', 'name', 'method', 'url', 'status', 'durationMs', 'rows', 'bytes', 'detail']
  const csv = [
    headers.join(','),
    ...rows.map((row) => headers.map((h) => csvCell(row[h])).join(','))
  ].join('\n')

  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `frontend-performance-${new Date().toISOString().replace(/[:.]/g, '-')}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

export function clearMetrics() {
  window.localStorage.removeItem(STORAGE_KEY)
}

export function getMetricsCount() {
  return readMetrics().length
}

function csvCell(value) {
  if (value == null) value = ''
  return `"${String(value).replace(/"/g, '""')}"`
}
