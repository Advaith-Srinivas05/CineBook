// Dark Mode Toggle
document.addEventListener('DOMContentLoaded', function() {
  // Create and insert dark mode toggle button
  const header = document.querySelector('.header-right');
  if (header) {
    const themeToggle = document.createElement('button');
    themeToggle.id = 'theme-toggle';
    themeToggle.className = 'theme-toggle';
    themeToggle.setAttribute('aria-label', 'Toggle dark mode');
    themeToggle.innerHTML = '🌙';
    
    // Insert before the menu icon
    const menuIcon = header.querySelector('.menu-icon');
    if (menuIcon) {
      header.insertBefore(themeToggle, menuIcon);
    } else {
      header.appendChild(themeToggle);
    }
  }

  // Check for saved theme preference or default to light
  const currentTheme = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateToggleButton(currentTheme);

  // Listen for toggle button clicks
  const toggleBtn = document.getElementById('theme-toggle');
  if (toggleBtn) {
    toggleBtn.addEventListener('click', toggleTheme);
  }

  // Seat selection interactions
  const seats = document.querySelectorAll('.seat.available');
  seats.forEach(seat => {
    seat.addEventListener('click', toggleSeatSelection);
  });

  // Ticket count selection
  const ticketButtons = document.querySelectorAll('.ticket-count');
  ticketButtons.forEach(button => {
    button.addEventListener('click', function() {
      ticketButtons.forEach(btn => btn.classList.remove('selected'));
      this.classList.add('selected');
    });
  });

  // Date selection
  const dateButtons = document.querySelectorAll('.date-button');
  dateButtons.forEach(button => {
    button.addEventListener('click', function() {
      dateButtons.forEach(btn => btn.classList.remove('selected'));
      this.classList.add('selected');
    });
  });

  // Showtime selection
  const showtimeButtons = document.querySelectorAll('.showtime-button.available');
  showtimeButtons.forEach(button => {
    button.addEventListener('click', function() {
      showtimeButtons.forEach(btn => btn.classList.remove('selected'));
      this.classList.add('selected');
    });
  });
});

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
    toggleBtn.innerHTML = theme === 'dark' ? '☀️' : '🌙';
  }
}

function toggleSeatSelection(event) {
  const seat = event.target;
  if (seat.classList.contains('available')) {
    seat.classList.toggle('selected');
  }
}

// Attach DOM-dependent listeners after DOMContentLoaded
document.addEventListener('DOMContentLoaded', function() {
  // Authentication Tab Switching
  const authTabs = document.querySelectorAll('.auth-tab');
  const authForms = document.querySelectorAll('.auth-form');

  authTabs.forEach(tab => {
    tab.addEventListener('click', function() {
      const tabName = this.getAttribute('data-tab');
      
      authTabs.forEach(t => t.classList.remove('active'));
      authForms.forEach(form => form.classList.remove('active'));
      
      this.classList.add('active');
      const target = document.getElementById(tabName + '-form');
      if (target) target.classList.add('active');
    });
  });

  // Auth Form Switch Links
  const authSwitchLinks = document.querySelectorAll('.auth-switch-link');
  authSwitchLinks.forEach(link => {
    link.addEventListener('click', function(e) {
      e.preventDefault();
      const targetTab = this.getAttribute('data-switch');
      const tabButton = document.querySelector(`[data-tab="${targetTab}"]`);
      if (tabButton) {
        tabButton.click();
      }
    });
  });

  // Admin Form Submissions
  const adminForms = document.querySelectorAll('.admin-card form');
  adminForms.forEach(form => {
    form.addEventListener('submit', function(e) {
      e.preventDefault();
      const submitBtn = this.querySelector('button[type="submit"]');
      if (submitBtn) {
        const originalText = submitBtn.textContent;
        submitBtn.textContent = 'Added Successfully!';
        submitBtn.style.opacity = '0.7';
        setTimeout(() => {
          submitBtn.textContent = originalText;
          submitBtn.style.opacity = '1';
          this.reset();
        }, 1500);
      }
    });
  });

  // Admin Remove Movie Buttons
  const removeMovieBtns = document.querySelectorAll('.admin-remove-btn');
  removeMovieBtns.forEach(btn => {
    btn.addEventListener('click', function(e) {
      e.preventDefault();
      const movieItem = this.parentElement;
      movieItem.style.opacity = '0.5';
      movieItem.style.textDecoration = 'line-through';
      const originalText = this.textContent;
      this.textContent = 'Removed';
      this.disabled = true;
      setTimeout(() => {
        movieItem.remove();
      }, 1000);
    });
  });

  // Admin Action Buttons (Edit/Delete)
  const adminActionBtns = document.querySelectorAll('.admin-action-btn');
  adminActionBtns.forEach(btn => {
    btn.addEventListener('click', function(e) {
      e.preventDefault();
      if (this.classList.contains('delete')) {
        const row = this.closest('tr');
        row.style.opacity = '0.5';
        const originalText = this.textContent;
        this.textContent = 'Deleted';
        this.disabled = true;
        setTimeout(() => {
          row.remove();
        }, 1000);
      } else if (this.classList.contains('edit')) {
        const originalText = this.textContent;
        this.textContent = 'Editing...';
        setTimeout(() => {
          this.textContent = originalText;
        }, 1500);
      }
    });
  });

  // Carousel Auto Scroll
  const carousel = document.querySelector('.carousel');
  if (carousel) {
    // Use track translation in pixels so adjacent previews are consistent
    let slides = carousel.querySelectorAll('.slider');
    if (slides.length > 0) {
      const firstClone = slides[0].cloneNode(true);
      const lastClone = slides[slides.length - 1].cloneNode(true);

      carousel.appendChild(firstClone);
      carousel.insertBefore(lastClone, slides[0]);

      // re-query slides after adding clones
      slides = carousel.querySelectorAll('.slider');

      let index = 1; // start at first real slide (after prepended clone)
      let slideWidth = slides[0].getBoundingClientRect().width + parseFloat(getComputedStyle(slides[0]).marginRight || 0);

      // position the track to show the first real slide
      carousel.style.transform = `translateX(-${index * slideWidth}px)`;
      carousel.style.transition = 'transform 0.5s ease';

      function updateDimensions() {
        slideWidth = slides[0].getBoundingClientRect().width + parseFloat(getComputedStyle(slides[0]).marginRight || 0);
        // reposition without animation to keep visuals correct on resize
        carousel.style.transition = 'none';
        carousel.style.transform = `translateX(-${index * slideWidth}px)`;
        // force reflow then restore transition
        // eslint-disable-next-line no-unused-expressions
        carousel.offsetHeight;
        carousel.style.transition = 'transform 0.5s ease';
      }

      function moveCarousel() {
        index++;
        carousel.style.transition = 'transform 0.5s ease';
        carousel.style.transform = `translateX(-${index * slideWidth}px)`;
      }

      // when we reach clones, snap back to the corresponding real slide without animation
      carousel.addEventListener('transitionend', function() {
        if (index >= slides.length - 1) {
          // jumped past last real slide -> snap to first real
          carousel.style.transition = 'none';
          index = 1;
          carousel.style.transform = `translateX(-${index * slideWidth}px)`;
          // force reflow then restore transition
          // eslint-disable-next-line no-unused-expressions
          carousel.offsetHeight;
          carousel.style.transition = 'transform 0.5s ease';
        }
      });

      // autoplay
      let carouselInterval = setInterval(moveCarousel, 4000);

      // recalc on resize
      window.addEventListener('resize', function() {
        updateDimensions();
      });
    }
  }
});


