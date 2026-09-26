// Orbis Stream: the only script of the Razor pages. Dialogs open with the native
// command/commandfor attributes; this file adds the confirmation dialog and polling.

// <form data-confirm="question">: asks in the shared ContentDialog before submitting.
document.addEventListener("submit", event => {
  const form = event.target;
  if (!form.dataset.confirm || form.dataset.confirmed) return;
  event.preventDefault();
  const dialog = document.getElementById("confirm-dialog");
  dialog.querySelector("[data-text]").textContent = form.dataset.confirm;
  dialog.returnValue = "";
  dialog.onclose = () => {
    if (dialog.returnValue !== "ok") return;
    form.dataset.confirmed = "1";
    form.requestSubmit(event.submitter);
  };
  dialog.showModal();
});

// <div data-refresh="url" data-interval="ms">: replaces its content with the server partial,
// skipped while one of its dialogs is open so that the user never loses the form in progress.
for (const region of document.querySelectorAll("[data-refresh]")) {
  const interval = Number(region.dataset.interval || 5000);
  setInterval(async () => {
    if (document.hidden || region.querySelector("dialog[open]")) return;
    try {
      const response = await fetch(region.dataset.refresh);
      if (response.ok) region.innerHTML = await response.text();
    } catch {
      // The server is restarting or the window is closing: try again on the next tick.
    }
  }, interval);
}

// <div data-countdown="seconds" data-href="url">: counts down, then navigates.
for (const counter of document.querySelectorAll("[data-countdown]")) {
  let seconds = Number(counter.dataset.countdown);
  const tick = () => {
    counter.textContent = seconds;
    if (seconds-- <= 0) location.href = counter.dataset.href;
    else setTimeout(tick, 1000);
  };
  tick();
}

// <dialog data-open>: dialogs the server rendered for an edit (?edit=, ?link=) open as modal.
for (const dialog of document.querySelectorAll("dialog[data-open]")) dialog.showModal();
