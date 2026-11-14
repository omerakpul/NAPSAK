// Supabase configuration (the URL and publishable key are public by design; access is enforced by RLS)
const SUPABASE_URL = "https://cynchfpgcoilogxkvigx.supabase.co";
const SUPABASE_KEY = "sb_publishable_TbD_9w3C5qOG8nSr-aAm9g_vWfEDBuX";
const sb = supabase.createClient(SUPABASE_URL, SUPABASE_KEY);

// App State
let roomId = "";
let userId = localStorage.getItem("napsak_user_id") || "";
let userName = localStorage.getItem("napsak_user_name") || "";
let isReady = false;
let roomData = null;
let choicesList = [];
let currentCardIndex = 0;
let likedChoiceIds = [];
let votesSubmitted = false;
let timerInterval = null;
let timeLeft = 0;

// DOM Elements
const screenJoin = document.getElementById("screen-join");
const screenLobby = document.getElementById("screen-lobby");
const screenVoting = document.getElementById("screen-voting");
const screenResult = document.getElementById("screen-result");

const inputName = document.getElementById("input-name");
const inputRoom = document.getElementById("input-room");
const btnJoin = document.getElementById("btn-join");

const lobbyRoomCode = document.getElementById("lobby-room-code");
const participantCount = document.getElementById("participant-count");
const readyRatio = document.getElementById("ready-ratio");
const participantsList = document.getElementById("participants-list");
const btnReady = document.getElementById("btn-ready");

const votingProgress = document.getElementById("voting-progress");
const progressFill = document.getElementById("progress-fill");
const cardContainer = document.getElementById("card-container");
const votingFinishedPlaceholder = document.getElementById("voting-finished-placeholder");
const votingActions = document.getElementById("voting-actions");
const btnLike = document.getElementById("btn-like");
const btnDislike = document.getElementById("btn-dislike");

const winnerImage = document.getElementById("winner-image");
const winnerVotes = document.getElementById("winner-votes");
const winnerName = document.getElementById("winner-name");
const winnerDetails = document.getElementById("winner-details");
const btnMaps = document.getElementById("btn-maps");
const btnReset = document.getElementById("btn-reset");
const winnerImageContainer = document.getElementById("winner-image-container");
const winnerInfo = document.querySelector(".winner-info");
const winnerCard = document.querySelector(".winner-card");
const resultBadge = document.getElementById("result-badge");
const resultTitle = document.getElementById("result-title");
let raffleCompleted = false;

// XSS Protection: Escape user-provided strings before inserting into HTML
function escapeHTML(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// On Load: Check URL parameters for Room ID
window.addEventListener("DOMContentLoaded", () => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomParam = urlParams.get("room") || urlParams.get("roomId");
    if (roomParam) {
        inputRoom.value = roomParam;
    }
    if (userName) {
        inputName.value = userName;
    }
});

// Supabase Anonymous Authentication
async function initAuth() {
    btnJoin.disabled = true;
    btnJoin.innerHTML = 'Bağlanılıyor... <i class="fa-solid fa-spinner fa-spin"></i>';
    try {
        let { data: { session } } = await sb.auth.getSession();
        if (!session) {
            const { data, error } = await sb.auth.signInAnonymously();
            if (error) throw error;
            session = data.session;
        }
        userId = session.user.id;
        localStorage.setItem("napsak_user_id", userId);
        btnJoin.disabled = false;
        btnJoin.innerHTML = 'Katıl <i class="fa-solid fa-right-to-bracket"></i>';
    } catch (error) {
        console.error("Anonymous auth failed:", error);
        alert("Sunucuya bağlanılamadı!");
    }
}
initAuth();

// Screen Navigation Utility
function showScreen(screenToShow) {
    document.querySelectorAll(".screen").forEach(screen => screen.classList.remove("active"));
    screenToShow.classList.add("active");
}

// Generate unique ID for the user
function generateUUID() {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return 'web-' + crypto.randomUUID();
    }
    return 'web-' + Math.random().toString(36).substr(2, 9) + '-' + Date.now().toString(36);
}

// Join Room Handler
btnJoin.addEventListener("click", () => {
    userName = inputName.value.trim();
    roomId = inputRoom.value.trim();

    if (!userName) {
        setFieldError(inputName, "Lütfen adını yaz.");
        return;
    }
    const nameRegex = /^[a-zA-Z0-9çÇğĞıİöÖşŞüÜ\s]{1,15}$/;
    if (!nameRegex.test(userName)) {
        setFieldError(inputName, "Sadece harf, rakam ve boşluk kullan (en fazla 15 karakter).");
        return;
    }
    if (!roomId || roomId.length !== 6) {
        setFieldError(inputRoom, "6 haneli oda kodunu yaz.");
        return;
    }

    localStorage.setItem("napsak_user_name", userName);
    btnJoin.disabled = true;
    btnJoin.innerHTML = 'Katılınıyor... <i class="fa-solid fa-spinner fa-spin"></i>';

    const resetJoinButton = () => {
        btnJoin.disabled = false;
        btnJoin.innerHTML = 'Katıl <i class="fa-solid fa-right-to-bracket"></i>';
    };

    sb.rpc("join_room", { p_room: roomId, p_name: userName }).then(({ error }) => {
        if (error) {
            console.error("Join room error:", error);
            const msg = (error.message || "").toLowerCase();
            if (msg.includes("room not found")) setFieldError(inputRoom, "Oda bulunamadı. Kodu kontrol et.");
            else if (msg.includes("voting already started")) setFieldError(inputRoom, "Oylama başlamış, odaya girilemiyor.");
            else if (msg.includes("room is full")) setFieldError(inputRoom, "Oda dolu.");
            else alert("Bağlantı hatası oluştu!");
            resetJoinButton();
            return;
        }
        // Update browser URL query parameter with room code
        try {
            const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname + `?room=${roomId}`;
            window.history.replaceState({ path: newUrl }, "", newUrl);
        } catch (err) {
            console.warn("history.replaceState failed:", err);
        }
        // Success: Start observing room in real-time
        observeRoom();
    });
});

