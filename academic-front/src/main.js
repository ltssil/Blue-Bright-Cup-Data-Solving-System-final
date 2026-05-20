// import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import { installAxiosPerformanceLogger } from './utils/performanceLogger'

installAxiosPerformanceLogger(axios)
createApp(App).mount('#app')
