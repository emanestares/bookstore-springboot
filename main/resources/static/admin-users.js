'use strict';

const PAGE_SIZE = 10;
let currentPage = 1, allUsers = [], sortKey = 'id', sortAsc = true;

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
  requireAdmin();
  initTheme();
  loadUsers();

  document.addEventListener('keydown', e => { if (e.key === 'Escape') closeOrdersModal(); });
  document.getElementById('ordersModal').addEventListener('click', e => {
    if (e.target === document.getElementById('ordersModal')) closeOrdersModal();
  });
});

/* ── Tab Switching ── */
function switchTab(tab) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.getElementById('tab-' + tab).classList.add('active');
  document.getElementById('panel-' + tab).classList.add('active');
}

/* ── Load Users ── */
async function loadUsers() {
  const tbody = document.getElementById('tableBody');
  tbody.innerHTML = '<tr class="state-row"><td colspan="5"><span class="state-icon">⏳</span>Loading users…</td></tr>';
  try {
    const res = await fetch('/api/auth/users', { headers: { 'X-User-Admin': 'true' } });
    if (!res.ok) throw new Error();
    allUsers = await res.json();
    buildStats();
    renderTable();
  } catch {
    tbody.innerHTML = '<tr class="state-row"><td colspan="5"><span class="state-icon">⚠️</span>Could not load users.</td></tr>';
  }
}

/* ── Stats ── */
function buildStats() {
  const total  = allUsers.length;
  const admins = allUsers.filter(u => u.admin).length;
  document.getElementById('statsBar').innerHTML = `
    <div class="stat-chip">
      <span class="stat-num">${total}</span>
      <span class="stat-label">Total Users</span>
    </div>
    <div class="stat-chip s-admins">
      <span class="stat-num">${admins}</span>
      <span class="stat-label">Admins</span>
    </div>
    <div class="stat-chip s-users">
      <span class="stat-num">${total - admins}</span>
      <span class="stat-label">Regular Users</span>
    </div>`;
}

/* ── Sorting ── */
function setSort(key) {
  if (sortKey === key) sortAsc = !sortAsc;
  else { sortKey = key; sortAsc = true; }
  currentPage = 1;
  renderTable();
}

function updateSortIndicators() {
  ['id', 'name', 'email'].forEach(k => {
    const th = document.getElementById('th-' + k);
    const si = document.getElementById('si-' + k);
    if (!th || !si) return;
    th.classList.toggle('sorted', sortKey === k);
    si.textContent = sortKey === k ? (sortAsc ? ' ↑' : ' ↓') : '';
  });
}

function resetAndRender() { currentPage = 1; renderTable(); }

