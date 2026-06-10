// ── Constantes ────────────────────────────────────────────────
const API = '/api';
const BROKER = '/broker';

// ── Utilidades ────────────────────────────────────────────────
function escHtml(v) {
  return String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// Badge de estatus de colores (MongoDB style)
function getStatusBadge(status) {
  if (!status) return '';
  const s = status.toUpperCase();
  let bg = 'rgba(0,0,0,0.1)';
  let color = 'var(--text-main)';
  let border = 'rgba(0,0,0,0.2)';
  
  if (s.includes('PEND')) {
    bg = 'rgba(245, 158, 11, 0.15)'; color = 'var(--yellow)'; border = 'rgba(245, 158, 11, 0.3)';
  } else if (s.includes('COMPLETED') || s.includes('PAG') || s.includes('CONFIRM')) {
    bg = 'rgba(0, 237, 100, 0.15)'; color = '#008a3d'; border = 'rgba(0, 237, 100, 0.3)';
  } else if (s.includes('ENV') || s.includes('ENTREG')) {
    bg = 'rgba(0, 104, 74, 0.15)'; color = 'var(--primary)'; border = 'rgba(0, 104, 74, 0.3)';
  } else if (s.includes('CANCEL') || s.includes('REFUND') || s.includes('FAIL')) {
    bg = 'rgba(225, 29, 72, 0.15)'; color = 'var(--red)'; border = 'rgba(225, 29, 72, 0.3)';
  }
  
  return `<span style="background: ${bg}; color: ${color}; border: 1px solid ${border}; padding: 0.3rem 0.6rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase; white-space: nowrap;"><i class='bx bxs-circle' style="font-size: 0.6rem; margin-right: 4px; vertical-align: middle;"></i>${escHtml(status)}</span>`;
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
function showTab(tabId, btn) {
  document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(el => el.classList.remove('active'));
  document.getElementById('tab-' + tabId).classList.add('active');
  btn.classList.add('active');

  // Auto-cargar tabla al cambiar de tab
  if (tabId === 'productos') listarProductos();
  if (tabId === 'ordenes') listarOrdenes();
  if (tabId === 'pagos') listarPagos();
  if (tabId === 'envios') listarEnvios();
}

function showSubTab(sectionId, subTabId, btn) {
  const section = document.getElementById('tab-' + sectionId);
  section.querySelectorAll('.sub-tab-content').forEach(el => el.classList.remove('active'));
  section.querySelectorAll('.sub-tab').forEach(el => el.classList.remove('active'));
  
  if (subTabId) {
    document.getElementById(subTabId).classList.add('active');
  }
  btn.classList.add('active');
}

// ── Health check ──────────────────────────────────────────────
async function checkHealth() {
  const badge = document.getElementById('health-badge');
  try {
    const { ok, data } = await request('GET', '/actuator/health');
    if (ok && data.status === 'UP') {
      badge.innerHTML = "<i class='bx bx-check-circle'></i> Sistema UP";
      badge.className = 'badge badge-up';
    } else {
      badge.innerHTML = "<i class='bx bx-error'></i> " + (data.status || 'Desconocido');
      badge.className = 'badge badge-warn';
    }
  } catch {
    badge.innerHTML = "<i class='bx bx-x-circle'></i> Sin conexión";
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
  if (stock <= 0) {
    return show('resp-productos', 'El stock debe ser mayor a cero.', true);
  }
  show('resp-productos', 'Creando producto...');
  const { ok, data } = await request('POST', '/productos', { name, description, price, stock });
  show('resp-productos', data, !ok);
}

async function listarProductos() {
  show('resp-productos', 'Cargando...');
  
  const [resProd, resOrd] = await Promise.all([
    request('GET', '/productos'),
    request('GET', '/ordenes')
  ]);

  if (resProd.ok && Array.isArray(resProd.data)) {
    if (resProd.data.length === 0) return show('resp-productos', 'Sin resultados');
    
    // Calcular productos que tienen órdenes
    let productIdsWithOrders = new Set();
    if (resOrd.ok && Array.isArray(resOrd.data)) {
      resOrd.data.forEach(o => {
        if (o.products) {
          o.products.forEach(p => productIdsWithOrders.add(p.productId));
        }
      });
    }

    const rows = resProd.data.map(p => {
      const hasOrder = productIdsWithOrders.has(p.id);
      
      const btnCrear = `<button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="prepararCrearOrden('${p.id}', '${p.price}')"><i class='bx bx-cart-add'></i> Crear Orden</button>`;
      const btnVer = hasOrder ? `<button class="" style="padding: 4px 8px; font-size: 0.8rem;" onclick="saltarAOrdenesDeProducto('${p.id}')"><i class='bx bx-receipt'></i> Ver Órdenes</button>` : '';
      
      return `
      <tr>
        <td style="display: flex; align-items: center;">${escHtml(p.name)}</td>
        <td>${escHtml(p.stock)}</td>
        <td>$${escHtml(p.price)}</td>
        <td>
          <div style="display: flex; gap: 5px;">
            <button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="prepararEdicionProducto('${p.id}', decodeURIComponent('${encodeURIComponent(p.name)}'), '${p.price}', '${p.stock}')"><i class='bx bx-edit'></i> Editar</button>
            <button class="btn-danger" style="padding: 4px 8px; font-size: 0.8rem;" onclick="document.getElementById('p-delete-id').value='${p.id}'; eliminarProducto()"><i class='bx bx-trash'></i> Eliminar</button>
            ${btnCrear}
            ${btnVer}
          </div>
        </td>
      </tr>
      `;
    }).join('');
    
    const html = `<table><thead><tr><th>Nombre</th><th>Stock</th><th>Precio</th><th>Acciones</th></tr></thead><tbody>${rows}</tbody></table>`;
    const el = document.getElementById('resp-productos');
    el.className = 'response success';
    el.innerHTML = html;
  } else {
    show('resp-productos', resProd.data, !resProd.ok);
  }
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
  // Suponiendo cantidad 1 por defecto en este formulario básico, validamos que 1 > 0
  // Para ser explícitos:
  const qty = 1;
  if (qty <= 0) {
    return show('resp-ordenes', 'La cantidad debe ser mayor a cero.', true);
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

async function listarOrdenes(sortByStatus = false) {
  show('resp-ordenes', 'Cargando órdenes...');
  const { ok, data } = await request('GET', '/ordenes');
  if (ok && Array.isArray(data)) {
    let processData = data;
    
    if (sortByStatus) {
      processData.sort((a, b) => {
        const sA = a.status ? a.status.toUpperCase() : '';
        const sB = b.status ? b.status.toUpperCase() : '';
        if (sA < sB) return -1;
        if (sA > sB) return 1;
        return 0;
      });
    }

    if (processData.length === 0) return show('resp-ordenes', 'Sin resultados');
    const rows = processData.map(o => {
      const isPending = (o.status && o.status.toUpperCase().includes('PEND'));
      const pagosBtnClass = isPending ? '' : 'btn-outline';
      const pagosBtnText = isPending ? 'Pagar' : 'Ver Pagos';
      return `
      <tr>
        <td>${escHtml(o.id)}</td>
        <td>${escHtml(o.userId)}</td>
        <td>$${escHtml(o.totalAmount)}</td>
        <td>${getStatusBadge(o.status)}</td>
        <td>${escHtml(o.createdAt || '')}</td>
        <td>
          <div style="display: flex; gap: 5px;">
            <button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="prepararEdicionOrden('${o.id}', '${o.status}')"><i class='bx bx-slider-alt'></i> Estatus</button>
            ${o.products && o.products.length > 0 ? `<button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="saltarAProducto('${o.products[0].productId}')"><i class='bx bx-box'></i> Producto</button>` : ''}
            <button class="${pagosBtnClass}" style="padding: 4px 8px; font-size: 0.8rem;" onclick="saltarAPagos('${o.id}')"><i class='bx bx-credit-card'></i> ${pagosBtnText}</button>
          </div>
        </td>
      </tr>
      `;
    }).join('');
    const html = `<table><thead><tr><th>ID</th><th>User ID</th><th>Monto</th><th>Estatus</th><th>Fecha</th><th>Acciones</th></tr></thead><tbody>${rows}</tbody></table>`;
    const el = document.getElementById('resp-ordenes');
    el.className = 'response success';
    el.innerHTML = html;
  } else {
    show('resp-ordenes', data, !ok);
  }
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
  
  show('resp-pagos', 'Buscando pagos y detalles de la orden...');
  
  const [resOrd, resPag] = await Promise.all([
    request('GET', '/ordenes/' + orderId),
    request('GET', '/pagos/orden/' + orderId)
  ]);

  if (resPag.ok && Array.isArray(resPag.data)) {
    let orderTotal = (resOrd.ok && resOrd.data) ? resOrd.data.totalAmount : null;
    
    if (resPag.data.length === 0) {
      const el = document.getElementById('resp-pagos');
      el.className = 'response success';
      let html = '';
      if (orderTotal !== null) {
        html = `<p>No hay pagos registrados para la orden ${orderId}. Faltan por pagar: $${orderTotal}</p>`;
        html += `<button class="btn-outline" style="margin-top: 10px; padding: 6px 12px; font-size: 0.85rem;" onclick="prepararCrearPago('${orderId}', '${orderTotal}')"><i class='bx bx-money'></i> Procesar Pago Faltante</button>`;
      } else {
        html = `<p>No hay pagos registrados para la orden ${orderId}.</p>`;
        html += `<button class="btn-outline" style="margin-top: 10px; padding: 6px 12px; font-size: 0.85rem;" onclick="prepararCrearPago('${orderId}', '')"><i class='bx bx-money'></i> Procesar Pago</button>`;
      }
      el.innerHTML = html;
    } else {
      const totalPaid = resPag.data.filter(p => p.status === 'COMPLETED').reduce((sum, p) => sum + (p.amount || 0), 0);
      let summary = `<div style="background: var(--mongo-gray); border-left: 4px solid var(--primary); padding: 15px; border-radius: 4px; margin-bottom: 20px;">
        <h3 style="margin-top:0; margin-bottom:10px; color: var(--primary);"><i class='bx bx-check-shield'></i> Resumen de Cuenta</h3>`;
        
      if (orderTotal !== null) {
        const falta = Math.max(0, orderTotal - totalPaid);
        summary += `<p style="margin: 5px 0;"><strong>Total de la Orden:</strong> $${orderTotal}</p>`;
        summary += `<p style="margin: 5px 0;"><strong>Total Pagado:</strong> $${totalPaid}</p>`;
        if (falta > 0) {
          summary += `<p style="margin: 5px 0; color: var(--yellow);"><strong>Falta por pagar:</strong> $${falta}</p>`;
          summary += `<button class="btn-outline" style="margin-top: 10px; padding: 6px 12px; font-size: 0.85rem;" onclick="prepararCrearPago('${orderId}', '${falta}')"><i class='bx bx-money'></i> Procesar Pago Faltante</button>`;
        } else {
          summary += `<p style="margin: 5px 0; color: var(--mongo-forest); font-weight: bold;"><i class='bx bx-check-circle'></i> ¡Orden pagada en su totalidad!</p>`;
        }
      } else {
        summary += `<p>Total Pagado: $${totalPaid}</p>`;
      }
      summary += `</div>`;
      
      const rows = resPag.data.map(p => `
        <tr>
          <td>${escHtml(p.id)}</td>
          <td>${escHtml(p.orderId)}</td>
          <td>$${escHtml(p.amount)}</td>
          <td>${getStatusBadge(p.status)}</td>
          <td>${escHtml(p.paymentMethod)}</td>
          <td>
            <button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="saltarAOrden('${p.orderId}')"><i class='bx bx-receipt'></i> Ver Orden</button>
          </td>
        </tr>
      `).join('');
      const html = summary + `<table><thead><tr><th>ID</th><th>Order ID</th><th>Monto</th><th>Estatus</th><th>Método</th><th>Acciones</th></tr></thead><tbody>${rows}</tbody></table>`;
      
      const el = document.getElementById('resp-pagos');
      el.className = 'response success';
      el.innerHTML = html;
    }
  } else {
    show('resp-pagos', resPag.data, true);
  }
}

async function reembolsarPago() {
  const id = document.getElementById('pg-reembolso-id').value.trim();
  if (!id) return show('resp-pagos', 'Ingresa un ID.', true);
  show('resp-pagos', 'Procesando reembolso...');
  const { ok, data } = await request('PUT', '/pagos/' + id + '/reembolso');
  show('resp-pagos', data, !ok);
}

async function listarPagos() {
  const el = document.getElementById('resp-pagos');
  el.className = 'response';
  el.innerHTML = '<p class="hint"><i class="bx bx-info-circle"></i> Utiliza las opciones de arriba para buscar pagos por ID o por Orden. El backend no tiene un endpoint para listar todos los pagos.</p>';
}

// ── Envíos ────────────────────────────────────────────────────
async function listarEnvios() {
  show('resp-envios', 'Cargando...');
  try {
    const { ok, data } = await brokerReq('GET', '/ordenes/envios');
    if (ok && data && data.data && Array.isArray(data.data)) {
      if (data.data.length === 0) return show('resp-envios', 'Sin envíos registrados');
      const rows = data.data.map(e => `
        <tr>
          <td>${escHtml(e.id)}</td>
          <td>${escHtml(e.orderId)}</td>
          <td>${escHtml(e.customerEmail)}</td>
          <td>${escHtml(e.status)}</td>
          <td>${escHtml(e.nextRunAt || e.sentAt || '')}</td>
        </tr>
      `).join('');
      const html = `<table><thead><tr><th>ID</th><th>Orden ID</th><th>Email</th><th>Estatus</th><th>Fecha</th></tr></thead><tbody>${rows}</tbody></table>`;
      const el = document.getElementById('resp-envios');
      el.className = 'response success';
      el.innerHTML = html;
    } else {
      show('resp-envios', data.data || data, !ok);
    }
  } catch (err) {
    show('resp-envios', 'Error al cargar envíos: ' + err.message, true);
  }
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


// ── Broker request helper ─────────────────────────────────────
async function brokerReq(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(BROKER + path, opts);
  const text = await res.text();
  try { return { ok: res.ok, data: JSON.parse(text) }; }
  catch { return { ok: res.ok, data: text }; }
}

// ── Broker ────────────────────────────────────────────────────
async function triggerRetry(type) {
  const entityId = document.getElementById(`br-${type}-entityid`).value.trim();
  const action = document.getElementById(`br-${type}-action`).value;
  const rawData = document.getElementById(`br-${type}-data`).value.trim();
  if (!entityId) return show('resp-broker', 'Ingresa un Entity ID.', true);
  let requestData;
  try { requestData = rawData ? JSON.parse(rawData) : {}; }
  catch { return show('resp-broker', 'El campo Request Data no es JSON válido.', true); }
  show('resp-broker', 'Enviando a Kafka...');
  const { ok, data } = await brokerReq('POST', `/retry/trigger/${type}`, { entityId, action, requestData });
  show('resp-broker', data, !ok);
}

async function listarJobs(type) {
  show('resp-broker', 'Cargando...');
  const { ok, data } = await brokerReq('GET', `/retry/${type}`);
  show('resp-broker', data, !ok);
}

async function getJobById() {
  const id = document.getElementById('br-job-id').value.trim();
  if (!id) return show('resp-broker', 'Ingresa un UUID de job.', true);
  show('resp-broker', 'Buscando...');
  const { ok, data } = await brokerReq('GET', `/retry/products/${id}`);
  show('resp-broker', data, !ok);
}

// ── Health Checks ─────────────────────────────────────────────
async function checkServiceHealth(service) {
  show('resp-health', 'Verificando...');
  if (service === 'broker') {
    const { ok, data } = await brokerReq('GET', '/actuator/health');
    show('resp-health', data, !ok);
  } else {
    const { ok, data } = await request('GET', '/actuator/health');
    show('resp-health', data, !ok);
  }
}

async function getEurekaApps() {
  show('resp-health', 'Cargando apps de Eureka...');
  try {
    const res = await fetch('/eureka-proxy/eureka/apps', { headers: { Accept: 'application/json' } });
    const text = await res.text();
    let parsed;
    try { parsed = JSON.parse(text); } catch { parsed = text; }
    show('resp-health', parsed, !res.ok);
  } catch (err) {
    show('resp-health', 'Error: ' + String(err), true);
  }
}

// ── Init ──────────────────────────────────────────────────────
checkHealth();
setInterval(checkHealth, 30000);

// ── Flujos E2E ────────────────────────────────────────────────
let e2eProductId = '';
let e2eOrderId = '';
let e2ePaymentId = '';

function updateE2eState() {
  document.getElementById('e2e-state').innerText = `Producto ID: ${e2eProductId || 'N/A'}
Orden ID: ${e2eOrderId || 'N/A'}
Pago ID: ${e2ePaymentId || 'N/A'}`;

  if (e2eOrderId) {
    document.getElementById('e2e-payment-order-id').value = e2eOrderId;
    document.getElementById('e2e-status-order-id').value = e2eOrderId;
  }
}

function resetE2eState() {
  e2eProductId = '';
  e2eOrderId = '';
  e2ePaymentId = '';

  document.getElementById('e2e-payment-order-id').value = '';
  document.getElementById('e2e-status-order-id').value = '';

  updateE2eState();
  show('resp-flujos', 'Variables y formularios reseteados.');
}

async function e2eCrearProducto() {
  const name = document.getElementById('e2e-p-name').value || 'Monitor Gamer';
  const price = parseFloat(document.getElementById('e2e-p-price').value) || 3500;
  const stock = parseInt(document.getElementById('e2e-p-stock').value) || 10;

  show('resp-flujos', `Creando producto "${name}"...`);
  const { ok, data } = await request('POST', '/productos', {
    name: name, description: "Demo E2E", price: price, stock: stock
  });
  if (ok && data && data.id) {
    e2eProductId = data.id;
    updateE2eState();
    show('resp-flujos', `Producto creado con ID: ${e2eProductId}\nStock inicial: ${stock}`);
  } else {
    show('resp-flujos', data, true);
  }
}

async function e2eCrearOrden() {
  if (!e2eProductId) return show('resp-flujos', 'Primero debes crear un producto (Paso 1).', true);

  const qty = parseInt(document.getElementById('e2e-o-qty').value) || 2;
  if (qty <= 0) {
    return show('resp-flujos', 'La cantidad solicitada debe ser mayor a cero.', true);
  }
  const price = parseFloat(document.getElementById('e2e-p-price').value) || 3500;
  const email = document.getElementById('e2e-customer-email').value || 'alumno@universidad.edu';
  const total = qty * price;

  show('resp-flujos', `Creando orden por ${qty} unidades (Total: $${total})...`);
  const { ok, data } = await request('POST', '/ordenes', {
    userId: 999, customerEmail: email, totalAmount: total, status: "PENDING",
    products: [{ productId: e2eProductId, quantity: qty }]
  });
  if (ok && data && data.id) {
    e2eOrderId = data.id;
    updateE2eState();
    show('resp-flujos', `Orden creada con ID: ${e2eOrderId}\nRevisa Kafka UI (inventory_update_events).`);
  } else {
    show('resp-flujos', data, true);
  }
}

async function e2eVerificarStock() {
  if (!e2eProductId) return show('resp-flujos', 'No hay Producto ID.', true);
  show('resp-flujos', 'Verificando stock...');
  const { ok, data } = await request('GET', '/productos/' + e2eProductId);
  if (ok && data) {
    show('resp-flujos', `Stock actual: ${data.stock} (Debe ser 8, ya que se restaron 2 en la orden)`);
  } else {
    show('resp-flujos', data, true);
  }
}

async function e2eProcesarPago(isTotal) {
  const orderId = document.getElementById('e2e-payment-order-id').value.trim();
  if (!orderId) return show('resp-flujos', 'Ingresa el ID de la orden a pagar.', true);

  const qty = parseInt(document.getElementById('e2e-o-qty').value) || 2;
  const price = parseFloat(document.getElementById('e2e-p-price').value) || 3500;
  const email = document.getElementById('e2e-customer-email').value || 'alumno@universidad.edu';

  let total = 0;
  if (isTotal) {
    total = qty * price;
    // Auto-completar el input para que el usuario vea cuánto se cobró
    document.getElementById('e2e-payment-amount').value = total;
  } else {
    const inputAmount = document.getElementById('e2e-payment-amount').value;
    if (!inputAmount) return show('resp-flujos', 'Ingresa el Monto a Pagar para el pago parcial.', true);
    total = parseFloat(inputAmount);
  }

  show('resp-flujos', `Procesando pago por $${total} para la orden ${orderId}...`);
  const { ok, data } = await request('POST', '/pagos/procesar', {
    orderId: orderId, amount: total, paymentMethod: "CREDIT_CARD", customerEmail: email
  });
  if (ok && data && data.id) {
    e2ePaymentId = data.id;
    updateE2eState();
    show('resp-flujos', `Pago procesado.\n1. Revisa MailHog para ver el correo de Pago Recibido.\n2. Espera 10 segundos y revisa MailHog para ver el correo de Confirmación de Orden.`);
  } else {
    show('resp-flujos', data, true);
  }
}

async function e2eVerPagosOrden() {
  const orderId = document.getElementById('e2e-payment-order-id').value.trim();
  if (!orderId) return show('resp-flujos', 'Ingresa el ID de la orden para buscar sus pagos.', true);

  show('resp-flujos', `Buscando pagos y calculando faltante para la orden ${orderId}...`);
  
  const [resOrd, resPag] = await Promise.all([
    request('GET', '/ordenes/' + orderId),
    request('GET', '/pagos/orden/' + orderId)
  ]);

  if (resPag.ok && Array.isArray(resPag.data)) {
    let orderTotal = resOrd.ok && resOrd.data ? resOrd.data.totalAmount : null;
    
    if (resPag.data.length === 0) {
      show('resp-flujos', `No hay pagos registrados para la orden ${orderId}. ${orderTotal ? `Falta por pagar: $${orderTotal}` : ''}`);
    } else {
      const totalPaid = resPag.data.filter(p => p.status === 'COMPLETED').reduce((sum, p) => sum + (p.amount || 0), 0);
      let summary = `Encontrados ${resPag.data.length} pago(s).\n\n`;
      if (orderTotal !== null) {
        const falta = Math.max(0, orderTotal - totalPaid);
        summary += `Total de la Orden: $${orderTotal}\nTotal Pagado: $${totalPaid}\n`;
        summary += falta > 0 ? `👉 FALTA POR PAGAR: $${falta}\n\n` : `¡ORDEN PAGADA COMPLETAMENTE!\n\n`;
      } else {
        summary += `Total Pagado: $${totalPaid}\n\n`;
      }
      summary += JSON.stringify(resPag.data, null, 2);
      show('resp-flujos', summary);
    }
  } else {
    show('resp-flujos', resPag.data, true);
  }
}

async function e2eActualizarEstatus() {
  const orderId = document.getElementById('e2e-status-order-id').value.trim();
  if (!orderId) return show('resp-flujos', 'Ingresa el ID de la orden.', true);

  const newStatus = document.getElementById('e2e-new-status').value;

  show('resp-flujos', `Cambiando estatus a ${newStatus} para la orden ${orderId}...`);
  const { ok, data } = await request('PUT', `/ordenes/${orderId}/status?status=${newStatus}`);
  if (ok) {
    show('resp-flujos', `Estatus actualizado a ${newStatus}.\nRevisa MailHog para ver el correo de Cambio de Estado.`);
  } else {
    show('resp-flujos', data, true);
  }
}

// ── Navegación Cruzada ────────────────────────────────────────
function saltarAPagos(orderId) {
  // 1. Cambiar a la pestaña de pagos
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Pagos'));
  if (tabBtn) showTab('pagos', tabBtn);

  // 2. Abrir el sub-tab de buscar por orden
  const section = document.getElementById('tab-pagos');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Pagos por Orden'));
  if (subTabBtn) showSubTab('pagos', 'sub-pg-search-order', subTabBtn);

  // 3. Rellenar input y ejecutar búsqueda
  document.getElementById('pg-orderid-buscar').value = orderId;
  getPagosPorOrden();
}

function saltarAOrden(orderId) {
  // 1. Cambiar a la pestaña de órdenes
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Órdenes'));
  if (tabBtn) showTab('ordenes', tabBtn);

  // 2. Abrir el sub-tab de buscar por ID
  const section = document.getElementById('tab-ordenes');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Buscar (ID)'));
  if (subTabBtn) showSubTab('ordenes', 'sub-o-search-id', subTabBtn);

  // 3. Rellenar input y ejecutar búsqueda
  document.getElementById('o-id').value = orderId;
  getOrden();
}

function saltarAProducto(productId) {
  // 1. Cambiar a la pestaña de productos
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Productos'));
  if (tabBtn) showTab('productos', tabBtn);

  // 2. Abrir el sub-tab de buscar por ID
  const section = document.getElementById('tab-productos');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Buscar (ID)'));
  if (subTabBtn) showSubTab('productos', 'sub-p-search-id', subTabBtn);

  // 3. Rellenar input y ejecutar búsqueda
  document.getElementById('p-id').value = productId;
  getProducto();
}

function prepararEdicionProducto(id, name, price, stock) {
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Productos'));
  if (tabBtn) showTab('productos', tabBtn);

  const section = document.getElementById('tab-productos');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Actualizar'));
  if (subTabBtn) showSubTab('productos', 'sub-p-update', subTabBtn);

  document.getElementById('p-update-id').value = id;
  document.getElementById('p-update-name').value = name;
  document.getElementById('p-update-price').value = price;
  document.getElementById('p-update-stock').value = stock;
  
  document.getElementById('p-update-id').focus();
}

function prepararEdicionOrden(id, status) {
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Órdenes'));
  if (tabBtn) showTab('ordenes', tabBtn);

  const section = document.getElementById('tab-ordenes');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Actualizar Estado'));
  if (subTabBtn) showSubTab('ordenes', 'sub-o-update', subTabBtn);

  document.getElementById('o-status-id').value = id;
  
  const sel = document.getElementById('o-nuevo-status');
  if (status) {
    for (let i = 0; i < sel.options.length; i++) {
      if (sel.options[i].value === status.toUpperCase()) {
        sel.selectedIndex = i;
        break;
      }
    }
  }
  
  document.getElementById('o-status-id').focus();
}

function prepararCrearPago(orderId, amount) {
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Pagos'));
  if (tabBtn) showTab('pagos', tabBtn);

  const section = document.getElementById('tab-pagos');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Procesar Pago'));
  if (subTabBtn) showSubTab('pagos', 'sub-pg-create', subTabBtn);

  document.getElementById('pg-orderid').value = orderId;
  document.getElementById('pg-amount').value = amount || '';
  
  document.getElementById('pg-amount').focus();
}

function prepararCrearOrden(productId, price) {
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Órdenes'));
  if (tabBtn) showTab('ordenes', tabBtn);

  const section = document.getElementById('tab-ordenes');
  const subTabBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Crear'));
  if (subTabBtn) showSubTab('ordenes', 'sub-o-create', subTabBtn);

  document.getElementById('o-productid').value = productId;
  document.getElementById('o-amount').value = price || '';
  
  // Hacemos focus en el User ID porque es el único campo manual faltante
  document.getElementById('o-userid').focus();
}

async function saltarAOrdenesDeProducto(productId) {
  // 1. Cambiar a la pestaña de órdenes
  const tabBtn = Array.from(document.querySelectorAll('.tab')).find(el => el.textContent.includes('Órdenes'));
  if (tabBtn) showTab('ordenes', tabBtn);

  // 2. Ocultar opciones y realizar búsqueda directa en memoria
  const section = document.getElementById('tab-ordenes');
  const hideBtn = Array.from(section.querySelectorAll('.sub-tab')).find(el => el.textContent.includes('Ocultar Opciones'));
  if (hideBtn) showSubTab('ordenes', '', hideBtn);

  show('resp-ordenes', 'Buscando órdenes...');
  const { ok, data } = await request('GET', '/ordenes');
  if (ok && Array.isArray(data)) {
    const matches = data.filter(o => o.products && o.products.some(p => p.productId === productId));
    if (matches.length === 0) {
      show('resp-ordenes', `No se encontraron órdenes para el producto ${productId}.`);
    } else {
      const summary = `<p class="hint"><i class='bx bx-info-circle'></i> Encontradas ${matches.length} orden(es) para el producto ${productId}</p>`;
      const rows = matches.map(o => {
        const isPending = (o.status && o.status.toUpperCase().includes('PEND'));
        const pagosBtnClass = isPending ? '' : 'btn-outline';
        const pagosBtnText = isPending ? 'Pagar' : 'Ver Pagos';
        return `
        <tr>
          <td>${escHtml(o.id)}</td>
          <td>${escHtml(o.userId)}</td>
          <td>$${escHtml(o.totalAmount)}</td>
          <td>${getStatusBadge(o.status)}</td>
          <td>${escHtml(o.createdAt || '')}</td>
          <td>
            <div style="display: flex; gap: 5px;">
              <button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="prepararEdicionOrden('${o.id}', '${o.status}')"><i class='bx bx-slider-alt'></i> Estatus</button>
              <button class="btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="saltarAProducto('${productId}')"><i class='bx bx-box'></i> Producto</button>
              <button class="${pagosBtnClass}" style="padding: 4px 8px; font-size: 0.8rem;" onclick="saltarAPagos('${o.id}')"><i class='bx bx-credit-card'></i> ${pagosBtnText}</button>
            </div>
          </td>
        </tr>
        `;
      }).join('');
      const html = `<table><thead><tr><th>ID</th><th>User ID</th><th>Monto</th><th>Estatus</th><th>Fecha</th><th>Acciones</th></tr></thead><tbody>${rows}</tbody></table>`;
      const el = document.getElementById('resp-ordenes');
      el.className = 'response success';
      el.innerHTML = summary + html;
    }
  } else {
    show('resp-ordenes', data, true);
  }
}

// ── Inicialización ─────────────────────────────────────────────
window.addEventListener('DOMContentLoaded', () => {
  listarProductos();
  checkHealth();
});
