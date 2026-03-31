document.addEventListener("DOMContentLoaded", function() {
  (function initMovieDetailsToggle() {
    const details = document.querySelector(".movie-details");
    const toggleBtn = document.getElementById("movie-details-toggle");
    const toggleText = toggleBtn ? toggleBtn.querySelector(".toggle-text") : null;

    if (!details || !toggleBtn) {
      return;
    }

    const syncToggleState = function(isCollapsed) {
      details.classList.toggle("collapsed", isCollapsed);
      toggleBtn.setAttribute("aria-expanded", String(!isCollapsed));
      toggleBtn.setAttribute("aria-label", isCollapsed ? "Expand details" : "Collapse details");
      if (toggleText) {
        toggleText.textContent = isCollapsed ? "Expand" : "Collapse";
      }
    };

    syncToggleState(false);

    toggleBtn.addEventListener("click", function() {
      const collapsed = details.classList.contains("collapsed");
      syncToggleState(!collapsed);
    });
  }());

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

  (function initMovieRating() {
    const widget = document.getElementById("movie-rating-widget");
    if (!widget) {
      return;
    }

    const movieId = Number(widget.dataset.movieId);
    if (!movieId) {
      return;
    }

    let isLoggedIn = widget.dataset.isLoggedIn === "true";
    const stars = Array.from(widget.querySelectorAll(".rating-star"));
    const userRatingEl = document.getElementById("movie-user-rating");
    const detailRatingEl = document.getElementById("movie-detail-rating");
    let selectedRating = 0;

    const renderStars = function(activeValue) {
      stars.forEach(function(star) {
        const starValue = Number(star.getAttribute("data-rating"));
        star.classList.toggle("filled", starValue <= activeValue);
      });
    };

    const updateUserRatingText = function(value, isSaving) {
      if (!userRatingEl) {
        return;
      }
      if (isSaving) {
        userRatingEl.textContent = "Your rating: Saving...";
        return;
      }
      userRatingEl.textContent = value > 0 ? "Your rating: " + value + "/5" : "Your rating: Not rated";
    };

    const setSelectedRating = function(value) {
      selectedRating = value;
      renderStars(value);
      updateUserRatingText(value, false);
    };

    const updateDetailRating = function(value, totalRatings) {
      if (!detailRatingEl) {
        return;
      }

      const avg = Number(value);
      const count = Number(totalRatings || 0);
      if (!Number.isFinite(avg) || count <= 0) {
        detailRatingEl.textContent = "N/A";
        return;
      }

      detailRatingEl.textContent = avg.toFixed(1) + " / 5";
    };

    const openLoginModal = function() {
      if (window.CineBookAuthModal && typeof window.CineBookAuthModal.open === "function") {
        window.CineBookAuthModal.open("login");
        return;
      }
      document.dispatchEvent(new CustomEvent("cinebook:open-auth-modal", { detail: { tab: "login" } }));
    };

    const fetchJson = async function(url) {
      const res = await fetch(url);
      if (!res.ok) {
        throw new Error("Request failed");
      }
      return res.json();
    };

    const loadInitialRatings = async function() {
      try {
        const movieRatings = await fetchJson("/ratings/" + movieId);
        updateDetailRating(movieRatings.averageRating, movieRatings.totalRatings);
      } catch (_err) {
        updateDetailRating(null, 0);
      }

      if (!isLoggedIn) {
        setSelectedRating(0);
        return;
      }

      try {
        const currentUser = await fetchJson("/ratings/user/" + movieId);
        const currentRating = Number(currentUser.rating || 0);
        setSelectedRating(currentRating);
      } catch (_err) {
        setSelectedRating(0);
      }
    };

    stars.forEach(function(star) {
      const starValue = Number(star.getAttribute("data-rating"));

      star.addEventListener("mouseenter", function() {
        renderStars(starValue);
      });

      star.addEventListener("mouseleave", function() {
        renderStars(selectedRating);
      });

      star.addEventListener("click", async function() {
        if (!isLoggedIn) {
          openLoginModal();
          return;
        }

        const previousRating = selectedRating;
        setSelectedRating(starValue);
        updateUserRatingText(starValue, true);

        try {
          const res = await fetch("/ratings", {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify({ movieId: movieId, rating: starValue })
          });

          if (res.status === 401) {
            isLoggedIn = false;
            setSelectedRating(previousRating);
            openLoginModal();
            return;
          }

          if (!res.ok) {
            throw new Error("Failed to save rating");
          }

          const payload = await res.json();
          setSelectedRating(Number(payload.rating || starValue));
          updateDetailRating(payload.averageRating, payload.totalRatings);
        } catch (_err) {
          setSelectedRating(previousRating);
        }
      });
    });

    loadInitialRatings();
  }());
});