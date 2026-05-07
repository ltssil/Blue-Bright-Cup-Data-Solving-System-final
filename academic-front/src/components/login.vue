<template>
  <div class="login-root">
    <div class="login-card">
      <h2>系统登录</h2>

      <div class="row">
        <label>用户名</label>
        <input v-model="username" placeholder="请输入用户名" />
      </div>

      <div class="row">
        <label>密码</label>
        <input v-model="password" type="password" placeholder="请输入密码" />
      </div>

      <div class="actions">
        <button @click="onLogin" :disabled="loading">登录</button>
      </div>

      <div v-if="error" class="error">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
const emits = defineEmits(['login-success'])

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref(null)

async function onLogin() {
  error.value = null
  if (!username.value || !password.value) {
    error.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  try {
    const res = await axios.post('/api/login', { username: username.value.trim(), password: password.value })
    if (res.data && res.data.success) {
      const user = res.data.user || {}
      // 本地保存用户信息（后续页面可读取）
      window.localStorage.setItem('user', JSON.stringify(user))
      // 向父组件汇报登录成功
      emits('login-success', user)
    } else {
      error.value = res.data?.message || '登录失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '网络错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-root {
  display:flex;
  justify-content:center;
  margin-top:30px;
}
.login-card {
  width:360px;
  border:1px solid #e1e1e1;
  padding:18px;
  border-radius:6px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  background:#fff;
}
.login-card h2 { margin:0 0 12px 0; font-size:18px; }
.row { margin-top:10px; display:flex; flex-direction:column; }
.row label { font-size:12px; color:#555; margin-bottom:6px; }
.row input { padding:8px; font-size:14px; border:1px solid #ccc; border-radius:4px; }
.actions { margin-top:14px; display:flex; justify-content:flex-end; }
button { padding:8px 14px; border-radius:4px; border:none; background:#1976d2; color:#fff; cursor:pointer; }
button:disabled { opacity:0.6; cursor:default; }
.error { margin-top:10px; color:#c00; }
</style>
