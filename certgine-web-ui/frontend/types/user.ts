export interface UserInfo {
  email: string
  admin: boolean
  groups: string[]
}

export interface SessionInfo {
  id: number
  created: string
  expires: string
}

export interface LoginResponse {
  token?: string
  totpRequired?: boolean
  webauthnRequired?: boolean
}
