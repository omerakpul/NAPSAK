// Firebase Configuration
const firebaseConfig = {
    apiKey: "AIzaSyAgnGXpzQ4ZvXIoasD3HjMf4Yq6zkb2t9U",
    authDomain: "napsak-official.firebaseapp.com",
    databaseURL: "https://napsak-official-default-rtdb.europe-west1.firebasedatabase.app",
    projectId: "napsak-official",
    storageBucket: "napsak-official.firebasestorage.app",
    messagingSenderId: "276047318651",
    appId: "1:276047318651:web:9c1a5d6e27b140cf8c42a2"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const database = firebase.database();

// App State
let roomId = "";
let userId = localStorage.getItem("napsak_user_id") || generateUUID();
localStorage.setItem("napsak_user_id", userId);

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

// Screen Navigation Utility
function showScreen(screenToShow) {
    [screenJoin, screenLobby, screenVoting, screenResult].forEach(screen => {
        screen.classList.remove("active");
    });
    screenToShow.classList.add("active");
}

// Generate unique ID for the user
function generateUUID() {
    return 'web-' + Math.random().toString(36).substr(2, 9) + '-' + Date.now().toString(36);
}

// Join Room Handler
btnJoin.addEventListener("click", () => {
    userName = inputName.value.trim();
    roomId = inputRoom.value.trim();

    if (!userName) {
        alert("Lütfen adınızı girin!");
        return;
    }
    if (!roomId || roomId.length !== 6) {
        alert("Lütfen 6 haneli oda kodunu girin!");
        return;
    }

    localStorage.setItem("napsak_user_name", userName);
    btnJoin.disabled = true;
    btnJoin.innerHTML = 'Katılınıyor... <i class="fa-solid fa-spinner fa-spin"></i>';

    // Verify room exists in Firebase
    const roomRef = database.ref("rooms/" + roomId);
    roomRef.once("value")
        .then(snapshot => {
            if (!snapshot.exists()) {
                alert("Oda bulunamadı! Lütfen kodu kontrol edin.");
                btnJoin.disabled = false;
                btnJoin.innerHTML = 'Katıl <i class="fa-solid fa-right-to-bracket"></i>';
                return;
            }
            
            // Add participant to Firebase
            const participantRef = database.ref("rooms/" + roomId + "/participants/" + userId);
            return participantRef.set({
                id: userId,
                name: userName,
                ready: false
            }).then(() => {
                // Success: Start observing room in real-time
                observeRoom();
            });
        })
        .catch(error => {
            console.error("Join room error:", error);
            alert("Bağlantı hatası oluştu!");
            btnJoin.disabled = false;
            btnJoin.innerHTML = 'Katıl <i class="fa-solid fa-right-to-bracket"></i>';
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

    // Write to Firebase
    database.ref("rooms/" + roomId + "/participants/" + userId + "/ready").set(isReady);
});

// Observe Room updates in Realtime
function observeRoom() {
    const roomRef = database.ref("rooms/" + roomId);
    roomRef.on("value", snapshot => {
        roomData = snapshot.val();
        if (!roomData) {
            alert("Oda kapatıldı veya bulunamadı.");
            window.location.reload();
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
        }

        // Check state transitions
        handleRoomStateChange();
    });
}

// Handle Room State Navigation
function handleRoomStateChange() {
    const state = roomData.state || "WAITING";

    if (state === "WAITING") {
        showScreen(screenLobby);
        updateLobbyUI();
    } else if (state === "VOTING") {
        showScreen(screenVoting);
        initVotingScreen();
    } else if (state === "RESULT") {
        showScreen(screenResult);
        showResultScreen();
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
        const displayName = isSelf ? `${p.name} (Sen)` : p.name;
        
        item.innerHTML = `
            <div class="p-info">
                <div class="p-avatar">${p.name.charAt(0).toUpperCase()}</div>
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

    // Sort choices to ensure consistency across devices
    choicesList.sort((a, b) => a.id.localeCompare(b.id));

    updateVotingProgress();

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

function updateVotingProgress() {
    votingProgress.textContent = `${Math.min(currentCardIndex, choicesList.length)} / ${choicesList.length}`;
    const percent = choicesList.length > 0 ? (currentCardIndex / choicesList.length) * 100 : 0;
    progressFill.style.width = percent + "%";
}

// Render Tinder Card Stack
function renderCardsStack() {
    cardContainer.innerHTML = "";
    
    // Render starting from current index
    for (let i = currentCardIndex; i < choicesList.length; i++) {
        const choice = choicesList[i];
        const card = document.createElement("div");
        card.className = "swipe-card";
        card.dataset.index = i;
        card.dataset.id = choice.id;
        
        // Stack index styling (visual hierarchy)
        card.style.zIndex = choicesList.length - i;
        
        const imageUrl = choice.imageUrl || "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop&q=60";
        
        card.innerHTML = `
            <div class="card-badge like">EVET</div>
            <div class="card-badge dislike">HAYIR</div>
            <div class="card-img-container">
                <img src="${imageUrl}" alt="${choice.name}" draggable="false">
            </div>
            <div class="card-info">
                <h2>${choice.name}</h2>
                <p>${choice.details || "Açıklama bulunmuyor."}</p>
            </div>
        `;
        
        cardContainer.appendChild(card);
        
        // Add Swipe Drag Physics only to the top card
        if (i === currentCardIndex) {
            initCardDrag(card);
        }
    }
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

    // Touch and pointer events support
    card.addEventListener("pointerdown", onPointerDown);
    
    function onPointerDown(e) {
        startX = e.clientX;
        startY = e.clientY;
        isDragging = true;
        card.classList.add("dragging");
        
        document.addEventListener("pointermove", onPointerMove);
        document.addEventListener("pointerup", onPointerUp);
        card.setPointerCapture(e.pointerId);
    }
    
    function onPointerMove(e) {
        if (!isDragging) return;
        
        currentX = e.clientX - startX;
        currentY = e.clientY - startY;
        
        const rot = (currentX / swipeThreshold) * maxRotation;
        const boundedRot = Math.min(Math.max(rot, -maxRotation), maxRotation);
        
        // Move card
        card.style.transform = `translate(${currentX}px, ${currentY}px) rotate(${boundedRot}deg)`;
        
        // Show badges
        const likeBadge = card.querySelector(".card-badge.like");
        const dislikeBadge = card.querySelector(".card-badge.dislike");
        
        if (currentX > 15) {
            likeBadge.style.opacity = Math.min((currentX - 15) / 50, 1);
            dislikeBadge.style.opacity = 0;
            card.classList.add("swiping-right");
            card.classList.remove("swiping-left");
        } else if (currentX < -15) {
            dislikeBadge.style.opacity = Math.min((-currentX - 15) / 50, 1);
            likeBadge.style.opacity = 0;
            card.classList.add("swiping-left");
            card.classList.remove("swiping-right");
        } else {
            likeBadge.style.opacity = 0;
            dislikeBadge.style.opacity = 0;
            card.classList.remove("swiping-left", "swiping-right");
        }
    }
    
    function onPointerUp(e) {
        if (!isDragging) return;
        isDragging = false;
        card.classList.remove("dragging");
        
        document.removeEventListener("pointermove", onPointerMove);
        document.removeEventListener("pointerup", onPointerUp);
        
        // Evaluate swipe
        if (currentX > swipeThreshold) {
            // Swipe Right (Like)
            swipeCardAction(card, "right", currentY);
        } else if (currentX < -swipeThreshold) {
            // Swipe Left (Dislike)
            swipeCardAction(card, "left", currentY);
        } else {
            // Reset position
            card.style.transform = "";
            card.querySelector(".card-badge.like").style.opacity = 0;
            card.querySelector(".card-badge.dislike").style.opacity = 0;
            card.classList.remove("swiping-left", "swiping-right");
        }
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
    votingFinishedPlaceholder.classList.remove("hidden");
    
    // Submit votes once to Firebase
    if (!votesSubmitted) {
        votesSubmitted = true;
        submitVotesToFirebase();
    }
}

function submitVotesToFirebase() {
    // Write hasVoted = true to Firebase for this participant
    database.ref("rooms/" + roomId + "/participants/" + userId + "/hasVoted").set(true);

    if (likedChoiceIds.length === 0) return;
    
    const choicesRef = database.ref("rooms/" + roomId + "/choices");
    
    likedChoiceIds.forEach(choiceId => {
        // Run database transaction to increment vote count atomically
        choicesRef.child(choiceId).child("voteCount").transaction(currentVotes => {
            return (currentVotes || 0) + 1;
        });
    });
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
    
    // Fill winner UI details
    winnerName.textContent = winner.name;
    winnerDetails.textContent = winner.details || "Açıklama bulunmuyor.";
    winnerVotes.textContent = winner.voteCount || 0;
    
    if (winner.imageUrl) {
        winnerImage.src = winner.imageUrl;
    } else {
        winnerImage.src = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop&q=60";
    }

    // Set Map URL
    btnMaps.classList.remove("hidden");
    const query = (winner.latitude && winner.longitude) 
        ? `${winner.latitude},${winner.longitude}` 
        : encodeURIComponent(winner.name);
    btnMaps.href = `https://www.google.com/maps/search/?api=1&query=${query}`;

    // Play celebration confetti!
    playConfetti();
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
btnReset.addEventListener("click", () => {
    window.location.href = window.location.pathname; // Reload without query params
});