// Toggle Ready Status
btnReady.addEventListener("click", () => {
    isReady = !isReady;
    
    // Update UI instantly for responsiveness
    if (isReady) {
        btnReady.classList.add("ready");
        btnReady.innerHTML = 'Hazırım! <i class="fa-solid fa-check-double"></i>';
    } else {
        btnReady.classList.remove("ready");
        btnReady.innerHTML = 'Hazır Durumuna Geç <i class="fa-solid fa-circle-check"></i>';
    }

    // supabase-js query builders are lazy: the request is only sent once .then() is attached
    sb.rpc("set_ready", { p_room: roomId, p_ready: isReady }).then(({ error }) => {
        if (error) console.warn("set_ready failed:", error.message);
    });
});

// Load the whole room in the shape the UI expects (participants/choices keyed by id)
async function fetchRoomData() {
    const [r, p, c, sg] = await Promise.all([
        sb.from("rooms").select("*").eq("id", roomId).maybeSingle(),
        sb.from("participants").select("*").eq("room_id", roomId),
        sb.from("choices").select("*").eq("room_id", roomId).order("position"),
        sb.from("suggestions").select("*").eq("room_id", roomId).order("created_at")
    ]);
    if (r.error) throw r.error;
    if (!r.data) return null;
    const participants = {};
    (p.data || []).forEach(x => {
        participants[x.user_id] = { id: x.user_id, name: x.name, ready: x.is_ready, isReady: x.is_ready, hasVoted: x.has_voted };
    });
    const choices = {};
    (c.data || []).forEach(x => {
        choices[x.id] = {
            id: x.id, name: x.name, details: x.details, imageUrl: x.image_url,
            voteCount: x.vote_count, latitude: x.latitude, longitude: x.longitude, category: x.category
        };
    });
    const suggestions = (sg.data || []).map(x => ({ id: x.id, name: x.name, details: x.details, userId: x.user_id }));
    return {
        id: r.data.id, hostId: r.data.host_id, state: r.data.state,
        winnerChoiceId: r.data.winner_choice_id, voteMode: r.data.vote_mode || "LIKE",
        participants, choices, suggestions
    };
}

let roomChannel = null;
let refreshing = false;
let refreshAgain = false;

async function refreshRoom() {
    if (refreshing) { refreshAgain = true; return; }
    refreshing = true;
    try {
        onRoomData(await fetchRoomData());
    } catch (err) {
        console.warn("Room refresh failed:", err);
    } finally {
        refreshing = false;
        if (refreshAgain) { refreshAgain = false; refreshRoom(); }
    }
}

// Observe Room updates in Realtime (events only trigger a re-read; a slow poll covers missed events)
function observeRoom() {
    try { localStorage.setItem("napsak_room", roomId); } catch (e) { /* storage unavailable */ }
    roomChannel = sb.channel("room-" + roomId);
    [["rooms", "id"], ["participants", "room_id"], ["choices", "room_id"], ["suggestions", "room_id"]].forEach(([table, col]) => {
        roomChannel.on("postgres_changes",
            { event: "*", schema: "public", table, filter: `${col}=eq.${roomId}` },
            () => refreshRoom());
    });
    roomChannel.subscribe(status => {
        if (status === "SUBSCRIBED") refreshRoom();
    });
    setInterval(refreshRoom, 15000);
}

// Leave the room when the page is closed so an absent player never blocks the vote
function leaveRoomOnExit() {
    if (!roomId || !roomData || roomData.state === "RESULT") return;
    if (typeof isHost === "function" && isHost()) return; // a host reload must not close the room
    sb.auth.getSession().then(({ data: { session } }) => {
        if (!session) return;
        fetch(SUPABASE_URL + "/rest/v1/rpc/leave_room", {
            method: "POST", keepalive: true,
            headers: { "apikey": SUPABASE_KEY, "Authorization": "Bearer " + session.access_token, "Content-Type": "application/json" },
            body: JSON.stringify({ p_room: roomId })
        });
    });
}
window.addEventListener("pagehide", leaveRoomOnExit);
window.addEventListener("pageshow", e => { if (e.persisted && roomId) window.location.reload(); });

function onRoomData(data) {
    roomData = data;
    if (!roomData) {
        try { localStorage.removeItem("napsak_room"); } catch (e) { /* ignore */ }
        alert("Oda kapatıldı veya bulunamadı.");
        window.location.href = window.location.pathname;
        return;
    }

    // Update real-time voter completion counts if in voting state
    if (roomData.participants) {
        const participants = Object.values(roomData.participants);
        const total = participants.length;
        const finishedCount = participants.filter(p => p.hasVoted).length;
        const countText = document.getElementById("web-finished-count");
        if (countText) {
            countText.textContent = `Tamamlanan: ${finishedCount} / ${total}`;
        }

        const allVoted = total > 0 && participants.every(p => p.hasVoted);
        if (allVoted && votesSubmitted && roomData.state === "VOTING") {
            startWebResultCountdown();
            if (typeof hostFinishVoting === "function") hostFinishVoting();
        }
    }
    if (typeof renderWaiting === "function") renderWaiting();

    // Check state transitions
    handleRoomStateChange();
}

