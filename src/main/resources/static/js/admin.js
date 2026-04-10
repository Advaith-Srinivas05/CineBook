function showError(inputEl, message) {
  if (!inputEl) return;
  const id = inputEl.id ? inputEl.id + '-error' : null;
  let container = id ? document.getElementById(id) : null;
  if (!container) {
    container = document.createElement('div');
    container.className = 'field-error';
    if (inputEl.parentNode) {
      inputEl.parentNode.appendChild(container);
    }
  }
  container.textContent = message;
  container.style.color = 'red';
}

function clearError(inputEl) {
  if (!inputEl) return;
  const id = inputEl.id ? inputEl.id + '-error' : null;
  const container = id ? document.getElementById(id) : null;
  if (container) {
    container.textContent = '';
  }
}

(function() {
  function escapeHtml(s) {
    if (!s && s !== 0) return '';
    return String(s).replace(/[&<>\"']/g, function(c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function formatDuration(minutes) {
    if (minutes == null || minutes === '') return '';
    const mins = Number(minutes);
    if (Number.isNaN(mins) || mins < 0) return '';
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    if (h > 0 && m > 0) return h + 'h ' + m + 'm';
    if (h > 0) return h + 'h';
    return m + 'm';
  }

  function formatDateTime(v) {
    if (!v) return '';
    try {
      const d = new Date(String(v));
      if (Number.isNaN(d.getTime())) {
        const raw = String(v).trim();
        const m = raw.match(/(\d{2}):(\d{2})/);
        return m ? m[1] + ':' + m[2] : raw;
      }
      const pad = function(n) {
        return String(n).padStart(2, '0');
      };
      return pad(d.getHours()) + ':' + pad(d.getMinutes());
    } catch (e) {
      const raw = String(v).trim();
      const m = raw.match(/(\d{2}):(\d{2})/);
      return m ? m[1] + ':' + m[2] : raw;
    }
  }

  function showSuggestions(container, items, labelSelector, onSelect) {
    container.innerHTML = '';
    if (!items || items.length === 0) {
      container.style.display = 'none';
      return;
    }
    for (const item of items) {
      const div = document.createElement('div');
      div.className = 'autocomplete-item';
      const label = labelSelector(item) || '';
      div.textContent = label;
      div.dataset.id = item.id;
      div.dataset.label = label;
      div.onclick = function() {
        onSelect(item, div);
      };
      container.appendChild(div);
    }
    container.style.display = 'block';
  }

  function closeAllModals() {
    document.querySelectorAll('.modal.active').forEach(function(modal) {
      modal.classList.remove('active');
    });
    document.body.classList.remove('modal-open');
  }

  function openModal(modal) {
    if (!modal) return;
    closeAllModals();
    modal.classList.add('active');
    document.body.classList.add('modal-open');
  }

  function closeModal(modal) {
    if (!modal) return;
    modal.classList.remove('active');
    if (!document.querySelector('.modal.active')) {
      document.body.classList.remove('modal-open');
    }
  }

  const bannerPreviewOverlay = document.getElementById('banner-preview-overlay');
  const bannerPreviewImage = document.getElementById('banner-preview-image');
  const bannerPreviewName = document.getElementById('banner-preview-name');
  const bannerHoverPreview = document.getElementById('banner-hover-preview');
  const bannerHoverPreviewImage = document.getElementById('banner-hover-preview-image');
  const bannerHoverPreviewName = document.getElementById('banner-hover-preview-name');

  function openBannerPreview(imageSrc, imageName) {
    if (!bannerPreviewOverlay || !bannerPreviewImage || !bannerPreviewName) return;
    if (!imageSrc) return;

    bannerPreviewImage.src = imageSrc;
    bannerPreviewImage.alt = imageName || 'Banner Preview';
    bannerPreviewName.textContent = imageName || 'Banner Preview';
    bannerPreviewOverlay.classList.add('active');
    bannerPreviewOverlay.setAttribute('aria-hidden', 'false');
    document.body.classList.add('modal-open');
  }

  function closeBannerPreview() {
    if (!bannerPreviewOverlay || !bannerPreviewImage) return;
    bannerPreviewOverlay.classList.remove('active');
    bannerPreviewOverlay.setAttribute('aria-hidden', 'true');
    bannerPreviewImage.src = '';
    if (!document.querySelector('.modal.active')) {
      document.body.classList.remove('modal-open');
    }
  }

  function setBannerImgFallback(imgEl) {
    if (!imgEl) return;
    imgEl.src = '/images/favicon.png';
    imgEl.alt = 'Banner unavailable';
  }

  function moveHoverPreview(x, y) {
    if (!bannerHoverPreview) return;
    const offset = 18;
    const maxLeft = Math.max(window.innerWidth - bannerHoverPreview.offsetWidth - 8, 8);
    const maxTop = Math.max(window.innerHeight - bannerHoverPreview.offsetHeight - 8, 8);
    const left = Math.min(Math.max(x + offset, 8), maxLeft);
    const top = Math.min(Math.max(y + offset, 8), maxTop);
    bannerHoverPreview.style.left = left + 'px';
    bannerHoverPreview.style.top = top + 'px';
  }

  function openBannerHoverPreview(imageSrc, imageName, x, y) {
    if (!bannerHoverPreview || !bannerHoverPreviewImage || !bannerHoverPreviewName || !imageSrc) return;
    bannerHoverPreviewImage.src = imageSrc;
    bannerHoverPreviewImage.alt = imageName || 'Banner quick preview';
    bannerHoverPreviewName.textContent = imageName || 'Banner Preview';
    bannerHoverPreview.classList.add('active');
    bannerHoverPreview.setAttribute('aria-hidden', 'false');
    moveHoverPreview(x, y);
  }

  function closeBannerHoverPreview() {
    if (!bannerHoverPreview || !bannerHoverPreviewImage) return;
    bannerHoverPreview.classList.remove('active');
    bannerHoverPreview.setAttribute('aria-hidden', 'true');
    bannerHoverPreviewImage.src = '';
  }

  function bindBannerPreviewInteractions(container) {
    if (!container) return;

    container.querySelectorAll('.banner-thumb').forEach(function(img) {
      img.addEventListener('error', function() {
        setBannerImgFallback(this);
      }, { once: true });
    });

    container.querySelectorAll('.banner-thumb-btn').forEach(function(btn) {
      btn.addEventListener('click', function() {
        const src = this.dataset.imageSrc || '';
        const name = this.dataset.imageName || 'Banner Preview';
        openBannerPreview(src, name);
      });

      btn.addEventListener('mouseenter', function(evt) {
        const src = this.dataset.imageSrc || '';
        const name = this.dataset.imageName || 'Banner Preview';
        openBannerHoverPreview(src, name, evt.clientX, evt.clientY);
      });

      btn.addEventListener('mousemove', function(evt) {
        moveHoverPreview(evt.clientX, evt.clientY);
      });

      btn.addEventListener('mouseleave', function() {
        closeBannerHoverPreview();
      });

      btn.addEventListener('focus', function() {
        const src = this.dataset.imageSrc || '';
        const name = this.dataset.imageName || 'Banner Preview';
        const rect = this.getBoundingClientRect();
        openBannerHoverPreview(src, name, rect.right, rect.top);
      });

      btn.addEventListener('blur', function() {
        closeBannerHoverPreview();
      });
    });
  }

  async function fetchBanners() {
    const tbody = document.querySelector('#admin-banners-table tbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="8">Loading banners...</td></tr>';
    try {
      const res = await fetch('/api/admin/banners');
      if (!res.ok) {
        tbody.innerHTML = '<tr><td colspan="8">Failed to load banners.</td></tr>';
        return;
      }

      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8">No banners found.</td></tr>';
        closeBannerPreview();
        return;
      }

      const sortedList = list.slice().sort(function(a, b) {
        const aId = Number(a && a.id);
        const bId = Number(b && b.id);
        if (!Number.isNaN(aId) && !Number.isNaN(bId)) return aId - bId;
        return String(a && a.id || '').localeCompare(String(b && b.id || ''));
      });

      tbody.innerHTML = '';
      for (const banner of sortedList) {
        const tr = document.createElement('tr');
        const imageSrc = banner.imageUrl || ('/admin/banner/' + encodeURIComponent(banner.id));
        tr.innerHTML =
          '<td>' + escapeHtml(banner.id) + '</td>' +
          '<td>' + escapeHtml(banner.imageName || '') + '</td>' +
          '<td>' + escapeHtml(banner.imageSizeDisplay || '-') + '</td>' +
          '<td>' + escapeHtml(banner.dimensions || '-') + '</td>' +
          '<td>' + escapeHtml(banner.aspectRatio || '-') + '</td>' +
          '<td>' + escapeHtml(banner.fileType || '-') + '</td>' +
          '<td class="banner-preview-cell"><button type="button" class="banner-thumb-btn" data-image-src="' + escapeHtml(imageSrc) + '" data-image-name="' + escapeHtml(banner.imageName || '') + '" aria-label="Preview banner ' + escapeHtml(banner.imageName || '') + '"><img class="banner-thumb" src="' + escapeHtml(imageSrc) + '" alt="Banner thumbnail" loading="lazy" decoding="async"></button></td>' +
          '<td><button type="button" class="delete-btn banner-delete-btn" data-id="' + escapeHtml(banner.id) + '">Delete</button></td>';
        tbody.appendChild(tr);
      }

      document.querySelectorAll('#admin-banners-table .banner-delete-btn').forEach(function(btn) {
        btn.onclick = onDeleteBanner;
      });

      bindBannerPreviewInteractions(tbody);
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="8">Error loading banners.</td></tr>';
    }
  }

  async function onDeleteBanner(e) {
    if (!confirm('Delete this banner image?')) return;
    const id = e.target.dataset.id;
    const res = await fetch('/admin/banners/' + id, { method: 'DELETE' });
    if (res.ok) {
      fetchBanners();
    } else {
      const message = await res.text();
      alert(message || 'Failed to delete banner');
    }
  }

  async function fetchMovies() {
    const tbody = document.querySelector('#admin-movies-table tbody');
    tbody.innerHTML = '<tr><td colspan="7">Loading movies...</td></tr>';
    try {
      const res = await fetch('/api/admin/movies');
      if (!res.ok) {
        tbody.innerHTML = '<tr><td colspan="7">Failed to load movies.</td></tr>';
        return;
      }

      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7">No movies found.</td></tr>';
        return;
      }

      const sortedList = list.slice().sort(function(a, b) {
        const aId = Number(a && a.id);
        const bId = Number(b && b.id);
        if (!Number.isNaN(aId) && !Number.isNaN(bId)) return aId - bId;
        return String(a && a.id || '').localeCompare(String(b && b.id || ''));
      });

      tbody.innerHTML = '';
      for (const m of sortedList) {
        const tr = document.createElement('tr');
        const status = (m.status || '').toString();
        tr.innerHTML = '<td>' + escapeHtml(m.id) + '</td><td>' + escapeHtml(m.title) + '</td><td>' + escapeHtml(formatDuration(m.duration)) + '</td><td>' + escapeHtml(m.language || '') + '</td><td>' + escapeHtml(m.certification || '') + '</td><td><span class="status-badge ' + escapeHtml(status.toLowerCase()) + '">' + escapeHtml(status) + '</span></td><td><button class="edit-btn" data-id="' + m.id + '">Edit</button> <button class="delete-btn" data-id="' + m.id + '">Delete</button></td>';
        tbody.appendChild(tr);
      }
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="7">Error loading movies.</td></tr>';
    }

    document.querySelectorAll('#admin-movies-table .edit-btn').forEach(function(btn) {
      btn.onclick = onEditMovie;
    });
    document.querySelectorAll('#admin-movies-table .delete-btn').forEach(function(btn) {
      btn.onclick = onDeleteMovie;
    });
  }

  async function fetchTheaters() {
    const tbody = document.querySelector('#admin-theaters-table tbody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="8">Loading theaters...</td></tr>';

    try {
      const res = await fetch('/api/admin/theaters');
      if (!res.ok) {
        tbody.innerHTML = '<tr><td colspan="8">Failed to load theaters.</td></tr>';
        return;
      }

      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8">No theaters found.</td></tr>';
        return;
      }

      const sortedList = list.slice().sort(function(a, b) {
        const aId = Number(a && a.id);
        const bId = Number(b && b.id);
        if (!Number.isNaN(aId) && !Number.isNaN(bId)) return aId - bId;
        return String(a && a.id || '').localeCompare(String(b && b.id || ''));
      });

      tbody.innerHTML = '';
      for (const t of sortedList) {
        const tr = document.createElement('tr');
        tr.innerHTML =
          '<td>' + escapeHtml(t.id) + '</td>' +
          '<td>' + escapeHtml(t.name || '') + '</td>' +
          '<td>' + escapeHtml(t.city || '') + '</td>' +
          '<td>' + escapeHtml(t.location || '') + '</td>' +
          '<td>' + escapeHtml(t.screenCount || '') + '</td>' +
          '<td>' + escapeHtml(t.price || '') + '</td>' +
          '<td>' + escapeHtml(t.elitePrice || '') + '</td>' +
          '<td><button class="admin-action-btn edit theater-edit-btn" data-id="' + escapeHtml(t.id || '') + '">Edit</button> <button class="admin-action-btn delete theater-delete-btn" data-id="' + escapeHtml(t.id || '') + '">Delete</button></td>';
        tbody.appendChild(tr);
      }

      document.querySelectorAll('#admin-theaters-table .theater-edit-btn').forEach(function(btn) {
        btn.onclick = onEditTheater;
      });
      document.querySelectorAll('#admin-theaters-table .theater-delete-btn').forEach(function(btn) {
        btn.onclick = onDeleteTheater;
      });
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="8">Error loading theaters.</td></tr>';
    }
  }

  async function onEditMovie(e) {
    const id = e.target.dataset.id;
    const res = await fetch('/api/admin/movies');
    if (!res.ok) return alert('Failed to load movie details');
    const list = await res.json();
    const m = list.find(function(x) {
      return String(x.id) === String(id);
    });
    if (!m) return alert('Movie not found');
    document.getElementById('edit-movie-id').value = m.id;
    document.getElementById('edit-title').value = m.title || '';
    document.getElementById('edit-duration').value = m.duration || '';
    document.getElementById('edit-language').value = m.language || '';
    document.getElementById('edit-certification').value = m.certification || '';
    document.getElementById('edit-description').value = m.description || '';
    document.getElementById('edit-poster').value = '';
    openModal(document.getElementById('edit-movie-modal'));
  }

  async function onDeleteMovie(e) {
    if (!confirm('Delete this movie? This will remove related schedules and ratings.')) return;
    const id = e.target.dataset.id;
    const res = await fetch('/admin/movies/' + id + '/delete', { method: 'POST' });
    if (res.ok) {
      fetchMovies();
    } else {
      alert('Failed to delete movie');
    }
  }

  async function onEditTheater(e) {
    const id = e.target.dataset.id;
    const res = await fetch('/api/admin/theaters');
    if (!res.ok) return alert('Failed to load theater details');
    const list = await res.json();
    const t = list.find(function(x) {
      return String(x.id) === String(id);
    });
    if (!t) return alert('Theater not found');

    document.getElementById('edit-theater-id').value = t.id || '';
    document.getElementById('edit-theater-name').value = t.name || '';
    document.getElementById('edit-theater-city').value = t.city || '';
    document.getElementById('edit-theater-location').value = t.location || '';
    document.getElementById('edit-theater-screen-count').value = t.screenCount || 1;
    document.getElementById('edit-theater-price').value = t.price || 1;
    document.getElementById('edit-theater-elite-price').value = t.elitePrice || 1;
    openModal(document.getElementById('edit-theater-modal'));
  }

  async function onDeleteTheater(e) {
    if (!confirm('Delete this theater? This will remove related show schedules.')) return;
    const id = e.target.dataset.id;
    const res = await fetch('/admin/theaters/' + id + '/delete', { method: 'POST' });
    if (res.ok) {
      fetchTheaters();
    } else {
      const message = await res.text();
      alert(message || 'Failed to delete theater');
    }
  }

  const editModal = document.getElementById('edit-movie-modal');
  document.getElementById('close-edit-modal').onclick = function() {
    closeModal(editModal);
  };

  const addMovieModal = document.getElementById('add-movie-modal');
  const addTheaterModal = document.getElementById('add-theater-modal');

  const openAddMovieBtn = document.getElementById('open-add-movie-modal');
  if (openAddMovieBtn) {
    openAddMovieBtn.addEventListener('click', function(evt) {
      evt.preventDefault();
      evt.stopPropagation();
      openModal(addMovieModal);
    });
    openAddMovieBtn.addEventListener('keydown', function(evt) {
      evt.stopPropagation();
    });
  }

  const openAddTheaterBtn = document.getElementById('open-add-theater-modal');
  if (openAddTheaterBtn) {
    openAddTheaterBtn.addEventListener('click', function(evt) {
      evt.preventDefault();
      evt.stopPropagation();
      openModal(addTheaterModal);
    });
    openAddTheaterBtn.addEventListener('keydown', function(evt) {
      evt.stopPropagation();
    });
  }

  if (addMovieModal) {
    const closeAddMovieBtn = document.getElementById('close-add-movie-modal');
    if (closeAddMovieBtn) {
      closeAddMovieBtn.onclick = function() {
        closeModal(addMovieModal);
      };
    }
  }

  if (addTheaterModal) {
    const closeAddTheaterBtn = document.getElementById('close-add-theater-modal');
    if (closeAddTheaterBtn) {
      closeAddTheaterBtn.onclick = function() {
        closeModal(addTheaterModal);
      };
    }
  }

  const bannerUploadButton = document.getElementById('open-banner-upload');
  const bannerFileInput = document.getElementById('banner-file-input');
  const bannerClosePreviewBtn = document.getElementById('banner-preview-close');

  if (bannerUploadButton && bannerFileInput) {
    bannerUploadButton.addEventListener('click', function(evt) {
      evt.preventDefault();
      evt.stopPropagation();
      bannerFileInput.click();
    });

    bannerFileInput.addEventListener('change', async function() {
      if (!this.files || this.files.length === 0) return;

      const file = this.files[0];
      const allowedMimeTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/x-webp'];
      const name = (file && file.name) ? file.name.toLowerCase() : '';
      const ext = name.includes('.') ? name.split('.').pop() : '';
      const allowedExtensions = ['png', 'jpg', 'jpeg', 'webp'];

      const normalizedType = (file.type || '').toLowerCase();
      const mimeValid = !normalizedType || allowedMimeTypes.includes(normalizedType);
      const extensionValid = allowedExtensions.includes(ext);
      if (!mimeValid || !extensionValid) {
        alert('Only PNG, JPEG, or WebP images are allowed.');
        this.value = '';
        return;
      }

      const formData = new FormData();
      formData.append('banner', file);

      try {
        const res = await fetch('/admin/banners', {
          method: 'POST',
          body: formData,
          credentials: 'same-origin'
        });

        if (!res.ok) {
          const message = await res.text();
          alert(message || 'Failed to upload banner');
        }
      } catch (err) {
        alert('Failed to upload banner');
      } finally {
        this.value = '';
        fetchBanners();
      }
    });
  }

  if (bannerClosePreviewBtn) {
    bannerClosePreviewBtn.addEventListener('click', function() {
      closeBannerPreview();
    });
  }

  if (bannerPreviewOverlay) {
    bannerPreviewOverlay.addEventListener('click', function(evt) {
      if (evt.target === bannerPreviewOverlay) {
        closeBannerPreview();
      }
    });
  }

  document.addEventListener('keydown', function(evt) {
    if (evt.key === 'Escape' && bannerPreviewOverlay && bannerPreviewOverlay.classList.contains('active')) {
      closeBannerPreview();
    }
    if (evt.key === 'Escape' && bannerHoverPreview && bannerHoverPreview.classList.contains('active')) {
      closeBannerHoverPreview();
    }
  });

  const editShowModal = document.getElementById('edit-show-modal');
  document.getElementById('close-edit-show-modal').onclick = function() {
    closeModal(editShowModal);
  };

  const editTheaterModal = document.getElementById('edit-theater-modal');
  if (editTheaterModal) {
    document.getElementById('close-edit-theater-modal').onclick = function() {
      closeModal(editTheaterModal);
    };
  }

  [editModal, editShowModal, editTheaterModal, addMovieModal, addTheaterModal].forEach(function(modal) {
    if (!modal) return;
    modal.addEventListener('click', function(evt) {
      if (evt.target === modal) {
        closeModal(modal);
      }
    });
  });

  document.getElementById('edit-movie-form').addEventListener('submit', async function(evt) {
    evt.preventDefault();
    const id = document.getElementById('edit-movie-id').value;
    const form = new FormData();
    form.append('title', document.getElementById('edit-title').value);
    form.append('duration', document.getElementById('edit-duration').value);
    form.append('language', document.getElementById('edit-language').value);
    form.append('certification', document.getElementById('edit-certification').value);
    form.append('description', document.getElementById('edit-description').value);
    const file = document.getElementById('edit-poster').files[0];
    if (file) form.append('poster', file);
    const res = await fetch('/admin/movies/' + id + '/update', { method: 'POST', body: form });
    if (res.ok) {
      closeModal(editModal);
      fetchMovies();
    } else {
      alert('Failed to save changes');
    }
  });

  const timeslotRows = document.getElementById('timeslot-rows');
  const addTimeslotBtn = document.getElementById('add-timeslot-btn');

  function addTimeslotRow(defaultTime, defaultScreen) {
    const row = document.createElement('div');
    row.className = 'timeslot-row';
    row.innerHTML =
      '<div class="admin-row">' +
        '<div class="form-group">' +
          '<label>Timeslot</label>' +
          '<input type="time" name="timeslot_time" value="' + escapeHtml(defaultTime || '') + '" required>' +
        '</div>' +
        '<div class="form-group">' +
          '<label>Select Screen for timeslot</label>' +
          '<input type="number" name="timeslot_screen" value="' + escapeHtml(defaultScreen || '1') + '" min="1" required>' +
        '</div>' +
      '</div>' +
      '<div class="admin-row">' +
        '<div class="form-group">' +
          '<label style="visibility:hidden;" aria-hidden="true">Remove Timeslot</label>' +
          '<button type="button" class="admin-btn remove-timeslot-btn">Remove</button>' +
        '</div>' +
      '</div>';

    row.querySelector('.remove-timeslot-btn').onclick = function() {
      row.remove();
      if (timeslotRows.children.length === 0) addTimeslotRow('', '1');
    };
    timeslotRows.appendChild(row);
  }

  addTimeslotBtn.addEventListener('click', function() {
    addTimeslotRow('', '1');
  });
  addTimeslotRow('', '1');

  async function bindAutocomplete(inputId, hiddenId, suggestionsId, endpoint, labelSelector, afterSelect) {
    const input = document.getElementById(inputId);
    const hidden = document.getElementById(hiddenId);
    const container = document.getElementById(suggestionsId);

    input.addEventListener('input', async function() {
      const q = this.value.trim();
      hidden.value = '';
      if (!q) {
        container.style.display = 'none';
        return;
      }
      try {
        const res = await fetch(endpoint + encodeURIComponent(q));
        if (!res.ok) {
          container.style.display = 'none';
          return;
        }
        const list = await res.json();
        showSuggestions(container, list.slice(0, 20), labelSelector, function(item) {
          hidden.value = item.id;
          input.value = labelSelector(item);
          container.style.display = 'none';
          if (afterSelect) afterSelect(item);
        });
      } catch (e) {
        container.style.display = 'none';
      }
    });

    input.addEventListener('blur', function() {
      setTimeout(function() {
        container.style.display = 'none';
      }, 200);
    });
  }

  bindAutocomplete('show-movie-input', 'show-movie-id', 'show-movie-suggestions', '/api/movies/search?q=', function(x) {
    return x.title || '';
  });

  bindAutocomplete('show-theater-input', 'show-theater-id', 'show-theater-suggestions', '/api/theaters/search?q=', function(x) {
    return x.name || '';
  }, function(selected) {
    const count = Number(selected.screenCount || 0);
    if (count > 0) {
      document.querySelectorAll('input[name="timeslot_screen"]').forEach(function(inp) {
        inp.max = String(count);
      });
    }
  });

  bindAutocomplete('manage-show-theater-input', 'manage-show-theater-id', 'manage-show-theater-suggestions', '/api/theaters/search?q=', function(x) {
    return x.name || '';
  }, function(selected) {
    document.getElementById('manage-show-theater-screen-count').value = selected && selected.screenCount ? String(selected.screenCount) : '';
    maybeLoadShowsForFilter();
  });

  bindAutocomplete('manage-show-movie-input', 'manage-show-movie-id', 'manage-show-movie-suggestions', '/api/movies/search?q=', function(x) {
    return x.title || '';
  }, function() {
    maybeLoadShowsForFilter();
  });

  function maybeLoadShowsForFilter() {
    const theaterId = document.getElementById('manage-show-theater-id').value;
    const movieId = document.getElementById('manage-show-movie-id').value;
    if (theaterId && movieId) {
      loadShowsForFilter();
    }
  }

  document.getElementById('manage-show-theater-input').addEventListener('input', function() {
    if (this.value.trim()) return;
    document.getElementById('manage-show-theater-id').value = '';
    document.getElementById('manage-show-theater-screen-count').value = '';
    document.querySelector('.shows-table tbody').innerHTML = '<tr><td colspan="6">Select theatre and movie to view shows.</td></tr>';
  });

  document.getElementById('manage-show-movie-input').addEventListener('input', function() {
    if (this.value.trim()) return;
    document.getElementById('manage-show-movie-id').value = '';
    document.querySelector('.shows-table tbody').innerHTML = '<tr><td colspan="6">Select theatre and movie to view shows.</td></tr>';
  });

  document.getElementById('schedule-show-form').addEventListener('submit', async function(evt) {
    evt.preventDefault();

    const movieId = document.getElementById('show-movie-id').value;
    const theaterId = document.getElementById('show-theater-id').value;
    const startDate = document.getElementById('show-start-date').value;
    const endDate = document.getElementById('show-end-date').value;
    if (!movieId) return alert('Please select a movie from suggestions.');
    if (!theaterId) return alert('Please select a theatre from suggestions.');
    if (!startDate || !endDate) return alert('Please choose both start and end dates.');

    const rows = Array.from(document.querySelectorAll('#timeslot-rows .timeslot-row'));
    if (rows.length === 0) return alert('Add at least one timeslot.');

    for (const row of rows) {
      const timeEl = row.querySelector('input[name="timeslot_time"]');
      const screenEl = row.querySelector('input[name="timeslot_screen"]');
      if (!timeEl.value) return alert('Each timeslot needs a start time.');
      const screen = Number(screenEl.value);
      if (!screen || screen < 1) return alert('Each timeslot needs a valid screen.');
    }

    const encodedBody = new URLSearchParams(new FormData(this));
    const res = await fetch('/admin/shows', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: encodedBody
    });

    if (res.ok) {
      alert('Schedule saved successfully.');
      this.reset();
      timeslotRows.innerHTML = '';
      addTimeslotRow('', '1');
      const selectedTheater = document.getElementById('manage-show-theater-id').value;
      const selectedMovie = document.getElementById('manage-show-movie-id').value;
      if (selectedTheater === theaterId && selectedMovie === movieId) {
        loadShowsForFilter();
      }
      return;
    }

    const message = await res.text();
    alert(message || 'Failed to schedule shows.');
  });

  async function loadShowsForFilter() {
    const theaterId = document.getElementById('manage-show-theater-id').value;
    const movieId = document.getElementById('manage-show-movie-id').value;
    const tbody = document.querySelector('.shows-table tbody');
    if (!theaterId || !movieId) {
      tbody.innerHTML = '<tr><td colspan="6">Select theatre and movie to view shows.</td></tr>';
      return;
    }

    tbody.innerHTML = '<tr><td colspan="6">Loading...</td></tr>';
    try {
      const url = '/api/admin/shows?theater_id=' + encodeURIComponent(theaterId) + '&movie_id=' + encodeURIComponent(movieId);
      const res = await fetch(url);
      if (!res.ok) {
        tbody.innerHTML = '<tr><td colspan="6">Failed to load shows.</td></tr>';
        return;
      }
      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">No shows found for selected theatre/movie.</td></tr>';
        return;
      }

      tbody.innerHTML = '';
      for (const s of list) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td>' + escapeHtml(s.movieTitle || '') + '</td><td>' + escapeHtml(formatDateTime(s.startTime)) + '</td><td>' + escapeHtml(s.startDate || '') + '</td><td>' + escapeHtml(s.endDate || '') + '</td><td>' + escapeHtml(s.screen || '') + '</td><td><button class="admin-action-btn edit" data-id="' + escapeHtml(s.id || '') + '" data-start-date="' + escapeHtml(s.startDate || '') + '" data-end-date="' + escapeHtml(s.endDate || '') + '" data-screen="' + escapeHtml(s.screen || '') + '">Edit</button> <button class="admin-action-btn delete" data-id="' + escapeHtml(s.id || '') + '">Delete</button></td>';
        tbody.appendChild(tr);
      }

      document.querySelectorAll('.shows-table .admin-action-btn.edit').forEach(function(btn) {
        btn.onclick = function() {
          document.getElementById('edit-show-id').value = this.dataset.id || '';
          document.getElementById('edit-show-start-date').value = this.dataset.startDate || '';
          document.getElementById('edit-show-end-date').value = this.dataset.endDate || '';
          document.getElementById('edit-show-screen').value = this.dataset.screen || '1';
          const screenCount = Number(document.getElementById('manage-show-theater-screen-count').value || 0);
          const screenInput = document.getElementById('edit-show-screen');
          if (screenCount > 0) {
            screenInput.max = String(screenCount);
          } else {
            screenInput.removeAttribute('max');
          }
          openModal(editShowModal);
        };
      });

      document.querySelectorAll('.shows-table .admin-action-btn.delete').forEach(function(btn) {
        btn.onclick = async function() {
          if (!confirm('Delete this scheduled slot?')) return;
          const id = this.dataset.id;
          const res = await fetch('/admin/shows/' + id + '/delete', { method: 'POST' });
          if (res.ok) {
            loadShowsForFilter();
          } else {
            alert('Failed to delete slot');
          }
        };
      });
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="6">Error loading shows.</td></tr>';
    }
  }

  document.getElementById('edit-show-form').addEventListener('submit', async function(evt) {
    evt.preventDefault();
    const id = document.getElementById('edit-show-id').value;
    const formBody = new URLSearchParams(new FormData(this));
    const res = await fetch('/admin/shows/' + id + '/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: formBody
    });

    if (res.ok) {
      closeModal(editShowModal);
      loadShowsForFilter();
      return;
    }

    const message = await res.text();
    alert(message || 'Failed to update show');
  });

  const editTheaterForm = document.getElementById('edit-theater-form');
  if (editTheaterForm) {
    editTheaterForm.addEventListener('submit', async function(evt) {
      evt.preventDefault();
      const id = document.getElementById('edit-theater-id').value;
      const formBody = new URLSearchParams(new FormData(this));
      const res = await fetch('/admin/theaters/' + id + '/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: formBody
      });

      if (res.ok) {
        closeModal(editTheaterModal);
        fetchTheaters();
        return;
      }

      const message = await res.text();
      alert(message || 'Failed to update theater');
    });
  }

  function initializeSectionToggles() {
    const headers = document.querySelectorAll('.section-header');

    headers.forEach(function(header) {
      const content = header.nextElementSibling;
      if (!content || !content.classList.contains('section-content')) return;

      const icon = header.querySelector('.toggle-icon');
      const toggleBtn = header.querySelector('.section-toggle-btn');
      const syncState = function() {
        const expanded = !content.classList.contains('hidden');
        header.setAttribute('aria-expanded', String(expanded));

        if (toggleBtn) {
          toggleBtn.setAttribute('aria-expanded', String(expanded));
        }

        if (icon) {
          if (icon.tagName === 'IMG') {
            const expandedSrc = icon.dataset.expandedSrc || '';
            const collapsedSrc = icon.dataset.collapsedSrc || '';
            if (expanded && expandedSrc) {
              icon.setAttribute('src', expandedSrc);
            } else if (!expanded && collapsedSrc) {
              icon.setAttribute('src', collapsedSrc);
            }
          } else {
            icon.classList.toggle('rotate', expanded);
          }
        }
      };

      const onToggle = function() {
        content.classList.toggle('hidden');
        syncState();
      };

      if (toggleBtn) {
        toggleBtn.addEventListener('click', function(event) {
          event.preventDefault();
          event.stopPropagation();
          onToggle();
        });
      } else {
        header.addEventListener('click', onToggle);
        header.addEventListener('keydown', function(event) {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            onToggle();
          }
        });
      }

      syncState();
    });
  }

  // Admin form submissions for add movie and add theater.
  const adminForms = document.querySelectorAll('#add-movie-form, #add-theater-form');
  adminForms.forEach(function(form) {
    if (form.id === 'add-movie-form') {
      form.addEventListener('submit', async function(e) {
        e.preventDefault();

        clearError(document.getElementById('movie-title'));
        clearError(document.getElementById('movie-duration'));
        clearError(document.getElementById('movie-language'));
        clearError(document.getElementById('movie-certification'));
        clearError(document.getElementById('movie-description'));
        clearError(document.getElementById('movie-poster'));

        let hasError = false;
        const titleEl = document.getElementById('movie-title');
        const durationEl = document.getElementById('movie-duration');
        const languageEl = document.getElementById('movie-language');
        const certificationEl = document.getElementById('movie-certification');
        const descriptionEl = document.getElementById('movie-description');
        const posterEl = document.getElementById('movie-poster');

        const title = titleEl.value && titleEl.value.trim();
        const duration = parseInt(durationEl.value, 10);
        const language = languageEl.value && languageEl.value.trim();
        const certification = certificationEl.value && certificationEl.value.trim();
        const description = descriptionEl.value && descriptionEl.value.trim();
        const posterFile = posterEl.files && posterEl.files[0];

        if (!title) {
          showError(titleEl, 'Movie title is required');
          hasError = true;
        }
        if (!duration || isNaN(duration)) {
          showError(durationEl, 'Duration is required');
          hasError = true;
        } else if (duration <= 60) {
          showError(durationEl, 'Duration must be greater than 60 minutes');
          hasError = true;
        }
        if (!language) {
          showError(languageEl, 'Language is required');
          hasError = true;
        }
        if (!certification) {
          showError(certificationEl, 'Certification is required');
          hasError = true;
        }
        if (!description) {
          showError(descriptionEl, 'Description is required');
          hasError = true;
        }
        if (!posterFile) {
          showError(posterEl, 'Movie poster is required');
          hasError = true;
        } else {
          const validTypes = ['image/jpeg', 'image/jpg', 'image/png'];
          if (!validTypes.includes(posterFile.type)) {
            const name = posterFile.name || '';
            const ext = name.split('.').pop().toLowerCase();
            if (!['jpg', 'jpeg', 'png'].includes(ext)) {
              showError(posterEl, 'Accepted formats: jpg, jpeg, png');
              hasError = true;
            }
          }
        }

        if (hasError) return;

        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn ? submitBtn.textContent : '';
        if (submitBtn) {
          submitBtn.textContent = 'Uploading...';
          submitBtn.style.opacity = '0.8';
        }

        const formData = new FormData(this);

        try {
          const res = await fetch('/admin/movies', {
            method: 'POST',
            body: formData,
            credentials: 'same-origin'
          });

          if (res.ok) {
            if (submitBtn) submitBtn.textContent = 'Added Successfully!';
            setTimeout(() => {
              if (submitBtn) {
                submitBtn.textContent = originalText;
                submitBtn.style.opacity = '1';
              }
              this.reset();
              closeModal(addMovieModal);
              fetchMovies();
            }, 1500);
          } else if (res.status === 409) {
            const msg = await res.text();
            showError(titleEl, msg || 'Duplicate movie');
            if (submitBtn) submitBtn.textContent = originalText;
            if (submitBtn) submitBtn.style.opacity = '1';
          } else {
            if (submitBtn) submitBtn.textContent = 'Error';
            setTimeout(() => {
              if (submitBtn) submitBtn.textContent = originalText;
            }, 1500);
          }
        } catch (err) {
          if (submitBtn) submitBtn.textContent = 'Error';
          setTimeout(() => {
            if (submitBtn) submitBtn.textContent = originalText;
          }, 1500);
        }
      });
      return;
    }

    if (form.id === 'add-theater-form') {
      form.addEventListener('submit', async function(e) {
        e.preventDefault();

        clearError(document.getElementById('theater-name'));
        clearError(document.getElementById('theater-city'));
        clearError(document.getElementById('theater-location'));
        clearError(document.getElementById('screen-count'));
        clearError(document.getElementById('ticket-price'));
        clearError(document.getElementById('elite-ticket-price'));

        let hasError = false;
        const nameEl = document.getElementById('theater-name');
        const cityEl = document.getElementById('theater-city');
        const locationEl = document.getElementById('theater-location');
        const screenEl = document.getElementById('screen-count');
        const priceEl = document.getElementById('ticket-price');
        const elitePriceEl = document.getElementById('elite-ticket-price');

        const name = nameEl.value && nameEl.value.trim();
        const city = cityEl.value && cityEl.value.trim();
        const location = locationEl.value && locationEl.value.trim();
        const screenCount = parseInt(screenEl.value, 10);
        const ticketPrice = parseInt(priceEl.value, 10);
        const eliteTicketPrice = parseInt(elitePriceEl.value, 10);

        if (!name) {
          showError(nameEl, 'Theater name is required');
          hasError = true;
        }
        if (!city) {
          showError(cityEl, 'City is required');
          hasError = true;
        }
        if (!location) {
          showError(locationEl, 'Location is required');
          hasError = true;
        }
        if (!screenCount || isNaN(screenCount)) {
          showError(screenEl, 'Screen count is required');
          hasError = true;
        } else if (screenCount < 1) {
          showError(screenEl, 'Screen count must be at least 1');
          hasError = true;
        }
        if (!ticketPrice || isNaN(ticketPrice)) {
          showError(priceEl, 'Ticket price is required');
          hasError = true;
        } else if (ticketPrice < 1) {
          showError(priceEl, 'Ticket price must be at least 1');
          hasError = true;
        }
        if (!eliteTicketPrice || isNaN(eliteTicketPrice)) {
          showError(elitePriceEl, 'Elite ticket price is required');
          hasError = true;
        } else if (eliteTicketPrice < 1) {
          showError(elitePriceEl, 'Elite ticket price must be at least 1');
          hasError = true;
        }

        if (hasError) return;

        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn ? submitBtn.textContent : '';
        if (submitBtn) {
          submitBtn.textContent = 'Adding...';
          submitBtn.style.opacity = '0.8';
        }

        const formData = new FormData(this);

        try {
          const res = await fetch('/admin/theaters', {
            method: 'POST',
            body: formData,
            credentials: 'same-origin'
          });

          if (res.ok) {
            if (submitBtn) submitBtn.textContent = 'Added Successfully!';
            setTimeout(() => {
              if (submitBtn) {
                submitBtn.textContent = originalText;
                submitBtn.style.opacity = '1';
              }
              this.reset();
              closeModal(addTheaterModal);
              fetchTheaters();
            }, 1500);
          } else if (res.status === 409) {
            const msg = await res.text();
            showError(nameEl, msg || 'Duplicate theater');
            if (submitBtn) submitBtn.textContent = originalText;
            if (submitBtn) submitBtn.style.opacity = '1';
          } else {
            if (submitBtn) submitBtn.textContent = 'Error';
            setTimeout(() => {
              if (submitBtn) submitBtn.textContent = originalText;
            }, 1500);
          }
        } catch (err) {
          if (submitBtn) submitBtn.textContent = 'Error';
          setTimeout(() => {
            if (submitBtn) submitBtn.textContent = originalText;
          }, 1500);
        }
      });
      return;
    }

  });

  const removeMovieBtns = document.querySelectorAll('.admin-remove-btn');
  removeMovieBtns.forEach(function(btn) {
    btn.addEventListener('click', function(e) {
      e.preventDefault();
      const movieItem = this.parentElement;
      movieItem.style.opacity = '0.5';
      movieItem.style.textDecoration = 'line-through';
      this.textContent = 'Removed';
      this.disabled = true;
      setTimeout(function() {
        movieItem.remove();
      }, 1000);
    });
  });

  initializeSectionToggles();
  fetchBanners();
  bindBannerPreviewInteractions(document);
  fetchMovies();
  fetchTheaters();
  document.querySelector('.shows-table tbody').innerHTML = '<tr><td colspan="6">Select theatre and movie to view shows.</td></tr>';
})();
