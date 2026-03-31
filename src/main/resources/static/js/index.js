// Index page specific behavior: movie search redirect and carousel animation.
document.addEventListener('DOMContentLoaded', function() {
  const homeSearchInput = document.getElementById('home-movie-search');
  const homeSuggestions = document.getElementById('home-movie-suggestions');

  if (homeSearchInput && homeSuggestions) {
    let activeFetch = 0;

    const escapeHtml = function(value) {
      if (!value && value !== 0) return '';
      return String(value).replace(/[&<>\"']/g, function(c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '\"': '&quot;', "'": '&#39;' }[c];
      });
    };

    const renderSuggestions = function(items) {
      if (!items || items.length === 0) {
        homeSuggestions.style.display = 'none';
        homeSuggestions.innerHTML = '';
        return;
      }

      homeSuggestions.innerHTML = items.map(function(item) {
        return '<button type="button" class="home-autocomplete-item" data-id="' + escapeHtml(item.id) + '" data-title="' + escapeHtml(item.title || '') + '">' + escapeHtml(item.title || '') + '</button>';
      }).join('');
      homeSuggestions.style.display = 'block';
    };

    const redirectToMovie = function(movieId) {
      const city = (homeSearchInput.dataset.city || '').trim();
      const url = new URL('/movie', window.location.origin);
      url.searchParams.set('movieId', movieId);
      if (city) {
        url.searchParams.set('city', city);
      }
      window.location.href = url.toString();
    };

    homeSuggestions.addEventListener('click', function(event) {
      const btn = event.target.closest('.home-autocomplete-item');
      if (!btn) return;
      const movieId = btn.getAttribute('data-id');
      if (!movieId) return;
      homeSearchInput.value = btn.getAttribute('data-title') || homeSearchInput.value;
      homeSuggestions.style.display = 'none';
      redirectToMovie(movieId);
    });

    homeSearchInput.addEventListener('input', async function() {
      const query = homeSearchInput.value.trim();
      if (!query) {
        homeSuggestions.style.display = 'none';
        homeSuggestions.innerHTML = '';
        return;
      }

      const fetchId = ++activeFetch;
      try {
        const res = await fetch('/api/movies/search?q=' + encodeURIComponent(query));
        if (!res.ok || fetchId !== activeFetch) {
          return;
        }
        const list = await res.json();
        if (fetchId !== activeFetch) {
          return;
        }
        renderSuggestions(Array.isArray(list) ? list.slice(0, 10) : []);
      } catch (_err) {
        if (fetchId !== activeFetch) {
          return;
        }
        homeSuggestions.style.display = 'none';
        homeSuggestions.innerHTML = '';
      }
    });

    homeSearchInput.addEventListener('keydown', function(event) {
      if (event.key !== 'Enter') return;
      const firstItem = homeSuggestions.querySelector('.home-autocomplete-item');
      if (!firstItem) return;
      event.preventDefault();
      const movieId = firstItem.getAttribute('data-id');
      if (movieId) {
        redirectToMovie(movieId);
      }
    });

    homeSearchInput.addEventListener('blur', function() {
      setTimeout(function() {
        homeSuggestions.style.display = 'none';
      }, 150);
    });
  }

  const carousel = document.querySelector('.carousel');
  if (!carousel) {
    return;
  }

  let slides = carousel.querySelectorAll('.slider');
  if (slides.length === 0) {
    return;
  }

  const firstClone = slides[0].cloneNode(true);
  const lastClone = slides[slides.length - 1].cloneNode(true);

  carousel.appendChild(firstClone);
  carousel.insertBefore(lastClone, slides[0]);

  slides = carousel.querySelectorAll('.slider');

  let index = 1;
  let slideWidth = slides[0].getBoundingClientRect().width + parseFloat(getComputedStyle(slides[0]).marginRight || 0);

  carousel.style.transform = 'translateX(-' + (index * slideWidth) + 'px)';
  carousel.style.transition = 'transform 0.5s ease';

  function updateDimensions() {
    slideWidth = slides[0].getBoundingClientRect().width + parseFloat(getComputedStyle(slides[0]).marginRight || 0);
    carousel.style.transition = 'none';
    carousel.style.transform = 'translateX(-' + (index * slideWidth) + 'px)';
    carousel.offsetHeight;
    carousel.style.transition = 'transform 0.5s ease';
  }

  function moveCarousel() {
    index += 1;
    carousel.style.transition = 'transform 0.5s ease';
    carousel.style.transform = 'translateX(-' + (index * slideWidth) + 'px)';
  }

  carousel.addEventListener('transitionend', function() {
    if (index >= slides.length - 1) {
      carousel.style.transition = 'none';
      index = 1;
      carousel.style.transform = 'translateX(-' + (index * slideWidth) + 'px)';
      carousel.offsetHeight;
      carousel.style.transition = 'transform 0.5s ease';
    }
  });

  setInterval(moveCarousel, 4000);
  window.addEventListener('resize', updateDimensions);
});
