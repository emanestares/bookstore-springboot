'use strict';
let allBooks = [];
let cart = JSON.parse(localStorage.getItem('cart') || '[]');
const coverCache = {};

// ── Pagination state ─────────────────────────────────────
const PAGE_SIZE = 10;
let currentPage = 1;

// ── Cover fetch ──────────────────────────────────────────
async function fetchCoversForBooks(books) {
  for (const b of books) {
    if (coverCache[b.id]) continue;
    const query = encodeURIComponent(b.title + ' ' + b.author);
    try {
      const res = await fetch('https://openlibrary.org/search.json?q=' + query + '&limit=1&fields=cover_i');
      const data = await res.json();
      const coverId = data?.docs?.[0]?.cover_i;
      if (coverId) {
        coverCache[b.id] = 'https://covers.openlibrary.org/b/id/' + coverId + '-M.jpg';
        const cardCover = document.querySelector('[data-book-id="' + b.id + '"] .book-cover');
        if (cardCover) {
          cardCover.style.backgroundImage = "url('" + coverCache[b.id] + "')";
          cardCover.style.backgroundSize = 'cover';
          cardCover.style.backgroundPosition = 'center';
          const spine = cardCover.querySelector('.cover-spine');
          if (spine) spine.remove();
          const cc = cardCover.querySelector('.cover-content');
          if (cc) { cc.style.background='linear-gradient(to top,rgba(0,0,0,0.75) 0%,transparent 100%)'; cc.style.position='absolute'; cc.style.bottom='0'; cc.style.left='0'; cc.style.right='0'; }
        }
      }
    } catch {}
  }
}

// ── Helpers ──────────────────────────────────────────────
function esc(s){ const d=document.createElement('div'); d.textContent=s??''; return d.innerHTML; }
function getUser(){ try{ return JSON.parse(localStorage.getItem('user')); }catch{ return null; } }
function isAdmin(){ return getUser()?.admin === true; }

// ── Theme ────────────────────────────────────────────────
function initTheme(){
  if(localStorage.getItem('theme')==='light') document.body.classList.add('light');
  updateThemeBtn();
}
function toggleTheme(){
  document.body.classList.toggle('light');
  localStorage.setItem('theme', document.body.classList.contains('light') ? 'light' : 'dark');
  updateThemeBtn();
}
function updateThemeBtn(){
  const btn = document.getElementById('themeBtn');
  if(btn) btn.textContent = document.body.classList.contains('light') ? '🌙' : '🌓';
}

// ── Admin Dropdown ───────────────────────────────────────
function toggleAdminDropdown(){
  document.getElementById('adminDropdown').classList.toggle('open');
}
// Close dropdown when clicking outside
document.addEventListener('click', e => {
  const dd = document.getElementById('adminDropdown');
  if(dd && !dd.contains(e.target)) dd.classList.remove('open');
});

// ── Nav ──────────────────────────────────────────────────
function initNav(){
  const u = getUser();
  if(u){
    const nu = document.getElementById('navUser');
    nu.textContent = u.name;
    nu.style.display = 'inline-block';
    document.getElementById('loginBtn').style.display  = 'none';
    document.getElementById('logoutBtn').style.display = 'inline-block';
    if(u.admin){
      document.getElementById('adminPanel').style.display    = 'block';
      document.getElementById('adminDropdown').style.display = 'inline-block';
      document.getElementById('ordersBtn').style.display     = 'none';
      document.getElementById('cartNavBtn').style.display    = 'none';
    } else {
      document.getElementById('ordersBtn').style.display  = 'inline-flex';
      document.getElementById('cartNavBtn').style.display = 'inline-flex';
    }
  }
}
function logout(){ localStorage.removeItem('user'); localStorage.removeItem('cart'); window.location.href='/login.html'; }

// ── Books ────────────────────────────────────────────────
async function loadBooks(){
  try{
    const res = await fetch('/api/books');
    if(!res.ok) throw new Error();
    allBooks = await res.json();
    buildCategoryFilter();
    renderBooks();
  } catch{
    document.getElementById('bookGrid').innerHTML =
      `<div class="state-box"><div class="s-icon">⚠️</div><p>Could not load books.<br/>Make sure the server is running on port 8080.</p></div>`;
  }
}

