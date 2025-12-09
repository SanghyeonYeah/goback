document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const link = btn.getAttribute('data-link');
      if (link) {
        window.location.href = link;
      }
    });
  });
});
