export interface LiteLoginConfig {
  liteLoginBaseUrl: string
  liteSsoOrigin: string
  tenantCode?: string
  appId?: string
  redirect?: string
  apiBase?: string
  meEndpoint?: string
}

const STORAGE_KEY = 'sample-lite-login.config'

export const normalizeOrigin = (input: string): string => {
  const trimmed = input.trim()
  if (!trimmed) return ''
  try {
    return new URL(trimmed).origin
  } catch (e) {
    try {
      return new URL(trimmed, window.location.origin).origin
    } catch (e2) {
      return ''
    }
  }
}

export const resolveOriginFromBaseUrl = (baseUrl: string): string => {
  const trimmed = baseUrl.trim()
  if (!trimmed) return ''
  try {
    return new URL(trimmed).origin
  } catch (e) {
    try {
      return new URL(trimmed, window.location.origin).origin
    } catch (e2) {
      return ''
    }
  }
}

export const loadLiteLoginConfig = (): LiteLoginConfig | null => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const obj = JSON.parse(raw) as Partial<LiteLoginConfig>
    const liteLoginBaseUrl = (obj.liteLoginBaseUrl || '').trim()
    const liteSsoOrigin = normalizeOrigin(obj.liteSsoOrigin || '') || resolveOriginFromBaseUrl(liteLoginBaseUrl)
    if (!liteLoginBaseUrl || !liteSsoOrigin) return null
    return {
      liteLoginBaseUrl,
      liteSsoOrigin,
      tenantCode: obj.tenantCode?.trim() || undefined,
      appId: obj.appId?.trim() || undefined,
      redirect: obj.redirect?.trim() || undefined,
      apiBase: obj.apiBase?.trim() || undefined,
      meEndpoint: obj.meEndpoint?.trim() || undefined
    }
  } catch (e) {
    return null
  }
}

export const saveLiteLoginConfig = (cfg: LiteLoginConfig) => {
  const liteLoginBaseUrl = cfg.liteLoginBaseUrl.trim()
  const liteSsoOrigin = resolveOriginFromBaseUrl(liteLoginBaseUrl)
  const normalized: LiteLoginConfig = {
    liteLoginBaseUrl,
    liteSsoOrigin,
    tenantCode: cfg.tenantCode?.trim() || undefined,
    appId: cfg.appId?.trim() || undefined,
    redirect: cfg.redirect?.trim() || undefined,
    apiBase: cfg.apiBase?.trim() || undefined,
    meEndpoint: cfg.meEndpoint?.trim() || undefined
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
}

export const clearLiteLoginConfig = () => {
  localStorage.removeItem(STORAGE_KEY)
}

export const hasLiteLoginConfig = (): boolean => {
  return loadLiteLoginConfig() !== null
}
