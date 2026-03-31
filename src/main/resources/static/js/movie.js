document.addEventListener("DOMContentLoaded", function() {
  (function initSevenDayDateSelector() {
    const dateSelector = document.getElementById("date-selector");
    if (!dateSelector) {
      return;
    }

    const movieId = dateSelector.dataset.movieId;
    const selectedCity = dateSelector.dataset.selectedCity || "";
    const selectedDate = dateSelector.dataset.selectedDate || "";

    if (!movieId) {
      return;
    }

    const formatIsoDate = (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    };

    const today = new Date();
    dateSelector.innerHTML = "";

    for (let i = 0; i < 7; i += 1) {
      const currentDate = new Date(today);
      currentDate.setDate(today.getDate() + i);

      const isoDate = formatIsoDate(currentDate);
      const dayText = currentDate
        .toLocaleDateString("en-US", { weekday: "short" })
        .toUpperCase();
      const dateText = String(currentDate.getDate()).padStart(2, "0");
      const monthText = currentDate
        .toLocaleDateString("en-US", { month: "short" })
        .toUpperCase();

      const queryParams = new URLSearchParams({
        movieId: String(movieId),
        date: isoDate
      });

      if (selectedCity) {
        queryParams.set("city", selectedCity);
      }

      const link = document.createElement("a");
      link.className = "date-button";
      link.href = `/movie?${queryParams.toString()}`;
      link.setAttribute("aria-label", `${dayText} ${dateText} ${monthText}`);

      const isSelected = selectedDate ? selectedDate === isoDate : i === 0;
      if (isSelected) {
        link.classList.add("selected");
      }

      const daySpan = document.createElement("span");
      daySpan.className = "day";
      daySpan.textContent = dayText;

      const dateSpan = document.createElement("span");
      dateSpan.className = "date";
      dateSpan.textContent = dateText;

      const monthSpan = document.createElement("span");
      monthSpan.className = "month";
      monthSpan.textContent = monthText;

      link.appendChild(daySpan);
      link.appendChild(dateSpan);
      link.appendChild(monthSpan);
      dateSelector.appendChild(link);
    }
  }());

  const dateButtons = document.querySelectorAll(".date-button");
  dateButtons.forEach(function(button) {
    button.addEventListener("click", function() {
      dateButtons.forEach(function(btn) {
        btn.classList.remove("selected");
      });
      this.classList.add("selected");
    });
  });

  const showtimeButtons = document.querySelectorAll(".showtime-button.available");
  showtimeButtons.forEach(function(button) {
    button.addEventListener("click", function() {
      showtimeButtons.forEach(function(btn) {
        btn.classList.remove("selected");
      });
      this.classList.add("selected");
    });
  });
});