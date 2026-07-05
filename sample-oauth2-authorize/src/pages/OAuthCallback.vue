<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {loadOAuth2Config} from '../utils/configStorage'
import {oauthLoginWithCode} from '../utils/api'
import {consumeState} from '../utils/oauth'

const router = useRouter()
const cfg = loadOAuth2Config()

const statusText = ref('等待处理回调...')
const errorText = ref('')

const handleCallback = async () => {
  errorText.value = ''

  if (!cfg) {
    router.replace({name: 'config'})
    return
  }

  const url = new URL(window.location.href)
  const code = url.searchParams.get('code') || ''
  const state = url.searchParams.get('state') || ''

  if (!code) {
    errorText.value = '回调缺少 code'
    statusText.value = '处理失败'
    return
  }

  const expectedState = consumeState()
  if (expectedState && state !== expectedState) {
    errorText.value = `state 校验失败：expected=${expectedState} actual=${state}`
    statusText.value = '处理失败'
    return
  }

  statusText.value = '正在调用后端 /api/oauth2/login 换取 token...'
  const res = await oauthLoginWithCode(cfg.apiBase, cfg.oauthLoginEndpoint, code)
  const accessToken = (res.accessToken || res.token || '').trim()
  if (!accessToken) {
    errorText.value = '后端返回缺少 token/accessToken'
    statusText.value = '处理失败'
    return
  }

  localStorage.setItem('sample-oauth2-authorize.accessToken', accessToken)
  statusText.value = '登录成功，跳转到 /me'
  router.replace({name: 'me'})
}

onMounted(() => {
  handleCallback().catch((e) => {
    errorText.value = String(e?.message || e)
    statusText.value = '处理失败'
  })
})
</script>

<template>
  <div class="page">
    <h1>OAuth Callback</h1>
    <div class="card">
      <div class="row">
        <label>status</label>
        <div>{{ statusText }}</div>
      </div>
      <div v-if="errorText" class="error">{{ errorText }}</div>
      <div class="actions">
        <button @click="$router.push({ name: 'home' })">返回主页</button>
        <button @click="$router.push({ name: 'config' })">返回配置</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 980px; margin: 0 auto; padding: 24px; }
.card { margin-top: 16px; padding: 16px; border: 1px solid rgba(0,0,0,0.12); border-radius: 10px; }
.row { display: grid; grid-template-columns: 180px 1fr; gap: 12px; align-items: start; padding: 8px 0; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 12px; }
button { padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.2); background: transparent; cursor: pointer; }
.error { margin-top: 8px; color: #b91c1c; }
</style>

