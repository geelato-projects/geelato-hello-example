<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {clearOAuth2Config, loadOAuth2Config, saveOAuth2Config} from '../utils/configStorage'
import {buildAuthorizeUrl, buildRedirectUri} from '../utils/oauth'

const router = useRouter()
const errorText = ref('')
const copiedKey = ref('')

const fromStorage = loadOAuth2Config()

const form = reactive({
  apiBase: fromStorage?.apiBase || '',
  ssoAuthorizeUrl: fromStorage?.ssoAuthorizeUrl || '',
  authCenterOrigin: fromStorage?.authCenterOrigin || '',
  clientId: fromStorage?.clientId || '',
  redirectPath: fromStorage?.redirectPath || '/oauth/callback',
  oauthLoginEndpoint: fromStorage?.oauthLoginEndpoint || '/api/oauth2/login',
  meEndpoint: fromStorage?.meEndpoint || '/api/user/info'
})

const redirectUri = computed(() => buildRedirectUri({
  apiBase: form.apiBase,
  ssoAuthorizeUrl: form.ssoAuthorizeUrl || undefined,
  authCenterOrigin: form.authCenterOrigin || undefined,
  clientId: form.clientId || undefined,
  redirectPath: form.redirectPath,
  oauthLoginEndpoint: form.oauthLoginEndpoint,
  meEndpoint: form.meEndpoint
}))

