import 'vite/client'

interface ImportMetaEnv {
  readonly VITE_LITE_LOGIN_BASE_URL?: string
  readonly VITE_LITE_SSO_ORIGIN?: string
  readonly VITE_API_BASE?: string
  readonly VITE_ME_ENDPOINT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

export {}