/* ── Table Rendering ── */
function renderTable() {
  const kw   = document.getElementById('searchInput').value.toLowerCase().trim();
  const role = document.getElementById('roleFilter').value;

  let list = allUsers.filter(u => {
    const mk = !kw || u.name?.toLowerCase().includes(kw)
                   || u.email?.toLowerCase().includes(kw)
                   || String(u.id).includes(kw);
    const mr = !role || (role === 'admin' ? u.admin : !u.admin);
    return mk && mr;
  });

  list.sort((a, b) => {
    let av = a[sortKey] ?? '', bv = b[sortKey] ?? '';
    if (typeof av === 'string') { av = av.toLowerCase(); bv = bv.toLowerCase(); }
    return av < bv ? (sortAsc ? -1 : 1) : av > bv ? (sortAsc ? 1 : -1) : 0;
  });

  updateSortIndicators();

  const total      = list.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  if (currentPage > totalPages) currentPage = totalPages;
  const page = list.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  document.getElementById('countTag').textContent =
    `${total} user${total !== 1 ? 's' : ''} · page ${currentPage} of ${totalPages}`;

  const tbody = document.getElementById('tableBody');
  if (!total) {
    tbody.innerHTML = '<tr class="state-row"><td colspan="5"><span class="state-icon">🔍</span>No users match your filter.</td></tr>';
    document.getElementById('pagination').innerHTML = '';
    return;
  }

  const me = getUser()?.id;
  tbody.innerHTML = page.map(u => `
    <tr class="user-row" onclick="openOrdersModal(${u.id})" title="View orders for ${escapeHtml(u.name || '')}">
      <td class="td-id">#${u.id}</td>
      <td style="width:36px;padding-right:0">
        <div class="td-avatar" style="background:${avatarColor(u.name)}">${avatarInitial(u.name)}</div>
      </td>
      <td class="td-name">
        ${escapeHtml(u.name || '—')}
        ${u.id === me ? '<span style="font-size:0.68rem;color:var(--accent);margin-left:4px">(you)</span>' : ''}
      </td>
      <td class="td-email">${escapeHtml(u.email || '—')}</td>
      <td>
        <span class="role-badge ${u.admin ? 'role-admin' : 'role-user'}">${u.admin ? 'Admin' : 'User'}</span>
      </td>
    </tr>`).join('');

  renderPagination(totalPages);
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/* ── Pagination ── */
function renderPagination(totalPages) {
  const el = document.getElementById('pagination');
  if (totalPages <= 1) { el.innerHTML = ''; return; }
  const r = 2, s = Math.max(1, currentPage - r), e = Math.min(totalPages, currentPage + r);
  let h = `<button class="page-btn" onclick="goToPage(${currentPage - 1})" ${currentPage === 1 ? 'disabled' : ''}>‹ Prev</button>`;
  if (s > 1) {
    h += `<button class="page-btn" onclick="goToPage(1)">1</button>`;
    if (s > 2) h += `<span class="page-info">…</span>`;
  }
  for (let i = s; i <= e; i++)
    h += `<button class="page-btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
  if (e < totalPages) {
    if (e < totalPages - 1) h += `<span class="page-info">…</span>`;
    h += `<button class="page-btn" onclick="goToPage(${totalPages})">${totalPages}</button>`;
  }
  h += `<button class="page-btn" onclick="goToPage(${currentPage + 1})" ${currentPage === totalPages ? 'disabled' : ''}>Next ›</button>`;
  el.innerHTML = h;
}

function goToPage(p) {
  currentPage = p;
  renderTable();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

/* ── Orders Modal ── */
async function openOrdersModal(userId) {
  const u = allUsers.find(x => x.id === userId);
  if (!u) return;

  document.getElementById('ordersModalTitle').textContent = u.name || `User #${userId}`;
  document.getElementById('ordersModalSub').textContent   = u.email || '';
  document.getElementById('ordersModalBody').innerHTML    =
    '<div style="text-align:center;padding:2rem;color:var(--text-secondary)">⏳ Loading orders…</div>';

  const modal = document.getElementById('ordersModal');
  modal.classList.add('open');
  modal.style.display = 'flex';
  document.body.style.overflow = 'hidden';

  try {
    const res = await fetch(`/api/orders?userId=${userId}`);
    if (!res.ok) throw new Error();
    const orders = await res.json();
    renderOrdersInModal(orders);
  } catch {
    document.getElementById('ordersModalBody').innerHTML =
      '<div style="text-align:center;padding:2rem;color:#ef4444">⚠️ Could not load orders.</div>';
  }
}

function closeOrdersModal() {
  const modal = document.getElementById('ordersModal');
  modal.classList.remove('open');
  modal.style.display = 'none';
  document.body.style.overflow = '';
}

function renderOrdersInModal(orders) {
  const body = document.getElementById('ordersModalBody');

  if (!orders || orders.length === 0) {
    body.innerHTML = '<div style="text-align:center;padding:2rem;color:var(--text-secondary)">📭 No orders found for this user.</div>';
    return;
  }

  const statusColor = { PREPARING: '#f59e0b', TO_DELIVER: '#3b82f6', DECLINED: '#ef4444' };
  const statusLabel = { PREPARING: 'Preparing', TO_DELIVER: 'To Deliver', DECLINED: 'Declined' };

  const sorted = [...orders].sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate));

  body.innerHTML = sorted.map(o => {
    const color = statusColor[o.status] || '#6b7280';
    const label = statusLabel[o.status] || o.status;
    const date  = o.orderDate
      ? new Date(o.orderDate).toLocaleString('en-PH', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
      : '—';
    const items = (o.items || []).map(i => `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:.35rem 0;border-bottom:1px solid var(--border);font-size:0.85rem">
        <span style="color:var(--text-primary)">${escapeHtml(i.bookTitle || `Book #${i.bookId}`)}
          <span style="color:var(--text-secondary);margin-left:4px">×${i.quantity}</span>
        </span>
        <span style="color:var(--text-secondary);white-space:nowrap;margin-left:1rem">₱${(i.priceAtPurchase * i.quantity).toFixed(2)}</span>
      </div>`).join('');

    return `
      <div style="border:1px solid var(--border);border-radius:10px;padding:1rem;margin-bottom:.85rem">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:.6rem;gap:.5rem;flex-wrap:wrap">
          <div>
            <span style="font-weight:600;font-size:0.9rem">Order #${o.id}</span>
            <span style="font-size:0.78rem;color:var(--text-secondary);margin-left:.5rem">${date}</span>
          </div>
          <span style="font-size:0.75rem;font-weight:600;padding:.25rem .65rem;border-radius:20px;background:${color}22;color:${color};white-space:nowrap">${label}</span>
        </div>
        <div style="margin-bottom:.5rem">${items}</div>
        <div style="text-align:right;font-weight:600;font-size:0.9rem;color:var(--accent)">Total: ₱${o.totalAmount.toFixed(2)}</div>
      </div>`;
  }).join('');
}

/* ── Add User ── */
async function submitAddUser() {
  const name     = document.getElementById('uName').value.trim();
  const email    = document.getElementById('uEmail').value.trim();
  const password = document.getElementById('uPassword').value;
  const isAdmin  = document.querySelector('input[name="newRole"]:checked').value === 'admin';

  if (!name)              { toast('Name is required', 'error'); return; }
  if (!email)             { toast('Email is required', 'error'); return; }
  if (password.length < 6){ toast('Password must be at least 6 characters', 'error'); return; }

  const btn = document.getElementById('submitBtn');
  btn.disabled = true; btn.textContent = 'Creating…';

  try {
    const res  = await fetch('/api/auth/admin/create-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-User-Admin': 'true' },
      body: JSON.stringify({ name, email, password, admin: isAdmin })
    });
    const data = await res.json();
    if (!res.ok) { toast(data.message || 'Failed to create user', 'error'); return; }

    const banner = document.getElementById('successBanner');
    document.getElementById('successMsg').textContent =
      `Account created for ${data.name} (${isAdmin ? 'Admin' : 'User'})`;
    banner.classList.add('show');
    setTimeout(() => banner.classList.remove('show'), 5000);

    document.getElementById('uName').value     = '';
    document.getElementById('uEmail').value    = '';
    document.getElementById('uPassword').value = '';
    document.getElementById('newRoleUser').checked = true;
    updateStrength();

    await loadUsers();
    switchTab('dashboard');
    toast(data.name + ' added successfully', 'success');
  } catch {
    toast('Network error', 'error');
  } finally {
    btn.disabled = false; btn.textContent = 'Create Account';
  }
}