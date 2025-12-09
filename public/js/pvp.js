(function () {
  'use strict';

  // 요소 탐색
  const startBtn = document.getElementById('pvpStartBtn'); // optional button to start random match
  const matchForm = document.getElementById('pvpForm'); // in EJS, form for submission
  const timerEl = document.getElementById('timer');
  const problemEl = document.getElementById('pvp-problem');

  // 내부 상태
  let currentMatchId = null;
  let pollInterval = null;
  let remaining = 0;
  const TIME_LIMIT = 300; // 서버와 일치

  // helper
  async function apiPost(url, body = {}) {
    if (window.appFetch) return window.appFetch(url, { method: 'POST', body: JSON.stringify(body) });
    const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    if (!res.ok) throw new Error('API error');
    return res.json();
  }

  async function startRandomMatch() {
    try {
      const data = await apiPost('/api/pvp/match', {}); // no targetUserId => random queue/match
      if (data.waiting) {
        window.appShowToast ? window.appShowToast('매칭 대기중... 잠시만 기다려주세요') : null;
      }
      if (data.matchId) {
        currentMatchId = data.matchId;
        window.location.href = `/pvp?matchId=${currentMatchId}`; // 또는 서버에서 render되는 페이지로 이동
      }
    } catch (err) {
      console.error('매칭 시작 실패', err);
      window.appShowToast ? window.appShowToast('매칭 시작 실패', { type: 'error' }) : alert('매칭 시작 실패');
    }
  }

  // 특정 유저와 매칭 (예: 랭킹에서 클릭)
  async function startMatchWithUser(targetUserId) {
    try {
      const data = await apiPost('/api/pvp/match', { targetUserId });
      if (data.matchId) {
        currentMatchId = data.matchId;
        window.location.href = `/pvp/${currentMatchId}`;
      }
    } catch (err) {
      console.error('지정 매칭 실패', err);
      window.appShowToast ? window.appShowToast('지정 매칭 실패', { type: 'error' }) : null;
    }
  }

  // 매치 정보 불러오기 (render problem, timer 등)
  async function loadMatch(matchId) {
    try {
      const r = await (window.appFetch ? window.appFetch(`/api/pvp/match/${matchId}`) : fetch(`/api/pvp/match/${matchId}`).then(x => x.json()));
      if (r.error) throw r;
      renderMatch(r);
      return r;
    } catch (err) {
      console.error('매치 조회 오류', err);
      window.appShowToast ? window.appShowToast('매치 정보를 불러오지 못했습니다.', { type: 'error' }) : null;
    }
  }

  function renderMatch(match) {
    // 문제 출력
    if (problemEl && match.problem) {
      problemEl.innerHTML = `
        <h3>${escapeHtml(match.problem.title || '')}</h3>
        <p>${escapeHtml(match.problem.content || '')}</p>
      `;
      // show opponent
      const opponentName = match.opponent || '상대';
      const opponentEl = document.getElementById('pvp-opponent');
      if (opponentEl) opponentEl.textContent = `대전 상대: ${opponentName}`;
    }

    // 타이머 세팅
    if (timerEl) {
      // startedAt may be null; fallback: start TIME_LIMIT
      const startedAt = match.startedAt ? new Date(match.startedAt) : new Date();
      const elapsed = Math.floor((Date.now() - startedAt.getTime()) / 1000);
      remaining = Math.max(0, TIME_LIMIT - elapsed);
      updateTimer();
      if (pollInterval) clearInterval(pollInterval);
      pollInterval = setInterval(async () => {
        remaining = Math.max(0, remaining - 1);
        updateTimer();
        // 매치 상태 새로고침 (poll every 3s)
      }, 1000);
      // poll match status every 3s to detect opponent submission/result
      setInterval(async () => {
        await checkMatchStatus(match.matchId || match.matchId);
      }, 3000);
    }

    // set hidden input pvp id if present
    const pvpInput = document.querySelector('input[name="pvp_id"]');
    if (pvpInput && match.matchId) pvpInput.value = match.matchId;
  }

  async function checkMatchStatus(matchId) {
    if (!matchId) return;
    try {
      const r = await (window.appFetch ? window.appFetch(`/api/pvp/match/${matchId}`) : fetch(`/api/pvp/match/${matchId}`).then(x => x.json()));
      if (r && r.result && r.result !== 'ongoing') {
        // 매치 종료 처리
        if (pollInterval) clearInterval(pollInterval);
        let message = '매치 종료: ';
        if (r.result === 'player1_win' || r.result === 'player2_win') {
          message += (r.winnerId ? `승자 ID: ${r.winnerId}` : '승부 결정');
        } else {
          message += '무승부';
        }
        window.appShowToast ? window.appShowToast(message, { type: 'success' }) : alert(message);
        // 이동 혹은 UI 업데이트
        setTimeout(() => window.location.reload(), 1000);
      }
    } catch (err) {
      console.error('매치 상태 확인 실패', err);
    }
  }

  function updateTimer() {
    if (!timerEl) return;
    const m = Math.floor(remaining / 60);
    const s = remaining % 60;
    timerEl.textContent = `${String(m).padStart(1,'0')}:${String(s).padStart(2,'0')}`;
    if (remaining <= 0) {
      timerEl.textContent = '시간초과';
    }
  }

  // 제출 처리 (폼이 존재하면 폼 submit 인터셉트)
  if (matchForm) {
    matchForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const form = e.target;
      const matchId = form.querySelector('input[name="pvp_id"]').value;
      const answer = (form.querySelector('input[name="answer"]') || {}).value;
      if (!matchId || !answer) {
        window.appShowToast ? window.appShowToast('정답을 입력하세요', { type: 'error' }) : alert('정답을 입력하세요');
        return;
      }
      try {
        const res = await apiPost(`/api/pvp/match/${matchId}/submit`, { answer });
        if (res.matchComplete) {
          window.appShowToast ? window.appShowToast('매치 완료', { type: 'success' }) : null;
          setTimeout(() => window.location.reload(), 800);
        } else {
          window.appShowToast ? window.appShowToast('제출되었습니다. 상대를 기다리는 중...', { type: 'info' }) : null;
        }
      } catch (err) {
        console.error('PVP 제출 오류', err);
        window.appShowToast ? window.appShowToast('제출 실패', { type: 'error' }) : alert('제출 실패');
      }
    });
  }

  // 페이지 진입 시 URL에 matchId 쿼리 혹은 서버에서 렌더된 hidden input이 있으면 로드
  document.addEventListener('DOMContentLoaded', async () => {
    // 버튼 바인딩 (있다면)
    if (startBtn) startBtn.addEventListener('click', startRandomMatch);

    // server-provided match id: hidden input or URL
    const urlParams = new URLSearchParams(window.location.search);
    let matchId = urlParams.get('matchId') || (document.querySelector('input[name="pvp_id"]') && document.querySelector('input[name="pvp_id"]').value);
    if (matchId) {
      await loadMatch(matchId);
      // start polling status
      setInterval(() => checkMatchStatus(matchId), 3000);
    }
  });

  // 아주 간단한 XSS 보호(출력용)
  function escapeHtml(s) {
    if (!s) return '';
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // 공개 인터페이스 (선택)
  window.pvpStartRandom = startRandomMatch;
  window.pvpStartWithUser = startMatchWithUser;
  window.pvpLoadMatch = loadMatch;
})();