<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {clearLiteLoginConfig, loadLiteLoginConfig, resolveOriginFromBaseUrl, saveLiteLoginConfig} from '../utils/configStorage'
import {buildLiteLoginUrl} from '../utils/url'

const router = useRouter()
const errorText = ref('')
const copiedKey = ref('')

const defaultRedirect = `${window.location.origin}/demo`

const fromStorage = loadLiteLoginConfig()
const form = reactive({
  liteLoginBaseUrl: fromStorage?.liteLoginBaseUrl || '',
  tenantCode: fromStorage?.tenantCode || '',
  appId: fromStorage?.appId || '',
  redirect: fromStorage?.redirect || defaultRedirect,
  apiBase: fromStorage?.apiBase || '',
  meEndpoint: fromStorage?.meEndpoint || '/api/user/info'
})

const derivedLiteSsoOrigin = computed(() => resolveOriginFromBaseUrl(form.liteLoginBaseUrl))

const previewConfig = computed(() => ({
  liteLoginBaseUrl: form.liteLoginBaseUrl.trim(),
  liteSsoOrigin: derivedLiteSsoOrigin.value,
  tenantCode: form.tenantCode.trim() || undefined,
  appId: form.appId.trim() || undefined,
  redirect: form.redirect.trim() || undefined,
  apiBase: form.apiBase.trim() || undefined,
  meEndpoint: form.meEndpoint.trim() || undefined
}))

const embeddedUrl = computed(() => {
  if (!previewConfig.value.liteLoginBaseUrl || !previewConfig.value.liteSsoOrigin) return ''
  return buildLiteLoginUrl(previewConfig.value, 'embedded')
})

