<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {loadLiteLoginConfig} from '../utils/configStorage'
import {parseLiteSsoMessage, resolveAccessToken} from '../utils/postMessage'
import {buildLiteLoginUrl} from '../utils/url'

const router = useRouter()
const cfg = loadLiteLoginConfig()

if (!cfg) {
  router.replace({name: 'config'})
}

const embeddedSrc = ref('')
const iframeRef = ref<HTMLIFrameElement | null>(null)
const accessToken = ref(localStorage.getItem('sample-lite-login.accessToken') || '')
const lastMessage = ref('')
const meResult = ref('')
const copiedKey = ref('')
const toastText = ref('')
const meOpen = ref(false)

const embeddedUrl = computed(() => {
  if (!cfg) return ''
  return buildLiteLoginUrl(cfg, 'embedded')
})

const popupUrl = computed(() => {
  if (!cfg) return ''
  return buildLiteLoginUrl(cfg, 'popup')
})

const tokenMasked = computed(() => {
  const t = accessToken.value || ''
  if (!t) return '-'
  if (t.length <= 24) return t
  return `${t.slice(0, 18)}...${t.slice(-10)}`
})

const formatMaybeJson = (raw: string): string => {
  const text = (raw || '').trim()
  if (!text) return '-'
  try {
    const obj = JSON.parse(text)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return text
  }
}

const mePretty = computed(() => formatMaybeJson(meResult.value))
const lastPretty = computed(() => formatMaybeJson(lastMessage.value))

const openEmbedded = () => {
  embeddedSrc.value = embeddedUrl.value
}

let popupRef: Window | null = null

const openPopup = () => {
  const width = 420
  const height = 560
  const left = Math.max(0, (window.screen.width - width) / 2)
  const top = Math.max(0, (window.screen.height - height) / 2)
  popupRef = window.open(
    popupUrl.value,
    'LiteLoginPopup',
    `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=no`
  )
}

const closePopup = () => {
  try {
    popupRef?.close()
  } catch (e) {
  } finally {
    popupRef = null
  }
}

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

const showToast = (text: string) => {
  toastText.value = text
  window.setTimeout(() => {
    if (toastText.value === text) toastText.value = ''
  }, 2600)
}

const onMessage = (event: MessageEvent) => {
  if (!cfg) return
  if (event.origin !== cfg.liteSsoOrigin) return

  const msg = parseLiteSsoMessage(event.data)
  lastMessage.value = JSON.stringify({origin: event.origin, data: event.data}, null, 2)

  if (msg?.type === 'OAUTH2_LOGIN') {
    const url = (msg.data as any)?.url
    if (typeof url === 'string' && url) {
      window.open(url, 'OAuth2Login')
    }
    return
  }

  if (msg?.type === 'LOGIN_CLOSE') {
    closePopup()
    return
  }

  const token = resolveAccessToken(msg)
  if (token) {
    accessToken.value = token
    localStorage.setItem('sample-lite-login.accessToken', token)
    closePopup()
    if (embeddedSrc.value) {
      embeddedSrc.value = ''
    }
    showToast('认证通过')
  }
}

const sendLoginInit = () => {
  if (!cfg) return
  const win = iframeRef.value?.contentWindow
  if (!win) return
  win.postMessage({type: 'LOGIN_INIT'}, cfg.liteSsoOrigin)
}

