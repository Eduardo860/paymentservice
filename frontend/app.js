// ── Constantes ────────────────────────────────────────────────
const API = '/api';

// ── Utilidades ────────────────────────────────────────────────
function escHtml(v) {
  return String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function renderData(data) {
  if (Array.isArray(data)) {
    if (data.length === 0) return '<span class="empty">Sin resultados</span>';
    if (typeof data[0] === 'object' && data[0] !== null) {
      const keys = Object.keys(data[0]);
      const head = keys.map(k => `<th>${escHtml(k)}</th>`).join('');
      const rows = data.map(row =>
        '<tr>' + keys.map(k => {
          const v = row[k];
          const display = v !== null && typeof v === 'object' ? JSON.stringify(v) : (v ?? '—');
          return `<td>${escHtml(display)}</td>`;
        }).join('') + '</tr>'
      ).join('');
      return `<table><thead><tr>${head}</tr></thead><tbody>${rows}</tbody></table>`;
    }
    return data.map(s => `<div class="log-line">${escHtml(s)}</div>`).join('');
  }
  if (typeof data === 'object' && data !== null) {
    const rows = Object.entries(data).map(([k, v]) => {
      const display = v !== null && typeof v === 'object' ? JSON.stringify(v) : (v ?? '—');
      return `<tr><td class="key">${escHtml(k)}</td><td>${escHtml(display)}</td></tr>`;
    }).join('');
    return `<table class="obj-table"><tbody>${rows}</tbody></table>`;
  }
  return `<span>${escHtml(data)}</span>`;
}

function show(id, data, isError = false) {
  const el = document.getElementById(id);
  el.className = 'response ' + (isError ? 'error' : 'success');
  if (typeof data === 'string') {
    el.innerHTML = `<span>${escHtml(data)}</span>`;
  } else {
    el.innerHTML = renderData(data);
  }
}

async function request(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  const text = await res.text();
  try {
    const json = JSON.parse(text);
    // Desempaquetar wrapper { status, data } o { status, message }
    if (json && typeof json === 'object' && 'status' in json && ('data' in json || 'message' in json)) {
      const isOk = typeof json.status === 'number' ? json.status >= 200 && json.status < 300 : json.status === 'success';
      return { ok: isOk, data: isOk ? json.data : (json.message ?? json.data) };
    }
    return { ok: res.ok, data: json };
  } catch { return { ok: res.ok, data: text }; }
}

// ── Tabs ──────────────────────────────────────────────────────
function showTab(name, btn) {
  document.querySelectorAll('.tab-content').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
  document.getElementById('tab-' + name).classList.add('active');
  btn.classList.add('active');
}

// ── Health check ──────────────────────────────────────────────
async function checkHealth() {
  const badge = document.getElementById('health-badge');
  try {
    const { ok, data } = await request('GET', '/actuator/health');
    if (ok && data.status === 'UP') {
      badge.textContent = '✅ Sistema UP';
      badge.className = 'badge badge-up';
    } else {
      badge.textContent = '⚠️ ' + (data.status || 'Desconocido');
      badge.className = 'badge badge-warn';
    }
  } catch {
    badge.textContent = '❌ Sin conexión';
    badge.className = 'badge badge-down';
  }
}

// ── Productos ─────────────────────────────────────────────────
async function crearProducto() {
  const name = document.getElementById('p-name').value.trim();
  const description = document.getElementById('p-desc').value.trim();
  const price = parseFloat(document.getElementById('p-price').value);
  const stock = parseInt(document.getElementById('p-stock').value);
  if (!name || !description || isNaN(price) || isNaN(stock)) {
    return show('resp-productos', 'Completa todos los campos.', true);
  }
  show('resp-productos', 'Creando producto...');
  const { ok, data } = await request('POST', '/productos', { name, description, price, stock });
  show('resp-productos', data, !ok);
}

async function listarProductos() {
  show('resp-productos', 'Cargando...');
  const { ok, data } = await request('GET', '/productos');
  show('resp-productos', data, !ok);
}

async function getProducto() {
  const id = document.getElementById('p-id').value.trim();
  if (!id) return show('resp-productos', 'Ingresa un ID.', true);
  show('resp-productos', 'Buscando...');
  const { ok, data } = await request('GET', '/productos/' + id);
  show('resp-productos', data, !ok);
}

async function buscarPorNombre() {
  const filtro = document.getElementById('p-nombre').value.trim().toLowerCase();
  if (!filtro) return show('resp-productos', 'Ingresa un nombre a buscar.', true);
  show('resp-productos', 'Buscando...');
  const { ok, data } = await request('GET', '/productos');
  if (!ok) return show('resp-productos', data, true);
  const matches = Array.isArray(data)
    ? data.filter(p => p.name && p.name.toLowerCase().includes(filtro))
    : [];
  if (matches.length === 0) {
    show('resp-productos', `Sin resultados para "${filtro}"`);
  } else {
    const el = document.getElementById('resp-productos');
    el.className = 'response success';
    el.innerHTML = `<p class="hint">${matches.length} resultado(s) para "<strong>${escHtml(filtro)}</strong>"</p>` + renderData(matches);
  }
}

async function actualizarProducto() {
  const id = document.getElementById('p-update-id').value.trim();
  const name = document.getElementById('p-update-name').value.trim();
  const description = document.getElementById('p-update-desc').value.trim();
  const price = parseFloat(document.getElementById('p-update-price').value);
  const stock = parseInt(document.getElementById('p-update-stock').value);
  if (!id || !name || !description || isNaN(price) || isNaN(stock)) {
    return show('resp-productos', 'Completa todos los campos incluyendo el ID.', true);
  }
  show('resp-productos', 'Actualizando producto...');
  const { ok, data } = await request('PUT', '/productos/' + id, { name, description, price, stock });
  show('resp-productos', data, !ok);
}

async function eliminarProducto() {
  const id = document.getElementById('p-delete-id').value.trim();
  if (!id) return show('resp-productos', 'Ingresa un ID.', true);
  show('resp-productos', 'Eliminando producto...');
  const { ok, data } = await request('DELETE', '/productos/' + id);
  show('resp-productos', data, !ok);
}

// ── Órdenes ───────────────────────────────────────────────────
async function crearOrden() {
  const userId = parseInt(document.getElementById('o-userid').value);
  const productId = document.getElementById('o-productid').value.trim();
  const totalAmount = parseFloat(document.getElementById('o-amount').value);
  const status = document.getElementById('o-status').value;
  if (isNaN(userId) || !productId || isNaN(totalAmount)) {
    return show('resp-ordenes', 'Completa todos los campos incluyendo el Product ID.', true);
  }
  show('resp-ordenes', 'Creando orden...');
  const { ok, data } = await request('POST', '/ordenes', { userId, productId, totalAmount, status });
  show('resp-ordenes', data, !ok);
}

async function getOrden() {
  const id = document.getElementById('o-id').value.trim();
  if (!id) return show('resp-ordenes', 'Ingresa un ID.', true);
  show('resp-ordenes', 'Buscando...');
  const { ok, data } = await request('GET', '/ordenes/' + id);
  show('resp-ordenes', data, !ok);
}

async function getOrdenesPorUsuario() {
  const userId = document.getElementById('o-userid-buscar').value.trim();
  if (!userId) return show('resp-ordenes', 'Ingresa un User ID.', true);
  show('resp-ordenes', 'Buscando...');
  const { ok, data } = await request('GET', '/ordenes/usuario/' + userId);
  show('resp-ordenes', data, !ok);
}

async function actualizarEstado() {
  const id = document.getElementById('o-status-id').value.trim();
  const status = document.getElementById('o-nuevo-status').value;
  if (!id) return show('resp-ordenes', 'Ingresa el ID de la orden.', true);
  show('resp-ordenes', 'Actualizando estado...');
  const { ok, data } = await request('PUT', `/ordenes/${id}/status?status=${status}`);
  show('resp-ordenes', data, !ok);
}

// ── Pagos ─────────────────────────────────────────────────────
async function procesarPago() {
  const orderId = document.getElementById('pg-orderid').value.trim();
  const amount = parseFloat(document.getElementById('pg-amount').value);
  const paymentMethod = document.getElementById('pg-method').value;
  if (!orderId || isNaN(amount)) {
    return show('resp-pagos', 'Completa todos los campos.', true);
  }
  show('resp-pagos', 'Procesando pago...');
  const { ok, data } = await request('POST', '/pagos/procesar', { orderId, amount, paymentMethod });
  show('resp-pagos', data, !ok);
}

async function getPago() {
  const id = document.getElementById('pg-id').value.trim();
  if (!id) return show('resp-pagos', 'Ingresa un ID.', true);
  show('resp-pagos', 'Buscando...');
  const { ok, data } = await request('GET', '/pagos/' + id);
  show('resp-pagos', data, !ok);
}

async function getPagosPorOrden() {
  const orderId = document.getElementById('pg-orderid-buscar').value.trim();
  if (!orderId) return show('resp-pagos', 'Ingresa un Order ID.', true);
  show('resp-pagos', 'Buscando...');
  const { ok, data } = await request('GET', '/pagos/orden/' + orderId);
  show('resp-pagos', data, !ok);
}

async function reembolsarPago() {
  const id = document.getElementById('pg-reembolso-id').value.trim();
  if (!id) return show('resp-pagos', 'Ingresa un ID.', true);
  show('resp-pagos', 'Procesando reembolso...');
  const { ok, data } = await request('PUT', '/pagos/' + id + '/reembolso');
  show('resp-pagos', data, !ok);
}

// ── Logs ──────────────────────────────────────────────────────
async function cargarCloudWatchLogs() {
  const group = document.getElementById('log-group-select').value;
  const el = document.getElementById('resp-logs');
  el.className = 'response success';
  el.innerHTML = '<span>Cargando...</span>';
  try {
    const res = await fetch('/logs/events/' + group);
    const events = await res.json();
    if (!Array.isArray(events) || events.length === 0) {
      el.innerHTML = '<span class="empty">Sin eventos en este grupo. Los servicios usan logging local; revisa los logs del contenedor para ver la actividad.</span>';
      return;
    }
    const html = events.map(e => {
      const ts = e.timestamp ? new Date(e.timestamp).toLocaleString('es-MX') : '';
      return `<div class="log-event"><span class="log-ts">${escHtml(ts)}</span><span class="log-msg">${escHtml(e.message || '')}</span></div>`;
    }).join('');
    el.innerHTML = `<p class="hint">${events.length} evento(s) — ${escHtml(group)}</p>` + html;
  } catch (err) {
    el.className = 'response error';
    el.innerHTML = `<span>Error al cargar logs: ${escHtml(String(err))}</span>`;
  }
}


// ── Init ──────────────────────────────────────────────────────
checkHealth();
setInterval(checkHealth, 30000);
