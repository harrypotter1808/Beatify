// App State
let currentUser = null;
let currentAudio = new Audio();
let songsCache = [];
let selectedSongForModal = null;
const apiBase = '/api';

// Utility API Fetch wrapper
async function apiRequest(endpoint, method = 'GET', body = null) {
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (body) options.body = JSON.stringify(body);
    
    const response = await fetch(apiBase + endpoint, options);
    if (!response.ok) {
        let errMessage = 'Request failed';
        try {
            const errData = await response.json();
            errMessage = errData.error || errMessage;
        } catch(e) {
            errMessage = await response.text();
        }
        throw new Error(errMessage);
    }
    return response.json();
}

// Navigation & Tab Switching
function switchAuthTab(tab) {
    document.querySelectorAll('.auth-tab').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.auth-form-wrapper').forEach(form => form.classList.remove('active'));
    document.getElementById('auth-msg').innerText = '';

    if (tab === 'login') {
        document.getElementById('tab-login-btn').classList.add('active');
        document.getElementById('login-form-wrapper').classList.add('active');
    } else if (tab === 'register') {
        document.getElementById('tab-register-btn').classList.add('active');
        document.getElementById('register-form-wrapper').classList.add('active');
    }
}

function switchToAdminLogin() {
    document.querySelectorAll('.auth-tab').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.auth-form-wrapper').forEach(form => form.classList.remove('active'));
    document.getElementById('admin-form-wrapper').classList.add('active');
    document.getElementById('auth-msg').innerText = '';
}

function switchSection(section) {
    // Update active nav link
    document.querySelectorAll('.sidebar-nav li').forEach(li => li.classList.remove('active'));
    document.querySelectorAll('.content-section').forEach(sec => sec.classList.remove('active'));

    document.getElementById(`nav-${section}`).classList.add('active');
    document.getElementById(`${section}-section`).classList.add('active');

    // Section specific loads
    if (section === 'library') {
        loadLibrary();
    } else if (section === 'playlists') {
        loadPlaylists();
    } else if (section === 'admin') {
        loadAdminCatalog();
    }
}

// Authentication Handlers
async function login() {
    const u = document.getElementById('username').value.trim();
    const p = document.getElementById('password').value.trim();
    const msg = document.getElementById('auth-msg');

    if (!u || !p) {
        msg.style.color = '#ef4444';
        msg.innerText = 'Username and password are required';
        return;
    }

    try {
        currentUser = await apiRequest('/login', 'POST', { username: u, password: p });
        currentUser.password = p; // cached locally for seamless updates
        enterDashboard();
    } catch (err) {
        msg.style.color = '#ef4444';
        msg.innerText = err.message || 'Login failed';
    }
}

async function register() {
    const u = document.getElementById('reg-username').value.trim();
    const e = document.getElementById('reg-email').value.trim();
    const p = document.getElementById('reg-password').value.trim();
    const m = document.getElementById('reg-mobile').value.trim();
    const l = document.getElementById('reg-location').value.trim();
    const msg = document.getElementById('auth-msg');

    if (!u || !p) {
        msg.style.color = '#ef4444';
        msg.innerText = 'Username and password are required';
        return;
    }

    try {
        currentUser = await apiRequest('/register', 'POST', {
            username: u,
            email: e,
            password: p,
            mobileNo: m,
            location: l
        });
        currentUser.password = p;
        msg.style.color = '#1db954';
        msg.innerText = 'Account created successfully!';
        setTimeout(enterDashboard, 800);
    } catch (err) {
        msg.style.color = '#ef4444';
        msg.innerText = err.message || 'Registration failed';
    }
}

async function adminLogin() {
    const u = document.getElementById('admin-username').value.trim();
    const p = document.getElementById('admin-password').value.trim();
    const msg = document.getElementById('auth-msg');

    if (!u || !p) {
        msg.style.color = '#ef4444';
        msg.innerText = 'Username and password are required';
        return;
    }

    try {
        const response = await apiRequest('/admin/login', 'POST', { username: u, password: p });
        currentUser = {
            username: 'Administrator',
            role: 'admin',
            playlists: {},
            location: 'Secure Server'
        };
        enterDashboard();
    } catch (err) {
        msg.style.color = '#ef4444';
        msg.innerText = err.message || 'Admin authentication failed';
    }
}

