// Authentication modal interactions shared across pages.
document.addEventListener('DOMContentLoaded', function() {
  const authModal = document.getElementById('auth-modal');

  if (!authModal) {
    return;
  }

  function setAuthTab(tabName) {
    const authTabs = document.querySelectorAll('.auth-tab');
    const authForms = document.querySelectorAll('.auth-form');

    authTabs.forEach(function(tab) {
      tab.classList.toggle('active', tab.getAttribute('data-tab') === tabName);
    });

    authForms.forEach(function(form) {
      form.classList.toggle('active', form.id === tabName + '-form');
    });
  }

  function getCurrentReturnTo() {
    const params = new URLSearchParams(window.location.search);
    params.delete('auth');
    const query = params.toString();
    return window.location.pathname + (query ? ('?' + query) : '');
  }

  function populateReturnToFields() {
    const returnToInputs = authModal.querySelectorAll('input.auth-return-to');
    const returnTo = getCurrentReturnTo();
    returnToInputs.forEach(function(input) {
      input.value = returnTo;
    });
  }

  function openAuthModal(tabName) {
    populateReturnToFields();
    setAuthTab(tabName || 'login');
    authModal.classList.add('active');
    document.body.classList.add('auth-modal-open');
  }

  function closeAuthModal() {
    authModal.classList.remove('active');
    document.body.classList.remove('auth-modal-open');
  }

  const authTabs = document.querySelectorAll('.auth-tab');
  authTabs.forEach(function(tab) {
    tab.addEventListener('click', function() {
      const tabName = this.getAttribute('data-tab');
      setAuthTab(tabName);
    });
  });

  const authSwitchLinks = document.querySelectorAll('.auth-switch-link');
  authSwitchLinks.forEach(function(link) {
    link.addEventListener('click', function(e) {
      e.preventDefault();
      const targetTab = this.getAttribute('data-switch');
      setAuthTab(targetTab || 'login');
    });
  });

  const signInLinks = document.querySelectorAll('a.signin-btn[data-auth-open]');
  signInLinks.forEach(function(link) {
    link.addEventListener('click', function(event) {
      event.preventDefault();
      const tabName = link.getAttribute('data-auth-open') || 'login';
      openAuthModal(tabName);
    });
  });

  const closeAuthButton = document.getElementById('auth-modal-close');
  if (closeAuthButton) {
    closeAuthButton.addEventListener('click', closeAuthModal);
  }

  authModal.addEventListener('click', function(event) {
    if (event.target === authModal) {
      closeAuthModal();
    }
  });

  const hasAuthError = authModal.getAttribute('data-has-error') === 'true';
  const startTab = authModal.getAttribute('data-start-tab') || 'login';
  const shouldForceOpen = document.body.getAttribute('data-auth-force-open') === 'true';
  const authParam = new URLSearchParams(window.location.search).get('auth');
  const shouldOpenFromQuery = authParam === 'login' || authParam === 'signup';
  const queryTab = authParam === 'signup' ? 'signup' : 'login';

  if (hasAuthError || shouldForceOpen || shouldOpenFromQuery) {
    openAuthModal(hasAuthError ? startTab : queryTab);
  }

  window.CineBookAuthModal = {
    open: openAuthModal,
    close: closeAuthModal
  };

  document.addEventListener('cinebook:open-auth-modal', function(event) {
    const tabName = event && event.detail && event.detail.tab ? event.detail.tab : 'login';
    openAuthModal(tabName);
  });

  document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape' && authModal.classList.contains('active')) {
      closeAuthModal();
    }
  });
});
