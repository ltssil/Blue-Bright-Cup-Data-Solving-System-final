<template>
  <div class="final-composer">
    <div class="controls">
      <label>
        年份：
        <select v-model="year">
          <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
        </select>
      </label>

      <label>
        获奖级别：
        <select v-model="awardLevel">
          <option v-for="award in awardLevels" :key="award" :value="award">{{ award }}</option>
        </select>
      </label>

      <label>
        比赛名：
        <select v-model="competitionName">
          <option v-for="c in competitions" :key="c" :value="c">{{ c }}</option>
        </select>
      </label>

      <!-- 专业复选 -->
      <label style="margin-left:12px">
        <input type="checkbox" v-model="majorSoftware" /> 软件工程
      </label>

      <button :disabled="!year || !competitionName" @click="compose">合成最终表单（预览）</button>

      <!-- 保存到数据库 -->
      <button :disabled="!composed || composed.length===0 || saving" @click="saveToDb">
        保存合成到数据库
      </button>

      <button :disabled="!composed || composed.length===0" @click="downloadExcel">保存最终表格到本地</button>
    </div>

    <div v-if="loading" class="loading">合成中...</div>

    <!-- 预览合成结果 -->
    <div v-if="composed && composed.length">
      <table class="preview-table">
        <thead>
        <tr>
          <th>年份</th><th>获奖级别</th><th>比赛名</th><th>科目</th><th>奖项等级</th>
          <th>学生名</th><th>学号</th><th>专业</th><th>年级</th><th>班级</th><th>指导老师</th><th>是否进入决赛</th>
        </tr>
        <!-- 分组标签 -->
        </thead>
        <!-- 封装行 -->
        <tbody>
        <tr v-for="(row, idx) in composed" :key="idx">
          <td>{{ row.year }}</td>
          <td>{{ row.awardLevel || '' }}</td>
          <td>{{ row.competitionName || '' }}</td>
          <td>{{ row.subject || '' }}</td>
          <td>{{ row.award || '' }}</td>
          <td>{{ row.stuName || '' }}</td>
          <td>{{ row.stuNo || '' }}</td>
          <td>{{ row.major || '' }}</td>
          <td>{{ row.stuGrade || '' }}</td>
          <td>{{ row.stuClass || '' }}</td>
          <td>{{ row.mentor || '' }}</td>
          <td>{{ row.inFinal || '' }}</td>
        </tr>
        </tbody>
      </table>
      <div class="count">合成条数：{{ composed.length }} | 已持久化条数：{{ persistedCountDisplay }}</div>
    </div>

    <div v-else-if="!loading">暂无数据，请先合成。</div>

    <!-- 查看已持久化表，调用 final-rows-db -->
    <div style="margin-top:10px">
      <button @click="loadPersisted" :disabled="persistLoading">加载已保存到 DB 的合成数据（finalreport）</button>
    </div>

    <div v-if="persistRows && persistRows.length" style="margin-top:8px">
      <h4>已持久化（finalreport）预览（最多 100 条）</h4>
      <table class="preview-table">
        <thead>
        <tr>
          <th>年份</th><th>获奖级别</th><th>比赛名</th><th>科目</th><th>奖项</th><th>学生名</th><th>学号</th>
          <th>专业</th><th>年级</th><th>班级</th><th>指导老师</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(r, i) in persistRows" :key="i">
          <td>{{ r.year }}</td>
          <td>{{ r.awardLevel }}</td>
          <td>{{ r.competitionName }}</td>
          <td>{{ r.subject }}</td>
          <td>{{ r.award }}</td>
          <td>{{ r.stuName }}</td>
          <td>{{ r.stuNo }}</td>
          <td>{{ r.major }}</td>
          <td>{{ r.stuGrade }}</td>
          <td>{{ r.stuClass }}</td>
          <td>{{ r.mentor }}</td>
        </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const competitionsDefault = ['蓝桥杯大赛','挑战杯','天梯赛']
const awardLevelsDefault = ['省级', '省部级单项', '国家级' , '国家级单项']

const year = ref('')
const competitionName = ref('')
const awardLevel = ref('')
const majorSoftware = ref(false) // 单个复选（软件工程）

