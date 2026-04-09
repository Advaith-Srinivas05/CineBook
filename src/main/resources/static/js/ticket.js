// Ticket page clipboard sharing.
(function () {
  const shareButton = document.getElementById('share-ticket-link');
  const feedback = document.getElementById('share-link-feedback');

  if (!shareButton || !feedback) {
    return;
  }

  shareButton.addEventListener('click', async function () {
    const publicId = shareButton.dataset.publicId;

    if (!publicId) {
      feedback.textContent = 'Unable to copy link.';
      return;
    }

    const ticketUrl = window.location.origin + '/ticket/' + publicId;

    try {
      await navigator.clipboard.writeText(ticketUrl);
      feedback.textContent = 'Link copied!';
      window.setTimeout(function () {
        feedback.textContent = '';
      }, 2000);
    } catch (error) {
      feedback.textContent = 'Copy failed. Please try again.';
    }
  });
})();