// ---- Option emoji (default visual for options without a photo) ----
const EMOJI_KEYWORDS = [
    ["pizza", "🍕"], ["burger", "🍔"], ["hamburger", "🍔"], ["kebap", "🥙"], ["döner", "🥙"], ["sushi", "🍣"],
    ["makarna", "🍝"], ["kahve", "☕"], ["coffee", "☕"], ["starbucks", "☕"], ["çay", "🍵"], ["tatlı", "🍰"],
    ["dondurma", "🍦"], ["balık", "🐟"], ["salata", "🥗"], ["sinema", "🎬"], ["film", "🍿"], ["bowling", "🎳"],
    ["konser", "🎵"], ["müzik", "🎵"], ["tiyatro", "🎭"], ["karaoke", "🎤"], ["bilardo", "🎱"], ["dart", "🎯"],
    ["playstation", "🎮"], ["oyun", "🎮"], ["park", "🌳"], ["piknik", "🧺"], ["plaj", "🏖️"], ["deniz", "🌊"],
    ["dağ", "⛰️"], ["kamp", "⛺"], ["alışveriş", "🛍️"], ["futbol", "⚽"], ["basketbol", "🏀"], ["yüzme", "🏊"],
    ["kitap", "📚"], ["bar", "🍻"], ["bira", "🍻"], ["restoran", "🍽️"], ["ev", "🏠"], ["tatil", "✈️"]
];
const CATEGORY_EMOJI = { "yemek": "🍔", "aktivite": "🎬", "film": "🍿", "eğlence": "🎮", "kahve": "☕" };
const FALLBACK_EMOJI = ["🎯", "⭐", "🎲", "🍀", "🎁", "🔥", "🌈", "🏈"];

function emojiFor(choice) {
    const name = (choice.name || "").toLowerCase();
    const tokens = name.split(/[ \-/,.]+/);
    for (const [word, emoji] of EMOJI_KEYWORDS) {
        if (tokens.some(t => t.startsWith(word))) return emoji;
    }
    const byCategory = CATEGORY_EMOJI[(choice.category || "").toLowerCase()];
    if (byCategory) return byCategory;
    let hash = 0;
    for (const ch of name) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
    return FALLBACK_EMOJI[hash % FALLBACK_EMOJI.length];
}

function votingMode() {
    return (roomData && roomData.voteMode) || "LIKE";
}

// ---- Suggestions (lobby): propose an option to the host ----
const inputSuggest = document.getElementById("input-suggest");
const btnSuggest = document.getElementById("btn-suggest");
const mySuggestions = document.getElementById("my-suggestions");

btnSuggest.addEventListener("click", () => {
    const name = inputSuggest.value.trim();
    if (!name) return;
    btnSuggest.disabled = true;
    sb.rpc("suggest_choice", { p_room: roomId, p_name: name, p_details: "" }).then(({ error }) => {
        btnSuggest.disabled = false;
        if (error) {
            const msg = (error.message || "").toLowerCase();
            if (msg.includes("limit")) alert("En fazla 5 öneri gönderebilirsin.");
            else alert("Öneri gönderilemedi. Oylama başlamış olabilir.");
            return;
        }
        inputSuggest.value = "";
        refreshRoom();
    });
});

function renderMySuggestions() {
    const mine = (roomData.suggestions || []).filter(x => x.userId === userId);
    mySuggestions.innerHTML = "";
    mine.forEach(sug => {
        const row = document.createElement("div");
        row.className = "my-suggestion";
        const label = document.createElement("span");
        label.textContent = sug.name;
        const remove = document.createElement("button");
        remove.type = "button";
        remove.title = "Önerimi geri çek";
        remove.innerHTML = '<i class="fa-solid fa-xmark"></i>';
        remove.addEventListener("click", () => {
            sb.rpc("dismiss_suggestion", { p_room: roomId, p_id: sug.id }).then(() => refreshRoom());
        });
        row.append(label, remove);
        mySuggestions.appendChild(row);
    });
    btnSuggest.disabled = mine.length >= 5;
}

// Handle Room State Navigation
function handleRoomStateChange() {
    const state = roomData.state || "WAITING";

    if (state === "WAITING") {
        if (typeof hostSetupOpen !== "undefined" && hostSetupOpen && isHost()) {
            renderSetup();
            return;
        }
        showScreen(screenLobby);
        updateLobbyUI();
        renderMySuggestions();
        if (typeof renderHostLobby === "function") renderHostLobby();
    } else if (state === "VOTING") {
        if (!screenVoting.classList.contains("active")) {
            showScreen(screenVoting);
            initVotingScreen();
        } else {
            // Already on voting screen, just update the real-time voter completion counts
            updateVoterProgressUI();
        }
    } else if (state === "RESULT") {
        if (!votesSubmitted) {
            votesSubmitted = true;
            submitVotes();
        }
        showScreen(screenResult);
        showResultScreen();
    }
}

function updateVoterProgressUI() {
    if (roomData.participants) {
        const participants = Object.values(roomData.participants);
        const total = participants.length;
        const finishedCount = participants.filter(p => p.hasVoted).length;
        const countText = document.getElementById("web-finished-count");
        if (countText) {
            countText.textContent = `Tamamlanan: ${finishedCount} / ${total}`;
        }
    }
}

