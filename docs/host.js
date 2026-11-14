// Host mode for the web client: create a room, build the options, start and finish voting.
// Loaded after app.js and relies on its globals (sb, roomId, roomData, userId, ...).

const btnCreateRoom = document.getElementById("btn-create-room");
const screenSetup = document.getElementById("screen-setup");

let hostSetupOpen = false;
let setupChoices = [];
let setupMode = "LIKE";
let hostFinishScheduled = false;
let waitSince = 0;
let waitSnoozeUntil = 0;
const WAIT_PROMPT_MS = 45000;

function isHost() {
    return !!roomData && roomData.hostId === userId;
}

const MODES = [
    { id: "LIKE", label: "Beğen", hint: "Herkes seçenekleri kaydırarak beğenir; en çok beğenilen kazanır." },
    { id: "SINGLE", label: "Tek seçim", hint: "Herkes tek bir seçenek seçer; en çok oy alan kazanır." },
    { id: "RANK", label: "Sırala", hint: "Herkes tüm seçenekleri sıralar; en yüksek toplam puan kazanır." }
];

const PRESETS = {
    "Yemek": [["Pizzacı", "Taş fırında İtalyan pizzası"], ["Burgerci", "Gurme hamburgerler ve çıtır patates"],
        ["Kebapçı", "Zengin meze ve enfes Adana kebap"], ["Sushi Bar", "Uzak doğu lezzetleri ve taze roll'lar"],
        ["Starbucks", "Kahve ve leziz tatlı molası"]],
    "Aktivite": [["Bowling", "Grupça bowling turnuvası"], ["Sinema", "Vizyondaki en yeni aksiyon filmi"],
        ["Kafe Sohbeti", "Loş bir kafede koyu muhabbet"], ["Konser", "Açık hava rock konseri coşkusu"],
        ["Tiyatro", "Sezonun popüler komedi oyunu"]],
    "Film": [["Bilim Kurgu", "Yıldızlararası yolculuk ve uzay temalı"], ["Komedi", "Gülme garantili yerli komedi"],
        ["Korku", "Gerilim dolu karanlık bir ev hikayesi"], ["Aksiyon / Macera", "Nefes kesen kovalamaca ve dövüş sahneleri"],
        ["Romantik", "Duygusal ve sıcak bir aşk öyküsü"]],
    "Eğlence": [["PlayStation Kafe", "FC 24 ve dövüş oyunları kapışması"], ["Karaoke", "Detone olmayı göze alanlar kulübü"],
        ["Kutu Oyunları", "Catan, Tabu veya Monopoly gecesi"], ["Bilardo / Dart", "Hassas atışlar ve rekabet"]]
};

function newId() {
    return (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random().toString(16).slice(2));
}

// One option per line; "Name - details" (or "Name | details") fills the description.
function parseQuickChoices(text, limit = 50) {
    const seen = new Set();
    const out = [];
    const sep = /\s+[-\u2013|]\s+|\s*[|:]\s+/;
    for (const raw of text.split(/\r?\n/)) {
        const line = raw.replace(/^\s*(?:[-*\u2022\u2013]+|\d{1,2}[.)])\s*/, "").trim();
        if (!line) continue;
        const m = sep.exec(line);
        const name = (m ? line.slice(0, m.index) : line).trim().slice(0, 100);
        if (!name || seen.has(name.toLowerCase())) continue;
        seen.add(name.toLowerCase());
        out.push({ id: newId(), name, details: m ? line.slice(m.index + m[0].length).trim().slice(0, 500) : "", category: "" });
        if (out.length >= limit) break;
    }
    return out;
}

function validName(name) {
    return /^[a-zA-Z0-9çÇğĞıİöÖşŞüÜ\s]{1,15}$/.test(name);
}

// ---------- Create a room ----------
(async function enableCreateWhenSignedIn() {
    // initAuth (app.js) signs in anonymously; enable the button as soon as there is a session
    for (let i = 0; i < 40; i++) {
        const { data: { session } } = await sb.auth.getSession();
        if (session) { btnCreateRoom.disabled = false; return; }
        await new Promise(r => setTimeout(r, 250));
    }
})();

btnCreateRoom.addEventListener("click", async () => {
    userName = inputName.value.trim();
    if (!userName) { setFieldError(inputName, "Lütfen adını yaz."); return; }
    if (!validName(userName)) {
        setFieldError(inputName, "Sadece harf, rakam ve boşluk kullan (en fazla 15 karakter).");
        return;
    }
    localStorage.setItem("napsak_user_name", userName);
    const label = btnCreateRoom.innerHTML;
    btnCreateRoom.disabled = true;
    btnCreateRoom.innerHTML = 'Oluşturuluyor... <i class="fa-solid fa-spinner fa-spin"></i>';
    const { data, error } = await sb.rpc("create_room", { p_name: userName });
    if (error || !data) {
        console.error("create_room failed:", error);
        alert("Oda oluşturulamadı. Bağlantını kontrol edip tekrar dene.");
        btnCreateRoom.disabled = false;
        btnCreateRoom.innerHTML = label;
        return;
    }
    roomId = data;
    inputRoom.value = data;
    try {
        window.history.replaceState({}, "", window.location.pathname + "?room=" + roomId);
    } catch (e) { /* ignore */ }
    observeRoom();
});