const popupUrl = computed(() => {
  if (!previewConfig.value.liteLoginBaseUrl || !previewConfig.value.liteSsoOrigin) return ''
  return buildLiteLoginUrl(previewConfig.value, 'popup')
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

const submit = () => {
  errorText.value = ''
  const liteLoginBaseUrl = form.liteLoginBaseUrl.trim()
  const liteSsoOrigin = derivedLiteSsoOrigin.value

  if (!liteLoginBaseUrl) {
    errorText.value = '请填写 liteLoginBaseUrl'
    return
  }
  if (!liteSsoOrigin) {
    errorText.value = 'liteLoginBaseUrl 无法解析 origin，请填写完整 URL（例如 http://localhost:1220/lite-login 或 http://localhost:1220/login）'
    return
  }

  saveLiteLoginConfig({
    liteLoginBaseUrl,
    liteSsoOrigin,
    tenantCode: form.tenantCode.trim() || undefined,
    appId: form.appId.trim() || undefined,
    redirect: form.redirect.trim() || undefined,
    apiBase: form.apiBase.trim() || undefined,
    meEndpoint: form.meEndpoint.trim() || undefined
  })
  router.push('/demo')
}

const reset = () => {
  clearLiteLoginConfig()
  form.liteLoginBaseUrl = ''
  form.tenantCode = ''
  form.appId = ''
  form.redirect = defaultRedirect
  form.apiBase = ''
  form.meEndpoint = '/api/user/info'
  errorText.value = ''
}
</script>

<template>
  <div class="page">
    <div class="hero">
      <div class="title">sample-lite-login</div>
      <div class="subtitle">第三方应用集成 lite-login（iframe + popup），并接收 LOGIN_SUCCESS(accessToken,...)</div>
    </div>

    <div class="card">
      <div class="card-title">集成配置</div>
      <div class="row">
        <label>liteLoginBaseUrl *</label>
        <div>
          <input v-model="form.liteLoginBaseUrl" placeholder="http://host/lite-login 或 http://host/login" />
          <div class="help">
            lite-login（或独立 lite-sso）的登录页入口，不包含 display 参数。示例会基于它拼出 embedded/popup 两种地址。
          </div>
          <div class="hint">
            派生 liteSsoOrigin（用于校验 postMessage 来源）：<span class="mono">{{ derivedLiteSsoOrigin || '-' }}</span>
            <button class="ghost" :disabled="!derivedLiteSsoOrigin" @click="copyText('origin', derivedLiteSsoOrigin)">复制</button>
            <span v-if="copiedKey === 'origin'" class="tag">已复制</span>
          </div>
        </div>
      </div>

      <div class="row">
        <label>tenantCode</label>
        <div>
          <input v-model="form.tenantCode" placeholder="可选" />
          <div class="help">透传给 lite-login 的租户编码参数（tenantCode），用于多租户场景。</div>
        </div>
      </div>

      <div class="row">
        <label>appId</label>
        <div>
          <input v-model="form.appId" placeholder="可选" />
          <div class="help">透传给 lite-login 的应用标识参数（appId），用于标识来源应用。</div>
        </div>
      </div>

      <div class="row">
        <label>redirect</label>
        <div>
          <input v-model="form.redirect" placeholder="可选" />
          <div class="help">
            透传给 lite-login 的 redirect 参数。部分实现会在 page 模式下登录成功后跳回该地址；对 embedded/popup 主要依赖 postMessage。
          </div>
        </div>
      </div>

      <div class="row">
        <label>apiBase</label>
        <div>
          <input v-model="form.apiBase" placeholder="可选，例如 http://localhost:8080" />
          <div class="help">
            第三方应用后端基地址。演示页点击“调用当前用户接口”会向该地址发请求，并携带 Authorization: Bearer &lt;accessToken&gt;。
          </div>
        </div>
      </div>

      <div class="row">
        <label>meEndpoint</label>
        <div>
          <input v-model="form.meEndpoint" placeholder="可选，默认 /api/user/info" />
          <div class="help">当前用户接口路径（相对 apiBase）。用于演示“拿到 token 后由业务后端确认用户”。</div>
        </div>
      </div>

      <div v-if="errorText" class="error">{{ errorText }}</div>

      <div class="actions">
        <button class="primary" @click="submit">确认</button>
        <button @click="reset">重置配置</button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">地址预览</div>
      <div class="grid2">
        <div class="kv">
          <div class="kv-title">embedded</div>
          <div class="codebox">
            <div class="mono">{{ embeddedUrl || '-' }}</div>
            <div class="code-actions">
              <button class="ghost" :disabled="!embeddedUrl" @click="copyText('embedded', embeddedUrl)">复制</button>
              <span v-if="copiedKey === 'embedded'" class="tag">已复制</span>
            </div>
          </div>
        </div>
        <div class="kv">
          <div class="kv-title">popup</div>
          <div class="codebox">
            <div class="mono">{{ popupUrl || '-' }}</div>
            <div class="code-actions">
              <button class="ghost" :disabled="!popupUrl" @click="copyText('popup', popupUrl)">复制</button>
              <span v-if="copiedKey === 'popup'" class="tag">已复制</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 1040px; margin: 0 auto; padding: 28px 18px 60px; }
.hero { padding: 18px 18px 14px; border: 1px solid var(--border); border-radius: 16px; background: linear-gradient(135deg, var(--accent-bg), transparent); box-shadow: var(--shadow); }
.title { font-size: 26px; font-weight: 650; color: var(--text-h); letter-spacing: -0.4px; }
.subtitle { margin-top: 8px; font-size: 14px; opacity: 0.8; }
.card { margin-top: 18px; padding: 16px 16px 18px; border: 1px solid var(--border); border-radius: 16px; background: var(--bg); box-shadow: var(--shadow); }
.card-title { font-size: 14px; font-weight: 650; color: var(--text-h); opacity: 0.9; margin-bottom: 10px; }
.row { display: grid; grid-template-columns: 180px 1fr; gap: 14px; align-items: start; padding: 10px 0; }
label { font-size: 13px; opacity: 0.85; padding-top: 10px; }
input { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 12px; font-size: 14px; background: transparent; color: var(--text-h); outline: none; }
input:focus { border-color: var(--accent-border); box-shadow: 0 0 0 4px var(--accent-bg); }
.actions { display: flex; gap: 12px; margin-top: 14px; flex-wrap: wrap; }
button { padding: 9px 12px; border-radius: 12px; border: 1px solid var(--border); background: transparent; cursor: pointer; color: var(--text-h); }
button:disabled { opacity: 0.5; cursor: not-allowed; }
button.primary { background: var(--accent); border-color: var(--accent); color: #fff; }
button.ghost { padding: 6px 10px; border-radius: 10px; }
.error { margin-top: 10px; padding: 10px 12px; border-radius: 12px; border: 1px solid rgba(185, 28, 28, 0.35); color: #b91c1c; background: rgba(185, 28, 28, 0.08); }
.mono { font-family: var(--mono); font-size: 12px; overflow-wrap: anywhere; }
.hint { margin-top: 8px; font-size: 12px; opacity: 0.8; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.help { margin-top: 7px; font-size: 12px; opacity: 0.78; line-height: 1.55; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.kv-title { font-size: 12px; opacity: 0.8; margin-bottom: 8px; }
.codebox { border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: var(--code-bg); display: grid; gap: 10px; }
.code-actions { display: flex; gap: 10px; align-items: center; }
.tag { font-size: 12px; padding: 3px 8px; border-radius: 999px; border: 1px solid var(--accent-border); background: var(--accent-bg); color: var(--accent); }

@media (max-width: 860px) {
  .row { grid-template-columns: 1fr; }
  label { padding-top: 0; }
  .grid2 { grid-template-columns: 1fr; }
}
</style>