function logout() {
    currentUser = null;
    currentAudio.pause();
    currentAudio = new Audio();
    
    // Clear Forms
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    document.getElementById('reg-username').value = '';
    document.getElementById('reg-email').value = '';
    document.getElementById('reg-password').value = '';
    document.getElementById('reg-mobile').value = '';
    document.getElementById('reg-location').value = '';
    document.getElementById('admin-username').value = '';
    document.getElementById('admin-password').value = '';
    document.getElementById('auth-msg').innerText = '';

    // Switch views
    document.getElementById('main-view').classList.remove('active');
    document.getElementById('auth-view').classList.add('active');
    document.getElementById('player').classList.add('hidden');
}

function enterDashboard() {
    document.getElementById('auth-view').classList.remove('active');
    document.getElementById('main-view').classList.add('active');
    
    // Set Profile Displays
    document.getElementById('user-display').innerText = currentUser.username;
    
    const badge = document.getElementById('user-badge');
    const loc = document.getElementById('user-meta');
    const adminNav = document.getElementById('nav-admin');
    
    if (currentUser.role === 'admin') {
        badge.innerText = 'Spotify Admin';
        badge.className = 'admin-badge';
        loc.innerText = '📍 Secure Console';
        adminNav.style.display = 'flex';
        switchSection('admin');
    } else {
        badge.innerText = 'Premium User';
        badge.className = '';
        loc.innerText = currentUser.location ? `📍 ${currentUser.location}` : '📍 Global User';
        adminNav.style.display = 'none';
        switchSection('library');
    }
}

// Library Management
async function loadLibrary() {
    try {
        songsCache = await apiRequest('/library');
        renderSongs(songsCache);
    } catch (err) {
        console.error('Error loading library:', err);
    }
}