function buildCategoryFilter(){
  const cats = [...new Set(allBooks.map(b=>b.category).filter(Boolean))].sort();
  const sel = document.getElementById('catFilter');
  while(sel.options.length > 1) sel.remove(1);
  cats.forEach(c => { const o=document.createElement('option'); o.value=c; o.textContent=c; sel.appendChild(o); });
}

function coverClass(cat){
  const map={fiction:'cover-fiction',fantasy:'cover-fantasy',science:'cover-science',history:'cover-history',programming:'cover-programming',romance:'cover-romance',mystery:'cover-mystery','self-help':'cover-self-help'};
  return map[(cat||'').toLowerCase()] || 'cover-default';
}
function stockLabel(n){
  if(n<=0)  return `<span class="stock-tag stock-none">Out of stock</span>`;
  if(n<=5)  return `<span class="stock-tag stock-low">${n} left</span>`;
  return `<span class="stock-tag">${n} in stock</span>`;
}

// Reset to page 1 whenever filters/search change
function resetPageAndRender(){
  currentPage = 1;
  renderBooks();
}

function renderBooks(){
  const kw  = document.getElementById('searchInput').value.toLowerCase().trim();
  const cat = document.getElementById('catFilter').value;
  const srt = document.getElementById('sortFilter').value;
  const admin = isAdmin();

  let filtered = allBooks.filter(b => {
    const mkw  = !kw  || b.title.toLowerCase().includes(kw) || b.author.toLowerCase().includes(kw);
    const mcat = !cat || b.category === cat;
    return mkw && mcat;
  });
  if(srt==='price-asc')  filtered.sort((a,b)=>a.price-b.price);
  if(srt==='price-desc') filtered.sort((a,b)=>b.price-a.price);
  if(srt==='title')      filtered.sort((a,b)=>a.title.localeCompare(b.title));

  const totalBooks  = filtered.length;
  const totalPages  = Math.max(1, Math.ceil(totalBooks / PAGE_SIZE));

  // Clamp currentPage
  if(currentPage > totalPages) currentPage = totalPages;

  const start = (currentPage - 1) * PAGE_SIZE;
  const books = filtered.slice(start, start + PAGE_SIZE);

  document.getElementById('resultsCount').textContent =
    `${totalBooks} book${totalBooks!==1?'s':''} · page ${currentPage} of ${totalPages}`;

  if(!totalBooks){
    document.getElementById('bookGrid').innerHTML =
      `<div class="state-box"><div class="s-icon">🔍</div><p>No books match your search.</p></div>`;
    document.getElementById('pagination').innerHTML = '';
    return;
  }

  document.getElementById('bookGrid').innerHTML = books.map(b => {
    const imgUrl = coverCache[b.id] || null;
    const safeTitle = esc(b.title).replace(/'/g,"\\'");
    return `
    <div class="book-card" data-book-id="${b.id}" onclick="openModal(${b.id})">
      <div class="book-cover ${coverClass(b.category)}" style="${imgUrl ? `background-image:url('${imgUrl}');background-size:cover;background-position:center;` : ''}">
        ${imgUrl ? '' : '<div class="cover-spine" style="background:rgba(255,255,255,0.08)"></div>'}
        <div class="cover-content" style="${imgUrl ? 'background:linear-gradient(to top,rgba(0,0,0,0.75) 0%,transparent 100%);position:absolute;bottom:0;left:0;right:0;' : ''}">
          <div class="cover-title">${esc(b.title)}</div>
          <div class="cover-author">${esc(b.author)}</div>
        </div>
      </div>
      <div class="book-body">
        <div class="book-cat">${esc(b.category||'General')}</div>
        <div class="book-title">${esc(b.title)}</div>
        <div class="book-author">by ${esc(b.author)}</div>
        ${b.description ? `<div class="book-desc">${esc(b.description)}</div>` : ''}
        <div class="book-price-row">
          <span class="book-price">₱${Number(b.price).toFixed(2)}</span>
          ${stockLabel(b.stock||0)}
        </div>
      </div>
      <div class="book-footer">
        ${!admin ? `<button class="btn-add-cart" onclick="event.stopPropagation();addToCart(${b.id},'${safeTitle}',${b.price})"
          ${(b.stock||0)<=0?'disabled':''}>
          ${(b.stock||0)<=0 ? 'Out of Stock' : '+ Add to Cart'}
        </button>` : ''}
        ${admin ? `
        <div class="admin-card-actions">
          <button class="btn-card-edit" onclick="event.stopPropagation();openEditModal(${b.id})">✏️ Edit</button>
          <button class="btn-card-delete" onclick="event.stopPropagation();openDeleteConfirm(${b.id})">🗑 Delete</button>
        </div>` : ''}
      </div>
    </div>`;
  }).join('');

  renderPagination(totalPages);
  fetchCoversForBooks(books);
}

// ── Pagination ───────────────────────────────────────────
function renderPagination(totalPages){
  const el = document.getElementById('pagination');
  if(totalPages <= 1){ el.innerHTML = ''; return; }

  let html = '';

  // Prev button
  html += `<button class="page-btn" onclick="goToPage(${currentPage-1})" ${currentPage===1?'disabled':''}>‹ Prev</button>`;

  // Page number buttons — show up to 5 around current page
  const range = 2;
  const start = Math.max(1, currentPage - range);
  const end   = Math.min(totalPages, currentPage + range);

  if(start > 1){
    html += `<button class="page-btn" onclick="goToPage(1)">1</button>`;
    if(start > 2) html += `<span class="page-info">…</span>`;
  }
  for(let i = start; i <= end; i++){
    html += `<button class="page-btn ${i===currentPage?'active':''}" onclick="goToPage(${i})">${i}</button>`;
  }
  if(end < totalPages){
    if(end < totalPages - 1) html += `<span class="page-info">…</span>`;
    html += `<button class="page-btn" onclick="goToPage(${totalPages})">${totalPages}</button>`;
  }

  // Next button
  html += `<button class="page-btn" onclick="goToPage(${currentPage+1})" ${currentPage===totalPages?'disabled':''}>Next ›</button>`;

  el.innerHTML = html;
}

function goToPage(page){
  currentPage = page;
  renderBooks();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ── Admin Add ────────────────────────────────────────────
async function adminAddBook(){
  if(!isAdmin()){ toast('Admin access required','error'); return; }
  const book = {
    title:       document.getElementById('aTitle').value.trim(),
    author:      document.getElementById('aAuthor').value.trim(),
    price:       parseFloat(document.getElementById('aPrice').value)||0,
    stock:       parseInt(document.getElementById('aStock').value)||0,
    category:    document.getElementById('aCategory').value.trim(),
    description: document.getElementById('aDesc').value.trim(),
  };
  if(!book.title||!book.author){ toast('Title and author are required','error'); return; }
  if(book.price<0){ toast('Price cannot be negative','error'); return; }
  try{
    const res = await fetch('/api/books',{method:'POST',headers:{'Content-Type':'application/json','X-User-Admin':'true'},body:JSON.stringify(book)});
    const data = await res.json();
    if(!res.ok){ toast(data.message||'Failed to add book','error'); return; }
    toast(`"${book.title}" added to catalog`,'success');
    ['aTitle','aAuthor','aPrice','aStock','aCategory','aDesc'].forEach(id=>document.getElementById(id).value='');
    loadBooks();
  } catch{ toast('Network error','error'); }
}

// ── Admin Edit ───────────────────────────────────────────
function openEditModal(bookId){
  const b = allBooks.find(x=>x.id===bookId);
  if(!b) return;
  document.getElementById('editBookId').value   = b.id;
  document.getElementById('editTitle').value    = b.title;
  document.getElementById('editAuthor').value   = b.author;
  document.getElementById('editPrice').value    = b.price;
  document.getElementById('editStock').value    = b.stock;
  document.getElementById('editCategory').value = b.category||'';
  document.getElementById('editDesc').value     = b.description||'';
  document.getElementById('editModal').classList.add('open');
  document.body.style.overflow = 'hidden';
}
function closeEditModal(){
  document.getElementById('editModal').classList.remove('open');
  document.body.style.overflow = '';
}
function handleEditModalClick(e){ if(e.target===document.getElementById('editModal')) closeEditModal(); }

async function saveBookEdit(){
  const id     = parseInt(document.getElementById('editBookId').value);
  const title  = document.getElementById('editTitle').value.trim();
  const author = document.getElementById('editAuthor').value.trim();
  const price  = parseFloat(document.getElementById('editPrice').value)||0;
  const stock  = parseInt(document.getElementById('editStock').value)||0;
  const cat    = document.getElementById('editCategory').value.trim();
  const desc   = document.getElementById('editDesc').value.trim();

  if(!title||!author){ toast('Title and author are required','error'); return; }
  if(price<0){ toast('Price cannot be negative','error'); return; }
  if(stock<0){ toast('Stock cannot be negative','error'); return; }

  const btn = document.getElementById('saveBtn');
  btn.disabled=true; btn.textContent='Saving…';
  try{
    const res = await fetch(`/api/books/${id}`,{
      method:'PUT',
      headers:{'Content-Type':'application/json','X-User-Admin':'true'},
      body:JSON.stringify({title,author,price,stock,category:cat,description:desc})
    });
    const data = await res.json();
    if(!res.ok){ toast(data.message||'Update failed','error'); return; }
    const idx = allBooks.findIndex(x=>x.id===id);
    if(idx>=0) allBooks[idx] = data;
    toast(`"${title}" updated successfully`,'success');
    closeEditModal();
    if(modalBookId===id) openModal(id);
    renderBooks();
  } catch{ toast('Network error','error'); }
  finally{ btn.disabled=false; btn.textContent='Save Changes'; }
}

// ── Admin Delete ─────────────────────────────────────────
let pendingDeleteId = null;

function openDeleteConfirm(bookId){
  const b = allBooks.find(x=>x.id===bookId);
  if(!b) return;
  pendingDeleteId = bookId;
  document.getElementById('confirmMsg').textContent =
    `"${b.title}" will be permanently removed from the catalog. This action cannot be undone.`;

  const oldBtn = document.getElementById('confirmDeleteBtn');
  const newBtn = oldBtn.cloneNode(true);
  oldBtn.parentNode.replaceChild(newBtn, oldBtn);
  newBtn.addEventListener('click', () => executeDelete(bookId));

  const modal = document.getElementById('confirmModal');
  modal.classList.add('open');
  modal.style.display = 'flex';
  document.body.style.overflow = 'hidden';
}

function closeConfirmModal(){
  const modal = document.getElementById('confirmModal');
  modal.classList.remove('open');
  modal.style.display = 'none';
  document.body.style.overflow = '';
  pendingDeleteId = null;
}

async function executeDelete(bookId){
  const btn = document.getElementById('confirmDeleteBtn');
  btn.disabled=true; btn.textContent='Deleting…';
  try{
    const res = await fetch(`/api/books/${bookId}`,{method:'DELETE',headers:{'X-User-Admin':'true'}});
    if(!res.ok && res.status !== 404){
      const d = await res.json().catch(()=>({}));
      toast(d.message||'Delete failed','error');
      return;
    }
    allBooks = allBooks.filter(x=>x.id!==bookId);
    toast('Book removed from catalog','success');
    closeConfirmModal();
    if(modalBookId===bookId) closeModal();
    buildCategoryFilter();
    renderBooks();
  } catch{ toast('Network error','error'); }
  finally{ btn.disabled=false; btn.textContent='Delete'; }
}

// ── Cart ─────────────────────────────────────────────────
function addToCart(id, title, price){
  const existing = cart.find(i=>i.id===id);
  existing ? existing.qty++ : cart.push({id,title,price,qty:1});
  saveCart(); updateCartDot();
  toast(`"${title}" added to cart`,'success');
}
function saveCart(){ localStorage.setItem('cart',JSON.stringify(cart)); }
function updateCartDot(){
  const n = cart.reduce((s,i)=>s+i.qty,0);
  document.getElementById('cartDot').textContent = n;
}
function openCart(){ document.getElementById('cartDrawer').classList.add('open'); document.getElementById('overlay').classList.add('open'); renderDrawer(); }
function closeCart(){ document.getElementById('cartDrawer').classList.remove('open'); document.getElementById('overlay').classList.remove('open'); }

function coverClassSmall(title){
  const colors=['#1e3a5f','#2d1b4e','#0f3460','#3d2314','#0d2137','#3d1a2d'];
  return colors[(title.charCodeAt(0)||0)%colors.length];
}

function renderDrawer(){
  const body=document.getElementById('drawerBody');
  const foot=document.getElementById('drawerFooter');
  if(!cart.length){
    body.innerHTML=`<div class="empty-drawer"><div class="e-icon">📖</div><p>Your cart is empty.<br/><small>Find something great to read!</small></p></div>`;
    foot.style.display='none'; return;
  }
  body.innerHTML = cart.map((it,i)=>{
    const cover = coverCache[it.id];
    const thumbStyle = cover
      ? `background-image:url('${cover}');background-size:cover;background-position:center;`
      : `background:${coverClassSmall(it.title)};`;
    const thumbInner = cover ? '' : `<span style="font-size:0.55rem;padding:3px;line-height:1.1">${esc(it.title.slice(0,18))}</span>`;
    return `
    <div class="c-item">
      <div class="c-thumb" style="${thumbStyle}">${thumbInner}</div>
      <div class="c-info">
        <div class="c-title">${esc(it.title)}</div>
        <div class="c-price">₱${Number(it.price).toFixed(2)} each</div>
      </div>
      <div class="c-qty">
        <button class="q-btn" onclick="qtyChange(${i},-1)">−</button>
        <span class="q-num">${it.qty}</span>
        <button class="q-btn" onclick="qtyChange(${i},1)">+</button>
      </div>
      <button class="rm-btn" onclick="removeItem(${i})">✕</button>
    </div>`;
  }).join('');
  const total=cart.reduce((s,i)=>s+i.price*i.qty,0);
  document.getElementById('cartTotal').textContent=`₱${total.toFixed(2)}`;
  foot.style.display='block';
}

function qtyChange(i,d){ cart[i].qty+=d; if(cart[i].qty<=0) cart.splice(i,1); saveCart(); updateCartDot(); renderDrawer(); }
function removeItem(i){ cart.splice(i,1); saveCart(); updateCartDot(); renderDrawer(); }

// ── Checkout ─────────────────────────────────────────────
async function checkout(){
  const user = getUser();
  if(!user){ closeCart(); toast('Please login to place an order','info'); setTimeout(()=>window.location.href='/login.html',1200); return; }
  if(!cart.length){ toast('Your cart is empty','error'); return; }

  const btn = document.getElementById('checkoutBtn');
  btn.disabled=true; btn.textContent='Placing order…';

  const items = cart.map(i=>({bookId:i.id,bookTitle:i.title,priceAtPurchase:i.price,quantity:i.qty}));
  try{
    const res = await fetch('/api/orders',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({userId:user.id,items})});
    const data = await res.json();
    if(res.ok){
      cart=[]; saveCart(); updateCartDot(); renderDrawer(); closeCart();
      toast('Order placed! Redirecting to your orders…','success');
      await loadBooks();
      setTimeout(()=>window.location.href='/orders.html',1400);
    } else {
      toast(data.message||'Checkout failed. Please try again.','error');
    }
  } catch{ toast('Network error. Is the server running?','error'); }
  finally{ btn.disabled=false; btn.textContent='Place Order →'; }
}

// ── Toast ─────────────────────────────────────────────────
let toastTimer;
function toast(msg,type=''){
  clearTimeout(toastTimer);
  const el=document.getElementById('toast');
  el.textContent=msg; el.className=`toast ${type} show`;
  toastTimer=setTimeout(()=>el.className='toast',3500);
}

// ── Book Detail Modal ───────────────────────────────────
let modalBookId = null;
const gradMap = {fiction:'linear-gradient(145deg,#1e3a5f,#0f1f3d)',fantasy:'linear-gradient(145deg,#2d1b4e,#1a0f2e)',science:'linear-gradient(145deg,#0f3460,#16213e)',history:'linear-gradient(145deg,#3d2314,#1f0f00)',programming:'linear-gradient(145deg,#0d2137,#071525)',romance:'linear-gradient(145deg,#3d1a2d,#1f0015)',mystery:'linear-gradient(145deg,#1a1a2e,#0f0f1a)'};

function openModal(bookId){
  const b = allBooks.find(x=>x.id===bookId);
  if(!b) return;
  modalBookId = bookId;

  const coverEl = document.getElementById('modalCoverImg');
  const fallbackGrad = gradMap[(b.category||'').toLowerCase()] || 'linear-gradient(145deg,#1e2a3a,#0f1520)';

  function applyModalCover(url) {
    coverEl.style.cssText = url
      ? "position:absolute;inset:0;background:url('" + url + "') center/cover no-repeat;"
      : 'position:absolute;inset:0;background:' + fallbackGrad + ';';
  }

  applyModalCover(coverCache[bookId] ? coverCache[bookId].replace('-M.jpg', '-L.jpg') : null);

  if (!coverCache[bookId]) {
    const query = encodeURIComponent(b.title + ' ' + b.author);
    fetch('https://openlibrary.org/search.json?q=' + query + '&limit=1&fields=cover_i')
      .then(r => r.json())
      .then(data => {
        const coverId = data?.docs?.[0]?.cover_i;
        if (coverId) {
          coverCache[bookId] = 'https://covers.openlibrary.org/b/id/' + coverId + '-M.jpg';
          if (modalBookId === bookId) {
            applyModalCover('https://covers.openlibrary.org/b/id/' + coverId + '-L.jpg');
          }
          const cardCover = document.querySelector('[data-book-id="' + bookId + '"] .book-cover');
          if (cardCover) {
            cardCover.style.backgroundImage = "url('" + coverCache[bookId] + "')";
            cardCover.style.backgroundSize = 'cover';
            cardCover.style.backgroundPosition = 'center';
          }
        }
      }).catch(() => {});
  }

  document.getElementById('modalTitle').textContent = b.title;
  document.getElementById('modalAuthorCover').textContent = 'by ' + b.author;
  document.getElementById('modalCat').textContent = b.category || 'General';
  document.getElementById('modalPrice').textContent = '₱' + Number(b.price).toFixed(2);

  const descEl = document.getElementById('modalDesc');
  if(b.description){ descEl.textContent=b.description; descEl.classList.remove('modal-desc-empty'); }
  else { descEl.textContent='No description available.'; descEl.classList.add('modal-desc-empty'); }

  const stockEl = document.getElementById('modalStockTag');
  const cartBtn = document.getElementById('modalCartBtn');
  cartBtn.style.display = isAdmin() ? 'none' : '';
  if((b.stock||0)<=0){ stockEl.textContent='Out of Stock'; stockEl.className='modal-stock-tag modal-stock-none'; cartBtn.disabled=true; cartBtn.textContent='Out of Stock'; }
  else if(b.stock<=5){ stockEl.textContent=b.stock+' left'; stockEl.className='modal-stock-tag modal-stock-low'; cartBtn.disabled=false; cartBtn.textContent='+ Add to Cart'; }
  else { stockEl.textContent=b.stock+' in stock'; stockEl.className='modal-stock-tag modal-stock-ok'; cartBtn.disabled=false; cartBtn.textContent='+ Add to Cart'; }

  const footerBtns = document.getElementById('modalFooterBtns');
  ['modalEditBtn','modalDeleteBtn'].forEach(id=>{ const el=document.getElementById(id); if(el) el.remove(); });
  if(isAdmin()){
    const editBtn = document.createElement('button');
    editBtn.id='modalEditBtn'; editBtn.className='btn-modal-edit'; editBtn.textContent='✏️ Edit';
    editBtn.onclick = () => { closeModal(); openEditModal(bookId); };
    const delBtn = document.createElement('button');
    delBtn.id='modalDeleteBtn'; delBtn.className='btn-modal-delete'; delBtn.textContent='🗑 Delete';
    delBtn.onclick = () => { closeModal(); openDeleteConfirm(bookId); };
    footerBtns.insertBefore(delBtn, cartBtn);
    footerBtns.insertBefore(editBtn, delBtn);
  }

  document.getElementById('bookModal').classList.add('open');
  document.body.style.overflow = 'hidden';
}

function closeModal(){
  document.getElementById('bookModal').classList.remove('open');
  document.body.style.overflow = '';
  modalBookId = null;
}
function handleModalClick(e){ if(e.target===document.getElementById('bookModal')) closeModal(); }
function modalAddToCart(){
  const b=allBooks.find(x=>x.id===modalBookId);
  if(!b) return;
  addToCart(b.id,b.title,b.price);
  closeModal();
}

document.addEventListener('keydown', e=>{
  if(e.key==='Escape'){
    if(document.getElementById('confirmModal').classList.contains('open')){ closeConfirmModal(); return; }
    if(document.getElementById('editModal').classList.contains('open')){ closeEditModal(); return; }
    closeModal();
  }
});

// ── Init ──────────────────────────────────────────────────
initTheme();
initNav();
updateCartDot();
loadBooks();