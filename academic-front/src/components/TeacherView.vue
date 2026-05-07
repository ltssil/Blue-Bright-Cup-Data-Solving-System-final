<template>
  <div class="teacher-view">
    <h2>教师用户页面</h2>

    <div class="controls">

      <label style="margin-left:0px;">
        指导教师：
        <input v-model="mentor" placeholder="输入指导老师姓名（精确）" />
      </label>
      <button @click="searchByMentor" :disabled="loading || !mentor" style="margin-left:6px">按导师查询</button>

      <span v-if="loading" style="margin-left:12px">加载中...</span>
    </div>

    <div v-if="rows && rows.length">
      <div class="meta">共 {{ rows.length }} 条（最多 {{ limit }} 条）</div>
      <div class="table-wrap">
        <table class="result-table">
          <thead>
          <tr>
            <th>年份</th><th>获奖级别</th><th>比赛名</th><th>科目</th><th>奖项</th>
            <th>学生名</th><th>专业</th><th>年级</th><th>班级</th><th>指导老师</th><th>是否进入决赛</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(r, idx) in rows" :key="idx">
            <td>{{ r.year }}</td>
            <td>{{ r.awardLevel }}</td>
            <td>{{ r.competitionName }}</td>
            <td>{{ r.subject }}</td>
            <td>{{ r.award }}</td>
            <td>{{ r.stuName }}</td>
            <td>{{ r.major }}</td>
            <td>{{ r.stuGrade }}</td>
            <td>{{ r.stuClass }}</td>
            <td>{{ r.mentor }}</td>
            <td>{{ r.inFinal }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="!loading">
      <p>暂无数据</p>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const rows = ref([])
const mentor = ref('')
const loading = ref(false)
const error = ref(null)
const limit = ref(500)

async function loadAll() {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get(`/api/final-rows?limit=${limit.value}`)
    if (res.data && res.data.success) {
      rows.value = res.data.rows || []
    } else {
      error.value = res.data?.message || '查询失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '网络或服务器错误'
  } finally {
    loading.value = false
  }
}

async function searchByMentor() {
  loading.value = true
  error.value = null
  try {
    const m = encodeURIComponent(mentor.value.trim())
    const res = await axios.get(`/api/final-rows?mentor=${m}&limit=${limit.value}`)
    if (res.data && res.data.success) {
      rows.value = res.data.rows || []
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
.teacher-view { padding:4px; }
.controls { margin-bottom:12px; display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.table-wrap { max-height:60vh; overflow:auto; border:1px solid #eee; padding:6px; background:#fff; }
.result-table { width:100%; border-collapse:collapse; font-size:13px; }
.result-table th, .result-table td { border:1px solid #ddd; padding:6px; text-align:left; vertical-align:top; }
.meta { margin-bottom:8px; color:#333; }
.error { color:#c00; margin-top:8px; }
</style>
