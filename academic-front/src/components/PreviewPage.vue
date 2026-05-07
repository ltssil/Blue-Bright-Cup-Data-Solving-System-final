<template>
  <div class="preview-page-root">
    <div class="preview-controls">
      <label>
        类型：
        <select v-model="type">
          <option value="students">students</option>
          <option value="teachers">teachers</option>
          <option value="sign">sign</option>
          <option value="award">award</option>
        </select>
      </label>

      <label>
        最大条数：
        <input type="number" v-model.number="limit" min="10" step="10" />
      </label>

      <button @click="load" :disabled="loading">加载预览</button>
      <button @click="onGotoCompose" :disabled="!canGotoCompose">转到合成页面并合成</button>
      <button @click="onClose">关闭并返回</button>
      <span v-if="loading" class="loading">加载中...</span>
    </div>

    <div v-if="rows && rows.length" class="preview-wrapper">
      <div class="preview-meta">共 {{ totalCountText }}（显示最多 {{ rows.length }} 条）</div>

      <div class="table-scroll">
        <table class="preview-table">
          <thead>
          <tr>
            <th v-for="c in cols" :key="c">{{ c }}</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(r, idx) in rows" :key="idx">
            <td v-for="c in cols" :key="c">{{ formatCell(r[c]) }}</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="preview-actions">
        <button @click="loadMore" :disabled="loading || !canLoadMore">加载更多</button>
        <button v-if="canDownload" @click="downloadCsv">下载 CSV</button>
      </div>
    </div>

    <div v-else class="no-data">
      <p>暂无数据。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'
const emits = defineEmits(['close','goto-compose'])

// local state
const type = ref('award')       // 默认打开时展示 award
const limit = ref(50)          // 默认显示 50 条
const rows = ref([])
const cols = ref([])
const loading = ref(false)
const totalCount = ref(null)   // 后端提供可获取总量，暂无时为 null

function formatCell(v) {
  if (v === null || v === undefined) return ''
  if (typeof v === 'object') {
    try { return JSON.stringify(v) } catch (e) { return String(v) }
  }
  return String(v)
}

async function load() {
  loading.value = true
  try {
    const res = await axios.get(`/api/preview/${type.value}?limit=${limit.value}`)
    if (res.data && res.data.success) {
      rows.value = res.data.rows || []
      cols.value = rows.value.length ? Object.keys(rows.value[0]) : []
      totalCount.value = res.data.count || null
    } else {
      rows.value = []
      cols.value = []
      totalCount.value = null
    }
  } catch (e) {
    console.error('预览加载失败', e)
    rows.value = []
    cols.value = []
    totalCount.value = null
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  limit.value = (limit.value || 0) + 50
  await load()
}

function onClose() {
  emits('close')
}

/**
 * 跳转到合成页面并触发合成 通过父组件桥接到 FinalComposer
 * 父组件App收到事件后会关闭预览页面并调用 FinalComposer.exposed.composeWithParams
 */
function onGotoCompose() {
  const payload = { fromPreviewType: type.value }
  emits('goto-compose', payload)
}

const canLoadMore = computed(() => true)
const canDownload = computed(() => rows.value && rows.value.length > 0)
const canGotoCompose = computed(() => true)

function downloadCsv() {
  if (!rows.value || rows.value.length === 0) return
  const header = cols.value.join(',')
  const lines = rows.value.map(r => cols.value.map(c => {
    const v = r[c]
    if (v === null || v === undefined) return ''
    return `"${String(v).replace(/"/g, '""')}"`
  }).join(','))
  const csv = [header, ...lines].join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const fileName = `preview_${type.value}_${new Date().toISOString().slice(0,10)}.csv`
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

const totalCountText = computed(() => {
  if (totalCount.value === null) return '（总量未知）'
  return totalCount.value
})
</script>

<style scoped>
.preview-page-root { padding: 12px; }
.preview-controls { display:flex; gap:8px; align-items:center; margin-bottom:12px; flex-wrap:wrap; }
.preview-controls label { display:flex; align-items:center; gap:6px; }
.preview-wrapper { margin-top:8px; }
.table-scroll { max-height: 60vh; overflow:auto; border:1px solid #eee; padding:6px; background:#fff; }
.preview-table { width:100%; border-collapse:collapse; font-size:13px; }
.preview-table th, .preview-table td { border:1px solid #ddd; padding:6px; text-align:left; vertical-align:top; word-break:break-word; white-space:normal; }
.preview-actions { margin-top:8px; }
.loading { color:#666; margin-left:8px; }
.no-data { color:#666; padding:12px; }
</style>
