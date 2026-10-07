import { useEffect, useMemo, useState } from 'react'
import {
  api,
  uploadImages,
  reorderImages,
  formatPrice,
  setToken,
  getUser,
  setUser as persistUser,
  clearAuth,
  onAuthExpired,
} from './api'

export default function App() {
  const [user, setUser] = useState(getUser)
  const [view, setView] = useState('market')
  const [selected, setSelected] = useState(null)
  const [vehicles, setVehicles] = useState([])
  const [query, setQuery] = useState('')
  const [log, setLog] = useState([])
  const [showLog, setShowLog] = useState(true)

  async function run(method, url, body, { silent = false } = {}) {
    const start = Date.now()
    const res = await api(method, url, body)
    if (!silent) {
      setLog((prev) => [
        { method, url, status: res.status, statusText: res.statusText, ms: Date.now() - start, data: res.data, at: new Date().toLocaleTimeString() },
        ...prev,
      ].slice(0, 20))
    }
    return res
  }

  async function loadVehicles() {
    const res = await run('GET', '/api/vehicles', undefined, { silent: true })
    if (res.data) setVehicles(res.data)
  }

  useEffect(() => {
    loadVehicles()
    onAuthExpired(() => {
      setUser(null)
      setView('login')
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function logout() {
    clearAuth()
    setUser(null)
    setView('market')
  }

  function login(auth) {
    if (auth.token) setToken(auth.token)
    persistUser(auth.user ?? auth)
    setUser(auth.user ?? auth)
    setView('market')
  }

  function openDetail(v) {
    setSelected(v)
    setView('detail')
  }

  return (
    <div className="app">
      <Header
        user={user}
        view={view}
        onView={(v) => setView(v)}
        query={query}
        setQuery={setQuery}
        onLogout={logout}
      />

      <main>
        {!user && view === 'login' && <LoginView run={run} onLogin={login} />}
        {view === 'market' && <MarketHome vehicles={vehicles} query={query} user={user} run={run} onOpen={openDetail} onLogin={() => setView('login')} />}
        {view === 'detail' && selected && <DetailView vehicle={selected} user={user} onBack={() => setView('market')} onLogin={() => setView('login')} run={run} onChanged={loadVehicles} />}
        {view === 'admin' && user?.admin && <AdminPanel user={user} run={run} onChanged={loadVehicles} />}
        {view === 'offers' && user && <OffersView user={user} run={run} />}
        {view === 'profile' && user && <ProfileView user={user} run={run} onSaved={(u) => { setUser(u); loadVehicles() }} />}
        {view === 'admin' && user && !user.admin && <p className="muted">Solo el perfil admin puede administrar vehículos.</p>}
      </main>

      <LogPanel show={showLog} log={log} onToggle={() => setShowLog(!showLog)} />
    </div>
  )
}

/* ---------------- Header ---------------- */

function Header({ user, view, onView, query, setQuery, onLogout }) {
  return (
    <header>
      <div className="racing-strip" />
      <div className="header-row">
        <button className="brand" onClick={() => onView('market')}>
          <span className="flag">🏁</span>
          <span className="brand-name">SEBRING</span>
        </button>
        <input
          className="search"
          placeholder="Buscá marca, modelo o año…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <div className="user-zone">
          {user ? (
            <>
              <div className="role-set">
                <span className={`role ${user.admin ? 'admin' : 'user'}`}>
                  {user.admin ? '👑 ADMIN' : '🛒 COMPRADOR'}
                </span>
                <small className="perm-hint">
                  {user.admin ? 'Gestiona autos, usuarios e imágenes' : 'Compra, reserva y test drive'}
                </small>
              </div>
              <button onClick={() => onView(user.admin ? 'admin' : 'offers')}>
                {user.admin ? 'Panel admin' : 'Mis operaciones'}
              </button>
              <button onClick={() => onView('profile')}>Perfil</button>
              <button className="ghost" onClick={onLogout}>Salir</button>
            </>
          ) : (
            <button onClick={() => onView('login')}>Ingresar</button>
          )}
        </div>
      </div>
    </header>
  )
}

/* ---------------- Marketplace ---------------- */

function MarketHome({ vehicles, query, user, run, onOpen, onLogin }) {
  const [showFilters, setShowFilters] = useState(false)
  const [brand, setBrand] = useState('')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [sort, setSort] = useState('')

  const brands = useMemo(() => {
    const set = new Set()
    vehicles.filter((v) => v.status !== 'deleted').forEach((v) => set.add(v.brand))
    return [...set].sort((a, b) => a.localeCompare(b))
  }, [vehicles])

  const list = useMemo(() => {
    const q = query.trim().toLowerCase()
    let filtered = vehicles.filter((v) => v.status !== 'deleted')
    if (q) {
      filtered = filtered.filter((v) =>
        `${v.brand} ${v.model} ${v.version} ${v.year}`.toLowerCase().includes(q)
      )
    }
    if (brand) {
      filtered = filtered.filter((v) => v.brand === brand)
    }
    const min = Number(minPrice)
    const max = Number(maxPrice)
    if (minPrice !== '' && !Number.isNaN(min)) {
      filtered = filtered.filter((v) => Number(v.price) >= min)
    }
    if (maxPrice !== '' && !Number.isNaN(max)) {
      filtered = filtered.filter((v) => Number(v.price) <= max)
    }
    if (sort === 'asc') filtered = [...filtered].sort((a, b) => Number(a.price) - Number(b.price))
    else if (sort === 'desc') filtered = [...filtered].sort((a, b) => Number(b.price) - Number(a.price))
    return filtered
  }, [vehicles, query, brand, minPrice, maxPrice, sort])

  function clearFilters() {
    setBrand('')
    setMinPrice('')
    setMaxPrice('')
    setSort('')
  }

  return (
    <section className="market">
      <div className="market-head">
        <h2>{query ? `Resultados para “${query}”` : 'Vehículos en venta'}</h2>
        <div className="head-right">
          <span className="muted">{list.length} resultados</span>
          <button className="filter-toggle" onClick={() => setShowFilters(!showFilters)}>
            {showFilters ? 'Ocultar filtros' : '⚙ Filtrar por precio y marca'}
          </button>
        </div>
      </div>

      {showFilters && (
        <div className="filterbar">
          <label>Marca
            <select value={brand} onChange={(e) => setBrand(e.target.value)}>
              <option value="">Todas</option>
              {brands.map((b) => <option key={b} value={b}>{b}</option>)}
            </select>
          </label>
          <label>Precio mín.
            <input type="number" min="0" placeholder="0" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} />
          </label>
          <label>Precio máx.
            <input type="number" min="0" placeholder="—" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} />
          </label>
          <label>Ordenar
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="">Relevancia</option>
              <option value="asc">Menor precio</option>
              <option value="desc">Mayor precio</option>
            </select>
          </label>
          <button className="ghost" onClick={clearFilters}>Limpiar filtros</button>
        </div>
      )}

      {list.length === 0 ? (
        <p className="muted">No hay vehículos que coincidan con la búsqueda o los filtros.</p>
      ) : (
        <div className="grid">
          {list.map((v) => (
            <VehicleCard key={v.id} v={v} onOpen={() => onOpen(v)} />
          ))}
        </div>
      )}
    </section>
  )
}

function VehicleCard({ v, onOpen }) {
  const main = v.images?.[0]
  return (
    <article className="card" onClick={onOpen}>
      <div className="thumb">
        {main ? <img src={main.url} alt={v.brand} loading="lazy" /> : <Placeholder brand={`${v.brand} ${v.model}`} />}
        <span className="stock">{v.stock > 0 ? `${v.stock} en stock` : 'Sin stock'}</span>
      </div>
      <div className="card-body">
        <h3>{v.brand} {v.model} <small>{v.version}</small></h3>
        <p className="year">{v.year}</p>
        <p className="price">{formatPrice(v)}</p>
        <div className="specs">
          <span>{v.transmission ?? '—'}</span>
          <span>{v.fuelType ?? '—'}</span>
        </div>
      </div>
    </article>
  )
}

function Placeholder({ brand }) {
  return (
    <div className="placeholder">
      <span className="car-emoji">🚗</span>
      <span>{brand}</span>
    </div>
  )
}

/* ---------------- Detalle ---------------- */

function DetailView({ vehicle: v, user, onBack, onLogin, run, onChanged }) {
  const [mainIndex, setMainIndex] = useState(0)
  const [msg, setMsg] = useState(null)
  const images = v.images ?? []

  async function createTxn(type, paymentMethod) {
    setMsg(null)
    const res = await run('POST', '/api/transactions', {
      userId: user.id,
      vehicleId: v.id,
      transactionType: type,
      totalAmount: v.price,
      paymentMethod,
    })
    if (res.ok) setMsg({ ok: true, text: `Transacción ${type} creada (${res.status}) ✓` })
    else setMsg({ ok: false, text: `Error ${res.status}: ${res.data?.message ?? 'no pudo completarse'}` })
  }

  return (
    <section className="detail panel">
      <button className="back" onClick={onBack}>← Volver al mercado</button>
      <div className="detail-grid">
        <div className="gallery">
          {images.length > 0 ? (
            <>
              <div className="gallery-main">
                <img src={images[mainIndex].url} alt={`${v.brand} ${v.model}`} />
              </div>
              {images.length > 1 && (
                <div className="gallery-thumbs">
                  {images.map((im, i) => (
                    <button key={im.id} className={i === mainIndex ? 'active' : ''} onClick={() => setMainIndex(i)}>
                      <img src={im.url} alt="" />
                    </button>
                  ))}
                </div>
              )}
            </>
          ) : (
            <div className="gallery-main"><Placeholder brand={`${v.brand} ${v.model}`} /></div>
          )}
        </div>

        <div className="info">
          <h2>{v.brand} {v.model} <small>{v.version}</small></h2>
          <p className="muted year">Año {v.year} · {v.stock} en stock · estado: {v.status}</p>
          <p className="price big">{formatPrice(v)}</p>
          <ul className="spec-list">
            <li><b>Transmisión</b><span>{v.transmission ?? '—'}</span></li>
            <li><b>Combustible</b><span>{v.fuelType ?? '—'}</span></li>
            <li><b>Motor</b><span>{v.engine ?? '—'}</span></li>
            <li><b>Moneda</b><span>{v.currency}</span></li>
          </ul>
          {v.description && <p className="desc">{v.description}</p>}

          {msg && <p className={`msg ${msg.ok ? 'ok' : 'err'}`}>{msg.text}</p>}

          {user ? (
            <div className="actions">
              <button className="buy" onClick={() => createTxn('purchase', 'tarjeta')}>Comprar</button>
              <button className="ok2" onClick={() => createTxn('reservation', 'transferencia')}>Reservar</button>
              <button className="ghost2" onClick={() => createTxn('test_drive', null)}>Test drive</button>
            </div>
          ) : (
            <p className="muted">
              Iniciá sesión para comprar o reservar. <button className="link" onClick={onLogin}>Ingresar</button>
            </p>
          )}
        </div>
      </div>
    </section>
  )
}

/* ---------------- Login ---------------- */

function LoginView({ run, onLogin }) {
  const [form, setForm] = useState({ login: '', password: '' })
  const [error, setError] = useState(null)
  const [registering, setRegistering] = useState(false)
  const [reg, setReg] = useState({ firstName: '', lastName: '', username: '', email: '', password: '', phone: '', nationalId: '' })

  async function doLogin(e) {
    e.preventDefault()
    setError(null)
    const id = form.login.trim()
    const payload = id.includes('@') ? { email: id, password: form.password } : { username: id, password: form.password }
    const res = await run('POST', '/api/auth/login', payload)
    if (res.ok) {
      onLogin(res.data)
    } else {
      setError(res.data?.message ?? 'Error al iniciar sesión')
    }
  }

  async function doRegister(e) {
    e.preventDefault()
    setError(null)
    const res = await run('POST', '/api/users', reg)
    if (res.ok) {
      const login = await run('POST', '/api/auth/login', { username: reg.username, password: reg.password }, { silent: true })
      if (login.ok) onLogin(login.data)
      else setError('Cuenta creada, inicia sesión')
    } else {
      setError(res.data?.message ?? 'No se pudo crear la cuenta')
    }
  }

  const setR = (k) => (el) => ({ ...reg, [k]: el.target.value })

  return (
    <section className="login panel">
      <h2>{registering ? 'Crear cuenta' : 'Ingresá a SEBRING'}</h2>
      {error && <p className="msg err">{error}</p>}

      {!registering ? (
        <form className="stack" onSubmit={doLogin}>
          <input placeholder="Email o nombre de usuario" value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} required />
          <input placeholder="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          <button type="submit">Iniciar sesión</button>
        </form>
      ) : (
        <form className="stack" onSubmit={doRegister}>
          <input placeholder="Nombre" value={reg.firstName} onChange={setR('firstName')} required />
          <input placeholder="Apellido" value={reg.lastName} onChange={setR('lastName')} required />
          <input placeholder="Nombre de usuario" value={reg.username} onChange={setR('username')} required />
          <input placeholder="Email" type="email" value={reg.email} onChange={setR('email')} required />
          <input placeholder="Password" type="password" value={reg.password} onChange={setR('password')} required />
          <input placeholder="Teléfono" value={reg.phone} onChange={setR('phone')} required />
          <input placeholder="DNI" value={reg.nationalId} onChange={setR('nationalId')} required />
          <button type="submit">Crear cuenta</button>
        </form>
      )}

      <div className="demo">
        <strong>Cuentas de prueba</strong>
        <button onClick={() => setForm({ login: 'alice', password: 'secret123' })}>Usuario: alice (o alice@example.com) / secret123</button>
        <button onClick={() => setForm({ login: 'bob', password: 'secret456' })}>Admin: bob (o bob@example.com) / secret456</button>
      </div>

      <p className="muted">
        {registering ? '¿Ya tenés cuenta?' : '¿No tenés cuenta?'}{' '}
        <button className="link" onClick={() => setRegistering(!registering)}>
          {registering ? 'Iniciar sesión' : 'Registrate'}
        </button>
      </p>

      <div className="muted" style={{ display: 'grid', gap: 4, fontSize: 12, marginTop: 10 }}>
        <span>Podés entrar con tu email o con tu nombre de usuario + contraseña.</span>
        <span>El registro crea un usuario COMPRADOR (sin permisos de admin).</span>
      </div>
    </section>
  )
}

/* ---------------- Panel Admin ---------------- */

function AdminPanel({ user, run, onChanged }) {
  const [tab, setTab] = useState('list')
  const [editing, setEditing] = useState(null)
  return (
    <section>
      <div className="panel">
        <h2>Panel admin <span className="muted">· {user.firstName} {user.lastName}</span></h2>
        <nav className="subnav">
          <button className={tab === 'list' ? 'active' : ''} onClick={() => setTab('list')}>Vehículos</button>
          <button className={tab === 'form' ? 'active' : ''} onClick={() => { setEditing(null); setTab('form') }}>
            {editing && tab === 'form' ? `Editando vehículo ${editing.id}` : 'Nuevo vehículo'}
          </button>
          <button className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>Usuarios</button>
        </nav>
        {tab === 'list' && <ManageVehicles run={run} onEdit={(v) => { setEditing(v); setTab('form') }} />}
        {tab === 'form' && <VehicleForm run={run} onSaved={onChanged} goList={() => { setEditing(null); setTab('list') }} initial={editing} />}
        {tab === 'users' && <UsersList run={run} />}
      </div>
    </section>
  )
}

function ManageVehicles({ run, onEdit }) {
  const [list, setList] = useState([])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function load() {
    const res = await run('GET', '/api/vehicles', undefined, { silent: true })
    if (res.data) setList(res.data)
  }

  async function remove(v) {
    if (window.confirm(`¿Borrar ${v.brand} ${v.model}?`)) {
      await run('DELETE', `/api/vehicles/${v.id}`)
      load()
    }
  }

  async function upImages(v, files) {
    const res = await uploadImages(v.id, files)
    if (res.ok) {
      await run('GET', `/api/vehicles/${v.id}`, undefined)
      load()
    }
  }

  async function removeImage(v, imageId) {
    if (!window.confirm('¿Quitar esta imagen?')) return
    const res = await run('DELETE', `/api/images/${imageId}`)
    if (res.ok) load()
  }

  async function applyOrder(v, imageIds) {
    const res = await reorderImages(v.id, imageIds)
    if (res.ok) load()
    else if (res.status === 401) alert('Sesión expirada, ingresá de nuevo.')
    else alert(`No se pudo reordenar (${res.status})`)
  }

  function move(v, imgs, index, delta) {
    const target = index + delta
    if (target < 0 || target >= imgs.length) return
    const arr = [...imgs]
    const [x] = arr.splice(index, 1)
    arr.splice(target, 0, x)
    applyOrder(v, arr.map((im) => im.id))
  }

  function makeCover(v, imgs, index) {
    const arr = [...imgs]
    const [x] = arr.splice(index, 1)
    arr.unshift(x)
    applyOrder(v, arr.map((im) => im.id))
  }

  return (
    <div>
      <table>
        <thead>
          <tr><th>id</th><th>vehículo</th><th>precio</th><th>stock</th><th>fotos</th><th>estado</th><th></th></tr>
        </thead>
        <tbody>
          {list.map((v) => (
            <tr key={v.id}>
              <td>{v.id}</td>
              <td>{v.brand} {v.model} {v.year}</td>
              <td>{formatPrice(v)}</td>
              <td>{v.stock}</td>
              <td>
                <div className="minithumbs">
                  {(v.images ?? []).map((im, idx) => {
                    const imgs = v.images ?? []
                    return (
                      <div className="imgcell" key={im.id}>
                        <img src={im.url} alt="" />
                        {idx === 0 && <span className="cover">PORTADA</span>}
                        <div className="img-actions">
                          <button className="tiny" title="Mover a la izquierda" onClick={() => move(v, imgs, idx, -1)}>◀</button>
                          <button className="tiny gold" title="Fijar como portada (primera)" onClick={() => makeCover(v, imgs, idx)}>★</button>
                          <button className="tiny" title="Mover a la derecha" onClick={() => move(v, imgs, idx, 1)}>▶</button>
                          <button className="tiny danger-t" title="Quitar imagen" onClick={() => removeImage(v, im.id)}>✕</button>
                        </div>
                      </div>
                    )
                  })}
                  <label className="upload" title="Agregar imágenes">
                    ➕
                    <input type="file" accept="image/*" multiple hidden
                      onChange={(e) => { if (e.target.files?.length) upImages(v, [...e.target.files]); e.target.value = '' }} />
                  </label>
                </div>
              </td>
              <td>{v.status}</td>
              <td>
                <button className="small" onClick={() => onEdit(v)}>editar</button>{' '}
                <button className="danger small" onClick={() => remove(v)}>borrar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="muted" style={{ marginBottom: 0 }}>Hacé clic en “editar” para cargar el vehículo en el formulario y guardar los cambios con el botón “Guardar cambios”.</p>
    </div>
  )
}

const EMPTY_FORM = {
  brand: '', model: '', version: '', year: '', price: '', currency: 'ARS', stock: 0,
  transmission: 'Manual', fuelType: 'Nafta', engine: '', description: '', status: 'available',
}

function toForm(v) {
  return {
    brand: v.brand ?? '',
    model: v.model ?? '',
    version: v.version ?? '',
    year: v.year ?? '',
    price: v.price ?? '',
    currency: v.currency ?? 'ARS',
    stock: v.stock ?? 0,
    transmission: v.transmission ?? 'Manual',
    fuelType: v.fuelType ?? 'Nafta',
    engine: v.engine ?? '',
    description: v.description ?? '',
    status: v.status ?? 'available',
  }
}

function VehicleForm({ run, onSaved, goList, initial }) {
  const [form, setForm] = useState(initial ? toForm(initial) : EMPTY_FORM)
  const [editingId, setEditingId] = useState(initial?.id ?? '')
  const [files, setFiles] = useState([])
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setMsg(null)
    const payload = { ...form, year: Number(form.year), price: Number(form.price), stock: Number(form.stock) }
    let res
    if (editingId) {
      res = await run('PUT', `/api/vehicles/${editingId}`, payload)
    } else {
      res = await run('POST', '/api/vehicles', payload)
    }
    if (res.ok) {
      const id = editingId || res.data.id
      let uploadedCount = 0
      if (files.length) {
        const up = await uploadImages(id, files)
        uploadedCount = up.ok ? (up.data?.length ?? 0) : 0
      }
      setMsg({ ok: true, text: editingId ? `Vehículo ${id} actualizado ✓` : `Vehículo ${id} creado (${res.status}) ✓ · ${uploadedCount} fotos subidas` })
      setForm(EMPTY_FORM)
      setEditingId('')
      setFiles([])
      onSaved()
      goList()
    } else {
      setMsg({ ok: false, text: `Error ${res.status}: ${res.data?.message ?? JSON.stringify(res.data)}` })
    }
    setBusy(false)
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <form className="vform" onSubmit={submit}>
      {editingId && (
        <div className="editing-banner">
          ✏️ Editando el vehículo <b>#{editingId}</b> — los cambios se guardan al hacer clic en “Guardar cambios”.
        </div>
      )}
      <input placeholder="Marca" value={form.brand} onChange={set('brand')} required />
      <input placeholder="Modelo" value={form.model} onChange={set('model')} required />
      <input placeholder="Versión (opcional)" value={form.version} onChange={set('version')} />
      <input placeholder="Año" type="number" value={form.year} onChange={set('year')} required />
      <input placeholder="Precio" type="number" step="0.01" value={form.price} onChange={set('price')} required />
      <select value={form.currency} onChange={set('currency')}>
        <option value="ARS">ARS</option>
        <option value="USD">USD</option>
      </select>
      <input placeholder="Stock" type="number" min="0" value={form.stock} onChange={set('stock')} />
      <select value={form.transmission} onChange={set('transmission')}>
        <option>Manual</option>
        <option>Automática</option>
      </select>
      <select value={form.fuelType} onChange={set('fuelType')}>
        <option>Nafta</option>
        <option>Diesel</option>
        <option>Hibrido</option>
        <option>Electrico</option>
      </select>
      <input placeholder="Motor (ej. 1.6L)" value={form.engine} onChange={set('engine')} />
      <select value={form.status} onChange={set('status')}>
        <option>available</option>
        <option>used</option>
      </select>
      <textarea rows="3" placeholder="Descripción" value={form.description} onChange={set('description')} />
      <label className="filepick">
        📷 Imágenes (podés elegir varias) — se suben al crear
        <input type="file" accept="image/*" multiple onChange={(e) => setFiles([...e.target.files])} />
      </label>

      {msg && <p className={`msg ${msg.ok ? 'ok' : 'err'}`}>{msg.text}</p>}

      <div className="form-actions">
        <button type="submit" disabled={busy} className="save">
          {busy ? 'Guardando…' : editingId ? '💾 Guardar cambios' : 'Publicar vehículo'}
        </button>
        {editingId && (
          <button type="button" className="ghost" onClick={goList}>Cancelar</button>
        )}
      </div>
    </form>
  )
}

function UsersList({ run }) {
  const [users, setUsers] = useState([])
  const [form, setForm] = useState({ firstName: '', lastName: '', username: '', email: '', password: '', phone: '', nationalId: '' })
  const [msg, setMsg] = useState(null)
  const [busy, setBusy] = useState(false)

  async function load() {
    const res = await run('GET', '/api/users', undefined, { silent: true })
    if (res.data) setUsers(res.data)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function create(e) {
    e.preventDefault()
    setBusy(true)
    setMsg(null)
    const res = await run('POST', '/api/users', form)
    if (res.ok) {
      setMsg({
        ok: true,
        text: `Comprador ${form.email} creado ✓ — ya puede iniciar sesión con esa contraseña.`,
      })
      setForm({ firstName: '', lastName: '', username: '', email: '', password: '', phone: '', nationalId: '' })
      load()
    } else {
      setMsg({ ok: false, text: `Error ${res.status}: ${res.data?.message ?? JSON.stringify(res.data)}` })
    }
    setBusy(false)
  }

  return (
    <div>
      <form className="vform" onSubmit={create}>
        <h3>Crear comprador</h3>
        <p className="muted">Alta sin permisos de admin: ese usuario podrá iniciar sesión con su nombre de usuario/email y contraseña.</p>
        <div className="fill-grid">
          <input placeholder="Nombre" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
          <input placeholder="Apellido" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
          <input placeholder="Nombre de usuario" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
          <input placeholder="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          <input placeholder="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          <input placeholder="Teléfono" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required />
          <input placeholder="DNI" value={form.nationalId} onChange={(e) => setForm({ ...form, nationalId: e.target.value })} required />
        </div>
        <div className="form-actions" style={{ paddingLeft: 0 }}>
          <button type="submit" disabled={busy} className="save">{busy ? 'Creando…' : '➕ Crear comprador'}</button>
        </div>
        {msg && <p className={`msg ${msg.ok ? 'ok' : 'err'}`}>{msg.text}</p>}
      </form>

      <table>
        <thead>
          <tr><th>id</th><th>nombre</th><th>usuario</th><th>email</th><th>rol</th><th>DNI</th><th>activo</th></tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.firstName} {u.lastName}</td>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>{u.admin ? 'admin' : 'usuario'}</td>
              <td>{u.nationalId}</td>
              <td>{u.active ? 'sí' : 'no'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/* ---------------- Perfil ---------------- */

function ProfileView({ user, run, onSaved }) {
  const [form, setForm] = useState({
    firstName: user.firstName ?? '',
    lastName: user.lastName ?? '',
    phone: user.phone ?? '',
    nationalId: user.nationalId ?? '',
    password: '',
  })
  const [msg, setMsg] = useState(null)
  const [busy, setBusy] = useState(false)

  async function save(e) {
    e.preventDefault()
    setBusy(true)
    setMsg(null)
    const res = await run('PUT', `/api/users/${user.id}`, {
      firstName: form.firstName,
      lastName: form.lastName,
      password: form.password,
      phone: form.phone,
      nationalId: form.nationalId,
    })
    if (res.ok) {
      setMsg({ ok: true, text: `Perfil guardado ✓ (${res.status})` })
      if (typeof onSaved === 'function') onSaved(res.data)
    } else {
      setMsg({ ok: false, text: `Error ${res.status}: ${res.data?.message ?? JSON.stringify(res.data)}` })
    }
    setBusy(false)
  }

  return (
    <section className="panel">
      <h2>
        Mi perfil
        <span className="muted"> · {user.username ?? user.email}</span>
        <span className={`role ${user.admin ? 'admin' : 'user'}`}>
          {user.admin ? '👑 ADMIN' : '🛒 COMPRADOR'}
        </span>
      </h2>
      <p className="muted">Nombre de usuario: <b>{user.username}</b> · Email: <b>{user.email}</b></p>

      <form className="vform" onSubmit={save}>
        <div className="fill-grid">
          <input placeholder="Nombre" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
          <input placeholder="Apellido" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
          <input placeholder="Teléfono" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required />
          <input placeholder="DNI" value={form.nationalId} onChange={(e) => setForm({ ...form, nationalId: e.target.value })} required />
          <input placeholder="Password (actual o nueva)" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        </div>
        <div className="form-actions" style={{ paddingLeft: 0 }}>
          <button type="submit" disabled={busy} className="save">{busy ? 'Guardando…' : '💾 Guardar cambios'}</button>
        </div>
        {msg && <p className={`msg ${msg.ok ? 'ok' : 'err'}`}>{msg.text}</p>}
      </form>
    </section>
  )
}

/* ---------------- Mis operaciones ---------------- */

function OffersView({ user, run }) {
  const [txs, setTxs] = useState([])
  const [vehicles, setVehicles] = useState([])

  useEffect(() => {
    Promise.all([
      run('GET', '/api/transactions', undefined, { silent: true }),
      run('GET', '/api/vehicles', undefined, { silent: true }),
    ]).then(([t, v]) => {
      if (t.data) setTxs(t.data)
      if (v.data) setVehicles(v.data)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const nameOf = (vid) => {
    const v = vehicles.find((x) => x.id === vid)
    return v ? `${v.brand} ${v.model} ${v.year}` : `#${vid}`
  }

  return (
    <section className="panel">
      <h2>{user.admin ? 'Todas las operaciones (admin)' : 'Mis operaciones'}</h2>
      <table>
        <thead>
          <tr><th>id</th><th>vehículo</th><th>tipo</th><th>monto</th><th>método</th><th>estado</th><th>fecha</th></tr>
        </thead>
        <tbody>
          {txs.map((t) => (
            <tr key={t.id}>
              <td>{t.id}</td>
              <td>{nameOf(t.vehicleId)}</td>
              <td>{t.transactionType}</td>
              <td>{Number(t.totalAmount).toLocaleString('es-AR')}</td>
              <td>{t.paymentMethod}</td>
              <td>{t.status}</td>
              <td className="muted">{String(t.createdAt).slice(0, 10)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}

/* ---------------- Log de respuestas ---------------- */

function LogPanel({ show, log, onToggle }) {
  return (
    <>
      {show && (
        <div className="log">
          <div className="log-head">
            <strong>API — códigos OK / error · autorización con Bearer token</strong>
            <button className="ghost small" onClick={onToggle}>ocultar</button>
          </div>
          {log.length === 0 ? (
            <p className="muted">Todavía no hay llamadas registradas.</p>
          ) : (
            <ul>
              {log.map((e, i) => (
                <li key={i}>
                  <Pill status={e.status} />
                  <b>{e.method}</b> <code>{e.url}</code> · {e.ms}ms · {e.statusText}
                  {e.data && e.status >= 400 && <pre>{JSON.stringify(e.data, null, 2)}</pre>}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
      {!show && (
        <button className="fab" onClick={onToggle}>🔎 Ver log</button>
      )}
    </>
  )
}

function Pill({ status }) {
  const cls = status < 300 ? 'ok' : status < 500 ? 'warn' : 'err'
  return <span className={`pill ${cls}`}>{status}</span>
}