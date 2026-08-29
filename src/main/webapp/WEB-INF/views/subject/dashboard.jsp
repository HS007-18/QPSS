<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>KIT QGen - Dashboard</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .dashboard-hero {
            background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
            border-radius: 16px;
            padding: 40px 36px;
            margin-bottom: 32px;
            color: #fff;
            position: relative;
            overflow: hidden;
        }
        .dashboard-hero::after {
            content: '';
            position: absolute;
            top: -60%;
            right: -15%;
            width: 400px;
            height: 400px;
            background: rgba(255,255,255,0.05);
            border-radius: 50%;
            pointer-events: none;
        }
        .dashboard-hero h1 {
            font-size: 28px;
            font-weight: 700;
            margin-bottom: 8px;
            color: #fff;
        }
        .dashboard-hero p {
            font-size: 15px;
            opacity: 0.85;
            margin-bottom: 24px;
        }
        .bulk-upload-zone {
            border: 2px dashed rgba(255,255,255,0.35);
            border-radius: 12px;
            padding: 28px 24px;
            text-align: center;
            cursor: pointer;
            transition: all 0.25s;
            background: rgba(255,255,255,0.06);
        }
        .bulk-upload-zone:hover {
            border-color: rgba(255,255,255,0.7);
            background: rgba(255,255,255,0.12);
        }
        .bulk-upload-zone .upload-label {
            font-weight: 600;
            font-size: 15px;
            margin-bottom: 4px;
        }
        .bulk-upload-zone .upload-hint {
            font-size: 13px;
            opacity: 0.7;
        }
        .bulk-upload-zone .file-count {
            margin-top: 8px;
            font-size: 13px;
            font-weight: 600;
            color: #fbbf24;
            display: none;
        }

        .search-bar {
            position: relative;
            margin-bottom: 24px;
        }
        .search-bar input {
            width: 100%;
            padding: 14px 20px 14px 48px;
            border-radius: 12px;
            border: 1px solid var(--border);
            font-size: 15px;
            background: var(--bg-card);
            box-shadow: var(--shadow-sm);
            transition: all 0.2s;
        }
        .search-bar input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--primary-light);
        }
        .search-bar .search-icon {
            position: absolute;
            left: 16px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
        }

        .subject-card {
            background: var(--bg-card);
            border-radius: 14px;
            border: 1px solid var(--border);
            padding: 24px;
            box-shadow: var(--shadow-sm);
            display: flex;
            flex-direction: column;
            transition: all 0.2s;
            position: relative;
        }
        .subject-card:hover {
            box-shadow: var(--shadow-md);
            transform: translateY(-2px);
            border-color: var(--primary);
        }
        .subject-code {
            display: inline-block;
            background: var(--primary-light);
            color: var(--primary);
            font-size: 12px;
            font-weight: 700;
            padding: 4px 10px;
            border-radius: 6px;
            margin-bottom: 10px;
            letter-spacing: 0.5px;
        }
        .subject-name {
            font-size: 17px;
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 12px;
            line-height: 1.3;
        }
        .subject-stats {
            display: flex;
            gap: 16px;
            margin-bottom: 16px;
            padding-bottom: 16px;
            border-bottom: 1px solid var(--bg-hover);
        }
        .stat-item {
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .stat-value {
            font-size: 20px;
            font-weight: 700;
            color: var(--text-main);
            line-height: 1;
        }
        .stat-label {
            font-size: 11px;
            color: var(--text-muted);
            margin-top: 4px;
            text-transform: uppercase;
            letter-spacing: 0.3px;
        }
        .subject-actions {
            margin-top: auto;
            display: flex;
            gap: 8px;
        }
        .subject-actions .btn { flex: 1; }

        .no-results {
            text-align: center;
            padding: 60px 24px;
            color: var(--text-muted);
            display: none;
        }
        .no-results.visible { display: block; }
        .no-results .icon { font-size: 48px; margin-bottom: 16px; opacity: 0.4; }

        .alert-banner {
            padding: 16px 20px;
            border-radius: 12px;
            margin-bottom: 20px;
            font-size: 14px;
            line-height: 1.5;
        }
        .alert-success {
            background: var(--success-light);
            border: 1px solid var(--success);
            color: #065f46;
        }
        .alert-error {
            background: var(--danger-light);
            border: 1px solid var(--danger);
            color: #991b1b;
        }

        .section-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
        }
        .section-title {
            font-size: 18px;
            font-weight: 600;
            color: var(--text-main);
        }
        .subject-count-badge {
            background: var(--bg-hover);
            color: var(--text-muted);
            font-size: 13px;
            font-weight: 600;
            padding: 4px 12px;
            border-radius: 20px;
            border: 1px solid var(--border);
        }

        .progress-overlay {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.5);
            z-index: 200;
            align-items: center;
            justify-content: center;
        }
        .progress-overlay.active { display: flex; }
        .progress-card {
            background: var(--bg-card);
            border-radius: 16px;
            padding: 48px 40px;
            text-align: center;
            box-shadow: var(--shadow-lg);
            max-width: 400px;
            width: 90%;
        }
        .progress-card h3 {
            font-size: 20px;
            font-weight: 700;
            margin-bottom: 8px;
        }
        .progress-card p {
            font-size: 14px;
            color: var(--text-muted);
            margin-bottom: 24px;
        }
        .spinner {
            width: 48px;
            height: 48px;
            border: 4px solid var(--bg-hover);
            border-top-color: var(--primary);
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
            margin: 0 auto 24px;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Dashboard" />
<c:set var="navRight"><a href="/history" class="btn btn-outline btn-sm">View History</a></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container">

    <%-- Flash messages --%>
    <c:if test="${not empty message}">
        <div class="alert-banner alert-success">
            <strong>✓</strong> <c:out value="${message}" />
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert-banner alert-error">
            <strong>✗</strong> <c:out value="${error}" />
        </div>
    </c:if>
    <c:if test="${not empty uploadErrors}">
        <div class="alert-banner alert-error" style="max-height:200px; overflow-y:auto;">
            <strong>Upload Warnings:</strong>
            <ul style="margin: 8px 0 0 20px; font-size:13px;">
                <c:forEach var="err" items="${uploadErrors}">
                    <li><c:out value="${err}" /></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <%-- Hero: Bulk Upload Zone --%>
    <div class="dashboard-hero">
        <h1>Question Paper Support System</h1>
        <p>Upload question banks in bulk — files are automatically grouped by subject code and name.</p>

        <form action="/upload-bulk" method="post" enctype="multipart/form-data" id="bulkUploadForm">
            <div class="bulk-upload-zone" id="bulkDropZone" onclick="document.getElementById('bulkFiles').click()">
                <svg style="width:36px;height:36px;margin-bottom:8px;opacity:0.8;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                <div class="upload-label">Drag & drop DOCX files here or click to browse</div>
                <div class="upload-hint">Supports 500+ files at once — automatically grouped by subject</div>
                <div class="file-count" id="fileCount"></div>
                <input type="file" name="files" id="bulkFiles" accept=".docx" multiple required style="display:none">
            </div>
            <div style="margin-top:16px; display:flex; gap:12px; justify-content:flex-end;">
                <button type="submit" id="uploadBtn" class="btn" style="background:rgba(255,255,255,0.2); color:#fff; border:1px solid rgba(255,255,255,0.3); padding:10px 28px;" disabled>
                    Upload & Process
                </button>
            </div>
        </form>
    </div>

    <%-- Search Bar --%>
    <div class="search-bar">
        <svg class="search-icon" style="width:20px;height:20px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
        <input type="text" id="searchInput" placeholder="Search by subject code or name... (e.g. ME3491, Theory of Machines)">
    </div>

    <%-- Section Header --%>
    <div class="section-header">
        <div class="section-title">Subjects</div>
        <span class="subject-count-badge" id="subjectCountBadge">${subjects.size()} subject(s)</span>
    </div>

    <%-- Subject Cards Grid --%>
    <div class="grid-3" id="subjectGrid">
        <c:forEach var="s" items="${subjects}">
            <div class="subject-card" data-code="${s.code}" data-name="${s.name}">
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
                        <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('Delete this subject and all its data?')">Delete</button>
                    </form>
                </div>
            </div>
        </c:forEach>
    </div>

    <%-- Empty State --%>
    <c:if test="${empty subjects}">
        <div class="no-results visible" id="emptyState">
            <div class="icon">📚</div>
            <div style="font-size:16px; font-weight:500; color:var(--text-main);">No subjects yet</div>
            <div style="color:var(--text-muted); font-size:14px; margin-top:4px;">Upload DOCX question bank files to get started. Subjects will be created automatically.</div>
        </div>
    </c:if>

    <%-- Search no-results --%>
    <div class="no-results" id="noResults">
        <div class="icon">🔍</div>
        <div style="font-size:16px; font-weight:500; color:var(--text-main);">No matching subjects</div>
        <div style="color:var(--text-muted); font-size:14px; margin-top:4px;">Try a different search term.</div>
    </div>


</div>

<%-- Upload Progress Overlay --%>
<div class="progress-overlay" id="progressOverlay">
    <div class="progress-card">
        <div class="spinner"></div>
        <h3>Processing Files...</h3>
        <p id="progressText">Uploading and parsing question banks. This may take a moment for large batches.</p>
    </div>
</div>

<script>
    // Bulk upload file selection
    const bulkFiles = document.getElementById('bulkFiles');
    const fileCount = document.getElementById('fileCount');
    const uploadBtn = document.getElementById('uploadBtn');
    const bulkDropZone = document.getElementById('bulkDropZone');

    bulkFiles.addEventListener('change', function() {
        if (this.files.length > 0) {
            fileCount.textContent = this.files.length + ' file(s) selected';
            fileCount.style.display = 'block';
            uploadBtn.disabled = false;
            uploadBtn.style.background = '#fff';
            uploadBtn.style.color = 'var(--primary)';
            uploadBtn.style.fontWeight = '600';
        } else {
            fileCount.style.display = 'none';
            uploadBtn.disabled = true;
            uploadBtn.style.background = 'rgba(255,255,255,0.2)';
            uploadBtn.style.color = '#fff';
        }
    });

    // Drag & drop support
    bulkDropZone.addEventListener('dragover', function(e) {
        e.preventDefault();
        this.style.borderColor = 'rgba(255,255,255,0.8)';
        this.style.background = 'rgba(255,255,255,0.15)';
    });
    bulkDropZone.addEventListener('dragleave', function(e) {
        e.preventDefault();
        this.style.borderColor = 'rgba(255,255,255,0.35)';
        this.style.background = 'rgba(255,255,255,0.06)';
    });
    bulkDropZone.addEventListener('drop', function(e) {
        e.preventDefault();
        this.style.borderColor = 'rgba(255,255,255,0.35)';
        this.style.background = 'rgba(255,255,255,0.06)';
        bulkFiles.files = e.dataTransfer.files;
        bulkFiles.dispatchEvent(new Event('change'));
    });

    // Show progress overlay on form submit
    document.getElementById('bulkUploadForm').addEventListener('submit', function() {
        document.getElementById('progressOverlay').classList.add('active');
    });

    // Real-time search filter
    const searchInput = document.getElementById('searchInput');
    const subjectGrid = document.getElementById('subjectGrid');
    const noResults = document.getElementById('noResults');
    const countBadge = document.getElementById('subjectCountBadge');

    searchInput.addEventListener('input', function() {
        const query = this.value.toLowerCase().trim();
        const cards = subjectGrid.querySelectorAll('.subject-card');
        let visibleCount = 0;

        cards.forEach(function(card) {
            const code = (card.getAttribute('data-code') || '').toLowerCase();
            const name = (card.getAttribute('data-name') || '').toLowerCase();
            const matches = !query || code.includes(query) || name.includes(query);
            card.style.display = matches ? '' : 'none';
            if (matches) visibleCount++;
        });

        countBadge.textContent = visibleCount + ' subject(s)';
        noResults.classList.toggle('visible', visibleCount === 0 && query.length > 0);
    });
</script>
</body>
</html>