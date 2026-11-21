<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KIT QGen - Dashboard</title>
    <link rel="stylesheet" href="/css/style.css?v=1.0.2">
</head>
<body>
<c:set var="pageTitle" value="Dashboard" />
<c:if test="${not empty subjects}">
    <c:set var="navRight">
        <button class="btn btn-primary" onclick="document.getElementById('uploadModal').classList.add('active')">
            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/></svg>
            Upload Question Banks
        </button>
    </c:set>
</c:if>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container" style="padding-top: var(--space-6); padding-bottom: var(--space-10);">

    <!-- Flash messages -->
    <c:if test="${not empty message}">
        <div class="alert alert-success" role="alert" aria-live="polite">
            <svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            <div class="alert-content"><c:out value="${message}" /></div>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert" aria-live="assertive">
            <svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            <div class="alert-content"><c:out value="${error}" /></div>
        </div>
    </c:if>
    <c:if test="${not empty uploadErrors}">
        <div class="alert alert-warning" role="alert" aria-live="polite" style="max-height:200px; overflow-y:auto;">
            <svg class="alert-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
            <div class="alert-content">
                <div class="alert-title">Upload Warnings:</div>
                <ul class="alert-list">
                    <c:forEach var="err" items="${uploadErrors}"><li><c:out value="${err}" /></li></c:forEach>
                </ul>
            </div>
        </div>
    </c:if>

    <!-- Page Header -->
    <header class="page-header" style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: var(--space-4); margin-bottom: var(--space-8);">
        <div>
            <h1 class="page-title" style="font-size: 1.75rem; font-weight: 700; letter-spacing: -0.02em;">Subjects</h1>
            <p class="page-subtitle" style="margin-top: var(--space-1); font-size: 1rem; color: var(--color-text-secondary);">Manage your question banks and generate papers</p>
        </div>
        <div style="display: flex; gap: var(--space-3); align-items: center;">
            <span class="count-badge" id="subjectCountBadge">${subjects.size()} subject(s)</span>
        </div>
    </header>

    <!-- Search Bar -->
    <div class="search-box" role="search" aria-label="Search subjects" style="max-width: 480px; margin-bottom: var(--space-6);">
        <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        <input type="text" class="search-input" id="searchInput" placeholder="Search by subject code or name..." aria-label="Search subjects">
    </div>

    <!-- Subject Cards Grid -->
    <div class="grid grid-3" id="subjectGrid" role="list">
        <c:forEach var="s" items="${subjects}">
            <div class="subject-card" data-code="${s.code}" data-name="${s.name}" role="listitem">
                <c:if test="${not empty s.code}">
                    <span class="subject-code"><c:out value="${s.code}" /></span>
                </c:if>
                <div class="subject-name"><c:out value="${s.name}" /></div>
                <div class="subject-stats">
                    <div class="stat-item">
                        <span class="stat-value">${questionCounts[s.id]}</span>
                        <span class="stat-label">Questions</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-value">${importCounts[s.id]}</span>
                        <span class="stat-label">Imports</span>
                    </div>
                </div>
                <div class="subject-actions">
                    <form action="/subjects/${s.id}/open" method="post" style="margin:0; flex:1;">
                        <button type="submit" class="btn btn-primary btn-sm" style="width:100%;">Open</button>
                    </form>
                    <form action="/subjects/${s.id}/delete" method="post" style="margin:0;">
                        <button type="submit" class="btn btn-secondary btn-sm" onclick="return confirm('Delete this subject and all its data?')">Delete</button>
                    </form>
                </div>
            </div>
        </c:forEach>
    </div>

    <!-- Empty State -->
    <c:if test="${empty subjects}">
        <div class="empty-state visible" id="emptyState" style="grid-column: 1 / -1;">
            <div class="empty-icon" aria-hidden="true">
                <svg width="64" height="64" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="opacity:0.3;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9.5a2.5 2.5 0 00-2.5-2.5H15"/></svg>
            </div>
            <div class="empty-title">No subjects yet</div>
            <div class="empty-desc">Click "Upload Question Banks" to add your first DOCX files. Subjects will be created automatically.</div>
            <button class="btn btn-primary" style="margin-top: var(--space-6);" onclick="document.getElementById('uploadModal').classList.add('active')">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/></svg>
                Upload Question Banks
            </button>
        </div>
    </c:if>

    <!-- Search no-results -->
    <div class="empty-state" id="noResults" style="display:none; grid-column: 1 / -1;">
        <div class="empty-icon" aria-hidden="true">
            <svg width="64" height="64" fill="none" stroke="currentColor" viewBox="0 0 24 24" style="opacity:0.3;"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        </div>
        <div class="empty-title">No matching subjects</div>
        <div class="empty-desc">Try a different search term.</div>
    </div>

