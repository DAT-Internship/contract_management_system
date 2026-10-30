(() => {
	const root = document.getElementById('importBatchesPage');
	if (!root) return;

	const state = { page: 1, pageSize: 25, total: 0, totalPages: 0, rows: [] };
	const $ = (id) => document.getElementById(id);
	const esc = (value) => String(value ?? '');
	const toast = (title, message) => {
		$('toastTitle').textContent = title;
		$('toastMessage').textContent = message;
		bootstrap.Toast.getOrCreateInstance($('importToast')).show();
	};
	const request = async (url, options = {}) => {
		const csrf = document.querySelector('meta[name="_csrf"]')?.content;
		const header = document.querySelector('meta[name="_csrf_header"]')?.content;
		const headers = new Headers(options.headers || {});
		if (csrf && header) headers.set(header, csrf);
		const response = await fetch(url, { ...options, headers });
		if (!response.ok) throw new Error((await response.text()) || `Request failed (${response.status})`);
		return response.status === 204 ? null : response.json();
	};
	const statusClass = (status) => {
		const value = String(status || '').toLowerCase();
		if (value === 'completed') return 'text-bg-success';
		if (value === 'failed') return 'text-bg-danger';
		if (value.includes('partial')) return 'text-bg-warning';
		if (value === 'processing') return 'text-bg-info';
		return 'text-bg-secondary';
	};
	const dateText = (value) => {
		if (!value) return 'â€”';
		const date = new Date(value);
		return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
	};
	const applyCounts = (data) => {
		const rows = Array.isArray(data) ? data : (data?.data || data?.content || []);
		const summary = data?.summary || {};
		$('totalBatches').textContent = summary.total ?? data?.total ?? rows.length;
		$('completedBatches').textContent = summary.completed ?? rows.filter(x => String(x.status).toLowerCase() === 'completed').length;
		$('failedBatches').textContent = summary.failed ?? rows.filter(x => String(x.status).toLowerCase() === 'failed').length;
		$('processingBatches').textContent = summary.inProgress ?? rows.filter(x => ['processing', 'pending'].includes(String(x.status).toLowerCase())).length;
	};
	const cell = (row, value, className = '') => {
		const td = document.createElement('td');
		td.textContent = esc(value);
		if (className) td.className = className;
		row.append(td);
		return td;
	};
	const renderPagination = () => {
		const nav = $('batchPagination'); nav.replaceChildren();
		const add = (label, page, disabled = false, active = false) => {
			const li = document.createElement('li'); li.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;
			const a = document.createElement('a'); a.className = 'page-link'; a.href = '#batchRows'; a.textContent = label;
			a.addEventListener('click', e => { e.preventDefault(); if (!disabled) { state.page = page; loadBatches(); } });
			li.append(a); nav.append(li);
		};
		add('Previous', state.page - 1, state.page <= 1);
		const totalPages = Math.max(1, state.totalPages || 1);
		const first = Math.max(1, Math.min(state.page - 2, totalPages - 4));
		for (let p = first; p <= Math.min(totalPages, first + 4); p++) add(String(p), p, false, p === state.page);
		add('Next', state.page + 1, state.page >= totalPages);
		const firstRecord = state.total ? (state.page - 1) * state.pageSize + 1 : 0;
		$('batchRange').textContent = `Showing ${firstRecord}â€“${Math.min(state.page * state.pageSize, state.total)} of ${state.total}`;
	};
	const renderRows = (rows) => {
		const body = $('batchRows'); body.replaceChildren();
		if (!rows.length) {
			const tr = document.createElement('tr'); const td = document.createElement('td');
			td.colSpan = 8; td.className = 'text-center py-5 text-muted'; td.textContent = 'No import batches found.'; tr.append(td); body.append(tr); return;
		}
		rows.forEach(batch => {
			const tr = document.createElement('tr');
			const id = batch.batchId ?? batch.id;
			const idCell = document.createElement('td'); const view = document.createElement('button');
			view.type = 'button'; view.className = 'btn btn-link p-0 text-decoration-none'; view.textContent = `#${esc(id)}`;
			view.addEventListener('click', () => showDetails(batch)); idCell.append(view); tr.append(idCell);
			cell(tr, batch.fileName);
			const kind = document.createElement('td'); const kindBadge = document.createElement('span'); kindBadge.className = 'badge text-bg-light'; kindBadge.textContent = String(batch.batchType ?? batch.type ?? '').toLowerCase().includes('user') ? 'User Excel' : 'Contract Excel'; kind.append(kindBadge); tr.append(kind);
			cell(tr, batch.totalRows ?? batch.total ?? 0);
			const statusCell = document.createElement('td'); const badge = document.createElement('span'); badge.className = `badge ${statusClass(batch.status)}`; badge.textContent = esc(batch.status || 'Pending'); statusCell.append(badge); tr.append(statusCell);
			cell(tr, batch.uploadedByName ?? batch.uploadedBy ?? 'â€”'); cell(tr, dateText(batch.createdAt ?? batch.uploadedAt));
			const actions = document.createElement('td'); actions.className = 'text-nowrap';
			const addAction = (label, cls, handler, disabled = false) => { const btn = document.createElement('button'); btn.type = 'button'; btn.className = `btn btn-sm ${cls} me-1`; btn.textContent = label; btn.disabled = disabled; btn.addEventListener('click', handler); actions.append(btn); };
			addAction('View', 'btn-outline-primary', () => showDetails(batch));
			tr.append(actions); body.append(tr);
		});
	};
	const loadBatches = async () => {
		$('batchLoading').textContent = 'Loadingâ€¦';
		const params = new URLSearchParams({ page: state.page, pageSize: state.pageSize });
		try {
			const result = await request(`/api/import-batches?${params}`);
			const rows = Array.isArray(result) ? result : (result?.data || result?.content || []);
			state.rows = rows; state.total = Number(result?.total ?? rows.length);
			state.totalPages = Number(result?.totalPages ?? Math.max(1, Math.ceil(state.total / state.pageSize)));
			applyCounts(result); renderRows(rows); renderPagination(); $('batchLoading').textContent = '';
		} catch (error) {
			state.total = 0; state.totalPages = 1; renderRows([]); renderPagination();
			$('batchLoading').textContent = 'Batch service is unavailable.';
		}
	};
	const showDetails = async (batch) => {
		const id = batch.batchId ?? batch.id;
		$('detailBatchSummary').textContent = `Batch #${id} Â· ${batch.status || 'Pending'} Â· ${dateText(batch.createdAt ?? batch.uploadedAt)}`;
		const body = $('batchErrors'); body.replaceChildren();
		try {
			const result = await request(`/api/import-batches/${encodeURIComponent(id)}`);
			const errors = result?.errors || result?.errorRows || [];
			if (!errors.length) { const tr = body.insertRow(); const td = tr.insertCell(); td.colSpan = 3; td.textContent = 'No row errors reported.'; td.className = 'text-muted'; }
			else errors.forEach(item => { const tr = body.insertRow(); [item.rowNumber ?? item.row, item.fieldName ?? item.field, item.errorMessage ?? item.message ?? item.error].forEach(value => { const td = tr.insertCell(); td.textContent = esc(value); }); });
		} catch { const tr = body.insertRow(); const td = tr.insertCell(); td.colSpan = 3; td.textContent = 'Error details are unavailable.'; td.className = 'text-muted'; }
		bootstrap.Modal.getOrCreateInstance($('batchDetailModal')).show();
	};
	const formatSize = bytes => bytes < 1024 ? `${bytes} B` : bytes < 1048576 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1048576).toFixed(1)} MB`;
	const setFile = (zone, file) => {
		if (!file) return;
		if (!/\.xlsx$/i.test(file.name)) { toast('Unsupported file', 'Choose an .xlsx workbook.'); return; }
		zone.dataset.fileName = file.name;
		zone._file = file;
		const selected = zone.querySelector('.upload-selected'); selected.textContent = `${file.name} Â· ${formatSize(file.size)}`; selected.classList.remove('d-none');
		zone.querySelector('.upload-submit').disabled = false;
	};
	const zone = $('importUploadZone');
	let selectedType = 'contract';
	const config = { contract: { title: 'Drop your contract workbook here', helper: 'Columns: contract_no, customer, project_id, category, status, department, value, currency, start_date, end_date', note: 'Customer, project, category, status, department, and currency must match existing master records.', template: '/api/import-batches/templates/contracts', label: 'Contract Excel' }, user: { title: 'Drop your user workbook here', helper: 'Columns: username, email, role, department, status', note: 'Use the user template and ensure referenced departments and roles exist.', template: '/api/import-batches/templates/users', label: 'User Excel' } };
	const chooseType = type => { selectedType = config[type] ? type : 'contract'; const item = config[selectedType]; zone.dataset.kind = selectedType; $('dropTitle').textContent = item.title; $('uploadHelper').textContent = item.helper; $('uploadNote').textContent = item.note; $('templateLink').href = item.template; $('templateLink').textContent = `Download ${item.label} Template`; zone.querySelector('.upload-submit').textContent = `Upload ${item.label}`; document.querySelectorAll('.import-type-button').forEach(button => { const active = button.dataset.kind === selectedType; button.classList.toggle('active',active); button.classList.toggle('btn-primary',active); button.classList.toggle('btn-outline-primary',!active); }); };
	document.querySelectorAll('.import-type-button').forEach(button => button.addEventListener('click', () => chooseType(button.dataset.kind)));
	const input = zone.querySelector('.upload-input');
	input.addEventListener('change', () => setFile(zone,input.files[0]));
	zone.addEventListener('dragover', event => { event.preventDefault(); zone.classList.add('drag-over'); });
	zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
	zone.addEventListener('drop', event => { event.preventDefault(); zone.classList.remove('drag-over'); setFile(zone,event.dataTransfer.files[0]); });
	zone.querySelector('.upload-submit').addEventListener('click', () => upload(zone));
	const upload = zone => {
		if (!zone._file) return;
		const button = zone.querySelector('.upload-submit'); button.disabled = true; button.textContent = 'Uploading…';
		const progress = zone.querySelector('.upload-progress'); const progressBar = progress.querySelector('.progress-bar'); progress.classList.remove('d-none'); progressBar.style.width = '0%';
		const form = new FormData(); form.append('type',selectedType); form.append('file',zone._file);
		const xhr = new XMLHttpRequest(); xhr.open('POST','/api/import-batches');
		const csrf = document.querySelector('meta[name="_csrf"]')?.content; const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content; if (csrf && csrfHeader) xhr.setRequestHeader(csrfHeader,csrf);
		xhr.upload.onprogress = event => { if(event.lengthComputable){const percent=Math.round(event.loaded*100/event.total);button.textContent=`Uploading ${percent}%`;progressBar.style.width=`${percent}%`;progressBar.setAttribute('aria-valuenow',percent);} };
		xhr.onload = () => { button.textContent=`Upload ${config[selectedType].label}`; if(xhr.status>=200&&xhr.status<300){progressBar.style.width='100%';progressBar.setAttribute('aria-valuenow','100');zone._file=null;input.value='';zone.querySelector('.upload-selected').classList.add('d-none');button.disabled=true;let detail='File uploaded.';try{detail=JSON.parse(xhr.responseText).message||detail;}catch{}toast('Import result',detail);state.page=1;loadBatches();}else{button.disabled=false;progress.classList.add('d-none');toast('Upload failed',xhr.responseText||`Server returned ${xhr.status}.`);} };
		xhr.onerror = () => {progress.classList.add('d-none');button.textContent=`Upload ${config[selectedType].label}`;button.disabled=false;toast('Upload failed','Could not connect to the import service.');}; xhr.send(form);
	};$('batchPageSize').addEventListener('change', event => { state.pageSize = Number(event.target.value); state.page = 1; loadBatches(); });
	const initialType = root.dataset.initialType;
	if (initialType?.toLowerCase().startsWith('user')) chooseType('user');
	loadBatches();
})();

