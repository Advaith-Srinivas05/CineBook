// Shared UI interactions used across multiple pages.
document.addEventListener('DOMContentLoaded', function() {
  // User profile dropdown interactions in navbar.
  const userMenuContainers = document.querySelectorAll('.user-menu-container');
  userMenuContainers.forEach(function(container) {
    const trigger = container.querySelector('.avatar-trigger');
    if (!trigger) {
      return;
    }

    trigger.addEventListener('click', function(event) {
      event.preventDefault();
      const isOpen = container.classList.contains('open');
      userMenuContainers.forEach(function(otherContainer) {
        otherContainer.classList.remove('open');
        const otherTrigger = otherContainer.querySelector('.avatar-trigger');
        if (otherTrigger) {
          otherTrigger.setAttribute('aria-expanded', 'false');
        }
      });
      if (!isOpen) {
        container.classList.add('open');
        trigger.setAttribute('aria-expanded', 'true');
      }
    });
  });

  document.addEventListener('click', function(event) {
    userMenuContainers.forEach(function(container) {
      if (!container.contains(event.target)) {
        container.classList.remove('open');
        const trigger = container.querySelector('.avatar-trigger');
        if (trigger) {
          trigger.setAttribute('aria-expanded', 'false');
        }
      }
    });
  });

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
});