const previewAuthorizeUrl = computed(() => {
  const apiBase = form.apiBase.trim()
  const ssoAuthorizeUrl = form.ssoAuthorizeUrl.trim()
  const authCenterOrigin = form.authCenterOrigin.trim()
  const clientId = form.clientId.trim()
  if (!apiBase) return ''
  if (!ssoAuthorizeUrl && (!authCenterOrigin || !clientId)) return ''
  try {
    return buildAuthorizeUrl({
      apiBase,
      ssoAuthorizeUrl: ssoAuthorizeUrl || undefined,
      authCenterOrigin: authCenterOrigin || undefined,
      clientId: clientId || undefined,
      redirectPath: form.redirectPath,
      oauthLoginEndpoint: form.oauthLoginEndpoint,
      meEndpoint: form.meEndpoint
    }, 'preview_state')
  } catch (e) {
    return ''
  }
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

  const apiBase = form.apiBase.trim()
  const ssoAuthorizeUrl = form.ssoAuthorizeUrl.trim()
  const authCenterOrigin = form.authCenterOrigin.trim()
  const clientId = form.clientId.trim()

  if (!apiBase) {
    errorText.value = '请填写 apiBase（后端宿主基地址）'
    return
  }
  if (!ssoAuthorizeUrl && (!authCenterOrigin || !clientId)) {
    errorText.value = '请填写 ssoAuthorizeUrl 或 authCenterOrigin + clientId'
    return
  }

  saveOAuth2Config({
    apiBase,
    ssoAuthorizeUrl: ssoAuthorizeUrl || undefined,
    authCenterOrigin: authCenterOrigin || undefined,
    clientId: clientId || undefined,
    redirectPath: form.redirectPath,
    oauthLoginEndpoint: form.oauthLoginEndpoint,
    meEndpoint: form.meEndpoint
  })
  router.push({name: 'home'})
}

const reset = () => {
  clearOAuth2Config()
  form.apiBase = ''
  form.ssoAuthorizeUrl = ''
  form.authCenterOrigin = ''
  form.clientId = ''
  form.redirectPath = '/oauth/callback'
  form.oauthLoginEndpoint = '/api/oauth2/login'
  form.meEndpoint = '/api/user/info'
  errorText.value = ''
}
</script>

<template>
  <div class="page">
    <div class="hero">
      <div class="title">sample-oauth2-authorize</div>
      <div class="subtitle">演示 OAuth2 授权码：跳转 /oauth2/authorize → 回跳 code → 调用 /api/oauth2/login 落地 token</div>
    </div>

    <div class="card">
      <div class="card-title">集成配置</div>
      <div class="row">
        <label>apiBase *</label>
        <div>
          <input v-model="form.apiBase" placeholder="http://localhost:8080" />
          <div class="help">
            宿主后端基地址。回调页会调用：POST {apiBase}{oauthLoginEndpoint}?code=... 换取 token；/me 页会调用：POST {apiBase}{meEndpoint} 获取当前用户。
          </div>
        </div>
      </div>

      <div class="row">
        <label>ssoAuthorizeUrl</label>
        <div>
          <input v-model="form.ssoAuthorizeUrl" placeholder="https://auth.example.com/oauth2/authorize?..." />
          <div class="help">
            OAuth2 授权入口完整 URL（推荐：直接填写后端运行时 .config 中的 ssoUrl）。示例会自动补齐 redirect_uri 与 state。
          </div>
        </div>
      </div>

      <div class="row">
        <label>authCenterOrigin</label>
        <div>
          <input v-model="form.authCenterOrigin" placeholder="https://auth.example.com" />
          <div class="help">
            当不填写 ssoAuthorizeUrl 时使用：示例会拼出 {authCenterOrigin}/oauth2/authorize 作为授权入口（并补齐参数）。
          </div>
        </div>
      </div>

      <div class="row">
        <label>clientId</label>
        <div>
          <input v-model="form.clientId" placeholder="geelato" />
          <div class="help">OAuth2 client_id。仅在使用 authCenterOrigin 拼接授权入口时必填。</div>
        </div>
      </div>

      <div class="row">
        <label>redirectPath</label>
        <div>
          <input v-model="form.redirectPath" placeholder="/oauth/callback" />
          <div class="help">本示例前端回调路由路径。认证中心登录成功后会带 code 回跳到 computed 的 redirect_uri。</div>
          <div class="hint">
            redirect_uri：<span class="mono">{{ redirectUri }}</span>
            <button class="ghost" @click="copyText('redirect', redirectUri)">复制</button>
            <span v-if="copiedKey === 'redirect'" class="tag">已复制</span>
          </div>
        </div>
      </div>

      <div class="row">
        <label>oauthLoginEndpoint</label>
        <div>
          <input v-model="form.oauthLoginEndpoint" placeholder="/api/oauth2/login" />
          <div class="help">
            宿主后端的“用 code 换 token”接口路径。示例会调用 POST {apiBase}{oauthLoginEndpoint}?code=...（对应仓库常用 /api/oauth2/login）。
          </div>
        </div>
      </div>

      <div class="row">
        <label>meEndpoint</label>
        <div>
          <input v-model="form.meEndpoint" placeholder="/api/user/info" />
          <div class="help">宿主后端“当前用户”接口路径。/me 页会带 Bearer token 调用它验证登录态。</div>
        </div>
      </div>

      <div v-if="errorText" class="error">{{ errorText }}</div>

      <div class="actions">
        <button class="primary" @click="submit">确认</button>
        <button @click="reset">重置配置</button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">授权地址预览</div>
      <div class="codebox">
        <div class="mono">{{ previewAuthorizeUrl || '-' }}</div>
        <div class="code-actions">
          <button class="ghost" :disabled="!previewAuthorizeUrl" @click="copyText('authorize', previewAuthorizeUrl)">复制</button>
          <span v-if="copiedKey === 'authorize'" class="tag">已复制</span>
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
button.ghost { padding: 7px 10px; border-radius: 10px; }
.error { margin-top: 10px; padding: 10px 12px; border-radius: 12px; border: 1px solid rgba(185, 28, 28, 0.35); color: #b91c1c; background: rgba(185, 28, 28, 0.08); }
.mono { font-family: var(--mono); font-size: 12px; overflow-wrap: anywhere; }
.hint { margin-top: 8px; font-size: 12px; opacity: 0.8; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.help { margin-top: 7px; font-size: 12px; opacity: 0.78; line-height: 1.55; }
.codebox { border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: var(--code-bg); display: grid; gap: 10px; }
.code-actions { display: flex; gap: 10px; align-items: center; }
.tag { font-size: 12px; padding: 3px 8px; border-radius: 999px; border: 1px solid var(--accent-border); background: var(--accent-bg); color: var(--accent); }

@media (max-width: 860px) {
  .row { grid-template-columns: 1fr; }
  label { padding-top: 0; }
}
</style>
