export type LiteSsoMessageType = 'LOGIN_SUCCESS' | 'LOGIN_CLOSE' | 'OAUTH2_LOGIN' | string

export interface LiteSsoMessage<T = unknown> {
  type: LiteSsoMessageType
  data?: T
}

export interface LiteSsoLoginSuccessData {
  accessToken?: string
  token?: string
  refreshToken?: string
  expireInSeconds?: number
  tokenType?: string
  issuer?: string
  user?: unknown
}

export const parseLiteSsoMessage = (raw: unknown): LiteSsoMessage | null => {
  if (!raw || typeof raw !== 'object') return null
  const obj = raw as Record<string, unknown>
  if (typeof obj.type !== 'string') return null
  return {type: obj.type, data: obj.data}
}

export const resolveAccessToken = (msg: LiteSsoMessage | null): string => {
  if (!msg) return ''
  if (msg.type !== 'LOGIN_SUCCESS') return ''
  const data = (msg.data || {}) as LiteSsoLoginSuccessData
  return (data.accessToken || data.token || '').trim()
}