// Update Lobby UI
function updateLobbyUI() {
    lobbyRoomCode.textContent = "Oda: " + roomId;
    
    // Map participants
    const participants = [];
    if (roomData.participants) {
        Object.keys(roomData.participants).forEach(key => {
            const p = roomData.participants[key];
            // Safe fallback logic for properties
            const pReady = p.ready || p.isReady || false;
            participants.push({
                id: p.id,
                name: p.name,
                ready: pReady
            });
        });
    }

    // Update count
    participantCount.textContent = participants.length;
    const readyCount = participants.filter(p => p.ready).length;
    readyRatio.textContent = `Hazır: ${readyCount}/${participants.length}`;

    // Render list
    participantsList.innerHTML = "";
    participants.forEach(p => {
        const isSelf = p.id === userId;
        const item = document.createElement("div");
        item.className = `participant-item ${isSelf ? 'self' : ''} ${p.ready ? 'ready' : ''}`;
        
        const isHost = roomData.hostId === p.id;
        const escapedName = escapeHTML(p.name);
        const displayName = isSelf ? `${escapedName} (Sen)` : escapedName;
        
        item.innerHTML = `
            <div class="p-info">
                <div class="p-avatar">${escapedName.charAt(0).toUpperCase()}</div>
                <div class="p-name">${displayName}${isHost ? ' <i class="fa-solid fa-crown" style="color:#FFD700;font-size:12px;"></i>' : ''}</div>
            </div>
            <span class="p-status-badge ${p.ready ? 'ready' : 'waiting'}">
                ${p.ready ? 'Hazır' : 'Bekliyor'}
            </span>
        `;
        participantsList.appendChild(item);
    });
}

// Init Voting Screen
function initVotingScreen() {
    // Collect choices
    choicesList = [];
    if (roomData.choices) {
        Object.keys(roomData.choices).forEach(key => {
            choicesList.push(roomData.choices[key]);
        });
    }

    // LIKE: sort for a consistent card order across devices. SINGLE/RANK keep the host's order.
    if (votingMode() === "LIKE") {
        choicesList.sort((a, b) => a.id.localeCompare(b.id));
    }

    // Check if current user has already voted
    const currentUser = roomData.participants ? roomData.participants[userId] : null;
    const userAlreadyVoted = currentUser && (currentUser.hasVoted || currentUser.voted);

    if (userAlreadyVoted) {
        currentCardIndex = choicesList.length;
        votesSubmitted = true;
    }

    updateVotingProgress();

    // SINGLE / RANK: show a list instead of swipe cards (no countdown)
    if (votingMode() !== "LIKE" && currentCardIndex < choicesList.length) {
        initListVoting();
        return;
    }

    // Start timer interval if not already started
    if (!timerInterval && choicesList.length > 0) {
        timeLeft = choicesList.length * 5;
        const timerBadge = document.getElementById("voting-timer");
        timerBadge.textContent = "Kalan Süre: " + timeLeft + "s";
        timerBadge.classList.remove("warning");
        timerBadge.classList.remove("hidden");
        
        timerInterval = setInterval(() => {
            timeLeft--;
            timerBadge.textContent = "Kalan Süre: " + timeLeft + "s";
            
            if (timeLeft <= 5) {
                timerBadge.classList.add("warning");
            } else {
                timerBadge.classList.remove("warning");
            }
            
            if (timeLeft <= 0) {
                clearInterval(timerInterval);
                timerInterval = null;
                currentCardIndex = choicesList.length;
                updateVotingProgress();
                showVotingFinished();
            }
        }, 1000);
    }

    // If we've already swiped or finished
    if (currentCardIndex >= choicesList.length) {
        showVotingFinished();
        return;
    }

    // Render cards
    renderCardsStack();
}

const listVoting = document.getElementById("list-voting");
const listVotingItems = document.getElementById("list-voting-items");
const btnSubmitList = document.getElementById("btn-submit-list");

function initListVoting() {
    const mode = votingMode();
    const byId = {};
    choicesList.forEach(c => { byId[c.id] = c; });
    let order = choicesList.map(c => c.id);
    let selectedId = null;

    cardContainer.innerHTML = "";
    votingActions.classList.add("hidden");
    document.querySelector(".progress-bar-container").classList.add("hidden");
    document.getElementById("voting-timer").classList.add("hidden");
    listVoting.classList.remove("hidden");
    document.getElementById("list-voting-hint").textContent = mode === "SINGLE"
        ? "En çok istediğin seçeneği işaretle."
        : "Seçenekleri sırala: en iyi en üstte olsun.";
    btnSubmitList.textContent = mode === "SINGLE" ? "Oyumu Gönder" : "Sıralamayı Gönder";
    btnSubmitList.disabled = mode === "SINGLE";

    function render() {
        listVotingItems.innerHTML = "";
        order.forEach((id, index) => {
            const choice = byId[id];
            const row = document.createElement("div");
            row.className = "list-vote-item" + (mode === "SINGLE" && id === selectedId ? " selected" : "");

            if (mode === "RANK") {
                const badge = document.createElement("span");
                badge.className = "rank-badge";
                badge.textContent = String(index + 1);
                row.appendChild(badge);
            }
            const emoji = document.createElement("span");
            emoji.className = "lv-emoji";
            emoji.textContent = emojiFor(choice);
            const text = document.createElement("div");
            text.className = "lv-text";
            const title = document.createElement("strong");
            title.textContent = choice.name;
            const details = document.createElement("small");
            details.textContent = choice.details || "";
            text.append(title, details);
            row.append(emoji, text);

            if (mode === "SINGLE") {
                const radio = document.createElement("span");
                radio.className = "lv-radio";
                row.appendChild(radio);
                row.addEventListener("click", () => {
                    selectedId = id;
                    btnSubmitList.disabled = false;
                    render();
                });
            } else {
                const controls = document.createElement("div");
                controls.className = "rank-controls";
                const up = document.createElement("button");
                up.type = "button";
                up.innerHTML = '<i class="fa-solid fa-chevron-up"></i>';
                up.disabled = index === 0;
                up.addEventListener("click", () => { order.splice(index - 1, 0, order.splice(index, 1)[0]); render(); });
                const down = document.createElement("button");
                down.type = "button";
                down.innerHTML = '<i class="fa-solid fa-chevron-down"></i>';
                down.disabled = index === order.length - 1;
                down.addEventListener("click", () => { order.splice(index + 1, 0, order.splice(index, 1)[0]); render(); });
                controls.append(up, down);
                row.appendChild(controls);
            }
            listVotingItems.appendChild(row);
        });
    }
    render();

    btnSubmitList.onclick = () => {
        if (mode === "SINGLE") {
            if (!selectedId) return;
            likedChoiceIds = [selectedId];
        } else {
            likedChoiceIds = order.slice();
        }
        currentCardIndex = choicesList.length;
        listVoting.classList.add("hidden");
        showVotingFinished();
    };
}

