<template>
  <div id="app">
    <header class="header">
      <h1>学科竞赛学生信息管理系统</h1>

      <div style="position:absolute; right:20px; top:18px;">
        <template v-if="!user">
        </template>
        <template v-else>
          <span style="margin-right:8px">欢迎：{{ user.username }} ({{ user.roles.join(',') }})</span>
          <button @click="onLogout">登出</button>
        </template>
      </div>
    </header>

    <main class="content" v-if="user">
      <template v-if="isAdmin">
        <ExcelUploader />
        <hr />
        <FinalComposer />
      </template>

      <template v-else-if="isTeacher">
        <TeacherView />
      </template>

      <template v-else-if="isStudent">

        <StudentView />
      </template>

      <template v-else>
        <div>
          <p>当前用户角色未识别：{{ user.roles }}</p>
        </div>
      </template>
    </main>

    <div v-else class="login-area">
      <Login @login-success="onLoginSuccess" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ExcelUploader from '@/components/excelUploader.vue'
import FinalComposer from '@/components/FinalComposer.vue'
import Login from '@/components/login.vue'
import TeacherView from '@/components/TeacherView.vue'
import StudentView from '@/components/StudentView.vue'

const user = ref(null)
try {
  const raw = window.localStorage.getItem('user')
  if (raw) user.value = JSON.parse(raw)
} catch (e) {
  user.value = null
}

const isAdmin = computed(() => user.value && Array.isArray(user.value.roles) && user.value.roles.includes('ADMIN'))
const isTeacher = computed(() => user.value && Array.isArray(user.value.roles) && user.value.roles.includes('TEACHER'))
const isStudent = computed(() => user.value && Array.isArray(user.value.roles) && user.value.roles.includes('STUDENT'))

function onLoginSuccess(u) {
  user.value = u
}

function onLogout() {
  window.localStorage.removeItem('user')
  user.value = null
}
</script>

<style scoped>
#app { font-family: Arial, Helvetica, sans-serif; padding: 20px; position: relative; }
.header { margin-bottom: 20px; position: relative ;}
.content { margin-top: 10px; }
.login-area { margin-top: 20px; }
</style>