const callMe = async () => {
  meResult.value = ''
  meOpen.value = false
  const base = (cfg?.apiBase || '').trim()
  const endpoint = (cfg?.meEndpoint || '/api/user/info').trim()
  if (!base) {
    meResult.value = '未配置 apiBase（在配置页填写）'
    meOpen.value = true
    return
  }
  if (!accessToken.value) {
    meResult.value = '未获取到 accessToken'
    meOpen.value = true
    return
  }
  const url = new URL(endpoint, base).toString()
  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken.value}`
    }
  })
  meResult.value = await resp.text()
  meOpen.value = true
}

const backToConfig = () => {
  router.push({name: 'config'})
}

const closeMe = () => {
  meOpen.value = false
}

onMounted(() => {
  window.addEventListener('message', onMessage)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', onMessage)
  closePopup()
})
</script>

<template>
  <div class="page">
    <div v-if="toastText" class="toast">{{ toastText }}</div>
    <div v-if="meOpen" class="modal-mask" @click="closeMe">
      <div class="modal" @click.stop>
        <div class="modal-head">
          <div class="modal-title">当前用户接口响应</div>
          <button class="ghost" @click="closeMe">关闭</button>
        </div>
        <div class="modal-sub">
          <span class="mono">POST</span>
          <span class="mono">{{ cfg?.apiBase || '-' }}{{ cfg?.meEndpoint || '-' }}</span>
        </div>
        <pre class="mono pre modal-pre">{{ mePretty }}</pre>
      </div>
    </div>
    <div class="hero">
      <div class="hero-left">
        <div class="title">lite-login 演示</div>
        <div class="subtitle">
          预期消息来源 origin：<span class="mono">{{ cfg?.liteSsoOrigin || '-' }}</span>
        </div>
      </div>
      <div class="hero-right">
        <button class="ghost" @click="backToConfig">返回配置</button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">集成地址</div>
      <div class="grid2">
        <div class="kv">
          <div class="kv-title">embedded</div>
          <div class="codebox">
            <div class="mono">{{ embeddedUrl }}</div>
            <div class="code-actions">
              <button class="ghost" @click="copyText('embedded', embeddedUrl)">复制</button>
              <span v-if="copiedKey === 'embedded'" class="tag">已复制</span>
            </div>
          </div>
        </div>
        <div class="kv">
          <div class="kv-title">popup</div>
          <div class="codebox">
            <div class="mono">{{ popupUrl }}</div>
            <div class="code-actions">
              <button class="ghost" @click="copyText('popup', popupUrl)">复制</button>
              <span v-if="copiedKey === 'popup'" class="tag">已复制</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">演示操作</div>
      <div class="actions">
        <button class="primary" @click="openEmbedded">加载 iframe</button>
        <button class="primary" @click="openPopup">打开 popup</button>
        <button class="ghost" @click="callMe">调用当前用户接口</button>
      </div>

      <div class="split">
        <div class="metric">
          <div class="metric-title">accessToken</div>
          <div class="metric-value mono">{{ tokenMasked }}</div>
          <div class="metric-actions">
            <button class="ghost" :disabled="!accessToken" @click="copyText('token', accessToken)">复制 token</button>
            <span v-if="copiedKey === 'token'" class="tag">已复制</span>
          </div>
        </div>
        <div class="metric">
          <div class="metric-title">apiBase / meEndpoint</div>
          <div class="metric-value mono">{{ cfg?.apiBase || '-' }}</div>
          <div class="metric-sub mono">{{ cfg?.meEndpoint || '-' }}</div>
          <div class="metric-actions">
            <button class="ghost" :disabled="!meResult" @click="meOpen = true">查看响应</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">iframe</div>
      <div class="iframe-wrap">
        <iframe
          v-if="embeddedSrc"
          ref="iframeRef"
          :src="embeddedSrc"
          frameborder="0"
          @load="sendLoginInit"
        />
        <div v-else class="placeholder">点击“加载 iframe”后渲染</div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">最近一次 postMessage</div>
      <pre class="mono pre">{{ lastPretty }}</pre>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 1040px; margin: 0 auto; padding: 28px 18px 60px; }
.toast { position: fixed; top: 18px; left: 50%; transform: translateX(-50%); padding: 12px 16px; border-radius: 999px; border: 1px solid rgba(34, 197, 94, 0.55); background: rgba(34, 197, 94, 0.15); color: var(--text-h); box-shadow: 0 10px 30px rgba(0,0,0,0.18); z-index: 40; font-size: 14px; font-weight: 650; letter-spacing: 0.2px; display: inline-flex; align-items: center; gap: 10px; animation: toast-in 160ms ease-out; }
.toast::before { content: ''; width: 10px; height: 10px; border-radius: 999px; background: rgba(34, 197, 94, 0.9); box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.18); }
@keyframes toast-in { from { transform: translateX(-50%) translateY(-8px); opacity: 0; } to { transform: translateX(-50%) translateY(0); opacity: 1; } }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 30; display: flex; align-items: center; justify-content: center; padding: 18px; }
.modal { width: min(980px, 100%); max-height: min(82vh, 980px); overflow: hidden; border: 1px solid var(--border); border-radius: 16px; background: var(--bg); box-shadow: var(--shadow); display: flex; flex-direction: column; }
.modal-head { padding: 14px 14px 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; border-bottom: 1px solid var(--border); }
.modal-title { font-size: 14px; font-weight: 650; color: var(--text-h); }
.modal-sub { padding: 10px 14px 0; font-size: 12px; opacity: 0.8; display: flex; gap: 10px; flex-wrap: wrap; }
.modal-pre { margin: 12px 14px 14px; max-height: 60vh; }
.hero { padding: 18px 18px 14px; border: 1px solid var(--border); border-radius: 16px; background: linear-gradient(135deg, var(--accent-bg), transparent); box-shadow: var(--shadow); display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.title { font-size: 22px; font-weight: 650; color: var(--text-h); }
.subtitle { margin-top: 8px; font-size: 13px; opacity: 0.8; }
.card { margin-top: 18px; padding: 16px 16px 18px; border: 1px solid var(--border); border-radius: 16px; background: var(--bg); box-shadow: var(--shadow); }
.card-title { font-size: 14px; font-weight: 650; color: var(--text-h); opacity: 0.9; margin-bottom: 10px; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; }
button { padding: 9px 12px; border-radius: 12px; border: 1px solid var(--border); background: transparent; cursor: pointer; color: var(--text-h); }
button:disabled { opacity: 0.5; cursor: not-allowed; }
button.primary { background: var(--accent); border-color: var(--accent); color: #fff; }
button.ghost { padding: 7px 10px; border-radius: 10px; }
.mono { font-family: var(--mono); font-size: 12px; overflow-wrap: anywhere; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.kv-title { font-size: 12px; opacity: 0.8; margin-bottom: 8px; }
.codebox { border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: var(--code-bg); display: grid; gap: 10px; }
.code-actions { display: flex; gap: 10px; align-items: center; }
.tag { font-size: 12px; padding: 3px 8px; border-radius: 999px; border: 1px solid var(--accent-border); background: var(--accent-bg); color: var(--accent); }
.split { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 14px; }
.metric { border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: rgba(0,0,0,0.02); }
.metric-title { font-size: 12px; opacity: 0.75; }
.metric-value { margin-top: 8px; font-size: 13px; color: var(--text-h); }
.metric-sub { margin-top: 6px; opacity: 0.75; }
.metric-actions { margin-top: 10px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.iframe-wrap { border: 1px solid var(--border); border-radius: 16px; overflow: hidden; background: var(--code-bg); min-height: 520px; display: flex; align-items: center; justify-content: center; }
iframe { width: 100%; height: 520px; border: 0; background: var(--bg); }
.placeholder { opacity: 0.7; font-size: 13px; padding: 24px; }
.pre { margin: 0; white-space: pre-wrap; border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: var(--code-bg); overflow: auto; max-height: 360px; }

@media (max-width: 860px) {
  .grid2 { grid-template-columns: 1fr; }
  .split { grid-template-columns: 1fr; }
  .hero { flex-direction: column; }
  iframe { height: 480px; }
  .iframe-wrap { min-height: 480px; }
}
</style>
