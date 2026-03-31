// Shared UI interactions used across multiple pages.
document.addEventListener('DOMContentLoaded', function() {
  // Seat selection interactions
  const seats = document.querySelectorAll('.seat.available');
  seats.forEach(function(seat) {
    seat.addEventListener('click', function(event) {
      const selectedSeat = event.target;
      if (selectedSeat.classList.contains('available')) {
        selectedSeat.classList.toggle('selected');
      }
    });
  });

  // Ticket count selection
  const ticketButtons = document.querySelectorAll('.ticket-count');
  ticketButtons.forEach(function(button) {
    button.addEventListener('click', function() {
      ticketButtons.forEach(function(btn) {
        btn.classList.remove('selected');
      });
      this.classList.add('selected');
    });
  });

  // Date selection
  const dateButtons = document.querySelectorAll('.date-button');
  dateButtons.forEach(function(button) {
    button.addEventListener('click', function() {
      dateButtons.forEach(function(btn) {
        btn.classList.remove('selected');
      });
      this.classList.add('selected');
    });
  });

  // Showtime selection
  const showtimeButtons = document.querySelectorAll('.showtime-button.available');
  showtimeButtons.forEach(function(button) {
    button.addEventListener('click', function() {
      showtimeButtons.forEach(function(btn) {
        btn.classList.remove('selected');
      });
      this.classList.add('selected');
    });
  });

  // Authentication tab switching
  const authTabs = document.querySelectorAll('.auth-tab');
  const authForms = document.querySelectorAll('.auth-form');
  authTabs.forEach(function(tab) {
    tab.addEventListener('click', function() {
      const tabName = this.getAttribute('data-tab');
      authTabs.forEach(function(t) {
        t.classList.remove('active');
      });
      authForms.forEach(function(form) {
        form.classList.remove('active');
      });

      this.classList.add('active');
      const target = document.getElementById(tabName + '-form');
      if (target) {
        target.classList.add('active');
      }
    });
  });

  // Auth form switch links
  const authSwitchLinks = document.querySelectorAll('.auth-switch-link');
  authSwitchLinks.forEach(function(link) {
    link.addEventListener('click', function(e) {
      e.preventDefault();
      const targetTab = this.getAttribute('data-switch');
      const tabButton = document.querySelector('[data-tab="' + targetTab + '"]');
      if (tabButton) {
        tabButton.click();
      }
    });
  });

});
