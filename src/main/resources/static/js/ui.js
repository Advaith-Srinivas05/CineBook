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
});
