(() => {
	// ==========================================
	// 1. Date & Man-Month Calculations
	// ==========================================
	const start = document.getElementById('startDate');
	const end = document.getElementById('endDate');
	const period = document.getElementById('periodMonths');
	const headcount = document.getElementById('headcountPerMonth');
	const manMonths = document.getElementById('totalManMonths');
	const remaining = document.getElementById('remainingDays');

	const asUtcDate = (value) => {
		if (!value) return null;
		const [year, month, day] = value.split('-').map(Number);
		return new Date(Date.UTC(year, month - 1, day));
	};

	const updatePeriod = () => {
		if (!start || !end) return;
		const first = asUtcDate(start.value);
		const last = asUtcDate(end.value);
		if (!first || !last || last < first) {
			if (period) period.value = '';
			if (remaining) remaining.value = '';
			updateManMonths();
			return;
		}

		last.setUTCDate(last.getUTCDate() + 1);
		let months = (last.getUTCFullYear() - first.getUTCFullYear()) * 12
			+ last.getUTCMonth() - first.getUTCMonth();
		if (last.getUTCDate() < first.getUTCDate()) months -= 1;
		if (period && !period.dataset.edited) period.value = Math.max(1, months);

		const now = new Date();
		const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
		if (remaining) {
			remaining.value = Math.max(0, Math.ceil((asUtcDate(end.value).getTime() - today) / 86400000));
		}
		updateManMonths();
	};

	const updateManMonths = () => {
		if (!manMonths) return;
		const months = Number(period?.value || 0);
		const people = Number(headcount?.value || 0);
		manMonths.value = (months * people).toFixed(2);
	};

	if (start && end) {
		start.addEventListener('change', updatePeriod);
		end.addEventListener('change', updatePeriod);
		headcount?.addEventListener('input', updateManMonths);
		period?.addEventListener('input', () => {
			period.dataset.edited = period.value ? 'true' : '';
			updateManMonths();
		});
		updatePeriod();
	}

	// ==========================================
	// 2. Attachments Upload & Drag and Drop
	// ==========================================
	const attachmentBox = document.getElementById('attachmentBox');
	const fileInput = document.getElementById('contractFilesInput');
	const filesContainer = document.getElementById('selectedFilesContainer');
	const filesList = document.getElementById('selectedFilesList');
	const fileCountSpan = document.getElementById('fileCount');
	const clearAllBtn = document.getElementById('clearAllFiles');

	if (attachmentBox && fileInput) {
		let dt = new DataTransfer();

		const formatFileSize = (bytes) => {
			if (bytes === 0) return '0 B';
			const k = 1024;
			const sizes = ['B', 'KB', 'MB', 'GB'];
			const i = Math.floor(Math.log(bytes) / Math.log(k));
			return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
		};

		const getFileIconClass = (fileName) => {
			const ext = fileName.split('.').pop().toLowerCase();
			if (ext === 'pdf') return 'fa-solid fa-file-pdf text-danger';
			if (['doc', 'docx'].includes(ext)) return 'fa-solid fa-file-word text-primary';
			if (['xls', 'xlsx'].includes(ext)) return 'fa-solid fa-file-excel text-success';
			if (['jpg', 'jpeg', 'png'].includes(ext)) return 'fa-solid fa-file-image text-info';
			return 'fa-solid fa-file text-secondary';
		};

		const renderSelectedFiles = () => {
			if (!filesList || !filesContainer || !fileCountSpan) return;

			filesList.innerHTML = '';
			const files = dt.files;

			if (files.length === 0) {
				filesContainer.classList.add('d-none');
				fileInput.files = dt.files;
				return;
			}

			filesContainer.classList.remove('d-none');
			fileCountSpan.textContent = files.length;

			Array.from(files).forEach((file, index) => {
				const item = document.createElement('div');
				item.className = 'selected-file-item';

				const info = document.createElement('div');
				info.className = 'selected-file-info';

				const icon = document.createElement('i');
				icon.className = getFileIconClass(file.name);

				const nameSpan = document.createElement('span');
				nameSpan.className = 'selected-file-name';
				nameSpan.textContent = file.name;
				nameSpan.title = file.name;

				const sizeSpan = document.createElement('span');
				sizeSpan.className = 'selected-file-size';
				sizeSpan.textContent = `(${formatFileSize(file.size)})`;

				info.appendChild(icon);
				info.appendChild(nameSpan);
				info.appendChild(sizeSpan);

				const removeBtn = document.createElement('button');
				removeBtn.type = 'button';
				removeBtn.className = 'selected-file-remove';
				removeBtn.title = 'Remove file';
				removeBtn.innerHTML = '<i class="fa-solid fa-trash-can"></i>';

				removeBtn.addEventListener('click', () => {
					const newDt = new DataTransfer();
					Array.from(dt.files).forEach((f, i) => {
						if (i !== index) newDt.items.add(f);
					});
					dt = newDt;
					fileInput.files = dt.files;
					renderSelectedFiles();
				});

				item.appendChild(info);
				item.appendChild(removeBtn);
				filesList.appendChild(item);
			});

			fileInput.files = dt.files;
		};

		const addFiles = (newFiles) => {
			Array.from(newFiles).forEach(file => {
				// Prevent duplicate by filename and size
				const exists = Array.from(dt.files).some(f => f.name === file.name && f.size === file.size);
				if (!exists) {
					dt.items.add(file);
				}
			});
			renderSelectedFiles();
		};

		fileInput.addEventListener('change', (e) => {
			if (e.target.files) {
				addFiles(e.target.files);
			}
		});

		clearAllBtn?.addEventListener('click', () => {
			dt = new DataTransfer();
			fileInput.value = '';
			fileInput.files = dt.files;
			renderSelectedFiles();
		});

		// Drag and Drop events
		['dragenter', 'dragover'].forEach(eventName => {
			attachmentBox.addEventListener(eventName, (e) => {
				e.preventDefault();
				e.stopPropagation();
				attachmentBox.classList.add('drag-over');
			}, false);
		});

		['dragleave', 'drop'].forEach(eventName => {
			attachmentBox.addEventListener(eventName, (e) => {
				e.preventDefault();
				e.stopPropagation();
				attachmentBox.classList.remove('drag-over');
			}, false);
		});

		attachmentBox.addEventListener('drop', (e) => {
			const droppedFiles = e.dataTransfer?.files;
			if (droppedFiles && droppedFiles.length > 0) {
				addFiles(droppedFiles);
			}
		}, false);
	}

	// ==========================================
	// 3. Draft vs Submit Status Auto-Select
	// ==========================================
	const statusSelect = document.getElementById('statusId');
	const draftBtn = document.querySelector('button[value="draft"]');
	const submitBtn = document.querySelector('button[value="submit"]');

	if (statusSelect) {
		const selectOptionByKeyword = (keywords) => {
			if (statusSelect.value) return; // keep current choice if chosen
			for (const opt of statusSelect.options) {
				const text = (opt.textContent || '').toLowerCase();
				if (keywords.some(k => text.includes(k))) {
					statusSelect.value = opt.value;
					break;
				}
			}
		};

		draftBtn?.addEventListener('click', () => {
			selectOptionByKeyword(['draft', 'waiting', 'pending']);
		});

		submitBtn?.addEventListener('click', () => {
			selectOptionByKeyword(['waiting', 'active', 'approval']);
		});
	}
})();
