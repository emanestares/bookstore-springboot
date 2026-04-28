'use strict';

/* ── Auth Helpers ── */
function getUser() {
  try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
}

function logout() {
  localStorage.removeItem('user');
  localStorage.removeItem('cart');
  window.location.href = '/login.html';
}

function requireAdmin() {
  const u = getUser();
  if (!u || !u.admin) window.location.href = '/index.html';
}

/* ── Theme ── */
function initTheme() {
  if (localStorage.getItem('theme') === 'light') document.body.classList.add('light');
  updateThemeBtn();
}

function toggleTheme() {
  document.body.classList.toggle('light');
  localStorage.setItem('theme', document.body.classList.contains('light') ? 'light' : 'dark');
  updateThemeBtn();
}

function updateThemeBtn() {
  const b = document.getElementById('themeBtn');
  if (b) b.textContent = document.body.classList.contains('light') ? '🌙' : '🌓';
}

/* ── Admin Dropdown ── */
function toggleAdminDropdown() {
  document.getElementById('adminDropdown').classList.toggle('open');
}

document.addEventListener('click', e => {
  const dd = document.getElementById('adminDropdown');
  if (dd && !dd.contains(e.target)) dd.classList.remove('open');
});

/* ── Toast ── */
let toastTimer;
function toast(msg, type = '') {
  clearTimeout(toastTimer);
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = `toast ${type} show`;
  toastTimer = setTimeout(() => el.className = 'toast', 3500);
}

/* ── Avatar Helpers ── */
const avatarColors = ['#1e3a5f','#2d1b4e','#0f3460','#3d2314','#0d2137','#3d1a2d','#1a3a2d','#1a1a2e'];
function avatarColor(n) { return avatarColors[(n?.charCodeAt(0) || 65) % avatarColors.length]; }
function avatarInitial(n) { return (n || '?')[0].toUpperCase(); }

/* ── Password Strength ── */
function updateStrength(inputId = 'uPassword', barId = 'pwBar', hintId = 'pwHint') {
  const pw   = document.getElementById(inputId)?.value ?? '';
  const bar  = document.getElementById(barId);
  const hint = document.getElementById(hintId);
  if (!bar || !hint) return;

  let score = 0;
  if (pw.length >= 6)          score++;
  if (pw.length >= 10)         score++;
  if (/[A-Z]/.test(pw))        score++;
  if (/[0-9]/.test(pw))        score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;

  const levels = [
    { w: '0%',   bg: 'transparent', text: 'Enter a password' },
    { w: '25%',  bg: '#ef4444',     text: 'Too short'  },
    { w: '50%',  bg: '#f59e0b',     text: 'Weak'       },
    { w: '70%',  bg: '#eab308',     text: 'Fair'       },
    { w: '85%',  bg: '#84cc16',     text: 'Good'       },
    { w: '100%', bg: '#22c55e',     text: 'Strong'     },
  ];
  const lvl = pw.length === 0 ? 0 : Math.max(1, Math.min(score, 5));
  bar.style.width      = levels[lvl].w;
  bar.style.background = levels[lvl].bg;
  hint.textContent     = levels[lvl].text;
}
