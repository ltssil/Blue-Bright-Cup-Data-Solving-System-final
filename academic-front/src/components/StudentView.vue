<template>
  <div class="student-view">
    <h2>学生用户页面</h2>

    <div class="controls">
      <label style="margin-left:0px;">
        学号：
        <input v-model="stuNo" placeholder="输入学号查询个人获奖" />
      </label>
      <button @click="searchByStuNo" :disabled="loading || !stuNo" style="margin-left:6px">查询</button>

      <span v-if="loading" style="margin-left:12px">加载中...</span>
    </div>

    <div v-if="previewRows && previewRows.length">
      <div class="meta">获奖名单（显示最多 {{ previewRows.length }} 条）</div>
      <div class="table-wrap">
        <table class="result-table">
          <thead>
          <tr>
            <th>ID</th><th>学生名</th><th>科目</th><th>奖项</th><th>是否进入决赛</th><th>年份</th><th>比赛名</th><th>获奖级别</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(r, idx) in previewRows" :key="idx">
            <td>{{ r.id || r.award_id || (idx+1) }}</td>
            <td>{{ r.stuName }}</td>
            <td>{{ r.subject }}</td>
            <td>{{ r.award }}</td>
            <td>{{ r.inFinal }}</td>
            <td>{{ r.year || r.year }}</td>
            <td>{{ r.competitionName || r.competitionName }}</td>
            <td>{{ r.awardLevel || r.awardLevel }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="!loading">
      <p>暂无数据。</p>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const previewRows = ref([])
const stuNo = ref('')
const loading = ref(false)
const error = ref(null)

async function loadAwardList() {
  loading.value = true
  error.value = null
  try {
    // 不传 limit，server 端返回
    const res = await axios.get('/api/preview/award')
    if (res.data && res.data.success) {
      previewRows.value = res.data.rows || []
    } else {
      error.value = res.data?.message || '获取获奖名单失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '网络或服务器错误'
  } finally {
    loading.value = false
  }
}

async function searchByStuNo() {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get(`/api/final-rows?stuNo=${encodeURIComponent(stuNo.value)}&limit=200`)
    if (res.data && res.data.success) {
      previewRows.value = res.data.rows || []
    } else {
      error.value = res.data?.message || '查询失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '网络或服务器错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.student-view { padding:2px; }
.controls { margin-bottom:12px; display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.table-wrap { max-height:60vh; overflow:auto; border:1px solid #eee; padding:6px; background:#fff; }
.result-table { width:100%; border-collapse:collapse; font-size:13px; }
.result-table th, .result-table td { border:1px solid #ddd; padding:6px; text-align:left; vertical-align:top; }
.meta { margin-bottom:8px; color:#333; }
.error { color:#c00; margin-top:8px; }
</style>