function updateVotingProgress() {
    votingProgress.textContent = `${Math.min(currentCardIndex, choicesList.length)} / ${choicesList.length}`;
    const percent = choicesList.length > 0 ? (currentCardIndex / choicesList.length) * 100 : 0;
    progressFill.style.width = percent + "%";
}

// Render Tinder Card Stack (renders only the current card to match native Android layout)
function renderCardsStack() {
    cardContainer.innerHTML = "";
    
    if (currentCardIndex >= choicesList.length) {
        showVotingFinished();
        return;
    }
    
    const choice = choicesList[currentCardIndex];
    if (!choice) return;
    
    const card = document.createElement("div");
    card.dataset.index = currentCardIndex;
    card.dataset.id = choice.id;
    card.style.zIndex = 1;
    
    const hasImage = choice.imageUrl && choice.imageUrl.trim() !== "" && choice.imageUrl !== "null" && choice.imageUrl !== "undefined";
    const safeImageUrl = (hasImage && choice.imageUrl.startsWith('https://')) ? escapeHTML(choice.imageUrl) : '';
    const safeName = escapeHTML(choice.name);
    const safeDetails = escapeHTML(choice.details) || "Açıklama bulunmuyor.";
    
    if (hasImage && safeImageUrl) {
        card.className = "swipe-card";
        card.innerHTML = `
            <div class="card-badge like">EVET</div>
            <div class="card-badge dislike">HAYIR</div>
            <div class="card-img-container">
                <img src="${safeImageUrl}" alt="${safeName}" draggable="false">
            </div>
            <div class="card-info">
                ${choice.category ? `<span class="card-category-badge">${escapeHTML(choice.category).toUpperCase()}</span>` : ''}
                <h2>${safeName}</h2>
                <p>${safeDetails}</p>
            </div>
        `;
    } else {
        card.className = "swipe-card no-image";
        card.innerHTML = `
            <div class="card-badge like">EVET</div>
            <div class="card-badge dislike">HAYIR</div>
            <div class="card-info centered">
                ${choice.category ? `<span class="card-category-badge">${escapeHTML(choice.category).toUpperCase()}</span>` : ''}
                <h2>${safeName}</h2>
                <p>${safeDetails}</p>
            </div>
        `;
    }
    
    cardContainer.appendChild(card);
    initCardDrag(card);
}

