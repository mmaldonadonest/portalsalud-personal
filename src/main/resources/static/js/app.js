(function () {
  const btn = document.getElementById('btnPing');
  const output = document.getElementById('pingResult');

  if (!btn || !output) return;

  btn.addEventListener('click', function () {
    const now = new Date();
    output.textContent = 'JS activo. Timestamp: ' + now.toISOString();
  });
})();