function renderSongs(songs) {
    const list = document.getElementById('library-list');
    list.innerHTML = '';
    
    if (songs.length === 0) {
        list.innerHTML = '<p class="section-desc" style="grid-column: 1/-1; text-align: center;">No tracks found matching criteria.</p>';
        return;
    }

    songs.forEach(song => {
        const minutes = Math.floor(song.durationInSeconds / 60);
        const seconds = String(song.durationInSeconds % 60).padStart(2, '0');
        const durationFormatted = `${minutes}:${seconds}`;

        const card = document.createElement('div');
        card.className = 'song-card';
        card.innerHTML = `
            <div class="song-card-art">🎵</div>
            <div class="song-card-info">
                <h4>${song.title}</h4>
                <p>${song.artist} • ${song.album}</p>
                <span class="song-card-badge">${song.genre}</span>
            </div>
            <div class="song-card-actions">
                <span style="font-size: 0.8rem; color: var(--text-gray); margin-right: 8px;">${durationFormatted}</span>
                <button class="action-btn" title="Play Now" onclick="event.stopPropagation(); playSong('${song.title.replace(/'/g, "\\'")}', '${song.artist.replace(/'/g, "\\'")}', '${durationFormatted}', '${song.genre}', '${song.filePath}')">▶</button>
                ${currentUser.role !== 'admin' ? `<button class="action-btn" title="Add to Playlist" onclick="event.stopPropagation(); openPlaylistModal('${song.title.replace(/'/g, "\\'")}')">+</button>` : ''}
            </div>
        `;
        // Double click card to play
        card.addEventListener('dblclick', () => {
            playSong(song.title, song.artist, durationFormatted, song.genre, song.filePath);
        });
        list.appendChild(card);
    });
}

function handleSearch() {
    const query = document.getElementById('search-input').value.toLowerCase().trim();
    const durationLimit = parseInt(document.getElementById('duration-slider').value);
    
    const filtered = songsCache.filter(song => {
        const matchesQuery = song.title.toLowerCase().includes(query) ||
                             song.artist.toLowerCase().includes(query) ||
                             song.genre.toLowerCase().includes(query) ||
                             song.album.toLowerCase().includes(query);
        const matchesDuration = song.durationInSeconds <= durationLimit;
        return matchesQuery && matchesDuration;
    });
    
    renderSongs(filtered);
}

function handleDurationFilter(val) {
    document.getElementById('duration-val').innerText = val;
    handleSearch();
}

// Playlist Management
async function loadPlaylists() {
    try {
        const playlists = await apiRequest(`/playlists?username=${currentUser.username}`);
        const container = document.getElementById('playlists-list');
        container.innerHTML = '';

        if (playlists.length === 0) {
            container.innerHTML = '<p class="section-desc" style="text-align: center;">No playlists created yet. Start by naming one above!</p>';
            return;
        }

        playlists.forEach(p => {
            const card = document.createElement('div');
            card.className = 'playlist-item-card glass-box';
            
            // Build tracks HTML
            let tracksHtml = '';
            if (p.songs.length === 0) {
                tracksHtml = '<p class="section-desc" style="font-style: italic; padding: 10px 0;">This playlist is empty. Add songs from the Library!</p>';
            } else {
                tracksHtml = p.songs.map((s, idx) => {
                    const min = Math.floor(s.durationInSeconds / 60);
                    const sec = String(s.durationInSeconds % 60).padStart(2, '0');
                    return `
                        <div class="track-row">
                            <span><strong>${idx + 1}. ${s.title}</strong> - ${s.artist}</span>
                            <div style="display:flex; align-items:center; gap: 8px;">
                                <span style="font-size: 0.8rem; color: var(--text-gray); margin-right: 5px;">${min}:${sec}</span>
                                <button class="action-btn" style="width: 26px; height: 26px;" onclick="playSong('${s.title.replace(/'/g, "\\'")}', '${s.artist.replace(/'/g, "\\'")}', '${min}:${sec}', '${s.genre}', '${s.filePath}')">▶</button>
                                <button class="action-btn" style="width: 26px; height: 26px; background: rgba(239, 68, 68, 0.1); color: #ef4444;" title="Remove track" onclick="removeSongFromPlaylist('${p.name.replace(/'/g, "\\'")}', '${s.title.replace(/'/g, "\\'")}')">×</button>
                            </div>
                        </div>
                    `;
                }).join('');
            }

            card.innerHTML = `
                <div class="playlist-header">
                    <div>
                        <h3>${p.name}</h3>
                        <p class="section-desc">${p.songs.length} song(s)</p>
                    </div>
                    <button class="logout-btn" style="width: auto; padding: 6px 14px;" onclick="deletePlaylist('${p.name.replace(/'/g, "\\'")}')">Delete Playlist</button>
                </div>
                <div class="playlist-tracks">
                    ${tracksHtml}
                </div>
            `;
            container.appendChild(card);
        });
    } catch (err) {
        console.error('Error loading playlists:', err);
    }
}

async function createPlaylist() {
    const input = document.getElementById('new-playlist-name');
    const name = input.value.trim();
    if (!name) return;

    try {
        await apiRequest('/playlists', 'POST', { username: currentUser.username, name });
        input.value = '';
        loadPlaylists();
    } catch (err) {
        alert(err.message || 'Error creating playlist');
    }
}

// Modal management
async function openPlaylistModal(songTitle) {
    selectedSongForModal = songTitle;
    document.getElementById('modal-song-title').innerText = songTitle;
    
    try {
        const playlists = await apiRequest(`/playlists?username=${currentUser.username}`);
        const listDiv = document.getElementById('modal-playlists-list');
        listDiv.innerHTML = '';

        if (playlists.length === 0) {
            listDiv.innerHTML = '<p class="section-desc" style="text-align: center;">No playlists. Create one in the Playlists section!</p>';
        } else {
            playlists.forEach(p => {
                const item = document.createElement('div');
                item.className = 'modal-list-item';
                item.innerText = p.name;
                item.onclick = () => selectPlaylistForAdd(p.name);
                listDiv.appendChild(item);
            });
        }
        
        document.getElementById('playlist-modal').style.display = 'flex';
    } catch (err) {
        console.error('Error loading modal playlists:', err);
    }
}

function closePlaylistModal() {
    document.getElementById('playlist-modal').style.display = 'none';
    selectedSongForModal = null;
}

async function selectPlaylistForAdd(playlistName) {
    if (!selectedSongForModal) return;
    try {
        await apiRequest('/playlists/add', 'POST', {
            username: currentUser.username,
            playlistName,
            songTitle: selectedSongForModal
        });
        closePlaylistModal();
        alert(`Added "${selectedSongForModal}" to playlist "${playlistName}"`);
    } catch (err) {
        alert(err.message || 'Error adding song');
    }
}

async function removeSongFromPlaylist(playlistName, songTitle) {
    try {
        await apiRequest('/playlists/remove', 'POST', {
            username: currentUser.username,
            playlistName,
            songTitle
        });
        loadPlaylists();
    } catch (err) {
        alert(err.message || 'Error removing song');
    }
}

async function deletePlaylist(playlistName) {
    if (!confirm(`Are you sure you want to delete playlist "${playlistName}"?`)) return;
    try {
        await apiRequest(`/playlists/${encodeURIComponent(playlistName)}?username=${currentUser.username}`, 'DELETE');
        loadPlaylists();
    } catch (err) {
        alert(err.message || 'Error deleting playlist');
    }
}

// Admin Operations
async function loadAdminCatalog() {
    try {
        const songs = await apiRequest('/library');
        const tbody = document.getElementById('admin-songs-tbody');
        tbody.innerHTML = '';

        songs.forEach(song => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${song.title}</strong></td>
                <td>${song.artist}</td>
                <td>${song.durationInSeconds}s</td>
                <td><span class="genre-tag">${song.genre}</span></td>
                <td>
                    <button class="accent-btn" style="padding: 4px 8px; font-size: 0.8rem; margin-right: 6px;" onclick="editAdminSong('${song.title.replace(/'/g, "\\'")}', '${song.artist.replace(/'/g, "\\'")}', ${song.durationInSeconds}, '${song.genre}')">Edit</button>
                    <button class="logout-btn" style="padding: 4px 8px; font-size: 0.8rem;" onclick="deleteAdminSong('${song.title.replace(/'/g, "\\'")}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('Error loading admin catalog:', err);
    }
}

async function submitAdminSong() {
    const titleInput = document.getElementById('admin-song-title');
    const artistInput = document.getElementById('admin-song-artist');
    const durInput = document.getElementById('admin-song-duration');
    const genreInput = document.getElementById('admin-song-genre');
    const isEdit = document.getElementById('admin-song-is-edit').value === 'true';

    const title = titleInput.value.trim();
    const artist = artistInput.value.trim();
    const duration = durInput.value.trim();
    const genre = genreInput.value.trim();

    if (!title || !artist || !duration || !genre) {
        alert('All fields are required');
        return;
    }

    try {
        if (isEdit) {
            // Edit mode calls PUT endpoint
            await apiRequest('/songs', 'PUT', { title, artist, duration, genre });
            alert(`Song "${title}" updated successfully!`);
        } else {
            // Add mode calls POST endpoint
            await apiRequest('/songs', 'POST', { title, artist, duration, genre });
            alert(`Song "${title}" added successfully!`);
        }
        resetAdminForm();
        loadAdminCatalog();
    } catch (err) {
        alert(err.message || 'Error processing request');
    }
}

function editAdminSong(title, artist, duration, genre) {
    document.getElementById('admin-song-title').value = title;
    document.getElementById('admin-song-title').disabled = true; // Key field can't be changed during edit
    document.getElementById('admin-song-artist').value = artist;
    document.getElementById('admin-song-duration').value = duration;
    document.getElementById('admin-song-genre').value = genre;
    
    document.getElementById('admin-song-is-edit').value = 'true';
    document.getElementById('form-action-title').innerText = 'Edit Song';
    document.getElementById('admin-submit-btn').innerText = 'Save Changes';
    document.getElementById('admin-cancel-btn').style.display = 'inline-block';
}

function resetAdminForm() {
    document.getElementById('admin-song-title').value = '';
    document.getElementById('admin-song-title').disabled = false;
    document.getElementById('admin-song-artist').value = '';
    document.getElementById('admin-song-duration').value = '';
    document.getElementById('admin-song-genre').value = '';
    
    document.getElementById('admin-song-is-edit').value = 'false';
    document.getElementById('form-action-title').innerText = 'Add New Song';
    document.getElementById('admin-submit-btn').innerText = 'Add Song';
    document.getElementById('admin-cancel-btn').style.display = 'none';
}

async function deleteAdminSong(title) {
    if (!confirm(`Are you sure you want to delete "${title}" from the catalog?`)) return;
    try {
        await apiRequest(`/songs/${encodeURIComponent(title)}`, 'DELETE');
        loadAdminCatalog();
    } catch (err) {
        alert(err.message || 'Error deleting song');
    }
}

// Media Player Logic
function playSong(title, artist, durationText, genre, filePath) {
    const player = document.getElementById('player');
    player.classList.remove('hidden');

    document.getElementById('playing-title').innerText = title;
    document.getElementById('playing-artist').innerText = artist;
    document.getElementById('playing-genre').innerText = genre;
    document.getElementById('playing-duration').innerText = durationText;

    // Check if loading a new file
    if (currentAudio.src && currentAudio.src.endsWith(filePath)) {
        if (currentAudio.paused) {
            currentAudio.play();
        }
    } else {
        currentAudio.pause();
        currentAudio = new Audio(filePath);
        currentAudio.volume = document.getElementById('volume-slider').value / 100;
        
        // Progress event listeners
        currentAudio.addEventListener('timeupdate', updateProgressBar);
        currentAudio.addEventListener('ended', () => {
            document.getElementById('play-pause').innerText = '▶';
            document.getElementById('visualizer').classList.remove('playing');
        });
        
        currentAudio.play().catch(e => console.log('Audio autoplay blocked, click play to start.'));
    }

    document.getElementById('play-pause').innerText = '⏸';
    document.getElementById('visualizer').classList.add('playing');
}

function togglePlay() {
    const btn = document.getElementById('play-pause');
    const vis = document.getElementById('visualizer');
    
    if (currentAudio.paused) {
        currentAudio.play();
        btn.innerText = '⏸';
        vis.classList.add('playing');
    } else {
        currentAudio.pause();
        btn.innerText = '▶';
        vis.classList.remove('playing');
    }
}

function seekAudio(percent) {
    if (!currentAudio.duration) return;
    const seekTo = (percent / 100) * currentAudio.duration;
    currentAudio.currentTime = seekTo;
    updateSeekFill(percent);
}

function updateProgressBar() {
    if (!currentAudio.duration) return;
    const progressPercent = (currentAudio.currentTime / currentAudio.duration) * 100;
    
    const seekInput = document.getElementById('player-seek');
    seekInput.value = progressPercent;
    updateSeekFill(progressPercent);

    // Format Current Time
    const min = Math.floor(currentAudio.currentTime / 60);
    const sec = String(Math.floor(currentAudio.currentTime % 60)).padStart(2, '0');
    document.getElementById('player-current-time').innerText = `${min}:${sec}`;
}

function updateSeekFill(percent) {
    document.getElementById('seek-fill').style.width = `${percent}%`;
}

function setVolume(val) {
    currentAudio.volume = val / 100;
    document.getElementById('volume-fill').style.width = `${val}%`;
}