// Tinder Swipe Physics (Drag interactions)
function initCardDrag(card) {
    let startX = 0;
    let startY = 0;
    let currentX = 0;
    let currentY = 0;
    let isDragging = false;
    
    const maxRotation = 16; // maximum rotation in degrees
    const swipeThreshold = 100; // swipe threshold in pixels

    // Pointer Events
    card.addEventListener("pointerdown", onPointerDown);
    card.addEventListener("pointercancel", onPointerCancel);
    
    // Touch Events (explicit mobile support)
    card.addEventListener("touchstart", onTouchStart, { passive: true });
    card.addEventListener("touchmove", onTouchMove, { passive: false });
    card.addEventListener("touchend", onTouchEnd);
    card.addEventListener("touchcancel", onTouchCancel);
    
    function onPointerDown(e) {
        if (e.button !== 0 && e.pointerType === 'mouse') return;
        
        startX = e.clientX;
        startY = e.clientY;
        isDragging = true;
        card.classList.add("dragging");
        
        document.addEventListener("pointermove", onPointerMove);
        document.addEventListener("pointerup", onPointerUp);
        document.addEventListener("pointercancel", onPointerCancel);
        
        try {
            card.setPointerCapture(e.pointerId);
        } catch (err) {
            console.warn("setPointerCapture failed:", err);
        }
    }
    
    function onPointerMove(e) {
        if (!isDragging) return;
        currentX = e.clientX - startX;
        currentY = e.clientY - startY;
        updateCardTransform(currentX, currentY);
    }
    
    function onPointerUp(e) {
        if (!isDragging) return;
        cleanupPointerDrag(e.pointerId);
        evaluateSwipe();
    }
    
    function onPointerCancel(e) {
        if (!isDragging) return;
        cleanupPointerDrag(e.pointerId);
        resetCardPosition();
    }
    
    function cleanupPointerDrag(pointerId) {
        isDragging = false;
        card.classList.remove("dragging");
        document.removeEventListener("pointermove", onPointerMove);
        document.removeEventListener("pointerup", onPointerUp);
        document.removeEventListener("pointercancel", onPointerCancel);
        try {
            card.releasePointerCapture(pointerId);
        } catch (err) {}
    }
    
    function onTouchStart(e) {
        if (e.touches.length > 1) return;
        const touch = e.touches[0];
        startX = touch.clientX;
        startY = touch.clientY;
        isDragging = true;
        card.classList.add("dragging");
    }
    
    function onTouchMove(e) {
        if (!isDragging) return;
        e.preventDefault(); // prevent scroll
        const touch = e.touches[0];
        currentX = touch.clientX - startX;
        currentY = touch.clientY - startY;
        updateCardTransform(currentX, currentY);
    }
    
    function onTouchEnd(e) {
        if (!isDragging) return;
        isDragging = false;
        card.classList.remove("dragging");
        evaluateSwipe();
    }
    
    function onTouchCancel(e) {
        if (!isDragging) return;
        isDragging = false;
        card.classList.remove("dragging");
        resetCardPosition();
    }
    
    function updateCardTransform(x, y) {
        const rot = (x / swipeThreshold) * maxRotation;
        const boundedRot = Math.min(Math.max(rot, -maxRotation), maxRotation);
        
        card.style.transform = `translate(${x}px, ${y}px) rotate(${boundedRot}deg)`;
        
        const likeBadge = card.querySelector(".card-badge.like");
        const dislikeBadge = card.querySelector(".card-badge.dislike");
        
        if (x > 15) {
            if (likeBadge) likeBadge.style.opacity = Math.min((x - 15) / 50, 1);
            if (dislikeBadge) dislikeBadge.style.opacity = 0;
            card.classList.add("swiping-right");
            card.classList.remove("swiping-left");
        } else if (x < -15) {
            if (dislikeBadge) dislikeBadge.style.opacity = Math.min((-x - 15) / 50, 1);
            if (likeBadge) likeBadge.style.opacity = 0;
            card.classList.add("swiping-left");
            card.classList.remove("swiping-right");
        } else {
            if (likeBadge) likeBadge.style.opacity = 0;
            if (dislikeBadge) dislikeBadge.style.opacity = 0;
            card.classList.remove("swiping-left", "swiping-right");
        }
    }
    
    function evaluateSwipe() {
        if (currentX > swipeThreshold) {
            swipeCardAction(card, "right", currentY);
        } else if (currentX < -swipeThreshold) {
            swipeCardAction(card, "left", currentY);
        } else {
            resetCardPosition();
        }
    }
    
    function resetCardPosition() {
        card.style.transform = "";
        const likeBadge = card.querySelector(".card-badge.like");
        const dislikeBadge = card.querySelector(".card-badge.dislike");
        if (likeBadge) likeBadge.style.opacity = 0;
        if (dislikeBadge) dislikeBadge.style.opacity = 0;
        card.classList.remove("swiping-left", "swiping-right");
        currentX = 0;
        currentY = 0;
    }
}

// Swipe animation and state update
function swipeCardAction(card, direction, yVal = 0) {
    const flyOutX = direction === "right" ? window.innerWidth : -window.innerWidth;
    card.style.transform = `translate(${flyOutX}px, ${yVal}px) rotate(${direction === "right" ? 30 : -30}deg)`;
    card.style.transition = "transform 0.4s ease-out";
    
    const choiceId = card.dataset.id;
    if (direction === "right") {
        likedChoiceIds.push(choiceId);
    }
    
    setTimeout(() => {
        currentCardIndex++;
        updateVotingProgress();
        
        if (currentCardIndex >= choicesList.length) {
            showVotingFinished();
        } else {
            renderCardsStack();
        }
    }, 200);
}

// Button click voting handlers
btnLike.addEventListener("click", () => {
    const card = cardContainer.querySelector(".swipe-card");
    if (card) {
        swipeCardAction(card, "right", 0);
    }
});

btnDislike.addEventListener("click", () => {
    const card = cardContainer.querySelector(".swipe-card");
    if (card) {
        swipeCardAction(card, "left", 0);
    }
});

// Submit user votes and display wait message
function showVotingFinished() {
    // Clear timer interval
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
    
    // Hide timer badge
    const timerBadge = document.getElementById("voting-timer");
    if (timerBadge) {
        timerBadge.classList.add("hidden");
    }

    cardContainer.innerHTML = "";
    votingActions.classList.add("hidden");
    listVoting.classList.add("hidden");
    votingFinishedPlaceholder.classList.remove("hidden");
    
    // Submit votes once
    if (!votesSubmitted) {
        votesSubmitted = true;
        submitVotes();
    }
}

function submitVotes(attempt = 1) {
    // One RPC: records this player's likes and marks them as voted atomically (server enforces one vote per player).
    // Network failures are retried with a growing delay; rule violations are not.
    const status = document.getElementById("vote-send-status");
    if (status && attempt > 1) status.textContent = "Oyların gönderiliyor, bağlantı bekleniyor…";
    sb.rpc("submit_votes", { p_room: roomId, p_choice_ids: likedChoiceIds }).then(({ error }) => {
        if (!error) { if (status) status.textContent = ""; return; }
        const msg = (error.message || "").toLowerCase();
        if (msg.includes("already voted")) { if (status) status.textContent = ""; return; }
        const permanent = ["not in voting", "not a participant", "invalid choice", "pick exactly", "duplicate"].some(k => msg.includes(k));
        if (permanent || attempt >= 30) {
            if (status) status.textContent = "Oylar gönderilemedi.";
            console.warn("submit_votes failed:", error.message);
            return;
        }
        setTimeout(() => submitVotes(attempt + 1), Math.min(2000 * attempt, 10000));
    });
}

