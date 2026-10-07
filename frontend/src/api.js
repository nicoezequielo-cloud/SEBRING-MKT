const LS_TOKEN = 'sebring_token'
const LS_USER = 'sebring_user'

export function getToken() {
  return localStorage.getItem(LS_TOKEN)
}

export function setToken(token) {
  if (token) localStorage.setItem(LS_TOKEN, token)
  else localStorage.removeItem(LS_TOKEN)
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(LS_USER))
  } catch {
    return null
  }
}

export function setUser(user) {
  localStorage.setItem(LS_USER, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(LS_TOKEN)
  localStorage.removeItem(LS_USER)
}

let authListener = null

export function onAuthExpired(cb) {
  authListener = cb
}

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function parseResponse(res) {
  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }
  return { ok: res.ok, status: res.status, statusText: res.statusText, data }
}

export async function api(method, url, body) {
  const opts = { method, headers: { ...authHeaders() } }
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json'
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(url, opts)
  if (res.status === 401 && authListener) {
    clearAuth()
    authListener()
  }
  return parseResponse(res)
}

export async function uploadImages(vehicleId, files) {
  const fd = new FormData()
  for (const f of files) fd.append('file', f)
  const res = await fetch(`/api/vehicles/${vehicleId}/images`, {
    method: 'POST',
    headers: { ...authHeaders() },
    body: fd,
  })
  if (res.status === 401 && authListener) {
    clearAuth()
    authListener()
  }
  return parseResponse(res)
}

export async function reorderImages(vehicleId, imageIds) {
  const res = await fetch(`/api/vehicles/${vehicleId}/images/reorder`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ imageIds }),
  })
  if (res.status === 401 && authListener) {
    clearAuth()
    authListener()
  }
  return parseResponse(res)
}

export function formatPrice(v) {
  if (v.currency === 'USD') return `US$ ${Number(v.price).toLocaleString('es-AR')}`
  return `$ ${Number(v.price).toLocaleString('es-AR')}`
}