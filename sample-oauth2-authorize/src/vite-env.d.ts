import 'vite/client'

interface ImportMetaEnv {
  readonly VITE_API_BASE?: string
  readonly VITE_SSO_AUTHORIZE_URL?: string
  readonly VITE_AUTH_CENTER_ORIGIN?: string
  readonly VITE_CLIENT_ID?: string
  readonly VITE_REDIRECT_PATH?: string
  readonly VITE_OAUTH_LOGIN_ENDPOINT?: string
  readonly VITE_ME_ENDPOINT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

export {}
