'use strict';

let allOrders = [];
const userCache   = {}; // { userId: { name, email } }
const spineColors = ['#1e3a5f','#2d1b4e','#0f3460','#3d2314','#0d2137','#3d1a2d','#1a3a2d','#1a1a2e'];

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
  requireAdmin();
  initTheme();
  loadOrders();
});

/* ── Helpers ── */
function esc(s) {
  const d = document.createElement('div');
  d.textContent = s ?? '';
  return d.innerHTML;
}

function spineColor(t) {
  return spineColors[(t?.charCodeAt(0) || 0) % spineColors.length];
}

function formatDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleString('en-PH', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

function statusLabel(s) {
  const map = { PREPARING: 'Preparing', TO_DELIVER: 'To Deliver', DECLINED: 'Declined' };
  return map[s] || s;
}

async function fetchUserName(userId) {
  if (userCache[userId]) return userCache[userId];
  try {
    const res = await fetch(`/api/auth/users/${userId}`, { headers: { 'X-User-Admin': 'true' } });
    if (!res.ok) throw new Error();
    const data = await res.json();
    userCache[userId] = { name: data.name || `User #${userId}`, email: data.email || '' };
  } catch {
    userCache[userId] = { name: `User #${userId}`, email: '' };
  }
  return userCache[userId];
}

/* ── Load Orders ── */
async function loadOrders() {
  const tbody = document.getElementById('tableBody');
  tbody.innerHTML = '<tr class="state-row"><td colspan="7"><span class="state-icon">⏳</span>Loading orders…</td></tr>';

  try {
    const res = await fetch('/api/orders/all', { headers: { 'X-User-Admin': 'true' } });
    if (!res.ok) throw new Error('Forbidden');
    allOrders = await res.json();

    // Pre-fetch all unique user names in parallel
    const uniqueUserIds = [...new Set(allOrders.map(o => o.userId))];
    await Promise.all(uniqueUserIds.map(id => fetchUserName(id)));

    buildStats();
    renderTable();
  } catch {
    tbody.innerHTML = '<tr class="state-row"><td colspan="7"><span class="state-icon">⚠️</span>Could not load orders. Make sure the server is running.</td></tr>';
  }
}

/* ── Stats ── */
function buildStats() {
  const total   = allOrders.length;
  const prep    = allOrders.filter(o => o.status === 'PREPARING').length;
  const deliver = allOrders.filter(o => o.status === 'TO_DELIVER').length;
  const declined= allOrders.filter(o => o.status === 'DECLINED').length;
  const revenue = allOrders
    .filter(o => o.status !== 'DECLINED')
    .reduce((s, o) => s + (o.totalAmount || 0), 0);

  document.getElementById('statsBar').innerHTML = `
    <div class="stat-chip">
      <span class="stat-num">${total}</span>
      <span class="stat-label">Total Orders</span>
    </div>
    <div class="stat-chip preparing">
      <span class="stat-num">${prep}</span>
      <span class="stat-label">Preparing</span>
    </div>
    <div class="stat-chip deliver">
      <span class="stat-num">${deliver}</span>
      <span class="stat-label">To Deliver</span>
    </div>
    <div class="stat-chip declined">
      <span class="stat-num">${declined}</span>
      <span class="stat-label">Declined</span>
    </div>
    <div class="stat-chip">
      <span class="stat-num">₱${revenue.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
      <span class="stat-label">Revenue</span>
    </div>`;
}

/* ── Table Rendering ── */
function renderTable() {
  const kw     = document.getElementById('searchInput').value.toLowerCase().trim();
  const status = document.getElementById('statusFilter').value;

  const filtered = allOrders.filter(o => {
    const userName  = (userCache[o.userId]?.name  || '').toLowerCase();
    const userEmail = (userCache[o.userId]?.email || '').toLowerCase();
    const matchKw     = !kw || String(o.id).includes(kw) || userName.includes(kw) || userEmail.includes(kw);
    const matchStatus = !status || o.status === status;
    return matchKw && matchStatus;
  });

  document.getElementById('countTag').textContent =
    `${filtered.length} order${filtered.length !== 1 ? 's' : ''}`;

  const tbody = document.getElementById('tableBody');
  if (!filtered.length) {
    tbody.innerHTML = '<tr class="state-row"><td colspan="7"><span class="state-icon">📭</span>No orders match your filter.</td></tr>';
    return;
  }

  tbody.innerHTML = filtered.map(o => {
    const items   = o.items || [];
    const preview = items.slice(0, 2);
    const more    = items.length - 2;

    return `
    <tr id="row-${o.id}">
      <td style="width:32px;padding-right:0">
        <button class="expand-btn" id="expand-${o.id}" onclick="toggleDetail(${o.id})" title="View items">▶</button>
      </td>
      <td class="td-id">#${o.id}</td>
      <td class="td-user">
        <strong>${esc(userCache[o.userId]?.name || 'User #' + o.userId)}</strong>
        <small>${esc(userCache[o.userId]?.email || '')}</small>
      </td>
      <td class="td-date">${formatDate(o.orderDate)}</td>
      <td class="td-items">
        <div class="items-list">
          ${preview.map(i => `
            <div class="item-line">
              <div class="item-spine" style="background:${spineColor(i.bookTitle)}"></div>
              <span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:160px">${esc(i.bookTitle || 'Book')}</span>
              <span style="color:var(--text3)">×${i.quantity}</span>
            </div>`).join('')}
          ${more > 0 ? `<div class="items-more">+${more} more</div>` : ''}
        </div>
      </td>
      <td class="td-total">₱${Number(o.totalAmount || 0).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
      <td>
        <select class="status-select" id="sel-${o.id}" onchange="updateStatus(${o.id}, this)">
          <option value="PREPARING"  ${o.status === 'PREPARING'  ? 'selected' : ''}>🟡 Preparing</option>
          <option value="TO_DELIVER" ${o.status === 'TO_DELIVER' ? 'selected' : ''}>🔵 To Deliver</option>
          <option value="DECLINED"   ${o.status === 'DECLINED'   ? 'selected' : ''}>🔴 Declined</option>
        </select>
      </td>
    </tr>
    <tr class="detail-row" id="detail-${o.id}">
      <td class="detail-cell" colspan="7">
        <div class="detail-inner">
          <div class="detail-head">
            <span>Book</span><span>Qty</span><span>Subtotal</span>
          </div>
          ${items.map(i => `
            <div class="detail-item">
              <div class="di-name">${esc(i.bookTitle || '—')}</div>
              <div class="di-qty">×${i.quantity}</div>
              <div class="di-sub">₱${(Number(i.priceAtPurchase || 0) * i.quantity).toFixed(2)}</div>
            </div>`).join('')}
        </div>
      </td>
    </tr>`;
  }).join('');
}

/* ── Expand Detail Row ── */
function toggleDetail(id) {
  const detailRow = document.getElementById(`detail-${id}`);
  const btn       = document.getElementById(`expand-${id}`);
  const open      = detailRow.classList.toggle('open');
  btn.textContent = open ? '▼' : '▶';
}

/* ── Update Order Status ── */
async function updateStatus(orderId, selectEl) {
  const newStatus = selectEl.value;
  selectEl.classList.add('updating');

  try {
    const res  = await fetch(`/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'X-User-Admin': 'true' },
      body: JSON.stringify({ status: newStatus })
    });
    const data = await res.json();
    if (!res.ok) { toast(data.message || 'Update failed', 'error'); return; }

    const order = allOrders.find(o => o.id === orderId);
    if (order) order.status = newStatus;

    buildStats();
    toast(`Order #${orderId} → ${statusLabel(newStatus)}`, 'success');
  } catch {
    toast('Network error', 'error');
  } finally {
    selectEl.classList.remove('updating');
  }
}
