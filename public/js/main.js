(function () {
  'use strict';

  // 토큰 찾기: window.csrfToken, meta, hidden input 등 다양한 위치 시도
  function getCsrfToken() {
    if (window.csrfToken) return window.csrfToken;
    const meta = document.querySelector('meta[name="csrf-token"]');
    if (meta) return meta.getAttribute('content');
    const input = document.querySelector('input[name="_csrf"]');
    if (input) return input.value;
    return null;
  }

  const csrfToken = getCsrfToken();

  async function fetchWithCSRF(url, options = {}) {
    options = options || {};
    options.headers = options.headers || {};

    // 기본으로 JSON
    if (!(options.body instanceof FormData)) {
      options.headers['Content-Type'] = options.headers['Content-Type'] || 'application/json';
    }

    // CSRF 토큰을 header로 첨부 (csurf가 header 'x-csrf-token' 또는 'x-xsrf-token'을 허용)
    if (csrfToken) {
      options.headers['x-csrf-token'] = csrfToken;
      options.headers['x-xsrf-token'] = csrfToken;
    }

    const res = await fetch(url, options);
    // 자동 에러 표시 (JSON이면 메시지 읽음)
    if (!res.ok) {
      let text;
      try { text = await res.json(); } catch (e) { text = await res.text(); }
      throw { status: res.status, body: text };
    }
    // try parse json
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) return res.json();
    return res.text();
  }

  // 간단한 토스트 (DOM에 #toast-container 생성)
  function showToast(message, { duration = 3000, type = 'info' } = {}) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.style.position = 'fixed';
      container.style.right = '20px';
      container.style.bottom = '20px';
      container.style.zIndex = 9999;
      document.body.appendChild(container);
    }
    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.style.marginTop = '8px';
    el.style.padding = '10px 14px';
    el.style.borderRadius = '8px';
    el.style.boxShadow = '0 4px 12px rgba(0,0,0,0.12)';
    el.style.background = type === 'error' ? '#ffdddd' : (type === 'success' ? '#ddffea' : '#ffffff');
    el.style.color = '#222';
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => {
      el.style.transition = 'opacity 300ms';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 350);
    }, duration);
  }

  // data-ajax="true"가 있는 폼은 AJAX로 전송 (응답이 redirect-url 을 주면 그쪽으로 이동)
  function initAjaxForms() {
    document.querySelectorAll('form[data-ajax="true"]').forEach(form => {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = form.querySelector('button[type="submit"]');
        if (btn) btn.disabled = true;

        const fm = new FormData(form);
        let body, headers;
        if ([...fm.keys()].some(k => k.endsWith('[]'))) {
          // keep FormData
          body = fm;
        } else {
          // try JSON when no files (convert)
          const obj = {};
          for (const [k, v] of fm.entries()) obj[k] = v;
          body = JSON.stringify(obj);
          headers = { 'Content-Type': 'application/json' };
        }

        try {
          const res = await fetchWithCSRF(form.action || window.location.href, {
            method: form.method || 'POST',
            body,
            headers
          });

          // 응답이 { redirect: '/home' } 또는 { success: true }
          if (res && res.redirect) {
            window.location.href = res.redirect;
            return;
          }
          if (res && res.success) {
            showToast('작업이 성공했습니다.', { type: 'success' });
            // optional: reload or callback
          } else {
            // 서버가 메시지 줬으면 표시
            if (res && res.error) showToast(res.error, { type: 'error' });
          }
        } catch (err) {
          console.error('AJAX form error', err);
          const msg = err && err.body && err.body.error ? err.body.error : '요청 처리 중 오류가 발생했습니다.';
          showToast(msg, { type: 'error' });
        } finally {
          if (btn) btn.disabled = false;
        }
      });
    });
  }

  // 초기화
  document.addEventListener('DOMContentLoaded', () => {
    initAjaxForms();
  });

  // 공개 API
  window.appFetch = fetchWithCSRF;
  window.appShowToast = showToast;
  window.appGetCsrf = getCsrfToken;
})();