import type {OAuth2AuthorizeConfig} from './configStorage'

const STATE_KEY = 'sample-oauth2-authorize.state'

export const generateState = (): string => {
  const arr = new Uint8Array(16)
  crypto.getRandomValues(arr)
  return Array.from(arr, b => b.toString(16).padStart(2, '0')).join('')
}

export const storeState = (state: string) => {
  sessionStorage.setItem(STATE_KEY, state)
}

export const consumeState = (): string => {
  const state = sessionStorage.getItem(STATE_KEY) || ''
  sessionStorage.removeItem(STATE_KEY)
  return state
}

export const buildRedirectUri = (cfg: OAuth2AuthorizeConfig): string => {
  const path = cfg.redirectPath?.trim() || '/oauth/callback'
  return new URL(path, window.location.origin).toString()
}

export const buildAuthorizeUrl = (cfg: OAuth2AuthorizeConfig, state: string): string => {
  const redirectUri = buildRedirectUri(cfg)

  const url = cfg.ssoAuthorizeUrl
    ? new URL(cfg.ssoAuthorizeUrl)
    : new URL('/oauth2/authorize', cfg.authCenterOrigin)

  url.searchParams.set('response_type', 'code')
  if (cfg.clientId) url.searchParams.set('client_id', cfg.clientId)
  url.searchParams.set('redirect_uri', redirectUri)
  url.searchParams.set('state', state)

  return url.toString()
}