// ---------- Lobby (host view) ----------
let lastQrLink = "";
function roomLink() {
    return window.location.origin + window.location.pathname + "?room=" + roomId;
}

function renderHostLobby() {
    const host = isHost();
    document.getElementById("host-lobby-tools").classList.toggle("hidden", !host);
    document.getElementById("suggest-card").classList.toggle("hidden", host);
    btnReady.classList.toggle("hidden", host);
    const waitingMsg = document.querySelector("#screen-lobby .waiting-message");
    if (waitingMsg) waitingMsg.classList.toggle("hidden", host);
    if (!host) return;

    const link = roomLink();
    document.getElementById("host-link").textContent = link;
    if (link !== lastQrLink && typeof qrcode === "function") {
        lastQrLink = link;
        const qr = qrcode(0, "M");
        qr.addData(link);
        qr.make();
        document.getElementById("host-qr").innerHTML = qr.createSvgTag({ cellSize: 4, margin: 0, scalable: true });
    }
    const everyoneReady = Object.values(roomData.participants || {}).every(p => p.ready);
    document.getElementById("btn-host-setup").disabled = !everyoneReady;
    document.getElementById("host-ready-hint").classList.toggle("hidden", everyoneReady);
    const n = (roomData.suggestions || []).length;
    document.getElementById("host-suggestion-count").textContent = n ? n + " öneri geldi, sonraki ekranda görebilirsin." : "";
}

document.getElementById("btn-copy-link").addEventListener("click", async () => {
    const link = roomLink();
    try {
        if (navigator.share) await navigator.share({ title: "N’APSAK?", text: "Odamıza katıl!", url: link });
        else { await navigator.clipboard.writeText(link); alert("Bağlantı kopyalandı!"); }
    } catch (e) { /* cancelled */ }
});

document.getElementById("btn-host-setup").addEventListener("click", () => {
    hostSetupOpen = true;
    showScreen(screenSetup);
    renderSetup();
});
document.getElementById("btn-setup-back").addEventListener("click", () => {
    hostSetupOpen = false;
    showScreen(screenLobby);
    renderHostLobby();
});

// ---------- Setup screen ----------
function addChoices(list) {
    const have = new Set(setupChoices.map(c => c.name.toLowerCase()));
    for (const c of list) {
        if (setupChoices.length >= 50) break;
        if (have.has(c.name.toLowerCase())) continue;
        have.add(c.name.toLowerCase());
        setupChoices.push(c);
    }
}

function renderSetup() {
    // templates (built once)
    const tpl = document.getElementById("setup-templates");
    if (!tpl.children.length) {
        Object.keys(PRESETS).forEach(cat => {
            const b = document.createElement("button");
            b.type = "button";
            b.className = "chip";
            b.textContent = cat;
            b.addEventListener("click", () => {
                addChoices(PRESETS[cat].map(([name, details]) => ({ id: newId(), name, details, category: cat })));
                renderSetup();
            });
            tpl.appendChild(b);
        });
    }

    // vote mode chips
    const modes = document.getElementById("setup-modes");
    modes.innerHTML = "";
    MODES.forEach(m => {
        const b = document.createElement("button");
        b.type = "button";
        b.className = "chip" + (m.id === setupMode ? " selected" : "");
        b.setAttribute("role", "radio");
        b.setAttribute("aria-checked", String(m.id === setupMode));
        b.textContent = m.label;
        b.addEventListener("click", () => { setupMode = m.id; renderSetup(); });
        modes.appendChild(b);
    });
    document.getElementById("setup-mode-hint").textContent = MODES.find(m => m.id === setupMode).hint;

    // chosen options
    const list = document.getElementById("setup-list");
    list.innerHTML = "";
    setupChoices.forEach(c => {
        const row = document.createElement("div");
        row.className = "setup-item";
        const emoji = document.createElement("span");
        emoji.className = "lv-emoji";
        emoji.textContent = emojiFor(c);
        emoji.setAttribute("aria-hidden", "true");
        const text = document.createElement("div");
        text.className = "lv-text";
        const name = document.createElement("strong");
        name.textContent = c.name;
        const details = document.createElement("small");
        details.textContent = c.details || "";
        text.append(name, details);
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "icon-btn danger";
        remove.setAttribute("aria-label", c.name + " seçeneğini kaldır");
        remove.innerHTML = '<i class="fa-solid fa-xmark"></i>';
        remove.addEventListener("click", () => { setupChoices = setupChoices.filter(x => x.id !== c.id); renderSetup(); });
        row.append(emoji, text, remove);
        list.appendChild(row);
    });
    document.getElementById("setup-count").textContent = String(setupChoices.length);
    document.getElementById("btn-setup-start").disabled = setupChoices.length < 2;

    // suggestions from players
    const suggestions = (roomData && roomData.suggestions) || [];
    const box = document.getElementById("setup-suggestions");
    const slist = document.getElementById("setup-suggestions-list");
    box.classList.toggle("hidden", suggestions.length === 0);
    slist.innerHTML = "";
    suggestions.forEach(sug => {
        const row = document.createElement("div");
        row.className = "setup-item";
        const text = document.createElement("div");
        text.className = "lv-text";
        const name = document.createElement("strong");
        name.textContent = sug.name;
        const details = document.createElement("small");
        details.textContent = sug.details || "";
        text.append(name, details);
        const accept = document.createElement("button");
        accept.type = "button";
        accept.className = "icon-btn ok";
        accept.setAttribute("aria-label", sug.name + " önerisini listeye ekle");
        accept.innerHTML = '<i class="fa-solid fa-check"></i>';
        accept.addEventListener("click", () => {
            addChoices([{ id: newId(), name: sug.name, details: sug.details || "", category: "" }]);
            dismissSuggestion(sug.id);
        });
        const dismiss = document.createElement("button");
        dismiss.type = "button";
        dismiss.className = "icon-btn danger";
        dismiss.setAttribute("aria-label", sug.name + " önerisini reddet");
        dismiss.innerHTML = '<i class="fa-solid fa-xmark"></i>';
        dismiss.addEventListener("click", () => dismissSuggestion(sug.id));
        row.append(text, accept, dismiss);
        slist.appendChild(row);
    });
}

