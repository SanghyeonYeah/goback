(function () {
  'use strict';

  const container = document.getElementById('calendar');
  if (!container) return;

  const monthNames = ['01','02','03','04','05','06','07','08','09','10','11','12'];

  function getTodayParts() {
    const d = new Date();
    return { year: d.getFullYear(), month: d.getMonth() + 1, date: d.getDate() };
  }

  function buildControls(currentYear, currentMonth) {
    const ctrl = document.createElement('div');
    ctrl.className = 'calendar-controls';
    ctrl.style.display = 'flex';
    ctrl.style.justifyContent = 'space-between';
    ctrl.style.alignItems = 'center';
    ctrl.style.marginBottom = '12px';

    const left = document.createElement('div');
    const prev = document.createElement('button');
    prev.textContent = '◀';
    prev.addEventListener('click', () => {
      const m = new Date(currentYear, currentMonth - 2, 1);
      renderCalendar(m.getFullYear(), m.getMonth() + 1);
    });
    const next = document.createElement('button');
    next.textContent = '▶';
    next.addEventListener('click', () => {
      const m = new Date(currentYear, currentMonth, 1);
      renderCalendar(m.getFullYear(), m.getMonth() + 1);
    });

    left.appendChild(prev);
    left.appendChild(next);

    const title = document.createElement('div');
    title.textContent = `${currentYear} - ${String(currentMonth).padStart(2, '0')}`;
    title.style.fontWeight = '700';

    const todayBtn = document.createElement('button');
    todayBtn.textContent = '오늘';
    todayBtn.addEventListener('click', () => {
      const t = getTodayParts();
      renderCalendar(t.year, t.month);
    });

    ctrl.appendChild(left);
    ctrl.appendChild(title);
    ctrl.appendChild(todayBtn);
    return ctrl;
  }

  function makeDayElement(dayNum, dateStr, status, isToday) {
    const day = document.createElement('div');
    day.className = 'calendar-day';
    if (isToday) day.classList.add('today');
    if (status === '완') day.classList.add('success');
    if (status === '실') day.classList.add('fail');

    const num = document.createElement('div');
    num.className = 'day-number';
    num.textContent = dayNum;

    const stat = document.createElement('div');
    stat.className = 'day-status';
    stat.textContent = status || '';

    day.appendChild(num);
    day.appendChild(stat);

    day.addEventListener('click', () => onDayClick(dateStr));
    return day;
  }

  async function onDayClick(dateStr) {
    try {
      const res = await window.appFetch ? window.appFetch(`/api/todo?date=${dateStr}`) : fetch(`/api/todo?date=${dateStr}`).then(r => r.json());
      const todos = res.todos || [];
      showTodosModal(dateStr, todos);
    } catch (err) {
      console.error('달력 날짜 클릭 오류', err);
      window.appShowToast ? window.appShowToast('할 일 불러오기 실패', { type: 'error' }) : alert('할 일 불러오기 실패');
    }
  }

  function showTodosModal(dateStr, todos) {
    // 단순 모달
    let modal = document.getElementById('calendar-modal');
    if (modal) modal.remove();

    modal = document.createElement('div');
    modal.id = 'calendar-modal';
    modal.style.position = 'fixed';
    modal.style.left = 0;
    modal.style.top = 0;
    modal.style.right = 0;
    modal.style.bottom = 0;
    modal.style.background = 'rgba(0,0,0,0.4)';
    modal.style.display = 'flex';
    modal.style.alignItems = 'center';
    modal.style.justifyContent = 'center';
    modal.style.zIndex = 9999;

    const box = document.createElement('div');
    box.style.width = 'min(720px, 95%)';
    box.style.maxHeight = '80vh';
    box.style.overflow = 'auto';
    box.style.background = '#fff';
    box.style.padding = '18px';
    box.style.borderRadius = '8px';

    const h = document.createElement('h3');
    h.textContent = `${dateStr} 할 일`;
    box.appendChild(h);

    if (!todos.length) {
      const p = document.createElement('p');
      p.textContent = '등록된 할 일이 없습니다.';
      box.appendChild(p);
    } else {
      const ul = document.createElement('ul');
      todos.forEach(t => {
        const li = document.createElement('li');
        li.textContent = `${t.subject || ''} — ${t.task || ''} ${t.completed ? '(완료)' : ''}`;
        ul.appendChild(li);
      });
      box.appendChild(ul);
    }

    const close = document.createElement('button');
    close.textContent = '닫기';
    close.style.marginTop = '12px';
    close.addEventListener('click', () => modal.remove());
    box.appendChild(close);

    modal.appendChild(box);
    document.body.appendChild(modal);
  }

  // main 렌더 함수
  async function renderCalendar(year, month) {
    container.innerHTML = ''; // 초기화

    const controls = buildControls(year, month);
    container.appendChild(controls);

    const grid = document.createElement('div');
    grid.className = 'calendar-grid';
    grid.style.display = 'grid';
    grid.style.gridTemplateColumns = 'repeat(7, 1fr)';
    grid.style.gap = '8px';

    // 요일 헤더
    ['일','월','화','수','목','금','토'].forEach(d => {
      const h = document.createElement('div');
      h.className = 'calendar-day-header';
      h.textContent = d;
      grid.appendChild(h);
    });

    // API 호출: 달력 상태 데이터
    let records = [];
    try {
      const res = await (window.appFetch ? window.appFetch(`/api/todo/calendar?year=${year}&month=${String(month).padStart(2,'0')}`) : fetch(`/api/todo/calendar?year=${year}&month=${String(month).padStart(2,'0')}`).then(r => r.json()));
      records = res.records || [];
    } catch (err) {
      console.error('달력 데이터 로드 실패', err);
      window.appShowToast ? window.appShowToast('달력 불러오기 실패', { type: 'error' }) : null;
    }

    // records -> map
    const recMap = {};
    records.forEach(r => recMap[r.date] = r.status);

    // 달력 계산
    const first = new Date(year, month - 1, 1);
    const last = new Date(year, month, 0);
    const startDayIndex = first.getDay();
    const daysInMonth = last.getDate();

    // 빈칸
    for (let i = 0; i < startDayIndex; i++) {
      const blank = document.createElement('div');
      blank.className = 'calendar-day empty';
      blank.style.visibility = 'hidden';
      grid.appendChild(blank);
    }

    const todayParts = getTodayParts();
    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
      const status = recMap[dateStr] || '';
      const isToday = (year === todayParts.year && month === todayParts.month && day === todayParts.date);
      const dayEl = makeDayElement(day, dateStr, status, isToday);
      grid.appendChild(dayEl);
    }

    container.appendChild(grid);
  }

  // 초기 렌더 (오늘 기준)
  const t = getTodayParts();
  renderCalendar(t.year, t.month);
})();