let countdownActive = false;
function startWebResultCountdown() {
    if (countdownActive) return;
    countdownActive = true;
    
    let secondsLeft = 3;
    const textElement = document.getElementById("voting-finished-text");
    const loader = document.querySelector(".loader-ring");
    
    if (loader) loader.style.display = "none";
    
    // Set initial display
    if (textElement) {
        textElement.innerHTML = `Herkes oyladı! Sonuçlar açıklanıyor... <span style="font-size:28px; font-weight:900; color:var(--primary); display:block; margin-top:8px;">${secondsLeft}</span>`;
    }
    secondsLeft--;

    const interval = setInterval(() => {
        if (secondsLeft < 0) {
            clearInterval(interval);
            if (textElement) {
                textElement.innerHTML = `Sonuçlar yükleniyor... <i class="fa-solid fa-spinner fa-spin"></i>`;
            }
            return;
        }
        if (textElement) {
            textElement.innerHTML = `Herkes oyladı! Sonuçlar açıklanıyor... <span style="font-size:28px; font-weight:900; color:var(--primary); display:block; margin-top:8px;">${secondsLeft}</span>`;
        }
        secondsLeft--;
    }, 1000);
}

// Show Result Screen
function showResultScreen() {
    const winnerChoiceId = roomData.winnerChoiceId;
    if (!winnerChoiceId || !roomData.choices || !roomData.choices[winnerChoiceId]) {
        // Fallback if winner isn't resolved yet
        winnerName.textContent = "Karar belirleniyor...";
        winnerDetails.textContent = "Lütfen bekleyin.";
        btnMaps.classList.add("hidden");
        return;
    }

    const winner = roomData.choices[winnerChoiceId];
    
    // Check if there is a tie to show a raffle animation
    const choices = Object.values(roomData.choices || {});
    const maxVotes = choices.length > 0 ? Math.max(...choices.map(c => c.voteCount || 0)) : 0;
    const candidates = maxVotes > 0 
        ? choices.filter(c => c.voteCount === maxVotes)
        : choices;
    const isTie = candidates.length > 1;

    if (isTie && !raffleCompleted) {
        raffleCompleted = true;
        
        // Show raffle header state
        resultBadge.className = "badge warning";
        resultBadge.innerHTML = 'Kura Çekiliyor... <i class="fa-solid fa-dice"></i>';
        resultTitle.textContent = "Kura Başladı";
        
        // Hide actions during raffle
        btnMaps.classList.add("hidden");
        btnReset.classList.add("hidden");
        
        runWebRaffle(candidates, winner, (finalWinner) => {
            // Restore final winner headers
            resultBadge.className = "badge success";
            resultBadge.textContent = "Karar Verildi!";
            resultTitle.textContent = "Kazanan Seçenek";
            
            // Render final winner card details
            renderWinnerCard(finalWinner);
            
            // Set Map URL and show buttons
            btnMaps.classList.remove("hidden");
            const query = (finalWinner.latitude && finalWinner.longitude) 
                ? `${finalWinner.latitude},${finalWinner.longitude}` 
                : encodeURIComponent(finalWinner.name);
            btnMaps.href = `https://www.google.com/maps/search/?api=1&query=${query}`;
            btnReset.classList.remove("hidden");
            
            // Play celebration confetti!
            playConfetti();
        });
    } else {
        // No tie or raffle already completed: show immediately
        resultBadge.className = "badge success";
        resultBadge.textContent = "Karar Verildi!";
        resultTitle.textContent = "Kazanan Seçenek";
        btnReset.classList.remove("hidden");
        
        renderWinnerCard(winner);
        
        // Set Map URL
        btnMaps.classList.remove("hidden");
        const query = (winner.latitude && winner.longitude) 
            ? `${winner.latitude},${winner.longitude}` 
            : encodeURIComponent(winner.name);
        btnMaps.href = `https://www.google.com/maps/search/?api=1&query=${query}`;
        
        // Play celebration confetti only once if we didn't just run a raffle
        if (!isTie) {
            playConfetti();
        }
    }
}

function renderWinnerCard(winner) {
    // Fill winner UI details
    winnerName.textContent = winner.name;
    winnerDetails.textContent = winner.details || "Açıklama bulunmuyor.";
    winnerVotes.textContent = winner.voteCount || 0;
    document.getElementById("winner-unit").textContent = votingMode() === "RANK" ? "Puan" : "Oy";
    
    const hasImage = winner.imageUrl && winner.imageUrl.trim() !== "" && winner.imageUrl !== "null" && winner.imageUrl !== "undefined";
    
    if (hasImage && winner.imageUrl.startsWith('https://')) {
        winnerImageContainer.classList.remove("hidden");
        winnerCard.classList.remove("no-image");
        winnerInfo.classList.remove("centered");
        winnerImage.src = winner.imageUrl;
    } else {
        winnerImageContainer.classList.add("hidden");
        winnerCard.classList.add("no-image");
        winnerInfo.classList.add("centered");
        winnerImage.src = "";
    }
}

