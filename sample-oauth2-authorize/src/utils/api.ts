export interface LoginRes {
  token?: string
  accessToken?: string
  refreshToken?: string
  expireInSeconds?: number
  tokenType?: string
  issuer?: string
}

const unwrapApiResult = <T>(raw: unknown): T => {
  if (!raw || typeof raw !== 'object') return raw as T
  const obj = raw as Record<string, unknown>
  if (!('data' in obj) || !('code' in obj)) return raw as T

  const code = obj.code as unknown
  const ok = code === 20000 || code === 200 || code === '200' || code === '20000'
  if (ok) return obj.data as T

  const msg = (obj.msg || obj.message || `请求失败(code=${String(code)})`) as string
  throw new Error(msg)
}

const requestJson = async <T>(input: RequestInfo | URL, init: RequestInit): Promise<T> => {
  const resp = await fetch(input, init)
  const text = await resp.text()
  const json = text ? JSON.parse(text) : {}
  return unwrapApiResult<T>(json)
}

export const oauthLoginWithCode = async (apiBase: string, oauthLoginEndpoint: string, code: string): Promise<LoginRes> => {
  const url = new URL(oauthLoginEndpoint, apiBase)
  url.searchParams.set('code', code)
  return requestJson<LoginRes>(url, {method: 'POST'})
}

export const postMe = async (apiBase: string, meEndpoint: string, accessToken: string): Promise<unknown> => {
  const url = new URL(meEndpoint, apiBase)
  return requestJson<unknown>(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  })
}

