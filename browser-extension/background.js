async function updateTabs() {
  try {
    const tabs = await chrome.tabs.query({});
    const mediaTabs = [];
    for (const tab of tabs) {
      if (tab.audible || (tab.url && (tab.url.includes("youtube.com/watch") || tab.url.includes("open.spotify.com") || tab.url.includes("soundcloud.com") || tab.url.includes("music.yandex")))) {
        mediaTabs.push({
          id: tab.id,
          title: tab.title || "Unknown",
          url: tab.url || "",
          favIconUrl: tab.favIconUrl || "",
          audible: tab.audible || false,
          active: tab.active || false
        });
      }
    }

    if (mediaTabs.length === 0) {
      for (const tab of tabs) {
        if (tab.url && (tab.url.includes("youtube.com/watch") || tab.url.includes("open.spotify.com") || tab.url.includes("soundcloud.com") || tab.url.includes("music.yandex"))) {
          mediaTabs.push({
            id: tab.id,
            title: tab.title || "Unknown",
            url: tab.url || "",
            favIconUrl: tab.favIconUrl || "",
            audible: tab.audible || false,
            active: tab.active || false
          });
          break;
        }
      }
    }

    const response = await fetch("http://localhost:31252/api/media/update", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mediaTabs)
    });
    
    if (response.ok) {
      const data = await response.json();
      if (data && data.command) {
        handleCommand(data.command, data.tabId);
      }
    }
  } catch (e) {
    // Server offline, ignore
  }
}

function handleCommand(command, tabId) {
  if (command === "play") {
    chrome.scripting.executeScript({
      target: { tabId: tabId },
      func: () => {
        let media = document.querySelectorAll('video, audio');
        media.forEach(m => m.play());
      }
    });
  } else if (command === "pause") {
    chrome.scripting.executeScript({
      target: { tabId: tabId },
      func: () => {
        let media = document.querySelectorAll('video, audio');
        media.forEach(m => m.pause());
      }
    });
  } else if (command === "playPause") {
    chrome.scripting.executeScript({
      target: { tabId: tabId },
      func: () => {
        let media = document.querySelectorAll('video, audio');
        if (media.length > 0) {
          let anyPlaying = Array.from(media).some(m => !m.paused);
          media.forEach(m => anyPlaying ? m.pause() : m.play());
        } else {
          let playBtn = document.querySelector('.playButton, .ytp-play-button, .spoticon-play-16, [data-testid="control-button-playpause"]');
          if (playBtn) playBtn.click();
        }
      }
    });
  } else if (command === "next") {
    chrome.scripting.executeScript({
      target: { tabId: tabId },
      func: () => {
        let nextBtn = document.querySelector('.ytp-next-button, .control-button-skip-forward, [data-testid="control-button-skip-forward"], .spoticon-skip-forward-16');
        if (nextBtn) nextBtn.click();
      }
    });
  } else if (command === "previous") {
    chrome.scripting.executeScript({
      target: { tabId: tabId },
      func: () => {
        let prevBtn = document.querySelector('.ytp-prev-button, .control-button-skip-back, [data-testid="control-button-skip-back"], .spoticon-skip-back-16');
        if (prevBtn) prevBtn.click();
      }
    });
  }
}

setInterval(updateTabs, 1000);
