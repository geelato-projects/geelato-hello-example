export interface OAuth2AuthorizeConfig {
  apiBase: string
  ssoAuthorizeUrl?: string
  authCenterOrigin?: string
  clientId?: string
  redirectPath: string
  oauthLoginEndpoint: string
  meEndpoint: string
}

const STORAGE_KEY = 'sample-oauth2-authorize.config'

const normalizeUrl = (input: string): string => {
  const trimmed = input.trim()
  if (!trimmed) return ''
  try {
    return new URL(trimmed).toString()
  } catch (e) {
    try {
      return new URL(trimmed, window.location.origin).toString()
    } catch (e2) {
      return ''
    }
  }
}

const normalizeOrigin = (input: string): string => {
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

export const loadOAuth2Config = (): OAuth2AuthorizeConfig | null => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const obj = JSON.parse(raw) as Partial<OAuth2AuthorizeConfig>
    const apiBase = normalizeOrigin(obj.apiBase || '')
    const redirectPath = (obj.redirectPath || '/oauth/callback').trim() || '/oauth/callback'
    const oauthLoginEndpoint = (obj.oauthLoginEndpoint || '/api/oauth2/login').trim() || '/api/oauth2/login'
    const meEndpoint = (obj.meEndpoint || '/api/user/info').trim() || '/api/user/info'

    const ssoAuthorizeUrl = obj.ssoAuthorizeUrl ? normalizeUrl(obj.ssoAuthorizeUrl) : ''
    const authCenterOrigin = obj.authCenterOrigin ? normalizeOrigin(obj.authCenterOrigin) : ''
    const clientId = (obj.clientId || '').trim()

    const hasAuthorize = !!ssoAuthorizeUrl || (!!authCenterOrigin && !!clientId)
    if (!apiBase || !hasAuthorize) return null

    return {
      apiBase,
      ssoAuthorizeUrl: ssoAuthorizeUrl || undefined,
      authCenterOrigin: authCenterOrigin || undefined,
      clientId: clientId || undefined,
      redirectPath,
      oauthLoginEndpoint,
      meEndpoint
    }
  } catch (e) {
    return null
  }
}

export const saveOAuth2Config = (cfg: OAuth2AuthorizeConfig) => {
  const apiBase = normalizeOrigin(cfg.apiBase)
  const redirectPath = (cfg.redirectPath || '/oauth/callback').trim() || '/oauth/callback'
  const oauthLoginEndpoint = (cfg.oauthLoginEndpoint || '/api/oauth2/login').trim() || '/api/oauth2/login'
  const meEndpoint = (cfg.meEndpoint || '/api/user/info').trim() || '/api/user/info'

  const ssoAuthorizeUrl = cfg.ssoAuthorizeUrl ? normalizeUrl(cfg.ssoAuthorizeUrl) : ''
  const authCenterOrigin = cfg.authCenterOrigin ? normalizeOrigin(cfg.authCenterOrigin) : ''
  const clientId = (cfg.clientId || '').trim()

  const normalized: OAuth2AuthorizeConfig = {
    apiBase,
    ssoAuthorizeUrl: ssoAuthorizeUrl || undefined,
    authCenterOrigin: authCenterOrigin || undefined,
    clientId: clientId || undefined,
    redirectPath,
    oauthLoginEndpoint,
    meEndpoint
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
}

export const clearOAuth2Config = () => {
  localStorage.removeItem(STORAGE_KEY)
}

export const hasOAuth2Config = (): boolean => {
  return loadOAuth2Config() !== null
}

