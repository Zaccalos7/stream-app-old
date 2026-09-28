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

// <input type="radio" data-stream-url="rtmp://…">: picking a platform fills its form's streamUrl,
// unless the user typed a custom ingest (anything that is not one of the platform presets).
document.addEventListener("change", event => {
  const url = event.target.dataset?.streamUrl;
  if (!url) return;
  const field = event.target.form.elements.streamUrl;
  const presets = [...event.target.form.querySelectorAll("[data-stream-url]")].map(radio => radio.dataset.streamUrl);
  if (!field.value.trim() || presets.includes(field.value.trim())) field.value = url;
});

// <input data-drop-path>: dropping a file or folder on it fills its absolute path. The browser
// never exposes paths, so the dropped File goes to the WebView2 host, which answers with its path.
// The whole field is the target (so the hint under it counts too) and lights up while the drag is
// over it, because a drop has no other visible sign until the path appears.
// Drops anywhere else are swallowed: by default WebView2 would navigate away to the file.
let dropTarget = null;
let dropZone = null;
let dropHint = "";
let dropHintTimer = 0;

const dropFieldOf = event => {
  const hit = event.target;
  return hit.closest?.("[data-drop-path]") || hit.closest?.(".dropzone")?.querySelector("[data-drop-path]");
};

document.addEventListener("dragover", event => {
  event.preventDefault();
  const field = dropFieldOf(event);
  if (!field) return;
  const zone = field.closest(".dropzone");
  if (zone === dropZone) return;
  dropZone?.classList.remove("is-over");
  dropZone = zone;
  dropZone?.classList.add("is-over");
});

document.addEventListener("dragleave", event => {
  if (!dropZone || dropZone.contains(event.relatedTarget)) return;
  dropZone.classList.remove("is-over");
});

document.addEventListener("drop", event => {
  event.preventDefault();
  const field = dropFieldOf(event);
  const files = event.dataTransfer?.files;
  dropZone?.classList.remove("is-over");
  dropZone = null;
  if (!field || !files?.length || !window.chrome?.webview) return;
  dropTarget = field;
  dropZone = field.closest(".dropzone");
  window.chrome.webview.postMessageWithAdditionalObjects("dropPath", files);
});

window.chrome?.webview?.addEventListener("message", event => {
  if (!dropTarget || typeof event.data !== "string") return;
  dropTarget.value = event.data;
  dropTarget.dispatchEvent(new Event("input", { bubbles: true }));

  // The path is in the field, but nothing says it came from the drop: say it for a couple of seconds.
  const zone = dropZone;
  const hint = zone?.querySelector("small span");
  if (hint) {
    if (!dropHint) dropHint = hint.textContent;
    hint.textContent = dropTarget.dataset.dropDone || hint.textContent;
    zone.classList.add("is-dropped");
    clearTimeout(dropHintTimer);
    dropHintTimer = setTimeout(() => {
      hint.textContent = dropHint;
      zone.classList.remove("is-dropped");
    }, 3000);
  }

  dropTarget = null;
  dropZone = null;
});

// <button data-reveal> glued to a password input: shows or hides the value. It is always in the
// markup, so the key of a stored configuration can be checked in the edit form too, not only while
// it is being typed for the first time.
document.addEventListener("click", event => {
  const button = event.target.closest?.("[data-reveal]");
  if (!button) return;
  const field = button.parentElement?.querySelector("input");
  if (!field) return;

  const showing = field.type === "text";
  field.type = showing ? "password" : "text";
  button.setAttribute("aria-pressed", showing ? "false" : "true");
  const label = showing ? field.dataset.revealLabelShow : field.dataset.revealLabelHide;
  if (label) {
    button.title = label;
    button.setAttribute("aria-label", label);
  }
});