function dismissSuggestion(id) {
    sb.rpc("dismiss_suggestion", { p_room: roomId, p_id: id }).then(() => refreshRoom());
    renderSetup();
}

document.getElementById("btn-setup-add").addEventListener("click", () => {
    const area = document.getElementById("setup-bulk");
    addChoices(parseQuickChoices(area.value));
    area.value = "";
    renderSetup();
});

document.getElementById("btn-setup-start").addEventListener("click", () => {
    if (setupChoices.length < 2) return;
    const button = document.getElementById("btn-setup-start");
    button.disabled = true;
    sb.rpc("start_voting", {
        p_room: roomId,
        p_mode: setupMode,
        p_choices: setupChoices.map(c => ({ id: c.id, name: c.name, details: c.details, category: c.category }))
    }).then(({ error }) => {
        if (error) {
            console.error("start_voting failed:", error);
            alert("Oylama başlatılamadı. Bağlantını kontrol edip tekrar dene.");
            button.disabled = false;
            return;
        }
        hostSetupOpen = false;
        refreshRoom();
    });
});

// ---------- Voting: finish, and who is still missing ----------
// When everyone has voted the host closes voting after the short countdown everyone sees.
function hostFinishVoting() {
    if (!isHost() || hostFinishScheduled) return;
    hostFinishScheduled = true;
    setTimeout(() => {
        sb.rpc("end_voting", { p_room: roomId, p_winner: null }).then(({ error }) => {
            if (error) { console.warn("end_voting failed:", error.message); hostFinishScheduled = false; }
            refreshRoom();
        });
    }, 3500);
}

function renderWaiting() {
    const names = document.getElementById("waiting-names");
    const prompt = document.getElementById("host-wait-prompt");
    if (!roomData || roomData.state !== "VOTING" || !votesSubmitted) {
        names.textContent = "";
        prompt.classList.add("hidden");
        waitSince = 0;
        return;
    }
    const waiting = Object.values(roomData.participants || {}).filter(p => !p.hasVoted).map(p => p.name);
    names.textContent = waiting.length ? "Bekleniyor: " + waiting.join(", ") : "";
    if (!waiting.length) {
        prompt.classList.add("hidden");
        waitSince = 0;
        return;
    }
    if (!waitSince) waitSince = Date.now();
    updateHostPrompt();
}

function updateHostPrompt() {
    const prompt = document.getElementById("host-wait-prompt");
    const show = isHost() && waitSince > 0 && Date.now() - waitSince >= WAIT_PROMPT_MS && Date.now() >= waitSnoozeUntil;
    prompt.classList.toggle("hidden", !show);
}
setInterval(updateHostPrompt, 1000);

document.getElementById("btn-wait-more").addEventListener("click", () => {
    waitSnoozeUntil = Date.now() + WAIT_PROMPT_MS;
    updateHostPrompt();
});
document.getElementById("btn-finish-now").addEventListener("click", () => {
    sb.rpc("end_voting", { p_room: roomId, p_winner: null }).then(() => refreshRoom());
});

// ---------- Resume after a reload (host, or a player who was not removed) ----------
(async function resumeRoom() {
    if (new URLSearchParams(window.location.search).get("room")) return; // an invite link wins
    let saved = null;
    try { saved = localStorage.getItem("napsak_room"); } catch (e) { /* ignore */ }
    if (!saved) return;
    for (let i = 0; i < 40; i++) {
        const { data: { session } } = await sb.auth.getSession();
        if (session) { userId = session.user.id; break; }
        await new Promise(r => setTimeout(r, 250));
    }
    roomId = saved;
    try {
        const data = await fetchRoomData();
        if (data && data.participants && data.participants[userId]) {
            observeRoom();
        } else {
            roomId = "";
            localStorage.removeItem("napsak_room");
        }
    } catch (e) {
        roomId = "";
    }
})();