function runWebRaffle(candidates, winner, onComplete) {
    let step = 0;
    const delays = [50, 50, 60, 60, 70, 80, 90, 100, 120, 140, 170, 200, 240, 300, 380, 480, 600];
    let lastIndex = -1;

    function nextStep() {
        if (step >= delays.length) {
            onComplete(winner);
            return;
        }

        let nextIndex = Math.floor(Math.random() * candidates.length);
        if (candidates.length > 1) {
            while (nextIndex === lastIndex) {
                nextIndex = Math.floor(Math.random() * candidates.length);
            }
        }
        lastIndex = nextIndex;
        const tempChoice = candidates[nextIndex];
        
        // Render temporary choice details
        winnerName.textContent = tempChoice.name;
        winnerDetails.textContent = "Eşit oy alan seçenekler arasında kura çekiliyor...";
        winnerVotes.textContent = "?";
        
        const hasImage = tempChoice.imageUrl && tempChoice.imageUrl.trim() !== "" && tempChoice.imageUrl !== "null" && tempChoice.imageUrl !== "undefined";
        if (hasImage && tempChoice.imageUrl.startsWith('https://')) {
            winnerImageContainer.classList.remove("hidden");
            winnerCard.classList.remove("no-image");
            winnerInfo.classList.remove("centered");
            winnerImage.src = tempChoice.imageUrl;
        } else {
            winnerImageContainer.classList.add("hidden");
            winnerCard.classList.add("no-image");
            winnerInfo.classList.add("centered");
            winnerImage.src = "";
        }
        
        setTimeout(nextStep, delays[step]);
        step++;
    }

    nextStep();
}

function playConfetti() {
    // Create soft celebration confetti explosion
    const duration = 2.5 * 1000;
    const end = Date.now() + duration;

    (function frame() {
        confetti({
            particleCount: 3,
            angle: 60,
            spread: 55,
            origin: { x: 0 },
            colors: ['#FF6B6B', '#FF9F43', '#10AC84']
        });
        confetti({
            particleCount: 3,
            angle: 120,
            spread: 55,
            origin: { x: 1 },
            colors: ['#FF6B6B', '#FF9F43', '#10AC84']
        });

        if (Date.now() < end) {
            requestAnimationFrame(frame);
        }
    }());
}

// Back to Home
btnReset.addEventListener("click", async () => {
    if (typeof isHost === "function" && isHost()) {
        try { await sb.rpc("delete_room", { p_room: roomId }); } catch (e) { /* the room expires on its own */ }
    }
    try { localStorage.removeItem("napsak_room"); } catch (e) { /* ignore */ }
    window.location.href = window.location.pathname; // Reload without query params
});

// Error logging (console only in production)
window.addEventListener("error", (e) => {
    console.error("App error:", e.message, e.filename, e.lineno);
});
window.addEventListener("unhandledrejection", (e) => {
    console.error("Unhandled promise rejection:", e.reason);
});


// Share the result (native share sheet where available, clipboard otherwise)
document.getElementById("btn-share").addEventListener("click", async () => {
    const winner = roomData && roomData.choices ? roomData.choices[roomData.winnerChoiceId] : null;
    if (!winner) return;
    const url = window.location.origin + window.location.pathname;
    const text = `N’APSAK? ile karar verdik: ${winner.name} kazandı! ${emojiFor(winner)}`;
    if (navigator.share) {
        try { await navigator.share({ title: "N’APSAK?", text, url }); } catch (e) { /* cancelled */ }
    } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(text + " " + url);
        alert("Sonuç panoya kopyalandı!");
    }
});


// ---- Connection status ----
const netBanner = document.getElementById("net-banner");
function updateNetBanner() {
    netBanner.classList.toggle("hidden", navigator.onLine);
}
window.addEventListener("offline", updateNetBanner);
window.addEventListener("online", () => {
    updateNetBanner();
    if (roomId) refreshRoom(); // catch up on anything missed while offline
});
updateNetBanner();


// ---- Form fields: clear button, character counter, inline errors ----
function setFieldError(input, message) {
    const group = input.closest(".input-group");
    if (!group) { alert(message); return; }
    group.classList.add("has-error");
    const slot = group.querySelector(".field-error");
    if (slot) slot.textContent = message;
    input.setAttribute("aria-invalid", "true");
    input.focus();
}

function clearFieldError(input) {
    const group = input.closest(".input-group");
    if (!group || !group.classList.contains("has-error")) return;
    group.classList.remove("has-error");
    const slot = group.querySelector(".field-error");
    if (slot) slot.textContent = "";
    input.removeAttribute("aria-invalid");
}

function enhanceInputs() {
    document.querySelectorAll(".input-wrap").forEach(wrap => {
        const input = wrap.querySelector("input, textarea");
        if (!input) return;
        const clear = wrap.querySelector(".field-clear");
        const counter = wrap.parentElement.querySelector(".counter");
        const refresh = () => {
            if (clear) clear.classList.toggle("hidden", !(input.value && wrap.matches(":focus-within")));
            if (counter && input.maxLength > 0) counter.textContent = input.value.length + "/" + input.maxLength;
        };
        input.addEventListener("input", () => { clearFieldError(input); refresh(); });
        input.addEventListener("focus", refresh);
        input.addEventListener("blur", () => setTimeout(refresh, 150));
        if (clear) {
            clear.addEventListener("mousedown", e => e.preventDefault()); // keep the focus in the field
            clear.addEventListener("click", () => {
                input.value = "";
                input.dispatchEvent(new Event("input"));
                input.focus();
            });
        }
        refresh();
    });
}
document.addEventListener("DOMContentLoaded", enhanceInputs);
if (document.readyState !== "loading") enhanceInputs();
