/* Local, dependency-free presentation enhancements. No network requests. */
(function () {
  'use strict';
  const paths = {
    search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
    arrow:'<path d="M4 12h16m-6-6 6 6-6 6"/>',
    plus:'<path d="M12 4v16M4 12h16"/>',
    ticket:'<path d="M3 5h18v5a2 2 0 0 0 0 4v5H3v-5a2 2 0 0 0 0-4V5Z"/><path d="M15 5v3m0 3v2m0 3v3"/>',
    calendar:'<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 3v4m10-4v4M3 11h18"/>',
    wallet:'<path d="M20 8V5H5a3 3 0 0 0 0 6h15v10H5a3 3 0 0 1-3-3V8m18 6h-5v4h5"/>',
    user:'<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
    users:'<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m0-16a3 3 0 0 1 0 6m3 4a5 5 0 0 1 3 5"/>',
    bell:'<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',
    support:'<path d="M4 13v-2a8 8 0 0 1 16 0v2M4 12H2v8h5v-8H4m16 0h2v8h-5v-8h3m-3 8c0 2-3 2-5 2"/>',
    bus:'<rect x="4" y="3" width="16" height="17" rx="3"/><path d="M4 11h16M7 15h2m6 0h2M7 20v2m10-2v2"/>',
    send:'<path d="m22 2-7 20-4-9-9-4L22 2ZM11 13 22 2"/>',
    check:'<path d="m5 12 4 4L19 6"/>',
    close:'<path d="m6 6 12 12M6 18 18 6"/>',
    eye:'<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
    edit:'<path d="m15 4 5 5M4 20l5-1L21 7a2 2 0 0 0-4-4L5 15l-1 5Z"/>',
    save:'<path d="M4 3h13l4 4v14H3V3h1ZM7 3v6h10V3M7 21v-8h10v8"/>',
    download:'<path d="M12 3v12m-5-5 5 5 5-5M3 15v6h18v-6"/>',
    print:'<path d="M6 8V3h12v5M6 17H3V8h18v9h-3M6 14h12v7H6v-7Z"/>',
    card:'<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/>',
    star:'<path d="m12 3 3 6 6 1-4.5 4.5L18 21l-6-3-6 3 1.5-6.5L3 10l6-1 3-6Z"/>',
    logout:'<path d="M9 4H3v16h6m5-13 5 5-5 5M8 12h12"/>',
    login:'<path d="M15 4h6v16h-6M9 7l5 5-5 5M3 12h11"/>',
    reset:'<path d="M3 10a9 9 0 1 1 1 8M3 3v7h7"/>',
    home:'<path d="m3 10 9-7 9 7v11h-7v-7h-4v7H3V10Z"/>',
    chart:'<path d="M4 3v18h17M8 17v-5m5 5V7m5 10V4"/>',
    lock:'<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 14v3"/>'
  };
  const seatPath='<path d="M7.3 1.8C9 .6 11.7 2.1 12.3 4.4c.8 3.3-.4 6.1-2.7 6.7l-2.1.4c-1.1.2-1.8-.7-1.6-1.8l.6-5.3c.1-1.1.3-2 .8-2.6ZM6.7 12.2c2.8-.8 5.3-.1 5.4 2.5.1 2.6-1.2 5.7-.8 8.1.4 2.8 2.1 4.7 2.2 6.3.1 1.2-1.7 1.9-3.5 2.1-1.2.2-2-.2-2.3-1.3L3.8 15c-.4-1.7.9-2.3 2.9-2.8ZM12.4 30c4.6 1.1 10.3-.6 13.7-1.4 3.8-.9 5.7.7 5.7 3.1 0 2.2-1.7 3.3-4.3 3.3H12.6c-2 0-3.3-.8-3.3-2.4 0-1.7 1.2-3 3.1-2.6Z"/>';
  function icon(name) {
    if(name==='seat') return '<svg class="sr-icon sr-seat-icon" viewBox="0 0 32 36" fill="currentColor" aria-hidden="true" focusable="false">'+seatPath+'</svg>';
    return '<svg class="sr-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">'+(paths[name]||paths.arrow)+'</svg>';
  }
  window.SmartRoute.icon=icon;
  function kind(node){
    if(node.dataset.icon) return node.dataset.icon;
    const href=node.getAttribute('href')||'';
    if(href.includes('seat-selection')) return 'seat';
    if(href.includes('profile')) return 'user';
    const label=node.textContent.trim().toLowerCase();
    const choices=[[/seat/,'seat'],[/logout/,'logout'],[/login|sign in/,'login'],[/register|create account/,'user'],[/search|filter/,'search'],[/download|export/,'download'],[/print/,'print'],[/send|reply/,'send'],[/reset|refresh|reload/,'reset'],[/cancel|delete|remove|deactivate|disable/,'close'],[/review|feedback/,'star'],[/ticket|booking/,'ticket'],[/save|update/,'save'],[/edit/,'edit'],[/view|details/,'eye'],[/add|create|top.up/,'plus'],[/pay|card/,'card'],[/wallet/,'wallet'],[/notification/,'bell'],[/support/,'support'],[/schedule/,'calendar'],[/verify|confirm|activate|enable|mark/,'check'],[/password/,'lock'],[/home/,'home'],[/report|dashboard|admin/,'chart'],[/continue|next|back|previous|return/,'arrow']];
    const match=choices.find(([pattern])=>pattern.test(label));return match?match[1]:null;
  }
  function enhance(root){
    if(!(root instanceof Element)&&root!==document)return;
    const selector='.btn,.nav-link,[data-icon]';
    const nodes=[...(root.matches&&root.matches(selector)?[root]:[]),...root.querySelectorAll(selector)];
    nodes.forEach(node=>{if(node.querySelector('svg,img,.spinner')||node.closest('.sr-page-loader'))return;const name=kind(node);if(name)node.insertAdjacentHTML('afterbegin',icon(name));});
  }
  document.addEventListener('DOMContentLoaded',()=>{
    enhance(document);
    // Re-apply icons after async data renders or loading-label restoration.
    const pending=new Set();let scheduled=false;
    const observer=new MutationObserver(records=>{records.forEach(record=>{if(record.target instanceof Element)pending.add(record.target);record.addedNodes.forEach(n=>{if(n instanceof Element)pending.add(n);});});if(!scheduled){scheduled=true;requestAnimationFrame(()=>{scheduled=false;const roots=[...pending];pending.clear();roots.forEach(enhance);});}});
    observer.observe(document.body,{childList:true,subtree:true});
    document.querySelectorAll('.metric').forEach((metric,index)=>{const node=document.createElement('div');node.className='premium-metric-icon';node.innerHTML=icon(['users','ticket','calendar','wallet','support','card','chart'][index]||'chart');metric.prepend(node);});
    const form=document.getElementById('manualNotificationForm');
    if(form){const update=()=>{document.getElementById('premiumPreviewTitle').textContent=form.elements.title.value.trim()||'Notification title';document.getElementById('premiumPreviewMessage').textContent=form.elements.message.value.trim()||'Your message will appear here as you type.';};form.addEventListener('input',update);form.addEventListener('reset',()=>setTimeout(update,0));update();}
  });
})();