const composed = ref([])
const loading = ref(false)
const competitions = ref([])
const years = ref([])
const awardLevels = ref([])

const saving = ref(false)
const persistRows = ref([])
const persistLoading = ref(false)
const persistedCountDisplay = ref('-')

onMounted(async () => {
  awardLevels.value = awardLevelsDefault.slice()
  competitions.value = competitionsDefault.slice()
  const now = new Date().getFullYear()
  for (let i = 0; i < 10; i++) years.value.push(String(now - i))

  try {
    const cRes = await axios.get('/api/competitions')
    if (cRes.data && cRes.data.success && Array.isArray(cRes.data.competitions) && cRes.data.competitions.length > 0) {
      competitions.value = cRes.data.competitions
    }
  } catch (e) {}

  try {
    const yRes = await axios.get('/api/years')
    if (yRes.data && yRes.data.success && Array.isArray(yRes.data.years) && yRes.data.years.length > 0) {
      years.value = yRes.data.years.map(v => String(v))
    }
  } catch (e) {}

  year.value = years.value.length ? years.value[0] : String(new Date().getFullYear())
  competitionName.value = competitions.value.length ? competitions.value[0] : ''
  awardLevel.value = awardLevels.value.length ? awardLevels.value[0] : ''
})

async function compose() {
  loading.value = true
  try {
    const major = majorSoftware.value ? '软件工程' : ''
    const res = await axios.post('/api/compose-final', {
      year: year.value,
      competitionName: competitionName.value,
      awardLevel: awardLevel.value,
      major: major
    })
    if (res.data && res.data.success) {
      composed.value = res.data.rows || []
    } else {
      composed.value = []
      alert('合成失败：' + (res.data?.message || '未知错误'))
    }
  } catch (err) {
    console.error(err)
    alert('合成请求失败：' + (err.message || err))
  } finally {
    loading.value = false
  }
}

async function saveToDb() {
  if (!composed.value || composed.value.length === 0) {
    alert('没有合成结果可保存，请先合成。')
    return
  }
  saving.value = true
  try {
    const major = majorSoftware.value ? '软件工程' : ''
    const res = await axios.post('/api/save-final-to-db', {
      year: year.value,
      competitionName: competitionName.value,
      awardLevel: awardLevel.value,
      major: major,
      overwrite: true
    })
    if (res.data && res.data.success) {
      alert('已保存到数据库（finalreport），插入条数：' + res.data.inserted)
      // 立即刷新持久化预览与计数
      await loadPersisted()
      persistedCountDisplay.value = String(res.data.inserted)
    } else {
      alert('保存失败：' + (res.data?.message || '未知错误'))
    }
  } catch (err) {
    console.error(err)
    alert('保存请求失败：' + (err.message || err))
  } finally {
    saving.value = false
  }
}

async function downloadExcel() {
  try {
    const res = await axios.post('/api/generate-final-excel',
        {
          year: year.value,
          competitionName: competitionName.value,
          awardLevel: awardLevel.value,
          major: majorSoftware.value ? '软件工程' : ''
        },
        { responseType: 'blob' }
    )
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const safeCompetition = competitionName.value || '比赛'
    const fileNameSafe = `Final_Report_${safeCompetition}_${year.value || ''}.xlsx`
    a.download = fileNameSafe
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    console.error(err)
    alert('下载失败：' + (err.message || err))
  }
}

async function loadPersisted() {
  persistLoading.value = true
  try {
    const res = await axios.get('/api/final-rows-db?limit=100')
    if (res.data && res.data.success) {
      persistRows.value = res.data.rows || []
    } else {
      persistRows.value = []
    }
  } catch (e) {
    console.error('加载 persisted 失败', e)
    persistRows.value = []
  } finally {
    persistLoading.value = false
  }
}
</script>

<style scoped>
.controls { margin-bottom: 12px; }
.controls label { margin-right: 12px; display: inline-block; }
.preview-table { border-collapse: collapse; width: 100%; margin-top: 10px; }
.preview-table th, .preview-table td { border: 1px solid #ccc; padding: 6px; font-size: 13px; }
.loading { margin: 8px 0; color: #666; }
.count { margin-top: 8px; color: #333; }
</style>
