const API_URL = "https://bidx-auction-app-production.up.railway.app";
const WS_URL = window.location.protocol === "https:" 
    ? "wss://bidx-auction-app-production.up.railway.app" 
    : "ws://bidx-auction-app-production.up.railway.app";

let auctions = [];
let currentAuctionId = null;
let bidderName = localStorage.getItem("bidxBidderName");

if (!bidderName) {
    bidderName = prompt("Enter your name for bidding:");

    if (!bidderName || !bidderName.trim()) {
        bidderName = "Anonymous";
    }

    bidderName = bidderName.trim();

    localStorage.setItem(
        "bidxBidderName",
        bidderName
    );
}


// =========================
// LOAD AUCTIONS
// =========================

async function loadAuctions() {
    try {
        const response = await fetch(`${API_URL}/auctions`);

        if (!response.ok) {
            throw new Error("Failed to load auctions");
        }

        auctions = await response.json();
        renderAuctions();

    } catch (error) {
        console.error(error);

        const auctionSection = document.querySelector(".auction-section .container");

        if (auctionSection) {
            auctionSection.innerHTML += `
                <p style="margin-top:20px; color: #dc2626;">
                    Could not load live auctions from server.
                </p>
            `;
        }
    }
}


// =========================
// RENDER AUCTIONS
// =========================

function renderAuctions() {
    const container = document.querySelector(".auction-section .container");

    if (!container) {
        return;
    }

    const liveAuctions = auctions.filter(
        auction => auction.status === "ACTIVE"
    );

    const sectionTitle = container.querySelector(".section-title");

    container.innerHTML = "";

    if (sectionTitle) {
        container.appendChild(sectionTitle);
    }

    if (sectionTitle) {
        const countElement = sectionTitle.querySelector("span");

        if (countElement) {
            countElement.textContent = `${liveAuctions.length} auctions active`;
        }
    }

    if (liveAuctions.length === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.textContent = "No live auctions right now.";
        emptyMessage.style.marginTop = "20px";
        container.appendChild(emptyMessage);
        currentAuctionId = null;
        return;
    }

    liveAuctions.forEach(auction => {
        const card = createAuctionCard(auction);
        container.appendChild(card);
    });

    currentAuctionId = liveAuctions[0].id;
}


// =========================
// CREATE AUCTION CARD
// =========================

function createAuctionCard(auction) {
    const card = document.createElement("div");
    card.className = "auction-card";
    card.dataset.auctionId = auction.id;

    const minimumBid = auction.currentBid + 1;

    const image = auction.imagePath && auction.imagePath !== "null" && auction.imagePath !== ""
        ? auction.imagePath
        : "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=1000&q=80";

    card.innerHTML = `
        <div class="product">
            <div class="product-image">
                <img
                    src="${escapeAttribute(image)}"
                    alt="${escapeAttribute(auction.itemName)}"
                >
            </div>

            <p class="category">
                ${escapeHtml(
                    auction.category
                        ? auction.category.toUpperCase()
                        : "OTHER"
                )}
            </p>

            <h2>
                ${escapeHtml(auction.itemName)}
            </h2>

            <p class="description">
                ${escapeHtml(
                    auction.description || ""
                )}
            </p>
        </div>

        <div class="auction-details">
            <div class="status">
                <span class="status-dot"></span>
                Auction Live
            </div>

            <div class="timer-section">
                <p class="small-heading">
                    TIME REMAINING
                </p>

                <div class="timer">
                    <div>
                        <strong data-hours>00</strong>
                        <span>Hours</span>
                    </div>
                    <div>
                        <strong data-minutes>00</strong>
                        <span>Minutes</span>
                    </div>
                    <div>
                        <strong data-seconds>00</strong>
                        <span>Seconds</span>
                    </div>
                </div>
            </div>

            <div class="current-bid">
                <p class="small-heading">
                    CURRENT BID
                </p>

                <strong data-current-bid>
                    ₹${auction.currentBid.toLocaleString("en-IN")}
                </strong>

                <span data-bid-count>
                    Live auction
                </span>
            </div>

            <form class="bid-form">
                <label>
                    Enter your bid
                </label>

                <div class="bid-input">
                    <span>₹</span>

                    <input
                        type="number"
                        name="bid"
                        placeholder="Enter amount"
                        min="${minimumBid}"
                        required
                    >

                    <button type="submit">
                        Place Bid
                    </button>
                </div>

                <p class="hint">
                    Your bid must be higher than
                    ₹${auction.currentBid.toLocaleString("en-IN")}.
                </p>
            </form>
        </div>
    `;

    const bidForm = card.querySelector(".bid-form");
    const bidInput = card.querySelector('input[name="bid"]');
    const hint = card.querySelector(".hint");

    bidForm.addEventListener("submit", async function(event) {
        event.preventDefault();

        const amount = Number(bidInput.value);

        if (!Number.isFinite(amount) || amount <= 0) {
            hint.textContent = "Please enter a valid positive bid amount.";
            return;
        }

        const latestAuction = auctions.find(
            item => item.id === auction.id
        );

        if (!latestAuction) {
            return;
        }

        if (amount <= latestAuction.currentBid) {
            hint.textContent = `Bid must be greater than ₹${latestAuction.currentBid.toLocaleString("en-IN")}`;
            return;
        }

        const data = new URLSearchParams();
        data.append("auction-id", auction.id);
        data.append("bidder", bidderName);
        data.append("amount", amount);

        try {
            const response = await fetch(
                `${API_URL}/bid`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: data
                }
            );

            const result = await response.text();

            if (response.ok) {
                hint.textContent = "Bid accepted!";
                bidInput.value = "";
                await loadAuctions();
            } else {
                hint.textContent = result;
            }

        } catch (error) {
            console.error(error);
            hint.textContent = "Could not connect to the backend.";
        }
    });

    startAuctionTimer(card, auction);

    return card;
}


