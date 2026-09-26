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
// Drops anywhere else are swallowed: by default WebView2 would navigate away to the file.
let dropTarget = null;
document.addEventListener("dragover", event => event.preventDefault());
document.addEventListener("drop", event => {
  event.preventDefault();
  const input = event.target.closest?.("[data-drop-path]");
  const files = event.dataTransfer?.files;
  if (!input || !files?.length || !window.chrome?.webview) return;
  dropTarget = input;
  window.chrome.webview.postMessageWithAdditionalObjects("dropPath", files);
});
window.chrome?.webview?.addEventListener("message", event => {
  if (!dropTarget || typeof event.data !== "string") return;
  dropTarget.value = event.data;
  dropTarget.dispatchEvent(new Event("input", { bubbles: true }));
  dropTarget = null;
});
