// Global theme toggle support.
document.addEventListener('DOMContentLoaded', function() {
  ensureThemeToggleExists();

  const currentTheme = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateToggleButton(currentTheme);

  const toggleBtn = document.getElementById('theme-toggle');
  if (toggleBtn) {
    toggleBtn.addEventListener('click', toggleTheme);
  }

  const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');
  darkModeQuery.addEventListener('change', function(e) {
    const newTheme = e.matches ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    updateToggleButton(newTheme);
  });
});

function ensureThemeToggleExists() {
  if (document.getElementById('theme-toggle')) {
    return;
  }

  const header = document.querySelector('.header-right');
  if (!header) {
    return;
  }

  const themeToggle = document.createElement('button');
  themeToggle.id = 'theme-toggle';
  themeToggle.className = 'theme-toggle';
  themeToggle.setAttribute('aria-label', 'Toggle dark mode');
  themeToggle.innerHTML = '🌙';
  header.insertBefore(themeToggle, header.firstChild);
}

function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute('data-theme');
  const newTheme = currentTheme === 'light' ? 'dark' : 'light';

  document.documentElement.setAttribute('data-theme', newTheme);
  localStorage.setItem('theme', newTheme);
  updateToggleButton(newTheme);
}

function updateToggleButton(theme) {
  const toggleBtn = document.getElementById('theme-toggle');
  if (toggleBtn) {
    const icon = theme === 'dark' ? '☀️' : '🌙';
    const iconSpan = toggleBtn.querySelector('.menu-item-icon');
    if (iconSpan) {
      iconSpan.textContent = icon;
      return;
    }
    toggleBtn.innerHTML = icon;
  }
}
