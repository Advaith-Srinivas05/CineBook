// Profile page behavior: click avatar to upload image and show toast feedback.
document.addEventListener('DOMContentLoaded', function() {
  const profilePage = document.getElementById('profile-page');
  if (!profilePage) {
    return;
  }

  const avatarButton = document.getElementById('profile-avatar-button');
  const imageInput = document.getElementById('profile-image-input');
  const imagePreview = document.getElementById('profile-avatar-preview');
  const toast = document.getElementById('profile-toast');

  const showToast = function(message, isError) {
    if (!toast || !message) {
      return;
    }
    toast.textContent = message;
    toast.classList.remove('show', 'error', 'success');
    toast.classList.add('show', isError ? 'error' : 'success');
    window.setTimeout(function() {
      toast.classList.remove('show');
    }, 2600);
  };

  const flashMessages = [
    profilePage.dataset.profileSuccess,
    profilePage.dataset.passwordSuccess,
    profilePage.dataset.profileError,
    profilePage.dataset.passwordError
  ];

  if (flashMessages[0]) showToast(flashMessages[0], false);
  if (flashMessages[1]) showToast(flashMessages[1], false);
  if (flashMessages[2]) showToast(flashMessages[2], true);
  if (flashMessages[3]) showToast(flashMessages[3], true);

  if (!avatarButton || !imageInput || !imagePreview) {
    return;
  }

  avatarButton.addEventListener('click', function() {
    imageInput.click();
  });

  imageInput.addEventListener('change', async function() {
    const file = imageInput.files && imageInput.files[0];
    if (!file) {
      return;
    }

    if (!file.type || file.type.indexOf('image/') !== 0) {
      showToast('Please choose a valid image file.', true);
      imageInput.value = '';
      return;
    }

    // Local preview before upload so the UI feels immediate.
    const localPreviewUrl = URL.createObjectURL(file);
    imagePreview.src = localPreviewUrl;

    const formData = new FormData();
    formData.append('profileImage', file);

    try {
      const response = await fetch('/profile/upload-image', {
        method: 'POST',
        body: formData,
        headers: {
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      const payload = await response.json();
      if (!response.ok) {
        showToast(payload && payload.error ? payload.error : 'Failed to upload profile image.', true);
        return;
      }

      if (payload && payload.imageUrl) {
        imagePreview.src = payload.imageUrl;
      }
      showToast(payload && payload.message ? payload.message : 'Profile image updated successfully.', false);
    } catch (_error) {
      showToast('Failed to upload profile image.', true);
    } finally {
      imageInput.value = '';
      URL.revokeObjectURL(localPreviewUrl);
    }
  });
});