// =========================
// AUCTION TIMER
// =========================

function startAuctionTimer(card, auction) {
    let remainingSeconds = auction.durationHours * 60 * 60;

    const hoursElement = card.querySelector("[data-hours]");
    const minutesElement = card.querySelector("[data-minutes]");
    const secondsElement = card.querySelector("[data-seconds]");

    function updateTimer() {
        const hours = Math.floor(remainingSeconds / 3600);
        const minutes = Math.floor((remainingSeconds % 3600) / 60);
        const seconds = remainingSeconds % 60;

        if (hoursElement) hoursElement.textContent = String(hours).padStart(2, "0");
        if (minutesElement) minutesElement.textContent = String(minutes).padStart(2, "0");
        if (secondsElement) secondsElement.textContent = String(seconds).padStart(2, "0");
    }

    updateTimer();

    const interval = setInterval(async function() {
        remainingSeconds--;

        if (remainingSeconds <= 0) {
            remainingSeconds = 0;
            clearInterval(interval);
            await loadAuctions();
            return;
        }

        updateTimer();
    }, 1000);
}


// =========================
// WEBSOCKET STUB
// =========================

let socket;

function connectWebSocket() {
    try {
        socket = new WebSocket(WS_URL);

        socket.onopen = function() {
            console.log("Connected to BidX live bidding.");
        };

        socket.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                updateAuctionBid(data.auctionId, data.currentBid);
            } catch (error) {
                console.error("Invalid WebSocket message:", error);
            }
        };

        socket.onclose = function() {
            setTimeout(connectWebSocket, 5000);
        };

        socket.onerror = function() {
            // Suppress fallback error logs for MVP mock socket
        };
    } catch (e) {
        console.log("WebSocket connection skipped or unavailable.");
    }
}


// =========================
// UPDATE LIVE BID
// =========================

function updateAuctionBid(auctionId, currentBid) {
    const auction = auctions.find(item => item.id === auctionId);

    if (auction) {
        auction.currentBid = currentBid;
    }

    const card = document.querySelector(`.auction-card[data-auction-id="${auctionId}"]`);

    if (!card) {
        return;
    }

    const currentBidElement = card.querySelector("[data-current-bid]");
    const bidInput = card.querySelector('input[name="bid"]');
    const hint = card.querySelector(".hint");

    if (currentBidElement) {
        currentBidElement.textContent = `₹${currentBid.toLocaleString("en-IN")}`;
    }

    if (bidInput) {
        bidInput.min = currentBid + 1;
    }

    if (hint) {
        hint.textContent = `Your bid must be higher than ₹${currentBid.toLocaleString("en-IN")}.`;
    }
}


// =========================
// SELLER FORM & IMAGE CONVERSION
// =========================

let imageBase64 = "";

const imageInput = document.getElementById("item-image");
if (imageInput) {
    imageInput.addEventListener("change", function(e) {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = function(event) {
            imageBase64 = event.target.result;
            const previewDiv = document.getElementById("upload-preview");
            if (previewDiv) {
                previewDiv.innerHTML = `<img src="${imageBase64}" style="max-height: 80px; border-radius: 4px; object-fit: contain;">`;
            }
        };
        reader.readAsDataURL(file);
    });
}

const sellForm = document.querySelector(".sell-form");

if (sellForm) {
    sellForm.addEventListener("submit", async function(event) {
        event.preventDefault();

        const data = new URLSearchParams();
        data.append("item-name", document.getElementById("item-name")?.value || "");
        data.append("category", document.getElementById("category")?.value || "");
        data.append("condition", document.getElementById("condition")?.value || "");
        data.append("starting-price", document.getElementById("starting-price")?.value || "0");
        data.append("duration", document.getElementById("auction-duration")?.value || document.getElementById("duration")?.value || "6");
        data.append("description", document.getElementById("description")?.value || "");
        data.append("pickup-location", document.getElementById("pickup-location")?.value || "");
        data.append("upi", document.getElementById("upi-id")?.value || document.getElementById("upi")?.value || "");
        data.append("image-path", imageBase64 || "");

        try {
            const response = await fetch(
                `${API_URL}/auction`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: data
                }
            );

            const result = await response.text();
            alert(result);

            if (response.ok) {
                sellForm.reset();
                imageBase64 = "";
                const previewDiv = document.getElementById("upload-preview");
                if (previewDiv) {
                    previewDiv.innerHTML = `<strong>Upload item photo</strong><span>JPG, PNG or WEBP</span>`;
                }
                await loadAuctions();
            }

        } catch (error) {
            console.error(error);
            alert("Could not connect to the backend.");
        }
    });
}


// =========================
// HTML SAFETY
// =========================

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function escapeAttribute(value) {
    return escapeHtml(value);
}


// =========================
// START APPLICATION
// =========================

loadAuctions();
connectWebSocket();