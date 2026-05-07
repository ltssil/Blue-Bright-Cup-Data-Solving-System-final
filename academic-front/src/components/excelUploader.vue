  <template>
    <div class="danger-zone">
      <h3 style="color:#c00">⚠ 数据清空（管理员）</h3>

      <div class="row">
        <label>
          <input type="checkbox" value="students" v-model="clearTables" />
          学生信息表（students）
        </label>
        <label>
          <input type="checkbox" value="teachers" v-model="clearTables" />
          教师信息表（teachers）
        </label>
        <label>
          <input type="checkbox" value="sign" v-model="clearTables" />
          报名表（sign）
        </label>
        <label>
          <input type="checkbox" value="award" v-model="clearTables" />
          获奖表（award）
        </label>
        <label>
          <input type="checkbox" value="finalreport" v-model="clearTables" />
          最终合成表（finalreport）
        </label>
        <label>
          <input type="checkbox" value="conflict" v-model="clearTables" />
          冲突表（conflict）
        </label>
      </div>

      <div class="row">
        <button class="danger-btn" @click="confirmClear">
          清空所选数据
        </button>
      </div>
    </div>

    <div class="uploader">
      <h2>Excel 文件导入</h2>

      <!-- 选择导入类型 -->
      <div class="row">
        <label>导入类型：</label>
        <select v-model="type" @change="onTypeChange">
          <option disabled value="">请选择</option>
          <option value="students">学生信息</option>
          <option value="teachers">教师信息</option>
          <option value="sign">报名表</option>
          <option value="award">获奖表</option>
        </select>
        <span v-if="type && dbCounts[type] !== undefined" style="margin-left:8px;color:#666">
          数据库已存在 <strong>{{ dbCounts[type] }}</strong> 条记录
        </span>
        <button v-if="type && dbCounts[type] > 0" @click="loadPreview" style="margin-left:8px">从数据库加载并预览</button>

        <br>

        <label style="margin-left:0px;">
          冲突年份：
          <input type="number" v-model="exportYear" placeholder="2025" style="width:100px; margin-left:0px"/>
        </label>
        <button @click="exportConflicts" style="margin-left:8px">导出冲突表</button>
      </div>

      <!-- 选择文件 -->
      <div class="row">
        <input type="file" accept=".xls,.xlsx" @change="onFileChange" />
      </div>

      <!-- 上传按钮 -->
      <div class="row">
        <button :disabled="!file || !type || uploadDisabled" @click="upload">上传</button>
  <!--      <span v-if="uploadDisabled" style="color:#c00;margin-left:10px">数据库已有数据—若要覆盖请勾选“强制重新导入”</span>-->
      </div>

      <!-- 进度 -->
      <div v-if="progress >= 0" class="row">
        上传进度：{{ Math.round(progress) }}%
      </div>

      <!-- 预览 -->
      <div v-if="previewRows && previewRows.length" class="row">
        <h3>预览（最多显示 50 条）</h3>
        <table class="preview-table">
          <thead>
          <tr>
            <th v-for="col in previewCols" :key="col">{{ col }}</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(r, idx) in previewRows" :key="idx">
            <td v-for="col in previewCols" :key="col">{{ r[col] }}</td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 返回结果 -->
      <div v-if="result" class="row">
        <pre>{{ result }}</pre>
      </div>

      <!-- 错误信息 -->
      <div v-if="error" class="row error">
        错误：{{ error }}
      </div>
    </div>
  </template>

  <script setup>
  import { ref, onMounted } from 'vue'
  import axios from 'axios'

  const type = ref('')
  const file = ref(null)
  const progress = ref(-1)
  const result = ref(null)
  const error = ref(null)

  const dbCounts = ref({})      // 存放后端返回的各表行数
  const previewRows = ref([])   // 预览数据行
  const previewCols = ref([])   // 预览表头列
  const forceImport = ref(false) // 强制覆盖数据库
  const uploadDisabled = ref(false)
  const clearTables = ref([]) // 要清除的表单
  const exportYear = ref(null);

  async function confirmClear() {
    if (clearTables.value.length === 0) {
      alert('请至少选择一个要清空的表')
      return
    }

    const msg =
        '⚠ 此操作将永久清空以下表的数据：\n\n' +
        clearTables.value.join(', ') +
        '\n\n该操作不可恢复，是否确认继续？'

    const ok = window.confirm(msg)
    if (!ok) return

    try {
      // 前端请求以 /api 开头走 vite proxy 到后端
      const res = await axios.post('/api/admin/clearTables', {
        tables: clearTables.value
      })

      if (res.data && res.data.success) {
        alert('数据清空成功')
        clearTables.value = []
        await fetchDataStatus()
        previewRows.value = []
        previewCols.value = []
      } else {
        alert(res.data?.message || '清空失败')
      }
    } catch (e) {
      // 显示后端返回的错误信息
      alert(e.response?.data?.message || e.message)
    }
  }


  // 导出冲突表，后端实际路径
  async function exportConflicts() {
    try {
      const params = {};
      if (exportYear.value) params.year = exportYear.value;

      const res = await axios.get('/api/admin/export-conflicts', {
        params,
        responseType: 'blob'
      });

      let filename = 'conflicts.xlsx'; // 仅当前会话可用
      const disposition = res.headers['content-disposition'];

      if (disposition) {
        const match = disposition.match(/filename\*=UTF-8''(.+)/);
        if (match && match[1]) {
          filename = decodeURIComponent(match[1]); // 解码 URL
        }
      }

      const blob = new Blob([res.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      const url = window.URL.createObjectURL(blob);

      const a = document.createElement('a'); // 创建标签
      a.href = url; //链接地址
      a.download = filename; // 下载的文件名
      document.body.appendChild(a); // 添加下载链接到页面
      a.click();

      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('导出冲突表失败', err);
      alert('导出失败：' + (err.message || err));
    }
  }

  async function fetchDataStatus() {
    try {
      const res = await axios.get('/api/data-status')
      if (res.data && res.data.success) {
        dbCounts.value = res.data.counts || {}
      }
    } catch (e) {
      console.warn('获取 data-status 失败', e)
      dbCounts.value = {}
    }
  }

  onMounted(() => {
    fetchDataStatus()
  })

  function onFileChange(e) {
    const f = e.target.files[0]
    if (!f) return

    if (!f.name.endsWith('.xls') && !f.name.endsWith('.xlsx')) {
      error.value = '请选择 Excel 文件 (.xls / .xlsx)'
      file.value = null
      return
    }
    if (f.size > 10 * 1024 * 1024) {
      error.value = '文件大小不能超过 10MB'
      file.value = null
      return
    }

    file.value = f
    error.value = null
    result.value = null
  }

  function onTypeChange() {
    previewRows.value = []
    previewCols.value = []
    const cnt = dbCounts.value[type.value] || 0
    uploadDisabled.value = (cnt > 0 && !forceImport.value)
  }

  async function loadPreview() {
    if (!type.value) return
    try {
      const res = await axios.get(`/api/preview/${type.value}?limit=50`)
      if (res.data && res.data.success) {
        previewRows.value = res.data.rows || []
        previewCols.value = previewRows.value.length ? Object.keys(previewRows.value[0]) : []
      } else {
        previewRows.value = []
      }
    } catch (e) {
      console.warn('预览请求失败', e)
      previewRows.value = []
    }
  }

  async function upload() {
    if (!file.value || !type.value) return

    const cnt = dbCounts.value[type.value] || 0
    if (cnt > 0 && !forceImport.value) {
      error.value = '数据库已存在该类型数据'
      return
    }

    const urlMap = {
      students: '/api/upload/students',
      teachers: '/api/upload/teachers',
      sign: '/api/upload/sign',
      award: '/api/upload/award'
    }

    const url = urlMap[type.value]
    if (!url) {
      error.value = '未知的导入类型'
      return
    }

    const fd = new FormData() // 发送文件和多部分表单数据
    fd.append('file', file.value)
    // 把 force 参数传给后端
    fd.append('force', forceImport.value ? 'true' : 'false')

    progress.value = 0
    error.value = null

    try {
      // axios 发送请求
      const res = await axios.post(url, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (evt) => { // 进度回调
          if (evt.lengthComputable) {
            progress.value = (evt.loaded / evt.total) * 100
          }
        }
      })

      if (res.data && res.data.success) {
        result.value = JSON.stringify(res.data, null, 2) // 格式
        await fetchDataStatus() // 即时刷新数据
        await loadPreview() // 即使刷新预览
        uploadDisabled.value = false
      } else {
        result.value = JSON.stringify(res.data || { success: false }, null, 2)
      }
    } catch (err) {
      error.value = err.response?.data?.message || err.message // 返回信息优先级
    } finally {
      progress.value = -1
    }
  }
  </script>

  <style scoped>
  .uploader {
    padding: 16px;
    border: 1px solid #ddd;
    border-radius: 6px;
    max-width: 900px;
  }

  .row {
    margin-top: 10px;
  }

  select {
    padding: 4px 6px;
  }

  button {
    padding: 6px 12px;
  }

  .error {
    color: red;
  }
  .danger-zone {
    margin-top: 30px;
    padding: 16px;
    border: 2px dashed #c00;
    background: #fff5f5;
  }

  .danger-zone label {
    margin-right: 16px;
  }

  .danger-btn {
    background-color: #c00;
    color: #fff;
    border: none;
    padding: 8px 16px;
    cursor: pointer;
  }

  .danger-btn:hover {
    background-color: #a00;
  }


  .preview-table { border-collapse: collapse; width: 100%; margin-top: 8px; font-size: 12px; }
  .preview-table th, .preview-table td { border: 1px solid #ccc; padding: 6px; }
  </style>
