<script setup lang="ts">
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {loadOAuth2Config} from '../utils/configStorage'
import {buildAuthorizeUrl, generateState, storeState} from '../utils/oauth'

const router = useRouter()
const cfg = loadOAuth2Config()

if (!cfg) {
  router.replace({name: 'config'})
}

const token = ref(localStorage.getItem('sample-oauth2-authorize.accessToken') || '')
const copiedKey = ref('')
const errorText = ref('')

const authorizeUrl = computed(() => {
  if (!cfg) return ''
  try {
    return buildAuthorizeUrl(cfg, 'preview_state')
  } catch (e) {
    return ''
  }
})

const tokenMasked = computed(() => {
  const t = token.value || ''
  if (!t) return '-'
  if (t.length <= 24) return t
  return `${t.slice(0, 18)}...${t.slice(-10)}`
})

const copyText = async (key: string, text: string) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    copiedKey.value = key
    window.setTimeout(() => {
      if (copiedKey.value === key) copiedKey.value = ''
    }, 1000)
  } catch (e) {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    copiedKey.value = key
    window.setTimeout(() => {
      if (copiedKey.value === key) copiedKey.value = ''
    }, 1000)
  }
}

const goAuthorize = () => {
  errorText.value = ''
  if (!cfg) return
  const state = generateState()
  storeState(state)
  const url = buildAuthorizeUrl(cfg, state)
  window.location.href = url
}

const goMe = () => {
  router.push({name: 'me'})
}

const backToConfig = () => {
  router.push({name: 'config'})
}

const logout = () => {
  localStorage.removeItem('sample-oauth2-authorize.accessToken')
  token.value = ''
}
</script>

<template>
  <div class="page">
    <div class="hero">
      <div>
        <div class="title">oauth2/authorize 演示</div>
        <div class="subtitle">跳转授权 → 回调带 code → 后端 /api/oauth2/login 落地 token</div>
      </div>
      <div class="actions">
        <button class="ghost" @click="backToConfig">返回配置</button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">当前状态</div>
      <div class="grid2">
        <div class="kv">
          <div class="kv-title">authorize URL（预览）</div>
          <div class="codebox">
            <div class="mono">{{ authorizeUrl || '-' }}</div>
            <div class="code-actions">
              <button class="ghost" :disabled="!authorizeUrl" @click="copyText('authorize', authorizeUrl)">复制</button>
              <span v-if="copiedKey === 'authorize'" class="tag">已复制</span>
            </div>
          </div>
        </div>
        <div class="kv">
          <div class="kv-title">accessToken</div>
          <div class="codebox">
            <div class="mono">{{ tokenMasked }}</div>
            <div class="code-actions">
              <button class="ghost" :disabled="!token" @click="copyText('token', token)">复制 token</button>
              <span v-if="copiedKey === 'token'" class="tag">已复制</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="errorText" class="error">{{ errorText }}</div>
      <div class="actions">
        <button class="primary" @click="goAuthorize">跳转授权登录</button>
        <button class="ghost" @click="goMe">查看当前用户</button>
        <button class="ghost" @click="logout">清理本地 token</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 1040px; margin: 0 auto; padding: 28px 18px 60px; }
.hero { padding: 18px 18px 14px; border: 1px solid var(--border); border-radius: 16px; background: linear-gradient(135deg, var(--accent-bg), transparent); box-shadow: var(--shadow); display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.title { font-size: 22px; font-weight: 650; color: var(--text-h); }
.subtitle { margin-top: 8px; font-size: 13px; opacity: 0.8; }
.card { margin-top: 18px; padding: 16px 16px 18px; border: 1px solid var(--border); border-radius: 16px; background: var(--bg); box-shadow: var(--shadow); }
.card-title { font-size: 14px; font-weight: 650; color: var(--text-h); opacity: 0.9; margin-bottom: 10px; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 14px; }
button { padding: 9px 12px; border-radius: 12px; border: 1px solid var(--border); background: transparent; cursor: pointer; color: var(--text-h); }
button:disabled { opacity: 0.5; cursor: not-allowed; }
button.primary { background: var(--accent); border-color: var(--accent); color: #fff; }
button.ghost { padding: 7px 10px; border-radius: 10px; }
.error { margin-top: 10px; padding: 10px 12px; border-radius: 12px; border: 1px solid rgba(185, 28, 28, 0.35); color: #b91c1c; background: rgba(185, 28, 28, 0.08); }
.mono { font-family: var(--mono); font-size: 12px; overflow-wrap: anywhere; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.kv-title { font-size: 12px; opacity: 0.8; margin-bottom: 8px; }
.codebox { border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: var(--code-bg); display: grid; gap: 10px; }
.code-actions { display: flex; gap: 10px; align-items: center; }
.tag { font-size: 12px; padding: 3px 8px; border-radius: 999px; border: 1px solid var(--accent-border); background: var(--accent-bg); color: var(--accent); }

@media (max-width: 860px) {
  .grid2 { grid-template-columns: 1fr; }
  .hero { flex-direction: column; }
}
</style>
