document.addEventListener("DOMContentLoaded", function() {
  const seatMap = document.getElementById("seat-map");
  if (!seatMap) {
    return;
  }

  const ticketCount = Number(seatMap.dataset.ticketCount || "1");
  const maxSelectableSeats = Number.isFinite(ticketCount) && ticketCount > 0 ? ticketCount : 1;
  const isLoggedIn = seatMap.dataset.isLoggedIn === "true";

  const bookedSeatsRaw = seatMap.dataset.bookedSeats || "";
  const bookedSeats = new Set(
    bookedSeatsRaw
      .split(",")
      .map(function(seat) { return seat.trim().toUpperCase(); })
      .filter(function(seat) { return seat.length > 0; })
  );

  const selectedSeats = new Set();
  const seatButtons = Array.from(document.querySelectorAll(".seat"));
  const proceedForm = document.getElementById("proceed-payment-form");
  const selectedSeatsInput = document.getElementById("selected-seats-input");

  const updateSeatInteractivity = function() {
    const hasReachedLimit = selectedSeats.size >= maxSelectableSeats;

    seatButtons.forEach(function(button) {
      const seatNumber = (button.dataset.seatNumber || "").trim().toUpperCase();
      if (!seatNumber || bookedSeats.has(seatNumber)) {
        return;
      }

      const isSelected = selectedSeats.has(seatNumber);
      const shouldDisable = hasReachedLimit && !isSelected;
      button.disabled = shouldDisable;
      button.setAttribute("aria-disabled", shouldDisable ? "true" : "false");
    });
  };

  const openLoginModal = function() {
    if (window.CineBookAuthModal && typeof window.CineBookAuthModal.open === "function") {
      window.CineBookAuthModal.open("login");
      return;
    }
    document.dispatchEvent(new CustomEvent("cinebook:open-auth-modal", { detail: { tab: "login" } }));
  };

  seatButtons.forEach(function(button) {
    const seatNumber = (button.dataset.seatNumber || "").trim().toUpperCase();
    if (!seatNumber) {
      return;
    }

    button.classList.remove("booked", "selected", "available");
    if (bookedSeats.has(seatNumber)) {
      button.classList.add("booked");
      button.disabled = true;
      button.setAttribute("aria-disabled", "true");
      return;
    }

    button.classList.add("available");
    button.disabled = false;
    button.setAttribute("aria-disabled", "false");

    button.addEventListener("click", function(event) {
      event.preventDefault();

      if (selectedSeats.has(seatNumber)) {
        selectedSeats.delete(seatNumber);
        button.classList.remove("selected");
        button.classList.add("available");
        updateSeatInteractivity();
        return;
      }

      if (selectedSeats.size >= maxSelectableSeats) {
        window.alert("You can only select " + maxSelectableSeats + " seat(s).");
        return;
      }

      selectedSeats.add(seatNumber);
      button.classList.remove("available");
      button.classList.add("selected");
      updateSeatInteractivity();
    });
  });

  updateSeatInteractivity();

  if (proceedForm && selectedSeatsInput) {
    proceedForm.addEventListener("submit", function(event) {
      if (!isLoggedIn) {
        event.preventDefault();
        openLoginModal();
        return;
      }

      if (selectedSeats.size !== maxSelectableSeats) {
        event.preventDefault();
        window.alert("Please select exactly " + maxSelectableSeats + " seat(s) to continue.");
        return;
      }

      selectedSeatsInput.value = Array.from(selectedSeats).join(",");
    });
  }
});
