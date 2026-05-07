import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
    plugins: [
        vue(),
        vueDevTools(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        },
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8081', //相当于在SpringBoot编写完好的启动类中通过本地访问http://localhost:8081/xxx算法
                changeOrigin: true
            },
            '/admin': {
                target: 'http://localhost:8081',
                changeOrigin: true
            }
        }
    }
})
// 前端通过 axios 通过url/target接口对后端接口发起请求 即调用算法