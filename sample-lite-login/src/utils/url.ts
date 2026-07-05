import type {LiteLoginConfig} from './configStorage'

export type LiteLoginDisplayMode = 'embedded' | 'popup' | 'page'

const createUrl = (input: string): URL => {
  try {
    return new URL(input)
  } catch (e) {
    return new URL(input, window.location.origin)
  }
}

export const buildLiteLoginUrl = (cfg: LiteLoginConfig, display: LiteLoginDisplayMode): string => {
  const url = createUrl(cfg.liteLoginBaseUrl)
  url.searchParams.set('display', display)
  if (cfg.redirect) url.searchParams.set('redirect', cfg.redirect)
  if (cfg.tenantCode) url.searchParams.set('tenantCode', cfg.tenantCode)
  if (cfg.appId) url.searchParams.set('appId', cfg.appId)
  return url.toString()
}

