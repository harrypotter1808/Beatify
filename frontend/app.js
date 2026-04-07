let currentUser = null;
const apiBase = '/api';

async function req(endpoint, method = 'GET', body = null) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(apiBase + endpoint, opts);
    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    if(!username || !password) return;
    try {
        currentUser = await req('/login', 'POST', { username, password });
        currentUser.password = password; // store locally to refresh data seamlessly if needed
        document.getElementById('auth-msg').innerText = '';
        enterMain();
    } catch (e) {
        document.getElementById('auth-msg').innerText = JSON.parse(e.message).error || 'Login failed';
    }
}

async function register() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    if(!username || !password) return;
    try {
        currentUser = await req('/register', 'POST', { username, password });
        currentUser.password = password;
        document.getElementById('auth-msg').style.color = '#1db954';
        document.getElementById('auth-msg').innerText = 'Registered successfully!';
        setTimeout(enterMain, 1000);
    } catch (e) {
        document.getElementById('auth-msg').style.color = '#ff5555';
        document.getElementById('auth-msg').innerText = JSON.parse(e.message).error || 'Registration failed';
    }
}

function logout() {
    currentUser = null;
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    document.getElementById('auth-msg').innerText = '';
    document.getElementById('main-view').classList.remove('active');
    document.getElementById('auth-view').classList.add('active');
    document.getElementById('player').classList.add('hidden');
}

function enterMain() {
    document.getElementById('auth-view').classList.remove('active');
    document.getElementById('main-view').classList.add('active');
    document.getElementById('user-display').innerText = `👋 Hi, ${currentUser.username}!`;
    showLibrary();
}

function showLibrary() {
    document.getElementById('playlists-section').classList.remove('active');
    document.getElementById('library-section').classList.add('active');
    loadLibrary();
}

function showPlaylists() {
    document.getElementById('library-section').classList.remove('active');
    document.getElementById('playlists-section').classList.add('active');
    loadPlaylists();
}

async function loadLibrary() {
    const songs = await req('/library');
    const container = document.getElementById('library-list');
    container.innerHTML = '';
    songs.forEach(song => {
        const dur = `${Math.floor(song.durationInSeconds/60)}:${String(song.durationInSeconds%60).padStart(2, '0')}`;
        // Create Dropdown for playlists if exist
        let selectHtml = '';
        if (currentUser.playlists && Object.keys(currentUser.playlists).length > 0) {
            selectHtml = `
                <select onchange="addSongToPlaylist('${song.title}', this.value); this.value=''" class="add-song-select">
                    <option value="">Add to Playlist...</option>
                    ${Object.keys(currentUser.playlists).map(p => `<option value="${p}">${p}</option>`).join('')}
                </select>
            `;
        }

        container.innerHTML += `
            <div class="song-card">
                <div class="song-info">
                    <h4>${song.title}</h4>
                    <p>${song.artist} • ${song.album}</p>
                </div>
                <div style="display:flex; align-items:center;">
                    <span style="color:#a0a0a0; margin-right: 20px; font-size:0.9em;">😽 ${dur}</span>
                    <button onclick="playSong('${song.title}', '${song.artist}', '${dur}')">▶ Play</button>
                    ${selectHtml}
                </div>
            </div>
        `;
    });
}

function playSong(title, artist, duration) {
    document.getElementById('player').classList.remove('hidden');
    document.getElementById('playing-title').innerText = title;
    document.getElementById('playing-artist').innerText = artist;
    document.getElementById('playing-duration').innerText = duration;
    document.getElementById('play-pause').innerText = '⏸';
}

function togglePlay() {
    const btn = document.getElementById('play-pause');
    btn.innerText = btn.innerText === '⏸' ? '▶' : '⏸';
}

async function loadPlaylists() {
    const playlists = await req('/playlists?username=' + currentUser.username);
    const container = document.getElementById('playlists-list');
    container.innerHTML = '';
    if(playlists.length === 0) {
        container.innerHTML = '<p style="color:#a0a0a0;">No playlists yet. Create one above!</p>';
        return;
    }
    
    playlists.forEach(p => {
        container.innerHTML += `
            <div class="song-card" style="flex-direction: column; align-items: flex-start;">
                <div style="display:flex; justify-content:space-between; width:100%; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 10px; margin-bottom: 15px;">
                    <h4 style="font-size: 1.3em;">💿 ${p.name}</h4>
                </div>
                <div style="width:100%; padding-left: 10px;">
                    ${p.songs.map(s => `
                        <div style="display:flex; justify-content:space-between; margin-bottom: 10px; background: rgba(0,0,0,0.1); padding: 8px; border-radius: 5px;">
                            <span><strong style="color:white;">${s.title}</strong> - <span style="color:#a0a0a0">${s.artist}</span></span>
                            <button style="padding: 5px 10px; background: rgba(255,255,255,0.1);" onclick="playSong('${s.title}', '${s.artist}', '0:00')">▶</button>
                        </div>
                    `).join('')}
                    ${p.songs.length === 0 ? '<p style="color:#666;">Playlist is empty</p>' : ''}
                </div>
            </div>
        `;
    });
}

async function createPlaylist() {
    const name = document.getElementById('new-playlist-name').value;
    if(!name) return;
    await req('/playlists', 'POST', { username: currentUser.username, name });
    document.getElementById('new-playlist-name').value = '';
    
    // Refresh user object so they get new playlists in dropdowns
    currentUser = await req('/login', 'POST', { username: currentUser.username, password: currentUser.password });
    currentUser.password = currentUser.password; // re-assign local
    
    loadPlaylists();
}

async function addSongToPlaylist(songTitle, playlistName) {
    if(!playlistName) return;
    try {
        await req('/playlists/add', 'POST', { username: currentUser.username, playlistName, songTitle });
    } catch(e) {
        alert(JSON.parse(e.message).error || 'Failed to add');
    }
}