</div>

<!-- Upload Modal -->
<div class="modal-overlay" id="uploadModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
    <div class="modal">
        <div class="modal-header">
            <h2 class="modal-title" id="modalTitle">Upload Question Banks</h2>
            <button class="modal-close" onclick="document.getElementById('uploadModal').classList.remove('active')" aria-label="Close">
                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
        </div>
        <form action="/upload-bulk" method="post" enctype="multipart/form-data" id="bulkUploadForm" class="modal-body">
            <div class="upload-zone" id="bulkDropZone" onclick="document.getElementById('bulkFiles').click()" role="button" tabindex="0" aria-label="Upload question bank files">
                <svg class="upload-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>
                <div class="upload-title">Drag & drop DOCX files here or click to browse</div>
                <div class="upload-hint">Supports 500+ files at once — automatically grouped by subject code and name</div>
                <div class="upload-file-count" id="fileCount"></div>
                <input type="file" name="files" id="bulkFiles" accept=".docx" multiple required style="display:none" aria-label="Select DOCX files">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="document.getElementById('uploadModal').classList.remove('active')">Cancel</button>
                <button type="submit" id="uploadBtn" class="btn btn-primary" disabled>
                    <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/></svg>
                    Upload & Process
                </button>
            </div>
        </form>
    </div>
</div>

<!-- Upload Progress Overlay -->
<div class="loading-overlay" id="progressOverlay" role="dialog" aria-modal="true" aria-label="Upload progress">
    <div class="loading-card">
        <div class="loading-spinner" aria-hidden="true"></div>
        <div class="loading-title">Processing Files</div>
        <div class="loading-text" id="progressText">Uploading and parsing question banks. This may take a moment for large batches.</div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    // Bulk upload file selection
    const bulkFiles = document.getElementById('bulkFiles');
    const fileCount = document.getElementById('fileCount');
    const uploadBtn = document.getElementById('uploadBtn');
    const bulkDropZone = document.getElementById('bulkDropZone');
    const progressOverlay = document.getElementById('progressOverlay');
    const uploadModal = document.getElementById('uploadModal');

    bulkFiles.addEventListener('change', function() {
        if (this.files.length > 0) {
            fileCount.textContent = this.files.length + ' file(s) selected';
            fileCount.style.display = 'block';
            uploadBtn.disabled = false;
        } else {
            fileCount.style.display = 'none';
            uploadBtn.disabled = true;
        }
    });

    // Drag & drop support
    bulkDropZone.addEventListener('dragover', function(e) {
        e.preventDefault();
        this.classList.add('dragover');
    });
    bulkDropZone.addEventListener('dragleave', function(e) {
        e.preventDefault();
        this.classList.remove('dragover');
    });
    bulkDropZone.addEventListener('drop', function(e) {
        e.preventDefault();
        this.classList.remove('dragover');
        bulkFiles.files = e.dataTransfer.files;
        bulkFiles.dispatchEvent(new Event('change'));
    });
    bulkDropZone.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            bulkFiles.click();
        }
    });

    // Show progress overlay on form submit
    document.getElementById('bulkUploadForm').addEventListener('submit', function() {
        progressOverlay.classList.add('active');
        uploadModal.classList.remove('active');
    });

    // Real-time search filter
    const searchInput = document.getElementById('searchInput');
    const subjectGrid = document.getElementById('subjectGrid');
    const noResults = document.getElementById('noResults');
    const countBadge = document.getElementById('subjectCountBadge');
    const emptyState = document.getElementById('emptyState');

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const query = this.value.toLowerCase().trim();
            const cards = subjectGrid ? subjectGrid.querySelectorAll('.subject-card') : [];
            let visibleCount = 0;

            cards.forEach(function(card) {
                const code = (card.getAttribute('data-code') || '').toLowerCase();
                const name = (card.getAttribute('data-name') || '').toLowerCase();
                const matches = !query || code.includes(query) || name.includes(query);
                card.style.display = matches ? '' : 'none';
                if (matches) visibleCount++;
            });

            if (countBadge) countBadge.textContent = visibleCount + ' subject(s)';
            if (noResults) noResults.style.display = (visibleCount === 0 && query.length > 0) ? 'block' : 'none';
            if (emptyState) emptyState.style.display = (query.length > 0) ? 'none' : (visibleCount === 0 ? 'block' : 'none');
        });
    }

    // Close modal on overlay click
    uploadModal.addEventListener('click', function(e) {
        if (e.target === this) this.classList.remove('active');
    });

    // ESC to close modal
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') uploadModal.classList.remove('active');
    });
});
</script>
</body>
</html>