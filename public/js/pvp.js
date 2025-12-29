(function () {
  'use strict';

  /* =========================
     DOM ELEMENTS
  ========================== */
  const startBtn = document.getElementById('pvpStartBtn');
  const matchForm = document.getElementById('pvpForm');
  const timerEl = document.getElementById('timer');
  const problemEl = document.getElementById('pvp-problem');
  const opponentEl = document.getElementById('pvp-opponent');

  /* =========================
     STATE
  ========================== */
  const TIME_LIMIT = 300;
  let currentMatchId = null;
  let remaining = TIME_LIMIT;
  let timerInterval = null;
  let statusInterval = null;

  /* =========================
     API HELPERS
  ========================== */
  async function apiPost(url, body = {}) {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    if (!res.ok) throw new Error('API POST failed');
    return res.json();
  }

  async function apiGet(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error('API GET failed');
    return res.json();
  }

  /* =========================
     MATCH START
  ========================== */
  async function startRandomMatch() {
    try {
      const data = await apiPost('/api/pvp/match');
      if (data.matchId) {
        window.location.href = `/pvp?matchId=${data.matchId}`;
      } else {
        alert('매칭 대기중입니다...');
      }
    } catch (e) {
      console.error(e);
      alert('매칭 실패');
    }
  }

  async function startMatchWithUser(targetUserId) {
    try {
      const data = await apiPost('/api/pvp/match', { targetUserId });
      if (data.matchId) {
        window.location.href = `/pvp?matchId=${data.matchId}`;
      }
    } catch (e) {
      console.error(e);
      alert('지정 매칭 실패');
    }
  }

  /* =========================
     LOAD & RENDER MATCH
  ========================== */
  async function loadMatch(matchId) {
    currentMatchId = matchId;
    const match = await apiGet(`/api/pvp/match/${matchId}`);
    renderMatch(match);
    startStatusPolling();
  }

  function renderMatch(match) {
    if (problemEl && match.problem) {
      problemEl.innerHTML = `
        <h3>${escapeHtml(match.problem.title || '')}</h3>
        <p>${escapeHtml(match.problem.content || '')}</p>
      `;
    }

    if (opponentEl) {
      opponentEl.textContent = `대전 상대: ${match.opponentName || '상대'}`;
    }

    const startedAt = match.startedAt
      ? new Date(match.startedAt).getTime()
      : Date.now();

    const elapsed = Math.floor((Date.now() - startedAt) / 1000);
    remaining = Math.max(0, TIME_LIMIT - elapsed);

    startTimer();
  }

  /* =========================
     TIMER
  ========================== */
  function startTimer() {
    clearInterval(timerInterval);

    updateTimer();
    timerInterval = setInterval(() => {
      remaining--;
      updateTimer();

      if (remaining <= 0) {
        clearInterval(timerInterval);
        autoSubmit();
      }
    }, 1000);
  }

  function updateTimer() {
    if (!timerEl) return;
    const m = Math.floor(remaining / 60);
    const s = remaining % 60;
    timerEl.textContent = `${m}:${String(s).padStart(2, '0')}`;
  }

  /* =========================
     MATCH STATUS POLLING
  ========================== */
  function startStatusPolling() {
    clearInterval(statusInterval);

    statusInterval = setInterval(async () => {
      try {
        const r = await apiGet(`/api/pvp/match/${currentMatchId}`);
        if (r.status === 'finished') {
          clearInterval(statusInterval);
          clearInterval(timerInterval);
          alert('매치 종료');
          location.reload();
        }
      } catch (e) {
        console.error('상태 확인 실패', e);
      }
    }, 3000);
  }

  /* =========================
     SUBMIT
  ========================== */
  if (matchForm) {
    matchForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const answerEl = matchForm.querySelector('input[name="answer"]:checked');
      if (!answerEl) {
        alert('정답을 선택하세요');
        return;
      }

      try {
        await apiPost(`/api/pvp/match/${currentMatchId}/submit`, {
          answer: answerEl.value
        });
        alert('제출 완료. 상대를 기다립니다.');
      } catch (e) {
        console.error(e);
        alert('제출 실패');
      }
    });
  }

  function autoSubmit() {
    if (!matchForm) return;
    const answerEl = matchForm.querySelector('input[name="answer"]:checked');
    if (answerEl) matchForm.requestSubmit();
  }

  /* =========================
     INIT
  ========================== */
  document.addEventListener('DOMContentLoaded', () => {
    if (startBtn) startBtn.addEventListener('click', startRandomMatch);

    const matchId =
      new URLSearchParams(location.search).get('matchId');

    if (matchId) loadMatch(matchId);
  });

  /* =========================
     UTILS
  ========================== */
  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  /* =========================
     PUBLIC API (OPTIONAL)
  ========================== */
  window.pvpStartRandom = startRandomMatch;
  window.pvpStartWithUser = startMatchWithUser;

})();
